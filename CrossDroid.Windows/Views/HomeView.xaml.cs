using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Animation;
using System;
using System.Security.Cryptography;
using System.Threading.Tasks;
using CrossDroid.Windows.Backend;

namespace CrossDroid.Windows.Views
{
    public sealed partial class HomeView : Page
    {
        private DispatcherTimer? _pinTimer;

        public HomeView()
        {
            this.InitializeComponent();
            this.Loaded += HomeView_Loaded;
            this.Unloaded += HomeView_Unloaded;
        }

        private void HomeView_Loaded(object sender, RoutedEventArgs e)
        {
            GenerateNewPin();
            
            _ = Task.Run(async () =>
            {
                var bmp = await App.Backend.Pairing.GenerateQrCodeAsync();
                
                this.DispatcherQueue.TryEnqueue(async () =>
                {
                    if (bmp != null && QrCodeImage != null)
                    {
                        var source = new Microsoft.UI.Xaml.Media.Imaging.SoftwareBitmapSource();
                        await source.SetBitmapAsync(bmp);
                        QrCodeImage.Source = source;
                    }

                    if (VisibilityToggle != null)
                    {
                        VisibilityToggle.IsOn = App.Backend.Settings.Current.Discoverable;
                    }
                });
            });

            // Start PIN countdown timer (1-second tick)
            _pinTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
            _pinTimer.Tick += PinTimer_Tick;
            _pinTimer.Start();
        }

        private void HomeView_Unloaded(object sender, RoutedEventArgs e)
        {
            _pinTimer?.Stop();
            _pinTimer = null;
        }

        private void PinTimer_Tick(object? sender, object e)
        {
            var pairing = App.Backend.Pairing;
            if (pairing.RefreshIfExpired())
            {
                // PIN was expired and got auto-regenerated
                PinTextBlock.Text = pairing.CurrentPin;
                // Also regenerate QR code
                _ = Task.Run(async () =>
                {
                    var bmp = await pairing.GenerateQrCodeAsync();
                    this.DispatcherQueue.TryEnqueue(async () =>
                    {
                        if (bmp != null && QrCodeImage != null)
                        {
                            var source = new Microsoft.UI.Xaml.Media.Imaging.SoftwareBitmapSource();
                            await source.SetBitmapAsync(bmp);
                            QrCodeImage.Source = source;
                        }
                    });
                });
            }

            // Update countdown
            var remaining = pairing.PinRemainingTime;
            if (remaining.TotalSeconds > 0)
            {
                PinCountdownText.Text = $"Expires in {remaining.Minutes:D1}:{remaining.Seconds:D2}";
            }
            else
            {
                PinCountdownText.Text = "";
            }
        }


        private void VisibilityToggle_Toggled(object sender, RoutedEventArgs e)
        {
            if (VisibilityToggle == null || DiscoveryPanel == null || HiddenPanel == null || VisibilityStatusText == null) 
                return;

            if (VisibilityToggle.IsOn)
            {
                App.Backend.Settings.Current.Discoverable = true;
                DiscoveryPanel.Visibility = Visibility.Visible;
                HiddenPanel.Visibility = Visibility.Collapsed;
                VisibilityStatusText.Text = "Visible to nearby devices";
                VisibilityStatusText.Foreground = (Microsoft.UI.Xaml.Media.Brush)Application.Current.Resources["ColorSuccess"];


            }
            else
            {
                App.Backend.Settings.Current.Discoverable = false;
                DiscoveryPanel.Visibility = Visibility.Collapsed;
                HiddenPanel.Visibility = Visibility.Visible;
                VisibilityStatusText.Text = "Hidden from nearby devices";
                VisibilityStatusText.Foreground = (Microsoft.UI.Xaml.Media.Brush)Application.Current.Resources["ColorError"];


            }
        }

        private void RefreshButton_Click(object sender, RoutedEventArgs e)
        {
            GenerateNewPin();
        }

        private void GenerateNewPin()
        {
            PinTextBlock.Text = App.Backend.Pairing.GenerateTemporaryPin();
            
            _ = Task.Run(async () =>
            {
                var bmp = await App.Backend.Pairing.GenerateQrCodeAsync();
                
                this.DispatcherQueue.TryEnqueue(async () =>
                {
                    if (bmp != null && QrCodeImage != null)
                    {
                        var source = new Microsoft.UI.Xaml.Media.Imaging.SoftwareBitmapSource();
                        await source.SetBitmapAsync(bmp);
                        QrCodeImage.Source = source;
                    }
                });
            });
        }

        private async void TempSendButton_Click(object sender, RoutedEventArgs e)
        {
            var window = App.MainWindowInstance;
            if (window == null) return;

            var picker = new global::Windows.Storage.Pickers.FileOpenPicker();
            picker.ViewMode = global::Windows.Storage.Pickers.PickerViewMode.Thumbnail;
            picker.SuggestedStartLocation = global::Windows.Storage.Pickers.PickerLocationId.PicturesLibrary;
            picker.FileTypeFilter.Add("*");

            // Initialize the picker with the window handle
            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(window);
            WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

            var files = await picker.PickMultipleFilesAsync();
            if (files != null && files.Count > 0)
            {
                var selectedFiles = new System.Collections.Generic.List<StorageFileItem>();
                foreach (var file in files)
                {
                    var props = await file.GetBasicPropertiesAsync();
                    selectedFiles.Add(new StorageFileItem 
                    { 
                        Name = file.Name, 
                        Size = props.Size,
                        Path = file.Path
                    });
                }

                await App.Backend.Staging.StageStorageItemsAsync(files);
                
                // Navigate to Radar Screen with selected files (ensure UI thread)
                this.DispatcherQueue.TryEnqueue(() =>
                {
                    this.Frame.Navigate(typeof(RadarView), selectedFiles);
                });
            }
        }
    }

    public class StorageFileItem
    {
        public string Name { get; set; } = string.Empty;
        public ulong Size { get; set; }
        public string Path { get; set; } = string.Empty;
    }
}
