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
# Ưu tiên JDK 17 nếu chưa đặt JAVA_HOME. JDK mới hơn vẫn build ra Java 17 theo pom.xml.
if (-not $env:JAVA_HOME) {
    $jdkFolders = @(
        Get-ChildItem -Path "$env:ProgramFiles\Java\jdk*" -Directory -ErrorAction SilentlyContinue
        Get-ChildItem -Path "$env:ProgramFiles\Eclipse Adoptium\jdk*" -Directory -ErrorAction SilentlyContinue
    )
    $javaOnPath = Get-Command java.exe -ErrorAction SilentlyContinue
    $candidateHomes = @($jdkFolders | Sort-Object @{Expression = { if ($_.Name -match '^jdk-?17([.\-]|$)') { 0 } else { 1 } }}, Name | ForEach-Object { $_.FullName })
    if ($javaOnPath) { $candidateHomes += Split-Path (Split-Path $javaOnPath.Source -Parent) -Parent }
    foreach ($candidateHome in $candidateHomes) {
        if (-not (Test-Path -LiteralPath "$candidateHome\bin\javac.exe")) { continue }
        $versionOutput = & "$candidateHome\bin\java.exe" --version
        if ($LASTEXITCODE -eq 0 -and ($versionOutput -join "`n") -match '(?m)^(?:openjdk|java)\s+(\d+)' -and [int]$Matches[1] -ge 17) {
            $env:JAVA_HOME = $candidateHome
            break
        }
    }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\javac.exe")) {
    throw 'Cần JDK 17 trở lên. Đặt JAVA_HOME tới thư mục JDK (không phải JRE) rồi chạy lại.'
}
$versionOutput = & "$env:JAVA_HOME\bin\java.exe" --version
if ($LASTEXITCODE -ne 0 -or ($versionOutput -join "`n") -notmatch '(?m)^(?:openjdk|java)\s+(\d+)' -or [int]$Matches[1] -lt 17) {
    throw 'JAVA_HOME phải trỏ tới JDK 17 trở lên. Nhóm nên thống nhất JDK 17 và Tomcat 10.1.'
}
Write-Host "JDK dùng để build/chạy: $env:JAVA_HOME; ứng dụng được biên dịch cho Java 17."
$env:HTTP_PORT = "$Port"
$env:APP_BASE_URL = "http://localhost:$Port/hospital"
Write-Host 'Biên dịch Servlet/JSP và đóng gói WAR...'
& $mavenPath -B clean package
if ($LASTEXITCODE -ne 0) { throw 'Build thất bại. Xem lỗi phía trên.' }
Write-Host "Mở http://localhost:$Port/hospital/login sau khi Tomcat khởi động."
& $mavenPath -B exec:java
if ($LASTEXITCODE -ne 0) { throw 'Không khởi động được. Kiểm tra MySQL, cấu hình local và cổng web.' }
