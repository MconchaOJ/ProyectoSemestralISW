package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.EquipoPractica;
import cl.ubb.pem.practicas.entity.Inscripcion;
import java.util.List;
import java.util.Objects;

/**
 * La dupla o el grupo de una inscripción visto desde uno de sus integrantes (HU-10): colegio y curso compartidos y
 * con quién trabaja (sin la propia inscripción). Va en la inscripción y en el historial (criterio 3).
 */
public record EquipoResumenDto(Long id, String establecimiento, String curso, List<CompaneroDto> companeros) {

    public record CompaneroDto(Long inscripcionId, Long estudianteId, String nombreCompleto) {
    }

    /** Nulo si la inscripción no está en un equipo. */
    public static EquipoResumenDto de(Inscripcion propia) {
        EquipoPractica e = propia.getEquipo();
        if (e == null) {
            return null;
        }
        List<CompaneroDto> companeros = e.getIntegrantes().stream()
                .filter(i -> !Objects.equals(i.getId(), propia.getId()))
                .map(i -> new CompaneroDto(i.getId(), i.getEstudiante().getId(),
                        i.getEstudiante().getUsuario().getNombreCompleto()))
                .toList();
        return new EquipoResumenDto(e.getId(), e.getEstablecimiento(), e.getCurso(), companeros);
    }
}
