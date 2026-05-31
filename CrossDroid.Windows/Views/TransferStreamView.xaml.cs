using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Threading.Tasks;

namespace CrossDroid.Windows.Views
{
    public sealed partial class TransferStreamView : Page
    {
        public ObservableCollection<TransferBubbleViewModel> ChatBubbles { get; set; } = new();

        public TransferStreamView()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            string deviceName = "Unknown Device";
            if (e.Parameter is HistoryItemViewModel historyItem)
            {
                deviceName = historyItem.DeviceName;
            }

            PeerDeviceNameText.Text = deviceName;
            PeerStatusText.Text = "Transferring 2 of 4 files";
            TotalSpeedText.Text = "12.4 MB/s";

            LoadMockData();
            SimulateProgress();
        }

        private void BackButton_Click(object sender, RoutedEventArgs e)
        {
            if (this.Frame.CanGoBack)
            {
                this.Frame.GoBack();
            }
        }

        private void LoadMockData()
        {
            ChatBubbles.Add(new TransferBubbleViewModel
            {
                IsOutgoing = true,
                FileName = "cyberpunk_assets.zip",
                FileSize = "1.2 GB",
                ProgressValue = 100,
                StatusText = "Completed",
                SpeedText = "",
                IconGlyph = "\xE814", // Folder/Zip
                IsActive = false,
                IsCompleted = true
            });

            ChatBubbles.Add(new TransferBubbleViewModel
            {
                IsOutgoing = false,
                FileName = "IMG_8842.JPG",
                FileSize = "4.5 MB",
                ProgressValue = 100,
                StatusText = "Completed",
                SpeedText = "",
                IconGlyph = "\xEB9F", // Image
                IsActive = false,
                IsCompleted = true
            });

            ChatBubbles.Add(new TransferBubbleViewModel
            {
                IsOutgoing = true,
                FileName = "NeonCity_4K_Render.mp4",
                FileSize = "850 MB",
                ProgressValue = 45,
                StatusText = "Sending 45%",
                SpeedText = "8.2 MB/s",
                IconGlyph = "\xE714", // Video
                IsActive = true,
                IsCompleted = false
            });

            ChatBubbles.Add(new TransferBubbleViewModel
            {
                IsOutgoing = false,
                FileName = "Project_Specs.pdf",
                FileSize = "12 MB",
                ProgressValue = 12,
                StatusText = "Receiving 12%",
                SpeedText = "4.2 MB/s",
                IconGlyph = "\xEA90", // Document
                IsActive = true,
                IsCompleted = false
            });
        }

        private async void SimulateProgress()
        {
            // Simple simulation for the active items
            while (true)
            {
                await Task.Delay(1000);
                
                foreach (var bubble in ChatBubbles)
                {
                    if (bubble.IsActive && bubble.ProgressValue < 100)
                    {
                        bubble.ProgressValue += 5;
                        if (bubble.ProgressValue >= 100)
                        {
                            bubble.ProgressValue = 100;
                            bubble.IsActive = false;
                            bubble.IsCompleted = true;
                            bubble.StatusText = "Completed";
                            bubble.SpeedText = "";
                        }
                        else
                        {
                            bubble.StatusText = (bubble.IsOutgoing ? "Sending " : "Receiving ") + bubble.ProgressValue + "%";
                        }
                    }
                }
            }
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
