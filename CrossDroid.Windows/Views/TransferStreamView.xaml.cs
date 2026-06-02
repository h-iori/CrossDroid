using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Threading.Tasks;
using CrossDroid.Windows.Backend;

namespace CrossDroid.Windows.Views
{
    public sealed partial class TransferStreamView : Page
    {
        public ObservableCollection<TransferBubbleViewModel> ChatBubbles { get; set; } = new();
        private DispatcherTimer? _refreshTimer;

        public TransferStreamView()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            string deviceName = "Unknown Device";
            int totalFiles = 0;

            if (e.Parameter is TransferNavParameter navParam)
            {
                deviceName = navParam.TargetDeviceName;
                
                var targetDevice = App.Backend.Devices.TrustedDevices.FirstOrDefault(d => d.AliasOrName == deviceName)
                    ?? App.Backend.Devices.Devices.FirstOrDefault(d => d.AliasOrName == deviceName);

                if (targetDevice != null && App.Backend.Staging.Items.Count > 0)
                {
                    _ = App.Backend.Transfers.StartSendAsync(targetDevice, App.Backend.Staging.Items.ToList());
                    App.Backend.Staging.Clear();
                }

                LoadActiveQueueData();
                totalFiles = ChatBubbles.Count;
            }
            else if (e.Parameter is HistoryItemViewModel historyItem)
            {
                deviceName = historyItem.DeviceName;
                LoadHistoryData(deviceName);
                totalFiles = ChatBubbles.Count;
            }
            else
            {
                LoadActiveQueueData();
                totalFiles = ChatBubbles.Count;
            }

            _refreshTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(250) };
            _refreshTimer.Tick += (_, _) => 
            {
                if (e.Parameter is HistoryItemViewModel) return;
                LoadActiveQueueData();
                PeerStatusText.Text = $"Transferring... {ChatBubbles.Count(c => c.IsCompleted)} of {ChatBubbles.Count} files";
                TotalSpeedText.Text = $"{StagedTransferItem.FormatBytes((long)App.Backend.Transfers.Queue.Sum(q => q.SpeedBytesPerSecond))}/s";
            };
            _refreshTimer.Start();

            PeerDeviceNameText.Text = deviceName;
            PeerStatusText.Text = $"Transferring 0 of {totalFiles} files";
            TotalSpeedText.Text = $"{StagedTransferItem.FormatBytes((long)App.Backend.Transfers.Queue.Sum(q => q.SpeedBytesPerSecond))}/s";
        }

        private void BackButton_Click(object sender, RoutedEventArgs e)
        {
            if (this.Frame.CanGoBack)
            {
                this.Frame.GoBack();
            }
        }

        private void LoadSelectedFiles(System.Collections.Generic.List<StorageFileItem> files)
        {
            ChatBubbles.Clear();
            if (files != null)
            {
                foreach (var file in files)
                {
                    double sizeInMb = file.Size / 1024.0 / 1024.0;
                    string sizeStr = sizeInMb > 1000 ? $"{(sizeInMb / 1024.0):F2} GB" : $"{sizeInMb:F2} MB";

                    ChatBubbles.Add(new TransferBubbleViewModel
                    {
                        IsOutgoing = true,
                        FileName = file.Name,
                        FileSize = sizeStr,
                        ProgressValue = 0,
                        StatusText = "Staged",
                        SpeedText = "0.0 MB/s",
                        IconGlyph = "\xE7C3", // Document/File icon
                        IsActive = true,
                        IsCompleted = false
                    });
                }
            }
        }

        private void LoadHistoryData(string deviceName)
        {
            ChatBubbles.Clear();
            foreach (var record in App.Backend.History.Records.Where(h => h.DeviceName == deviceName).OrderBy(h => h.CreatedUtc))
            {
                ChatBubbles.Add(new TransferBubbleViewModel
                {
                    IsOutgoing = record.Direction == TransferDirection.Outgoing,
                    FileName = record.FileName,
                    FileSize = StagedTransferItem.FormatBytes(record.TotalBytes),
                    ProgressValue = record.Status == TransferStatus.Completed ? 100 : 0,
                    StatusText = record.Status.ToString(),
                    SpeedText = "",
                    IconGlyph = record.IsFolder ? "\xE8B7" : "\xE7C3",
                    IsActive = false,
                    IsCompleted = record.Status == TransferStatus.Completed
                });
            }
        }

        private void LoadActiveQueueData()
        {
            var activeItems = App.Backend.Transfers.Queue.OrderBy(q => q.CreatedUtc).ToList();
            
            // Remove old bubbles
            for (int i = ChatBubbles.Count - 1; i >= 0; i--)
            {
                if (!activeItems.Any(a => a.FileName == ChatBubbles[i].FileName))
                {
                    ChatBubbles.RemoveAt(i);
                }
            }

            // Update or Add
            foreach (var record in activeItems)
            {
                var existing = ChatBubbles.FirstOrDefault(b => b.FileName == record.FileName);
                if (existing != null)
                {
                    existing.ProgressValue = record.ProgressPercent;
                    existing.StatusText = record.StatusText;
                    existing.SpeedText = record.SpeedText;
                    existing.IsActive = record.Status is TransferStatus.Transferring or TransferStatus.Paused or TransferStatus.Queued;
                    existing.IsCompleted = record.Status == TransferStatus.Completed;
                }
                else
                {
                    ChatBubbles.Add(new TransferBubbleViewModel
                    {
                        IsOutgoing = record.Direction == TransferDirection.Outgoing,
                        FileName = record.FileName,
                        FileSize = record.SizeText,
                        ProgressValue = record.ProgressPercent,
                        StatusText = record.StatusText,
                        SpeedText = record.SpeedText,
                        IconGlyph = record.IsFolder ? "\xE8B7" : "\xE7C3",
                        IsActive = record.Status is TransferStatus.Transferring or TransferStatus.Paused or TransferStatus.Queued,
                        IsCompleted = record.Status == TransferStatus.Completed
                    });
                }
            }
        }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            base.OnNavigatedFrom(e);
            _refreshTimer?.Stop();
            _refreshTimer = null;
        }
    }

    public class TransferBubbleViewModel : INotifyPropertyChanged
    {
        public bool IsOutgoing { get; set; }
        public HorizontalAlignment BubbleAlignment => IsOutgoing ? HorizontalAlignment.Right : HorizontalAlignment.Left;
        
        // Use standard properties for DataBinding
        public CornerRadius BubbleCornerRadius => IsOutgoing ? new CornerRadius(16, 16, 4, 16) : new CornerRadius(16, 16, 16, 4);

        private string fileName = "";
        public string FileName
        {
            get => fileName;
            set { fileName = value; OnPropertyChanged(nameof(FileName)); }
        }

        private string fileSize = "";
        public string FileSize
        {
            get => fileSize;
            set { fileSize = value; OnPropertyChanged(nameof(FileSize)); }
        }

        private double progressValue;
        public double ProgressValue
        {
            get => progressValue;
            set { progressValue = value; OnPropertyChanged(nameof(ProgressValue)); }
        }

        private string statusText = "";
        public string StatusText
        {
            get => statusText;
            set { statusText = value; OnPropertyChanged(nameof(StatusText)); }
        }

        private string speedText = "";
        public string SpeedText
        {
            get => speedText;
            set { speedText = value; OnPropertyChanged(nameof(SpeedText)); }
        }

        private string iconGlyph = "";
        public string IconGlyph
        {
            get => iconGlyph;
            set { iconGlyph = value; OnPropertyChanged(nameof(IconGlyph)); }
        }

        private bool isActive;
        public bool IsActive
        {
            get => isActive;
            set { isActive = value; OnPropertyChanged(nameof(IsActive)); OnPropertyChanged(nameof(ActiveUIVisibility)); OnPropertyChanged(nameof(InactiveUIVisibility)); }
        }

        private bool isCompleted;
        public bool IsCompleted
        {
            get => isCompleted;
            set { isCompleted = value; OnPropertyChanged(nameof(IsCompleted)); }
        }

        public Visibility ActiveUIVisibility => IsActive ? Visibility.Visible : Visibility.Collapsed;
        public Visibility InactiveUIVisibility => !IsActive ? Visibility.Visible : Visibility.Collapsed;
        
        public Microsoft.UI.Xaml.Media.Brush AccentBrush => IsOutgoing ? 
            (Microsoft.UI.Xaml.Application.Current.Resources["NeonHighlight"] as Microsoft.UI.Xaml.Media.Brush)! : 
            (Microsoft.UI.Xaml.Application.Current.Resources["AccentCyan"] as Microsoft.UI.Xaml.Media.Brush)!;

        public event PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged(string prop) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(prop));
    }

    public class TransferBubbleTemplateSelector : DataTemplateSelector
    {
        public DataTemplate? OutgoingTemplate { get; set; }
        public DataTemplate? IncomingTemplate { get; set; }

        protected override DataTemplate? SelectTemplateCore(object item)
        {
            if (item is TransferBubbleViewModel vm)
            {
                return vm.IsOutgoing ? OutgoingTemplate : IncomingTemplate;
            }
            return base.SelectTemplateCore(item);
        }

        protected override DataTemplate? SelectTemplateCore(object item, DependencyObject container)
        {
            return SelectTemplateCore(item);
        }
    }
}
