package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.ComponenteNota;
import java.math.BigDecimal;

public record ComponenteNotaDto(Integer id, String nombre, BigDecimal peso, Integer orden) {

    public static ComponenteNotaDto desde(ComponenteNota c) {
        return new ComponenteNotaDto(c.getId(), c.getNombre(), c.getPeso(), c.getOrden());
    }
}
