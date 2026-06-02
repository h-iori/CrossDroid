using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using Windows.Storage;
using CrossDroid.Windows.Backend;

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

            // Apply Mica backdrop programmatically so that unpackaged (dotnet run) mode
            // does not crash with STATUS_FAIL_FAST_EXCEPTION when the XAML parser tries
            // to instantiate MicaBackdrop before the package identity is available.
            try
            {
                if (Microsoft.UI.Composition.SystemBackdrops.MicaController.IsSupported())
                {
                    this.SystemBackdrop = new Microsoft.UI.Xaml.Media.MicaBackdrop
                    {
                        Kind = Microsoft.UI.Composition.SystemBackdrops.MicaKind.BaseAlt
                    };
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Mica backdrop unavailable: {ex.Message}");
            }

            // Completely remove native window borders (WS_BORDER, WS_THICKFRAME, WS_CAPTION, WS_DLGFRAME) to avoid any white border artifacts
            try
            {
                uint style = User32.GetWindowLong(hwnd, -16); // -16 = GWL_STYLE
                style &= ~(0x00800000U | 0x00040000U | 0x00C00000U | 0x00400000U); // WS_BORDER, WS_THICKFRAME, WS_CAPTION, WS_DLGFRAME
                User32.SetWindowLong(hwnd, -16, style);
                User32.SetWindowPos(hwnd, IntPtr.Zero, 0, 0, 0, 0, 0x0002 | 0x0001 | 0x0004 | 0x0010 | 0x0020); // SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Failed to remove native window borders: {ex.Message}");
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

            // Auto-hide feature removed per user request

            LoadBackendHistory();

            // Start bottom bar pulsing animation
            try
            {
                if (RootGrid.Resources.TryGetValue("BottomBarPulse", out var bottomBarPulseObj) && bottomBarPulseObj is Microsoft.UI.Xaml.Media.Animation.Storyboard bottomBarPulse)
                {
                    bottomBarPulse.Begin();
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Failed to start bottom bar pulsing animation: {ex.Message}");
            }

            // Navigate to home on startup and position the window near the tray
            NavigateToPage("Home");
            PositionWindowNearTray();
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

        private void LoadBackendHistory()
        {
            HistoryRecords.Clear();
            foreach (var group in App.Backend.History.Records.GroupBy(h => h.DeviceName))
            {
                var latest = group.OrderByDescending(h => h.CompletedUtc ?? h.CreatedUtc).First();
                var isSuccess = latest.Status == TransferStatus.Completed;
                HistoryRecords.Add(new Views.HistoryItemViewModel
                {
                    DeviceName = group.Key,
                    DeviceType = "DEVICE",
                    DeviceIconGlyph = "\xE8EA",
                    FileCount = group.Count(),
                    TotalBytesText = StagedTransferItem.FormatBytes(group.Sum(h => h.TotalBytes)),
                    Status = latest.Status.ToString(),
                    DateText = (latest.CompletedUtc ?? latest.CreatedUtc).ToLocalTime().ToString("g"),
                    LastTransferInfo = $"Last: {latest.FileName} ({latest.Status})",
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

        public void AddHistoryRecord(string deviceName, string fileName, string fileSize, string status, string message)
        {
            bool isSuccess = status == "Success";
            
            // Find existing device history item
            var deviceRecord = HistoryRecords.FirstOrDefault(r => r.DeviceName.Equals(deviceName, StringComparison.OrdinalIgnoreCase));
            
            if (deviceRecord == null)
            {
                deviceRecord = new Views.HistoryItemViewModel
                {
                    DeviceName = deviceName,
                    DeviceType = deviceName.Contains("Tab", StringComparison.OrdinalIgnoreCase) ? "TABLET" : (deviceName.Contains("PC", StringComparison.OrdinalIgnoreCase) || deviceName.Contains("Desktop", StringComparison.OrdinalIgnoreCase) ? "PC" : "PHONE"),
                    DeviceIconGlyph = deviceName.Contains("Tab", StringComparison.OrdinalIgnoreCase) ? "\xE70B" : (deviceName.Contains("PC", StringComparison.OrdinalIgnoreCase) || deviceName.Contains("Desktop", StringComparison.OrdinalIgnoreCase) ? "\xE7F4" : "\xE8EA"),
                    FileCount = 0,
                    TotalBytesText = "0 MB"
                };
                // Add to list (newest device at top)
                HistoryRecords.Insert(0, deviceRecord);
            }

            deviceRecord.FileCount++;
            deviceRecord.LastTransferInfo = $"Last: {fileName} ({status})";
            deviceRecord.DateText = DateTime.Now.ToString("g");
            deviceRecord.Status = status;
            deviceRecord.StatusGlyph = isSuccess ? "\xE73E" : "\xE10A";
            deviceRecord.StatusBrush = isSuccess 
                ? new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 0, 255, 136))
                : new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(255, 255, 51, 102));
            deviceRecord.IconBg = isSuccess
                ? new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 0, 255, 136))
                : new Microsoft.UI.Xaml.Media.SolidColorBrush(global::Windows.UI.Color.FromArgb(51, 255, 51, 102));
            
            deviceRecord.TotalBytesText = AddSizes(deviceRecord.TotalBytesText, fileSize);
        }

        private string AddSizes(string currentText, string sizeToAdd)
        {
            double val1 = ParseSizeToMB(currentText);
            double val2 = ParseSizeToMB(sizeToAdd);
            double total = val1 + val2;
            if (total >= 1024)
            {
                return $"{total / 1024.0:F1} GB";
            }
            return $"{total:F1} MB";
        }

        private double ParseSizeToMB(string sizeText)
        {
            if (string.IsNullOrWhiteSpace(sizeText)) return 0;
            try
            {
                string clean = sizeText.Trim().ToUpper();
                if (clean.EndsWith("GB"))
                {
                    double.TryParse(clean.Replace("GB", "").Trim(), out double val);
                    return val * 1024.0;
                }
                if (clean.EndsWith("MB"))
                {
                    double.TryParse(clean.Replace("MB", "").Trim(), out double val);
                    return val;
                }
                if (clean.EndsWith("KB"))
                {
                    double.TryParse(clean.Replace("KB", "").Trim(), out double val);
                    return val / 1024.0;
                }
                double.TryParse(clean, out double valDirect);
                return valDirect;
            }
            catch
            {
                return 0;
            }
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
                "About" => typeof(Views.AboutView),
                _ => typeof(Views.HomeView)
            };

            if (pageType != null && ContentFrame.CurrentSourcePageType != pageType)
            {
                ContentFrame.Navigate(pageType);
            }

            // Sync the active style on bottom navigation bar
            UpdateBottomNavSelection(pageTag);
        }

        private void AnimateDoubleProperty(DependencyObject target, string propertyPath, double toValue, double durationMs, Microsoft.UI.Xaml.Media.Animation.EasingFunctionBase? easing = null)
        {
            var anim = new Microsoft.UI.Xaml.Media.Animation.DoubleAnimation
            {
                To = toValue,
                Duration = TimeSpan.FromMilliseconds(durationMs),
                EasingFunction = easing
            };
            Microsoft.UI.Xaml.Media.Animation.Storyboard.SetTarget(anim, target);
            Microsoft.UI.Xaml.Media.Animation.Storyboard.SetTargetProperty(anim, propertyPath);
            var sb = new Microsoft.UI.Xaml.Media.Animation.Storyboard();
            sb.Children.Add(anim);
            sb.Begin();
        }

        private void UpdateBottomNavSelection(string selectedTag)
        {
            if (HomeIcon == null) return; // UI not fully initialized yet

            var activeBrush = (Microsoft.UI.Xaml.Media.SolidColorBrush)Application.Current.Resources["NeonHighlight"];
            var inactiveBrush = (Microsoft.UI.Xaml.Media.SolidColorBrush)Application.Current.Resources["TextMuted"];
            var textActiveBrush = (Microsoft.UI.Xaml.Media.SolidColorBrush)Application.Current.Resources["TextStrong"];
            var textInactiveBrush = (Microsoft.UI.Xaml.Media.SolidColorBrush)Application.Current.Resources["TextSecondary"];

            // Spring-like easing functions
            var tabBouncyEasing = new Microsoft.UI.Xaml.Media.Animation.BackEase { Amplitude = 0.35, EasingMode = Microsoft.UI.Xaml.Media.Animation.EasingMode.EaseOut };
            var smoothEasing = new Microsoft.UI.Xaml.Media.Animation.QuadraticEase { EasingMode = Microsoft.UI.Xaml.Media.Animation.EasingMode.EaseInOut };

            void SetTabState(FontIcon icon, Microsoft.UI.Xaml.Media.ScaleTransform iconScale, TextBlock text, Border pill, bool isActive)
            {
                // Set colors and font weight
                icon.Foreground = isActive ? activeBrush : inactiveBrush;
                text.Foreground = isActive ? textActiveBrush : textInactiveBrush;
                text.FontWeight = isActive ? Microsoft.UI.Text.FontWeights.SemiBold : Microsoft.UI.Text.FontWeights.Medium;

                // Animate icon scale
                AnimateDoubleProperty(iconScale, "ScaleX", isActive ? 1.18 : 1.0, 300, tabBouncyEasing);
                AnimateDoubleProperty(iconScale, "ScaleY", isActive ? 1.18 : 1.0, 300, tabBouncyEasing);

                // Animate selection pill opacity
                AnimateDoubleProperty(pill, "Opacity", isActive ? 1.0 : 0.0, 200, smoothEasing);
            }

            SetTabState(HomeIcon, HomeIconScale, HomeText, HomePill, selectedTag == "Home");
            SetTabState(DevicesIcon, DevicesIconScale, DevicesText, DevicesPill, selectedTag == "Devices");
            SetTabState(HistoryIcon, HistoryIconScale, HistoryText, HistoryPill, selectedTag == "History");
            SetTabState(SettingsIcon, SettingsIconScale, SettingsText, SettingsPill, selectedTag == "Settings");
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
            _ = App.Backend.Staging.StageStorageItemsAsync(items);
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
            _ = App.Backend.Staging.StagePathAsync(folder.Path);
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
            Environment.Exit(0);
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

        [System.Runtime.InteropServices.DllImport("dwmapi.dll", PreserveSig = true)]
        public static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref uint attrValue, int attrSize);

        [System.Runtime.InteropServices.DllImport("user32.dll", EntryPoint = "GetWindowLongW")]
        public static extern uint GetWindowLong(IntPtr hWnd, int nIndex);

        [System.Runtime.InteropServices.DllImport("user32.dll", EntryPoint = "SetWindowLongW")]
        public static extern uint SetWindowLong(IntPtr hWnd, int nIndex, uint dwNewLong);

        [System.Runtime.InteropServices.DllImport("user32.dll")]
        [return: System.Runtime.InteropServices.MarshalAs(System.Runtime.InteropServices.UnmanagedType.Bool)]
        public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);
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
