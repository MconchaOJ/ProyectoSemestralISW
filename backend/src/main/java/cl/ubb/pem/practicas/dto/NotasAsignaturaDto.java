package cl.ubb.pem.practicas.dto;

import java.util.List;

/** HU-09: la estructura de una asignatura y la nota de cada inscripción de un período que ve quien consulta. */
public record NotasAsignaturaDto(EstructuraEvaluativaDto estructura, String periodo, List<NotaFinalDto> inscripciones) {
}
