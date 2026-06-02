using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using System.Collections.Generic;
using System.Linq;
using CrossDroid.Windows.Backend;

namespace CrossDroid.Windows.Views
{
    public sealed partial class RadarView : Page
    {
        private List<StorageFileItem> _selectedFiles = new();

        public RadarView()
        {
            this.InitializeComponent();
            this.Loaded += RadarView_Loaded;
        }

        private void RadarView_Loaded(object sender, RoutedEventArgs e)
        {
            RadarSweepAnimation.Begin();
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
            if (sender is Button btn && btn.Tag is string deviceName)
            {
                NavigateToTransfer(deviceName);
            }
        }

        private async void ConnectPin_Click(object sender, RoutedEventArgs e)
        {
            string pin = PinTextBox.Text;
            if (pin.Length == 4)
            {
                var trusted = App.Backend.Devices.TrustedDevices.FirstOrDefault();
                if (trusted != null)
                {
                    NavigateToTransfer(trusted.AliasOrName);
                }
                else
                {
                    var dialog = new ContentDialog
                    {
                        Title = "No trusted device",
                        Content = "Manual PIN entry is ready, but no trusted backend device is available yet. Use the local reference receiver from Devices or pair a peer first.",
                        CloseButtonText = "OK",
                        XamlRoot = this.XamlRoot
                    };
                    await dialog.ShowAsync();
                }
            }
            else
            {
                // Simple error feedback
                var dialog = new ContentDialog
                {
                    Title = "Invalid PIN",
                    Content = "Please enter a valid 4-digit PIN.",
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
}
