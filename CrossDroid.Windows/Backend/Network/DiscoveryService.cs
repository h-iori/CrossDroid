using System;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Net.NetworkInformation;
using System.Threading;
using System.Threading.Tasks;

namespace CrossDroid.Windows.Backend.Network;

public class DiscoveryMessage
{
    public string Action { get; set; } = "Announce";
    public string DeviceId { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string DeviceType { get; set; } = "";
    public string Fingerprint { get; set; } = "";
    public int TcpPort { get; set; }
    public string Pin { get; set; } = "";
}

public sealed class DiscoveryService : IDisposable
{
    private const int DiscoveryPort = 53100;
    private readonly IdentityService _identity;
    private readonly DeviceService _devices;
    private readonly SettingsService _settings;
    private UdpClient? _listener;
    private CancellationTokenSource? _cts;
    private Task? _listenTask;
    private Task? _broadcastTask;

    public DiscoveryService(IdentityService identity, DeviceService devices, SettingsService settings)
    {
        _identity = identity;
        _devices = devices;
        _settings = settings;
    }

    public void Start(int tcpPort)
    {
        if (_cts != null) return;
        _cts = new CancellationTokenSource();

        try
        {
            _listener = new UdpClient(new IPEndPoint(IPAddress.Any, DiscoveryPort));
            _listener.EnableBroadcast = true;
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Failed to bind discovery UDP port {DiscoveryPort}: {ex.Message}");
            return;
        }

        _listenTask = Task.Run(() => ListenLoopAsync(_cts.Token), _cts.Token);
        _broadcastTask = Task.Run(() => BroadcastLoopAsync(tcpPort, _cts.Token), _cts.Token);
    }

    public void Stop()
    {
        _cts?.Cancel();
        _listener?.Close();
        _listener?.Dispose();
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
                var result = await _listener.ReceiveAsync(token);
                var json = Encoding.UTF8.GetString(result.Buffer);
                var msg = JsonSerializer.Deserialize<DiscoveryMessage>(json);

                if (msg != null && msg.DeviceId != _identity.LocalDevice.DeviceId)
                {
                    if (msg.Action == "Announce")
                    {
                        HandleDiscovery(msg, result.RemoteEndPoint.Address.ToString());
                    }
                    else if (msg.Action == "PinSearch" && !string.IsNullOrEmpty(msg.Pin))
                    {
                        var currentPin = CrossDroidBackend.Current?.Pairing?.CurrentPin;
                        if (currentPin == msg.Pin)
                        {
                            // A device is searching for us using the correct PIN.
                            // Respond directly with an Announce packet so they can discover us,
                            // even if we are normally "Hidden".
                            var responseMsg = new DiscoveryMessage
                            {
                                Action = "Announce",
                                DeviceId = _identity.LocalDevice.DeviceId,
                                DisplayName = _identity.LocalDevice.DisplayName,
                                DeviceType = _identity.LocalDevice.DeviceType,
                                Fingerprint = _identity.LocalDevice.PublicFingerprint,
                                TcpPort = CrossDroidBackend.Current?.Listener?.Port ?? 53100
                            };
                            
                            var responseJson = JsonSerializer.Serialize(responseMsg);
                            var responseBytes = Encoding.UTF8.GetBytes(responseJson);
                            
                            using var responder = new UdpClient();
                            await responder.SendAsync(responseBytes, responseBytes.Length, result.RemoteEndPoint);
                        }
                    }
                }
            }
            catch (OperationCanceledException) { break; }
            catch (Exception ex)
            {
                Debug.WriteLine($"Discovery listen error: {ex.Message}");
                await Task.Delay(1000, token); // Backoff on error
            }
        }
    }

    private async Task BroadcastLoopAsync(int tcpPort, CancellationToken token)
    {
        using var broadcaster = new UdpClient();
        broadcaster.EnableBroadcast = true;

        while (!token.IsCancellationRequested)
        {
            try
            {
                if (_settings.Current.Discoverable && NetworkInterface.GetIsNetworkAvailable())
                {
                    var msg = new DiscoveryMessage
                    {
                        Action = "Announce",
                        DeviceId = _identity.LocalDevice.DeviceId,
                        DisplayName = _identity.LocalDevice.DisplayName,
                        DeviceType = _identity.LocalDevice.DeviceType,
                        Fingerprint = _identity.LocalDevice.PublicFingerprint,
                        TcpPort = tcpPort
                    };

                    var json = JsonSerializer.Serialize(msg);
                    var bytes = Encoding.UTF8.GetBytes(json);

                    foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
                    {
                        if (ni.OperationalStatus == OperationalStatus.Up && 
                            ni.NetworkInterfaceType != NetworkInterfaceType.Loopback)
                        {
                            foreach (var ip in ni.GetIPProperties().UnicastAddresses)
                            {
                                if (ip.Address.AddressFamily == AddressFamily.InterNetwork)
                                {
                                    var broadcast = GetBroadcastAddress(ip.Address, ip.IPv4Mask);
                                    if (broadcast != null)
                                    {
                                        try {
                                            await broadcaster.SendAsync(bytes, bytes.Length, new IPEndPoint(broadcast, DiscoveryPort));
                                        } catch { /* Ignore per-interface errors */ }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Discovery broadcast error: {ex.Message}");
            }

            await Task.Delay(TimeSpan.FromSeconds(3), token);
        }
    }

    private static IPAddress? GetBroadcastAddress(IPAddress address, IPAddress mask)
    {
        if (mask == null) return null;
        var ipAddressBytes = address.GetAddressBytes();
        var subnetMaskBytes = mask.GetAddressBytes();

        if (ipAddressBytes.Length != subnetMaskBytes.Length)
            return null;

        var broadcastAddress = new byte[ipAddressBytes.Length];
        for (int i = 0; i < broadcastAddress.Length; i++)
        {
            broadcastAddress[i] = (byte)(ipAddressBytes[i] | (subnetMaskBytes[i] ^ 255));
        }
        return new IPAddress(broadcastAddress);
    }

    public async Task BroadcastPinSearchAsync(string pin)
    {
        if (!NetworkInterface.GetIsNetworkAvailable()) return;

        var msg = new DiscoveryMessage
        {
            Action = "PinSearch",
            DeviceId = _identity.LocalDevice.DeviceId,
            DisplayName = _identity.LocalDevice.DisplayName,
            DeviceType = _identity.LocalDevice.DeviceType,
            Pin = pin,
            TcpPort = CrossDroidBackend.Current?.Listener?.Port ?? 53100
        };

        var json = JsonSerializer.Serialize(msg);
        var bytes = Encoding.UTF8.GetBytes(json);

        using var broadcaster = new UdpClient();
        broadcaster.EnableBroadcast = true;

        foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (ni.OperationalStatus == OperationalStatus.Up && 
                ni.NetworkInterfaceType != NetworkInterfaceType.Loopback)
            {
                foreach (var ip in ni.GetIPProperties().UnicastAddresses)
                {
                    if (ip.Address.AddressFamily == AddressFamily.InterNetwork)
                    {
                        var broadcast = GetBroadcastAddress(ip.Address, ip.IPv4Mask);
                        if (broadcast != null)
                        {
                            try {
                                await broadcaster.SendAsync(bytes, bytes.Length, new IPEndPoint(broadcast, DiscoveryPort));
                            } catch { /* Ignore */ }
                        }
                    }
                }
            }
        }
    }

    private void HandleDiscovery(DiscoveryMessage msg, string ipAddress)
    {
        // Must marshal to UI thread if we update ObservableCollections
        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
        dispatcher?.TryEnqueue(() =>
        {
            UpdateDevice(msg, ipAddress);
        });
    }

    private void UpdateDevice(DiscoveryMessage msg, string ipAddress)
    {
        var endpoint = $"{ipAddress}:{msg.TcpPort}";
        _devices.UpdateFromDiscovery(msg.DeviceId, msg.DisplayName, msg.DeviceType, msg.Fingerprint, endpoint);
    }
}
