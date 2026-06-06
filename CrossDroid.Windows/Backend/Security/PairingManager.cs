using QRCoder;
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Security.Cryptography;
using System.Threading.Tasks;

namespace CrossDroid.Windows.Backend.Security;

public class PairingManager
{
    private readonly IdentityService _identity;

    public string CurrentPin { get; private set; } = string.Empty;
    public DateTimeOffset PinExpiresUtc { get; private set; } = DateTimeOffset.MinValue;
    public bool IsPinValid => !string.IsNullOrEmpty(CurrentPin) && DateTimeOffset.UtcNow < PinExpiresUtc;
    public TimeSpan PinRemainingTime => IsPinValid ? PinExpiresUtc - DateTimeOffset.UtcNow : TimeSpan.Zero;
    public int FailedAttempts { get; set; } = 0;

    public PairingManager(IdentityService identity)
    {
        _identity = identity;
    }

    public string GeneratePairingUri()
    {
        var device = _identity.LocalDevice;
        var encodedName = Uri.EscapeDataString(device.DisplayName);
        var uri = $"crossdroid://pair?id={device.DeviceId}&fp={device.PublicFingerprint}&name={encodedName}&type=Windows";
        if (!string.IsNullOrEmpty(CurrentPin) && IsPinValid)
        {
            uri += $"&pin={CurrentPin}";
        }
        return uri;
    }

    public async Task<global::Windows.Graphics.Imaging.SoftwareBitmap?> GenerateQrCodeAsync()
    {
        var uri = GeneratePairingUri();
        using var qrGenerator = new QRCodeGenerator();
        using var qrCodeData = qrGenerator.CreateQrCode(uri, QRCodeGenerator.ECCLevel.Q);
        using var qrCode = new QRCode(qrCodeData);
        using var bitmap = qrCode.GetGraphic(20, Color.White, Color.Transparent, true);

        using var ms = new MemoryStream();
        bitmap.Save(ms, ImageFormat.Png);
        ms.Position = 0;

        var decoder = await global::Windows.Graphics.Imaging.BitmapDecoder.CreateAsync(ms.AsRandomAccessStream());
        var softwareBitmap = await decoder.GetSoftwareBitmapAsync();
        
        if (softwareBitmap.BitmapPixelFormat != global::Windows.Graphics.Imaging.BitmapPixelFormat.Bgra8 ||
            softwareBitmap.BitmapAlphaMode == global::Windows.Graphics.Imaging.BitmapAlphaMode.Straight)
        {
            softwareBitmap = global::Windows.Graphics.Imaging.SoftwareBitmap.Convert(
                softwareBitmap, 
                global::Windows.Graphics.Imaging.BitmapPixelFormat.Bgra8, 
                global::Windows.Graphics.Imaging.BitmapAlphaMode.Premultiplied);
        }
        
        return softwareBitmap;
    }

    public string GenerateTemporaryPin()
    {
        // Cryptographically secure 6-digit PIN
        CurrentPin = RandomNumberGenerator.GetInt32(100000, 1000000).ToString();
        PinExpiresUtc = DateTimeOffset.UtcNow.AddMinutes(5);
        FailedAttempts = 0;
        return CurrentPin;
    }

    public void InvalidatePin()
    {
        CurrentPin = string.Empty;
        PinExpiresUtc = DateTimeOffset.MinValue;
        FailedAttempts = 0;
    }

    /// <summary>
    /// Checks if the PIN has expired and auto-regenerates if needed.
    /// Returns true if a new PIN was generated.
    /// </summary>
    public bool RefreshIfExpired()
    {
        if (!string.IsNullOrEmpty(CurrentPin) && !IsPinValid)
        {
            GenerateTemporaryPin();
            return true;
        }
        return false;
    }
}
