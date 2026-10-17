package cl.ubb.pem.practicas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** HU-07: asignar, cambiar o quitar (null) el tutor de una inscripción de Práctica Profesional. */
public record InscripcionTutorRequest(
        @Schema(description = "Id del usuario con rol TUTOR; vacío (null) quita el tutor", example = "33") Long tutorId) {
}
