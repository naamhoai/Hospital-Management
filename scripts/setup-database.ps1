param(
    [string]$DbHost = '127.0.0.1',
    [ValidateRange(1,65535)][int]$Port = 3306,
    [string]$User = 'root',
    [string]$MySqlPath = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $MySqlPath) {
    $client = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($client) { $MySqlPath = $client.Source }
    else {
        $client = Get-ChildItem -Path "$env:ProgramFiles\MySQL\MySQL Server *\bin\mysql.exe" -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
        if ($client) { $MySqlPath = $client.FullName }
    }
}
if (-not $MySqlPath -or -not (Test-Path -LiteralPath $MySqlPath)) {
    throw 'Không tìm thấy mysql.exe. Mở database/hospital.sql bằng MySQL Workbench và chạy nguyên file.'
}
# Đọc đúng MỘT file tạo bảng và dữ liệu; không sinh file SQL, backup hay seed phụ.
$sql = [IO.File]::ReadAllText((Join-Path $projectRoot 'database/hospital.sql'), [Text.Encoding]::UTF8)
$mysqlArguments = @('--protocol=TCP', "--host=$DbHost", "--port=$Port", "--user=$User", '--default-character-set=utf8mb4', '--batch')
if (-not $env:MYSQL_PWD) { $mysqlArguments += '--password' }
$oldEncoding = $OutputEncoding
$oldConsoleEncoding = [Console]::OutputEncoding
try {
    $OutputEncoding = [Text.UTF8Encoding]::new($false)
    [Console]::OutputEncoding = $OutputEncoding
    $sql | & $MySqlPath @mysqlArguments
    if ($LASTEXITCODE -ne 0) { throw 'Nạp SQL thất bại. Không chạy lại trên DB đã có bảng; xem lỗi MySQL phía trên.' }
    Write-Host 'Đã tạo hospital. Sửa cấu hình MySQL rồi chạy scripts/run-dev.ps1.'
} finally {
    $OutputEncoding = $oldEncoding
    [Console]::OutputEncoding = $oldConsoleEncoding
}
