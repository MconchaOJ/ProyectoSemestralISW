package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    /** Tutor de la universidad (HU-07): solo en Práctica Profesional; nulo = sin tutor. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Usuario tutor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado = EstadoInscripcion.EN_CURSO;

    @Column(name = "nota_final", precision = 3, scale = 1)
    private BigDecimal notaFinal;

    /** Día en que se cerró (APROBADA o REPROBADA); nulo mientras está EN_CURSO. La cartola (HU-31) filtra por ella. */
    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    /**
     * Dupla o grupo (HU-10, migración 013); nulo = sin grupo. En la base la clave foránea es compuesta con la
     * asignatura y el periodo, así que el grupo siempre es de la misma asignatura y periodo que esta inscripción.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private GrupoPractica grupo;

    /** Los otros integrantes de su dupla o grupo, por apellido (vacío si no tiene grupo). */
    public java.util.List<Inscripcion> companeros() {
        return grupo == null ? java.util.List.of()
                : grupo.integrantesOrdenados().stream().filter(i -> !i.getId().equals(id)).toList();
    }
}
