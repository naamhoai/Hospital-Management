param([ValidateRange(1,65535)][int]$Port = 3000)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot

# File này là cấu hình riêng của từng máy, không đưa mật khẩu lên Git.
$config = Join-Path $projectRoot 'src/main/resources/application-local.properties'
if (-not (Test-Path -LiteralPath $config) -and -not $env:DB_URL) {
    Copy-Item -LiteralPath "$config.example" -Destination $config
    throw "Đã tạo $config. Hãy nhập DB_USER/DB_PASSWORD của MySQL trên máy bạn, rồi chạy lại."
}

# Maven đi kèm NetBeans thường đã được tải vào thư mục .m2 của người dùng.
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($maven) { $mavenPath = $maven.Source }
else {
    $maven = Get-ChildItem -LiteralPath "$env:USERPROFILE\.m2\wrapper\dists" -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if (-not $maven) { throw 'Cần Maven trên PATH hoặc Maven của NetBeans. Xem README.' }
    $mavenPath = $maven.FullName
}
if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem -Path "$env:ProgramFiles\Java\jdk*" -Directory -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) {
    throw 'Cần JDK 17 trở lên. Đặt JAVA_HOME tới thư mục JDK rồi chạy lại.'
}
$env:HTTP_PORT = "$Port"
$env:APP_BASE_URL = "http://localhost:$Port/hospital"
Write-Host 'Biên dịch Servlet/JSP và đóng gói WAR...'
& $mavenPath -B package
if ($LASTEXITCODE -ne 0) { throw 'Build thất bại. Xem lỗi phía trên.' }
Write-Host "Mở http://localhost:$Port/hospital/login sau khi Tomcat khởi động."
& $mavenPath -B exec:java
if ($LASTEXITCODE -ne 0) { throw 'Không khởi động được. Kiểm tra MySQL, cấu hình local và cổng web.' }
