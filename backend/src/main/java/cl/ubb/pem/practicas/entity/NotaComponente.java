package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** La nota (1,0 a 7,0) de un componente de la estructura evaluativa en una inscripción (HU-09, migración 012). */
@Entity
@Table(name = "nota_componente")
@Getter
@Setter
@NoArgsConstructor
public class NotaComponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "componente_id")
    private ComponenteNota componente;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal nota;

    /** El profesor del curso que la registró (o la corrigió por última vez). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrada_por")
    private Usuario registradaPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
