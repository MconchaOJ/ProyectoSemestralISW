package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.Estudiante;

/** Referencia corta a un estudiante dentro de otra respuesta (inscripción, planificación...). */
public record EstudianteResumenDto(Long id, String nombreCompleto, String rut) {

    public static EstudianteResumenDto desde(Estudiante e) {
        return new EstudianteResumenDto(e.getId(), e.getUsuario().getNombreCompleto(), e.getRut());
    }
}
