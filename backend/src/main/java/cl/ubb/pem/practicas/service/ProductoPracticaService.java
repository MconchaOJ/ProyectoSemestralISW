package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.ProductoDto;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.ProductoPractica;
import cl.ubb.pem.practicas.entity.TipoProducto;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.ProductoPracticaRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * HU-18: informes y productos de la práctica (informe final, diario de reflexión, proyecto de intervención u otro).
 * Reutiliza el guardado de archivos de HU-21 ({@link AlmacenamientoArchivosService}).
 * <ul>
 *   <li>SUPUESTO S-93: solo el estudiante dueño de la inscripción sube, reemplaza o borra (403 para cualquier otro),
 *       y solo con la inscripción EN_CURSO (409 si ya se cerró: lo cerrado es el recorrido que quedó).</li>
 *   <li>SUPUESTO S-94: varios archivos por tipo y todos los tipos en las 6 prácticas.</li>
 *   <li>Listar y descargar: quien ya puede ver la inscripción (guía, profesor del curso, tutor, coordinador y jefe
 *       de carrera), igual que los documentos de la planificación.</li>
 * </ul>
 * El archivo se borra del disco solo después del commit (si la transacción falla, se borra el nuevo).
 */
@Service
public class ProductoPracticaService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".docx", ".pdf");
    private static final String CARPETA = "productos";

    private final InscripcionRepository inscripciones;
    private final ProductoPracticaRepository productos;
    private final UsuarioRepository usuarios;
    private final AccesoService acceso;
    private final AlmacenamientoArchivosService almacenamiento;

    public ProductoPracticaService(InscripcionRepository inscripciones, ProductoPracticaRepository productos,
                                   UsuarioRepository usuarios, AccesoService acceso,
                                   AlmacenamientoArchivosService almacenamiento) {
        this.inscripciones = inscripciones;
        this.productos = productos;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public ProductoDto subir(Long inscripcionId, TipoProducto tipo, MultipartFile archivo, UsuarioAutenticado actor) {
        Inscripcion i = buscar(inscripcionId);
        exigirDuenoEnCurso(i, actor, "subir");
        String ruta = almacenamiento.guardarDocumento(CARPETA, archivo, EXTENSIONES_PERMITIDAS);
        ProductoPractica p = new ProductoPractica();
        p.setInscripcion(i);
        p.setTipo(tipo);
        llenarArchivo(p, archivo, ruta, actor);
        return ProductoDto.desde(productos.save(p));
    }

    /** Cambia el archivo (y, si se indica, el tipo) de un producto subido por error; conserva su id. */
    @Transactional
    public ProductoDto reemplazar(Long inscripcionId, Long productoId, TipoProducto tipo, MultipartFile archivo,
                                  UsuarioAutenticado actor) {
        Inscripcion i = buscar(inscripcionId);
        exigirDuenoEnCurso(i, actor, "reemplazar");
        ProductoPractica p = productoDe(inscripcionId, productoId);
        String nueva = almacenamiento.guardarDocumento(CARPETA, archivo, EXTENSIONES_PERMITIDAS);
        almacenamiento.reemplazarAlConfirmar(p.getRutaArchivo(), nueva);
        if (tipo != null) {
            p.setTipo(tipo);
        }
        llenarArchivo(p, archivo, nueva, actor);
        p.setFechaSubida(LocalDateTime.now());
        return ProductoDto.desde(productos.saveAndFlush(p));
    }

    @Transactional
    public void borrar(Long inscripcionId, Long productoId, UsuarioAutenticado actor) {
        Inscripcion i = buscar(inscripcionId);
        exigirDuenoEnCurso(i, actor, "borrar");
        ProductoPractica p = productoDe(inscripcionId, productoId);
        productos.delete(p);
        almacenamiento.borrarAlConfirmar(p.getRutaArchivo());
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> listar(Long inscripcionId, UsuarioAutenticado actor) {
        acceso.exigirVerInscripcion(buscar(inscripcionId), actor);
        return productos.findDeInscripcion(inscripcionId).stream().map(ProductoDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargar(Long inscripcionId, Long productoId, UsuarioAutenticado actor) {
        acceso.exigirVerInscripcion(buscar(inscripcionId), actor);
        ProductoPractica p = productoDe(inscripcionId, productoId);
        return new ArchivoDescarga(almacenamiento.cargar(p.getRutaArchivo()), p.getNombreArchivo(), p.getTipoMime());
    }

    private void llenarArchivo(ProductoPractica p, MultipartFile archivo, String ruta, UsuarioAutenticado actor) {
        p.setNombreArchivo(archivo.getOriginalFilename());
        p.setRutaArchivo(ruta);
        p.setTipoMime(archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream");
        p.setTamanoBytes(archivo.getSize());
        p.setSubidoPor(usuarios.findById(actor.id()).orElseThrow());
    }

    private static void exigirDuenoEnCurso(Inscripcion i, UsuarioAutenticado actor, String accion) {
        if (!i.getEstudiante().getUsuario().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el estudiante de la inscripción puede " + accion + " sus informes y productos.");
        }
        if (i.getEstado() != EstadoInscripcion.EN_CURSO) {
            throw new ConflictoException("La inscripción ya está cerrada (" + i.getEstado().name().toLowerCase()
                    + "): sus informes y productos ya no se suben, cambian ni borran.");
        }
    }

    private ProductoPractica productoDe(Long inscripcionId, Long productoId) {
        ProductoPractica p = productos.findById(productoId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el producto con id " + productoId + "."));
        if (!p.getInscripcion().getId().equals(inscripcionId)) {
            throw new RecursoNoEncontradoException("El producto " + productoId + " no pertenece a esta inscripción.");
        }
        return p;
    }

    private Inscripcion buscar(Long id) {
        return inscripciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la inscripción con id " + id + "."));
    }
}
