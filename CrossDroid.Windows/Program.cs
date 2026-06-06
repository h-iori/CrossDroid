using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.Windows.AppLifecycle;
using System;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using Windows.ApplicationModel.Activation;

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
            
            // Write debug info
            var debugInfo = $"[{DateTime.Now}] IsCurrent: {keyInstance.IsCurrent}, RegisteredInstance PID: {keyInstance.ProcessId}, Current PID: {Environment.ProcessId}";
            Debug.WriteLine(debugInfo);
            
            // Optional --verbose file logging
            if (args.Contains("--verbose"))
            {
                try
                {
                    var logDir = System.IO.Path.Combine(
                        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                        "IoriStudios", "CrossDroid");
                    System.IO.Directory.CreateDirectory(logDir);
                    System.IO.File.AppendAllText(System.IO.Path.Combine(logDir, "debug.log"), debugInfo + "\n");
                }
                catch { }
            }

            if (keyInstance.IsCurrent)
            {
                // Hook activation redirection
                keyInstance.Activated += OnInstanceActivated;
                
                // Start application
                Application.Start((p) => new App());
            }
            else
            {
                // Write our actual command line args to a temp file for the primary instance to read.
                // WinUI 3's GetActivatedEventArgs often fails to capture command-line args for Win32 CreateProcess launches.
                try
                {
                    var argsToWrite = Environment.GetCommandLineArgs();
                    if (argsToWrite.Length > 1)
                    {
                        var tempDir = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "IoriStudios", "CrossDroid", "Activation");
                        System.IO.Directory.CreateDirectory(tempDir);
                        var tempFile = System.IO.Path.Combine(tempDir, $"cmd_{Guid.NewGuid():N}.txt");
                        System.IO.File.WriteAllLines(tempFile, argsToWrite);
                    }
                }
                catch { }

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
                string[] cmdArgs = Array.Empty<string>();
                
                // 1. Try reading from temp files first (contains actual Win32 args)
                try
                {
                    var tempDir = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "IoriStudios", "CrossDroid", "Activation");
                    if (System.IO.Directory.Exists(tempDir))
                    {
                        var files = System.IO.Directory.GetFiles(tempDir, "cmd_*.txt");
                        foreach (var file in files)
                        {
                            try
                            {
                                var lines = System.IO.File.ReadAllLines(file);
                                System.IO.File.Delete(file);
                                if (lines.Length > 1)
                                {
                                    // Environment.GetCommandLineArgs() includes the executable path at index 0.
                                    // We skip it so ProcessCommandLineArgsAsync only sees the arguments.
                                    cmdArgs = lines.Skip(1).ToArray();
                                    break;
                                }
                            }
                            catch { }
                        }
                    }
                }
                catch { }

                // 2. Fallback to WinRT args if temp file wasn't found
                if (cmdArgs.Length == 0)
                {
                    if (args.Kind == ExtendedActivationKind.Launch)
                    {
                        if (args.Data is global::Windows.ApplicationModel.Activation.LaunchActivatedEventArgs launchArgs)
                        {
                            cmdArgs = SplitCommandLine(launchArgs.Arguments);
                        }
                    }
                    else if (args.Kind == ExtendedActivationKind.CommandLineLaunch)
                    {
                        if (args.Data is global::Windows.ApplicationModel.Activation.CommandLineActivatedEventArgs cmdLineArgs)
                        {
                            cmdArgs = SplitCommandLine(cmdLineArgs.Operation.Arguments);
                        }
                    }
                }

                // Enqueue on UI thread
                App.MainWindowInstance.DispatcherQueue.TryEnqueue(async () =>
                {
                    if (cmdArgs.Length > 0)
                    {
                        await App.ProcessCommandLineArgsAsync(cmdArgs);
                    }

                    // Bring main window to front
                    // ProcessCommandLineArgsAsync already handles Radar navigation when files are present,
                    // so we only need to show the window here for bare re-launch scenarios
                    App.MainWindowInstance.AppWindow.Show();
                    var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(App.MainWindowInstance);
                    User32.SetForegroundWindow(hwnd);
                });
            }
        }

        [DllImport("shell32.dll", SetLastError = true)]
        private static extern IntPtr CommandLineToArgvW(
            [MarshalAs(UnmanagedType.LPWStr)] string lpCmdLine,
            out int pNumArgs);

        [DllImport("kernel32.dll")]
        private static extern IntPtr LocalFree(IntPtr hMem);

        private static string[] SplitCommandLine(string commandLine)
        {
            if (string.IsNullOrWhiteSpace(commandLine))
                return Array.Empty<string>();

            IntPtr argv = CommandLineToArgvW(commandLine, out int argc);
            if (argv == IntPtr.Zero)
                return Array.Empty<string>();

            try
            {
                var args = new string[argc];
                for (int i = 0; i < argc; i++)
                {
                    IntPtr p = Marshal.ReadIntPtr(argv, i * IntPtr.Size);
                    args[i] = Marshal.PtrToStringUni(p)!;
                }
                return args;
            }
            finally
            {
                LocalFree(argv);
            }
        }
    }
}
