package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El rol es un catálogo de 5 filas y se necesita en cada petición: se carga siempre.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    /**
     * Segundo rol opcional, solo para cuentas especiales (2026-10-11, confirmado por el cliente: el jefe de carrera
     * también es profesor guía). A propósito no es una lista: como máximo dos roles. Nulo = cuenta normal.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_secundario_id")
    private Rol rolSecundario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(nullable = false, length = 150)
    private String email;

    /** Hash BCrypt. Nunca se expone en un DTO. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    /** Nombres de sus roles: el principal primero y, si tiene, el segundo. */
    public List<String> getNombresRoles() {
        return rolSecundario == null ? List.of(rol.getNombre()) : List.of(rol.getNombre(), rolSecundario.getNombre());
    }

    public boolean tieneRol(String nombreRol) {
        return getNombresRoles().contains(nombreRol);
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}
