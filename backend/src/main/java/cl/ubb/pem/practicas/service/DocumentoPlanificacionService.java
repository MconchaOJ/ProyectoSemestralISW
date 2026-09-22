package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.DocumentoDto;
import cl.ubb.pem.practicas.entity.DocumentoPlanificacion;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.DocumentoPlanificacionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Documentos adjuntos de una planificación (Fase E, punto 3). Solo el estudiante dueño sube; quien ya puede ver
 * la planificación (guía, profesor del curso, coordinador) puede listarlos y descargarlos.
 */
@Service
public class DocumentoPlanificacionService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".docx", ".pdf");

    private final PlanificacionRepository planificaciones;
    private final DocumentoPlanificacionRepository documentos;
    private final UsuarioRepository usuarios;
    private final AccesoService acceso;
    private final AlmacenamientoArchivosService almacenamiento;

    public DocumentoPlanificacionService(PlanificacionRepository planificaciones, DocumentoPlanificacionRepository documentos,
                                         UsuarioRepository usuarios, AccesoService acceso,
                                         AlmacenamientoArchivosService almacenamiento) {
        this.planificaciones = planificaciones;
        this.documentos = documentos;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public DocumentoDto subir(Long planificacionId, MultipartFile archivo, UsuarioAutenticado actor) {
        Planificacion p = buscar(planificacionId);
        if (!p.getInscripcion().getEstudiante().getUsuario().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el estudiante dueño de la planificación puede subir documentos.");
        }
        String ruta = almacenamiento.guardar("planificaciones", archivo, EXTENSIONES_PERMITIDAS,
                AlmacenamientoArchivosService.TAMANO_MAXIMO_DOCUMENTO);
        DocumentoPlanificacion d = new DocumentoPlanificacion();
        d.setPlanificacion(p);
        d.setNombreArchivo(archivo.getOriginalFilename());
        d.setRutaArchivo(ruta);
        d.setTipoMime(tipoMime(archivo));
        d.setTamanoBytes(archivo.getSize());
        d.setSubidoPor(usuarios.findById(actor.id()).orElseThrow());
        return DocumentoDto.desde(documentos.save(d));
    }

    @Transactional(readOnly = true)
    public List<DocumentoDto> listar(Long planificacionId, UsuarioAutenticado actor) {
        Planificacion p = buscar(planificacionId);
        acceso.exigirVerInscripcion(p.getInscripcion(), actor);
        return documentos.findByPlanificacionIdOrderByFechaSubidaAsc(planificacionId).stream()
                .map(DocumentoDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargar(Long planificacionId, Long documentoId, UsuarioAutenticado actor) {
        Planificacion p = buscar(planificacionId);
        acceso.exigirVerInscripcion(p.getInscripcion(), actor);
        DocumentoPlanificacion d = documentoDe(planificacionId, documentoId);
        return new ArchivoDescarga(almacenamiento.cargar(d.getRutaArchivo()), d.getNombreArchivo(), d.getTipoMime());
    }

    private DocumentoPlanificacion documentoDe(Long planificacionId, Long documentoId) {
        DocumentoPlanificacion d = documentos.findById(documentoId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el documento con id " + documentoId + "."));
        if (!d.getPlanificacion().getId().equals(planificacionId)) {
            throw new RecursoNoEncontradoException("El documento " + documentoId + " no pertenece a esta planificación.");
        }
        return d;
    }

    private Planificacion buscar(Long id) {
        return planificaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la planificación con id " + id + "."));
    }

    private static String tipoMime(MultipartFile archivo) {
        return archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream";
    }
}
