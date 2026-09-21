package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.Estudiante;

/** Ficha del estudiante. Incluye el semestre actual, que será parte del contexto del chatbot (S-14). */
public record EstudianteDto(Long id, Long usuarioId, String nombre, String apellidos, String email, String rut,
                            Integer semestreActual, Integer anioIngreso, boolean activo) {

    public static EstudianteDto desde(Estudiante e) {
        var u = e.getUsuario();
        return new EstudianteDto(e.getId(), u.getId(), u.getNombre(), u.getApellidos(), u.getEmail(), e.getRut(),
                e.getSemestreActual(), e.getAnioIngreso(), Boolean.TRUE.equals(u.getActivo()));
    }
}
