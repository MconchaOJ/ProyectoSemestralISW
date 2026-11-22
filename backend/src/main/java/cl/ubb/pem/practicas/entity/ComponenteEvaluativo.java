package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * HU-09 (migración 015): un componente de la estructura evaluativa de una asignatura, con su peso en la nota final.
 * Los de una asignatura suman 100 % (lo comprueba {@code EstructuraEvaluativaService}).
 */
@Entity
@Table(name = "componente_evaluativo")
@Getter
@Setter
@NoArgsConstructor
public class ComponenteEvaluativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id")
    private AsignaturaPractica asignatura;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TipoComponente tipo;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Solo en los de tipo PAUTA: la versión vigente (se mueve sola cuando la pauta tiene una versión nueva). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pauta_id")
    private Pauta pauta;

    @Column(nullable = false)
    private Integer porcentaje;

    @Column(nullable = false)
    private Integer orden;
}
