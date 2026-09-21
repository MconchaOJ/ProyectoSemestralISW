package cl.ubb.pem.practicas.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/** Cuerpo JSON de todas las respuestas de error (en español). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(LocalDateTime timestamp, int status, String error, String mensaje, String ruta,
                       List<CampoError> errores) {

    public record CampoError(String campo, String mensaje) {
    }

    public static ApiError de(int status, String error, String mensaje, String ruta) {
        return new ApiError(LocalDateTime.now(), status, error, mensaje, ruta, null);
    }

    public static ApiError de(int status, String error, String mensaje, String ruta, List<CampoError> errores) {
        return new ApiError(LocalDateTime.now(), status, error, mensaje, ruta, errores);
    }
}
