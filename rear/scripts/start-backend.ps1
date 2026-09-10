<#
.SYNOPSIS
    加载仓库根 .env 中的 WFIT_* 环境变量并启动后端（workload-admin，端口 8084）。

.DESCRIPTION
    凭据自 612984f 起一律由环境变量注入，配置文件中不再有明文口令。
    WFIT_DB_PASSWORD / WFIT_DRUID_PASSWORD / WFIT_TOKEN_SECRET 无默认值，
    未注入则 Spring 在解析占位符时直接启动失败（这是刻意设计）。

    本脚本只在当前 PowerShell 进程内设置变量，不写系统环境、不复制口令到任何文件，
    .env 始终是唯一的口令来源。

.PARAMETER Build
    启动前先执行 mvn clean package -DskipTests 重新打包。

.PARAMETER Mvn
    改用 mvn spring-boot:run 启动（默认是 java -jar 跑已打好的 jar）。

.EXAMPLE
    .\scripts\start-backend.ps1
    .\scripts\start-backend.ps1 -Build
#>
param(
    [switch]$Build,
    [switch]$Mvn
)

$ErrorActionPreference = 'Stop'

$RearDir  = Split-Path -Parent $PSScriptRoot
$RepoRoot = Split-Path -Parent $RearDir
$EnvFile  = Join-Path $RepoRoot '.env'

if (-not (Test-Path $EnvFile)) {
    throw "未找到 $EnvFile。先复制样例并填值：Copy-Item '$RepoRoot\.env.example' '$EnvFile'"
}

# ---- 解析 .env（忽略注释与空行，值中的 = 保留）----
$loaded = 0
foreach ($line in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
    $t = $line.Trim()
    if ($t -eq '' -or $t.StartsWith('#')) { continue }
    $i = $t.IndexOf('=')
    if ($i -lt 1) { continue }
    $key = $t.Substring(0, $i).Trim()
    $val = $t.Substring($i + 1).Trim().Trim('"').Trim("'")
    # 空值一律跳过：Spring 的 ${VAR:default} 遇到"已设置但为空"会解析成空串而非默认值，
    # 例如 WFIT_DEFAULT_PASSWORD= 会让导入的教师账号初始口令变成空串。
    if ($val -eq '') { continue }
    Set-Item -Path "Env:$key" -Value $val
    $loaded++
}
Write-Host "已从 .env 载入 $loaded 个变量" -ForegroundColor DarkGray

# ---- 必填项校验：缺一项就别启动了，省得看一屏 Spring 堆栈 ----
$required = @('WFIT_DB_PASSWORD', 'WFIT_DRUID_PASSWORD', 'WFIT_TOKEN_SECRET')
$missing = $required | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) }
if ($missing) {
    throw "以下必填变量为空，后端会在解析占位符时启动失败：$($missing -join ', ')。请在 $EnvFile 中填值。"
}
# HS512 要求密钥 >= 64 字节
$secretLen = [Text.Encoding]::UTF8.GetByteCount($env:WFIT_TOKEN_SECRET)
if ($secretLen -lt 64) {
    throw "WFIT_TOKEN_SECRET 仅 $secretLen 字节，HS512 要求 >= 64。重新生成：openssl rand -hex 40"
}

Write-Host "DB   : $($env:WFIT_DB_USER)@$($env:WFIT_DB_HOST):$($env:WFIT_DB_PORT)/$($env:WFIT_DB_NAME)" -ForegroundColor Cyan
Write-Host "Redis: $($env:WFIT_REDIS_HOST):$($env:WFIT_REDIS_PORT)" -ForegroundColor Cyan

Push-Location $RearDir
try {
    if ($Build) {
        Write-Host '打包中：mvn clean package -DskipTests ...' -ForegroundColor Yellow
        mvn clean package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "打包失败（exit $LASTEXITCODE）" }
    }

    if ($Mvn) {
        mvn spring-boot:run -pl workload-admin
    }
    else {
        $jar = Join-Path $RearDir 'workload-admin\target\workload-admin.jar'
        if (-not (Test-Path $jar)) {
            throw "未找到 $jar。先加 -Build 参数打包，或改用 -Mvn 直接跑。"
        }
        Write-Host "启动 $jar（Ctrl+C 停止，端口 8084）" -ForegroundColor Green
        java -jar $jar
    }
}
finally {
    Pop-Location
}
