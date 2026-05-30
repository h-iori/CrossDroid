using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Animation;
using System;

namespace CrossDroid.Windows.Views
{
    public sealed partial class HomeView : Page
    {
        public HomeView()
        {
            this.InitializeComponent();
            this.Loaded += HomeView_Loaded;
        }

        private void HomeView_Loaded(object sender, RoutedEventArgs e)
        {
            if (RadarGrid.Resources.TryGetValue("RadarPulseStoryboard", out object storyboardObj) && 
                storyboardObj is Storyboard storyboard)
            {
                storyboard.Begin();
            }
        }

        private void DiscoverableToggle_Toggled(object sender, RoutedEventArgs e)
        {
            if (DiscoverableToggle == null || RadarGrid == null) return;

            if (DiscoverableToggle.IsOn)
            {
                if (RadarGrid.Resources.TryGetValue("RadarPulseStoryboard", out object storyboardObj) && 
                    storyboardObj is Storyboard storyboard)
                {
                    storyboard.Begin();
                }
            }
            else
            {
                if (RadarGrid.Resources.TryGetValue("RadarPulseStoryboard", out object storyboardObj) && 
                    storyboardObj is Storyboard storyboard)
                {
                    storyboard.Stop();
                }
            }
        }

        private async void SendFilesButton_Click(object sender, RoutedEventArgs e)
        {
            App.MainWindowInstance.IsPickingFile = true;
            try
            {
                var picker = new global::Windows.Storage.Pickers.FileOpenPicker();
                
                // Get the Window's HWND (Required in WinUI 3)
                var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(App.MainWindowInstance);
                WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

                picker.ViewMode = global::Windows.Storage.Pickers.PickerViewMode.List;
                picker.SuggestedStartLocation = global::Windows.Storage.Pickers.PickerLocationId.ComputerFolder;
                picker.FileTypeFilter.Add("*");

                var files = await picker.PickMultipleFilesAsync();
                if (files != null && files.Count > 0)
                {
                    // Stage selected files
                    App.MainWindowInstance.StageTransfers(files);
                }
            }
            finally
            {
                App.MainWindowInstance.IsPickingFile = false;
            }
        }

        private async void SendFolderButton_Click(object sender, RoutedEventArgs e)
        {
            App.MainWindowInstance.IsPickingFile = true;
            try
            {
                var picker = new global::Windows.Storage.Pickers.FolderPicker();

                var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(App.MainWindowInstance);
                WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

                picker.SuggestedStartLocation = global::Windows.Storage.Pickers.PickerLocationId.ComputerFolder;
                picker.FileTypeFilter.Add("*");

                var folder = await picker.PickSingleFolderAsync();
                if (folder != null)
                {
                    App.MainWindowInstance.StageFolder(folder);
                }
            }
            finally
            {
                App.MainWindowInstance.IsPickingFile = false;
            }
        }
    }
}
