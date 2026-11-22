package cl.ubb.pem.practicas.entity;

/** HU-09: de dónde sale la nota de un componente de la estructura evaluativa. */
public enum TipoComponente {
    /** De las evaluaciones finalizadas con una pauta del sistema (promediadas si hay varias). */
    PAUTA,
    /** La escribe el profesor del curso: la califica otro docente fuera del sistema (productos, informe, TANEP). */
    EXTERNO
}
