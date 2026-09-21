package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una clase evaluada con una pauta. Al finalizar guarda su puntaje total y su promedio. */
@Entity
@Table(name = "evaluacion")
@Getter
@Setter
@NoArgsConstructor
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id")
    private Pauta pauta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_id")
    private Usuario evaluador;

    /** Opcional: planificación de la clase observada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planificacion_id")
    private Planificacion planificacion;

    @Column(length = 150)
    private String establecimiento;

    @Column(length = 50)
    private String curso;

    @Column(name = "fecha_clase", nullable = false)
    private LocalDate fechaClase;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_termino")
    private LocalTime horaTermino;

    @Column(name = "objetivo_clase", columnDefinition = "text")
    private String objetivoClase;

    @Column(name = "observaciones_generales", columnDefinition = "text")
    private String observacionesGenerales;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    private EstadoEvaluacion estado = EstadoEvaluacion.BORRADOR;

    @Column(name = "puntaje_total")
    private Integer puntajeTotal;

    @Column(precision = 4, scale = 2)
    private BigDecimal promedio;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL)
    private List<RespuestaIndicador> respuestas = new ArrayList<>();

    public boolean estaFinalizada() {
        return estado == EstadoEvaluacion.FINALIZADA;
    }

    /** Congela el resultado: después de esto la base impide modificar los puntajes (trigger). */
    public void finalizar(int total, BigDecimal promedioCalculado, LocalDateTime cuando) {
        this.estado = EstadoEvaluacion.FINALIZADA;
        this.puntajeTotal = total;
        this.promedio = promedioCalculado;
        this.fechaFinalizacion = cuando;
    }
}
