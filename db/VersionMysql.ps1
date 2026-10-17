<#
.SYNOPSIS
    Versión mínima de MySQL que exige el proyecto y la función que la comprueba.

.DESCRIPTION
    El piso es MySQL 8.0.16: desde esa versión MySQL EVALÚA los CHECK (antes los acepta y los ignora sin
    error) y existe ALTER TABLE ... DROP CHECK (migración 001). MariaDB y MySQL 5.7 son otra familia: no
    tienen la collation utf8mb4_0900_ai_ci que usa schema.sql. Para el servidor se pide 8.4 LTS.
    La misma regla está en db/datos-iniciales.sql (en SQL) y en deploy/install.sh (en bash, antes de crear las
    tablas). Las tres se prueban con db/pruebas-version-mysql.ps1.
    Lo usa db/bd.ps1 (se carga con: . "$PSScriptRoot\VersionMysql.ps1").
#>

$VersionMinimaMysql = [version]'8.0.16'

# $true si el texto de SELECT VERSION() corresponde a MySQL 8.0.16 o superior.
# Acepta sufijos como "8.0.36-0ubuntu0.22.04.1" o "8.0.16-log"; rechaza MariaDB y textos que no son versiones.
function Test-VersionMysql([string]$Version) {
    if ($Version -match 'MariaDB') { return $false }
    if ($Version -notmatch '^\s*(\d+)\.(\d+)\.(\d+)') { return $false }
    $detectada = [version]"$($Matches[1]).$($Matches[2]).$($Matches[3])"
    return $detectada -ge $VersionMinimaMysql
}
