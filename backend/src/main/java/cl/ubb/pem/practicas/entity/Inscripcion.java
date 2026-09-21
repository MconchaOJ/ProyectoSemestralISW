package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Un estudiante cursando una asignatura en un periodo: el centro del modelo. */
@Entity
@Table(name = "inscripcion")
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id")
    private AsignaturaPractica asignatura;

    @Column(nullable = false, length = 6)
    private String periodo;

    /** Nulo en el ciclo inicial (no va al colegio). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_guia_id")
    private Usuario profesorGuia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesor_curso_id")
    private Usuario profesorCurso;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado = EstadoInscripcion.EN_CURSO;

    @Column(name = "nota_final", precision = 3, scale = 1)
    private BigDecimal notaFinal;
}
