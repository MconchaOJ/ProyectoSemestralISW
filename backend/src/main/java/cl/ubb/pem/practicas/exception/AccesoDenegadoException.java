package cl.ubb.pem.practicas.exception;

/** 403: el usuario está autenticado pero no le corresponde acceder a este recurso. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
