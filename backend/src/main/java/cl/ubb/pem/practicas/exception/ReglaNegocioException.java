package cl.ubb.pem.practicas.exception;

/** 400: la petición es válida en su forma pero rompe una regla de negocio. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
