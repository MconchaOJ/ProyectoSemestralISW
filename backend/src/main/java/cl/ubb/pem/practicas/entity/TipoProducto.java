package cl.ubb.pem.practicas.entity;

/**
 * HU-18: tipos de informes y productos que el estudiante sube a su inscripción. Los tres primeros los nombra el
 * backlog; OTRO cubre lo que no calza (por ejemplo la autoevaluación de Trabajo Colaborativo). SUPUESTO S-94: todos
 * los tipos en las 6 prácticas y varios archivos por tipo.
 */
public enum TipoProducto {
    INFORME_FINAL("Informe final"),
    DIARIO_REFLEXION("Diario de reflexión"),
    PROYECTO_INTERVENCION("Proyecto de intervención"),
    OTRO("Otro producto");

    private final String nombre;

    TipoProducto(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
