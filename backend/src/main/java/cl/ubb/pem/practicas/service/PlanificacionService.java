package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.config.PlanificacionProperties;
import cl.ubb.pem.practicas.dto.PlanificacionActualizarRequest;
import cl.ubb.pem.practicas.dto.PlanificacionCrearRequest;
import cl.ubb.pem.practicas.dto.PlanificacionDto;
import cl.ubb.pem.practicas.dto.PlanificacionResumenDto;
import cl.ubb.pem.practicas.dto.RevisionRequest;
import cl.ubb.pem.practicas.entity.DecisionRevision;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.EtapaRevision;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.DocumentoPlanificacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-04: planificación de clase con flujo de aprobación. Máquina de estados (MER, sección 5):
 * <pre>
 * BORRADOR ──enviar──▶ EN_REVISION_GUIA ──guía aprueba──▶ EN_AUTORIZACION ──curso autoriza──▶ AUTORIZADA
 *   (sin profesor guía —ciclo inicial— "enviar" lleva directo a EN_AUTORIZACION)
 *                          │                                   │
 *   CON_OBSERVACIONES ◀────┴──── guía o curso devuelven (comentario obligatorio) ───┘
 *   CON_OBSERVACIONES ──enviar──▶ EN_REVISION_GUIA   (S-10: repite el flujo desde el guía)
 * </pre>
 * Toda transición no permitida responde 409; el actor equivocado, 403. Cada decisión queda en el historial.
 */
@Service
public class PlanificacionService {

    private final PlanificacionRepository planificaciones;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final AccesoService acceso;
    private final Clock reloj;
    private final PlanificacionProperties reglas;
    private final DocumentoPlanificacionRepository documentos;

    public PlanificacionService(PlanificacionRepository planificaciones, InscripcionRepository inscripciones,
                                UsuarioRepository usuarios, AccesoService acceso, Clock reloj,
                                PlanificacionProperties reglas, DocumentoPlanificacionRepository documentos) {
        this.planificaciones = planificaciones;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.reloj = reloj;
        this.reglas = reglas;
        this.documentos = documentos;
    }

    @Transactional
    public PlanificacionDto crear(PlanificacionCrearRequest r, UsuarioAutenticado actor) {
        Inscripcion inscripcion = inscripciones.findById(r.inscripcionId()).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la inscripción con id " + r.inscripcionId() + "."));
        if (!esDelEstudiante(inscripcion, actor)) {
            throw new AccesoDenegadoException("Solo el estudiante de la inscripción puede crear planificaciones en ella.");
        }
        if (inscripcion.getEstado() != EstadoInscripcion.EN_CURSO) {
            throw new ConflictoException("Solo se puede planificar en una inscripción EN_CURSO (esta está "
                    + inscripcion.getEstado() + ").");
        }
        if (!Boolean.TRUE.equals(inscripcion.getAsignatura().getRequierePlanificacion())) {
            throw new ConflictoException("En " + inscripcion.getAsignatura().getNombre()
                    + " no se entregan planificaciones (solo trabajos hechos en clase).");
        }
        Planificacion p = new Planificacion();
        p.setInscripcion(inscripcion);
        p.setEstado(EstadoPlanificacion.BORRADOR);
        copiarElementos(p, r.titulo(), r.fechaClase(), r.objetivo(), r.recursos(), r.tiempos(), r.duracionMinutos(),
                r.evaluacionAprendizaje(), r.interaccionParticipantes());
        return PlanificacionDto.desde(planificaciones.save(p));
    }

    /** Criterio 5 y 8: solo en BORRADOR o CON_OBSERVACIONES; una AUTORIZADA (o en revisión) no se toca. */
    @Transactional
    public PlanificacionDto actualizar(Long id, PlanificacionActualizarRequest r, UsuarioAutenticado actor) {
        Planificacion p = obtenerDelEstudiante(id, actor);
        if (!p.esEditable()) {
            throw new ConflictoException("La planificación está " + p.getEstado()
                    + ": solo se puede editar en BORRADOR o CON_OBSERVACIONES.");
        }
        copiarElementos(p, r.titulo(), r.fechaClase(), r.objetivo(), r.recursos(), r.tiempos(), r.duracionMinutos(),
                r.evaluacionAprendizaje(), r.interaccionParticipantes());
        return PlanificacionDto.desde(planificaciones.saveAndFlush(p));
    }

    /**
     * Criterios 2, 3 y 12: para enviarla a revisión todos los elementos del formato son obligatorios, y también
     * la planificación formal en Word o PDF (S-47): los campos del formulario quedan como resumen y el documento
     * adjunto como el respaldo formal. Basta con que exista al menos un documento adjunto (no hay que volver a
     * subirlo en cada reenvío).
     */
    @Transactional
    public PlanificacionDto enviar(Long id, UsuarioAutenticado actor) {
        Planificacion p = obtenerDelEstudiante(id, actor);
        if (!p.esEditable()) {
            throw new ConflictoException("La planificación está " + p.getEstado()
                    + ": solo se puede enviar desde BORRADOR o CON_OBSERVACIONES.");
        }
        List<String> faltan = p.elementosFaltantes();
        if (!documentos.existsByPlanificacionId(p.getId())) {
            faltan.add("documentoFormal");
        }
        if (!faltan.isEmpty()) {
            throw new ReglaNegocioException("Para enviar la planificación a revisión faltan estos elementos: " + faltan + ".");
        }
        // S-35: se entrega con anticipación (un estudiante reportó "como mínimo unos 3 días antes de la clase").
        LocalDate primeraFechaValida = LocalDate.now(reloj).plusDays(reglas.diasAnticipacionMinimos());
        if (p.getFechaClase().isBefore(primeraFechaValida)) {
            throw new ReglaNegocioException("La planificación debe entregarse con al menos " + reglas.diasAnticipacionMinimos()
                    + " días de anticipación: enviándola hoy, la clase debe ser el " + primeraFechaValida + " o después.");
        }
        // S-34: sin profesor guía (ciclo inicial, que no va a un colegio) la planificación va directo al profesor del curso.
        boolean tieneGuia = p.getInscripcion().getProfesorGuia() != null;
        p.setEstado(tieneGuia ? EstadoPlanificacion.EN_REVISION_GUIA : EstadoPlanificacion.EN_AUTORIZACION);
        return PlanificacionDto.desde(planificaciones.saveAndFlush(p));
    }

    /** Criterio 4: el guía asignado aprueba (pasa a EN_AUTORIZACION) o devuelve con observaciones. */
    @Transactional
    public PlanificacionDto revisarComoGuia(Long id, RevisionRequest r, UsuarioAutenticado actor) {
        Planificacion p = buscar(id);
        Usuario guia = p.getInscripcion().getProfesorGuia();
        if (guia == null || !guia.getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el profesor guía asignado a la inscripción puede revisar esta planificación.");
        }
        return revisar(p, r, actor, EtapaRevision.GUIA, EstadoPlanificacion.EN_REVISION_GUIA, EstadoPlanificacion.EN_AUTORIZACION);
    }

    /** Criterio 6: el profesor del curso autoriza (AUTORIZADA) o devuelve (vuelve a CON_OBSERVACIONES, S-10). */
    @Transactional
    public PlanificacionDto revisarComoProfesorDelCurso(Long id, RevisionRequest r, UsuarioAutenticado actor) {
        Planificacion p = buscar(id);
        if (!p.getInscripcion().getProfesorCurso().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el profesor del curso asignado a la inscripción puede autorizar esta planificación.");
        }
        return revisar(p, r, actor, EtapaRevision.CURSO, EstadoPlanificacion.EN_AUTORIZACION, EstadoPlanificacion.AUTORIZADA);
    }

    /** Criterio 9: cada rol ve las planificaciones de sus inscripciones, en cualquier estado. */
    @Transactional(readOnly = true)
    public PlanificacionDto obtener(Long id, UsuarioAutenticado actor) {
        Planificacion p = buscar(id);
        acceso.exigirVerInscripcion(p.getInscripcion(), actor);
        return PlanificacionDto.desde(p);
    }

    @Transactional(readOnly = true)
    public List<PlanificacionResumenDto> listar(Long inscripcionId, EstadoPlanificacion estado, UsuarioAutenticado actor) {
        AccesoService.Alcance a = acceso.alcanceDe(actor);
        return planificaciones.buscar(inscripcionId, estado, a.guiaId(), a.cursoId(), a.usuarioEstudianteId()).stream()
                .filter(p -> acceso.puedeVerInscripcion(p.getInscripcion(), actor))   // cuentas con dos roles
                .map(PlanificacionResumenDto::desde).toList();
    }

    // ---------------------------------------------------------------------------------------------

    /** Núcleo común de las dos etapas de revisión: comprueba el estado, exige comentario al devolver y guarda la decisión. */
    private PlanificacionDto revisar(Planificacion p, RevisionRequest r, UsuarioAutenticado actor, EtapaRevision etapa,
                                     EstadoPlanificacion estadoRequerido, EstadoPlanificacion estadoSiAprueba) {
        if (p.getEstado() != estadoRequerido) {
            throw new ConflictoException("La planificación está " + p.getEstado() + ": la etapa " + etapa
                    + " solo puede revisarla cuando está " + estadoRequerido + ".");
        }
        String comentario = limpiar(r.comentario());
        if (r.decision() == DecisionRevision.OBSERVADA && comentario == null) {
            throw new ReglaNegocioException("Al devolver una planificación con observaciones el comentario es obligatorio.");
        }
        Usuario revisor = usuarios.findById(actor.id()).orElseThrow();
        p.agregarRevision(revisor, etapa, r.decision(), comentario);
        p.setEstado(r.decision() == DecisionRevision.APROBADA ? estadoSiAprueba : EstadoPlanificacion.CON_OBSERVACIONES);
        return PlanificacionDto.desde(planificaciones.saveAndFlush(p));
    }

    private Planificacion buscar(Long id) {
        return planificaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la planificación con id " + id + "."));
    }

    private Planificacion obtenerDelEstudiante(Long id, UsuarioAutenticado actor) {
        Planificacion p = buscar(id);
        if (!esDelEstudiante(p.getInscripcion(), actor)) {
            throw new AccesoDenegadoException("Solo el estudiante dueño de la planificación puede modificarla o enviarla.");
        }
        return p;
    }

    private boolean esDelEstudiante(Inscripcion i, UsuarioAutenticado actor) {
        return i.getEstudiante().getUsuario().getId().equals(actor.id());
    }

    private static void copiarElementos(Planificacion p, String titulo, LocalDate fecha, String objetivo, String recursos,
                                        String tiempos, Integer duracion, String evaluacion, String interaccion) {
        p.setTitulo(titulo.trim());
        p.setFechaClase(fecha);
        p.setObjetivo(limpiar(objetivo));
        p.setRecursos(limpiar(recursos));
        p.setTiempos(limpiar(tiempos));
        p.setDuracionMinutos(duracion);
        p.setEvaluacionAprendizaje(limpiar(evaluacion));
        p.setInteraccionParticipantes(limpiar(interaccion));
    }

    /** Un texto vacío o solo con espacios cuenta como no completado (nulo). */
    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
