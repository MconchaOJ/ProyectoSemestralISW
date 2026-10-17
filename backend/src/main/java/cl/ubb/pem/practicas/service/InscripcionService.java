package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.InscripcionCierreRequest;
import cl.ubb.pem.practicas.dto.InscripcionCrearRequest;
import cl.ubb.pem.practicas.dto.InscripcionDto;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-02: inscripción de un estudiante en una asignatura de práctica, por periodo. */
@Service
public class InscripcionService {

    private final InscripcionRepository inscripciones;
    private final EstudianteRepository estudiantes;
    private final AsignaturaPracticaRepository asignaturas;
    private final UsuarioRepository usuarios;
    private final AccesoService acceso;
    private final Clock reloj;

    public InscripcionService(InscripcionRepository inscripciones, EstudianteRepository estudiantes,
                              AsignaturaPracticaRepository asignaturas, UsuarioRepository usuarios,
                              AccesoService acceso, Clock reloj) {
        this.inscripciones = inscripciones;
        this.estudiantes = estudiantes;
        this.asignaturas = asignaturas;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.reloj = reloj;
    }

    @Transactional
    public InscripcionDto crear(InscripcionCrearRequest r) {
        Estudiante estudiante = estudiantes.findById(r.estudianteId()).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el estudiante con id " + r.estudianteId() + "."));
        AsignaturaPractica asignatura = asignaturas.findById(r.asignaturaId()).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la asignatura con id " + r.asignaturaId() + "."));

        Usuario curso = profesor(r.profesorCursoId(), Roles.PROFESOR_CURSO, "profesor del curso");
        boolean vaAUnColegio = Boolean.TRUE.equals(asignatura.getRequiereCentroEducativo());
        Usuario guia = null;
        if (r.profesorGuiaId() != null) {
            // El profesor guía (colaborador) es el del colegio: donde no se va a un colegio, no existe (S-25).
            if (!vaAUnColegio) {
                throw new ReglaNegocioException("La asignatura " + asignatura.getNombre()
                        + " no se realiza en un colegio, así que no tiene profesor guía: deja profesorGuiaId vacío.");
            }
            guia = profesor(r.profesorGuiaId(), Roles.PROFESOR_GUIA, "profesor guía");
        } else if (vaAUnColegio) {
            throw new ReglaNegocioException("La asignatura " + asignatura.getNombre()
                    + " se realiza en un centro educativo: debes asignar un profesor guía.");
        }

        Usuario tutor = r.tutorId() == null ? null : tutorValido(r.tutorId(), asignatura, guia, curso);

        if (inscripciones.existsByEstudianteIdAndAsignaturaIdAndPeriodo(estudiante.getId(), asignatura.getId(), r.periodo())) {
            throw new ConflictoException("El estudiante ya está inscrito en " + asignatura.getNombre()
                    + " en el periodo " + r.periodo() + ".");
        }

        Inscripcion i = new Inscripcion();
        i.setEstudiante(estudiante);
        i.setAsignatura(asignatura);
        i.setPeriodo(r.periodo());
        i.setProfesorGuia(guia);
        i.setProfesorCurso(curso);
        i.setTutor(tutor);
        i.setEstado(EstadoInscripcion.EN_CURSO);
        return InscripcionDto.desde(inscripciones.save(i));
    }

    /** Cada rol ve solo sus inscripciones: el coordinador todas; el estudiante, las suyas; los profesores, las asignadas. */
    @Transactional(readOnly = true)
    public List<InscripcionDto> listar(Long estudianteId, Integer asignaturaId, String periodo, UsuarioAutenticado actor) {
        AccesoService.Alcance a = acceso.alcanceDe(actor);
        String periodoFiltro = periodo == null || periodo.isBlank() ? null : periodo.trim();
        return inscripciones.buscar(estudianteId, asignaturaId, periodoFiltro, a.guiaId(), a.cursoId(), a.usuarioEstudianteId())
                .stream().filter(i -> acceso.puedeVerInscripcion(i, actor))   // cuentas con dos roles (ver AccesoService)
                .map(InscripcionDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public InscripcionDto obtener(Long id, UsuarioAutenticado actor) {
        Inscripcion i = buscar(id);
        acceso.exigirVerInscripcion(i, actor);
        return InscripcionDto.desde(i);
    }

    /**
     * Cierra la inscripción con su resultado. SUPUESTO S-22 (extra al backlog): así el coordinador carga las
     * asignaturas aprobadas que alimentan el historial y el contexto del chatbot (S-14). No se valida que la nota
     * sea coherente con el estado (la nota mínima de aprobación no está confirmada). Desde HU-31 también guarda el
     * día del cierre, que es la fecha por la que filtra la cartola de prácticas.
     */
    @Transactional
    public InscripcionDto cerrar(Long id, InscripcionCierreRequest r) {
        if (r.estado() == EstadoInscripcion.EN_CURSO) {
            throw new ReglaNegocioException("El estado de cierre debe ser APROBADA o REPROBADA.");
        }
        Inscripcion i = buscar(id);
        if (i.getEstado() != EstadoInscripcion.EN_CURSO) {
            throw new ConflictoException("La inscripción ya está cerrada (" + i.getEstado() + ").");
        }
        i.setEstado(r.estado());
        i.setNotaFinal(r.notaFinal());
        i.setFechaCierre(LocalDate.now(reloj));
        return InscripcionDto.desde(inscripciones.save(i));
    }

    /**
     * HU-07: asigna, cambia o quita (tutorId nulo) el tutor. SUPUESTO S-57: solo con la inscripción EN_CURSO; las
     * evaluaciones que ya hizo un tutor anterior quedan a su nombre (registro histórico, como el cambio de guía).
     */
    @Transactional
    public InscripcionDto asignarTutor(Long id, Long tutorId) {
        Inscripcion i = buscar(id);
        if (i.getEstado() != EstadoInscripcion.EN_CURSO) {
            throw new ConflictoException("La inscripción está cerrada (" + i.getEstado() + "): ya no se cambia su tutor.");
        }
        i.setTutor(tutorId == null ? null : tutorValido(tutorId, i.getAsignatura(), i.getProfesorGuia(), i.getProfesorCurso()));
        return InscripcionDto.desde(inscripciones.save(i));
    }

    /** Criterio de HU-07: el tutor existe solo donde la asignatura lo lleva, tiene rol TUTOR y es otra persona. */
    private Usuario tutorValido(Long tutorId, AsignaturaPractica asignatura, Usuario guia, Usuario curso) {
        if (!Boolean.TRUE.equals(asignatura.getLlevaTutor())) {
            throw new ReglaNegocioException("La asignatura " + asignatura.getNombre()
                    + " no tiene tutor de la universidad (solo Práctica Profesional): deja tutorId vacío.");
        }
        Usuario tutor = profesor(tutorId, Roles.TUTOR, "tutor");
        if (tutor.getId().equals(curso.getId()) || (guia != null && tutor.getId().equals(guia.getId()))) {
            throw new ReglaNegocioException("El tutor debe ser una persona distinta del profesor guía y del profesor del curso.");
        }
        return tutor;
    }

    Inscripcion buscar(Long id) {
        return inscripciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la inscripción con id " + id + "."));
    }

    /** El profesor debe existir, estar activo y tener el rol pedido, como principal o como segundo rol (HU-02, criterio 7). */
    private Usuario profesor(Long id, String rolEsperado, String descripcion) {
        Usuario u = usuarios.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el " + descripcion + " con id " + id + "."));
        if (!u.tieneRol(rolEsperado)) {
            throw new ReglaNegocioException("El usuario " + u.getNombreCompleto() + " no puede ser " + descripcion
                    + ": su rol es " + u.getRol().getNombre() + " y se requiere " + rolEsperado + ".");
        }
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new ReglaNegocioException("El " + descripcion + " " + u.getNombreCompleto() + " está inactivo.");
        }
        return u;
    }
}
