<#
.SYNOPSIS
    Prueba unitaria del control de versión mínima de MySQL (8.0.16), sin necesitar un servidor antiguo.

.DESCRIPTION
    1. Test-VersionMysql (db/VersionMysql.ps1, la usa bd.ps1) con textos de versión de ejemplo.
    2. El control de db/datos-iniciales.sql: toma del archivo las dos sentencias del control, cambia VERSION()
       por cada texto de ejemplo y las ejecuta en el MySQL local como isw_app (no lee ni escribe tablas).
       Una versión menor debe detenerse con el error del sql_mode; una suficiente debe pasar.
    3. El control de deploy/install.sh: toma del archivo la función version_mysql_ok y la ejecuta en bash
       (Git para Windows o WSL) con los mismos textos de ejemplo.
    La parte 2 necesita el cliente mysql y la variable ISW_DB_PASSWORD, y la 3 necesita bash; si faltan, se omiten avisando.

.EXAMPLE
    .\db\pruebas-version-mysql.ps1
#>
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
. (Join-Path $PSScriptRoot 'VersionMysql.ps1')

# Texto de VERSION() -> ¿cumple el mínimo?
$casos = [ordered]@{
    '8.0.15'                  = $false
    '8.0.16'                  = $true
    '8.0.16-log'              = $true
    '8.0.36-0ubuntu0.22.04.1' = $true
    '8.4.0'                   = $true
    '8.4.9'                   = $true
    '9.1.0'                   = $true
    '8.0.4-rc'                = $false
    '5.7.44'                  = $false
    '5.7.44-log'              = $false
    '10.11.6-MariaDB'         = $false
    '11.4.2-MariaDB-ubu2404'  = $false
}

$fallos = 0
function Resultado([bool]$ok, [string]$texto) {
    if ($ok) { Write-Host "  OK     $texto" -ForegroundColor Green }
    else { $script:fallos++; Write-Host "  FALLÓ  $texto" -ForegroundColor Red }
}

Write-Host "=== 1. Test-VersionMysql (bd.ps1), mínimo $VersionMinimaMysql ===" -ForegroundColor Cyan
foreach ($v in $casos.Keys) {
    $obtenido = Test-VersionMysql $v
    Resultado ($obtenido -eq $casos[$v]) ("{0,-26} -> {1}" -f $v, $(if ($obtenido) { 'pasa' } else { 'se detiene' }))
}
Resultado (-not (Test-VersionMysql '')) "texto vacío -> se detiene"
Resultado (-not (Test-VersionMysql 'no es una versión')) "texto sin versión -> se detiene"

Write-Host "`n=== 2. Control de db/datos-iniciales.sql (ejecutado en el MySQL local) ===" -ForegroundColor Cyan
$sql = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'datos-iniciales.sql'), [Text.Encoding]::UTF8)
$m = [regex]::Match($sql, '(?s)SET @version_mysql_ok = .*?;\s*SET SESSION sql_mode = .*?;')
if (-not $m.Success) { throw "No se encontró el control de versión en datos-iniciales.sql." }
$control = $m.Value

function Get-Entorno($n) { foreach ($a in 'Process', 'User', 'Machine') { $x = [Environment]::GetEnvironmentVariable($n, $a); if ($x) { return $x } } }
$clave = Get-Entorno 'ISW_DB_PASSWORD'
$mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    $mysql = Get-ChildItem 'C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe' -ErrorAction SilentlyContinue |
        Select-Object -Last 1 -ExpandProperty FullName
}

if (-not $clave -or -not $mysql) {
    Write-Host "  (omitida: falta el cliente mysql o la variable ISW_DB_PASSWORD)" -ForegroundColor Yellow
} else {
    $dbHost = Get-Entorno 'ISW_DB_HOST'; if (-not $dbHost) { $dbHost = '127.0.0.1' }
    $dbPort = Get-Entorno 'ISW_DB_PORT'; if (-not $dbPort) { $dbPort = '3306' }
    foreach ($v in $casos.Keys) {
        $psi = New-Object Diagnostics.ProcessStartInfo
        $psi.FileName = $mysql
        $psi.Arguments = "--protocol=TCP --host=$dbHost --port=$dbPort --user=isw_app --default-character-set=utf8mb4"
        $psi.UseShellExecute = $false; $psi.CreateNoWindow = $true
        $psi.RedirectStandardInput = $true; $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
        $psi.EnvironmentVariables['MYSQL_PWD'] = $clave
        $p = [Diagnostics.Process]::Start($psi)
        $p.StandardInput.Write($control.Replace('VERSION()', "'$v'"))
        $p.StandardInput.Close()
        $null = $p.StandardOutput.ReadToEnd(); $err = $p.StandardError.ReadToEnd(); $p.WaitForExit()
        $seDetuvo = $p.ExitCode -ne 0 -and $err -like '*ERROR_se_necesita_MySQL_8_0_16_o_superior*'
        $paso = $p.ExitCode -eq 0
        if ($casos[$v]) { Resultado $paso ("{0,-26} -> pasa" -f $v) }
        else { Resultado $seDetuvo ("{0,-26} -> se detiene con el mensaje de versión" -f $v) }
        if (-not $seDetuvo -and -not $paso) { Write-Host "         $($err.Trim())" -ForegroundColor DarkGray }
    }
}

Write-Host "`n=== 3. Control de deploy/install.sh (función version_mysql_ok, ejecutada en bash) ===" -ForegroundColor Cyan
$sh = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '..\deploy\install.sh'), [Text.Encoding]::UTF8)
$m = [regex]::Match($sh, '(?s)VERSION_MINIMA_MYSQL=\S+\s+version_mysql_ok\(\) \{.*?\n\}')
if (-not $m.Success) { throw "No se encontró la función version_mysql_ok en deploy/install.sh." }
$bash = @("$env:ProgramFiles\Git\bin\bash.exe", (Get-Command bash -ErrorAction SilentlyContinue).Source) |
    Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $bash) {
    Write-Host "  (omitida: no se encontró bash; con Git para Windows o WSL se ejecuta)" -ForegroundColor Yellow
} else {
    $todos = @($casos.Keys) + @('', 'no es una versión')
    $guion = $m.Value.Replace("`r", '') + "`n" + (($todos | ForEach-Object {
        "if version_mysql_ok '$_'; then echo 1; else echo 0; fi" }) -join "`n") + "`n"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $bash; $psi.Arguments = '-s'
    $psi.UseShellExecute = $false; $psi.CreateNoWindow = $true
    $psi.RedirectStandardInput = $true; $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
    $p = [Diagnostics.Process]::Start($psi)
    $p.StandardInput.Write($guion); $p.StandardInput.Close()
    $salida = @($p.StandardOutput.ReadToEnd() -split "`n" | Where-Object { $_ -match '^[01]$' })
    $err = $p.StandardError.ReadToEnd(); $p.WaitForExit()
    if ($salida.Count -ne $todos.Count) { Resultado $false "bash no devolvió un resultado por caso: $($err.Trim())" }
    else {
        for ($i = 0; $i -lt $todos.Count; $i++) {
            $v = $todos[$i]; $obtenido = $salida[$i] -eq '1'
            $esperado = if ($casos.Contains($v)) { $casos[$v] } else { $false }
            $nombre = if ($v) { $v } else { 'texto vacío' }
            Resultado ($obtenido -eq $esperado) ("{0,-26} -> {1}" -f $nombre, $(if ($obtenido) { 'pasa' } else { 'se detiene' }))
        }
    }
}

Write-Host ""
if ($fallos -eq 0) { Write-Host "Todas las pruebas del control de versión pasaron." -ForegroundColor Green }
else { Write-Host "$fallos prueba(s) fallaron." -ForegroundColor Red; exit 1 }
