package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.PanelDto;
import cl.ubb.pem.practicas.dto.PanelDto.AsignaturaPanelDto;
import cl.ubb.pem.practicas.dto.PanelDto.AvanceDto;
import cl.ubb.pem.practicas.dto.PanelDto.PendienteDto;
import cl.ubb.pem.practicas.dto.PanelDto.ResumenDto;
import cl.ubb.pem.practicas.dto.PanelDto.SinEvaluarDto;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PautaRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-14: panel de monitoreo del coordinador. Solo lectura y en 4 consultas (inscripciones, evaluaciones,
 * planificaciones y las asignaturas con pauta), sin repetir consultas por cada inscripción. El rol (coordinador o jefe
 * de carrera) lo restringe @PreAuthorize en el controlador: los dos ven toda la carrera.
 */
@Service
@Transactional(readOnly = true)
public class PanelService {

    private static final Pattern PERIODO = Pattern.compile("^[0-9]{4}-[12]$");
    /**
     * Enviadas y esperando a un profesor, o devueltas esperando al estudiante. El BORRADOR aún no se envía. Solo cuentan
     * en inscripciones EN_CURSO: en una práctica cerrada ya nadie las va a revisar (S-81).
     */
    private static final Set<EstadoPlanificacion> PENDIENTES = EnumSet.of(EstadoPlanificacion.EN_REVISION_GUIA,
            EstadoPlanificacion.EN_AUTORIZACION, EstadoPlanificacion.CON_OBSERVACIONES);

    private final InscripcionRepository inscripciones;
    private final EvaluacionRepository evaluaciones;
    private final PlanificacionRepository planificaciones;
    private final PautaRepository pautas;
    private final Clock reloj;

    public PanelService(InscripcionRepository inscripciones, EvaluacionRepository evaluaciones,
                        PlanificacionRepository planificaciones, PautaRepository pautas, Clock reloj) {
        this.inscripciones = inscripciones;
        this.evaluaciones = evaluaciones;
        this.planificaciones = planificaciones;
        this.pautas = pautas;
        this.reloj = reloj;
    }

    /** Criterio 2: periodo y asignatura opcionales (nulo o vacío = todos). */
    public PanelDto panel(String periodo, Integer asignaturaId) {
        String filtro = periodo == null || periodo.isBlank() ? null : periodo.strip();
        if (filtro != null && !PERIODO.matcher(filtro).matches()) {
            throw new ReglaNegocioException("El periodo debe tener el formato AAAA-S, por ejemplo 2026-2.");
        }
        List<Inscripcion> lista = inscripciones.findParaPanel(asignaturaId, filtro);
        List<Long> ids = lista.stream().map(Inscripcion::getId).toList();
        Map<Long, List<Evaluacion>> evals = ids.isEmpty() ? Map.of()
                : evaluaciones.findTodasPorInscripciones(ids).stream()
                        .collect(Collectors.groupingBy(e -> e.getInscripcion().getId()));
        Map<Long, List<Planificacion>> planes = ids.isEmpty() ? Map.of()
                : planificaciones.findPorInscripciones(ids).stream()
                        .collect(Collectors.groupingBy(p -> p.getInscripcion().getId()));
        // SUPUESTO S-81: "sin evaluar" solo donde hay con qué evaluar (la asignatura tiene alguna pauta)
        Set<Integer> conPauta = pautas.findAll().stream().map(p -> p.getAsignatura().getId()).collect(Collectors.toSet());

        List<SinEvaluarDto> sinEvaluar = lista.stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.EN_CURSO && conPauta.contains(i.getAsignatura().getId()))
                .filter(i -> finalizadas(evals.get(i.getId())).isEmpty())
                .map(i -> new SinEvaluarDto(i.getId(), i.getEstudiante().getId(), nombre(i), i.getAsignatura().getNombre(),
                        i.getPeriodo(), i.getProfesorGuia() == null ? null : i.getProfesorGuia().getNombreCompleto(),
                        borradores(evals.get(i.getId()))))
                .toList();
        Set<Long> idsSinEvaluar = sinEvaluar.stream().map(SinEvaluarDto::inscripcionId).collect(Collectors.toSet());

        LocalDate hoy = LocalDate.now(reloj);
        List<PendienteDto> pendientes = lista.stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.EN_CURSO)
                .flatMap(i -> planes.getOrDefault(i.getId(), List.of()).stream()
                        .filter(p -> PENDIENTES.contains(p.getEstado()))
                        .map(p -> new PendienteDto(p.getId(), i.getId(), nombre(i), i.getAsignatura().getNombre(),
                                p.getTitulo(), p.getFechaClase(), p.getEstado(), esperaA(p, i),
                                Math.max(0, ChronoUnit.DAYS.between(p.getFechaActualizacion().toLocalDate(), hoy)))))
                .sorted(Comparator.comparingLong(PendienteDto::diasEsperando).reversed()
                        .thenComparing(PendienteDto::planificacionId))
                .toList();

        List<AvanceDto> avance = lista.stream().map(i -> {
            List<Evaluacion> fin = finalizadas(evals.get(i.getId()));
            List<Planificacion> ps = planes.getOrDefault(i.getId(), List.of());
            return new AvanceDto(i.getId(), i.getEstudiante().getId(), nombre(i), i.getAsignatura().getNombre(),
                    i.getPeriodo(), i.getEstado(), ps.size(), contar(ps, EstadoPlanificacion.AUTORIZADA), fin.size(),
                    SintesisInscripcion.promedio(fin.stream().map(Evaluacion::getPromedio).toList()),
                    i.getNotaFinal(), i.getNotaCalculada());
        }).toList();

        // Una fila por asignatura, en el orden de la malla (la consulta ya viene ordenada por asignatura)
        Map<AsignaturaPractica, List<Inscripcion>> porAsignatura = lista.stream()
                .collect(Collectors.groupingBy(Inscripcion::getAsignatura, LinkedHashMap::new, Collectors.toList()));
        List<AsignaturaPanelDto> filas = porAsignatura.entrySet().stream().map(en -> {
            List<Inscripcion> deEsta = en.getValue();
            List<Planificacion> ps = deEsta.stream().flatMap(i -> planes.getOrDefault(i.getId(), List.of()).stream()).toList();
            List<Planificacion> abiertas = deEsta.stream().filter(i -> i.getEstado() == EstadoInscripcion.EN_CURSO)
                    .flatMap(i -> planes.getOrDefault(i.getId(), List.of()).stream()).toList();
            List<Evaluacion> es = deEsta.stream().flatMap(i -> evals.getOrDefault(i.getId(), List.of()).stream()).toList();
            List<Evaluacion> fin = finalizadas(es);
            AsignaturaPractica a = en.getKey();
            return new AsignaturaPanelDto(a.getId(), a.getNombre(), a.getOrden(), deEsta.size(), enCurso(deEsta),
                    contar(abiertas, EstadoPlanificacion.EN_REVISION_GUIA),
                    contar(abiertas, EstadoPlanificacion.EN_AUTORIZACION),
                    contar(abiertas, EstadoPlanificacion.CON_OBSERVACIONES), contar(ps, EstadoPlanificacion.AUTORIZADA),
                    fin.size(), es.size() - fin.size(),
                    (int) deEsta.stream().filter(i -> idsSinEvaluar.contains(i.getId())).count(),
                    SintesisInscripcion.promedio(fin.stream().map(Evaluacion::getPromedio).toList()));
        }).toList();

        List<Planificacion> abiertas = lista.stream().filter(i -> i.getEstado() == EstadoInscripcion.EN_CURSO)
                .flatMap(i -> planes.getOrDefault(i.getId(), List.of()).stream()).toList();
        List<Evaluacion> todasEvals = evals.values().stream().flatMap(List::stream).toList();
        int finalizadasTotal = finalizadas(todasEvals).size();
        ResumenDto resumen = new ResumenDto(lista.size(), enCurso(lista),
                contar(abiertas, EstadoPlanificacion.EN_REVISION_GUIA),
                contar(abiertas, EstadoPlanificacion.EN_AUTORIZACION),
                contar(abiertas, EstadoPlanificacion.CON_OBSERVACIONES), finalizadasTotal,
                todasEvals.size() - finalizadasTotal, sinEvaluar.size());

        return new PanelDto(filtro, asignaturaId, resumen, filas, pendientes, sinEvaluar, avance);
    }

    private static List<Evaluacion> finalizadas(List<Evaluacion> lista) {
        return lista == null ? List.of()
                : lista.stream().filter(e -> e.getEstado() == EstadoEvaluacion.FINALIZADA).toList();
    }

    private static int borradores(List<Evaluacion> lista) {
        return lista == null ? 0 : (int) lista.stream().filter(e -> e.getEstado() == EstadoEvaluacion.BORRADOR).count();
    }

    private static int contar(List<Planificacion> lista, EstadoPlanificacion estado) {
        return (int) lista.stream().filter(p -> p.getEstado() == estado).count();
    }

    private static int enCurso(List<Inscripcion> lista) {
        return (int) lista.stream().filter(i -> i.getEstado() == EstadoInscripcion.EN_CURSO).count();
    }

    private static String nombre(Inscripcion i) {
        return i.getEstudiante().getUsuario().getNombreCompleto();
    }

    /** Quién debe actuar: el guía, el profesor del curso o el propio estudiante (como PlanificacionDto.pendienteDe). */
    private static String esperaA(Planificacion p, Inscripcion i) {
        return switch (p.getEstado()) {
            case EN_REVISION_GUIA -> i.getProfesorGuia() == null ? "Profesor guía" : i.getProfesorGuia().getNombreCompleto();
            case EN_AUTORIZACION -> i.getProfesorCurso().getNombreCompleto();
            default -> "Estudiante";
        };
    }
}
