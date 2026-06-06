using System;
using System.IO;
using System.Net.Security;
using System.Net.Sockets;
using System.Security.Authentication;
using System.Security.Cryptography.X509Certificates;
using System.Threading;
using System.Threading.Tasks;

namespace CrossDroid.Windows.Backend.Network;

public class SecureSession : IDisposable
{
    private readonly TcpClient _client;
    private SslStream? _sslStream;
    public Stream Stream => _sslStream ?? throw new InvalidOperationException("Session not established");
    
    private string? _expectedFingerprint;
    public string RemoteFingerprint { get; private set; } = "";

    public SecureSession(TcpClient client)
    {
        _client = client;
    }

    public async Task AuthenticateAsServerAsync(X509Certificate2 localCert, CancellationToken token)
    {
        _sslStream = new SslStream(_client.GetStream(), false, ValidateRemoteCertificate);
        await _sslStream.AuthenticateAsServerAsync(new SslServerAuthenticationOptions
        {
            ServerCertificate = localCert,
            ClientCertificateRequired = true, // We require mutual authentication
            EnabledSslProtocols = SslProtocols.Tls12 | SslProtocols.Tls13,
            CertificateRevocationCheckMode = X509RevocationMode.NoCheck
        }, token);
    }

    public async Task AuthenticateAsClientAsync(X509Certificate2 localCert, string expectedFingerprint, CancellationToken token)
    {
        _expectedFingerprint = expectedFingerprint;
        _sslStream = new SslStream(_client.GetStream(), false, ValidateRemoteCertificate);
        await _sslStream.AuthenticateAsClientAsync(new SslClientAuthenticationOptions
        {
            TargetHost = "CrossDroid", // Dummy target host for self-signed
            ClientCertificates = new X509CertificateCollection { localCert },
            EnabledSslProtocols = SslProtocols.Tls12 | SslProtocols.Tls13,
            CertificateRevocationCheckMode = X509RevocationMode.NoCheck
        }, token);
    }

    private bool ValidateRemoteCertificate(object sender, X509Certificate? certificate, X509Chain? chain, SslPolicyErrors sslPolicyErrors)
    {
        if (certificate == null) return false;
        
        RemoteFingerprint = certificate.GetCertHashString();
        if (!string.IsNullOrEmpty(_expectedFingerprint) && !string.Equals(RemoteFingerprint, _expectedFingerprint, StringComparison.OrdinalIgnoreCase))
        {
            return false; // MitM detected or spoofed device
        }
        return true;
    }

    public async Task WriteMessageAsync(ProtocolMessage message, ReadOnlyMemory<byte> binaryPayload, CancellationToken token)
    {
        await ProtocolFramer.WriteMessageAsync(Stream, message, binaryPayload, token);
    }

    public async Task<(ProtocolMessage Message, byte[]? BinaryPayload, int BinLen)> ReadMessageAsync(CancellationToken token)
    {
        return await ProtocolFramer.ReadMessageAsync(Stream, token);
    }

    public void Dispose()
    {
        _sslStream?.Dispose();
        _client.Dispose();
    }
}
