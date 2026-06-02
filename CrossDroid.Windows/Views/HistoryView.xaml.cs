using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using CrossDroid.Windows.Backend;

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
            DisplayedItems.Clear();
            foreach (var group in App.Backend.History.Records.GroupBy(h => h.DeviceName))
            {
                var latest = group.OrderByDescending(h => h.CompletedUtc ?? h.CreatedUtc).First();
                bool isSuccess = latest.Status == TransferStatus.Completed;
                DisplayedItems.Add(new HistoryItemViewModel
                {
                    DeviceName = group.Key,
                    DeviceType = "DEVICE",
                    DeviceIconGlyph = "\xE8EA",
                    FileCount = group.Count(),
                    TotalBytesText = StagedTransferItem.FormatBytes(group.Sum(h => h.TotalBytes)),
                    LastTransferInfo = $"Last: {latest.FileName} ({latest.Status})",
                    DateText = (latest.CompletedUtc ?? latest.CreatedUtc).ToLocalTime().ToString("g"),
                    Status = latest.Status.ToString(),
                    StatusGlyph = isSuccess ? "\xE73E" : "\xE10A",
                    StatusBrush = isSuccess
                        ? new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136))
                        : new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102)),
                    IconBg = isSuccess
                        ? new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136))
                        : new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 255, 51, 102))
                });
            }
        }

        private void HistoryItemsList_ItemClick(object sender, ItemClickEventArgs e)
        {
            if (e.ClickedItem is HistoryItemViewModel clickedItem)
            {
                this.Frame.Navigate(typeof(TransferStreamView), clickedItem);
            }
        }
    }

    public class HistoryItemViewModel : System.ComponentModel.INotifyPropertyChanged
    {
        private string deviceName = string.Empty;
        public string DeviceName
        {
            get => deviceName;
            set { deviceName = value; OnPropertyChanged(nameof(DeviceName)); }
        }

        private string deviceType = string.Empty;
        public string DeviceType
        {
            get => deviceType;
            set { deviceType = value; OnPropertyChanged(nameof(DeviceType)); }
        }

        private string deviceIconGlyph = string.Empty;
        public string DeviceIconGlyph
        {
            get => deviceIconGlyph;
            set { deviceIconGlyph = value; OnPropertyChanged(nameof(DeviceIconGlyph)); }
        }

        private int fileCount;
        public int FileCount
        {
            get => fileCount;
            set { fileCount = value; OnPropertyChanged(nameof(FileCount)); OnPropertyChanged(nameof(TransfersCountText)); }
        }

        public string TransfersCountText => $"{FileCount} file{(FileCount == 1 ? "" : "s")} transferred";

        private string totalBytesText = "0 MB";
        public string TotalBytesText
        {
            get => totalBytesText;
            set { totalBytesText = value; OnPropertyChanged(nameof(TotalBytesText)); }
        }

        private string lastTransferInfo = string.Empty;
        public string LastTransferInfo
        {
            get => lastTransferInfo;
            set { lastTransferInfo = value; OnPropertyChanged(nameof(LastTransferInfo)); }
        }

        private string dateText = string.Empty;
        public string DateText
        {
            get => dateText;
            set { dateText = value; OnPropertyChanged(nameof(DateText)); }
        }

        private string status = string.Empty;
        public string Status
        {
            get => status;
            set { status = value; OnPropertyChanged(nameof(Status)); }
        }

        private string statusGlyph = string.Empty;
        public string StatusGlyph
        {
            get => statusGlyph;
            set { statusGlyph = value; OnPropertyChanged(nameof(StatusGlyph)); }
        }

        private Microsoft.UI.Xaml.Media.Brush? statusBrush;
        public Microsoft.UI.Xaml.Media.Brush? StatusBrush
        {
            get => statusBrush;
            set { statusBrush = value; OnPropertyChanged(nameof(StatusBrush)); }
        }

        private Microsoft.UI.Xaml.Media.Brush? iconBg;
        public Microsoft.UI.Xaml.Media.Brush? IconBg
        {
            get => iconBg;
            set { iconBg = value; OnPropertyChanged(nameof(IconBg)); }
        }

        public event System.ComponentModel.PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged(string prop) => PropertyChanged?.Invoke(this, new System.ComponentModel.PropertyChangedEventArgs(prop));
    }
}
