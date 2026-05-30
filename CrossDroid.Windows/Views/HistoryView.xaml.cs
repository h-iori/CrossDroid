using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;
using System.Collections.ObjectModel;
using System.Linq;

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
            
            // Apply Search filter
            var query = SearchBox?.Text?.Trim().ToLower() ?? "";
            var filtered = string.IsNullOrEmpty(query) 
                ? records 
                : records.Where(r => r.FileName.ToLower().Contains(query) || r.DetailMessage.ToLower().Contains(query));

            // Apply Sort
            if (SortCombo != null)
            {
                if (SortCombo.SelectedIndex == 1) // Size
                {
                    // Parse size string roughly for sorting, e.g. "12 MB" vs "45 MB"
                    filtered = filtered.OrderByDescending(r => ParseSize(r.FileSize));
                }
                else if (SortCombo.SelectedIndex == 2) // Name
                {
                    filtered = filtered.OrderBy(r => r.FileName);
                }
                else // Date
                {
                    // Records are naturally added in order, reverse for newest
                    filtered = filtered.Reverse();
                }
            }
            else
            {
                filtered = filtered.Reverse();
            }

            DisplayedItems.Clear();
            foreach (var item in filtered)
            {
                DisplayedItems.Add(item);
            }
        }

        private double ParseSize(string sizeStr)
        {
            try
            {
                var parts = sizeStr.Split(' ');
                if (parts.Length >= 1 && double.TryParse(parts[0], out double val))
                {
                    if (parts.Length >= 2 && parts[1].Equals("GB", StringComparison.OrdinalIgnoreCase))
                    {
                        return val * 1024;
                    }
                    return val;
                }
            }
            catch {}
            return 0;
        }

        private void SearchBox_QuerySubmitted(AutoSuggestBox sender, AutoSuggestBoxQuerySubmittedEventArgs args)
        {
            RefreshList();
        }

        private void SortCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            RefreshList();
        }

        private void ClearHistory_Click(object sender, RoutedEventArgs e)
        {
            App.MainWindowInstance.HistoryRecords.Clear();
            RefreshList();
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
