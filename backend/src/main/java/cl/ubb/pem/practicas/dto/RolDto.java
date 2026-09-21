package cl.ubb.pem.practicas.dto;

import cl.ubb.pem.practicas.entity.Rol;

public record RolDto(Integer id, String nombre, String descripcion) {

    public static RolDto desde(Rol r) {
        return new RolDto(r.getId(), r.getNombre(), r.getDescripcion());
    }
}
