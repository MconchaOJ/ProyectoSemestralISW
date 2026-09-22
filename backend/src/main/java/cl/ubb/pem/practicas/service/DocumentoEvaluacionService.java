package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.DocumentoDto;
import cl.ubb.pem.practicas.entity.DocumentoEvaluacion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.DocumentoEvaluacionRepository;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Documentos adjuntos de una evaluación (Fase E, punto 3): la pauta y evaluación formal que sube el profesor guía,
 * al finalizar o después de finalizar la evaluación. Solo el evaluador dueño sube; quien ya puede ver la
 * evaluación (mismas reglas de HU-03, incluida la privacidad del borrador) puede listar y descargar.
 */
@Service
public class DocumentoEvaluacionService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".docx", ".xlsx", ".pdf");

    private final EvaluacionRepository evaluaciones;
    private final DocumentoEvaluacionRepository documentos;
    private final UsuarioRepository usuarios;
    private final EvaluacionService evaluacionService;
    private final AlmacenamientoArchivosService almacenamiento;

    public DocumentoEvaluacionService(EvaluacionRepository evaluaciones, DocumentoEvaluacionRepository documentos,
                                      UsuarioRepository usuarios, EvaluacionService evaluacionService,
                                      AlmacenamientoArchivosService almacenamiento) {
        this.evaluaciones = evaluaciones;
        this.documentos = documentos;
        this.usuarios = usuarios;
        this.evaluacionService = evaluacionService;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public DocumentoDto subir(Long evaluacionId, MultipartFile archivo, UsuarioAutenticado actor) {
        Evaluacion e = buscar(evaluacionId);
        if (!e.getEvaluador().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el profesor guía evaluador puede subir documentos a esta evaluación.");
        }
        String ruta = almacenamiento.guardar("evaluaciones", archivo, EXTENSIONES_PERMITIDAS,
                AlmacenamientoArchivosService.TAMANO_MAXIMO_DOCUMENTO);
        DocumentoEvaluacion d = new DocumentoEvaluacion();
        d.setEvaluacion(e);
        d.setNombreArchivo(archivo.getOriginalFilename());
        d.setRutaArchivo(ruta);
        d.setTipoMime(tipoMime(archivo));
        d.setTamanoBytes(archivo.getSize());
        d.setSubidoPor(usuarios.findById(actor.id()).orElseThrow());
        return DocumentoDto.desde(documentos.save(d));
    }

    @Transactional(readOnly = true)
    public List<DocumentoDto> listar(Long evaluacionId, UsuarioAutenticado actor) {
        Evaluacion e = buscar(evaluacionId);
        exigirVer(e, actor);
        return documentos.findByEvaluacionIdOrderByFechaSubidaAsc(evaluacionId).stream()
                .map(DocumentoDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargar(Long evaluacionId, Long documentoId, UsuarioAutenticado actor) {
        Evaluacion e = buscar(evaluacionId);
        exigirVer(e, actor);
        DocumentoEvaluacion d = documentoDe(evaluacionId, documentoId);
        return new ArchivoDescarga(almacenamiento.cargar(d.getRutaArchivo()), d.getNombreArchivo(), d.getTipoMime());
    }

    private void exigirVer(Evaluacion e, UsuarioAutenticado actor) {
        if (!evaluacionService.puedeVer(e, actor)) {
            throw new AccesoDenegadoException("No tienes acceso a esta evaluación.");
        }
    }

    private DocumentoEvaluacion documentoDe(Long evaluacionId, Long documentoId) {
        DocumentoEvaluacion d = documentos.findById(documentoId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el documento con id " + documentoId + "."));
        if (!d.getEvaluacion().getId().equals(evaluacionId)) {
            throw new RecursoNoEncontradoException("El documento " + documentoId + " no pertenece a esta evaluación.");
        }
        return d;
    }

    private Evaluacion buscar(Long id) {
        return evaluaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la evaluación con id " + id + "."));
    }

    private static String tipoMime(MultipartFile archivo) {
        return archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream";
    }
}
