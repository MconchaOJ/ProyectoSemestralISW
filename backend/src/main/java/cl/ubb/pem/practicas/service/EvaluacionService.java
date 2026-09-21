package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EvaluacionActualizarRequest;
import cl.ubb.pem.practicas.dto.EvaluacionCrearRequest;
import cl.ubb.pem.practicas.dto.EvaluacionDto;
import cl.ubb.pem.practicas.dto.EvaluacionResumenDto;
import cl.ubb.pem.practicas.dto.RespuestasRequest;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Indicador;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.entity.RespuestaIndicador;
import cl.ubb.pem.practicas.entity.TipoEvaluador;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PautaRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-03: evaluar una clase con la pauta digital. Una evaluación nace en BORRADOR, se le cargan los puntajes
 * y al finalizarla se calculan el puntaje total y el promedio; desde ahí ya no se puede modificar.
 */
@Service
public class EvaluacionService {

    private final EvaluacionRepository evaluaciones;
    private final InscripcionRepository inscripciones;
    private final PautaRepository pautas;
    private final PlanificacionRepository planificaciones;
    private final UsuarioRepository usuarios;
    private final AccesoService acceso;

    public EvaluacionService(EvaluacionRepository evaluaciones, InscripcionRepository inscripciones,
                             PautaRepository pautas, PlanificacionRepository planificaciones,
                             UsuarioRepository usuarios, AccesoService acceso) {
        this.evaluaciones = evaluaciones;
        this.inscripciones = inscripciones;
        this.pautas = pautas;
        this.planificaciones = planificaciones;
        this.usuarios = usuarios;
        this.acceso = acceso;
    }

    @Transactional
    public EvaluacionDto crear(EvaluacionCrearRequest r, UsuarioAutenticado actor) {
        Inscripcion inscripcion = inscripciones.findById(r.inscripcionId()).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la inscripción con id " + r.inscripcionId() + "."));
        // Criterio 6: solo el guía asignado a ESA inscripción puede evaluarla.
        if (inscripcion.getProfesorGuia() == null || !inscripcion.getProfesorGuia().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el profesor guía asignado a la inscripción puede evaluarla.");
        }
        Pauta pauta = pautas.findById(r.pautaId()).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la pauta con id " + r.pautaId() + "."));
        validarPauta(pauta, inscripcion, actor);
        if (inscripcion.getEstado() != EstadoInscripcion.EN_CURSO) {
            throw new ConflictoException("Solo se puede evaluar una inscripción EN_CURSO (esta está " + inscripcion.getEstado() + ").");
        }
        validarHoras(r.horaInicio(), r.horaTermino());
        Usuario evaluador = usuarios.findById(actor.id()).orElseThrow();

        Evaluacion e = new Evaluacion();
        e.setInscripcion(inscripcion);
        e.setPauta(pauta);
        e.setEvaluador(evaluador);
        e.setPlanificacion(planificacionDeLaClase(r.planificacionId(), inscripcion));
        copiarCabecera(e, r.fechaClase(), r.establecimiento(), r.curso(), r.horaInicio(), r.horaTermino(),
                r.objetivoClase(), r.observacionesGenerales());
        e.setEstado(EstadoEvaluacion.BORRADOR);
        return EvaluacionDto.desde(evaluaciones.save(e));
    }

    @Transactional
    public EvaluacionDto actualizar(Long id, EvaluacionActualizarRequest r, UsuarioAutenticado actor) {
        Evaluacion e = obtenerPropia(id, actor);
        exigirBorrador(e);
        validarHoras(r.horaInicio(), r.horaTermino());
        e.setPlanificacion(planificacionDeLaClase(r.planificacionId(), e.getInscripcion()));
        copiarCabecera(e, r.fechaClase(), r.establecimiento(), r.curso(), r.horaInicio(), r.horaTermino(),
                r.objetivoClase(), r.observacionesGenerales());
        return EvaluacionDto.desde(evaluaciones.save(e));
    }

    /** Guarda (o reemplaza) los puntajes de uno o varios indicadores. Se valida todo antes de guardar nada. */
    @Transactional
    public EvaluacionDto registrarRespuestas(Long id, RespuestasRequest request, UsuarioAutenticado actor) {
        Evaluacion e = obtenerPropia(id, actor);
        exigirBorrador(e);
        Pauta pauta = e.getPauta();
        Map<Integer, Indicador> indicadoresDeLaPauta = pauta.indicadores().stream()
                .collect(Collectors.toMap(Indicador::getId, i -> i));

        Set<Integer> vistos = new HashSet<>();
        for (RespuestasRequest.Item item : request.respuestas()) {
            Indicador ind = indicadoresDeLaPauta.get(item.indicadorId());
            if (ind == null) {   // criterio 3: indicador de otra pauta -> 400
                throw new ReglaNegocioException("El indicador " + item.indicadorId() + " no pertenece a la pauta de la evaluación.");
            }
            if (!vistos.add(item.indicadorId())) {
                throw new ReglaNegocioException("El indicador " + ind.getNumero() + " viene repetido en la petición.");
            }
            if (item.puntaje() < pauta.getEscalaMin() || item.puntaje() > pauta.getEscalaMax()) {   // criterio 3: rango de la pauta
                throw new ReglaNegocioException("El puntaje del indicador " + ind.getNumero() + " debe ser un entero entre "
                        + pauta.getEscalaMin() + " y " + pauta.getEscalaMax() + " (recibido: " + item.puntaje() + ").");
            }
        }

        Map<Integer, RespuestaIndicador> existentes = new HashMap<>();
        e.getRespuestas().forEach(r -> existentes.put(r.getIndicador().getId(), r));
        for (RespuestasRequest.Item item : request.respuestas()) {
            RespuestaIndicador r = existentes.get(item.indicadorId());
            if (r == null) {
                r = new RespuestaIndicador();
                r.setEvaluacion(e);
                r.setIndicador(indicadoresDeLaPauta.get(item.indicadorId()));
                e.getRespuestas().add(r);
            }
            r.setPuntaje(item.puntaje());
            r.setComentario(item.comentario());
        }
        return EvaluacionDto.desde(evaluaciones.save(e));
    }

    /** Criterio 5: exige los indicadores de la pauta puntuados, calcula total y promedio y congela la evaluación. */
    @Transactional
    public EvaluacionDto finalizar(Long id, UsuarioAutenticado actor) {
        Evaluacion e = obtenerPropia(id, actor);
        exigirBorrador(e);
        List<Indicador> indicadores = e.getPauta().indicadores();
        Set<Integer> respondidos = e.getRespuestas().stream().map(r -> r.getIndicador().getId()).collect(Collectors.toSet());
        List<Integer> faltan = indicadores.stream().filter(i -> !respondidos.contains(i.getId())).map(Indicador::getNumero).toList();
        if (!faltan.isEmpty()) {
            throw new ReglaNegocioException("No se puede finalizar: faltan " + faltan.size() + " de " + indicadores.size()
                    + " indicadores por puntuar (números " + faltan + ").");
        }
        int total = e.getRespuestas().stream().mapToInt(RespuestaIndicador::getPuntaje).sum();
        BigDecimal promedio = BigDecimal.valueOf(total).divide(BigDecimal.valueOf(indicadores.size()), 2, RoundingMode.HALF_UP);
        e.finalizar(total, promedio, LocalDateTime.now());
        return EvaluacionDto.desde(evaluaciones.save(e));
    }

    @Transactional(readOnly = true)
    public EvaluacionDto obtener(Long id, UsuarioAutenticado actor) {
        Evaluacion e = buscar(id);
        if (!puedeVer(e, actor)) {
            throw new AccesoDenegadoException("No tienes acceso a esta evaluación.");
        }
        return EvaluacionDto.desde(e);
    }

    /** Cada rol ve lo suyo. Los borradores solo los ven quien evalúa y el coordinador. */
    @Transactional(readOnly = true)
    public List<EvaluacionResumenDto> listar(Long inscripcionId, EstadoEvaluacion estado, UsuarioAutenticado actor) {
        AccesoService.Alcance a = acceso.alcanceDe(actor);
        return evaluaciones.buscar(inscripcionId, estado, a.guiaId(), a.cursoId(), a.usuarioEstudianteId()).stream()
                .filter(e -> puedeVer(e, actor))
                .map(EvaluacionResumenDto::desde)
                .toList();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Regla de visibilidad. SUPUESTO S-29: una evaluación en BORRADOR es privada del evaluador (y del coordinador);
     * el estudiante y el profesor del curso ven solo las FINALIZADAS.
     */
    boolean puedeVer(Evaluacion e, UsuarioAutenticado actor) {
        if (!acceso.puedeVerInscripcion(e.getInscripcion(), actor)) {
            return false;
        }
        if (e.getEstado() == EstadoEvaluacion.FINALIZADA) {
            return true;
        }
        return actor.tieneRol(Roles.COORDINADOR) || e.getEvaluador().getId().equals(actor.id());
    }

    private Evaluacion buscar(Long id) {
        return evaluaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la evaluación con id " + id + "."));
    }

    /** La evaluación debe existir y ser del evaluador que la modifica. */
    private Evaluacion obtenerPropia(Long id, UsuarioAutenticado actor) {
        Evaluacion e = buscar(id);
        if (!e.getEvaluador().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el profesor guía que creó la evaluación puede modificarla.");
        }
        return e;
    }

    private void exigirBorrador(Evaluacion e) {
        if (e.estaFinalizada()) {
            throw new ConflictoException("La evaluación está FINALIZADA y ya no se puede modificar.");
        }
    }

    private void validarPauta(Pauta pauta, Inscripcion inscripcion, UsuarioAutenticado actor) {
        if (!Boolean.TRUE.equals(pauta.getActiva())) {
            throw new ReglaNegocioException("La pauta " + pauta.getId() + " no está activa.");
        }
        if (!pauta.getAsignatura().getId().equals(inscripcion.getAsignatura().getId())) {
            throw new ReglaNegocioException("La pauta no corresponde a la asignatura de la inscripción ("
                    + inscripcion.getAsignatura().getNombre() + ").");
        }
        // El evaluador debe corresponder al tipo de evaluador de la pauta (MER, sección 5).
        String rolRequerido = rolDelEvaluador(pauta.getTipoEvaluador());
        if (rolRequerido == null || !rolRequerido.equals(actor.rol())) {
            throw new ReglaNegocioException("Esta pauta la aplica un evaluador de tipo " + pauta.getTipoEvaluador()
                    + (rolRequerido == null ? " (aún no disponible: llega en el Sprint 2)." : " (rol " + rolRequerido + ")."));
        }
    }

    private static String rolDelEvaluador(TipoEvaluador tipo) {
        return switch (tipo) {
            case COLABORADOR -> Roles.PROFESOR_GUIA;
            case ACADEMICO -> Roles.PROFESOR_CURSO;
            case TUTOR -> null;   // el tutor se incorpora en el Sprint 2 (S-12)
        };
    }

    private void validarHoras(LocalTime inicio, LocalTime termino) {
        if (!termino.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de término debe ser posterior a la hora de inicio.");
        }
    }

    /** La planificación de la clase es opcional; si viene, debe ser de la misma inscripción y estar AUTORIZADA. */
    private Planificacion planificacionDeLaClase(Long planificacionId, Inscripcion inscripcion) {
        if (planificacionId == null) {
            return null;
        }
        Planificacion p = planificaciones.findById(planificacionId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la planificación con id " + planificacionId + "."));
        if (!p.getInscripcion().getId().equals(inscripcion.getId())) {
            throw new ReglaNegocioException("La planificación " + planificacionId + " no pertenece a esta inscripción.");
        }
        if (p.getEstado() != EstadoPlanificacion.AUTORIZADA) {
            throw new ConflictoException("La planificación debe estar AUTORIZADA para vincularla a la clase evaluada (está "
                    + p.getEstado() + ").");
        }
        return p;
    }

    private static void copiarCabecera(Evaluacion e, java.time.LocalDate fecha, String establecimiento, String curso,
                                       LocalTime inicio, LocalTime termino, String objetivo, String observaciones) {
        e.setFechaClase(fecha);
        e.setEstablecimiento(establecimiento.trim());
        e.setCurso(curso.trim());
        e.setHoraInicio(inicio);
        e.setHoraTermino(termino);
        e.setObjetivoClase(objetivo.trim());
        e.setObservacionesGenerales(observaciones == null || observaciones.isBlank() ? null : observaciones.trim());
    }
}
