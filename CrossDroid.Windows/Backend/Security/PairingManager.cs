using QRCoder;
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Threading.Tasks;
using System.Threading.Tasks;

namespace CrossDroid.Windows.Backend.Security;

public class PairingManager
{
    private readonly IdentityService _identity;

    public PairingManager(IdentityService identity)
    {
        _identity = identity;
    }

    public string GeneratePairingUri()
    {
        var device = _identity.LocalDevice;
        var encodedName = Uri.EscapeDataString(device.DisplayName);
        var uri = $"crossdroid://pair?id={device.DeviceId}&fp={device.PublicFingerprint}&name={encodedName}&type=Windows";
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
        // Simple 4-digit PIN for demonstration
        var random = new Random();
        return random.Next(1000, 9999).ToString();
    }
}
