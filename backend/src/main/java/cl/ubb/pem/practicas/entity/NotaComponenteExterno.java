package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** HU-09 (migración 015): la nota de un componente EXTERNO para una inscripción (una por componente). */
@Entity
@Table(name = "nota_componente_externo")
@Getter
@Setter
@NoArgsConstructor
public class NotaComponenteExterno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "componente_id")
    private ComponenteEvaluativo componente;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal nota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrada_por")
    private Usuario registradaPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
