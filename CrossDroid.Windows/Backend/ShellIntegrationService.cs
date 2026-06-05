using Microsoft.Win32;
using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using Windows.ApplicationModel;

namespace CrossDroid.Windows.Backend;

public sealed class ShellIntegrationService
{
    private readonly SettingsService _settings;

    public ShellIntegrationService(SettingsService settings)
    {
        _settings = settings;
    }

    /// <summary>
    /// MSIX-native auto-start using Windows.ApplicationModel.StartupTask API.
    /// Requires a matching <desktop:Extension Category="windows.startupTask"> in Package.appxmanifest.
    /// </summary>
    public async Task EnsureAutoStartAsync(bool enable)
    {
        try
        {
            var startupTask = await StartupTask.GetAsync("CrossDroidStartup");
            
            if (enable)
            {
                switch (startupTask.State)
                {
                    case StartupTaskState.Disabled:
                        var newState = await startupTask.RequestEnableAsync();
                        Debug.WriteLine($"StartupTask.RequestEnableAsync returned: {newState}");
                        break;
                    case StartupTaskState.DisabledByUser:
                        // User disabled it via Task Manager — we cannot re-enable programmatically
                        Debug.WriteLine("Startup was disabled by user via Task Manager. Cannot re-enable programmatically.");
                        break;
                    case StartupTaskState.DisabledByPolicy:
                        Debug.WriteLine("Startup is disabled by system policy.");
                        break;
                    case StartupTaskState.Enabled:
                    case StartupTaskState.EnabledByPolicy:
                        // Already enabled
                        break;
                }
            }
            else
            {
                if (startupTask.State == StartupTaskState.Enabled)
                {
                    startupTask.Disable();
                    Debug.WriteLine("StartupTask disabled.");
                }
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Failed to manage MSIX StartupTask: {ex.Message}");
            // Fallback: try registry approach for unpackaged development builds
            EnsureAutoStartFallback(enable);
        }
    }

    /// <summary>
    /// Registry fallback for unpackaged development builds only.
    /// </summary>
    private void EnsureAutoStartFallback(bool enable)
    {
        try
        {
            var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", true);
            if (key == null) return;

            if (enable)
            {
                var exePath = Process.GetCurrentProcess().MainModule?.FileName;
                if (!string.IsNullOrEmpty(exePath))
                {
                    key.SetValue("CrossDroid", $"\"{exePath}\" --minimized");
                }
            }
            else
            {
                key.DeleteValue("CrossDroid", false);
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Failed to update AutoStart via registry fallback: {ex.Message}");
        }
    }

    public void EnsureContextMenu(bool enable)
    {
        try
        {
            var baseKey = Registry.CurrentUser.CreateSubKey(@"Software\Classes\*\shell\CrossDroid");
            if (enable)
            {
                baseKey.SetValue("", "Share using CrossDroid");
                baseKey.SetValue("Icon", Process.GetCurrentProcess().MainModule?.FileName ?? "");
                var cmdKey = baseKey.CreateSubKey("command");
                
                var exePath = Process.GetCurrentProcess().MainModule?.FileName;
                if (!string.IsNullOrEmpty(exePath))
                {
                    cmdKey.SetValue("", $"\"{exePath}\" --send \"%1\"");
                }
            }
            else
            {
                Registry.CurrentUser.DeleteSubKeyTree(@"Software\Classes\*\shell\CrossDroid", false);
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Failed to update Context Menu: {ex.Message}");
        }
    }
}
