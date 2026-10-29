param([int]$Port=3000)
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$env:HTTP_PORT="$Port"
if(-not $env:APP_BASE_URL) { $env:APP_BASE_URL="http://localhost:$Port/hospital" }
& mvn -B package
if($LASTEXITCODE -ne 0) { throw 'Build thất bại.' }
& mvn -Pdev '-Dfrontend.skip=true' test-compile exec:java '-Dexec.mainClass=vn.hospital.dev.DevServer' '-Dexec.classpathScope=test'
