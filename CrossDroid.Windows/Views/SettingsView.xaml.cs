using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;

namespace CrossDroid.Windows.Views
{
    public sealed partial class SettingsView : Page
    {
        public SettingsView()
        {
            this.InitializeComponent();
            
            // Set initial state from App properties
            AutoStartToggle.IsOn = App.AutoStartEnabled;
            StartMinimizedToggle.IsOn = App.StartMinimized;
            CloseToTrayToggle.IsOn = App.CloseToTray;
            AutoAcceptTrustedToggle.IsOn = App.AutoAcceptTrusted;
            WifiOnlyToggle.IsOn = App.WifiOnly;
            P2pFallbackToggle.IsOn = App.P2pFallback;
            ToastNotifyToggle.IsOn = App.ToastNotify;
            SoundNotifyToggle.IsOn = App.SoundNotify;
            DownloadPathBox.Text = App.DownloadsDirectory;
        }

        private void Setting_Toggled(object sender, RoutedEventArgs e)
        {
            if (sender == (object)AutoStartToggle) App.AutoStartEnabled = AutoStartToggle.IsOn;
            else if (sender == (object)StartMinimizedToggle) App.StartMinimized = StartMinimizedToggle.IsOn;
            else if (sender == (object)CloseToTrayToggle) App.CloseToTray = CloseToTrayToggle.IsOn;
            else if (sender == (object)AutoAcceptTrustedToggle) App.AutoAcceptTrusted = AutoAcceptTrustedToggle.IsOn;
            else if (sender == (object)WifiOnlyToggle) App.WifiOnly = WifiOnlyToggle.IsOn;
            else if (sender == (object)P2pFallbackToggle) App.P2pFallback = P2pFallbackToggle.IsOn;
            else if (sender == (object)ToastNotifyToggle) App.ToastNotify = ToastNotifyToggle.IsOn;
            else if (sender == (object)SoundNotifyToggle) App.SoundNotify = SoundNotifyToggle.IsOn;
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
                    App.DownloadsDirectory = folder.Path;
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
