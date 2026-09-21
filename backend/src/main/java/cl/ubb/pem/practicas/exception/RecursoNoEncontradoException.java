package cl.ubb.pem.practicas.exception;

/** 404: el recurso pedido no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
