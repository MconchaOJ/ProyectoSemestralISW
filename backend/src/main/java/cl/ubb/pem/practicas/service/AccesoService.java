package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import org.springframework.stereotype.Component;

/**
 * Reglas de "quién puede ver qué" que dependen de los datos y no solo del rol (HU-03, HU-04 y HU-05).
 * El rol lo filtra @PreAuthorize en el controlador; aquí se decide si ESE usuario tiene relación con la inscripción.
 */
@Component
public class AccesoService {

    /** Filtros de alcance para los listados: un valor nulo significa "sin restricción por ese lado". */
    public record Alcance(Long guiaId, Long cursoId, Long usuarioEstudianteId) {
    }

    private final InscripcionRepository inscripciones;

    public AccesoService(InscripcionRepository inscripciones) {
        this.inscripciones = inscripciones;
    }

    /**
     * El coordinador ve todo; cada otro rol ve solo lo suyo. Cualquier rol que no conozca esta clase (por ejemplo
     * uno nuevo agregado a la tabla rol) NO ve nada: se falla hacia el lado seguro, no hacia "ver todo".
     */
    public Alcance alcanceDe(UsuarioAutenticado actor) {
        return switch (actor.rol()) {
            case Roles.COORDINADOR -> new Alcance(null, null, null);
            case Roles.PROFESOR_GUIA -> new Alcance(actor.id(), null, null);
            case Roles.PROFESOR_CURSO -> new Alcance(null, actor.id(), null);
            case Roles.ESTUDIANTE -> new Alcance(null, null, actor.id());
            default -> throw new AccesoDenegadoException("Tu rol no tiene acceso a este recurso.");
        };
    }

    public boolean puedeVerInscripcion(Inscripcion i, UsuarioAutenticado actor) {
        return switch (actor.rol()) {
            case Roles.COORDINADOR -> true;
            case Roles.ESTUDIANTE -> i.getEstudiante().getUsuario().getId().equals(actor.id());
            case Roles.PROFESOR_GUIA -> i.getProfesorGuia() != null && i.getProfesorGuia().getId().equals(actor.id());
            case Roles.PROFESOR_CURSO -> i.getProfesorCurso().getId().equals(actor.id());
            default -> false;
        };
    }

    public void exigirVerInscripcion(Inscripcion i, UsuarioAutenticado actor) {
        if (!puedeVerInscripcion(i, actor)) {
            throw new AccesoDenegadoException("No tienes acceso a esta inscripción.");
        }
    }

    /**
     * Historial completo de un estudiante (HU-05, criterio 5). SUPUESTO S-21: un profesor guía o del curso ve el
     * recorrido completo del estudiante si está asignado a él en al menos una inscripción, porque el objetivo es
     * ver la evolución en el tiempo y no solo la práctica que él dirige.
     */
    public boolean puedeVerHistorial(Estudiante e, UsuarioAutenticado actor) {
        return switch (actor.rol()) {
            case Roles.COORDINADOR -> true;
            case Roles.ESTUDIANTE -> e.getUsuario().getId().equals(actor.id());
            case Roles.PROFESOR_GUIA -> inscripciones.existsByEstudianteIdAndProfesorGuiaId(e.getId(), actor.id());
            case Roles.PROFESOR_CURSO -> inscripciones.existsByEstudianteIdAndProfesorCursoId(e.getId(), actor.id());
            default -> false;
        };
    }
}
