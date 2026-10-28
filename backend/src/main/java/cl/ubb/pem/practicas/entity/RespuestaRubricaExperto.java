package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * La rúbrica de juicio experto que respondió un validador (HU-30, migración 011): los 9 criterios de la propuesta
 * RetroPlani Mate V3 (en {@link RespuestaRubricaCriterio}) y sus 4 preguntas abiertas. Una sola vez por validador.
 */
@Entity
@Table(name = "respuesta_rubrica_experto")
@Getter
@Setter
@NoArgsConstructor
public class RespuestaRubricaExperto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validador_id")
    private Estudiante validador;

    @Column(columnDefinition = "text") private String fortalezas;
    @Column(columnDefinition = "text") private String debilidades;
    @Column(columnDefinition = "text") private String mejoras;
    @Column(columnDefinition = "text") private String recomendacion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @OneToMany(mappedBy = "respuesta", cascade = CascadeType.ALL)
    @OrderBy("numero")
    private List<RespuestaRubricaCriterio> criterios = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
