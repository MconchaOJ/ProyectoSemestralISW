package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "asignatura_practica")
@Getter
@Setter
@NoArgsConstructor
public class AsignaturaPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Ciclo ciclo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    @Column(name = "requiere_centro_educativo", nullable = false)
    private Boolean requiereCentroEducativo;

    @Column(name = "chatbot_habilitado", nullable = false)
    private Boolean chatbotHabilitado;

    /** Falso en Aproximación al Sistema Escolar: solo trabajos en clase, sin planificaciones. */
    @Column(name = "requiere_planificacion", nullable = false)
    private Boolean requierePlanificacion = true;

    /** TRUE si la práctica tiene tutor de la universidad: solo Práctica Profesional (HU-07, migración 006). */
    @Column(name = "lleva_tutor", nullable = false)
    private Boolean llevaTutor = false;
}
