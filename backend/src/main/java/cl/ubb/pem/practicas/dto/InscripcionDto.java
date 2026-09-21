package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import java.math.BigDecimal;

public record InscripcionDto(Long id, EstudianteResumenDto estudiante, AsignaturaDto asignatura, String periodo,
                             UsuarioResumenDto profesorGuia, UsuarioResumenDto profesorCurso,
                             EstadoInscripcion estado, BigDecimal notaFinal) {

    public static InscripcionDto desde(Inscripcion i) {
        return new InscripcionDto(i.getId(), EstudianteResumenDto.desde(i.getEstudiante()),
                AsignaturaDto.desde(i.getAsignatura()), i.getPeriodo(),
                UsuarioResumenDto.desde(i.getProfesorGuia()), UsuarioResumenDto.desde(i.getProfesorCurso()),
                i.getEstado(), i.getNotaFinal());
    }
}
