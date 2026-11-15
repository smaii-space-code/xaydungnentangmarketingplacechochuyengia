# Chạy trong terminal cá nhân. Không nhập bí mật vào chat.
$ErrorActionPreference = 'Stop'
$env:BIZ04_SMTP_HOST = 'smtp.gmail.com'
$env:BIZ04_SMTP_PORT = '587'
$env:BIZ04_SMTP_USERNAME = Read-Host 'Nhap email tai khoan gui'
$env:BIZ04_SMTP_FROM = $env:BIZ04_SMTP_USERNAME
foreach ($biz04KeyName in @('BIZ04_OTP_HMAC_KEY','BIZ04_CCCD_KEY')) {
    $biz04KeyValue = [Environment]::GetEnvironmentVariable($biz04KeyName,'Process')
    if ([string]::IsNullOrWhiteSpace($biz04KeyValue)) {
        $biz04KeyValue = [Environment]::GetEnvironmentVariable($biz04KeyName,'User')
    }
    if ([string]::IsNullOrWhiteSpace($biz04KeyValue)) {
        $biz04KeyBytes = [byte[]]::new(32)
        [Security.Cryptography.RandomNumberGenerator]::Fill($biz04KeyBytes)
        $biz04KeyValue = [Convert]::ToBase64String($biz04KeyBytes)
        [Environment]::SetEnvironmentVariable($biz04KeyName,$biz04KeyValue,'User')
        [Array]::Clear($biz04KeyBytes,0,$biz04KeyBytes.Length)
        Write-Host "Da tao $biz04KeyName trong bien moi truong nguoi dung. Giu on dinh khoa nay khi sao luu/chuyen may."
    }
    try { if ([Convert]::FromBase64String($biz04KeyValue).Length -ne 32) { throw 'Invalid key length' } }
    catch { throw "$biz04KeyName phai la Base64 cua 32 byte. Khong tu thay khoa CCCD da dung." }
    [Environment]::SetEnvironmentVariable($biz04KeyName,$biz04KeyValue,'Process')
}
$biz04MailSecret = Read-Host 'Nhap Mat khau ung dung Gmail (khong phai mat khau dang nhap Google)' -AsSecureString
try {
    $env:BIZ04_SMTP_PASSWORD = [System.Net.NetworkCredential]::new('',$biz04MailSecret).Password.Replace(' ','')
    if ([string]::IsNullOrWhiteSpace($env:BIZ04_SMTP_PASSWORD)) { throw 'Chua nhap Mat khau ung dung Gmail.' }
} finally { $biz04MailSecret.Dispose(); Remove-Variable biz04MailSecret,biz04KeyValue -ErrorAction SilentlyContinue }
Write-Host 'Da nap cau hinh vao phien PowerShell hien tai; chua thu gui email. Khong in hoac chia se cac bien bi mat.'
