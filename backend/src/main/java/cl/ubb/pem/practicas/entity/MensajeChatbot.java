package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un mensaje de la conversación entre el estudiante y el chatbot (HU-15), guardado por inscripción (criterio 9).
 * El profesor guía de la inscripción lo lee (HU-16). Se guardan siempre de a dos (el del estudiante y la respuesta):
 * si el proveedor del modelo falla, no se guarda ninguno.
 */
@Entity
@Table(name = "mensaje_chatbot")
@Getter
@Setter
@NoArgsConstructor
public class MensajeChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    /** Planificación que el estudiante adjuntó a este mensaje (migración 007); nula si no adjuntó ninguna. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planificacion_id")
    private Planificacion planificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AutorMensaje autor;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
