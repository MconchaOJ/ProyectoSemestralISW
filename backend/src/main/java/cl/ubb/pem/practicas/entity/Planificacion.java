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

    /** "Asignatura" del formato oficial: siempre Matemática en esta carrera (S-45; texto fijo, no se guarda). */
    public static final String ASIGNATURA_ESCOLAR = "Matemática";

    // --- Formato oficial de la carrera (S-45, migración 012). El nombre del profesor/a (el estudiante), el año y el
    // período salen de la inscripción, y "Asignatura: Matemática" es un texto fijo: no se guardan aquí.
    @Column(length = 150)
    private String establecimiento;

    @Column(length = 50)
    private String curso;

    @Column(length = 200)
    private String unidad;

    /** Objetivo de aprendizaje / indicadores de evaluación. */
    @Column(name = "objetivo_aprendizaje", columnDefinition = "text")
    private String objetivoAprendizaje;

    @Column(name = "objetivo_actitudinal", columnDefinition = "text")
    private String objetivoActitudinal;

    @Column(name = "objetivo_clase", columnDefinition = "text")
    private String objetivoClase;

    @Column(name = "fecha_clase")
    private LocalDate fechaClase;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(columnDefinition = "text")
    private String recursos;

    @Column(name = "instrumentos_evaluacion", columnDefinition = "text")
    private String instrumentosEvaluacion;

    // Descripción de la clase en tres partes, cada una con su relato y sus minutos (suman la duración, S-76)
    @Column(columnDefinition = "text")
    private String inicio;

    @Column(name = "inicio_minutos")
    private Integer inicioMinutos;

    @Column(columnDefinition = "text")
    private String desarrollo;

    @Column(name = "desarrollo_minutos")
    private Integer desarrolloMinutos;

    @Column(columnDefinition = "text")
    private String cierre;

    @Column(name = "cierre_minutos")
    private Integer cierreMinutos;

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

    /** Campos del formato que aún están vacíos (todos son obligatorios para enviarla a revisión), en su orden. */
    public List<String> elementosFaltantes() {
        List<String> faltan = new ArrayList<>();
        if (establecimiento == null) faltan.add("establecimiento");
        if (curso == null) faltan.add("curso");
        if (unidad == null) faltan.add("unidad");
        if (objetivoAprendizaje == null) faltan.add("objetivoAprendizaje");
        if (objetivoActitudinal == null) faltan.add("objetivoActitudinal");
        if (objetivoClase == null) faltan.add("objetivoClase");
        if (fechaClase == null) faltan.add("fechaClase");
        if (duracionMinutos == null) faltan.add("duracionMinutos");
        if (recursos == null) faltan.add("recursos");
        if (instrumentosEvaluacion == null) faltan.add("instrumentosEvaluacion");
        if (inicio == null) faltan.add("inicio");
        if (inicioMinutos == null) faltan.add("inicioMinutos");
        if (desarrollo == null) faltan.add("desarrollo");
        if (desarrolloMinutos == null) faltan.add("desarrolloMinutos");
        if (cierre == null) faltan.add("cierre");
        if (cierreMinutos == null) faltan.add("cierreMinutos");
        return faltan;
    }

    /**
     * S-76: con los cuatro valores completos, inicio + desarrollo + cierre debe ser igual a la duración de la clase.
     * Devuelve la suma cuando no calza, o nulo si calza o si aún falta alguno (un borrador puede estar a medias).
     */
    public Integer sumaDePartesQueNoCalza() {
        return sumaDePartesQueNoCalza(duracionMinutos, inicioMinutos, desarrolloMinutos, cierreMinutos);
    }

    /** La misma regla con valores sueltos, para validar un pedido antes de tocar la entidad. */
    public static Integer sumaDePartesQueNoCalza(Integer duracion, Integer inicio, Integer desarrollo, Integer cierre) {
        if (inicio == null || desarrollo == null || cierre == null || duracion == null) {
            return null;
        }
        int suma = inicio + desarrollo + cierre;
        return suma == duracion ? null : suma;
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
