package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Regla operativa que edita el coordinador (HU-27, migración 014), con su rango permitido (S-80). */
@Entity
@Table(name = "parametro_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ParametroSistema {

    @Id
    @Column(length = 50)
    private String clave;

    @Column(nullable = false)
    private Integer valor;

    @Column(nullable = false)
    private Integer minimo;

    @Column(nullable = false)
    private Integer maximo;

    @Column(nullable = false, length = 20)
    private String unidad;

    @Column(nullable = false, length = 300)
    private String descripcion;

    /** Coordinador que lo cambió por última vez; nulo = valor inicial. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editado_por")
    private Usuario editadoPor;

    @Column(name = "fecha_edicion")
    private LocalDateTime fechaEdicion;
}
