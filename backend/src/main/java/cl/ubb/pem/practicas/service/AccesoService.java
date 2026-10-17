package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Reglas de "quién puede ver qué" que dependen de los datos y no solo del rol (HU-03, HU-04 y HU-05).
 * El rol lo filtra @PreAuthorize en el controlador; aquí se decide si ESE usuario tiene relación con la inscripción.
 * Todo lo de esta clase es de LECTURA: crear, editar, revisar o evaluar lo comprueba cada servicio con el id del
 * actor (el estudiante dueño, el guía asignado, el evaluador), nunca con "puede verla".
 *
 * Cuentas con dos roles (migración 004, confirmado por el cliente el 2026-10-11): lo que ve la cuenta es la SUMA
 * de lo que ve con cada uno de sus roles.
 */
@Component
public class AccesoService {

    /** Filtros de alcance para los listados: un valor nulo significa "sin restricción por ese lado". */
    public record Alcance(Long guiaId, Long cursoId, Long usuarioEstudianteId) {

        static final Alcance TODO = new Alcance(null, null, null);
    }

    private final InscripcionRepository inscripciones;

    public AccesoService(InscripcionRepository inscripciones) {
        this.inscripciones = inscripciones;
    }

    /**
     * Coordinador y jefe de carrera leen todo (el jefe de carrera, desde el 2026-10-11 y confirmado por el cliente,
     * tiene el mismo alcance de lectura que el coordinador, pero ningún permiso de escritura por ese rol).
     */
    public static boolean veTodo(UsuarioAutenticado actor) {
        return actor.tieneRol(Roles.COORDINADOR) || actor.tieneRol(Roles.JEFE_CARRERA);
    }

    /**
     * Filtro de la consulta de los listados. Cada rol ve solo lo suyo; un rol que no conozca esta clase (por ejemplo
     * uno nuevo agregado a la tabla rol) NO ve nada: se falla hacia el lado seguro, no hacia "ver todo".
     * Con dos roles restringidos (por ejemplo guía y profesor del curso) la consulta no puede expresar el "o":
     * se pide sin filtro y el servicio deja solo las filas que {@link #puedeVerInscripcion} acepta. El tutor (HU-07)
     * tampoco tiene columna en las consultas: se pide sin filtro y se filtra igual (son pocas filas).
     */
    public Alcance alcanceDe(UsuarioAutenticado actor) {
        if (veTodo(actor)) {
            return Alcance.TODO;
        }
        List<Alcance> porRol = actor.roles().stream().map(r -> alcanceDeRol(r, actor.id())).filter(Objects::nonNull).toList();
        if (porRol.isEmpty()) {
            throw new AccesoDenegadoException("Tu rol no tiene acceso a este recurso.");
        }
        return porRol.size() == 1 ? porRol.get(0) : Alcance.TODO;
    }

    private static Alcance alcanceDeRol(String rol, Long id) {
        return switch (rol) {
            case Roles.PROFESOR_GUIA -> new Alcance(id, null, null);
            case Roles.PROFESOR_CURSO -> new Alcance(null, id, null);
            case Roles.ESTUDIANTE -> new Alcance(null, null, id);
            case Roles.TUTOR -> Alcance.TODO;   // la consulta no filtra por tutor: lo hace puedeVerInscripcion
            default -> null;
        };
    }

    public boolean puedeVerInscripcion(Inscripcion i, UsuarioAutenticado actor) {
        return veTodo(actor) || actor.roles().stream().anyMatch(r -> switch (r) {
            case Roles.ESTUDIANTE -> i.getEstudiante().getUsuario().getId().equals(actor.id());
            case Roles.PROFESOR_GUIA -> i.getProfesorGuia() != null && i.getProfesorGuia().getId().equals(actor.id());
            case Roles.PROFESOR_CURSO -> i.getProfesorCurso().getId().equals(actor.id());
            case Roles.TUTOR -> i.getTutor() != null && i.getTutor().getId().equals(actor.id());
            default -> false;
        });
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
        return veTodo(actor) || actor.roles().stream().anyMatch(r -> switch (r) {
            case Roles.ESTUDIANTE -> e.getUsuario().getId().equals(actor.id());
            case Roles.PROFESOR_GUIA -> inscripciones.existsByEstudianteIdAndProfesorGuiaId(e.getId(), actor.id());
            case Roles.PROFESOR_CURSO -> inscripciones.existsByEstudianteIdAndProfesorCursoId(e.getId(), actor.id());
            case Roles.TUTOR -> inscripciones.existsByEstudianteIdAndTutorId(e.getId(), actor.id());
            default -> false;
        });
    }
}
