package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Cada decisión del guía o del profesor del curso. Nada se sobrescribe: queda el historial completo. */
@Entity
@Table(name = "revision_planificacion")
@Getter
@Setter
@NoArgsConstructor
public class RevisionPlanificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planificacion_id")
    private Planificacion planificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revisor_id")
    private Usuario revisor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private EtapaRevision etapa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private DecisionRevision decision;

    @Column(columnDefinition = "text")
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
