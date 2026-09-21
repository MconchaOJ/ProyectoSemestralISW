package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.AsignaturaDto;
import cl.ubb.pem.practicas.dto.EstudianteDto;
import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.dto.HistorialDto.AsignaturaAprobadaDto;
import cl.ubb.pem.practicas.dto.HistorialDto.InscripcionHistorialDto;
import cl.ubb.pem.practicas.dto.PlanificacionResumenDto;
import cl.ubb.pem.practicas.dto.UsuarioResumenDto;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-05: historial (recorrido) del estudiante. Solo lectura: junta lo que ya existe en 4 consultas
 * (inscripciones, evaluaciones finalizadas, planificaciones) sin repetir consultas por cada inscripción.
 */
@Service
@Transactional(readOnly = true)
public class HistorialService {

    private final EstudianteRepository estudiantes;
    private final InscripcionRepository inscripciones;
    private final EvaluacionRepository evaluaciones;
    private final PlanificacionRepository planificaciones;
    private final AccesoService acceso;

    public HistorialService(EstudianteRepository estudiantes, InscripcionRepository inscripciones,
                            EvaluacionRepository evaluaciones, PlanificacionRepository planificaciones,
                            AccesoService acceso) {
        this.estudiantes = estudiantes;
        this.inscripciones = inscripciones;
        this.evaluaciones = evaluaciones;
        this.planificaciones = planificaciones;
        this.acceso = acceso;
    }

    public HistorialDto historialDe(Long estudianteId, UsuarioAutenticado actor) {
        Estudiante e = estudiantes.findById(estudianteId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el estudiante con id " + estudianteId + "."));
        if (!acceso.puedeVerHistorial(e, actor)) {
            throw new AccesoDenegadoException("No tienes acceso al historial de este estudiante.");
        }
        return armar(e);
    }

    /** El historial del estudiante que hizo la petición. */
    public HistorialDto historialPropio(UsuarioAutenticado actor) {
        Estudiante e = estudiantes.findByUsuarioId(actor.id()).orElseThrow(
                () -> new RecursoNoEncontradoException("El usuario no tiene una ficha de estudiante."));
        return armar(e);
    }

    private HistorialDto armar(Estudiante e) {
        // Criterio 1: inscripciones en orden cronológico (periodo y luego orden de la práctica)
        List<Inscripcion> recorrido = inscripciones.findRecorrido(e.getId());
        List<Long> ids = recorrido.stream().map(Inscripcion::getId).toList();

        Map<Long, List<Evaluacion>> evaluacionesPorInscripcion = ids.isEmpty() ? Map.of()
                : evaluaciones.findPorInscripciones(ids, EstadoEvaluacion.FINALIZADA).stream()
                        .collect(Collectors.groupingBy(ev -> ev.getInscripcion().getId()));
        Map<Long, List<Planificacion>> planificacionesPorInscripcion = ids.isEmpty() ? Map.of()
                : planificaciones.findPorInscripciones(ids).stream()
                        .collect(Collectors.groupingBy(p -> p.getInscripcion().getId()));

        List<InscripcionHistorialDto> detalle = recorrido.stream()
                .map(i -> new InscripcionHistorialDto(i.getId(), AsignaturaDto.desde(i.getAsignatura()), i.getPeriodo(),
                        i.getEstado(), i.getNotaFinal(), UsuarioResumenDto.desde(i.getProfesorGuia()),
                        UsuarioResumenDto.desde(i.getProfesorCurso()),
                        evaluacionesPorInscripcion.getOrDefault(i.getId(), List.of()).stream()
                                .map(EvaluacionResumenDto::desde).toList(),
                        planificacionesPorInscripcion.getOrDefault(i.getId(), List.of()).stream()
                                .map(PlanificacionResumenDto::desde).toList()))
                .toList();

        // Criterio 4: lo que el chatbot necesitará como contexto (S-14)
        List<AsignaturaAprobadaDto> aprobadas = recorrido.stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.APROBADA)
                .map(i -> new AsignaturaAprobadaDto(i.getAsignatura().getId(), i.getAsignatura().getCodigo(),
                        i.getAsignatura().getNombre(), i.getPeriodo(), i.getNotaFinal()))
                .toList();

        return new HistorialDto(EstudianteDto.desde(e), aprobadas, detalle);
    }
}
