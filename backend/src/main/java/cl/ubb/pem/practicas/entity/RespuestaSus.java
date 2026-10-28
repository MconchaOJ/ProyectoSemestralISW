package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

/**
 * La Escala SUS adaptada al chatbot que respondió un validador (HU-30, migración 011): 10 ítems de 1 a 5, una sola
 * vez por validador. El puntaje SUS (0 a 100) lo calcula la base (columna generada), no la aplicación.
 */
@Entity
@Table(name = "respuesta_sus")
@Getter
@Setter
@NoArgsConstructor
public class RespuestaSus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validador_id")
    private Estudiante validador;

    @Column(nullable = false) private Integer item1;
    @Column(nullable = false) private Integer item2;
    @Column(nullable = false) private Integer item3;
    @Column(nullable = false) private Integer item4;
    @Column(nullable = false) private Integer item5;
    @Column(nullable = false) private Integer item6;
    @Column(nullable = false) private Integer item7;
    @Column(nullable = false) private Integer item8;
    @Column(nullable = false) private Integer item9;
    @Column(nullable = false) private Integer item10;

    /** Columna generada por la base (fórmula de Brooke): Hibernate la relee justo después de insertar. */
    @Generated
    @Column(insertable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal puntaje;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    public List<Integer> getItems() {
        return List.of(item1, item2, item3, item4, item5, item6, item7, item8, item9, item10);
    }

    /** Los 10 ítems en orden (el servicio ya validó que sean 10, de 1 a 5). */
    public void setItems(List<Integer> items) {
        item1 = items.get(0);
        item2 = items.get(1);
        item3 = items.get(2);
        item4 = items.get(3);
        item5 = items.get(4);
        item6 = items.get(5);
        item7 = items.get(6);
        item8 = items.get(7);
        item9 = items.get(8);
        item10 = items.get(9);
    }

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
