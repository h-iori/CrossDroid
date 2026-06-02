using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.Linq;
using CrossDroid.Windows.Backend;
using System;

namespace CrossDroid.Windows.Views
{
    public sealed partial class RadarView : Page
    {
        private List<StorageFileItem> _selectedFiles = new();
        private Random _random = new Random();

        public ObservableCollection<RadarItem> RadarItems { get; } = new();

        public RadarView()
        {
            this.InitializeComponent();
            this.Loaded += RadarView_Loaded;
            this.Unloaded += RadarView_Unloaded;
        }

        private void RadarView_Loaded(object sender, RoutedEventArgs e)
        {
            RadarSweepAnimation.Begin();
            SyncDevices();
            App.Backend.Devices.Devices.CollectionChanged += Devices_CollectionChanged;
        }

        private void RadarView_Unloaded(object sender, RoutedEventArgs e)
        {
            App.Backend.Devices.Devices.CollectionChanged -= Devices_CollectionChanged;
        }

        private void SyncDevices()
        {
            RadarItems.Clear();
            foreach (var device in App.Backend.Devices.Devices)
            {
                AddRadarItem(device);
            }
        }

        private void Devices_CollectionChanged(object? sender, NotifyCollectionChangedEventArgs e)
        {
            var dispatcher = this.DispatcherQueue;
            dispatcher.TryEnqueue(() =>
            {
                if (e.Action == NotifyCollectionChangedAction.Add && e.NewItems != null)
                {
                    foreach (DeviceRecord device in e.NewItems)
                    {
                        AddRadarItem(device);
                    }
                }
                else if (e.Action == NotifyCollectionChangedAction.Remove && e.OldItems != null)
                {
                    foreach (DeviceRecord device in e.OldItems)
                    {
                        var item = RadarItems.FirstOrDefault(i => i.Device.DeviceId == device.DeviceId);
                        if (item != null) RadarItems.Remove(item);
                    }
                }
                else if (e.Action == NotifyCollectionChangedAction.Reset)
                {
                    SyncDevices();
                }
            });
        }

        private void AddRadarItem(DeviceRecord device)
        {
            if (RadarItems.Any(i => i.Device.DeviceId == device.DeviceId)) return;
            
            // Random margin roughly within a circle of radius 120
            double angle = _random.NextDouble() * 2 * Math.PI;
            double radius = 40 + _random.NextDouble() * 80;
            double x = radius * Math.Cos(angle);
            double y = radius * Math.Sin(angle);

            string icon = "\uE8EA"; // Android default
            string colorKey = "ColorSuccess";
            string label = "ANDROID";
            
            if (device.DeviceType.Contains("Windows", StringComparison.OrdinalIgnoreCase))
            {
                icon = "\uE7F4";
                colorKey = "AccentCyan";
                label = "WINDOWS";
            }
            else if (device.DeviceType.Contains("Apple", StringComparison.OrdinalIgnoreCase) || device.DeviceType.Contains("Mac", StringComparison.OrdinalIgnoreCase) || device.DeviceType.Contains("iOS", StringComparison.OrdinalIgnoreCase))
            {
                icon = "\uE7F4"; // generic PC/Device for now
                colorKey = "TextStrong";
                label = "APPLE";
            }

            RadarItems.Add(new RadarItem
            {
                Device = device,
                Margin = new Thickness(x, y, 0, 0),
                IconGlyph = icon,
                ColorKey = colorKey,
                DeviceTypeLabel = label
            });
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);
            if (e.Parameter is List<StorageFileItem> files)
            {
                _selectedFiles = files;
            }
            else
            {
                _selectedFiles = new List<StorageFileItem>();
            }
        }

        private void BackButton_Click(object sender, RoutedEventArgs e)
        {
            if (this.Frame.CanGoBack)
            {
                this.Frame.GoBack();
            }
        }

        private void Device_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is RadarItem item)
            {
                NavigateToTransfer(item.Device.AliasOrName);
            }
        }

        private async void ConnectPin_Click(object sender, RoutedEventArgs e)
        {
            string pin = PinTextBox.Text;
            if (pin.Length == 6)
            {
                // Send UDP PinSearch challenge
                await App.Backend.Discovery.BroadcastPinSearchAsync(pin);

                var dialog = new ContentDialog
                {
                    Title = "Searching...",
                    Content = $"Sent discovery request for PIN {pin}. If the device is online, it will appear on the radar shortly. Tap it to connect.",
                    CloseButtonText = "OK",
                    XamlRoot = this.XamlRoot
                };
                await dialog.ShowAsync();
                
                PinTextBox.Text = ""; // Clear after search
            }
            else
            {
                var dialog = new ContentDialog
                {
                    Title = "Invalid PIN",
                    Content = "Please enter a valid 6-digit PIN.",
                    CloseButtonText = "OK",
                    XamlRoot = this.XamlRoot
                };
                await dialog.ShowAsync();
            }
        }

        private void NavigateToTransfer(string targetDeviceName)
        {
            var trusted = App.Backend.Devices.TrustedDevices.FirstOrDefault(d => d.AliasOrName == targetDeviceName)
                ?? App.Backend.Devices.TrustedDevices.FirstOrDefault();
            var parameter = new TransferNavParameter
            {
                TargetDeviceName = trusted?.AliasOrName ?? targetDeviceName,
                Files = _selectedFiles
            };
            this.Frame.Navigate(typeof(TransferStreamView), parameter);
        }
    }

    public class TransferNavParameter
    {
        public string TargetDeviceName { get; set; } = string.Empty;
        public List<StorageFileItem> Files { get; set; } = new();
    }

    public class RadarItem
    {
        public DeviceRecord Device { get; set; } = null!;
        public Thickness Margin { get; set; }
        public string IconGlyph { get; set; } = "";
        public string ColorKey { get; set; } = "";
        public string DeviceTypeLabel { get; set; } = "";
    }
}
