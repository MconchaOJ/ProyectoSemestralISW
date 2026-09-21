package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Instrumento de evaluación: es un dato (dimensiones e indicadores en tablas), no código. */
@Entity
@Table(name = "pauta")
@Getter
@Setter
@NoArgsConstructor
public class Pauta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id")
    private AsignaturaPractica asignatura;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_evaluador", nullable = false, length = 20)
    private TipoEvaluador tipoEvaluador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private AmbitoPauta ambito;

    @Column(name = "escala_min", nullable = false)
    private Integer escalaMin;

    @Column(name = "escala_max", nullable = false)
    private Integer escalaMax;

    @Column(nullable = false)
    private Boolean activa;

    @OneToMany(mappedBy = "pauta")
    @OrderBy("orden ASC")
    private List<DimensionPauta> dimensiones = new ArrayList<>();

    /** Todos los indicadores de la pauta, en el orden de la pauta impresa. */
    public List<Indicador> indicadores() {
        return dimensiones.stream().flatMap(d -> d.getIndicadores().stream()).toList();
    }
}
