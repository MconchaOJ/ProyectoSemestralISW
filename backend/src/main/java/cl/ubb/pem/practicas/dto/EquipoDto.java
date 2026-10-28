package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EquipoPractica;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import java.time.LocalDateTime;
import java.util.List;

/** Una dupla o un grupo completo (HU-10), para la pantalla del coordinador. */
public record EquipoDto(Long id, AsignaturaDto asignatura, String periodo, String establecimiento, String curso,
                        UsuarioResumenDto profesorGuia, List<IntegranteDto> integrantes, LocalDateTime fechaCreacion) {

    public record IntegranteDto(Long inscripcionId, EstudianteResumenDto estudiante, EstadoInscripcion estado) {
    }

    public static EquipoDto desde(EquipoPractica e) {
        List<Inscripcion> integrantes = e.getIntegrantes();
        // El guía es el mismo en todos los integrantes (S-80): se toma del primero
        UsuarioResumenDto guia = integrantes.isEmpty() ? null
                : UsuarioResumenDto.desde(integrantes.get(0).getProfesorGuia());
        return new EquipoDto(e.getId(), AsignaturaDto.desde(e.getAsignatura()), e.getPeriodo(), e.getEstablecimiento(),
                e.getCurso(), guia,
                integrantes.stream().map(i -> new IntegranteDto(i.getId(), EstudianteResumenDto.desde(i.getEstudiante()),
                        i.getEstado())).toList(),
                e.getFechaCreacion());
    }
}
