using System;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Net.NetworkInformation;
using System.Threading;
using System.Threading.Tasks;
using Makaretu.Dns;
using System.Linq;

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
    private readonly IdentityService _identity;
    private readonly DeviceService _devices;
    private readonly SettingsService _settings;
    private MulticastService? _mdns;
    private ServiceDiscovery? _sd;
    private ServiceProfile? _profile;

    public DiscoveryService(IdentityService identity, DeviceService devices, SettingsService settings)
    {
        _identity = identity;
        _devices = devices;
        _settings = settings;
    }

    public void Start(int tcpPort)
    {
        if (_mdns != null) return;

        try
        {
            _mdns = new MulticastService();
            _mdns.Start();

            _sd = new ServiceDiscovery(_mdns);
            
            _profile = new ServiceProfile(_identity.LocalDevice.DeviceId, "_crossdroid._tcp", (ushort)tcpPort);
            _profile.AddProperty("DeviceId", _identity.LocalDevice.DeviceId);
            _profile.AddProperty("DisplayName", _identity.LocalDevice.DisplayName);
            _profile.AddProperty("DeviceType", _identity.LocalDevice.DeviceType);
            _profile.AddProperty("Fingerprint", _identity.LocalDevice.PublicFingerprint);
            
            _sd.ServiceDiscovered += OnServiceDiscovered;
            _sd.ServiceInstanceDiscovered += OnServiceInstanceDiscovered;
            
            if (_settings.Current.Discoverable)
            {
                _sd.Advertise(_profile);
            }
            
            // Query for existing services
            _mdns.SendQuery("_crossdroid._tcp.local");
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Failed to start mDNS discovery: {ex.Message}");
        }
    }

    private void OnServiceDiscovered(object? sender, DomainName e)
    {
        if (e.ToString().Contains("_crossdroid._tcp"))
        {
            _mdns?.SendQuery(e);
        }
    }

    private void OnServiceInstanceDiscovered(object? sender, ServiceInstanceDiscoveryEventArgs e)
    {
        try
        {
            var msg = new DiscoveryMessage
            {
                DeviceId = GetProperty(e.Message, "DeviceId"),
                DisplayName = GetProperty(e.Message, "DisplayName"),
                DeviceType = GetProperty(e.Message, "DeviceType"),
                Fingerprint = GetProperty(e.Message, "Fingerprint")
            };

            if (msg.DeviceId == _identity.LocalDevice.DeviceId) return;
            if (string.IsNullOrEmpty(msg.DeviceId)) return;

            var incomingPin = GetProperty(e.Message, "Pin");
            if (!string.IsNullOrEmpty(incomingPin))
            {
                var pairing = CrossDroidBackend.Current?.Pairing;
                if (pairing != null && pairing.IsPinValid)
                {
                    if (pairing.CurrentPin == incomingPin)
                    {
                        // Valid PIN search: Temporarily broadcast ourselves if we are hidden
                        if (!_settings.Current.Discoverable && _profile != null)
                        {
                            _sd?.Advertise(_profile);
                            Task.Run(async () => {
                                await Task.Delay(30000);
                                if (!_settings.Current.Discoverable)
                                    _sd?.Unadvertise(_profile);
                            });
                        }
                    }
                    else
                    {
                        // Wrong PIN attempt
                        pairing.FailedAttempts++;
                        if (pairing.FailedAttempts >= 5)
                        {
                            pairing.InvalidatePin();
                            Debug.WriteLine("Pairing PIN invalidated due to excessive failed attempts.");
                        }
                    }
                }
            }

            var srv = e.Message.AdditionalRecords.OfType<SRVRecord>().FirstOrDefault() ?? e.Message.Answers.OfType<SRVRecord>().FirstOrDefault();
            var a = e.Message.AdditionalRecords.OfType<ARecord>().FirstOrDefault() ?? e.Message.Answers.OfType<ARecord>().FirstOrDefault();

            if (srv != null && a != null)
            {
                msg.TcpPort = srv.Port;
                HandleDiscovery(msg, a.Address.ToString());
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Error parsing mDNS record: {ex.Message}");
        }
    }

    private string GetProperty(Makaretu.Dns.Message msg, string key)
    {
        var txt = msg.AdditionalRecords.OfType<TXTRecord>().FirstOrDefault() ?? msg.Answers.OfType<TXTRecord>().FirstOrDefault();
        if (txt != null)
        {
            var prefix = key + "=";
            var str = txt.Strings.FirstOrDefault(s => s.StartsWith(prefix));
            if (str != null)
            {
                return str.Substring(prefix.Length);
            }
        }
        return "";
    }

    public void Stop()
    {
        if (_sd != null)
        {
            if (_profile != null) _sd.Unadvertise(_profile);
            _sd.Dispose();
            _sd = null;
        }
        if (_mdns != null)
        {
            _mdns.Stop();
            _mdns.Dispose();
            _mdns = null;
        }
    }

    public void Dispose()
    {
        Stop();
    }

    public async Task BroadcastPinSearchAsync(string pin)
    {
        if (_profile != null && _sd != null)
        {
            _sd.Unadvertise(_profile);
            
            var pinProfile = new ServiceProfile(_identity.LocalDevice.DeviceId, "_crossdroid._tcp", (ushort)_profile.Resources.OfType<SRVRecord>().First().Port);
            pinProfile.AddProperty("DeviceId", _identity.LocalDevice.DeviceId);
            pinProfile.AddProperty("DisplayName", _identity.LocalDevice.DisplayName);
            pinProfile.AddProperty("DeviceType", _identity.LocalDevice.DeviceType);
            pinProfile.AddProperty("Fingerprint", _identity.LocalDevice.PublicFingerprint);
            pinProfile.AddProperty("Pin", pin);
            
            _sd.Advertise(pinProfile);
            await Task.Delay(5000);
            _sd.Unadvertise(pinProfile);
            
            if (_settings.Current.Discoverable)
            {
                _sd.Advertise(_profile);
            }
        }
    }

    private void HandleDiscovery(DiscoveryMessage msg, string ipAddress)
    {
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
