using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using System;
using System.Collections.ObjectModel;

namespace CrossDroid.Windows.Views
{
    public sealed partial class DevicesView : Page
    {
        public ObservableCollection<DeviceViewModel> PairedDevices { get; set; } = new();
        public ObservableCollection<NearbyDeviceViewModel> NearbyDevices { get; set; } = new();

        public DevicesView()
        {
            this.InitializeComponent();
            LoadMockData();
            
            PairedDevicesList.ItemsSource = PairedDevices;
            NearbyDevicesList.ItemsSource = NearbyDevices;
        }

        private void LoadMockData()
        {
            PairedDevices.Clear();
            PairedDevices.Add(new DeviceViewModel
            {
                Name = "Pixel 8 Pro",
                IconGlyph = "\xE8EA", // Phone
                Status = "TRUSTED",
                Info = "Last seen: Active on 192.168.1.50 • Fingerprint: E8:4B:92...",
                StatusBackground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136)), // SuccessGreen translucent
                StatusForeground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136))
            });
            PairedDevices.Add(new DeviceViewModel
            {
                Name = "Galaxy Tab S9",
                IconGlyph = "\xE90A", // Tablet
                Status = "TRUSTED",
                Info = "Last seen: 2 hours ago • Fingerprint: 4A:9C:23...",
                StatusBackground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136)),
                StatusForeground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136))
            });
            PairedDevices.Add(new DeviceViewModel
            {
                Name = "Desktop-Office",
                IconGlyph = "\xE7F4", // Computer
                Status = "BLOCKED",
                Info = "Fingerprint: 12:FF:A9...",
                StatusBackground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 255, 51, 102)), // ErrorRed translucent
                StatusForeground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102))
            });

            NearbyDevices.Clear();
            NearbyDevices.Add(new NearbyDeviceViewModel { Name = "iPhone 15 Pro", Address = "192.168.1.12 • Tap to pair", IconGlyph = "\xE8EA" });
            NearbyDevices.Add(new NearbyDeviceViewModel { Name = "MacBook Pro M3", Address = "192.168.1.75 • Tap to pair", IconGlyph = "\xE7F4" });
        }

        private void RefreshPaired_Click(object sender, RoutedEventArgs e)
        {
            LoadMockData();
        }

        private void BlockDevice_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is DeviceViewModel vm)
            {
                if (vm.Status == "TRUSTED")
                {
                    vm.Status = "BLOCKED";
                    vm.StatusBackground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 255, 51, 102));
                    vm.StatusForeground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102));
                }
                else
                {
                    vm.Status = "TRUSTED";
                    vm.StatusBackground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136));
                    vm.StatusForeground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136));
                }
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
                    vm.Name = input.Text;
                }
            }
        }

        private void DeleteDevice_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is DeviceViewModel vm)
            {
                PairedDevices.Remove(vm);
            }
        }

        private void PairDevice_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is NearbyDeviceViewModel vm)
            {
                // Put address PIN numbers in boxes for mock typing
                PinBox1.Text = "8";
                PinBox2.Text = "4";
                PinBox3.Text = "2";
                PinBox4.Text = "9";
            }
        }

        private void PinBox_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (sender is TextBox currentBox && currentBox.Text.Length == 1)
            {
                // Advance focus to next pin text box
                if (currentBox == PinBox1) PinBox2.Focus(FocusState.Programmatic);
                else if (currentBox == PinBox2) PinBox3.Focus(FocusState.Programmatic);
                else if (currentBox == PinBox3) PinBox4.Focus(FocusState.Programmatic);
            }
        }

        private async void VerifyPin_Click(object sender, RoutedEventArgs e)
        {
            string pin = $"{PinBox1.Text}{PinBox2.Text}{PinBox3.Text}{PinBox4.Text}";
            if (pin == "8429")
            {
                var dialog = new ContentDialog
                {
                    Title = "Pairing Successful",
                    Content = "The connection is established. This device is now trusted.",
                    CloseButtonText = "OK",
                    XamlRoot = this.XamlRoot
                };
                await dialog.ShowAsync();

                // Add to paired devices
                PairedDevices.Add(new DeviceViewModel
                {
                    Name = "New Paired Device",
                    IconGlyph = "\xE8EA",
                    Status = "TRUSTED",
                    Info = "Last seen: Just now • Fingerprint: F4:22:AA...",
                    StatusBackground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136)),
                    StatusForeground = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136))
                });

                // Clear fields
                PinBox1.Text = PinBox2.Text = PinBox3.Text = PinBox4.Text = string.Empty;
            }
            else
            {
                var dialog = new ContentDialog
                {
                    Title = "Pairing Failed",
                    Content = "The entered PIN is incorrect. Please try again.",
                    CloseButtonText = "OK",
                    XamlRoot = this.XamlRoot
                };
                await dialog.ShowAsync();
            }
        }

        private async void OpenCamera_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new ContentDialog
            {
                Title = "Camera Scanner",
                Content = "Scanning for QR codes... (Simulated camera preview pane)",
                CloseButtonText = "Cancel",
                XamlRoot = this.XamlRoot
            };
            await dialog.ShowAsync();
        }
    }

    public class DeviceViewModel : System.ComponentModel.INotifyPropertyChanged
    {
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

        public event System.ComponentModel.PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged(string prop) => PropertyChanged?.Invoke(this, new System.ComponentModel.PropertyChangedEventArgs(prop));
    }

    public class NearbyDeviceViewModel
    {
        public string Name { get; set; } = string.Empty;
        public string Address { get; set; } = string.Empty;
        public string IconGlyph { get; set; } = string.Empty;
    }
}
