package cl.ubb.pem.practicas.exception;

/** 401: credenciales incorrectas. No distingue entre correo y contraseña. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
