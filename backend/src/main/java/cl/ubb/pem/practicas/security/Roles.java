package cl.ubb.pem.practicas.security;

/** Nombres de los roles: coinciden con la tabla rol (HU-01). Spring les antepone "ROLE_". */
public final class Roles {

    public static final String ESTUDIANTE = "ESTUDIANTE";
    public static final String PROFESOR_GUIA = "PROFESOR_GUIA";
    public static final String PROFESOR_CURSO = "PROFESOR_CURSO";
    public static final String COORDINADOR = "COORDINADOR";

    private Roles() {
    }
}
