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
            foreach (var item in records)
            {
                DisplayedItems.Add(item);
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
