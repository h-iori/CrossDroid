using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using CrossDroid.Windows.Backend;

namespace CrossDroid.Windows.Views
{
    public sealed partial class TransfersView : Page
    {
        public ObservableCollection<TransferItemViewModel> QueueItems { get; set; } = new();
        public ObservableCollection<StagedItemViewModel> StagedItems { get; set; } = new();

        private DispatcherTimer? _refreshTimer;

        public TransfersView()
        {
            this.InitializeComponent();

            QueueItemsList.ItemsSource = QueueItems;
            StagedItemsList.ItemsSource = StagedItems;

            this.Loaded += TransfersView_Loaded;
            this.Unloaded += TransfersView_Unloaded;
        }

        private void TransfersView_Loaded(object sender, RoutedEventArgs e)
        {
            RefreshDeviceTargets();
            LoadStagedItems();
            RefreshQueueItems();

            _refreshTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(250) };
            _refreshTimer.Tick += (_, _) => RefreshQueueItems();
            _refreshTimer.Start();
        }

        private void TransfersView_Unloaded(object sender, RoutedEventArgs e)
        {
            _refreshTimer?.Stop();
            _refreshTimer = null;
        }

        private void RefreshDeviceTargets()
        {
            TargetDeviceCombo.ItemsSource = App.Backend.Devices.TrustedDevices.Select(d => d.AliasOrName).ToArray();
            if (TargetDeviceCombo.Items.Count > 0)
            {
                TargetDeviceCombo.SelectedIndex = 0;
            }
        }

        public void LoadStagedItems()
        {
            StagedItems.Clear();
            foreach (var item in App.Backend.Staging.Items)
            {
                StagedItems.Add(new StagedItemViewModel
                {
                    Name = item.IsFolder ? $"[Folder] {item.Name}" : item.Name,
                    Path = item.Path,
                    SizeText = item.SizeText
                });
            }

            if (StagedItems.Count > 0 && TransfersPivot != null)
            {
                TransfersPivot.SelectedIndex = 1;
            }
        }

        private void RefreshQueueItems()
        {
            QueueItems.Clear();
            foreach (var item in App.Backend.Transfers.Queue.OrderByDescending(q => q.CreatedUtc))
            {
                QueueItems.Add(TransferItemViewModel.FromRecord(item));
            }

            // Toggle empty state
            QueueEmptyState.Visibility = QueueItems.Count == 0 ? Visibility.Visible : Visibility.Collapsed;

            long totalBytes = App.Backend.Transfers.Queue.Sum(q => q.TotalBytes);
            long transferred = App.Backend.Transfers.Queue.Sum(q => q.BytesTransferred);
            double overall = totalBytes > 0 ? transferred * 100d / totalBytes : 0;
            OverallProgressBar.Value = overall;
            OverallProgressText.Text = $"{overall:F0}%";
            QueueStatusText.Text = App.Backend.Transfers.Queue.Any(q => q.Status == TransferStatus.Transferring)
                ? "Transferring"
                : App.Backend.Transfers.Queue.Any(q => q.Status == TransferStatus.Paused) ? "Paused" : "Idle";
            SpeedText.Text = $"Speed: {StagedTransferItem.FormatBytes((long)App.Backend.Transfers.Queue.Sum(q => q.SpeedBytesPerSecond))}/s";

            // Calculate aggregate ETA
            double totalEta = App.Backend.Transfers.Queue
                .Where(q => q.Status == TransferStatus.Transferring && q.SpeedBytesPerSecond > 0)
                .Sum(q => q.EstimatedSecondsRemaining);
            TimeRemainingText.Text = totalEta > 0 ? $"Remaining: {TimeSpan.FromSeconds(totalEta):mm\\:ss}" : "Remaining: --:--";

            TransferredItemsText.Text = $"Items: {App.Backend.Transfers.Queue.Count(q => q.Status == TransferStatus.Completed)} / {App.Backend.Transfers.Queue.Count}";
        }

        private void PauseAll_Click(object sender, RoutedEventArgs e)
        {
            bool anyTransferring = App.Backend.Transfers.Queue.Any(q => q.Status == TransferStatus.Transferring);
            foreach (var item in App.Backend.Transfers.Queue)
            {
                if (anyTransferring && item.Status == TransferStatus.Transferring)
                {
                    App.Backend.Transfers.Pause(item.TransferId);
                }
                else if (!anyTransferring && item.Status == TransferStatus.Paused)
                {
                    App.Backend.Transfers.Resume(item.TransferId);
                }
            }
            RefreshQueueItems();
        }

        private void CancelAll_Click(object sender, RoutedEventArgs e)
        {
            foreach (var item in App.Backend.Transfers.Queue.Where(q => q.Status is TransferStatus.Transferring or TransferStatus.Paused or TransferStatus.Queued).ToList())
            {
                App.Backend.Transfers.Cancel(item.TransferId);
            }
            RefreshQueueItems();
        }

        private async void StartTransfer_Click(object sender, RoutedEventArgs e)
        {
            var selectedName = TargetDeviceCombo.SelectedItem as string;
            var targetDevice = App.Backend.Devices.TrustedDevices.FirstOrDefault(d => d.AliasOrName == selectedName)
                ?? App.Backend.Devices.TrustedDevices.FirstOrDefault();
            if (targetDevice == null)
            {
                await ShowErrorAsync("No trusted device", "Pair or enable a trusted receiver before starting a transfer.");
                return;
            }

            if (App.Backend.Staging.Items.Count == 0)
            {
                await ShowErrorAsync("Nothing staged", "Choose files or folders before starting a transfer.");
                return;
            }

            await App.Backend.Transfers.StartSendAsync(targetDevice, App.Backend.Staging.Items.ToList());
            App.Backend.Staging.Clear();
            LoadStagedItems();
            TransfersPivot.SelectedIndex = 0;
            RefreshQueueItems();
        }

        private void RemoveStaged_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is StagedItemViewModel vm)
            {
                var item = App.Backend.Staging.Items.FirstOrDefault(x => x.Path == vm.Path);
                if (item != null)
                {
                    App.Backend.Staging.Remove(item);
                }
                LoadStagedItems();
            }
        }

        private void ItemPause_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is TransferItemViewModel vm)
            {
                var transfer = App.Backend.Transfers.Queue.FirstOrDefault(q => q.TransferId == vm.TransferId);
                if (transfer == null) return;
                if (transfer.Status == TransferStatus.Paused)
                {
                    App.Backend.Transfers.Resume(transfer.TransferId);
                }
                else if (transfer.Status == TransferStatus.Transferring)
                {
                    App.Backend.Transfers.Pause(transfer.TransferId);
                }
            }
        }

        private void ItemCancel_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is TransferItemViewModel vm)
            {
                App.Backend.Transfers.Cancel(vm.TransferId);
            }
        }

        private async void ItemRetry_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is TransferItemViewModel vm)
            {
                var record = App.Backend.Transfers.Queue.FirstOrDefault(q => q.TransferId == vm.TransferId);
                if (record != null)
                {
                    await App.Backend.Transfers.RetryAsync(record);
                    RefreshQueueItems();
                }
            }
        }

        private async System.Threading.Tasks.Task ShowErrorAsync(string title, string message)
        {
            var dialog = new ContentDialog
            {
                Title = title,
                Content = message,
                CloseButtonText = "OK",
                XamlRoot = this.XamlRoot
            };
            await dialog.ShowAsync();
        }
    }

    public class TransferItemViewModel : System.ComponentModel.INotifyPropertyChanged
    {
        public string TransferId { get; set; } = string.Empty;
        public string FileName { get; set; } = string.Empty;
        public string FileSize { get; set; } = string.Empty;
        public long BytesTotal { get; set; }

        private double progressValue;
        public double ProgressValue
        {
            get => progressValue;
            set
            {
                progressValue = value;
                OnPropertyChanged(nameof(ProgressValue));
                OnPropertyChanged(nameof(ProgressPercentText));
            }
        }

        public string ProgressPercentText => $"{ProgressValue:F0}%";

        private string status = string.Empty;
        public string Status
        {
            get => status;
            set { status = value; OnPropertyChanged(nameof(Status)); }
        }

        private Brush? statusBrush;
        public Brush? StatusBrush
        {
            get => statusBrush;
            set { statusBrush = value; OnPropertyChanged(nameof(StatusBrush)); }
        }

        public string IconGlyph { get; set; } = string.Empty;

        private string pauseGlyph = "\xE103";
        public string PauseGlyph
        {
            get => pauseGlyph;
            set { pauseGlyph = value; OnPropertyChanged(nameof(PauseGlyph)); }
        }

        private string deviceName = "";
        public string DeviceName
        {
            get => deviceName;
            set { deviceName = value; OnPropertyChanged(nameof(DeviceName)); }
        }

        public Visibility RetryVisibility { get; set; } = Visibility.Collapsed;

        public static TransferItemViewModel FromRecord(TransferRecord record)
        {
            var isFailed = record.Status is TransferStatus.Failed or TransferStatus.Cancelled;
            return new TransferItemViewModel
            {
                TransferId = record.TransferId,
                FileName = record.FileName,
                FileSize = record.SizeText,
                BytesTotal = record.TotalBytes,
                ProgressValue = record.ProgressPercent,
                Status = record.StatusText,
                IconGlyph = record.IsFolder ? "\xE8B7" : "\xE8A5",
                StatusBrush = StatusToBrush(record.Status),
                PauseGlyph = record.Status == TransferStatus.Paused ? "\xE768" : "\xE103",
                DeviceName = record.DeviceName,
                RetryVisibility = isFailed ? Visibility.Visible : Visibility.Collapsed
            };
        }

        private static Brush StatusToBrush(TransferStatus status)
        {
            return status switch
            {
                TransferStatus.Completed => new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136)),
                TransferStatus.Failed or TransferStatus.Cancelled => new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102)),
                TransferStatus.Paused => new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 248, 184, 78)),
                _ => new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 39, 215, 231))
            };
        }

        public event System.ComponentModel.PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged(string prop) => PropertyChanged?.Invoke(this, new System.ComponentModel.PropertyChangedEventArgs(prop));
    }

    public class StagedItemViewModel
    {
        public string Name { get; set; } = string.Empty;
        public string Path { get; set; } = string.Empty;
        public string SizeText { get; set; } = string.Empty;
    }
}
