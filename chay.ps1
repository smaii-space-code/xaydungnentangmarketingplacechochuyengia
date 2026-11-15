param(
    [string]$ServerName = 'localhost',
    [int]$Port = 1433,
    [string]$Database = 'BIZ04',
    [string]$DbUser,
    [int]$AppPort = 8081,
    [switch]$WindowsAuth,
    [switch]$Demo,
    [switch]$SkipBuild,
    [switch]$EmailSetup
)
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
if ($EmailSetup) { . (Join-Path $PSScriptRoot 'cau-hinh-email.ps1') }
foreach ($biz04KeyName in @('BIZ04_OTP_HMAC_KEY','BIZ04_CCCD_KEY')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($biz04KeyName,'Process'))) {
        $biz04SavedKey = [Environment]::GetEnvironmentVariable($biz04KeyName,'User')
        if (-not [string]::IsNullOrWhiteSpace($biz04SavedKey)) { [Environment]::SetEnvironmentVariable($biz04KeyName,$biz04SavedKey,'Process') }
    }
}
Remove-Variable biz04SavedKey -ErrorAction SilentlyContinue
if ($ServerName -notmatch '^[a-zA-Z0-9_.\\-]+$' -or $Database -notmatch '^[a-zA-Z][a-zA-Z0-9_]{0,63}$' -or $Port -lt 1 -or $Port -gt 65535 -or $AppPort -lt 1 -or $AppPort -gt 65535) {
    throw 'ServerName, Port, AppPort hoac Database khong hop le.'
}
$biz04Probe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $AppPort)
try { $biz04Probe.Start() } catch { throw "Cong $AppPort dang duoc su dung. Kiem tra dung tien trinh truoc khi chay." } finally { $biz04Probe.Stop() }
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $biz04Java = Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    $biz04Jdk = Get-ChildItem (Join-Path $env:ProgramFiles 'Java') -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') } | Sort-Object Name -Descending | Select-Object -First 1
    $biz04Java = if ($biz04Jdk) { Join-Path $biz04Jdk.FullName 'bin\java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
}
$biz04JavaVersion = (& $biz04Java --version 2>&1 | Select-Object -First 1)
#if ($LASTEXITCODE -ne 0 -or $biz04JavaVersion -notmatch '\b(?:java|openjdk)\s+(?:version\s+)?["'']?25(?:[.+\s"'']|$)') {
    #throw 'Ung dung yeu cau JDK 25.'
#}
Write-Host $biz04JavaVersion
if (-not $SkipBuild) {
    $biz04Maven = (Get-Command mvn -ErrorAction Stop).Source
    & $biz04Maven '-q' '-DskipTests' 'package'
    if ($LASTEXITCODE -ne 0) { throw 'Build chua thanh cong. Giu lai thong bao loi de kiem tra.' }
}
$biz04Jar = Join-Path $PSScriptRoot 'target\marketplace-1.0.0.jar'
if (-not (Test-Path $biz04Jar)) { throw 'Chua co JAR. Chay lai khong co -SkipBuild.' }
if (-not $WindowsAuth -and -not $DbUser) { $DbUser = Read-Host 'Ten dang nhap SQL Server Authentication' }
if (-not $WindowsAuth -and [string]::IsNullOrWhiteSpace($DbUser)) { throw 'Can ten dang nhap SQL Server.' }
$biz04AuthDir = Join-Path $PSScriptRoot '.tools\sqlserver'
if ($WindowsAuth -and -not (Test-Path (Join-Path $biz04AuthDir 'mssql-jdbc_auth-13.4.0.x64.dll'))) {
    throw 'Thieu thu vien Windows Authentication. Xem README.md hoac dung SQL Server Authentication.'
}
$biz04Secret = if ($WindowsAuth) { $null } else { Read-Host 'Mat khau SQL Server (chi nhap tai terminal nay)' -AsSecureString }
$biz04OldPassword = $env:BIZ04_DB_PASSWORD
$biz04OldUser = $env:BIZ04_DB_USER
$biz04OldUrl = $env:BIZ04_DB_URL
$biz04OldDemo = $env:BIZ04_DEMO
$biz04OldPort = $env:BIZ04_PORT
try {
    $env:BIZ04_DB_URL = ('jdbc:sqlserver://{0}:{1};databaseName={2};encrypt=true;trustServerCertificate=true;loginTimeout=10;' -f $ServerName,$Port,$Database)
    if ($WindowsAuth) { $env:BIZ04_DB_URL += 'integratedSecurity=true;authenticationScheme=NativeAuthentication;' }
    $env:BIZ04_DB_USER = if ($WindowsAuth) { '' } else { $DbUser }
    $env:BIZ04_DB_PASSWORD = if ($WindowsAuth) { '' } else { [System.Net.NetworkCredential]::new('', $biz04Secret).Password }
    $env:BIZ04_DEMO = if ($Demo) { 'true' } else { 'false' }
    $env:BIZ04_PORT = "$AppPort"
    New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot '.run') | Out-Null
    Write-Host "Sang Connect: http://localhost:$AppPort . Doi thong bao Started MarketplaceApplication."
    Write-Host 'Thanh toan chi la mo phong. Ctrl+C de dung. Log: .run/application.log'
    $biz04JvmArgs = @('-Xms128m', '-Xmx768m')
    if ($WindowsAuth) { $biz04JvmArgs += "-Djava.library.path=$biz04AuthDir" }
    & $biz04Java @biz04JvmArgs '-jar' $biz04Jar 2>&1 | Tee-Object -FilePath (Join-Path $PSScriptRoot '.run\application.log')
    if ($LASTEXITCODE -ne 0) { throw 'Ung dung dung voi loi. Kiem tra thong bao JDBC/Flyway va cau hinh SQL Server.' }
} finally {
    $env:BIZ04_DB_PASSWORD = $biz04OldPassword
    $env:BIZ04_DB_USER = $biz04OldUser
    $env:BIZ04_DB_URL = $biz04OldUrl
    $env:BIZ04_DEMO = $biz04OldDemo
    $env:BIZ04_PORT = $biz04OldPort
    if ($biz04Secret) { $biz04Secret.Dispose() }
    Remove-Variable biz04OldPassword -ErrorAction SilentlyContinue
}
