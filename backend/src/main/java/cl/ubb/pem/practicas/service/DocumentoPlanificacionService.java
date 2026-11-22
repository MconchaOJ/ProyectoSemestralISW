package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.DocumentoDto;
import cl.ubb.pem.practicas.entity.DocumentoPlanificacion;
import cl.ubb.pem.practicas.entity.EstadoPlanificacion;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.DocumentoPlanificacionRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Documentos adjuntos de una planificación (Fase E, punto 3). Solo el estudiante dueño sube; quien ya puede ver
 * la planificación (guía, profesor del curso, coordinador) puede listarlos y descargarlos.
 *
 * HU-21 (2026-11-22): el dueño también borra o reemplaza un documento subido por error. Subir, borrar y reemplazar
 * solo con la planificación en BORRADOR o CON_OBSERVACIONES (409 en revisión, en autorización o autorizada: es lo que
 * el guía y el profesor del curso están revisando o ya aprobaron). Se puede borrar el último documento: al enviar
 * la planificación se vuelve a exigir al menos uno (S-47). El archivo se borra del disco solo después del commit.
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
        exigirEditable(p);
        String ruta = almacenamiento.guardarDocumento("planificaciones", archivo, EXTENSIONES_PERMITIDAS);
        DocumentoPlanificacion d = new DocumentoPlanificacion();
        d.setPlanificacion(p);
        d.setNombreArchivo(archivo.getOriginalFilename());
        d.setRutaArchivo(ruta);
        d.setTipoMime(tipoMime(archivo));
        d.setTamanoBytes(archivo.getSize());
        d.setSubidoPor(usuarios.findById(actor.id()).orElseThrow());
        return DocumentoDto.desde(documentos.save(d));
    }

    /** HU-21: borra un documento subido por error (la fila ahora; el archivo, cuando la base lo confirma). */
    @Transactional
    public void borrar(Long planificacionId, Long documentoId, UsuarioAutenticado actor) {
        Planificacion p = buscar(planificacionId);
        exigirDuenoYEditable(p, actor, "borrar");
        DocumentoPlanificacion d = documentoDe(planificacionId, documentoId);
        documentos.delete(d);
        almacenamiento.borrarAlConfirmar(d.getRutaArchivo());
    }

    /** HU-21: cambia el archivo de un documento (mismo id) en un solo paso: si falla, queda el original. */
    @Transactional
    public DocumentoDto reemplazar(Long planificacionId, Long documentoId, MultipartFile archivo, UsuarioAutenticado actor) {
        Planificacion p = buscar(planificacionId);
        exigirDuenoYEditable(p, actor, "reemplazar");
        DocumentoPlanificacion d = documentoDe(planificacionId, documentoId);
        String nueva = almacenamiento.guardarDocumento("planificaciones", archivo, EXTENSIONES_PERMITIDAS);
        almacenamiento.reemplazarAlConfirmar(d.getRutaArchivo(), nueva);
        d.setNombreArchivo(archivo.getOriginalFilename());
        d.setRutaArchivo(nueva);
        d.setTipoMime(tipoMime(archivo));
        d.setTamanoBytes(archivo.getSize());
        d.setSubidoPor(usuarios.findById(actor.id()).orElseThrow());
        d.setFechaSubida(LocalDateTime.now());
        return DocumentoDto.desde(documentos.saveAndFlush(d));
    }

    private void exigirDuenoYEditable(Planificacion p, UsuarioAutenticado actor, String accion) {
        if (!p.getInscripcion().getEstudiante().getUsuario().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el estudiante dueño de la planificación puede " + accion + " sus documentos.");
        }
        exigirEditable(p);
    }

    /** Solo mientras la planificación está en manos del estudiante (borrador o devuelta con observaciones). */
    private static void exigirEditable(Planificacion p) {
        if (p.getEstado() != EstadoPlanificacion.BORRADOR && p.getEstado() != EstadoPlanificacion.CON_OBSERVACIONES) {
            throw new ConflictoException("La planificación está " + p.getEstado().name().replace('_', ' ').toLowerCase()
                    + ": sus documentos solo se suben, cambian o borran en borrador o con observaciones.");
        }
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
