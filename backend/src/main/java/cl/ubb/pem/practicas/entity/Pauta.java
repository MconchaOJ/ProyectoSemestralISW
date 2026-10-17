package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Instrumento de evaluación: es un dato (dimensiones e indicadores en tablas), no código.
 * HU-06: el coordinador la crea y la edita. Una pauta ya usada en una evaluación no se modifica: se crea otra
 * fila con {@code version + 1} que apunta a esta ({@code versionAnterior}), y las evaluaciones antiguas conservan
 * la suya (indicadores, escala y puntaje máximo).
 */
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

    @Column(nullable = false)
    private Integer version = 1;

    /** Versión que esta reemplaza; nula en la versión 1 (migración 005). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_anterior_id")
    private Pauta versionAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_evaluador", nullable = false, length = 20)
    private TipoEvaluador tipoEvaluador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private AmbitoPauta ambito;

    /**
     * HU-07 (migración 006): 1 o 2, primera o segunda observación, en las pautas de Especialidad y Orientación; nulo en
     * la observación de clase. Va en la pauta porque la primera y la segunda son instrumentos distintos (S-55).
     */
    @Column(name = "numero_observacion")
    private Integer numeroObservacion;

    @Column(name = "escala_min", nullable = false)
    private Integer escalaMin;

    @Column(name = "escala_max", nullable = false)
    private Integer escalaMax;

    @Column(nullable = false)
    private Boolean activa;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Las dimensiones (y sus indicadores) se guardan y se borran junto con la pauta (HU-06)
    @OneToMany(mappedBy = "pauta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<DimensionPauta> dimensiones = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    /** Todos los indicadores de la pauta, en el orden de la pauta impresa. */
    public List<Indicador> indicadores() {
        return dimensiones.stream().flatMap(d -> d.getIndicadores().stream()).toList();
    }
}
