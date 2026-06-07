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
    
    private readonly HashSet<string> _activePrompts = new();

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
            var (message, binaryPayload, binLen) = await session.ReadMessageAsync(token);
            try
            {
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
            finally
            {
                if (binaryPayload != null)
                {
                    System.Buffers.ArrayPool<byte>.Shared.Return(binaryPayload);
                }
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

        var device = _devices.Devices.FirstOrDefault(d => d.Fingerprint == session.RemoteFingerprint)
            ?? new DeviceRecord
            {
                DeviceId = request.DeviceId,
                DisplayName = request.DisplayName,
                Fingerprint = session.RemoteFingerprint
            };

        if (device.IsBlocked) return;

        bool accepted = false;
        var pairing = CrossDroidBackend.Current?.Pairing;

        if (!string.IsNullOrEmpty(request.Pin))
        {
            // The client is explicitly attempting PIN authentication
            if (pairing != null && pairing.IsPinValid)
            {
                if (request.Pin == pairing.CurrentPin)
                {
                    accepted = true;
                    pairing.FailedAttempts = 0; // Reset on success
                }
                else
                {
                    pairing.FailedAttempts++;
                    if (pairing.FailedAttempts >= 5)
                    {
                        pairing.InvalidatePin();
                    }
                    accepted = false; // Immediately reject incorrect PINs
                }
            }
            else
            {
                accepted = false; // PIN is no longer valid or enabled
            }
        }
        else
        {
            // No PIN provided, fall back to manual user approval
            if (!_activePrompts.Add(device.DeviceId))
            {
                Debug.WriteLine($"Prompt already active for {device.DeviceId}. Rejecting new pair request.");
                return; // Silently drop
            }

            try
            {
                var mainWindow = CrossDroid.Windows.App.MainWindowInstance;
                if (mainWindow != null)
                {
                    accepted = await mainWindow.ShowPairingPromptAsync(request.DisplayName, request.Pin);
                }

                if (accepted)
                {
                    device.RejectedCount = 0;
                }
                else
                {
                    device.RejectedCount++;
                    if (device.RejectedCount >= 3)
                    {
                        device.IsBlocked = true;
                        _ = _devices.SaveDevicesAsync();
                    }
                }
            }
            finally
            {
                _activePrompts.Remove(device.DeviceId);
            }
        }

        var responsePayload = new PairResponsePayload
        {
            Accepted = accepted,
            PublicKeyBase64 = _identity.GetCertificate().GetCertHashString()
        };

        var response = new ProtocolMessage
        {
            Type = MessageType.PairResponse,
            PayloadJson = JsonSerializer.Serialize(responsePayload)
        };

        await session.WriteMessageAsync(response, null, token);
        
        if (accepted)
        {
            var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
            dispatcher?.TryEnqueue(() =>
            {
                _devices.UpdateFromDiscovery(request.DeviceId, request.DisplayName, "Device", session.RemoteFingerprint, "");
                var device = _devices.Devices.FirstOrDefault(d => d.DeviceId == request.DeviceId);
                if (device != null)
                {
                    device.TrustState = DeviceTrustState.Trusted;
                    device.PairingStatus = PairingStatus.Paired;
                    device.ConnectionStatus = ConnectionStatus.Ready;
                    device.EncryptionStatus = EncryptionStatus.Negotiated;
                }
            });
        }
    }

    private async Task HandleTransferOfferAsync(SecureSession session, ProtocolMessage message, CancellationToken token)
    {
        var offer = JsonSerializer.Deserialize<TransferOfferPayload>(message.PayloadJson);
        if (offer == null) return;

        var device = _devices.Devices.FirstOrDefault(d => d.Fingerprint == session.RemoteFingerprint)
            ?? new DeviceRecord
            {
                DeviceId = "TempPeer",
                DisplayName = "Unknown Device",
                Fingerprint = session.RemoteFingerprint
            };

        // Silently reject transfers from blocked devices
        if (device.IsBlocked)
        {
            var rejectMsg = new ProtocolMessage
            {
                Type = MessageType.TransferReject,
                PayloadJson = JsonSerializer.Serialize(new TransferAcceptPayload
                {
                    TransferId = offer.TransferId,
                    Accepted = false
                })
            };
            await session.WriteMessageAsync(rejectMsg, ReadOnlyMemory<byte>.Empty, token);
            Debug.WriteLine($"Silently rejected transfer from blocked device: {device.AliasOrName}");
            return;
        }

        bool isResume = false;
        long requestedOffset = 0;
        
        // Check if this is a resume attempt for an existing transfer
        string? existingDestinationPath = null;
        var histRecord = CrossDroidBackend.Current?.History.Records.FirstOrDefault(r => r.TransferId == offer.TransferId);
        if (histRecord != null) existingDestinationPath = histRecord.DestinationPath;
        else 
        {
            var queueRecord = _transfers.Queue.FirstOrDefault(r => r.TransferId == offer.TransferId);
            if (queueRecord != null) existingDestinationPath = queueRecord.DestinationPath;
        }

        if (!string.IsNullOrEmpty(existingDestinationPath) && File.Exists(existingDestinationPath))
        {
            isResume = true;
            requestedOffset = new FileInfo(existingDestinationPath).Length;
            Debug.WriteLine($"Resuming incoming transfer {offer.TransferId} at offset {requestedOffset}");
        }

        bool accepted = false;
        if (isResume || (device.TrustState == DeviceTrustState.Trusted && CrossDroidBackend.Current?.Settings.Current.AutoAcceptTrusted == true))
        {
            accepted = true;
        }
        else
        {
            if (!_activePrompts.Add(device.DeviceId))
            {
                Debug.WriteLine($"Prompt already active for {device.DeviceId}. Rejecting new transfer offer.");
                await session.WriteMessageAsync(new ProtocolMessage { Type = MessageType.TransferCancel }, ReadOnlyMemory<byte>.Empty, CancellationToken.None);
                return;
            }

            try
            {
                var mainWindow = CrossDroid.Windows.App.MainWindowInstance;
                if (mainWindow != null)
                {
                    accepted = await mainWindow.ShowIncomingTransferPromptAsync(device, offer);
                }

                if (accepted)
                {
                    device.RejectedCount = 0;
                }
                else
                {
                    device.RejectedCount++;
                    if (device.RejectedCount >= 3)
                    {
                        device.IsBlocked = true;
                        _ = _devices.SaveDevicesAsync();
                    }
                }
            }
            finally
            {
                _activePrompts.Remove(device.DeviceId);
            }
        }

        if (accepted)
        {
            var acceptMsg = new ProtocolMessage
            {
                Type = MessageType.TransferAccept,
                PayloadJson = JsonSerializer.Serialize(new TransferAcceptPayload
                {
                    TransferId = offer.TransferId,
                    Accepted = true,
                    RequestedOffset = requestedOffset
                })
            };
            await session.WriteMessageAsync(acceptMsg, ReadOnlyMemory<byte>.Empty, token);

            // Hand off to TransferQueueService
            await _transfers.ReceiveNetworkTransferAsync(session, offer, existingDestinationPath);
        }
        else
        {
            var rejectMsg = new ProtocolMessage
            {
                Type = MessageType.TransferReject,
                PayloadJson = JsonSerializer.Serialize(new TransferAcceptPayload
                {
                    TransferId = offer.TransferId,
                    Accepted = false
                })
            };
            await session.WriteMessageAsync(rejectMsg, ReadOnlyMemory<byte>.Empty, token);
        }
    }
}
