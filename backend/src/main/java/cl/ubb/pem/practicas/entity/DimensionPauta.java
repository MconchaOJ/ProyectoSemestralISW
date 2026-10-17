package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dimension_pauta")
@Getter
@Setter
@NoArgsConstructor
public class DimensionPauta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id")
    private Pauta pauta;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private Integer orden;

    @OneToMany(mappedBy = "dimension", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<Indicador> indicadores = new ArrayList<>();
}
