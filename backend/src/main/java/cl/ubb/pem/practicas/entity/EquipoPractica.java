package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una dupla o un grupo de estudiantes de una misma asignatura y periodo (HU-10, migración 013). Comparte colegio y
 * curso (aquí) y profesor guía (en cada inscripción; el servicio exige que sea el mismo, S-80). Cada integrante
 * sigue con su propia inscripción y sus propias evaluaciones (criterio 2).
 */
@Entity
@Table(name = "equipo_practica")
@Getter
@Setter
@NoArgsConstructor
public class EquipoPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id")
    private AsignaturaPractica asignatura;

    @Column(nullable = false, length = 6)
    private String periodo;

    /** Nulo si la asignatura no va a un colegio (ciclo inicial). */
    @Column(length = 150)
    private String establecimiento;

    @Column(length = 50)
    private String curso;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "equipo")
    @OrderBy("id ASC")
    private List<Inscripcion> integrantes = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
