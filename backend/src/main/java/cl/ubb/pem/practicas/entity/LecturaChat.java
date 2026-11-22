package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una lectura de la conversación con el chatbot hecha por quien la lee sin participar en la inscripción: el jefe de
 * carrera (HU-20, migración 010) o, desde el 2026-11-22, el coordinador (migración 018). Se guarda una por cada vez que
 * abre el chat, con el rol con que la leyó; el estudiante ve la última de cada uno (criterio 3). SUPUESTO S-66.
 */
@Entity
@Table(name = "lectura_chat")
@Getter
@Setter
@NoArgsConstructor
public class LecturaChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    /** El jefe de carrera o el coordinador que leyó. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** JEFE_CARRERA o COORDINADOR: el rol con el que leyó. */
    @Column(nullable = false, length = 20)
    private String rol;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
