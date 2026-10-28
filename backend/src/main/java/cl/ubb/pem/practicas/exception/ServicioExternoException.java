package cl.ubb.pem.practicas.exception;

/**
 * Un servicio externo del que depende una función no respondió (hoy: el proveedor del modelo de lenguaje del chatbot,
 * HU-15). Responde 503: la falla es de afuera, el resto del sistema sigue funcionando y se puede reintentar.
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
