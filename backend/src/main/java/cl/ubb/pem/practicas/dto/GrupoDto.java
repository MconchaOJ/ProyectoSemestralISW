package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.GrupoPractica;
import cl.ubb.pem.practicas.entity.Modalidad;
import java.time.LocalDateTime;
import java.util.List;

/** HU-10: una dupla o grupo con sus integrantes (por apellido). */
public record GrupoDto(Long id, Integer asignaturaId, String asignatura, Modalidad modalidad, String periodo,
                       String establecimiento, String curso, UsuarioResumenDto profesorGuia,
                       List<Integrante> integrantes, LocalDateTime fechaCreacion) {

    /** Un integrante: su inscripción y el estudiante. */
    public record Integrante(Long inscripcionId, EstudianteResumenDto estudiante) {
    }

    public static GrupoDto desde(GrupoPractica g) {
        var integrantes = g.integrantesOrdenados();
        // todos comparten el mismo guía (S-78): basta con el del primero
        var guia = integrantes.isEmpty() ? null : integrantes.get(0).getProfesorGuia();
        return new GrupoDto(g.getId(), g.getAsignatura().getId(), g.getAsignatura().getNombre(),
                g.getAsignatura().getModalidad(), g.getPeriodo(), g.getEstablecimiento(), g.getCurso(),
                UsuarioResumenDto.desde(guia),
                integrantes.stream().map(i -> new Integrante(i.getId(), EstudianteResumenDto.desde(i.getEstudiante()))).toList(),
                g.getFechaCreacion());
    }
}
