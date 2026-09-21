package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "indicador")
@Getter
@Setter
@NoArgsConstructor
public class Indicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dimension_id")
    private DimensionPauta dimension;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;
}
