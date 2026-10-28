package cl.ubb.pem.practicas.dto;

import jakarta.validation.constraints.NotNull;

/** Marcar (true) o desmarcar (false) a un estudiante como validador del chatbot (HU-30). */
public record ValidadorRequest(@NotNull Boolean validador) {
}
