package cl.ubb.pem.practicas.dto;

import jakarta.validation.constraints.NotNull;

/** Abrir (true) o cerrar (false) la validación del chatbot (HU-30). */
public record ValidacionAbiertaRequest(@NotNull Boolean abierta) {
}
