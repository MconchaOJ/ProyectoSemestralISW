package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dupla o grupo de práctica (HU-10, migración 013): junta inscripciones de la misma asignatura y periodo. Cada
 * integrante conserva su inscripción, su pauta (S-11) y sus planificaciones (S-77); el grupo solo agrupa y, en las
 * prácticas que van a un colegio, guarda el establecimiento y el curso que comparten.
 */
@Entity
@Table(name = "grupo_practica")
@Getter
@Setter
@NoArgsConstructor
public class GrupoPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id")
    private AsignaturaPractica asignatura;

    @Column(nullable = false, length = 6)
    private String periodo;

    @Column(length = 150)
    private String establecimiento;

    @Column(length = 50)
    private String curso;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Integrantes (solo lectura desde aquí: el dueño de la relación es {@code Inscripcion.grupo}). */
    @OneToMany(mappedBy = "grupo")
    private List<Inscripcion> integrantes = new ArrayList<>();

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }

    /** Los integrantes ordenados por apellido y nombre. */
    public List<Inscripcion> integrantesOrdenados() {
        return integrantes.stream()
                .sorted(Comparator.comparing((Inscripcion i) -> i.getEstudiante().getUsuario().getApellidos())
                        .thenComparing(i -> i.getEstudiante().getUsuario().getNombre()))
                .toList();
    }
}
