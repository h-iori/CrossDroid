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

        public DevicesView()
        {
            this.InitializeComponent();
            LoadMockData();
            
            PairedDevicesList.ItemsSource = PairedDevices;
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
}
