using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.Windows.AppLifecycle;
using System;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;

namespace CrossDroid.Windows
{
    public static class Program
    {
        [DllImport("Microsoft.ui.xaml.dll")]
        private static extern void XamlCheckProcessRequirements();

        [STAThread]
        static void Main(string[] args)
        {
            XamlCheckProcessRequirements();
            
            // Register/find single instance key
            var keyInstance = AppInstance.FindOrRegisterForKey("CrossDroidWindowsCompanionInstance");
            
            // Write debug info to log
            try
            {
                string logContent = $"[{DateTime.Now}] IsCurrent: {keyInstance.IsCurrent}, RegisteredInstance PID: {keyInstance.ProcessId}, Current PID: {Environment.ProcessId}\n";
                System.IO.File.AppendAllText(@"c:\Users\harsh\OneDrive\Desktop\CrossDroid\debug.log", logContent);
            }
            catch {}

            if (keyInstance.IsCurrent)
            {
                // Hook activation redirection
                keyInstance.Activated += OnInstanceActivated;
                
                // Start application
                Application.Start((p) => new App());
            }
            else
            {
                // Redirect arguments and exit
                var currentArgs = AppInstance.GetCurrent().GetActivatedEventArgs();
                RedirectActivation(keyInstance, currentArgs);
            }
        }

        private static void RedirectActivation(AppInstance primaryInstance, AppActivationArguments currentArgs)
        {
            // Redirect asynchronously and wait
            var redirectTask = primaryInstance.RedirectActivationToAsync(currentArgs).AsTask();
            redirectTask.Wait();
        }

        private static void OnInstanceActivated(object? sender, AppActivationArguments args)
        {
            if (App.MainWindowInstance != null)
            {
                // Enqueue on UI thread
                App.MainWindowInstance.DispatcherQueue.TryEnqueue(() =>
                {
                    // Bring main window to front
                    App.MainWindowInstance.AppWindow.Show();
                    var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(App.MainWindowInstance);
                    User32.SetForegroundWindow(hwnd);
                    
                    // If files were shared, we navigate to the transfers queue view
                    App.MainWindowInstance.NavigateToPage("Transfers");
                });
            }
        }
    }
}
