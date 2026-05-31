using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;
using System.Collections.ObjectModel;

namespace CrossDroid.Windows.Views
{
    public sealed partial class HistoryView : Page
    {
        public ObservableCollection<HistoryItemViewModel> DisplayedItems { get; set; } = new();

        public HistoryView()
        {
            this.InitializeComponent();
            this.Loaded += (s, e) => RefreshList();
        }

        private void RefreshList()
        {
            if (App.MainWindowInstance == null) return;
            var records = App.MainWindowInstance.HistoryRecords;
            
            DisplayedItems.Clear();
            // Display items in reverse order (newest first)
            for (int i = records.Count - 1; i >= 0; i--)
            {
                DisplayedItems.Add(records[i]);
            }
        }

        private async void OpenFile_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new ContentDialog
            {
                Title = "Open File (Simulation)",
                Content = "Simulating launching local system file viewer...",
                CloseButtonText = "Close",
                XamlRoot = this.XamlRoot
            };
            await dialog.ShowAsync();
        }

        private async void OpenFolder_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new ContentDialog
            {
                Title = "Open Folder (Simulation)",
                Content = "Simulating opening default downloads directory in File Explorer...",
                CloseButtonText = "Close",
                XamlRoot = this.XamlRoot
            };
            await dialog.ShowAsync();
        }
    }

    public class HistoryItemViewModel
    {
        public string FileName { get; set; } = string.Empty;
        public string FileSize { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
        public string DateText { get; set; } = string.Empty;
        public string DetailMessage { get; set; } = string.Empty;
        public string StatusGlyph { get; set; } = string.Empty;
        public Microsoft.UI.Xaml.Media.Brush? StatusBrush { get; set; }
        public Microsoft.UI.Xaml.Media.Brush? IconBg { get; set; }
        public Visibility OpenVisible { get; set; }
    }
}
