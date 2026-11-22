package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.PanelDto;
import cl.ubb.pem.practicas.dto.PanelDto.Avance;
import cl.ubb.pem.practicas.dto.PanelDto.Fila;
import cl.ubb.pem.practicas.dto.PanelDto.Pendiente;
import cl.ubb.pem.practicas.dto.PanelDto.PlanificacionEsperando;
import cl.ubb.pem.practicas.dto.PanelDto.SinEvaluar;
import cl.ubb.pem.practicas.dto.PanelDto.Totales;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-14: panel de monitoreo (solo lectura; el rol COORDINADOR o JEFE_CARRERA lo exige @PreAuthorize en el
 * controlador). Criterio 1: planificaciones pendientes, evaluaciones hechas y estudiantes sin evaluar; criterio 2:
 * filtro por periodo y asignatura (ambos opcionales). SUPUESTO S-79: una planificación "requiere atención" cuando lleva
 * más de {@code app.panel.dias-atencion} días (3) esperando al guía o al profesor del curso, contados desde su última
 * actualización (el envío o la última decisión).
 * Del trabajo de Alej (commit 7e9b132): una planificación de una práctica ya cerrada (APROBADA o REPROBADA) no está
 * pendiente de nada, aunque haya quedado en revisión: no se cuenta ni se lista como pendiente.
 */
@Service
@Transactional(readOnly = true)
public class PanelService {

    private static final Pattern PERIODO = Pattern.compile("\\d{4}-[12]");
    private static final Set<EstadoPlanificacion> PENDIENTES =
            Set.of(EstadoPlanificacion.EN_REVISION_GUIA, EstadoPlanificacion.EN_AUTORIZACION);
    /** Las que esperan a alguien: además de las dos anteriores, las devueltas, que esperan al estudiante. */
    private static final Set<EstadoPlanificacion> ESPERAN = Set.of(EstadoPlanificacion.EN_REVISION_GUIA,
            EstadoPlanificacion.EN_AUTORIZACION, EstadoPlanificacion.CON_OBSERVACIONES);

    private final InscripcionRepository inscripciones;
    private final PlanificacionRepository planificaciones;
    private final EvaluacionRepository evaluaciones;
    private final Clock reloj;
    private final ParametroService parametros;   // HU-27: el umbral lo edita el coordinador

    public PanelService(InscripcionRepository inscripciones, PlanificacionRepository planificaciones,
                        EvaluacionRepository evaluaciones, Clock reloj, ParametroService parametros) {
        this.inscripciones = inscripciones;
        this.planificaciones = planificaciones;
        this.evaluaciones = evaluaciones;
        this.reloj = reloj;
        this.parametros = parametros;
    }

    public PanelDto panel(Integer asignaturaId, String periodo) {
        String p = periodo == null || periodo.isBlank() ? null : periodo.trim();
        if (p != null && !PERIODO.matcher(p).matches()) {
            throw new ReglaNegocioException("El período debe tener el formato AAAA-S, por ejemplo 2026-2.");
        }
        List<Inscripcion> todas = inscripciones.buscar(null, asignaturaId, p, null, null, null);
        List<Long> ids = todas.stream().map(Inscripcion::getId).toList();
        List<Planificacion> planes = ids.isEmpty() ? List.of() : planificaciones.findPorInscripciones(ids);
        List<Evaluacion> todasLasEvaluaciones = ids.isEmpty() ? List.of() : evaluaciones.findTodasPorInscripciones(ids);
        List<Evaluacion> finalizadas = todasLasEvaluaciones.stream().filter(Evaluacion::estaFinalizada).toList();
        Map<Long, Long> borradoresPorInsc = todasLasEvaluaciones.stream().filter(e -> !e.estaFinalizada())
                .collect(Collectors.groupingBy(e -> e.getInscripcion().getId(), Collectors.counting()));
        Map<Long, List<Evaluacion>> finalizadasPorInsc = finalizadas.stream()
                .collect(Collectors.groupingBy(e -> e.getInscripcion().getId()));
        Set<Long> conEvaluacion = finalizadas.stream().map(e -> e.getInscripcion().getId()).collect(Collectors.toSet());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int diasAtencion = parametros.diasAtencionPanel();

        // Filas: una por asignatura y periodo, del periodo más reciente al más antiguo y por orden de la práctica
        Map<String, List<Inscripcion>> grupos = todas.stream()
                .sorted(Comparator.comparing(Inscripcion::getPeriodo).reversed()
                        .thenComparing(i -> i.getAsignatura().getOrden()))
                .collect(Collectors.groupingBy(i -> i.getAsignatura().getId() + "|" + i.getPeriodo(),
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<Planificacion>> plPorInsc = planes.stream().collect(Collectors.groupingBy(x -> x.getInscripcion().getId()));
        Map<Long, Long> evPorInsc = finalizadas.stream()
                .collect(Collectors.groupingBy(e -> e.getInscripcion().getId(), Collectors.counting()));
        List<Fila> filas = grupos.values().stream().map(g -> {
            var a = g.get(0).getAsignatura();
            List<Planificacion> pl = g.stream().flatMap(i -> plPorInsc.getOrDefault(i.getId(), List.of()).stream()).toList();
            List<Planificacion> abiertas = pl.stream().filter(x -> enCurso(x.getInscripcion())).toList();
            boolean evaluaGuia = Boolean.TRUE.equals(a.getRequiereCentroEducativo());
            return new Fila(a.getId(), a.getNombre(), a.getOrden(), g.get(0).getPeriodo(), g.size(),
                    (int) g.stream().filter(PanelService::enCurso).count(),
                    (int) abiertas.stream().filter(x -> x.getEstado() == EstadoPlanificacion.EN_REVISION_GUIA).count(),
                    (int) abiertas.stream().filter(x -> x.getEstado() == EstadoPlanificacion.EN_AUTORIZACION).count(),
                    (int) g.stream().mapToLong(i -> evPorInsc.getOrDefault(i.getId(), 0L)).sum(),
                    evaluaGuia ? (int) g.stream().filter(i -> enCurso(i) && !conEvaluacion.contains(i.getId())).count() : null,
                    (int) abiertas.stream().filter(x -> x.getEstado() == EstadoPlanificacion.CON_OBSERVACIONES).count(),
                    (int) pl.stream().filter(x -> x.getEstado() == EstadoPlanificacion.AUTORIZADA).count(),
                    (int) g.stream().mapToLong(i -> borradoresPorInsc.getOrDefault(i.getId(), 0L)).sum(),
                    promedio(g.stream().flatMap(i -> finalizadasPorInsc.getOrDefault(i.getId(), List.of()).stream()).toList()));
        }).toList();

        // Todas las planificaciones que esperan a alguien (del trabajo de Alej), la que lleva más tiempo primero
        List<Pendiente> pendientes = planes.stream()
                .filter(x -> ESPERAN.contains(x.getEstado()) && enCurso(x.getInscripcion()))
                .sorted(Comparator.comparing(Planificacion::getFechaActualizacion).thenComparing(Planificacion::getId))
                .map(x -> {
                    Inscripcion i = x.getInscripcion();
                    String rol = switch (x.getEstado()) {
                        case EN_REVISION_GUIA -> "PROFESOR_GUIA";
                        case EN_AUTORIZACION -> "PROFESOR_CURSO";
                        default -> "ESTUDIANTE";
                    };
                    Usuario quien = switch (rol) {
                        case "PROFESOR_GUIA" -> i.getProfesorGuia();
                        case "PROFESOR_CURSO" -> i.getProfesorCurso();
                        default -> i.getEstudiante().getUsuario();
                    };
                    long dias = ChronoUnit.DAYS.between(x.getFechaActualizacion(), ahora);
                    return new Pendiente(x.getId(), x.getTitulo(), i.getId(), i.getEstudiante().getUsuario().getNombreCompleto(),
                            i.getAsignatura().getNombre(), i.getPeriodo(), x.getEstado().name(),
                            quien == null ? null : quien.getNombreCompleto(), rol, x.getFechaClase(), dias,
                            !"ESTUDIANTE".equals(rol) && dias > diasAtencion);
                })
                .toList();

        // Avance por estudiante (del trabajo de Alej): una fila por inscripción del filtro
        List<Avance> avance = todas.stream()
                .sorted(Comparator.comparing((Inscripcion i) -> i.getAsignatura().getOrden())
                        .thenComparing(Comparator.comparing(Inscripcion::getPeriodo).reversed())
                        .thenComparing(i -> i.getEstudiante().getUsuario().getApellidos()))
                .map(i -> {
                    List<Planificacion> pl = plPorInsc.getOrDefault(i.getId(), List.of());
                    List<Evaluacion> ev = finalizadasPorInsc.getOrDefault(i.getId(), List.of());
                    return new Avance(i.getId(), i.getEstudiante().getId(), i.getEstudiante().getUsuario().getNombreCompleto(),
                            i.getAsignatura().getNombre(), i.getPeriodo(), i.getEstado(), pl.size(),
                            (int) pl.stream().filter(x -> x.getEstado() == EstadoPlanificacion.AUTORIZADA).count(),
                            ev.size(), promedio(ev), i.getNotaFinal());
                })
                .toList();

        // Requiere atención (1): planificaciones que esperan más días que el umbral (S-79), la más antigua primero
        List<PlanificacionEsperando> esperando = planes.stream()
                .filter(x -> PENDIENTES.contains(x.getEstado()) && enCurso(x.getInscripcion()))
                // por fecha y hora exactas (no por días enteros): dos del mismo día quedan en el orden en que llegaron
                .sorted(Comparator.comparing(Planificacion::getFechaActualizacion).thenComparing(Planificacion::getId))
                .map(x -> {
                    Inscripcion i = x.getInscripcion();
                    boolean guia = x.getEstado() == EstadoPlanificacion.EN_REVISION_GUIA;
                    Usuario quien = guia ? i.getProfesorGuia() : i.getProfesorCurso();
                    return new PlanificacionEsperando(x.getId(), x.getTitulo(), i.getId(),
                            i.getEstudiante().getUsuario().getNombreCompleto(), i.getAsignatura().getNombre(),
                            i.getPeriodo(), quien == null ? null : quien.getNombreCompleto(),
                            guia ? "PROFESOR_GUIA" : "PROFESOR_CURSO", x.getFechaClase(),
                            ChronoUnit.DAYS.between(x.getFechaActualizacion(), ahora));
                })
                .filter(x -> x.diasEsperando() > diasAtencion)
                .toList();

        // Requiere atención (2): en curso, en una práctica con guía que evalúa, y sin ninguna evaluación finalizada
        List<SinEvaluar> sinEvaluar = todas.stream()
                .filter(i -> enCurso(i) && Boolean.TRUE.equals(i.getAsignatura().getRequiereCentroEducativo())
                        && !conEvaluacion.contains(i.getId()))
                .sorted(Comparator.comparing((Inscripcion i) -> i.getAsignatura().getOrden())
                        .thenComparing(i -> i.getEstudiante().getUsuario().getApellidos()))
                .map(i -> new SinEvaluar(i.getId(), i.getEstudiante().getId(),
                        i.getEstudiante().getUsuario().getNombreCompleto(), i.getAsignatura().getNombre(), i.getPeriodo(),
                        i.getProfesorGuia() == null ? null : i.getProfesorGuia().getNombreCompleto(),
                        borradoresPorInsc.getOrDefault(i.getId(), 0L).intValue()))
                .toList();

        Totales totales = new Totales(filas.stream().mapToInt(Fila::inscritos).sum(),
                filas.stream().mapToInt(Fila::enCurso).sum(),
                filas.stream().mapToInt(Fila::planificacionesEnRevisionGuia).sum(),
                filas.stream().mapToInt(Fila::planificacionesEnAutorizacion).sum(),
                filas.stream().mapToInt(Fila::evaluacionesFinalizadas).sum(), sinEvaluar.size(),
                filas.stream().mapToInt(Fila::planificacionesConObservaciones).sum(),
                filas.stream().mapToInt(Fila::evaluacionesBorrador).sum());
        return new PanelDto(ahora, asignaturaId, p, diasAtencion, totales, filas, esperando, sinEvaluar, pendientes, avance);
    }

    /** Promedio (en la escala de la pauta, 2 decimales) de los promedios de unas evaluaciones finalizadas; nulo sin datos. */
    private static BigDecimal promedio(List<Evaluacion> finalizadas) {
        if (finalizadas.isEmpty()) {
            return null;
        }
        BigDecimal suma = finalizadas.stream().map(Evaluacion::getPromedio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(finalizadas.size()), 2, RoundingMode.HALF_UP);
    }

    private static boolean enCurso(Inscripcion i) {
        return i.getEstado() == EstadoInscripcion.EN_CURSO;
    }
}
