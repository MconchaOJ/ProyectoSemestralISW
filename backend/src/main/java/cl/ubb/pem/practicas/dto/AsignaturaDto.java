package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.Ciclo;
import cl.ubb.pem.practicas.entity.Modalidad;

public record AsignaturaDto(Integer id, String codigo, String nombre, Integer orden, Ciclo ciclo, Modalidad modalidad,
                            boolean requiereCentroEducativo, boolean chatbotHabilitado,
                            boolean requierePlanificacion) {

    public static AsignaturaDto desde(AsignaturaPractica a) {
        return new AsignaturaDto(a.getId(), a.getCodigo(), a.getNombre(), a.getOrden(), a.getCiclo(), a.getModalidad(),
                Boolean.TRUE.equals(a.getRequiereCentroEducativo()), Boolean.TRUE.equals(a.getChatbotHabilitado()),
                Boolean.TRUE.equals(a.getRequierePlanificacion()));
    }
}
