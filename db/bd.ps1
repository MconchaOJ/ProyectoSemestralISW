<#
.SYNOPSIS
    Crea, carga y verifica la base de datos MySQL del proyecto (isw_practicas).

.DESCRIPTION
    -Accion Instalar  (por defecto)
        1. Ejecuta schema.sql y seed.sql con el usuario root de MySQL.
        2. Crea (o actualiza) el usuario de la aplicación isw_app con permisos solo
           de datos (SELECT, INSERT, UPDATE, DELETE) sobre isw_practicas.
        3. Ejecuta la verificación.
    -Accion Verificar
        Se conecta como isw_app, muestra las consultas de verificacion.sql y corre
        pruebas negativas (inserciones inválidas que la base DEBE rechazar).

    Las contraseñas se leen SOLO de variables de entorno; nunca de archivos ni
    de argumentos de línea de comandos:
        ISW_DB_ROOT_PASSWORD  clave de root de MySQL (solo para Instalar)
        ISW_DB_PASSWORD       clave de isw_app. Si no existe, se genera una aleatoria
                              y se guarda como variable de entorno de usuario de
                              Windows (no se muestra ni se escribe en ningún archivo)
    Opcionales: ISW_DB_HOST (127.0.0.1), ISW_DB_PORT (3306), ISW_DB_ROOT_USER (root).

    ATENCIÓN: Instalar BORRA y recrea las tablas (schema.sql). Solo para desarrollo/demo.

.EXAMPLE
    .\db\bd.ps1                    # crea todo y verifica
    .\db\bd.ps1 -Accion Verificar  # solo verifica
#>
param(
    [ValidateSet('Instalar', 'Verificar')]
    [string]$Accion = 'Instalar'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8   # para ver bien tildes y ñ

$BaseDatos  = 'isw_practicas'
$UsuarioApp = 'isw_app'

# ---------- Configuración (variables de entorno) ----------------------------------
function Get-Entorno([string]$Nombre, [string]$PorDefecto = $null) {
    foreach ($ambito in 'Process', 'User', 'Machine') {
        $v = [Environment]::GetEnvironmentVariable($Nombre, $ambito)
        if ($v) { return $v }
    }
    return $PorDefecto
}

$DbHost     = Get-Entorno 'ISW_DB_HOST' '127.0.0.1'
$DbPort     = Get-Entorno 'ISW_DB_PORT' '3306'
$RootUser   = Get-Entorno 'ISW_DB_ROOT_USER' 'root'

. (Join-Path $PSScriptRoot 'VersionMysql.ps1')   # $VersionMinimaMysql y Test-VersionMysql

# Cliente mysql: el del PATH o, si no está, el de cualquier "MySQL Server X.Y" instalado (el más nuevo).
$mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    $mysql = Get-ChildItem 'C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe' -ErrorAction SilentlyContinue |
        Sort-Object { [version]($_.Directory.Parent.Name -replace '^MySQL Server ', '') } -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mysql -or -not (Test-Path $mysql)) { throw "No se encontró mysql.exe. Abre una terminal nueva o revisa la instalación de MySQL." }

# Ejecuta el cliente mysql. El SQL entra por la entrada estándar (en bytes UTF-8, para
# no perder tildes) y la clave viaja en MYSQL_PWD del proceso hijo: no queda en la línea
# de comandos ni en ningún archivo.
function Invoke-Mysql {
    param([string]$Usuario, [string]$Clave, [string]$Sql, [switch]$Tabla, [string]$Db)
    $opciones = @('--protocol=TCP', "--host=$DbHost", "--port=$DbPort", "--user=$Usuario", '--default-character-set=utf8mb4')
    if ($Tabla) { $opciones += '--table' }
    if ($Db)    { $opciones += $Db }

    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $mysql
    $psi.Arguments = $opciones -join ' '
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.StandardOutputEncoding = [Text.Encoding]::UTF8
    $psi.StandardErrorEncoding  = [Text.Encoding]::UTF8
    $psi.EnvironmentVariables['MYSQL_PWD'] = $Clave

    $p = [Diagnostics.Process]::Start($psi)
    $bytes = (New-Object Text.UTF8Encoding($false)).GetBytes($Sql)
    $p.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
    $p.StandardInput.Close()
    $salida = $p.StandardOutput.ReadToEndAsync()
    $error_ = $p.StandardError.ReadToEndAsync()
    $p.WaitForExit()
    [pscustomobject]@{ Codigo = $p.ExitCode; Salida = $salida.Result; Error = $error_.Result }
}

function Read-ArchivoSql([string]$Nombre) {
    $ruta = Join-Path $PSScriptRoot $Nombre
    $texto = [IO.File]::ReadAllText($ruta, [Text.Encoding]::UTF8)
    return $texto.TrimStart([char]0xFEFF)
}

function Assert-Ok($r, [string]$Paso) {
    if ($r.Codigo -ne 0) { throw "Falló: $Paso`n$($r.Error)" }
    Write-Host "  OK  $Paso" -ForegroundColor Green
}

# ---------- Instalar ---------------------------------------------------------------
function Install-Bd {
    $claveRoot = Get-Entorno 'ISW_DB_ROOT_PASSWORD'
    if (-not $claveRoot) {
        throw "Falta la variable de entorno ISW_DB_ROOT_PASSWORD (clave de root de MySQL). Ver instrucciones en .env.example."
    }

    $claveApp = Get-Entorno 'ISW_DB_PASSWORD'
    if (-not $claveApp) {
        # Aleatoria de 24 caracteres alfanuméricos, guardada solo como variable de entorno
        $chars = [char[]]'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        $buf = New-Object byte[] 24
        $rng.GetBytes($buf)
        $claveApp = -join ($buf | ForEach-Object { $chars[$_ % $chars.Length] })
        [Environment]::SetEnvironmentVariable('ISW_DB_PASSWORD', $claveApp, 'User')
        [Environment]::SetEnvironmentVariable('ISW_DB_PASSWORD', $claveApp, 'Process')
        Write-Host "  Se generó ISW_DB_PASSWORD y se guardó como variable de entorno de usuario (no se muestra)." -ForegroundColor Yellow
    }

    Write-Host "Instalando en $DbHost`:$DbPort como $RootUser ..."
    $r = Invoke-Mysql $RootUser $claveRoot 'SELECT VERSION();'
    if ($r.Codigo -ne 0) { throw "No se pudo conectar como $RootUser a ${DbHost}:${DbPort}. Revisa ISW_DB_ROOT_PASSWORD.`n$($r.Error)" }
    $version = ($r.Salida -split "`n")[1].Trim()
    Assert-Ok $r "Conexión como $RootUser (MySQL $version)"

    # Antes de tocar nada: con una versión menor los CHECK se aceptan pero NO se cumplen (sin ningún error)
    if (-not (Test-VersionMysql $version)) {
        throw "El servidor tiene MySQL '$version' y el proyecto necesita MySQL $VersionMinimaMysql o superior " +
              "(se recomienda 8.4 LTS). Antes de 8.0.16 las restricciones CHECK se ignoran sin avisar; MariaDB y " +
              "MySQL 5.7 no son compatibles. No se modificó la base de datos."
    }
    Write-Host "  OK  Versión de MySQL compatible ($version >= $VersionMinimaMysql)" -ForegroundColor Green

    Assert-Ok (Invoke-Mysql $RootUser $claveRoot (Read-ArchivoSql 'schema.sql')) 'schema.sql (25 tablas, trigger)'
    Assert-Ok (Invoke-Mysql $RootUser $claveRoot (Read-ArchivoSql 'seed.sql'))   'seed.sql (catálogo y datos de demo)'

    # Usuario de la aplicación: solo puede leer y escribir datos, no cambiar la estructura.
    $claveSql = $claveApp.Replace('\', '\\').Replace("'", "''")
    $sql = @"
CREATE USER IF NOT EXISTS '$UsuarioApp'@'localhost' IDENTIFIED BY '$claveSql';
ALTER USER '$UsuarioApp'@'localhost' IDENTIFIED BY '$claveSql';
GRANT SELECT, INSERT, UPDATE, DELETE ON $BaseDatos.* TO '$UsuarioApp'@'localhost';
FLUSH PRIVILEGES;
"@
    Assert-Ok (Invoke-Mysql $RootUser $claveRoot $sql) "Usuario $UsuarioApp con permisos SELECT, INSERT, UPDATE, DELETE"
}

# ---------- Verificar --------------------------------------------------------------
function Test-Bd {
    $claveApp = Get-Entorno 'ISW_DB_PASSWORD'
    if (-not $claveApp) { throw "Falta la variable de entorno ISW_DB_PASSWORD. Ejecuta primero: .\db\bd.ps1" }

    Write-Host "`n=== Consultas de verificación (verificacion.sql) ===" -ForegroundColor Cyan
    $r = Invoke-Mysql $UsuarioApp $claveApp (Read-ArchivoSql 'verificacion.sql') -Tabla -Db $BaseDatos
    Write-Host $r.Salida
    if ($r.Codigo -ne 0) { throw "verificacion.sql falló:`n$($r.Error)" }

    Write-Host "=== Pruebas negativas: la base DEBE rechazar cada una, por la regla indicada ===" -ForegroundColor Cyan
    # Cada prueba corre dentro de una transacción con ROLLBACK, así que no deja datos aunque
    # por error la base la aceptara. Se comprueba el código de error Y que el mensaje nombre la
    # restricción esperada (así no se da por buena una prueba que falla por otra causa).
    # Formato: descripción, código de error, texto que debe aparecer en el mensaje, SQL.
    $hashOk = '$2a$10$oYiGClqHFshZpKW/kdQIXeUU.LlcoFD47bh.VoUQwhvyRUqRtyxLO'
    $unEvaluacion = "INSERT INTO evaluacion (inscripcion_id,pauta_id,evaluador_id,fecha_clase) VALUES (3,1,2,'2026-09-22'); SET @ev = LAST_INSERT_ID()"
    $pruebas = @(
        @('Correo duplicado',                                     1062, 'uq_usuario_email',        "INSERT INTO usuario (rol_id,nombre,apellidos,email,password_hash) VALUES (1,'X','Y','guia1@demo.example','$hashOk')"),
        @('Contraseña que no es BCrypt (texto plano)',            3819, 'ck_usuario_password_bcrypt', "INSERT INTO usuario (rol_id,nombre,apellidos,email,password_hash) VALUES (1,'X','Y','nuevo@demo.example','clave-en-texto-plano')"),
        @('Rol inexistente',                                      1452, 'fk_usuario_rol',          "INSERT INTO usuario (rol_id,nombre,apellidos,email,password_hash) VALUES (99,'X','Y','nuevo@demo.example','$hashOk')"),
        @('RUT duplicado',                                        1062, 'uq_estudiante_rut',       "INSERT INTO estudiante (usuario_id,rut,semestre_actual,anio_ingreso) VALUES (1,'11111111-1',5,2024)"),
        @('RUT con formato inválido',                             3819, 'ck_estudiante_rut',       "INSERT INTO estudiante (usuario_id,rut,semestre_actual,anio_ingreso) VALUES (1,'ABC',5,2024)"),
        @('Inscripción duplicada (mismo estudiante, asignatura y periodo)', 1062, 'uq_inscripcion_estudiante_asignatura_periodo', "INSERT INTO inscripcion (estudiante_id,asignatura_id,periodo,profesor_curso_id) VALUES (1,3,'2026-2',4)"),
        @('Periodo con formato inválido',                         3819, 'ck_inscripcion_periodo',  "INSERT INTO inscripcion (estudiante_id,asignatura_id,periodo,profesor_curso_id) VALUES (5,2,'2026-3',4)"),
        @('Inscripción EN_CURSO con nota final',                  3819, 'ck_inscripcion_nota_cierre', "INSERT INTO inscripcion (estudiante_id,asignatura_id,periodo,profesor_curso_id,estado,nota_final) VALUES (5,2,'2026-2',4,'EN_CURSO',5.0)"),
        @('Inscripción EN_CURSO con fecha de cierre',             3819, 'ck_inscripcion_fecha_cierre', "INSERT INTO inscripcion (estudiante_id,asignatura_id,periodo,profesor_curso_id,estado,fecha_cierre) VALUES (5,2,'2026-2',4,'EN_CURSO','2026-07-31')"),
        @('Guía y profesor del curso son la misma persona',       3819, 'ck_inscripcion_profesores_distintos', "INSERT INTO inscripcion (estudiante_id,asignatura_id,periodo,profesor_guia_id,profesor_curso_id) VALUES (5,3,'2026-2',4,4)"),
        @('Chatbot habilitado en una asignatura que no es final', 3819, 'ck_asignatura_chatbot',   "UPDATE asignatura_practica SET chatbot_habilitado = TRUE WHERE id = 1"),
        @('Tutor que es también el guía de la inscripción (HU-07)', 3819, 'ck_inscripcion_tutor_distinto', "UPDATE inscripcion SET tutor_id = profesor_guia_id WHERE id = 17"),
        @('Tutor en una asignatura que no va a un colegio (HU-07)', 3819, 'ck_asignatura_tutor',  "UPDATE asignatura_practica SET lleva_tutor = TRUE WHERE id = 1"),
        @('Pauta de Especialidad sin número de observación (HU-07)', 3819, 'ck_pauta_numero_observacion', "UPDATE pauta SET numero_observacion = NULL WHERE id = 2"),
        @('Puntaje fuera de la escala 1-7 de la pauta',           1644, 'fuera de la escala',      "$unEvaluacion; INSERT INTO respuesta_indicador (evaluacion_id,indicador_id,puntaje) VALUES (@ev,1,9)"),
        @('Segundo puntaje para el mismo indicador',              1062, 'uq_respuesta_evaluacion_indicador', "$unEvaluacion; INSERT INTO respuesta_indicador (evaluacion_id,indicador_id,puntaje) VALUES (@ev,1,5); INSERT INTO respuesta_indicador (evaluacion_id,indicador_id,puntaje) VALUES (@ev,1,6)"),
        @('Indicador que no pertenece a la pauta de la evaluación', 1644, 'no pertenece a la pauta', "INSERT INTO pauta (asignatura_id,nombre,tipo_evaluador,ambito,escala_min,escala_max) VALUES (4,'Otra pauta','ACADEMICO','OBSERVACION_CLASE',1,7); SET @pa = LAST_INSERT_ID(); INSERT INTO dimension_pauta (pauta_id,nombre,orden) VALUES (@pa,'D',1); SET @di = LAST_INSERT_ID(); INSERT INTO indicador (dimension_id,numero,descripcion) VALUES (@di,1,'I'); SET @in = LAST_INSERT_ID(); $unEvaluacion; INSERT INTO respuesta_indicador (evaluacion_id,indicador_id,puntaje) VALUES (@ev,@in,5)"),
        @('Modificar una evaluación FINALIZADA',                  1644, 'esta FINALIZADA',         "UPDATE respuesta_indicador SET puntaje = 1 WHERE evaluacion_id = 1 AND indicador_id = 1"),
        @('Evaluación FINALIZADA sin puntaje calculado',          3819, 'ck_evaluacion_finalizada', "INSERT INTO evaluacion (inscripcion_id,pauta_id,evaluador_id,fecha_clase,estado) VALUES (3,1,2,'2026-09-22','FINALIZADA')"),
        @('Horas de clase invertidas (término antes del inicio)', 3819, 'ck_evaluacion_horas',     "INSERT INTO evaluacion (inscripcion_id,pauta_id,evaluador_id,fecha_clase,hora_inicio,hora_termino) VALUES (3,1,2,'2026-09-22','11:00','10:00')"),
        @('Planificación con estado inventado',                   3819, 'ck_planificacion_estado', "INSERT INTO planificacion (inscripcion_id,titulo,fecha_clase,objetivo,recursos,tiempos,duracion_minutos,evaluacion_aprendizaje,interaccion_participantes,estado) VALUES (3,'T','2026-10-01','o','r','t',90,'e','i','PUBLICADA')"),
        @('Planificación enviada a revisión con campos vacíos',   3819, 'ck_planificacion_completa', "INSERT INTO planificacion (inscripcion_id,titulo,estado) VALUES (3,'T','EN_REVISION_GUIA')"),
        @('Devolver una planificación sin comentario',            3819, 'ck_revision_comentario',  "INSERT INTO revision_planificacion (planificacion_id,revisor_id,etapa,decision) VALUES (4,2,'GUIA','OBSERVADA')"),
        @('Segunda nota de voz para la misma evaluación',         1062, 'uq_nota_voz_evaluacion',  "INSERT INTO nota_voz (evaluacion_id,archivo_ruta,duracion_segundos) VALUES (1,'a.mp3',30); INSERT INTO nota_voz (evaluacion_id,archivo_ruta,duracion_segundos) VALUES (1,'b.mp3',40)"),
        @('Documento de planificación más grande que 10 MB',      3819, 'ck_documento_planificacion_tamano', "INSERT INTO documento_planificacion (planificacion_id,nombre_archivo,ruta_archivo,tipo_mime,tamano_bytes,subido_por) VALUES (3,'x.pdf','planificaciones/x.pdf','application/pdf',99999999,6)"),
        @('Escala SUS con un ítem fuera de 1 a 5 (HU-30)',       3819, 'ck_respuesta_sus_items',  "INSERT INTO respuesta_sus (validador_id,item1,item2,item3,item4,item5,item6,item7,item8,item9,item10) VALUES (3,6,1,1,1,1,1,1,1,1,1)"),
        @('Segunda Escala SUS del mismo validador (HU-30)',       1062, 'uq_respuesta_sus_validador', "INSERT INTO respuesta_sus (validador_id,item1,item2,item3,item4,item5,item6,item7,item8,item9,item10) VALUES (3,5,1,5,1,5,1,5,1,5,1); INSERT INTO respuesta_sus (validador_id,item1,item2,item3,item4,item5,item6,item7,item8,item9,item10) VALUES (3,4,2,4,2,4,2,4,2,4,2)"),
        @('Criterio 10 en la rúbrica de 9 criterios (HU-30)',     3819, 'ck_rubrica_criterio_numero', "INSERT INTO respuesta_rubrica_experto (validador_id) VALUES (3); SET @ru = LAST_INSERT_ID(); INSERT INTO respuesta_rubrica_criterio (respuesta_id,numero,puntaje) VALUES (@ru,10,4)"),
        @('Lectura del jefe en una inscripción inexistente (HU-20)', 1452, 'fk_lectura_chat_inscripcion', "INSERT INTO lectura_chat_jefe_carrera (inscripcion_id,usuario_id) VALUES (99999,32)"),
        @('Nota de un componente fuera de 1,0 a 7,0 (HU-09)',     3819, 'ck_nota_componente_nota', "INSERT INTO nota_componente (inscripcion_id,componente_id,nota,registrada_por) VALUES (11,4,7.5,5)"),
        @('Segunda nota para el mismo componente (HU-09)',        1062, 'uq_nota_componente',      "INSERT INTO nota_componente (inscripcion_id,componente_id,nota,registrada_por) VALUES (11,1,6.0,5)"),
        @('Componente de la nota con peso 0 (HU-09)',             3819, 'ck_componente_nota_peso', "INSERT INTO componente_nota (asignatura_id,nombre,peso,orden) VALUES (3,'X',0,1)"),
        @('Inscripción en un equipo inexistente (HU-10)',         1452, 'fk_inscripcion_equipo',   "UPDATE inscripcion SET equipo_id = 99999 WHERE id = 18"),
        @('Borrar un usuario que tiene inscripciones (FK)',       1451, 'foreign key constraint fails', "DELETE FROM usuario WHERE id = 4"),
        @('La app no puede cambiar la estructura (DROP TABLE)',   1142, 'DROP command denied',     "DROP TABLE usuario")
    )

    $fallos = 0
    foreach ($t in $pruebas) {
        $desc = $t[0]; $esperado = $t[1]; $fragmento = $t[2]; $sql = "START TRANSACTION; $($t[3]); ROLLBACK;"
        $r = Invoke-Mysql $UsuarioApp $claveApp $sql -Db $BaseDatos
        $codigo = if ($r.Error -match 'ERROR (\d+)') { [int]$Matches[1] } else { 0 }
        if ($codigo -eq $esperado -and $r.Error -like "*$fragmento*") {
            Write-Host ("  OK     error {0} ({1}): {2}" -f $codigo, $fragmento, $desc) -ForegroundColor Green
        } else {
            $fallos++
            Write-Host ("  FALLÓ  {0} -> se esperaba error {1} con '{2}' y fue {3}" -f $desc, $esperado, $fragmento, $codigo) -ForegroundColor Red
            if ($r.Error) { Write-Host "         $($r.Error.Trim())" -ForegroundColor DarkGray }
        }
    }
    Write-Host ""
    if ($fallos -eq 0) { Write-Host "Todas las pruebas negativas pasaron ($($pruebas.Count) de $($pruebas.Count))." -ForegroundColor Green }
    else { Write-Host "$fallos prueba(s) fallaron." -ForegroundColor Red; exit 1 }
}

if ($Accion -eq 'Instalar') { Install-Bd }
Test-Bd
