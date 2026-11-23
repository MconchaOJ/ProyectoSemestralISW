package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.ArchivoOficialDto;
import cl.ubb.pem.practicas.entity.ArchivoOficialPauta;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.ArchivoOficialPautaRepository;
import cl.ubb.pem.practicas.repository.PautaRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * HU-25: archivos oficiales de las pautas (PDF o Word firmados y timbrados).
 * <ul>
 *   <li>Criterio 1: el coordinador sube un archivo por pauta y lo reemplaza subiendo otro (el rol lo exige el
 *       controlador). Reemplazar no borra el anterior: es una versión nueva del archivo.</li>
 *   <li>Criterio 2: cada versión guarda quién y cuándo; el historial lo ven el coordinador y el jefe de carrera.</li>
 *   <li>Criterio 3: quien puede ver la pauta (cualquier usuario autenticado, S-27) descarga el archivo vigente.</li>
 *   <li>SUPUESTO S-95: PDF o Word, con el tamaño máximo vigente (HU-27). SUPUESTO S-96: el archivo es de una versión
 *       de la pauta; una versión nueva (HU-06) no lo hereda.</li>
 * </ul>
 * Reutiliza el guardado de archivos de HU-21 ({@link AlmacenamientoArchivosService}).
 */
@Service
@Transactional(readOnly = true)
public class ArchivoOficialPautaService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".pdf", ".docx");
    private static final String CARPETA = "pautas";

    private final PautaRepository pautas;
    private final ArchivoOficialPautaRepository archivos;
    private final UsuarioRepository usuarios;
    private final AlmacenamientoArchivosService almacenamiento;

    public ArchivoOficialPautaService(PautaRepository pautas, ArchivoOficialPautaRepository archivos,
                                      UsuarioRepository usuarios, AlmacenamientoArchivosService almacenamiento) {
        this.pautas = pautas;
        this.archivos = archivos;
        this.usuarios = usuarios;
        this.almacenamiento = almacenamiento;
    }

    /** Sube el archivo oficial de la pauta; si ya tenía uno, este pasa a ser el vigente y el otro queda en el historial. */
    @Transactional
    public ArchivoOficialDto subir(Integer pautaId, MultipartFile archivo, UsuarioAutenticado actor) {
        Pauta p = buscarPauta(pautaId);
        int version = archivos.findFirstByPautaIdOrderByVersionDesc(pautaId).map(a -> a.getVersion() + 1).orElse(1);
        String ruta = almacenamiento.guardarDocumento(CARPETA, archivo, EXTENSIONES_PERMITIDAS);
        ArchivoOficialPauta a = new ArchivoOficialPauta();
        a.setPauta(p);
        a.setVersion(version);
        a.setNombreArchivo(archivo.getOriginalFilename());
        a.setRutaArchivo(ruta);
        a.setTipoMime(archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream");
        a.setTamanoBytes(archivo.getSize());
        a.setSubidoPor(usuarios.findById(actor.id()).orElseThrow());
        return ArchivoOficialDto.desde(archivos.saveAndFlush(a));
    }

    /** El archivo vigente de una pauta, o vacío si no tiene. */
    public Optional<ArchivoOficialDto> vigente(Integer pautaId) {
        return archivos.findFirstByPautaIdOrderByVersionDesc(pautaId).map(ArchivoOficialDto::desde);
    }

    /** El archivo vigente de cada pauta que tenga alguno, por id de pauta. */
    public Map<Integer, ArchivoOficialDto> vigentes() {
        return archivos.findVigentes().stream().map(ArchivoOficialDto::desde)
                .collect(Collectors.toMap(ArchivoOficialDto::pautaId, a -> a));
    }

    public List<ArchivoOficialDto> historial(Integer pautaId) {
        buscarPauta(pautaId);
        return archivos.findHistorial(pautaId).stream().map(ArchivoOficialDto::desde).toList();
    }

    public ArchivoDescarga descargarVigente(Integer pautaId) {
        buscarPauta(pautaId);
        return descarga(archivos.findFirstByPautaIdOrderByVersionDesc(pautaId).orElseThrow(
                () -> new RecursoNoEncontradoException("La pauta " + pautaId + " todavía no tiene archivo oficial.")));
    }

    public ArchivoDescarga descargar(Integer pautaId, Long archivoId) {
        ArchivoOficialPauta a = archivos.findById(archivoId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el archivo oficial con id " + archivoId + "."));
        if (!a.getPauta().getId().equals(pautaId)) {
            throw new RecursoNoEncontradoException("El archivo " + archivoId + " no es de la pauta " + pautaId + ".");
        }
        return descarga(a);
    }

    /**
     * Al borrar una pauta sin uso (HU-06) se borran también sus archivos oficiales: las filas ahora (antes que la
     * pauta, por la clave foránea) y los archivos del disco cuando la base lo confirma.
     */
    @Transactional
    public void borrarDePauta(Integer pautaId) {
        List<ArchivoOficialPauta> lista = archivos.findHistorial(pautaId);
        if (lista.isEmpty()) {
            return;
        }
        archivos.deleteAll(lista);
        archivos.flush();
        lista.forEach(a -> almacenamiento.borrarAlConfirmar(a.getRutaArchivo()));
    }

    private ArchivoDescarga descarga(ArchivoOficialPauta a) {
        return new ArchivoDescarga(almacenamiento.cargar(a.getRutaArchivo()), a.getNombreArchivo(), a.getTipoMime());
    }

    private Pauta buscarPauta(Integer id) {
        return pautas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la pauta con id " + id + "."));
    }
}
