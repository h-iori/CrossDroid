using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using System;
using System.Collections.ObjectModel;
using System.Linq;

namespace CrossDroid.Windows.Views
{
    public sealed partial class TransfersView : Page
    {
        public ObservableCollection<TransferItemViewModel> QueueItems { get; set; } = new();
        public ObservableCollection<StagedItemViewModel> StagedItems { get; set; } = new();
        
        private DispatcherTimer? _simTimer;
        private Random _rand = new();
        private bool _isPaused = false;

        public TransfersView()
        {
            this.InitializeComponent();
            
            QueueItemsList.ItemsSource = QueueItems;
            StagedItemsList.ItemsSource = StagedItems;
            
            this.Loaded += TransfersView_Loaded;
            this.Unloaded += TransfersView_Unloaded;

            TargetDeviceCombo.ItemsSource = new string[] { "Pixel 8 Pro (Active)", "Galaxy Tab S9 (Offline)" };
            if (TargetDeviceCombo.Items.Count > 0)
            {
                TargetDeviceCombo.SelectedIndex = 0;
            }
        }

        private void TransfersView_Loaded(object sender, RoutedEventArgs e)
        {
            // Load app staged files
            LoadStagedItems();
            
            // Set up simulator timer
            _simTimer = new DispatcherTimer();
            _simTimer.Interval = TimeSpan.FromMilliseconds(500);
            _simTimer.Tick += SimTimer_Tick;

            if (QueueItems.Count == 0 && StagedItems.Count == 0)
            {
                QueueItems.Add(new TransferItemViewModel
                {
                    FileName = "work_presentation.pptx",
                    FileSize = "24.5 MB",
                    BytesTotal = 24 * 1024 * 1024 + 500 * 1024,
                    ProgressValue = 68,
                    Status = "Transferring",
                    IconGlyph = "\xE7C3", 
                    StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 39, 215, 231)), 
                    PauseGlyph = "\xE103" 
                });
                QueueItems.Add(new TransferItemViewModel
                {
                    FileName = "family_photo_39.jpg",
                    FileSize = "3.2 MB",
                    BytesTotal = 3 * 1024 * 1024 + 200 * 1024,
                    ProgressValue = 100,
                    Status = "Completed",
                    IconGlyph = "\xE114", 
                    StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136)), 
                    PauseGlyph = "\xE768" 
                });
                QueueItems.Add(new TransferItemViewModel
                {
                    FileName = "crossdroid_archive.zip",
                    FileSize = "340.8 MB",
                    BytesTotal = 340 * 1024 * 1024 + 800 * 1024,
                    ProgressValue = 15,
                    Status = "Paused",
                    IconGlyph = "\xE838", 
                    StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 248, 184, 78)), 
                    PauseGlyph = "\xE768" 
                });
                
                // Start simulator timer automatically
                _simTimer.Start();
            }
        }

        private void TransfersView_Unloaded(object sender, RoutedEventArgs e)
        {
            _simTimer?.Stop();
        }

        public void LoadStagedItems()
        {
            StagedItems.Clear();
            foreach (var item in App.MainWindowInstance.StagedFilesList)
            {
                StagedItems.Add(new StagedItemViewModel { Name = item.Name, Path = item.Path });
            }
            if (App.MainWindowInstance.StagedFolderInstance != null)
            {
                StagedItems.Add(new StagedItemViewModel 
                { 
                    Name = $"[Folder] {App.MainWindowInstance.StagedFolderInstance.Name}", 
                    Path = App.MainWindowInstance.StagedFolderInstance.Path 
                });
            }

            if (StagedItems.Count > 0 && TransfersPivot != null)
            {
                TransfersPivot.SelectedIndex = 1;
            }
        }

        private void SimTimer_Tick(object? sender, object e)
        {
            if (_isPaused) return;

            bool anyActive = false;
            long totalBytes = 0;
            long totalTransferred = 0;

            foreach (var item in QueueItems)
            {
                if (item.Status == "Transferring")
                {
                    anyActive = true;
                    item.ProgressValue += _rand.Next(2, 8);
                    if (item.ProgressValue >= 100)
                    {
                        item.ProgressValue = 100;
                        item.Status = "Completed";
                        item.StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136));
                        item.PauseGlyph = "\xE768"; // Play (shows it can't pause anymore)
                        
                        // Add to history list in App
                        App.MainWindowInstance.AddHistoryRecord(item.FileName, item.FileSize, "Success", "Transferred successfully");
                    }
                }

                totalBytes += item.BytesTotal;
                totalTransferred += (long)(item.BytesTotal * (item.ProgressValue / 100.0));
            }

            if (anyActive)
            {
                double overallPercent = (double)totalTransferred / totalBytes * 100.0;
                OverallProgressBar.Value = overallPercent;
                OverallProgressText.Text = $"{overallPercent:F0}%";
                QueueStatusText.Text = "Sending...";
                SpeedText.Text = $"Speed: {(_rand.NextDouble() * 12 + 18):F1} MB/s";
                
                int remainingSeconds = (int)((totalBytes - totalTransferred) / (25 * 1024 * 1024)); // Assume 25MB/s
                TimeRemainingText.Text = $"Time remaining: {remainingSeconds}s";
                TransferredItemsText.Text = $"Items: {QueueItems.Count(i => i.Status == "Completed")} / {QueueItems.Count}";
            }
            else
            {
                _simTimer?.Stop();
                QueueStatusText.Text = "Idle";
                SpeedText.Text = "Speed: 0.0 MB/s";
                TimeRemainingText.Text = "Time remaining: --:--";
                OverallProgressBar.Value = QueueItems.Count > 0 ? 100 : 0;
                OverallProgressText.Text = QueueItems.Count > 0 ? "100%" : "0%";
            }
        }

        private void SimulateIncoming_Click(object sender, RoutedEventArgs e)
        {
            IncomingOverlay.Visibility = Visibility.Visible;
        }

        private void PauseAll_Click(object sender, RoutedEventArgs e)
        {
            _isPaused = !_isPaused;
            if (_isPaused)
            {
                QueueStatusText.Text = "Paused";
                SpeedText.Text = "Speed: 0.0 MB/s";
                foreach (var item in QueueItems)
                {
                    if (item.Status == "Transferring")
                    {
                        item.Status = "Paused";
                        item.StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 248, 184, 78));
                    }
                }
            }
            else
            {
                foreach (var item in QueueItems)
                {
                    if (item.Status == "Paused")
                    {
                        item.Status = "Transferring";
                        item.StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 39, 215, 231));
                    }
                }
                _simTimer?.Start();
            }
        }

        private void CancelAll_Click(object sender, RoutedEventArgs e)
        {
            _simTimer?.Stop();
            QueueItems.Clear();
            OverallProgressBar.Value = 0;
            OverallProgressText.Text = "0%";
            QueueStatusText.Text = "Idle";
            SpeedText.Text = "Speed: 0.0 MB/s";
            TimeRemainingText.Text = "Time remaining: --:--";
            TransferredItemsText.Text = "Items: 0 / 0";
        }

        private void RejectIncoming_Click(object sender, RoutedEventArgs e)
        {
            IncomingOverlay.Visibility = Visibility.Collapsed;
        }

        private void AcceptIncoming_Click(object sender, RoutedEventArgs e)
        {
            IncomingOverlay.Visibility = Visibility.Collapsed;
            
            // Add a mock incoming download
            var item = new TransferItemViewModel
            {
                FileName = "vacation_video.mp4",
                FileSize = "45.0 MB",
                BytesTotal = 45 * 1024 * 1024,
                ProgressValue = 0,
                Status = "Transferring",
                IconGlyph = "\xE714", // Video
                StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 39, 215, 231)) // Cyan
            };
            QueueItems.Add(item);
            _simTimer?.Start();
        }

        private void StartTransfer_Click(object sender, RoutedEventArgs e)
        {
            if (StagedItems.Count == 0) return;

            foreach (var staged in StagedItems)
            {
                bool isFolder = staged.Name.StartsWith("[Folder]");
                QueueItems.Add(new TransferItemViewModel
                {
                    FileName = staged.Name,
                    FileSize = isFolder ? "Folder Struct" : "12.4 MB",
                    BytesTotal = 12 * 1024 * 1024,
                    ProgressValue = 0,
                    Status = "Transferring",
                    IconGlyph = isFolder ? "\xE8B7" : "\xE8A5", // Folder vs File icon
                    StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 39, 215, 231))
                });
            }

            // Clear staged
            StagedItems.Clear();
            App.MainWindowInstance.StagedFilesList.Clear();
            App.MainWindowInstance.StagedFolderInstance = null;

            if (TransfersPivot != null)
            {
                TransfersPivot.SelectedIndex = 0;
            }

            _simTimer?.Start();
        }

        private void RemoveStaged_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is StagedItemViewModel vm)
            {
                StagedItems.Remove(vm);
                
                // Clear from main window state as well
                var f = App.MainWindowInstance.StagedFilesList.FirstOrDefault(x => x.Path == vm.Path);
                if (f != null) App.MainWindowInstance.StagedFilesList.Remove(f);
                if (App.MainWindowInstance.StagedFolderInstance?.Path == vm.Path) App.MainWindowInstance.StagedFolderInstance = null;
            }
        }

        private void ItemPause_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is TransferItemViewModel vm)
            {
                if (vm.Status == "Transferring")
                {
                    vm.Status = "Paused";
                    vm.StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 248, 184, 78));
                    vm.PauseGlyph = "\xE768"; // Play icon
                }
                else if (vm.Status == "Paused")
                {
                    vm.Status = "Transferring";
                    vm.StatusBrush = new SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 39, 215, 231));
                    vm.PauseGlyph = "\xE103"; // Pause icon
                    _simTimer?.Start();
                }
            }
        }

        private void ItemCancel_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.DataContext is TransferItemViewModel vm)
            {
                QueueItems.Remove(vm);
                if (vm.Status != "Completed")
                {
                    App.MainWindowInstance.AddHistoryRecord(vm.FileName, vm.FileSize, "Cancelled", "User cancelled transfer");
                }
            }
        }
    }

    public class TransferItemViewModel : System.ComponentModel.INotifyPropertyChanged
    {
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

        private string pauseGlyph = "\xE103"; // Pause icon
        public string PauseGlyph
        {
            get => pauseGlyph;
            set { pauseGlyph = value; OnPropertyChanged(nameof(PauseGlyph)); }
        }

        public event System.ComponentModel.PropertyChangedEventHandler? PropertyChanged;
        private void OnPropertyChanged(string prop) => PropertyChanged?.Invoke(this, new System.ComponentModel.PropertyChangedEventArgs(prop));
    }

    public class StagedItemViewModel
    {
        public string Name { get; set; } = string.Empty;
        public string Path { get; set; } = string.Empty;
    }
}
