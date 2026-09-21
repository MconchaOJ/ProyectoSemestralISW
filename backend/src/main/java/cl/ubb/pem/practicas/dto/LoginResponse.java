package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "Token JWT: envíalo en la cabecera Authorization: Bearer <token>") String token,
        String tipo,
        @Schema(description = "Segundos de vigencia del token (8 horas)") long expiraEnSegundos,
        UsuarioDto usuario) {
}
