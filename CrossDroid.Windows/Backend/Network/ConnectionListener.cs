using System;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography.X509Certificates;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace CrossDroid.Windows.Backend.Network;

public sealed class ConnectionListener : IDisposable
{
    private readonly IdentityService _identity;
    private readonly DeviceService _devices;
    private readonly TransferQueueService _transfers;
    private TcpListener? _listener;
    private CancellationTokenSource? _cts;
    private Task? _listenTask;

    public int Port { get; private set; }

    public ConnectionListener(IdentityService identity, DeviceService devices, TransferQueueService transfers)
    {
        _identity = identity;
        _devices = devices;
        _transfers = transfers;
    }

    public void Start()
    {
        if (_cts != null) return;
        _cts = new CancellationTokenSource();

        _listener = new TcpListener(IPAddress.Any, 0); // Port 0 automatically selects an available port
        _listener.Start();
        Port = ((IPEndPoint)_listener.LocalEndpoint).Port;

        _listenTask = Task.Run(() => ListenLoopAsync(_cts.Token), _cts.Token);
    }

    public void Stop()
    {
        _cts?.Cancel();
        _listener?.Stop();
        _cts?.Dispose();
        _cts = null;
    }

    public void Dispose()
    {
        Stop();
    }

    private async Task ListenLoopAsync(CancellationToken token)
    {
        if (_listener == null) return;
        
        while (!token.IsCancellationRequested)
        {
            try
            {
                var client = await _listener.AcceptTcpClientAsync(token);
                _ = Task.Run(() => HandleClientAsync(client, token));
            }
            catch (OperationCanceledException) { break; }
            catch (Exception ex)
            {
                Debug.WriteLine($"Connection listener error: {ex.Message}");
                await Task.Delay(1000, token);
            }
        }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken token)
    {
        using var session = new SecureSession(client);
        try
        {
            await session.AuthenticateAsServerAsync(_identity.GetCertificate(), token);

            // Read the first message
            var (message, binaryPayload) = await session.ReadMessageAsync(token);

            if (message.Type == MessageType.PairRequest)
            {
                await HandlePairRequestAsync(session, message, token);
            }
            else if (message.Type == MessageType.TransferOffer)
            {
                await HandleTransferOfferAsync(session, message, token);
            }
            else
            {
                Debug.WriteLine($"Unexpected first message type: {message.Type}");
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Failed handling incoming connection: {ex.Message}");
        }
    }

    private async Task HandlePairRequestAsync(SecureSession session, ProtocolMessage message, CancellationToken token)
    {
        var request = JsonSerializer.Deserialize<PairRequestPayload>(message.PayloadJson);
        if (request == null) return;

        // Auto-accept pairing for now. In a real app, prompt the user or check PIN.
        var responsePayload = new PairResponsePayload
        {
            Accepted = true,
            PublicKeyBase64 = _identity.GetCertificate().GetCertHashString()
        };

        var response = new ProtocolMessage
        {
            Type = MessageType.PairResponse,
            PayloadJson = JsonSerializer.Serialize(responsePayload)
        };

        await session.WriteMessageAsync(response, null, token);
        
        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
        dispatcher?.TryEnqueue(() =>
        {
            _devices.UpdateFromDiscovery(request.DeviceId, request.DisplayName, "Device", session.RemoteFingerprint, "");
        });
    }

    private async Task HandleTransferOfferAsync(SecureSession session, ProtocolMessage message, CancellationToken token)
    {
        var offer = JsonSerializer.Deserialize<TransferOfferPayload>(message.PayloadJson);
        if (offer == null) return;

        // Auto-accept for now
        var acceptMsg = new ProtocolMessage
        {
            Type = MessageType.TransferAccept,
            PayloadJson = JsonSerializer.Serialize(new TransferAcceptPayload
            {
                TransferId = offer.TransferId,
                Accepted = true
            })
        };
        await session.WriteMessageAsync(acceptMsg, null, token);

        // Hand off to TransferQueueService
        await _transfers.ReceiveNetworkTransferAsync(session, offer);
    }
}
