using Windows.ApplicationModel;
using Windows.ApplicationModel.Activation;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.Storage;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Navigation;
using Microsoft.UI.Xaml.Shapes;
using CrossDroid.Windows.Backend;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace CrossDroid.Windows;

/// <summary>
/// Provides application-specific behavior to supplement the default Application class.
/// </summary>
    public partial class App : Application
    {
        public static MainWindow MainWindowInstance { get; private set; } = null!;

        public static CrossDroidBackend Backend => CrossDroidBackend.Current;

        public static bool AutoStartEnabled { get => Backend.Settings.Current.AutoStartEnabled; set => Backend.Settings.Current.AutoStartEnabled = value; }
        public static bool StartMinimized { get => Backend.Settings.Current.StartMinimized; set => Backend.Settings.Current.StartMinimized = value; }
        public static bool CloseToTray { get => Backend.Settings.Current.CloseToTray; set => Backend.Settings.Current.CloseToTray = value; }
        public static bool AutoAcceptTrusted { get => Backend.Settings.Current.AutoAcceptTrusted; set => Backend.Settings.Current.AutoAcceptTrusted = value; }
        public static bool WifiOnly { get => Backend.Settings.Current.WifiOnly; set => Backend.Settings.Current.WifiOnly = value; }
        public static bool P2pFallback { get => Backend.Settings.Current.P2pFallback; set => Backend.Settings.Current.P2pFallback = value; }
        public static bool ToastNotify { get => Backend.Settings.Current.ToastNotify; set => Backend.Settings.Current.ToastNotify = value; }
        public static bool SoundNotify { get => Backend.Settings.Current.SoundNotify; set => Backend.Settings.Current.SoundNotify = value; }
        public static string DownloadsDirectory { get => Backend.Settings.Current.DownloadsDirectory; set => Backend.Settings.Current.DownloadsDirectory = value; }

        /// <summary>
        /// Initializes the singleton application object.  This is the first line of authored code
        /// executed, and as such is the logical equivalent of main() or WinMain().
        /// </summary>
        public App()
        {
            InitializeComponent();
        }

        /// <summary>
        /// Invoked when the application is launched.
        /// </summary>
        /// <param name="args">Details about the launch request and process.</param>
        protected override void OnLaunched(Microsoft.UI.Xaml.LaunchActivatedEventArgs args)
        {
            try
            {
                CrossDroidBackend.InitializeAsync().GetAwaiter().GetResult();
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Backend initialization failed: {ex.Message}");
            }

            MainWindowInstance = new MainWindow();
            
            // Check command line arguments
            string[] cmdArgs = Environment.GetCommandLineArgs();
            
            bool hasFilesToShare = cmdArgs.Contains("--send") || cmdArgs.Skip(1).Any(arg => 
                arg != "--minimized" && arg != "-m" && arg != "--hidden" && arg != "--verbose" && 
                (System.IO.File.Exists(arg) || System.IO.Directory.Exists(arg)));

            bool runMinimized = (cmdArgs.Contains("--minimized") || cmdArgs.Contains("-m") || cmdArgs.Contains("--hidden") || StartMinimized) && !hasFilesToShare;
            
            if (runMinimized)
            {
                // Start minimized to system tray: create window but do not call Activate/Show
                MainWindowInstance.AppWindow.Hide();
            }
            else
            {
                MainWindowInstance.Activate();
            }

            // Process command line arguments on the dispatcher queue to ensure the window and UI thread are fully ready
            MainWindowInstance.DispatcherQueue.TryEnqueue(async () =>
            {
                try
                {
                    await ProcessCommandLineArgsAsync(cmdArgs.Skip(1).ToArray());
                }
                catch (Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine($"Failed to process command line arguments: {ex.Message}");
                }
            });
        }

        public static async Task ProcessCommandLineArgsAsync(string[] cmdArgs)
        {
            // Collect any file paths passed via explorer context menu (--send "filepath" or bare paths)
            var filesToShare = new List<string>();
            for (int i = 0; i < cmdArgs.Length; i++)
            {
                var arg = cmdArgs[i];
                if (arg == "--minimized" || arg == "-m" || arg == "--hidden" || arg == "--verbose") continue;
                
                if (arg == "--send")
                {
                    // Next arg is the file path
                    if (i + 1 < cmdArgs.Length)
                    {
                        i++;
                        var sendPath = cmdArgs[i];
                        if (System.IO.File.Exists(sendPath) || System.IO.Directory.Exists(sendPath))
                        {
                            filesToShare.Add(sendPath);
                        }
                    }
                    continue;
                }
                
                if (System.IO.File.Exists(arg) || System.IO.Directory.Exists(arg))
                {
                    filesToShare.Add(arg);
                }
            }

            if (filesToShare.Count > 0)
            {
                LogShareDebug($"Staging {filesToShare.Count} path(s) for sharing");

                // Stage all paths using System.IO-based staging (works reliably in all contexts,
                // unlike StorageFile.GetFileFromPathAsync which throws UnauthorizedAccessException
                // on MSIX cold start)
                foreach (var path in filesToShare)
                {
                    try
                    {
                        await Backend.Staging.StagePathAsync(path);
                        LogShareDebug($"Staged: {path}");
                    }
                    catch (Exception ex)
                    {
                        LogShareDebug($"Failed to stage path: {path}. Error: {ex.Message}");
                        System.Diagnostics.Debug.WriteLine($"Failed to stage path: {path}. Error: {ex.Message}");
                    }
                }

                // ALWAYS show window and navigate to Radar when files were requested for sharing,
                // even if some staging calls failed — the Radar screen should still appear
                MainWindowInstance.AppWindow.Show();
                MainWindowInstance.Activate();

                var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(MainWindowInstance);
                User32.SetForegroundWindow(hwnd);

                MainWindowInstance.NavigateToPage("Radar");
                LogShareDebug("Navigated to Radar screen");
            }
        }

        /// <summary>
        /// Writes a debug log entry to the CrossDroid log file for diagnosing cold-start share issues.
        /// </summary>
        private static void LogShareDebug(string message)
        {
            try
            {
                var logDir = System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "IoriStudios", "CrossDroid");
                System.IO.Directory.CreateDirectory(logDir);
                System.IO.File.AppendAllText(
                    System.IO.Path.Combine(logDir, "share-debug.log"),
                    $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss.fff}] {message}\n");
            }
            catch { }
        }
    }
