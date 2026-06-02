using Microsoft.Win32;
using System;
using System.Diagnostics;
using System.IO;

namespace CrossDroid.Windows.Backend;

public sealed class ShellIntegrationService
{
    private readonly SettingsService _settings;

    public ShellIntegrationService(SettingsService settings)
    {
        _settings = settings;
    }

    public void EnsureAutoStart(bool enable)
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
                    key.SetValue("CrossDroid", $"\"{exePath}\" --hidden");
                }
            }
            else
            {
                key.DeleteValue("CrossDroid", false);
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"Failed to update AutoStart: {ex.Message}");
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
