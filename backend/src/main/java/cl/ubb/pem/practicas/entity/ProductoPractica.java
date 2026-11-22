package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * HU-18 (migración 019): informe final, diario de reflexión, proyecto de intervención u otro producto (Word o PDF,
 * máximo 10 MB) que el estudiante sube a su inscripción para que quede en su recorrido. El archivo va en disco.
 */
@Entity
@Table(name = "producto_practica")
@Getter
@Setter
@NoArgsConstructor
public class ProductoPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id")
    private Inscripcion inscripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TipoProducto tipo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tipo_mime", nullable = false, length = 150)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subido_por")
    private Usuario subidoPor;

    @Column(name = "fecha_subida", nullable = false)   // cambia al reemplazar el archivo
    private LocalDateTime fechaSubida;

    @PrePersist
    void alCrear() {
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }
}
