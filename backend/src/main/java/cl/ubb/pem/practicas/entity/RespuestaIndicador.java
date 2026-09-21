package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "respuesta_indicador")
@Getter
@Setter
@NoArgsConstructor
public class RespuestaIndicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id")
    private Evaluacion evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "indicador_id")
    private Indicador indicador;

    @Column(nullable = false)
    private Integer puntaje;

    @Column(length = 500)
    private String comentario;
}
