using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using System.Collections.Generic;

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
                NavigateToTransfer($"Device PIN-{pin}");
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
            var parameter = new TransferNavParameter
            {
                TargetDeviceName = targetDeviceName,
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
