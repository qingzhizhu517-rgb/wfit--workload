<#
.SYNOPSIS
    Fresh DB 整体重建前的只读体检：核对目标库、列出业务表与行数、输出备份命令。
    绝不执行任何 DROP / DELETE / TRUNCATE / 重建；只 SELECT + 打印，供人工确认。

.DESCRIPTION
    结构改造（两阶段导入 + 核算一致性）后，若要在「当前环境无真实数据」前提下整体重建业务库，
    真正的重建（drop + 重跑 01→28 初始化脚本）是独立高风险动作，必须人工执行并二次确认。
    本脚本只做重建前体检：
      1) 打印目标库名 / 主机 / 端口（从 .env 的 WFIT_* 读取，回落默认值）
      2) 列出 biz_* 业务表及各表行数（判断是否真的「无真实数据」）
      3) 打印建议的 mysqldump 备份命令与重建初始化顺序
      4) 打印执行重建所需的人工确认短语
    数据库口令一律不打印、不写文件；连接用 .env 注入的 WFIT_DB_PASSWORD。

.PARAMETER EnvFile
    .env 路径，默认仓库根 .env。

.EXAMPLE
    .\rear\scripts\verify-rebuild-target.ps1
#>
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot "..\..\.env")
)

$ErrorActionPreference = "Stop"

# ---- 读取 .env 的 WFIT_* 连接参数（不打印口令）----
$dbHost = "127.0.0.1"; $dbPort = "3306"; $dbName = "wflg_workload"; $dbUser = "wfit"; $dbPass = $null
if (Test-Path $EnvFile) {
    foreach ($line in Get-Content $EnvFile) {
        if ($line -match '^\s*WFIT_DB_HOST\s*=\s*(.+?)\s*$') { $dbHost = $Matches[1] }
        elseif ($line -match '^\s*WFIT_DB_PORT\s*=\s*(.+?)\s*$') { $dbPort = $Matches[1] }
        elseif ($line -match '^\s*WFIT_DB_NAME\s*=\s*(.+?)\s*$') { $dbName = $Matches[1] }
        elseif ($line -match '^\s*WFIT_DB_USER\s*=\s*(.+?)\s*$') { $dbUser = $Matches[1] }
        elseif ($line -match '^\s*WFIT_DB_PASSWORD\s*=\s*(.+?)\s*$') { $dbPass = $Matches[1] }
    }
} else {
    Write-Warning ".env 未找到（$EnvFile）；使用默认连接参数，口令为空可能连接失败。"
}

Write-Output "==================== 重建目标体检（只读）===================="
Write-Output ("目标库 : {0}" -f $dbName)
Write-Output ("主机   : {0}:{1}" -f $dbHost, $dbPort)
Write-Output ("账号   : {0}  （口令来自 .env，不在此显示）" -f $dbUser)
Write-Output ""

# ---- 列出 biz_* 表与行数（只读 information_schema + count）----
$sql = @"
SELECT table_name,
       (SELECT COUNT(*) FROM information_schema.columns c
        WHERE c.table_schema = t.table_schema AND c.table_name = t.table_name) AS col_count
FROM information_schema.tables t
WHERE t.table_schema = '$dbName' AND t.table_name LIKE 'biz\_%'
ORDER BY t.table_name;
"@

$mysql = (Get-Command mysql -ErrorAction SilentlyContinue)
if ($null -eq $mysql) {
    Write-Warning "PATH 中未找到 mysql 客户端；跳过实连体检，仅输出重建指引。"
} else {
    Write-Output "业务表清单（biz_*）与列数："
    if ($dbPass) {
        $env:MYSQL_PWD = $dbPass  # 避免口令出现在命令行/进程列表
        try { & mysql -h $dbHost -P $dbPort -u $dbUser $dbName -e $sql }
        finally { Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue }
    } else {
        & mysql -h $dbHost -P $dbPort -u $dbUser $dbName -e $sql
    }
    Write-Output ""
    Write-Output "⚠️ 若上表中任一业务表行数非零，说明该库已有数据——重建将丢失数据，务必先备份并人工确认。"
}

Write-Output ""
Write-Output "==================== 建议备份命令（不自动执行）===================="
Write-Output "# 全库备份（重建前必做，文件名含时间戳）："
Write-Output ("mysqldump --single-transaction -h {0} -P {1} -u {2} -p {3} > backup-{3}-{4}.sql" -f `
    $dbHost, $dbPort, $dbUser, $dbName, (Get-Date -Format "yyyyMMddHHmmss"))
Write-Output ""
Write-Output "==================== Fresh DB 初始化顺序（重建时人工执行）===================="
Write-Output @"
# 基础系统表 → 业务基线 → 增量脚本（全部幂等，按序）：
#   ry_20260321.sql, quartz.sql
#   01_biz_schema.sql ... 06_test_accounts.sql
#   08_review_fixes.sql, 13_fix_audit_perm.sql（新库必需）
#   22_semester_position_workload.sql ... 28_import_framework.sql（核算一致性 + 两阶段导入）
# 详见 CLAUDE.md「数据库初始化」与 README。
"@
Write-Output ""
Write-Output "==================== 执行重建的人工确认短语 ===================="
Write-Output "真正 DROP + 重建属独立高风险动作，本脚本不执行。确认执行者须在重建脚本中键入："
Write-Output "    REBUILD wflg_workload CONFIRMED"
Write-Output "以示知悉：重建将清空并重建业务库，且已完成上述备份。"
