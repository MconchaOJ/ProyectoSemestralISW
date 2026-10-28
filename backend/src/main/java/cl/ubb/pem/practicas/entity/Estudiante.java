package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estudiante")
@Getter
@Setter
@NoArgsConstructor
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 10)
    private String rut;

    @Column(name = "semestre_actual", nullable = false)
    private Integer semestreActual;

    @Column(name = "anio_ingreso", nullable = false)
    private Integer anioIngreso;

    /** HU-30: participa en la validación del chatbot (migración 009). */
    @Column(name = "validador_chatbot", nullable = false)
    private Boolean validadorChatbot = false;
}
