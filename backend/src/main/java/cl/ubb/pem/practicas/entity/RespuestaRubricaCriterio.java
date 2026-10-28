package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un criterio (1 a 9) de una rúbrica de juicio experto: puntaje de 1 a 4 y comentario opcional (HU-30). */
@Entity
@Table(name = "respuesta_rubrica_criterio")
@Getter
@Setter
@NoArgsConstructor
public class RespuestaRubricaCriterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "respuesta_id")
    private RespuestaRubricaExperto respuesta;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private Integer puntaje;

    @Column(length = 1000)
    private String comentario;
}
