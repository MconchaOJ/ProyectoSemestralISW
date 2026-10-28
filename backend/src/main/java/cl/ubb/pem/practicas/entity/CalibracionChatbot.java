package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * La configuración del chatbot, UNA sola fila (id 1) para todo el sistema: las instrucciones que calibra el
 * coordinador (HU-17, migración 008: tono, expresiones a usar y a evitar, reglas del flujo y respuestas modelo) y si la
 * validación con estudiantes está abierta (HU-30, migración 009). El contexto de cada estudiante no se guarda aquí:
 * lo agrega el sistema en cada mensaje.
 */
@Entity
@Table(name = "instrucciones_chatbot")
@Getter
@Setter
@NoArgsConstructor
public class CalibracionChatbot {

    /** La única fila de la tabla (CHECK id = 1). */
    public static final Long ID_UNICO = 1L;

    @Id
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String texto;

    /** Quién lo editó por última vez; nulo = el texto base cargado por la instalación, nunca editado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editado_por_id")
    private Usuario editadoPor;

    @Column(name = "fecha_edicion")
    private LocalDateTime fechaEdicion;

    /** HU-30 (migración 009): mientras está abierta, el chatbot solo responde a los estudiantes validadores. */
    @Column(name = "validacion_abierta", nullable = false)
    private Boolean validacionAbierta = false;
}
