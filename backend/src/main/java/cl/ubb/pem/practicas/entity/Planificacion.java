package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Plan escrito de una clase, con los 5 elementos del formato de la carrera (S-09). */
@Entity
@Table(name = "planificacion")
@Getter
@Setter
@NoArgsConstructor
public class Planificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "fecha_clase")
    private LocalDate fechaClase;

    @Column(columnDefinition = "text")
    private String objetivo;

    @Column(columnDefinition = "text")
    private String recursos;

    @Column(columnDefinition = "text")
    private String tiempos;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(name = "evaluacion_aprendizaje", columnDefinition = "text")
    private String evaluacionAprendizaje;

    @Column(name = "interaccion_participantes", columnDefinition = "text")
    private String interaccionParticipantes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPlanificacion estado = EstadoPlanificacion.BORRADOR;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "planificacion", cascade = CascadeType.ALL)
    @OrderBy("fecha ASC, id ASC")
    private List<RevisionPlanificacion> revisiones = new ArrayList<>();

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    /** El estudiante solo puede editarla en BORRADOR o CON_OBSERVACIONES (HU-04, criterio 5). */
    public boolean esEditable() {
        return estado == EstadoPlanificacion.BORRADOR || estado == EstadoPlanificacion.CON_OBSERVACIONES;
    }

    /** Elementos del formato que aún están vacíos (todos son obligatorios para enviarla a revisión). */
    public List<String> elementosFaltantes() {
        List<String> faltan = new ArrayList<>();
        if (fechaClase == null) faltan.add("fechaClase");
        if (objetivo == null) faltan.add("objetivo");
        if (recursos == null) faltan.add("recursos");
        if (tiempos == null) faltan.add("tiempos");
        if (duracionMinutos == null) faltan.add("duracionMinutos");
        if (evaluacionAprendizaje == null) faltan.add("evaluacionAprendizaje");
        if (interaccionParticipantes == null) faltan.add("interaccionParticipantes");
        return faltan;
    }

    public RevisionPlanificacion agregarRevision(Usuario revisor, EtapaRevision etapa,
                                                 DecisionRevision decision, String comentario) {
        RevisionPlanificacion r = new RevisionPlanificacion();
        r.setPlanificacion(this);
        r.setRevisor(revisor);
        r.setEtapa(etapa);
        r.setDecision(decision);
        r.setComentario(comentario);
        r.setFecha(LocalDateTime.now());
        revisiones.add(r);
        return r;
    }
}
