package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo de roles (tabla rol): ESTUDIANTE, PROFESOR_GUIA, PROFESOR_CURSO, COORDINADOR. */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}
