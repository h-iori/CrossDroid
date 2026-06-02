using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using CrossDroid.Windows.Backend;

namespace CrossDroid.Windows.Views
{
    public sealed partial class DevicesView : Page
    {
        public ObservableCollection<DeviceViewModel> PairedDevices { get; set; } = new();

        public DevicesView()
        {
            this.InitializeComponent();
            PairedDevicesList.ItemsSource = PairedDevices;
            LoadBackendData();
        }

        private void LoadBackendData()
        {
            App.Backend.Devices.RefreshPresenceAsync().GetAwaiter().GetResult();
            PairedDevices.Clear();
            foreach (var device in App.Backend.Devices.Devices
                .OrderByDescending(d => d.Presence == DevicePresence.Online)
                .ThenBy(d => d.AliasOrName))
            {
                PairedDevices.Add(DeviceViewModel.FromDevice(device));
            }
        }

        private async void BlockDevice_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is DeviceViewModel vm)
            {
                var device = App.Backend.Devices.Devices.FirstOrDefault(d => d.DeviceId == vm.DeviceId);
                if (device == null) return;
                await App.Backend.Devices.SetBlockedAsync(device, !device.IsBlocked);
                LoadBackendData();
            }
        }

        private async void RenameDevice_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is DeviceViewModel vm)
            {
                var input = new TextBox { Text = vm.Name, PlaceholderText = "Device alias name" };
                var dialog = new ContentDialog
                {
                    Title = "Rename Device Alias",
                    Content = input,
                    PrimaryButtonText = "Rename",
                    CloseButtonText = "Cancel",
                    XamlRoot = this.XamlRoot
                };

                var res = await dialog.ShowAsync();
                if (res == ContentDialogResult.Primary && !string.IsNullOrWhiteSpace(input.Text))
                {
                    var device = App.Backend.Devices.Devices.FirstOrDefault(d => d.DeviceId == vm.DeviceId);
                    if (device == null) return;
                    await App.Backend.Devices.RenameAsync(device, input.Text);
                    LoadBackendData();
                }
            }
        }

        private async void DeleteDevice_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is DeviceViewModel vm)
            {
                var device = App.Backend.Devices.Devices.FirstOrDefault(d => d.DeviceId == vm.DeviceId);
                if (device == null) return;
                await App.Backend.Devices.RemoveAsync(device);
                LoadBackendData();
            }
        }
    }

    public class DeviceViewModel : System.ComponentModel.INotifyPropertyChanged
    {
        public string DeviceId { get; set; } = string.Empty;

        private string name = string.Empty;
        public string Name
        {
            get => name;
            set { name = value; OnPropertyChanged(nameof(Name)); }
        }

        public string IconGlyph { get; set; } = string.Empty;

        private string status = string.Empty;
        public string Status
        {
            get => status;
            set { status = value; OnPropertyChanged(nameof(Status)); }
        }

        private Brush? statusBackground;
        public Brush? StatusBackground
        {
            get => statusBackground;
            set { statusBackground = value; OnPropertyChanged(nameof(StatusBackground)); }
        }

        private Brush? statusForeground;
        public Brush? StatusForeground
        {
            get => statusForeground;
            set { statusForeground = value; OnPropertyChanged(nameof(StatusForeground)); }
        }

        public string Info { get; set; } = string.Empty;

        public static DeviceViewModel FromDevice(DeviceRecord device)
        {
            bool blocked = device.IsBlocked || device.TrustState == DeviceTrustState.Blocked;
            return new DeviceViewModel
            {
                DeviceId = device.DeviceId,
                Name = device.AliasOrName,
                IconGlyph = device.DeviceType.Contains("PC", StringComparison.OrdinalIgnoreCase) ? "\xE7F4" : "\xE8EA",
                Status = blocked ? "BLOCKED" : device.TrustState.ToString().ToUpperInvariant(),
                Info = $"Presence: {device.Presence} - Connection: {device.ConnectionStatus} - Encryption: {device.EncryptionStatus} - Last seen: {device.LastSeenUtc.LocalDateTime:g} - Fingerprint: {device.Fingerprint}",
                StatusBackground = blocked
                    ? new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 255, 51, 102))
                    : new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136)),
                StatusForeground = blocked
                    ? new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102))
                    : new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136))
            };
        }

        public event System.ComponentModel.PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged(string prop) => PropertyChanged?.Invoke(this, new System.ComponentModel.PropertyChangedEventArgs(prop));
    }
}
