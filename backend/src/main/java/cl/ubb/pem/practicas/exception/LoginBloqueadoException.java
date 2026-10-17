package cl.ubb.pem.practicas.exception;

/** 429: el correo superó los intentos fallidos de inicio de sesión y está bloqueado por un tiempo. */
public class LoginBloqueadoException extends RuntimeException {

    private final long segundosRestantes;

    public LoginBloqueadoException(String mensaje, long segundosRestantes) {
        super(mensaje);
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
