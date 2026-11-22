package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.config.PlanificacionProperties;
import cl.ubb.pem.practicas.dto.ParametroDto;
import cl.ubb.pem.practicas.entity.ParametroSistema;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.ParametroSistemaRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-27: reglas operativas que edita el coordinador en la pantalla "Parámetros del sistema". Se leen de la base en
 * cada petición, así que un cambio rige desde la siguiente (criterio 2). Si falta la fila de un parámetro (una base sin
 * la migración 014), se usa el valor de application.properties o el de siempre. El rango de cada uno (S-80) lo
 * comprueban este servicio (400) y la base (ck_parametro_rango).
 *
 * 2026-11-22: la exigencia de la escala de notas (S-81, 60 %) pasa a ser un parámetro (migración 017). Es el único que
 * también puede editar el JEFE_CARRERA (decisión del usuario); los demás, solo el COORDINADOR (403). Cambiarla rige para
 * las evaluaciones que se finalicen después: la nota de una evaluación ya finalizada queda guardada y no cambia.
 */
@Service
public class ParametroService {

    public static final String DIAS_ANTICIPACION = "planificacion.dias_anticipacion";
    public static final String DIAS_ATENCION_PANEL = "panel.dias_atencion";
    public static final String DOCUMENTO_MB = "archivos.documento_mb";
    public static final String AUDIO_MB = "archivos.audio_mb";
    public static final String EXIGENCIA_PCT = "evaluacion.exigencia_pct";
    /** Roles que editan cada parámetro: la exigencia también el jefe de carrera; el resto, solo el coordinador. */
    private static final List<String> SOLO_COORDINADOR = List.of(Roles.COORDINADOR);
    private static final List<String> COORDINADOR_Y_JEFE = List.of(Roles.COORDINADOR, Roles.JEFE_CARRERA);
    private static final long MB = 1024L * 1024L;
    private static final Logger LOG = LoggerFactory.getLogger(ParametroService.class);

    private final ParametroSistemaRepository parametros;
    private final UsuarioRepository usuarios;
    private final Clock reloj;
    private final int diasAnticipacionInicial;
    private final int diasAtencionInicial;

    public ParametroService(ParametroSistemaRepository parametros, UsuarioRepository usuarios, Clock reloj,
                            PlanificacionProperties reglas, @Value("${app.panel.dias-atencion:3}") int diasAtencion) {
        this.parametros = parametros;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.diasAnticipacionInicial = reglas.diasAnticipacionMinimos();
        this.diasAtencionInicial = diasAtencion;
    }

    /** Días mínimos entre enviar una planificación y la clase (S-35). */
    public int diasAnticipacion() {
        return valor(DIAS_ANTICIPACION, diasAnticipacionInicial);
    }

    /** Días que puede esperar revisión una planificación antes de "requerir atención" en el panel (S-79). */
    public int diasAtencionPanel() {
        return valor(DIAS_ATENCION_PANEL, diasAtencionInicial);
    }

    /** Tamaño máximo de un documento adjunto, en bytes; nunca sobre el techo de la base (10 MB). */
    public long tamanoMaximoDocumento() {
        return Math.min(valor(DOCUMENTO_MB, 10) * MB, AlmacenamientoArchivosService.TAMANO_MAXIMO_DOCUMENTO);
    }

    /** Tamaño máximo del audio de una nota de voz, en bytes; nunca sobre el techo de 25 MB. */
    public long tamanoMaximoAudio() {
        return Math.min(valor(AUDIO_MB, 25) * MB, AlmacenamientoArchivosService.TAMANO_MAXIMO_AUDIO);
    }

    /** Exigencia de la escala de notas (S-81), como fracción: 60 % -> 0,60. */
    public BigDecimal exigencia() {
        return BigDecimal.valueOf(valor(EXIGENCIA_PCT, 60)).movePointLeft(2);
    }

    /** Roles que pueden editar este parámetro. */
    public static List<String> rolesQueEditan(String clave) {
        return EXIGENCIA_PCT.equals(clave) ? COORDINADOR_Y_JEFE : SOLO_COORDINADOR;
    }

    @Transactional(readOnly = true)
    public List<ParametroDto> listar() {
        return parametros.findAllByOrderByClaveAsc().stream().map(ParametroDto::desde).toList();
    }

    @Transactional
    public ParametroDto actualizar(String clave, int valor, UsuarioAutenticado actor) {
        ParametroSistema p = parametros.findById(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el parámetro «" + clave + "»."));
        if (rolesQueEditan(clave).stream().noneMatch(actor::tieneRol)) {
            throw new AccesoDenegadoException("Solo el coordinador puede cambiar «" + p.getDescripcion() + "».");
        }
        if (valor < p.getMinimo() || valor > p.getMaximo()) {
            throw new ReglaNegocioException("«" + p.getDescripcion() + "» debe estar entre " + p.getMinimo() + " y "
                    + p.getMaximo() + " " + p.getUnidad() + ".");
        }
        int anterior = p.getValor();
        p.setValor(valor);
        p.setEditadoPor(usuarios.getReferenceById(actor.id()));
        p.setFechaEdicion(LocalDateTime.now(reloj));
        ParametroSistema guardado = parametros.saveAndFlush(p);
        LOG.info("El usuario {} cambió el parámetro {}: {} -> {}", actor.id(), clave, anterior, valor);
        return ParametroDto.desde(guardado);
    }

    private int valor(String clave, int siFalta) {
        return parametros.findById(clave).map(ParametroSistema::getValor).orElse(siFalta);
    }
}
