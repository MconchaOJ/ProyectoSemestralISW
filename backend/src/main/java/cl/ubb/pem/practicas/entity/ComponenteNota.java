package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un componente de la estructura evaluativa de una asignatura (HU-09, migración 012), por ejemplo "TANEP" con un
 * 20 %. Los de una asignatura suman 100 (lo valida el servicio). Los define el coordinador.
 */
@Entity
@Table(name = "componente_nota")
@Getter
@Setter
@NoArgsConstructor
public class ComponenteNota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id")
    private AsignaturaPractica asignatura;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Porcentaje de la nota final, mayor que 0 y hasta 100. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(nullable = false)
    private Integer orden;
}
