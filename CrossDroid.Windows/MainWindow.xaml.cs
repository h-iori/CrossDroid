using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using Windows.Storage;

namespace CrossDroid.Windows
{
    public sealed partial class MainWindow : Window
    {
        public List<IStorageItem> StagedFilesList { get; } = new();
        public IStorageFolder? StagedFolderInstance { get; set; }
        public ObservableCollection<Views.HistoryItemViewModel> HistoryRecords { get; } = new();
        public bool IsPickingFile { get; set; } = false;

        public MainWindow()
        {
            this.InitializeComponent();

            // Configure window as borderless popup and hide from Alt+Tab switcher/taskbar
            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(this);
            var windowId = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(hwnd);
            var appWindow = Microsoft.UI.Windowing.AppWindow.GetFromWindowId(windowId);
            
            appWindow.IsShownInSwitchers = false;

            if (appWindow.Presenter is Microsoft.UI.Windowing.OverlappedPresenter presenter)
            {
                presenter.SetBorderAndTitleBar(false, false);
                presenter.IsMinimizable = false;
                presenter.IsMaximizable = false;
                presenter.IsResizable = false;
                presenter.IsAlwaysOnTop = true;
            }

            // Set window icon safely using absolute path to prevent startup crashes
            try
            {
                var iconPath = System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", "AppIcon.ico");
                if (System.IO.File.Exists(iconPath))
                {
                    appWindow.SetIcon(iconPath);
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Failed to set window icon: {ex.Message}");
            }

            // Register window closing event
            appWindow.Closing += AppWindow_Closing;

            // Bind tray commands to left click and double click
            TrayIcon.LeftClickCommand = new RelayCommand(ToggleOrRestoreWindow);
            TrayIcon.DoubleClickCommand = new RelayCommand(ToggleOrRestoreWindow);

            // Load tray icon programmatically to avoid H.NotifyIcon thread-marshalling COMException bug
            try
            {
                var iconPath = System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", "AppIcon.ico");
                if (System.IO.File.Exists(iconPath))
                {
                    TrayIcon.Icon = new global::System.Drawing.Icon(iconPath);
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Failed to load tray icon: {ex.Message}");
            }

            // Hook window activated event for auto-hiding when losing focus
            this.Activated += MainWindow_Activated;

            // Load initial history items
            LoadMockHistory();

            // Navigate to home on startup and position the window near the tray
            NavigateToPage("Home");
            PositionWindowNearTray();
        }

        private void MainWindow_Activated(object sender, WindowActivatedEventArgs args)
        {
            if (args.WindowActivationState == WindowActivationState.Deactivated)
            {
                // Auto-hide the window when focus is lost, unless we are currently picking a file/folder
                if (!IsPickingFile)
                {
                    this.AppWindow.Hide();
                }
            }
        }

        private void PositionWindowNearTray()
        {
            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(this);
            
            // Calculate sizes based on DPI
            uint dpi = User32.GetDpiForWindow(hwnd);
            if (dpi == 0) dpi = 96; // fallback
            double scale = dpi / 96.0;

            int width = (int)(380 * scale);
            int height = (int)(640 * scale);
            int margin = (int)(12 * scale);

            // Get screen coordinates where the tray click occurred (cursor position)
            POINT cursorPt;
            if (!User32.GetCursorPos(out cursorPt))
            {
                cursorPt = new POINT { X = 0, Y = 0 };
            }

            IntPtr hMonitor = User32.MonitorFromPoint(cursorPt, 2); // MONITOR_DEFAULTTONEAREST

            MONITORINFO monitorInfo = new MONITORINFO();
            monitorInfo.cbSize = System.Runtime.InteropServices.Marshal.SizeOf(typeof(MONITORINFO));
            if (User32.GetMonitorInfo(hMonitor, ref monitorInfo))
            {
                var rcWork = monitorInfo.rcWork;
                var rcMonitor = monitorInfo.rcMonitor;

                int x = 0;
                int y = 0;

                // Determine taskbar position by comparing work area to monitor bounds
                if (rcWork.Bottom < rcMonitor.Bottom) // Taskbar at the bottom
                {
                    x = rcWork.Right - width - margin;
                    y = rcWork.Bottom - height - margin;
                }
                else if (rcWork.Top > rcMonitor.Top) // Taskbar at the top
                {
                    x = rcWork.Right - width - margin;
                    y = rcWork.Top + margin;
                }
                else if (rcWork.Left > rcMonitor.Left) // Taskbar at the left
                {
                    x = rcWork.Left + margin;
                    y = rcWork.Bottom - height - margin;
                }
                else if (rcWork.Right < rcMonitor.Right) // Taskbar at the right
                {
                    x = rcWork.Right - width - margin;
                    y = rcWork.Bottom - height - margin;
                }
                else // Fallback
                {
                    x = rcWork.Right - width - margin;
                    y = rcWork.Bottom - height - margin;
                }

                // Move and resize the window
                this.AppWindow.MoveAndResize(new global::Windows.Graphics.RectInt32(x, y, width, height));
            }
        }

        private void LoadMockHistory()
        {
            HistoryRecords.Add(new Views.HistoryItemViewModel
            {
                FileName = "photo_gallery.zip",
                FileSize = "156.4 MB",
                Status = "Success",
                DateText = "Today, 10:15 AM",
                DetailMessage = "Received from Pixel 8 Pro",
                StatusGlyph = "\xE73E", // Checkmark
                StatusBrush = new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136)),
                IconBg = new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136)),
                OpenVisible = Visibility.Visible
            });
            HistoryRecords.Add(new Views.HistoryItemViewModel
            {
                FileName = "Presentation.pdf",
                FileSize = "4.2 MB",
                Status = "Success",
                DateText = "Yesterday, 3:30 PM",
                DetailMessage = "Sent to Galaxy Tab S9",
                StatusGlyph = "\xE73E",
                StatusBrush = new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136)),
                IconBg = new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136)),
                OpenVisible = Visibility.Visible
            });
            HistoryRecords.Add(new Views.HistoryItemViewModel
            {
                FileName = "raw_video_footage.mov",
                FileSize = "1.8 GB",
                Status = "Failed",
                DateText = "May 28, 11:20 AM",
                DetailMessage = "Connection lost during transfer",
                StatusGlyph = "\xE10A", // Cancel X
                StatusBrush = new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102)),
                IconBg = new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 255, 51, 102)),
                OpenVisible = Visibility.Collapsed
            });
        }

        public void AddHistoryRecord(string fileName, string fileSize, string status, string message)
        {
            bool isSuccess = status == "Success";
            HistoryRecords.Add(new Views.HistoryItemViewModel
            {
                FileName = fileName,
                FileSize = fileSize,
                Status = status,
                DateText = DateTime.Now.ToString("g"),
                DetailMessage = isSuccess ? $"Transferred successfully" : message,
                StatusGlyph = isSuccess ? "\xE73E" : "\xE10A",
                StatusBrush = isSuccess 
                    ? new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136))
                    : new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102)),
                IconBg = isSuccess
                    ? new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136))
                    : new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 255, 51, 102)),
                OpenVisible = isSuccess ? Visibility.Visible : Visibility.Collapsed
            });
        }

        private void AppWindow_Closing(Microsoft.UI.Windowing.AppWindow sender, Microsoft.UI.Windowing.AppWindowClosingEventArgs args)
        {
            if (App.CloseToTray)
            {
                args.Cancel = true;
                sender.Hide();
            }
            else
            {
                TrayIcon.Dispose();
            }
        }

        public void NavigateToPage(string pageTag)
        {
            Type? pageType = pageTag switch
            {
                "Home" => typeof(Views.HomeView),
                "Devices" => typeof(Views.DevicesView),
                "Transfers" => typeof(Views.TransfersView),
                "History" => typeof(Views.HistoryView),
                "Settings" => typeof(Views.SettingsView),
                _ => typeof(Views.HomeView)
            };

            if (pageType != null && ContentFrame.CurrentSourcePageType != pageType)
            {
                ContentFrame.Navigate(pageType);
            }

            // Sync the active style on bottom navigation bar
            UpdateBottomNavSelection(pageTag);
        }

        private void UpdateBottomNavSelection(string selectedTag)
        {
            if (HomeIcon == null) return; // UI not fully initialized yet

            var activeBrush = (Microsoft.UI.Xaml.Media.SolidColorBrush)Application.Current.Resources["NeonPrimary"];
            var inactiveBrush = (Microsoft.UI.Xaml.Media.SolidColorBrush)Application.Current.Resources["TextSecondary"];

            void SetState(FontIcon icon, TextBlock text, Border indicator, bool isActive)
            {
                icon.Foreground = isActive ? activeBrush : inactiveBrush;
                text.Foreground = isActive ? activeBrush : inactiveBrush;
                indicator.Visibility = isActive ? Visibility.Visible : Visibility.Collapsed;
            }

            SetState(HomeIcon, HomeText, HomeIndicator, selectedTag == "Home");
            SetState(DevicesIcon, DevicesText, DevicesIndicator, selectedTag == "Devices");
            SetState(HistoryIcon, HistoryText, HistoryIndicator, selectedTag == "History");
            SetState(SettingsIcon, SettingsText, SettingsIndicator, selectedTag == "Settings");
        }

        private void NavTab_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag)
            {
                NavigateToPage(tag);
            }
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            this.AppWindow.Hide();
        }

        // File Staging
        public void StageTransfers(IReadOnlyList<IStorageItem> items)
        {
            StagedFilesList.AddRange(items);
            NavigateToPage("Transfers");

            // If we are currently showing the Transfers view, reload the list
            if (ContentFrame.Content is Views.TransfersView transfersView)
            {
                transfersView.LoadStagedItems();
            }
        }

        public void StageFolder(IStorageFolder folder)
        {
            StagedFolderInstance = folder;
            NavigateToPage("Transfers");

            if (ContentFrame.Content is Views.TransfersView transfersView)
            {
                transfersView.LoadStagedItems();
            }
        }

        // Tray Events
        private void TrayIcon_DoubleClick(object sender, RoutedEventArgs e)
        {
            ToggleOrRestoreWindow();
        }

        private void TrayOpen_Click(object sender, RoutedEventArgs e)
        {
            RestoreWindow();
        }

        private async void TrayToggleReceive_Click(object sender, RoutedEventArgs e)
        {
            App.AutoAcceptTrusted = !App.AutoAcceptTrusted;
            var dialog = new ContentDialog
            {
                Title = "Receive Mode Changed",
                Content = App.AutoAcceptTrusted ? "Receive Mode: Auto-Accept Enabled" : "Receive Mode: Standard prompt verification",
                CloseButtonText = "OK",
                XamlRoot = this.ContentFrame.XamlRoot
            };
            await dialog.ShowAsync();
        }

        private void TrayDevices_Click(object sender, RoutedEventArgs e)
        {
            NavigateToPage("Devices");
            RestoreWindow();
        }

        private void TrayExit_Click(object sender, RoutedEventArgs e)
        {
            App.CloseToTray = false;
            TrayIcon.Dispose();
            this.Close();
        }

        public void RestoreWindow()
        {
            // Force home view show
            NavigateToPage("Home");
            PositionWindowNearTray();
            this.AppWindow.Show();
            
            // Set focus using Win32 API to bring to front
            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(this);
            User32.SetForegroundWindow(hwnd);
        }

        public void ToggleOrRestoreWindow()
        {
            if (this.AppWindow.IsVisible)
            {
                this.AppWindow.Hide();
            }
            else
            {
                RestoreWindow();
            }
        }
    }

    // P/Invoke structs and helpers
    internal static class User32
    {
        [System.Runtime.InteropServices.DllImport("user32.dll")]
        [return: System.Runtime.InteropServices.MarshalAs(System.Runtime.InteropServices.UnmanagedType.Bool)]
        public static extern bool SetForegroundWindow(IntPtr hWnd);

        [System.Runtime.InteropServices.DllImport("user32.dll")]
        public static extern bool GetCursorPos(out POINT lpPoint);

        [System.Runtime.InteropServices.DllImport("user32.dll")]
        public static extern IntPtr MonitorFromPoint(POINT pt, uint dwFlags);

        [System.Runtime.InteropServices.DllImport("user32.dll", CharSet = System.Runtime.InteropServices.CharSet.Auto)]
        public static extern bool GetMonitorInfo(IntPtr hMonitor, ref MONITORINFO lpmi);

        [System.Runtime.InteropServices.DllImport("user32.dll")]
        public static extern uint GetDpiForWindow(IntPtr hwnd);
    }

    [System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Sequential)]
    public struct POINT
    {
        public int X;
        public int Y;
    }

    [System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Sequential, CharSet = System.Runtime.InteropServices.CharSet.Auto)]
    public struct MONITORINFO
    {
        public int cbSize;
        public RECT rcMonitor;
        public RECT rcWork;
        public uint dwFlags;
    }

    [System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Sequential)]
    public struct RECT
    {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    // Simple implementation of ICommand for DoubleClickCommand
    public class RelayCommand : System.Windows.Input.ICommand
    {
        private readonly Action _execute;
        public RelayCommand(Action execute) => _execute = execute;
        public bool CanExecute(object? parameter) => true;
        public void Execute(object? parameter) => _execute();
        public event EventHandler? CanExecuteChanged { add { } remove { } }
    }
}
