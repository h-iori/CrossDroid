using System;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace CrossDroid.Windows.Backend.Network;

public enum MessageType
{
    PairRequest,
    PairResponse,
    TransferOffer,
    TransferAccept,
    TransferReject,
    TransferCancel,
    FileChunk
}

public class ProtocolMessage
{
    public MessageType Type { get; set; }
    public string PayloadJson { get; set; } = "";
}

public class PairRequestPayload
{
    public string DeviceId { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string PublicKeyBase64 { get; set; } = ""; // ECDH public key
    public string Pin { get; set; } = ""; // Optional for PIN pairing
}

public class PairResponsePayload
{
    public bool Accepted { get; set; }
    public string PublicKeyBase64 { get; set; } = "";
}

public class TransferOfferPayload
{
    public string TransferId { get; set; } = "";
    public string FileName { get; set; } = "";
    public long TotalBytes { get; set; }
    public bool IsFolder { get; set; }
    public int ItemCount { get; set; }
    public string Hash { get; set; } = "";
}

public class TransferAcceptPayload
{
    public string TransferId { get; set; } = "";
    public bool Accepted { get; set; }
}

public class FileChunkPayload
{
    public string TransferId { get; set; } = "";
    public string RelativePath { get; set; } = ""; // Used for folders
    public long Offset { get; set; }
    // Binary data follows the JSON message in the stream for this type
}

public static class ProtocolFramer
{
    // Write length-prefixed message
    public static async Task WriteMessageAsync(Stream stream, ProtocolMessage message, byte[]? binaryPayload, CancellationToken token)
    {
        var json = JsonSerializer.Serialize(message);
        var jsonBytes = Encoding.UTF8.GetBytes(json);
        
        // Format: [4 bytes JSON length] [JSON bytes] [4 bytes binary length] [binary bytes]
        var header = new byte[8];
        BitConverter.TryWriteBytes(header.AsSpan(0, 4), jsonBytes.Length);
        BitConverter.TryWriteBytes(header.AsSpan(4, 4), binaryPayload?.Length ?? 0);
        
        await stream.WriteAsync(header, token);
        await stream.WriteAsync(jsonBytes, token);
        if (binaryPayload != null && binaryPayload.Length > 0)
        {
            await stream.WriteAsync(binaryPayload, token);
        }
    }

    public static async Task<(ProtocolMessage Message, byte[]? BinaryPayload)> ReadMessageAsync(Stream stream, CancellationToken token)
    {
        var header = new byte[8];
        if (!await TryReadExactAsync(stream, header, token))
            throw new EndOfStreamException();

        var jsonLen = BitConverter.ToInt32(header, 0);
        var binLen = BitConverter.ToInt32(header, 4);

        if (jsonLen > 1024 * 1024 || binLen > 1024 * 1024 * 50) // Arbitrary limits
            throw new InvalidDataException("Message too large");

        var jsonBytes = new byte[jsonLen];
        if (!await TryReadExactAsync(stream, jsonBytes, token))
            throw new EndOfStreamException();

        var json = Encoding.UTF8.GetString(jsonBytes);
        var message = JsonSerializer.Deserialize<ProtocolMessage>(json) 
            ?? throw new InvalidDataException("Invalid JSON message");

        byte[]? binaryPayload = null;
        if (binLen > 0)
        {
            binaryPayload = new byte[binLen];
            if (!await TryReadExactAsync(stream, binaryPayload, token))
                throw new EndOfStreamException();
        }

        return (message, binaryPayload);
    }

    private static async Task<bool> TryReadExactAsync(Stream stream, byte[] buffer, CancellationToken token)
    {
        int totalRead = 0;
        while (totalRead < buffer.Length)
        {
            int read = await stream.ReadAsync(buffer.AsMemory(totalRead, buffer.Length - totalRead), token);
            if (read == 0) return false;
            totalRead += read;
        }
        return true;
    }
}
