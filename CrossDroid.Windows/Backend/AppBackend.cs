using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Diagnostics;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Windows.ApplicationModel;
using Windows.Storage;
using Windows.Storage.Pickers;
using Windows.System;

namespace CrossDroid.Windows.Backend;

public sealed class CrossDroidBackend
{
    private CrossDroidBackend(PersistentAppState state, string statePath)
    {
        StateStore = new AppStateStore(state, statePath);
        Settings = new SettingsService(StateStore);
        Identity = new IdentityService(StateStore);
        Devices = new DeviceService(StateStore);
        History = new HistoryService(StateStore);
        Staging = new TransferStagingService();
        Notifications = new NotificationService(Settings);
        SystemControl = new SystemControlService();
        Transfers = new TransferQueueService(Settings, Devices, History, Notifications);
        Health = new HealthService(Settings, Identity, Devices, Transfers);
        Discovery = new Network.DiscoveryService(Identity, Devices, Settings);
        Listener = new Network.ConnectionListener(Identity, Devices, Transfers);
        Pairing = new Security.PairingManager(Identity);
        Shell = new ShellIntegrationService(Settings);
    }

    public static CrossDroidBackend Current { get; private set; } = null!;

    public AppStateStore StateStore { get; }
    public SettingsService Settings { get; }
    public IdentityService Identity { get; }
    public DeviceService Devices { get; }
    public HistoryService History { get; }
    public TransferStagingService Staging { get; }
    public TransferQueueService Transfers { get; }
    public NotificationService Notifications { get; }
    public SystemControlService SystemControl { get; }
    public HealthService Health { get; }
    public Network.DiscoveryService Discovery { get; }
    public Network.ConnectionListener Listener { get; }
    public Security.PairingManager Pairing { get; }
    public ShellIntegrationService Shell { get; }

    public static async Task InitializeAsync()
    {
        var dataRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "IoriStudios",
            "CrossDroid");
        Directory.CreateDirectory(dataRoot);

        var statePath = Path.Combine(dataRoot, "crossdroid-state.json");
        var state = await AppStateStore.LoadAsync(statePath);
        var backend = new CrossDroidBackend(state, statePath);
        backend.Identity.EnsureLocalIdentity();
        backend.History.LoadFromState();
        backend.Devices.LoadFromState();
        
        backend.Listener.Start();
        backend.Discovery.Start(backend.Listener.Port);
        
        await backend.Shell.EnsureAutoStartAsync(backend.Settings.Current.AutoStartEnabled);
        backend.Shell.EnsureContextMenu(backend.Settings.Current.EnableContextMenu);
        
        Current = backend;
        await backend.StateStore.SaveAsync();
    }
}

public sealed class AppStateStore
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
        Converters = { new JsonStringEnumConverter() }
    };

    private readonly SemaphoreSlim _saveLock = new(1, 1);

    public AppStateStore(PersistentAppState state, string statePath)
    {
        State = state;
        StatePath = statePath;
    }

    public PersistentAppState State { get; }
    public string StatePath { get; }

    public static async Task<PersistentAppState> LoadAsync(string statePath)
    {
        if (!File.Exists(statePath))
        {
            return PersistentAppState.CreateDefault();
        }

        try
        {
            await using var stream = File.OpenRead(statePath);
            return await JsonSerializer.DeserializeAsync<PersistentAppState>(stream, JsonOptions)
                ?? PersistentAppState.CreateDefault();
        }
        catch
        {
            return PersistentAppState.CreateDefault();
        }
    }

    public async Task SaveAsync()
    {
        await _saveLock.WaitAsync();
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(StatePath)!);
            var tempPath = StatePath + ".tmp";
            await using (var stream = File.Create(tempPath))
            {
                await JsonSerializer.SerializeAsync(stream, State, JsonOptions);
            }

            File.Move(tempPath, StatePath, true);
        }
        finally
        {
            _saveLock.Release();
        }
    }

    public void SaveSoon()
    {
        _ = SaveAsync();
    }
}

public sealed class PersistentAppState
{
    public AppSettings Settings { get; set; } = new();
    public LocalDeviceIdentity? LocalIdentity { get; set; }
    public List<DeviceRecord> Devices { get; set; } = new();
    public List<TransferHistoryRecord> History { get; set; } = new();
    public List<AuditEvent> Audit { get; set; } = new();

    public static PersistentAppState CreateDefault()
    {
        return new PersistentAppState
        {
            Settings = AppSettings.CreateDefault()
        };
    }
}

public sealed class AppSettings : NotifyBase
{
    private bool _autoStartEnabled;
    private bool _startMinimized;
    private bool _closeToTray = true;
    private bool _autoAcceptTrusted = true;
    private bool _wifiOnly = true;
    private bool _p2pFallback = true;
    private bool _toastNotify = true;
    private bool _soundNotify = true;
    private bool _discoverable = true;
    private string _downloadsDirectory = "";
    private bool _enableContextMenu = true;
    private int _preferredNetworkBand = 2; // 0=5GHz, 1=2.4GHz, 2=Auto

    public bool AutoStartEnabled { get => _autoStartEnabled; set => SetField(ref _autoStartEnabled, value); }
    public bool StartMinimized { get => _startMinimized; set => SetField(ref _startMinimized, value); }
    public bool CloseToTray { get => _closeToTray; set => SetField(ref _closeToTray, value); }
    public bool AutoAcceptTrusted { get => _autoAcceptTrusted; set => SetField(ref _autoAcceptTrusted, value); }
    public bool WifiOnly { get => _wifiOnly; set => SetField(ref _wifiOnly, value); }
    public bool P2pFallback { get => _p2pFallback; set => SetField(ref _p2pFallback, value); }
    public bool ToastNotify { get => _toastNotify; set => SetField(ref _toastNotify, value); }
    public bool SoundNotify { get => _soundNotify; set => SetField(ref _soundNotify, value); }
    public bool Discoverable { get => _discoverable; set => SetField(ref _discoverable, value); }
    public string DownloadsDirectory { get => _downloadsDirectory; set => SetField(ref _downloadsDirectory, value); }
    public bool EnableContextMenu { get => _enableContextMenu; set => SetField(ref _enableContextMenu, value); }
    public int PreferredNetworkBand { get => _preferredNetworkBand; set => SetField(ref _preferredNetworkBand, value); }
    public int RejectedCount { get; set; } = 0;

    public static AppSettings CreateDefault()
    {
        return new AppSettings
        {
            DownloadsDirectory = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                "Downloads",
                "CrossDroid")
        };
    }
}

public sealed class SettingsService
{
    private readonly AppStateStore _store;

    public SettingsService(AppStateStore store)
    {
        _store = store;
        Current = store.State.Settings;
        if (string.IsNullOrWhiteSpace(Current.DownloadsDirectory))
        {
            Current.DownloadsDirectory = AppSettings.CreateDefault().DownloadsDirectory;
        }

        Current.PropertyChanged += (_, _) => _store.SaveSoon();
    }

    public AppSettings Current { get; }

    public async Task SetDownloadsDirectoryAsync(string path)
    {
        Directory.CreateDirectory(path);
        Current.DownloadsDirectory = path;
        await _store.SaveAsync();
    }
}

public sealed class IdentityService
{
    private readonly AppStateStore _store;

    public IdentityService(AppStateStore store)
    {
        _store = store;
    }

    public LocalDeviceIdentity LocalDevice => _store.State.LocalIdentity
        ?? throw new InvalidOperationException("Local identity has not been initialized.");

    public X509Certificate2 GetCertificate()
    {
        var pfxBytes = UnprotectForCurrentUser(Convert.FromBase64String(_store.State.LocalIdentity!.ProtectedPrivateKey));
        return X509CertificateLoader.LoadPkcs12(pfxBytes, (string)null!, X509KeyStorageFlags.Exportable);
    }

    public void EnsureLocalIdentity()
    {
        if (_store.State.LocalIdentity != null)
        {
            return;
        }

        using var rsa = RSA.Create(2048);
        var req = new CertificateRequest("CN=CrossDroid", rsa, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
        using var cert = req.CreateSelfSigned(DateTimeOffset.UtcNow.AddDays(-1), DateTimeOffset.UtcNow.AddYears(10));
        
        var pfxBytes = cert.Export(X509ContentType.Pfx);
        var publicFingerprint = cert.GetCertHashString();

        _store.State.LocalIdentity = new LocalDeviceIdentity
        {
            DeviceId = Guid.NewGuid().ToString("N"),
            DisplayName = Environment.MachineName,
            DeviceType = "Windows PC",
            CreatedUtc = DateTimeOffset.UtcNow,
            PublicFingerprint = publicFingerprint,
            ProtectedPrivateKey = Convert.ToBase64String(ProtectForCurrentUser(pfxBytes))
        };
        _store.State.Audit.Add(AuditEvent.Create("IdentityCreated", "Generated protected local CrossDroid device identity certificate."));
    }

    private static byte[] ProtectForCurrentUser(byte[] data)
    {
        var input = new NativeBlob(data);
        var output = new DataBlob();
        try
        {
            if (!CryptProtectData(ref input.Blob, "CrossDroid local identity", IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, 0, ref output))
            {
                throw new CryptographicException(Marshal.GetLastWin32Error());
            }

            var protectedData = new byte[output.cbData];
            Marshal.Copy(output.pbData, protectedData, 0, protectedData.Length);
            return protectedData;
        }
        finally
        {
            input.Dispose();
            if (output.pbData != IntPtr.Zero)
            {
                LocalFree(output.pbData);
            }
        }
    }

    private static byte[] UnprotectForCurrentUser(byte[] data)
    {
        var input = new NativeBlob(data);
        var output = new DataBlob();
        try
        {
            if (!CryptUnprotectData(ref input.Blob, out _, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, 0, ref output))
            {
                throw new CryptographicException(Marshal.GetLastWin32Error());
            }

            var unprotectedData = new byte[output.cbData];
            Marshal.Copy(output.pbData, unprotectedData, 0, unprotectedData.Length);
            return unprotectedData;
        }
        finally
        {
            input.Dispose();
            if (output.pbData != IntPtr.Zero)
            {
                LocalFree(output.pbData);
            }
        }
    }

    [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CryptProtectData(
        ref DataBlob pDataIn,
        string? szDataDescr,
        IntPtr pOptionalEntropy,
        IntPtr pvReserved,
        IntPtr pPromptStruct,
        int dwFlags,
        ref DataBlob pDataOut);

    [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CryptUnprotectData(
        ref DataBlob pDataIn,
        out string ppszDataDescr,
        IntPtr pOptionalEntropy,
        IntPtr pvReserved,
        IntPtr pPromptStruct,
        int dwFlags,
        ref DataBlob pDataOut);

    [DllImport("kernel32.dll")]
    private static extern IntPtr LocalFree(IntPtr hMem);

    [StructLayout(LayoutKind.Sequential)]
    private struct DataBlob
    {
        public int cbData;
        public IntPtr pbData;
    }

    private sealed class NativeBlob : IDisposable
    {
        public DataBlob Blob;

        public NativeBlob(byte[] data)
        {
            Blob.cbData = data.Length;
            Blob.pbData = Marshal.AllocHGlobal(data.Length);
            Marshal.Copy(data, 0, Blob.pbData, data.Length);
        }

        public void Dispose()
        {
            if (Blob.pbData != IntPtr.Zero)
            {
                Marshal.FreeHGlobal(Blob.pbData);
                Blob.pbData = IntPtr.Zero;
            }
        }
    }
}

public sealed class DeviceService
{
    private readonly AppStateStore _store;

    public DeviceService(AppStateStore store)
    {
        _store = store;
    }

    public ObservableCollection<DeviceRecord> Devices { get; } = new();
    public IEnumerable<DeviceRecord> TrustedDevices => Devices.Where(d => d.TrustState == DeviceTrustState.Trusted && !d.IsBlocked);

    public Task SaveDevicesAsync() => _store.SaveAsync();

    public void LoadFromState()
    {
        Devices.Clear();
        foreach (var device in _store.State.Devices.OrderByDescending(d => d.LastSeenUtc))
        {
            device.PropertyChanged += DeviceChanged;
            Devices.Add(device);
        }
    }


    public async Task RenameAsync(DeviceRecord device, string alias)
    {
        device.Alias = alias.Trim();
        await _store.SaveAsync();
    }

    public async Task SetBlockedAsync(DeviceRecord device, bool blocked)
    {
        device.IsBlocked = blocked;
        device.TrustState = blocked ? DeviceTrustState.Blocked : DeviceTrustState.Trusted;
        device.Presence = blocked ? DevicePresence.Blocked : DevicePresence.Online;
        _store.State.Audit.Add(AuditEvent.Create(blocked ? "DeviceBlocked" : "DeviceUnblocked", device.DisplayName));
        await _store.SaveAsync();
    }

    public async Task RemoveAsync(DeviceRecord device)
    {
        Devices.Remove(device);
        _store.State.Devices.RemoveAll(d => d.DeviceId == device.DeviceId);
        _store.State.Audit.Add(AuditEvent.Create("DeviceRemoved", device.DisplayName));
        await _store.SaveAsync();
    }

    public async Task RefreshPresenceAsync()
    {
        foreach (var device in Devices)
        {
            if (device.IsBlocked)
            {
                device.Presence = DevicePresence.Blocked;
                device.ConnectionStatus = ConnectionStatus.Blocked;
            }
            else if (device.LastSeenUtc < DateTimeOffset.UtcNow.AddMinutes(-5))
            {
                device.Presence = DevicePresence.Offline;
                device.ConnectionStatus = ConnectionStatus.Offline;
            }
        }

        await _store.SaveAsync();
    }

    public void UpdateFromDiscovery(string deviceId, string displayName, string deviceType, string fingerprint, string endpoint)
    {
        var device = _store.State.Devices.FirstOrDefault(d => d.DeviceId == deviceId);
        if (device == null)
        {
            device = new DeviceRecord
            {
                DeviceId = deviceId,
                DisplayName = displayName,
                DeviceType = deviceType,
                Fingerprint = fingerprint,
                Endpoint = endpoint,
                TrustState = DeviceTrustState.Unknown,
                Presence = DevicePresence.Online,
                ConnectionStatus = ConnectionStatus.Ready,
                EncryptionStatus = EncryptionStatus.Unknown,
                LastSeenUtc = DateTimeOffset.UtcNow
            };
            device.PropertyChanged += DeviceChanged;
            _store.State.Devices.Add(device);
            Devices.Add(device);
        }
        else
        {
            if (!device.IsBlocked)
            {
                device.DisplayName = displayName;
                device.DeviceType = deviceType;
                device.Fingerprint = fingerprint;
                device.Endpoint = endpoint;
                device.Presence = DevicePresence.Online;
                device.ConnectionStatus = ConnectionStatus.Ready;
                device.LastSeenUtc = DateTimeOffset.UtcNow;
            }
        }
        
        _store.SaveSoon();
    }

    private void DeviceChanged(object? sender, PropertyChangedEventArgs e)
    {
        _store.SaveSoon();
    }
}

public sealed class TransferStagingService
{
    public ObservableCollection<StagedTransferItem> Items { get; } = new();

    public async Task StageStorageItemsAsync(IEnumerable<IStorageItem> items)
    {
        foreach (var item in items)
        {
            await StagePathAsync(item.Path);
        }
    }

    public async Task StagePathAsync(string path)
    {
        if (File.Exists(path))
        {
            var info = new FileInfo(path);
            Items.Add(new StagedTransferItem
            {
                Name = info.Name,
                Path = info.FullName,
                IsFolder = false,
                Bytes = info.Length,
                ItemCount = 1
            });
        }
        else if (Directory.Exists(path))
        {
            var dir = new DirectoryInfo(path);
            var files = dir.EnumerateFiles("*", SearchOption.AllDirectories).ToList();
            Items.Add(new StagedTransferItem
            {
                Name = dir.Name,
                Path = dir.FullName,
                IsFolder = true,
                Bytes = files.Sum(f => f.Length),
                ItemCount = files.Count
            });
        }

        await Task.CompletedTask;
    }

    public void Remove(StagedTransferItem item)
    {
        Items.Remove(item);
    }

    public void Clear()
    {
        Items.Clear();
    }
}

public sealed class TransferQueueService
{
    private readonly SettingsService _settings;
    private readonly DeviceService _devices;
    private readonly HistoryService _history;
    private readonly NotificationService _notifications;
    private readonly Dictionary<string, TransferRuntime> _runtimes = new();

    public TransferQueueService(SettingsService settings, DeviceService devices, HistoryService history, NotificationService notifications)
    {
        _settings = settings;
        _devices = devices;
        _history = history;
        _notifications = notifications;
    }

    public ObservableCollection<TransferRecord> Queue { get; } = new();
    public ObservableCollection<IncomingTransferRequest> IncomingRequests { get; } = new();

    public async Task StartSendAsync(DeviceRecord target, IEnumerable<StagedTransferItem> stagedItems)
    {
        if (target.IsBlocked)
        {
            throw new InvalidOperationException("Cannot send files to a blocked device.");
        }

        foreach (var staged in stagedItems.ToList())
        {
            var record = new TransferRecord
            {
                TransferId = Guid.NewGuid().ToString("N"),
                Direction = TransferDirection.Outgoing,
                DeviceId = target.DeviceId,
                DeviceName = target.AliasOrName,
                FileName = staged.Name,
                SourcePath = staged.Path,
                DestinationPath = _settings.Current.DownloadsDirectory,
                IsFolder = staged.IsFolder,
                TotalBytes = Math.Max(staged.Bytes, 1),
                Status = TransferStatus.Queued,
                CreatedUtc = DateTimeOffset.UtcNow
            };
            Queue.Add(record);
            var runtime = new TransferRuntime();
            _runtimes[record.TransferId] = runtime;
            
            if (string.IsNullOrEmpty(target.Endpoint))
            {
                record.Status = TransferStatus.Failed;
                record.ErrorMessage = "Device has no network endpoint. Ensure the device is online and discoverable.";
                record.CompletedUtc = DateTimeOffset.UtcNow;
                await _history.RecordAsync(record);
                continue;
            }
            _ = RunNetworkTransferAsync(record, target, runtime);
        }

        await Task.CompletedTask;
    }

    public void Pause(string transferId)
    {
        if (_runtimes.TryGetValue(transferId, out var runtime))
        {
            runtime.IsPaused = true;
        }
    }

    public void Resume(string transferId)
    {
        if (_runtimes.TryGetValue(transferId, out var runtime))
        {
            runtime.IsPaused = false;
            runtime.PauseEvent.Set();
        }
    }

    public void Cancel(string transferId)
    {
        if (_runtimes.TryGetValue(transferId, out var runtime))
        {
            runtime.Cancellation.Cancel();
        }
    }

    public async Task RetryAsync(TransferRecord record)
    {
        if (record.Status is not (TransferStatus.Failed or TransferStatus.Cancelled))
        {
            return;
        }

        var device = _devices.Devices.FirstOrDefault(d => d.DeviceId == record.DeviceId);
        if (device == null || string.IsNullOrEmpty(device.Endpoint))
        {
            record.ErrorMessage = "Device not found or offline. Cannot retry.";
            return;
        }

        record.Status = TransferStatus.Queued;
        record.BytesTransferred = 0;
        record.ProgressPercent = 0;
        record.ErrorMessage = "";
        var runtime = new TransferRuntime();
        _runtimes[record.TransferId] = runtime;
        await RunNetworkTransferAsync(record, device, runtime);
    }

    public async Task CreateIncomingRequestAsync(DeviceRecord source, IReadOnlyList<StagedTransferItem> items)
    {
        var request = new IncomingTransferRequest
        {
            RequestId = Guid.NewGuid().ToString("N"),
            Device = source,
            Items = items.ToList(),
            CreatedUtc = DateTimeOffset.UtcNow
        };
        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
        if (dispatcher != null && !dispatcher.HasThreadAccess)
        {
            dispatcher.TryEnqueue(() => IncomingRequests.Add(request));
        }
        else
        {
            IncomingRequests.Add(request);
        }
        await _notifications.ShowAsync("Incoming CrossDroid transfer", $"{source.AliasOrName} wants to send {items.Count} item(s).");
    }

    public async Task AcceptIncomingAsync(IncomingTransferRequest request)
    {
        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
        if (dispatcher != null && !dispatcher.HasThreadAccess) dispatcher.TryEnqueue(() => IncomingRequests.Remove(request));
        else IncomingRequests.Remove(request);
        foreach (var item in request.Items)
        {
            var record = new TransferRecord
            {
                TransferId = Guid.NewGuid().ToString("N"),
                Direction = TransferDirection.Incoming,
                DeviceId = request.Device.DeviceId,
                DeviceName = request.Device.AliasOrName,
                FileName = item.Name,
                SourcePath = item.Path,
                DestinationPath = _settings.Current.DownloadsDirectory,
                IsFolder = item.IsFolder,
                TotalBytes = Math.Max(item.Bytes, 1),
                Status = TransferStatus.Queued,
                CreatedUtc = DateTimeOffset.UtcNow
            };
            Queue.Add(record);
            var runtime = new TransferRuntime();
            _runtimes[record.TransferId] = runtime;
            // Accepted incoming transfers still come via network through ReceiveNetworkTransferAsync
            // so this path is only used for local UI acceptance tracking
        }

        await Task.CompletedTask;
    }

    public void RejectIncoming(IncomingTransferRequest request)
    {
        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
        if (dispatcher != null && !dispatcher.HasThreadAccess) dispatcher.TryEnqueue(() => IncomingRequests.Remove(request));
        else IncomingRequests.Remove(request);
    }


    private async Task RunNetworkTransferAsync(TransferRecord record, DeviceRecord target, TransferRuntime runtime)
    {
        var stopwatch = Stopwatch.StartNew();
        try
        {
            record.Status = TransferStatus.Transferring;

            // Connect
            var parts = target.Endpoint.Split(':');
            var host = parts[0];
            var port = parts.Length > 1 ? int.Parse(parts[1]) : 53100;

            using var client = new TcpClient();
            await client.ConnectAsync(host, port, runtime.Cancellation.Token);

            using var session = new Network.SecureSession(client);
            await session.AuthenticateAsClientAsync(CrossDroidBackend.Current.Identity.GetCertificate(), target.Fingerprint, runtime.Cancellation.Token);

            // Compute file hash
            var hash = record.IsFolder ? "" : await ComputeHashAsync(record.SourcePath);
            record.SourceHash = hash;

            // Send Offer
            var offer = new Network.ProtocolMessage
            {
                Type = Network.MessageType.TransferOffer,
                PayloadJson = JsonSerializer.Serialize(new Network.TransferOfferPayload
                {
                    TransferId = record.TransferId,
                    FileName = record.FileName,
                    TotalBytes = record.TotalBytes,
                    IsFolder = record.IsFolder,
                    Hash = hash
                })
            };
            await session.WriteMessageAsync(offer, ReadOnlyMemory<byte>.Empty, runtime.Cancellation.Token);

            // Wait for TransferAccept
            var (responseMsg, binary, _) = await session.ReadMessageAsync(runtime.Cancellation.Token);
            if (binary != null) System.Buffers.ArrayPool<byte>.Shared.Return(binary);
            if (responseMsg.Type == Network.MessageType.TransferReject)
            {
                throw new OperationCanceledException("Transfer rejected by receiver.");
            }
            else if (responseMsg.Type != Network.MessageType.TransferAccept)
            {
                throw new InvalidDataException("Unexpected message received after transfer offer.");
            }

            var acceptPayload = JsonSerializer.Deserialize<Network.TransferAcceptPayload>(responseMsg.PayloadJson);
            if (acceptPayload == null || !acceptPayload.Accepted)
            {
                throw new OperationCanceledException("Transfer rejected by receiver.");
            }

            if (record.IsFolder)
            {
                var sourceDirInfo = new DirectoryInfo(record.SourcePath);
                var files = sourceDirInfo.EnumerateFiles("*", SearchOption.AllDirectories).ToList();
                var buffer = System.Buffers.ArrayPool<byte>.Shared.Rent(1024 * 1024 * 2);
                try
                {
                    foreach (var file in files)
                    {
                        var relative = Path.GetRelativePath(sourceDirInfo.FullName, file.FullName);
                        await using var input = File.Open(file.FullName, FileMode.Open, FileAccess.Read, FileShare.Read);
                        int read;
                        long offset = 0;
                        while ((read = await input.ReadAsync(buffer.AsMemory(0, 1024 * 1024 * 2), runtime.Cancellation.Token)) > 0)
                        {
                            while (runtime.IsPaused)
                            {
                                record.Status = TransferStatus.Paused;
                                runtime.PauseEvent.Reset();
                                runtime.PauseEvent.Wait(runtime.Cancellation.Token);
                                record.Status = TransferStatus.Transferring;
                            }

                            var chunkData = buffer.AsMemory(0, read);
                            var chunkMsg = new Network.ProtocolMessage
                            {
                                Type = Network.MessageType.FileChunk,
                                PayloadJson = JsonSerializer.Serialize(new Network.FileChunkPayload
                                {
                                    TransferId = record.TransferId,
                                    RelativePath = relative,
                                    Offset = offset
                                })
                            };

                            await session.WriteMessageAsync(chunkMsg, chunkData, runtime.Cancellation.Token);
                            offset += read;

                            record.BytesTransferred += read;
                            record.ProgressPercent = Math.Min(100, record.BytesTransferred * 100d / Math.Max(record.TotalBytes, 1));
                            var speed = runtime.CalculateSpeed(record.BytesTransferred);
                            if (speed >= 0) record.SpeedBytesPerSecond = speed;
                        }
                    }
                }
                finally
                {
                    System.Buffers.ArrayPool<byte>.Shared.Return(buffer);
                }
            }
            else
            {
                await using var input = File.Open(record.SourcePath, FileMode.Open, FileAccess.Read, FileShare.Read);
                var buffer = System.Buffers.ArrayPool<byte>.Shared.Rent(1024 * 1024 * 2);
                try
                {
                    int read;
                    long offset = 0;
                    while ((read = await input.ReadAsync(buffer.AsMemory(0, 1024 * 1024 * 2), runtime.Cancellation.Token)) > 0)
                    {
                        while (runtime.IsPaused)
                        {
                            record.Status = TransferStatus.Paused;
                            runtime.PauseEvent.Reset();
                            runtime.PauseEvent.Wait(runtime.Cancellation.Token);
                            record.Status = TransferStatus.Transferring;
                        }

                        var chunkData = buffer.AsMemory(0, read);
                        var chunkMsg = new Network.ProtocolMessage
                        {
                            Type = Network.MessageType.FileChunk,
                            PayloadJson = JsonSerializer.Serialize(new Network.FileChunkPayload
                            {
                                TransferId = record.TransferId,
                                Offset = offset
                            })
                        };

                        await session.WriteMessageAsync(chunkMsg, chunkData, runtime.Cancellation.Token);
                        offset += read;

                        record.BytesTransferred += read;
                        record.ProgressPercent = Math.Min(100, record.BytesTransferred * 100d / Math.Max(record.TotalBytes, 1));
                        var speed = runtime.CalculateSpeed(record.BytesTransferred);
                        if (speed >= 0) record.SpeedBytesPerSecond = speed;
                    }
                }
                finally
                {
                    System.Buffers.ArrayPool<byte>.Shared.Return(buffer);
                }
            }

            record.Status = TransferStatus.Completed;
            record.CompletedUtc = DateTimeOffset.UtcNow;
            record.SpeedBytesPerSecond = record.BytesTransferred / Math.Max(stopwatch.Elapsed.TotalSeconds, 1);
            await _history.RecordAsync(record);
            await _notifications.ShowAsync("CrossDroid network transfer complete", $"{record.FileName} sent to {record.DeviceName}.");
        }
        catch (OperationCanceledException)
        {
            record.Status = TransferStatus.Cancelled;
            record.CompletedUtc = DateTimeOffset.UtcNow;
            await _history.RecordAsync(record);
        }
        catch (Exception ex)
        {
            record.Status = TransferStatus.Failed;
            record.CompletedUtc = DateTimeOffset.UtcNow;
            record.ErrorMessage = ex.Message;
            await _history.RecordAsync(record);
            await _notifications.ShowAsync("CrossDroid network transfer failed", $"{record.FileName}: {ex.Message}");
        }
        finally
        {
            _runtimes.Remove(record.TransferId);
        }
    }

    public async Task ReceiveNetworkTransferAsync(Network.SecureSession session, Network.TransferOfferPayload offer)
    {
        var remoteFingerprint = session.RemoteFingerprint;
        var device = _devices.Devices.FirstOrDefault(d => d.Fingerprint == remoteFingerprint);
        var deviceId = device?.DeviceId ?? $"Peer-{remoteFingerprint[..Math.Min(8, remoteFingerprint.Length)]}";
        var deviceName = device?.AliasOrName ?? $"Device ({remoteFingerprint[..Math.Min(6, remoteFingerprint.Length)]})";

        var record = new TransferRecord
        {
            TransferId = offer.TransferId,
            Direction = TransferDirection.Incoming,
            DeviceId = deviceId,
            DeviceName = deviceName,
            FileName = offer.FileName,
            DestinationPath = _settings.Current.DownloadsDirectory,
            IsFolder = offer.IsFolder,
            TotalBytes = offer.TotalBytes,
            Status = TransferStatus.Queued,
            CreatedUtc = DateTimeOffset.UtcNow
        };

        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
        dispatcher?.TryEnqueue(() => Queue.Add(record));

        var stopwatch = Stopwatch.StartNew();
        try
        {
            record.Status = TransferStatus.Transferring;
            Directory.CreateDirectory(_settings.Current.DownloadsDirectory);
            var destination = ResolveConflictPath(Path.Combine(_settings.Current.DownloadsDirectory, record.FileName));
            var baseDestDir = Path.GetFullPath(record.IsFolder ? destination : Path.GetDirectoryName(destination)!);

            string? currentStreamPath = null;
            FileStream? currentStream = null;

            try
            {
                if (record.IsFolder)
                {
                    Directory.CreateDirectory(baseDestDir);
                }
                else if (record.TotalBytes == 0)
                {
                    File.Create(destination).Dispose();
                }

                while (record.BytesTransferred < record.TotalBytes)
                {
                    var (msg, binary, binLen) = await session.ReadMessageAsync(CancellationToken.None);
                    try
                    {
                        if (msg.Type == Network.MessageType.FileChunk && binary != null)
                        {
                            var chunkPayload = JsonSerializer.Deserialize<Network.FileChunkPayload>(msg.PayloadJson);
                            var relative = chunkPayload?.RelativePath ?? "";
                            
                            string fileDest;
                            if (!record.IsFolder || string.IsNullOrEmpty(relative))
                            {
                                fileDest = Path.GetFullPath(destination);
                            }
                            else
                            {
                                // Path traversal protection: strip illegal characters and resolve path safely
                                var safeRelative = relative.Replace("..", "").Replace(":", "");
                                fileDest = Path.GetFullPath(Path.Combine(baseDestDir, safeRelative));
                                
                                // Ensure the resulting path is strictly inside the base destination directory
                                if (!fileDest.StartsWith(baseDestDir + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase) && 
                                    !fileDest.Equals(baseDestDir, StringComparison.OrdinalIgnoreCase))
                                {
                                    throw new InvalidOperationException("Path traversal attempt detected and blocked.");
                                }
                            }

                            Directory.CreateDirectory(Path.GetDirectoryName(fileDest)!);

                            if (currentStreamPath != fileDest)
                            {
                                currentStream?.Dispose();
                                // Use CreateNew if offset is 0 to avoid overwriting existing local files maliciously, else OpenOrCreate
                                var mode = (chunkPayload?.Offset ?? 0) == 0 ? FileMode.CreateNew : FileMode.OpenOrCreate;
                                currentStream = new FileStream(fileDest, mode, FileAccess.Write, FileShare.None, 4096, FileOptions.Asynchronous);
                                currentStreamPath = fileDest;
                            }

                            if (currentStream!.Position != (chunkPayload?.Offset ?? 0))
                            {
                                currentStream.Seek(chunkPayload?.Offset ?? 0, SeekOrigin.Begin);
                            }
                            
                            await currentStream.WriteAsync(binary!.AsMemory(0, binLen));

                            record.BytesTransferred += binLen;
                            record.ProgressPercent = Math.Min(100, record.BytesTransferred * 100d / Math.Max(record.TotalBytes, 1));
                        }
                        else if (msg.Type == Network.MessageType.TransferCancel)
                        {
                            throw new OperationCanceledException("Transfer cancelled by sender.");
                        }
                    }
                    finally
                    {
                        if (binary != null)
                        {
                            System.Buffers.ArrayPool<byte>.Shared.Return(binary);
                        }
                    }
                }
            }
            finally
            {
                currentStream?.Dispose();
            }

            record.DestinationPath = destination;
            record.Status = TransferStatus.Completed;
            record.CompletedUtc = DateTimeOffset.UtcNow;
            record.SpeedBytesPerSecond = record.BytesTransferred / Math.Max(stopwatch.Elapsed.TotalSeconds, 1);
            if (!record.IsFolder)
            {
                record.SourceHash = offer.Hash;
                record.DestinationHash = await ComputeHashAsync(destination);
                await VerifyHashesAsync(record);
            }
            await _history.RecordAsync(record);
            await _notifications.ShowAsync("CrossDroid network transfer received", $"{record.FileName} received successfully.");
        }
        catch (OperationCanceledException)
        {
            record.Status = TransferStatus.Cancelled;
            record.CompletedUtc = DateTimeOffset.UtcNow;
            await _history.RecordAsync(record);
        }
        catch (Exception ex)
        {
            record.Status = TransferStatus.Failed;
            record.CompletedUtc = DateTimeOffset.UtcNow;
            record.ErrorMessage = ex.Message;
            await _history.RecordAsync(record);
            await _notifications.ShowAsync("CrossDroid network transfer failed", $"{record.FileName}: {ex.Message}");
        }
    }


    private static async Task VerifyHashesAsync(TransferRecord record)
    {
        if (!record.IsFolder && !string.Equals(record.SourceHash, record.DestinationHash, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("Hash verification failed after transfer.");
        }

        await Task.CompletedTask;
    }

    private static async Task<string> ComputeHashAsync(string path)
    {
        await using var stream = File.OpenRead(path);
        var hash = await SHA256.HashDataAsync(stream);
        return Convert.ToHexString(hash);
    }

    private static string ResolveConflictPath(string targetPath)
    {
        if (!File.Exists(targetPath) && !Directory.Exists(targetPath))
        {
            return targetPath;
        }

        var directory = Path.GetDirectoryName(targetPath)!;
        var name = Path.GetFileNameWithoutExtension(targetPath);
        var extension = Path.GetExtension(targetPath);
        for (var i = 1; ; i++)
        {
            var candidate = Path.Combine(directory, $"{name} ({i}){extension}");
            if (!File.Exists(candidate) && !Directory.Exists(candidate))
            {
                return candidate;
            }
        }
    }
}

public sealed class HistoryService
{
    private readonly AppStateStore _store;

    public HistoryService(AppStateStore store)
    {
        _store = store;
    }

    public ObservableCollection<TransferHistoryRecord> Records { get; } = new();

    public void LoadFromState()
    {
        Records.Clear();
        foreach (var item in _store.State.History.OrderByDescending(h => h.CompletedUtc ?? h.CreatedUtc))
        {
            Records.Add(item);
        }
    }

    public async Task RecordAsync(TransferRecord transfer)
    {
        var history = TransferHistoryRecord.FromTransfer(transfer);
        _store.State.History.RemoveAll(h => h.TransferId == history.TransferId);
        _store.State.History.Add(history);
        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;
        if (dispatcher != null && !dispatcher.HasThreadAccess)
        {
            dispatcher.TryEnqueue(() => Records.Insert(0, history));
        }
        else
        {
            Records.Insert(0, history);
        }
        _store.State.Audit.Add(AuditEvent.Create("TransferRecorded", $"{history.Status}: {history.FileName}"));
        await _store.SaveAsync();
    }
}

public sealed class NotificationService
{
    private readonly SettingsService _settings;

    public NotificationService(SettingsService settings)
    {
        _settings = settings;
    }

    public async Task ShowAsync(string title, string message)
    {
        if (_settings.Current.ToastNotify)
        {
            try
            {
                var toastXml = global::Windows.UI.Notifications.ToastNotificationManager.GetTemplateContent(global::Windows.UI.Notifications.ToastTemplateType.ToastText02);
                var textNodes = toastXml.GetElementsByTagName("text");
                textNodes[0].AppendChild(toastXml.CreateTextNode(title));
                textNodes[1].AppendChild(toastXml.CreateTextNode(message));
                var toast = new global::Windows.UI.Notifications.ToastNotification(toastXml);
                global::Windows.UI.Notifications.ToastNotificationManager.CreateToastNotifier().Show(toast);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Toast notification error: {ex.Message}");
            }
        }

        if (_settings.Current.SoundNotify)
        {
            try
            {
                // Use native Windows MessageBeep (0x40 = MB_ICONASTERISK)
                NativeMethods.MessageBeep(0x00000040);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Sound notification error: {ex.Message}");
            }
        }

        await Task.CompletedTask;
    }
}

public sealed class SystemControlService
{
    public async Task<SystemControlResult> EnsureWifiAvailableAsync()
    {
        try
        {
            var access = await global::Windows.Devices.Radios.Radio.RequestAccessAsync();
            if (access != global::Windows.Devices.Radios.RadioAccessStatus.Allowed)
            {
                return new SystemControlResult(false, "Access to radio control was denied.");
            }

            var radios = await global::Windows.Devices.Radios.Radio.GetRadiosAsync();
            var wifiRadio = radios.FirstOrDefault(r => r.Kind == global::Windows.Devices.Radios.RadioKind.WiFi);
            if (wifiRadio != null)
            {
                if (wifiRadio.State == global::Windows.Devices.Radios.RadioState.Off)
                {
                    await wifiRadio.SetStateAsync(global::Windows.Devices.Radios.RadioState.On);
                    return new SystemControlResult(true, "Wi-Fi enabled successfully.");
                }
                return new SystemControlResult(true, "Wi-Fi is already enabled.");
            }
            return new SystemControlResult(false, "No Wi-Fi adapter found on this system.");
        }
        catch (Exception ex)
        {
            return new SystemControlResult(false, $"Failed checking radio: {ex.Message}");
        }
    }

    public async Task OpenWifiSettingsAsync()
    {
        await Launcher.LaunchUriAsync(new Uri("ms-settings:network-wifi"));
    }

    public async Task OpenStartupSettingsAsync()
    {
        await Launcher.LaunchUriAsync(new Uri("ms-settings:startupapps"));
    }
}

public sealed class HealthService
{
    private readonly SettingsService _settings;
    private readonly IdentityService _identity;
    private readonly DeviceService _devices;
    private readonly TransferQueueService _transfers;

    public HealthService(SettingsService settings, IdentityService identity, DeviceService devices, TransferQueueService transfers)
    {
        _settings = settings;
        _identity = identity;
        _devices = devices;
        _transfers = transfers;
    }

    public IReadOnlyList<HealthCheckResult> GetSnapshot()
    {
        var checks = new List<HealthCheckResult>
        {
            new("Local identity", !string.IsNullOrWhiteSpace(_identity.LocalDevice.DeviceId), _identity.LocalDevice.DeviceId),
            new("Receive folder", Directory.Exists(_settings.Current.DownloadsDirectory), _settings.Current.DownloadsDirectory),
            new("Trusted devices", _devices.TrustedDevices.Any(), $"{_devices.TrustedDevices.Count()} trusted"),
            new("Transfer queue", true, $"{_transfers.Queue.Count} active record(s)")
        };
        return checks;
    }
}

public sealed class TransferRuntime
{
    public CancellationTokenSource Cancellation { get; } = new();
    public ManualResetEventSlim PauseEvent { get; } = new(true);
    public bool IsPaused { get; set; }
    
    // Sliding window speed tracking
    private long _lastBytesSnapshot;
    private DateTimeOffset _lastSnapshotTime = DateTimeOffset.UtcNow;
    
    public double CalculateSpeed(long currentBytesTransferred)
    {
        var now = DateTimeOffset.UtcNow;
        var elapsed = (now - _lastSnapshotTime).TotalSeconds;
        if (elapsed < 0.5) return -1; // Not enough time has passed
        
        var bytesDelta = currentBytesTransferred - _lastBytesSnapshot;
        var speed = bytesDelta / elapsed;
        
        _lastBytesSnapshot = currentBytesTransferred;
        _lastSnapshotTime = now;
        
        return Math.Max(0, speed);
    }
}

public sealed class LocalDeviceIdentity
{
    public string DeviceId { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string DeviceType { get; set; } = "";
    public string PublicFingerprint { get; set; } = "";
    public string ProtectedPrivateKey { get; set; } = "";
    public DateTimeOffset CreatedUtc { get; set; }
}

public sealed class DeviceRecord : NotifyBase
{
    private string _alias = "";
    private DeviceTrustState _trustState;
    private DevicePresence _presence;
    private ConnectionStatus _connectionStatus;
    private EncryptionStatus _encryptionStatus;
    private bool _isBlocked;
    private DateTimeOffset? _lastTransferUtc;

    public string DeviceId { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string Alias { get => _alias; set => SetField(ref _alias, value); }
    public string AliasOrName => string.IsNullOrWhiteSpace(Alias) ? DisplayName : Alias;
    public string DeviceType { get; set; } = "";
    public string Endpoint { get; set; } = "";
    public string Fingerprint { get; set; } = "";
    public DeviceTrustState TrustState { get => _trustState; set => SetField(ref _trustState, value); }
    public DevicePresence Presence { get => _presence; set => SetField(ref _presence, value); }
    public PairingStatus PairingStatus { get; set; }
    public ConnectionStatus ConnectionStatus { get => _connectionStatus; set => SetField(ref _connectionStatus, value); }
    public EncryptionStatus EncryptionStatus { get => _encryptionStatus; set => SetField(ref _encryptionStatus, value); }
    public bool IsBlocked { get => _isBlocked; set => SetField(ref _isBlocked, value); }
    public DateTimeOffset LastSeenUtc { get; set; }
    public DateTimeOffset? LastTransferUtc { get => _lastTransferUtc; set => SetField(ref _lastTransferUtc, value); }
    public int RejectedCount { get; set; } = 0;
}

public sealed class StagedTransferItem
{
    public string Name { get; set; } = "";
    public string Path { get; set; } = "";
    public bool IsFolder { get; set; }
    public long Bytes { get; set; }
    public int ItemCount { get; set; }
    public string SizeText => IsFolder ? $"{ItemCount} files - {FormatBytes(Bytes)}" : FormatBytes(Bytes);

    public static string FormatBytes(long bytes)
    {
        string[] units = ["B", "KB", "MB", "GB", "TB"];
        var size = (double)Math.Max(bytes, 0);
        var unit = 0;
        while (size >= 1024 && unit < units.Length - 1)
        {
            size /= 1024;
            unit++;
        }

        return $"{size:F1} {units[unit]}";
    }
}

public sealed class TransferRecord : NotifyBase
{
    private TransferStatus _status;
    private long _bytesTransferred;
    private double _progressPercent;
    private double _speedBytesPerSecond;
    private string _errorMessage = "";

    public string TransferId { get; set; } = "";
    public TransferDirection Direction { get; set; }
    public string DeviceId { get; set; } = "";
    public string DeviceName { get; set; } = "";
    public string FileName { get; set; } = "";
    public string SourcePath { get; set; } = "";
    public string DestinationPath { get; set; } = "";
    public bool IsFolder { get; set; }
    public long TotalBytes { get; set; }
    public long BytesTransferred { get => _bytesTransferred; set { if (SetField(ref _bytesTransferred, value)) OnPropertyChanged(nameof(ProgressText)); } }
    public double ProgressPercent { get => _progressPercent; set { if (SetField(ref _progressPercent, value)) OnPropertyChanged(nameof(ProgressText)); } }
    public double SpeedBytesPerSecond { get => _speedBytesPerSecond; set { if (SetField(ref _speedBytesPerSecond, value)) OnPropertyChanged(nameof(SpeedText)); } }
    public TransferStatus Status { get => _status; set { if (SetField(ref _status, value)) OnPropertyChanged(nameof(StatusText)); } }
    public string ErrorMessage { get => _errorMessage; set => SetField(ref _errorMessage, value); }
    public string SourceHash { get; set; } = "";
    public string DestinationHash { get; set; } = "";
    public DateTimeOffset CreatedUtc { get; set; }
    public DateTimeOffset? CompletedUtc { get; set; }
    public string ProgressText => $"{ProgressPercent:F0}%";
    public string StatusText => Status.ToString();
    public string SizeText => StagedTransferItem.FormatBytes(TotalBytes);
    public string SpeedText => $"{StagedTransferItem.FormatBytes((long)SpeedBytesPerSecond)}/s";
    public double EstimatedSecondsRemaining => SpeedBytesPerSecond > 0 ? (TotalBytes - BytesTransferred) / SpeedBytesPerSecond : 0;
    public string ETAText => EstimatedSecondsRemaining > 0 ? $"{TimeSpan.FromSeconds(EstimatedSecondsRemaining):mm\\:ss} remaining" : "";
}

public sealed class TransferHistoryRecord
{
    public string TransferId { get; set; } = "";
    public TransferDirection Direction { get; set; }
    public string DeviceId { get; set; } = "";
    public string DeviceName { get; set; } = "";
    public string FileName { get; set; } = "";
    public bool IsFolder { get; set; }
    public long TotalBytes { get; set; }
    public TransferStatus Status { get; set; }
    public string ErrorMessage { get; set; } = "";
    public string DestinationPath { get; set; } = "";
    public string SourceHash { get; set; } = "";
    public string DestinationHash { get; set; } = "";
    public DateTimeOffset CreatedUtc { get; set; }
    public DateTimeOffset? CompletedUtc { get; set; }

    public static TransferHistoryRecord FromTransfer(TransferRecord transfer)
    {
        return new TransferHistoryRecord
        {
            TransferId = transfer.TransferId,
            Direction = transfer.Direction,
            DeviceId = transfer.DeviceId,
            DeviceName = transfer.DeviceName,
            FileName = transfer.FileName,
            IsFolder = transfer.IsFolder,
            TotalBytes = transfer.TotalBytes,
            Status = transfer.Status,
            ErrorMessage = transfer.ErrorMessage,
            DestinationPath = transfer.DestinationPath,
            SourceHash = transfer.SourceHash,
            DestinationHash = transfer.DestinationHash,
            CreatedUtc = transfer.CreatedUtc,
            CompletedUtc = transfer.CompletedUtc
        };
    }
}

public sealed class IncomingTransferRequest
{
    public string RequestId { get; set; } = "";
    public DeviceRecord Device { get; set; } = new();
    public List<StagedTransferItem> Items { get; set; } = new();
    public DateTimeOffset CreatedUtc { get; set; }
}

public sealed class AuditEvent
{
    public string EventId { get; set; } = "";
    public string Type { get; set; } = "";
    public string Message { get; set; } = "";
    public DateTimeOffset CreatedUtc { get; set; }

    public static AuditEvent Create(string type, string message)
    {
        return new AuditEvent
        {
            EventId = Guid.NewGuid().ToString("N"),
            Type = type,
            Message = message,
            CreatedUtc = DateTimeOffset.UtcNow
        };
    }
}

public sealed record SystemControlResult(bool DirectActionSucceeded, string Message);
public sealed record HealthCheckResult(string Name, bool IsHealthy, string Details);

public enum DeviceTrustState { Unknown, Trusted, Blocked, Revoked, Mismatch }
public enum DevicePresence { Unknown, Online, Offline, Blocked }
public enum PairingStatus { NotPaired, Pairing, Paired, Failed }
public enum ConnectionStatus { Unknown, Ready, Connecting, Connected, Offline, Blocked, Failed }
public enum EncryptionStatus { Unknown, LocalProtected, Negotiated, Mismatch, Failed }
public enum TransferDirection { Outgoing, Incoming }
public enum TransferStatus { Queued, Transferring, Paused, Completed, Failed, Cancelled }

public abstract class NotifyBase : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;

    protected bool SetField<T>(ref T field, T value, [System.Runtime.CompilerServices.CallerMemberName] string propertyName = "")
    {
        if (EqualityComparer<T>.Default.Equals(field, value))
        {
            return false;
        }

        field = value;
        OnPropertyChanged(propertyName);
        return true;
    }

    protected void OnPropertyChanged(string propertyName)
    {
        var dispatcher = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread() 
                         ?? CrossDroid.Windows.App.MainWindowInstance?.DispatcherQueue;

        if (dispatcher != null && !dispatcher.HasThreadAccess)
        {
            dispatcher.TryEnqueue(() => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName)));
        }
        else
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
    }
}

internal static class NativeMethods
{
    [System.Runtime.InteropServices.DllImport("user32.dll")]
    internal static extern bool MessageBeep(uint uType);
}
