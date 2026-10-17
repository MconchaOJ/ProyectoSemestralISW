package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InscripcionDto(Long id, EstudianteResumenDto estudiante, AsignaturaDto asignatura, String periodo,
                             UsuarioResumenDto profesorGuia, UsuarioResumenDto profesorCurso, UsuarioResumenDto tutor,
                             EstadoInscripcion estado, BigDecimal notaFinal, LocalDate fechaCierre) {

    public static InscripcionDto desde(Inscripcion i) {
        return new InscripcionDto(i.getId(), EstudianteResumenDto.desde(i.getEstudiante()),
                AsignaturaDto.desde(i.getAsignatura()), i.getPeriodo(),
                UsuarioResumenDto.desde(i.getProfesorGuia()), UsuarioResumenDto.desde(i.getProfesorCurso()),
                UsuarioResumenDto.desde(i.getTutor()),
                i.getEstado(), i.getNotaFinal(), i.getFechaCierre());
    }
}
