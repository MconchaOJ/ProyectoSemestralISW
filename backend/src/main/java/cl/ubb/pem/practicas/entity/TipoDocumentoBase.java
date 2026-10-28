package cl.ubb.pem.practicas.entity;

/**
 * Los documentos de la base de conocimiento del chatbot (HU-29): la lista "Archivos que deberías cargar" de la
 * propuesta RetroPlani Mate V3, en su mismo orden. Uno vigente por tipo (SUPUESTO S-69).
 */
public enum TipoDocumentoBase {
    MOMENTOS_CLASE("Momentos de la clase", "Revisar inicio, desarrollo y cierre"),
    RUBRICA_PLANIFICACION("Rúbrica de planificación de clases", "Criterios de evaluación de la planificación"),
    DUA_DIVERSIFICACION("DUA y diversificación de la enseñanza", "Revisar DUA, barreras, apoyos y acciones"),
    BANCO_COMENTARIOS("Comentarios de retroalimentación", "Calibrar tono, estilo y banco de comentarios"),
    ENLACES_RECURSOS("Enlaces a recursos para el diseño de clases", "Enlaces de apoyo para inicio, cierre, instrumentos"),
    BASES_CURRICULARES_7B_2M("Bases Curriculares de Matemática 7.º básico a 2.º medio", "Verificar OA, nivel y eje"),
    BASES_CURRICULARES_3M_4M("Bases Curriculares de Matemática 3.º y 4.º medio", "Verificar OA, nivel y eje"),
    ACCIONES_DUA("Ejemplos de acciones DUA en Matemática", "Banco orientativo de ideas DUA"),
    PLANIFICACION_COMENTADA_1("Ejemplo 1 de planificación comentada", "Calibrar la retroalimentación"),
    PLANIFICACION_COMENTADA_2("Ejemplo 2 de planificación comentada", "Calibrar la retroalimentación");

    private final String nombre;
    private final String uso;

    TipoDocumentoBase(String nombre, String uso) {
        this.nombre = nombre;
        this.uso = uso;
    }

    public String nombre() {
        return nombre;
    }

    public String uso() {
        return uso;
    }
}
