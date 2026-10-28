package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.DocumentoBaseDto;
import cl.ubb.pem.practicas.entity.DocumentoBaseConocimiento;
import cl.ubb.pem.practicas.entity.TipoDocumentoBase;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.DocumentoBaseConocimientoRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * HU-29: base de conocimiento del chatbot. El coordinador sube o reemplaza los documentos de referencia (uno por
 * tipo, SUPUESTO S-69); el chatbot recibe en cada mensaje los fragmentos más pertinentes (S-70).
 *
 * - Mismo almacenamiento en disco que los documentos adjuntos (Fase E), Word o PDF de hasta 10 MB.
 * - El texto se extrae UNA vez, al subir, y se guarda en la base: un PDF de cientos de páginas no se convierte en cada
 *   mensaje. Si el documento no tiene texto legible (escaneado), se rechaza con 400.
 * - Reemplazar deja el archivo anterior en disco (respaldo); la base solo apunta al nuevo.
 * - Sin documentos cargados el chatbot funciona igual, solo con sus instrucciones.
 */
@Service
public class BaseConocimientoService {

    private static final Logger log = LoggerFactory.getLogger(BaseConocimientoService.class);

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".docx", ".pdf");
    /** Las Bases Curriculares de 7.º a 2.º medio tienen unas 250 páginas. */
    static final int MAXIMO_PAGINAS_PDF = 400;
    static final int MAXIMO_CARACTERES_GUARDADOS = 500_000;
    /** Tope de la base de conocimiento en cada mensaje (unos 1 500 tokens), por la cuota por minuto de Groq. */
    static final int MAXIMO_CARACTERES_POR_MENSAJE = 5_000;

    private final DocumentoBaseConocimientoRepository documentos;
    private final UsuarioRepository usuarios;
    private final AlmacenamientoArchivosService almacenamiento;

    public BaseConocimientoService(DocumentoBaseConocimientoRepository documentos, UsuarioRepository usuarios,
                                   AlmacenamientoArchivosService almacenamiento) {
        this.documentos = documentos;
        this.usuarios = usuarios;
        this.almacenamiento = almacenamiento;
    }

    /** Los 10 tipos de documento, en el orden de la propuesta, con el archivo vigente si lo hay. */
    @Transactional(readOnly = true)
    public List<DocumentoBaseDto> listar() {
        Map<TipoDocumentoBase, DocumentoBaseConocimiento> cargados = documentos.findAll().stream()
                .collect(Collectors.toMap(DocumentoBaseConocimiento::getTipo, Function.identity()));
        return Arrays.stream(TipoDocumentoBase.values())
                .map(t -> cargados.containsKey(t) ? DocumentoBaseDto.desde(cargados.get(t)) : DocumentoBaseDto.vacio(t))
                .toList();
    }

    @Transactional
    public DocumentoBaseDto subir(String tipoTexto, MultipartFile archivo, UsuarioAutenticado actor) {
        TipoDocumentoBase tipo = tipo(tipoTexto);
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debes adjuntar un archivo.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        boolean esPdf = nombre.toLowerCase(Locale.ROOT).endsWith(".pdf");
        if (!esPdf && !nombre.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new ReglaNegocioException("Solo se aceptan documentos Word (.docx) o PDF. Un .odt o .doc se puede "
                    + "guardar como .docx desde el procesador de texto.");
        }
        String texto = extraer(archivo, esPdf);
        String ruta = almacenamiento.guardar("base-conocimiento", archivo, EXTENSIONES_PERMITIDAS,
                AlmacenamientoArchivosService.TAMANO_MAXIMO_DOCUMENTO);

        DocumentoBaseConocimiento d = documentos.findByTipo(tipo).orElseGet(DocumentoBaseConocimiento::new);
        d.setTipo(tipo);
        d.setNombreArchivo(nombre);
        d.setRutaArchivo(ruta);
        d.setTipoMime(archivo.getContentType() == null ? "application/octet-stream" : archivo.getContentType());
        d.setTamanoBytes(archivo.getSize());
        d.setTexto(texto);
        d.setSubidoPor(usuarios.getReferenceById(actor.id()));
        d.setFechaSubida(LocalDateTime.now());
        documentos.save(d);
        log.info("El coordinador {} cargó {} en la base de conocimiento ({}, {} caracteres)", actor.id(), tipo, nombre,
                texto.length());
        return DocumentoBaseDto.desde(d);
    }

    @Transactional
    public void eliminar(String tipoTexto) {
        DocumentoBaseConocimiento d = documentos.findByTipo(tipo(tipoTexto)).orElseThrow(() ->
                new RecursoNoEncontradoException("No hay un documento cargado de ese tipo."));
        documentos.delete(d);
    }

    /**
     * El material de referencia para un mensaje del chatbot: los fragmentos más pertinentes para la consulta (el
     * mensaje del estudiante con su planificación). Vacío si no hay documentos o nada coincide.
     */
    @Transactional(readOnly = true)
    public String referenciaPara(String consulta) {
        List<FragmentosBaseConocimiento.Documento> docs = documentos.findAll().stream()
                .sorted(Comparator.comparing(DocumentoBaseConocimiento::getTipo))
                .map(d -> new FragmentosBaseConocimiento.Documento(
                        d.getTipo().nombre() + " (\"" + d.getNombreArchivo() + "\")", d.getTexto()))
                .toList();
        return FragmentosBaseConocimiento.elegir(docs, consulta, MAXIMO_CARACTERES_POR_MENSAJE);
    }

    private static TipoDocumentoBase tipo(String texto) {
        try {
            return TipoDocumentoBase.valueOf(texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Tipo de documento desconocido: " + texto + ". Tipos válidos: "
                    + Arrays.toString(TipoDocumentoBase.values()) + ".");
        }
    }

    private static String extraer(MultipartFile archivo, boolean esPdf) {
        String texto;
        try {
            byte[] bytes = archivo.getBytes();
            texto = esPdf ? TextoPdf.extraer(bytes, MAXIMO_PAGINAS_PDF)
                    : TextoDocx.extraer(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            throw new ReglaNegocioException("No se pudo leer el documento: revisa que sea un Word (.docx) o PDF válido.");
        }
        texto = InstruccionesChatbot.normalizar(texto).strip();
        if (texto.isBlank()) {
            throw new ReglaNegocioException("El documento no tiene texto legible (puede ser un PDF escaneado o solo "
                    + "imágenes): el asistente solo lee texto.");
        }
        return texto.length() > MAXIMO_CARACTERES_GUARDADOS ? texto.substring(0, MAXIMO_CARACTERES_GUARDADOS) : texto;
    }
}
