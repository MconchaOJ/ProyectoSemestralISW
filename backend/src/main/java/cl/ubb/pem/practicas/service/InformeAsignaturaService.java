package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.InformeAsignaturaDto;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.FilaEstudiante;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.Inscripciones;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.Notas;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.Planificaciones;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.PorDimension;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.PorPauta;
import cl.ubb.pem.practicas.dto.InformeAsignaturaDto.Tramo;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.DecisionRevision;
import cl.ubb.pem.practicas.entity.DimensionPauta;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.entity.RespuestaIndicador;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-08, criterio 2: informe de una asignatura en un período. Solo lectura y solo agregados de lo que ya existe; el
 * rol (COORDINADOR o JEFE_CARRERA, que ven toda la carrera) lo restringe @PreAuthorize en el controlador.
 * Las notas y los promedios se reparten en 4 tramos de la escala chilena: menos de 4,0 (bajo la nota de aprobación),
 * 4,0 a menos de 5,0, 5,0 a menos de 6,0 y 6,0 a 7,0.
 */
@Service
@Transactional(readOnly = true)
public class InformeAsignaturaService {

    private static final Pattern PERIODO = Pattern.compile("\\d{4}-[12]");
    private static final BigDecimal CUATRO = new BigDecimal("4.0");
    private static final BigDecimal CINCO = new BigDecimal("5.0");
    private static final BigDecimal SEIS = new BigDecimal("6.0");
    static final List<String> TRAMOS = List.of("Menos de 4,0", "4,0 a menos de 5,0", "5,0 a menos de 6,0", "6,0 a 7,0");

    private final AsignaturaPracticaRepository asignaturas;
    private final InscripcionRepository inscripciones;
    private final EvaluacionRepository evaluaciones;
    private final PlanificacionRepository planificaciones;
    private final InformeAsignaturaPdfGenerador pdf;
    private final Clock reloj;

    public InformeAsignaturaService(AsignaturaPracticaRepository asignaturas, InscripcionRepository inscripciones,
                                    EvaluacionRepository evaluaciones, PlanificacionRepository planificaciones,
                                    InformeAsignaturaPdfGenerador pdf, Clock reloj) {
        this.asignaturas = asignaturas;
        this.inscripciones = inscripciones;
        this.evaluaciones = evaluaciones;
        this.planificaciones = planificaciones;
        this.pdf = pdf;
        this.reloj = reloj;
    }

    public InformeAsignaturaDto informe(Integer asignaturaId, String periodo) {
        if (periodo == null || !PERIODO.matcher(periodo).matches()) {
            throw new ReglaNegocioException("El período debe tener el formato AAAA-S, por ejemplo 2026-2.");
        }
        AsignaturaPractica a = asignaturas.findById(asignaturaId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la asignatura con id " + asignaturaId + "."));

        List<Inscripcion> inscritas = inscripciones.buscar(null, asignaturaId, periodo, null, null, null).stream()
                .sorted(Comparator.comparing((Inscripcion i) -> i.getEstudiante().getUsuario().getApellidos())
                        .thenComparing(i -> i.getEstudiante().getUsuario().getNombre()))
                .toList();
        List<Long> ids = inscritas.stream().map(Inscripcion::getId).toList();
        List<Evaluacion> finalizadas = ids.isEmpty() ? List.of()
                : evaluaciones.findPorInscripciones(ids, EstadoEvaluacion.FINALIZADA);
        List<Planificacion> planes = ids.isEmpty() ? List.of() : planificaciones.findPorInscripciones(ids);

        return new InformeAsignaturaDto(a.getId(), a.getCodigo(), a.getNombre(), periodo, LocalDateTime.now(reloj),
                resumen(inscritas), notas(inscritas.stream().map(Inscripcion::getNotaFinal).toList()),
                porPauta(finalizadas), planificaciones(planes), filas(inscritas, finalizadas, planes));
    }

    /** El mismo informe en PDF. */
    public byte[] pdf(Integer asignaturaId, String periodo) {
        return pdf.generar(informe(asignaturaId, periodo));
    }

    private static Inscripciones resumen(List<Inscripcion> inscritas) {
        Map<EstadoInscripcion, Long> porEstado = inscritas.stream()
                .collect(Collectors.groupingBy(Inscripcion::getEstado, Collectors.counting()));
        return new Inscripciones(inscritas.size(), cuenta(porEstado, EstadoInscripcion.EN_CURSO),
                cuenta(porEstado, EstadoInscripcion.APROBADA), cuenta(porEstado, EstadoInscripcion.REPROBADA));
    }

    /** Estadística y tramos de un conjunto de notas o promedios (se ignoran los nulos: las EN_CURSO no tienen nota). */
    static Notas notas(Collection<BigDecimal> valores) {
        List<BigDecimal> v = valores.stream().filter(Objects::nonNull).toList();
        int[] enTramo = new int[TRAMOS.size()];
        for (BigDecimal n : v) {
            enTramo[n.compareTo(CUATRO) < 0 ? 0 : n.compareTo(CINCO) < 0 ? 1 : n.compareTo(SEIS) < 0 ? 2 : 3]++;
        }
        List<Tramo> tramos = new java.util.ArrayList<>();
        for (int i = 0; i < TRAMOS.size(); i++) {
            tramos.add(new Tramo(TRAMOS.get(i), enTramo[i]));
        }
        return new Notas(v.size(), promedio(v),
                v.stream().min(Comparator.naturalOrder()).orElse(null),
                v.stream().max(Comparator.naturalOrder()).orElse(null), tramos);
    }

    /** Una entrada por pauta (y versión) usada, en orden de id. */
    private static List<PorPauta> porPauta(List<Evaluacion> finalizadas) {
        Map<Integer, List<Evaluacion>> grupos = finalizadas.stream()
                .sorted(Comparator.comparing((Evaluacion e) -> e.getPauta().getId()))
                .collect(Collectors.groupingBy(e -> e.getPauta().getId(), LinkedHashMap::new, Collectors.toList()));
        return grupos.values().stream().map(evs -> {
            Pauta p = evs.get(0).getPauta();
            int maximo = p.indicadores().size() * p.getEscalaMax();
            BigDecimal puntaje = promedio(evs.stream().map(e -> BigDecimal.valueOf(e.getPuntajeTotal())).toList());
            BigDecimal logro = maximo == 0 || puntaje == null ? null
                    : puntaje.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(maximo), 1, RoundingMode.HALF_UP);
            List<RespuestaIndicador> respuestas = evs.stream().flatMap(e -> e.getRespuestas().stream()).toList();
            List<PorDimension> dimensiones = p.getDimensiones().stream()
                    .sorted(Comparator.comparing(DimensionPauta::getOrden))
                    .map(d -> new PorDimension(d.getNombre(), promedio(respuestas.stream()
                            .filter(r -> r.getIndicador().getDimension().getId().equals(d.getId()))
                            .map(r -> BigDecimal.valueOf(r.getPuntaje())).toList())))
                    .toList();
            return new PorPauta(p.getId(), p.getNombre(), p.getVersion(), p.getTipoEvaluador().name(), evs.size(),
                    maximo, puntaje, logro, notas(evs.stream().map(Evaluacion::getPromedio).toList()), dimensiones);
        }).toList();
    }

    private static Planificaciones planificaciones(List<Planificacion> planes) {
        Map<EstadoPlanificacion, Long> porEstado = planes.stream()
                .collect(Collectors.groupingBy(Planificacion::getEstado, Collectors.counting()));
        int devueltas = (int) planes.stream().filter(InformeAsignaturaService::fueDevuelta).count();
        return new Planificaciones(planes.size(), cuenta(porEstado, EstadoPlanificacion.BORRADOR),
                cuenta(porEstado, EstadoPlanificacion.EN_REVISION_GUIA),
                cuenta(porEstado, EstadoPlanificacion.CON_OBSERVACIONES),
                cuenta(porEstado, EstadoPlanificacion.EN_AUTORIZACION),
                cuenta(porEstado, EstadoPlanificacion.AUTORIZADA), devueltas);
    }

    private static boolean fueDevuelta(Planificacion p) {
        return p.getRevisiones().stream().anyMatch(r -> r.getDecision() == DecisionRevision.OBSERVADA);
    }

    private static List<FilaEstudiante> filas(List<Inscripcion> inscritas, List<Evaluacion> finalizadas,
                                              List<Planificacion> planes) {
        Map<Long, List<Evaluacion>> evPorInsc = finalizadas.stream()
                .collect(Collectors.groupingBy(e -> e.getInscripcion().getId()));
        Map<Long, List<Planificacion>> plPorInsc = planes.stream()
                .collect(Collectors.groupingBy(p -> p.getInscripcion().getId()));
        return inscritas.stream().map(i -> {
            List<Evaluacion> evs = evPorInsc.getOrDefault(i.getId(), List.of());
            List<Planificacion> pls = plPorInsc.getOrDefault(i.getId(), List.of());
            var u = i.getEstudiante().getUsuario();
            return new FilaEstudiante(i.getEstudiante().getId(), u.getApellidos() + ", " + u.getNombre(),
                    i.getEstudiante().getRut(), i.getEstado().name(), i.getNotaFinal(), evs.size(),
                    promedio(evs.stream().map(Evaluacion::getPromedio).toList()), pls.size(),
                    (int) pls.stream().filter(p -> p.getEstado() == EstadoPlanificacion.AUTORIZADA).count());
        }).toList();
    }

    /** Promedio con 2 decimales, o nulo si no hay valores. */
    private static BigDecimal promedio(Collection<BigDecimal> valores) {
        List<BigDecimal> v = valores.stream().filter(Objects::nonNull).toList();
        if (v.isEmpty()) {
            return null;
        }
        return v.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(v.size()), 2, RoundingMode.HALF_UP);
    }

    private static <E> int cuenta(Map<E, Long> porEstado, E estado) {
        return porEstado.getOrDefault(estado, 0L).intValue();
    }
}
