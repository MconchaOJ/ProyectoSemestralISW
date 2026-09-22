package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audio que graba el profesor evaluador al terminar la clase, con su transcripción (HU-11, HU-12).
 * Como máximo una por evaluación (UNIQUE en la base). Es retroalimentación complementaria: se puede grabar tanto
 * en BORRADOR como en FINALIZADA (SUPUESTO S-42: no cambia el puntaje ni rompe la inmutabilidad de la evaluación).
 */
@Entity
@Table(name = "nota_voz")
@Getter
@Setter
@NoArgsConstructor
public class NotaVoz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id")
    private Evaluacion evaluacion;

    @Column(name = "archivo_ruta", nullable = false, length = 500)
    private String archivoRuta;

    @Column(name = "duracion_segundos", nullable = false)
    private Integer duracionSegundos;

    @Column(columnDefinition = "text")
    private String transcripcion;

    @Column(name = "transcripcion_corregida", nullable = false)
    private Boolean transcripcionCorregida = false;

    @Column(name = "fecha_grabacion", nullable = false, updatable = false)
    private LocalDateTime fechaGrabacion;

    @PrePersist
    void alCrear() {
        fechaGrabacion = LocalDateTime.now();
    }
}
