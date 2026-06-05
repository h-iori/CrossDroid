# PowerShell Script to Import CrossDroid Certificate
$CertPath = "C:\Users\harsh\OneDrive\Desktop\CrossDroid\CrossDroidDev.cer"

# Check if running as Admin. If not, request elevation.
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "Requesting Administrator privileges to install certificate..."
    Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

# Import the certificate to the LocalMachine Trusted People store
Import-Certificate -FilePath $CertPath -CertStoreLocation "Cert:\LocalMachine\TrustedPeople"
Write-Host "`n✅ Certificate imported successfully into Local Machine's Trusted People store!`n" -ForegroundColor Green

Read-Host "Press Enter to close this window"
