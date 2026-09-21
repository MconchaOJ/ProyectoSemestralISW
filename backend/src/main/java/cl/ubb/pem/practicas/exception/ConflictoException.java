package cl.ubb.pem.practicas.exception;

/** 409: el estado actual del recurso no permite la operación (duplicado o transición no permitida). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
