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

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace CrossDroid.Windows;

/// <summary>
/// Provides application-specific behavior to supplement the default Application class.
/// </summary>
    public partial class App : Application
    {
        public static MainWindow MainWindowInstance { get; private set; } = null!;

        // Global Configuration Settings (Stored in memory for the UI shell)
        public static bool AutoStartEnabled { get; set; } = true;
        public static bool StartMinimized { get; set; } = false;
        public static bool CloseToTray { get; set; } = true;
        public static bool AutoAcceptTrusted { get; set; } = true;
        public static bool WifiOnly { get; set; } = true;
        public static bool P2pFallback { get; set; } = true;
        public static bool ToastNotify { get; set; } = true;
        public static bool SoundNotify { get; set; } = true;
        public static string DownloadsDirectory { get; set; } = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "Downloads", "CrossDroid");

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
        protected override async void OnLaunched(Microsoft.UI.Xaml.LaunchActivatedEventArgs args)
        {
            MainWindowInstance = new MainWindow();
            
            // Check command line arguments
            string[] cmdArgs = Environment.GetCommandLineArgs();
            bool runMinimized = cmdArgs.Contains("--minimized") || cmdArgs.Contains("-m") || StartMinimized;
            
            // Collect any file paths passed via explorer context menu
            var filesToShare = new List<string>();
            foreach (var arg in cmdArgs.Skip(1)) // Skip executable path
            {
                if (arg == "--minimized" || arg == "-m") continue;
                
                if (System.IO.File.Exists(arg) || System.IO.Directory.Exists(arg))
                {
                    filesToShare.Add(arg);
                }
            }

            if (filesToShare.Count > 0)
            {
                // Stage files for sharing
                var storageItems = new List<IStorageItem>();
                foreach (var path in filesToShare)
                {
                    try
                    {
                        if (System.IO.Directory.Exists(path))
                        {
                            var folder = await StorageFolder.GetFolderFromPathAsync(path);
                            MainWindowInstance.StageFolder(folder);
                        }
                        else
                        {
                            var file = await StorageFile.GetFileFromPathAsync(path);
                            storageItems.Add(file);
                        }
                    }
                    catch (Exception ex)
                    {
                        System.Diagnostics.Debug.WriteLine($"Failed to load path: {path}. Error: {ex.Message}");
                    }
                }

                if (storageItems.Count > 0)
                {
                    MainWindowInstance.StageTransfers(storageItems);
                }
                
                // Show the window since the user explicitly started a share action
                MainWindowInstance.Activate();
            }
            else if (runMinimized)
            {
                // Start minimized to system tray: create window but do not call Activate/Show
                MainWindowInstance.AppWindow.Hide();
            }
            else
            {
                MainWindowInstance.Activate();
            }
        }
    }
