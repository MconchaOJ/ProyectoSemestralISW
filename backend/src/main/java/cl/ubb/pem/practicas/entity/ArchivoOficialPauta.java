package cl.ubb.pem.practicas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HU-25 (migración 020): archivo oficial (PDF o Word firmado y timbrado) de una pauta. Cada subida es una fila nueva
 * con el número siguiente; la de número mayor es la vigente y las anteriores son el historial (quién y cuándo).
 * Va con una versión concreta de la pauta (SUPUESTO S-96). El archivo se guarda en disco.
 */
@Entity
@Table(name = "archivo_oficial_pauta")
@Getter
@Setter
@NoArgsConstructor
public class ArchivoOficialPauta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id")
    private Pauta pauta;

    @Column(nullable = false)
    private Integer version;

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

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    void alCrear() {
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }
}
