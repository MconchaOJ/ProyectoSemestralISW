package cl.ubb.pem.practicas.security;

/** Nombres de los roles: coinciden con la tabla rol (HU-01). Spring les antepone "ROLE_". */
public final class Roles {

    public static final String ESTUDIANTE = "ESTUDIANTE";
    public static final String PROFESOR_GUIA = "PROFESOR_GUIA";
    public static final String PROFESOR_CURSO = "PROFESOR_CURSO";
    public static final String COORDINADOR = "COORDINADOR";
    /**
     * Rol propio, no el coordinador (resuelve S-38). Desde el 2026-10-11 (confirmado por el cliente): el mismo alcance
     * de LECTURA que el coordinador más la cartola de prácticas (HU-31), sin crear, editar, aprobar ni evaluar nada por
     * este rol. Si además evalúa como guía, eso le llega por su segundo rol (migración 004).
     */
    public static final String JEFE_CARRERA = "JEFE_CARRERA";
    /**
     * Tutor de la universidad (HU-07, migración 006): solo en Práctica Profesional. Ve las inscripciones donde es tutor
     * y evalúa en ellas, solo con pautas de tipo TUTOR (criterio 3).
     */
    public static final String TUTOR = "TUTOR";

    private Roles() {
    }
}
