using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;
using CrossDroid.Windows.Backend;

namespace CrossDroid.Windows.Views
{
    public sealed partial class SettingsView : Page
    {
        private bool _isInitializing = true;

        public SettingsView()
        {
            this.InitializeComponent();
            
            // Set initial state from backend settings
            AutoStartToggle.IsOn = App.AutoStartEnabled;
            StartMinimizedToggle.IsOn = App.StartMinimized;
            CloseToTrayToggle.IsOn = App.CloseToTray;
            AutoAcceptTrustedToggle.IsOn = App.AutoAcceptTrusted;
            WifiOnlyToggle.IsOn = App.WifiOnly;
            P2pFallbackToggle.IsOn = App.P2pFallback;
            ToastNotifyToggle.IsOn = App.ToastNotify;
            SoundNotifyToggle.IsOn = App.SoundNotify;
            DownloadPathBox.Text = App.DownloadsDirectory;

            // Set network band combo
            var band = App.Backend.Settings.Current.PreferredNetworkBand;
            NetworkBandCombo.SelectedIndex = band;

            _isInitializing = false;
        }

        private async void Setting_Toggled(object sender, RoutedEventArgs e)
        {
            if (_isInitializing) return;

            if (sender == (object)AutoStartToggle)
            {
                App.AutoStartEnabled = AutoStartToggle.IsOn;
                // Actually call the MSIX StartupTask API
                await App.Backend.Shell.EnsureAutoStartAsync(AutoStartToggle.IsOn);
            }
            else if (sender == (object)StartMinimizedToggle)
            {
                App.StartMinimized = StartMinimizedToggle.IsOn;
            }
            else if (sender == (object)CloseToTrayToggle)
            {
                App.CloseToTray = CloseToTrayToggle.IsOn;
            }
            else if (sender == (object)AutoAcceptTrustedToggle)
            {
                App.AutoAcceptTrusted = AutoAcceptTrustedToggle.IsOn;
            }
            else if (sender == (object)WifiOnlyToggle)
            {
                App.WifiOnly = WifiOnlyToggle.IsOn;
            }
            else if (sender == (object)P2pFallbackToggle)
            {
                App.P2pFallback = P2pFallbackToggle.IsOn;
            }
            else if (sender == (object)ToastNotifyToggle)
            {
                App.ToastNotify = ToastNotifyToggle.IsOn;
            }
            else if (sender == (object)SoundNotifyToggle)
            {
                App.SoundNotify = SoundNotifyToggle.IsOn;
            }

            // Settings auto-persist via PropertyChanged -> SaveSoon()
        }

        private void NetworkBandCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (_isInitializing) return;
            App.Backend.Settings.Current.PreferredNetworkBand = NetworkBandCombo.SelectedIndex;
        }

        private async void BrowseDownloadFolder_Click(object sender, RoutedEventArgs e)
        {
            App.MainWindowInstance.IsPickingFile = true;
            try
            {
                var picker = new global::Windows.Storage.Pickers.FolderPicker();
                
                var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(App.MainWindowInstance);
                WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

                picker.SuggestedStartLocation = global::Windows.Storage.Pickers.PickerLocationId.Downloads;
                picker.FileTypeFilter.Add("*");

                var folder = await picker.PickSingleFolderAsync();
                if (folder != null)
                {
                    await App.Backend.Settings.SetDownloadsDirectoryAsync(folder.Path);
                    DownloadPathBox.Text = folder.Path;
                }
            }
            finally
            {
                App.MainWindowInstance.IsPickingFile = false;
            }
        }

        private void ViewAbout_Click(object sender, RoutedEventArgs e)
        {
            App.MainWindowInstance.NavigateToPage("About");
        }
    }
}
