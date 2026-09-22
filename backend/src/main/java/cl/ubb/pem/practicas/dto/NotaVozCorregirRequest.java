package cl.ubb.pem.practicas.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de PUT /api/evaluaciones/{id}/nota-voz: el evaluador corrige el texto transcrito. */
public record NotaVozCorregirRequest(@NotBlank(message = "La transcripción corregida no puede estar vacía.") String transcripcion) {
}
