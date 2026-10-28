package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una lectura de la conversación con el chatbot hecha por el jefe de carrera (HU-20, criterio 4; migración 010).
 * Se guarda una por cada vez que abre el chat; el estudiante ve la última (criterio 3). SUPUESTO S-66.
 */
@Entity
@Table(name = "lectura_chat_jefe_carrera")
@Getter
@Setter
@NoArgsConstructor
public class LecturaChatJefeCarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    /** El jefe de carrera que leyó. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
