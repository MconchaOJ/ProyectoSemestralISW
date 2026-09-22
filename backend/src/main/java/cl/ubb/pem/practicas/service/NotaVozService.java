package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.NotaVozDto;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.NotaVoz;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.NotaVozRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * HU-11 y HU-12: nota de voz del profesor evaluador, con transcripción a texto (Fase E, punto 4). Como máximo una
 * por evaluación. Solo el profesor guía evaluador dueño de esa evaluación puede grabar, subir o corregir la
 * transcripción (403 para cualquier otro, incluido otro guía). SUPUESTO S-42: se puede grabar tanto con la
 * evaluación en BORRADOR como ya FINALIZADA, porque es retroalimentación complementaria que no cambia el puntaje.
 * Pueden escucharla y leerla el evaluador, el estudiante evaluado, su profesor del curso y el coordinador (las
 * mismas reglas de acceso que ya existen para ver la evaluación, HU-03).
 */
@Service
public class NotaVozService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".webm", ".ogg", ".mp3", ".wav", ".m4a");

    private final EvaluacionRepository evaluaciones;
    private final NotaVozRepository notasVoz;
    private final EvaluacionService evaluacionService;
    private final AlmacenamientoArchivosService almacenamiento;

    public NotaVozService(EvaluacionRepository evaluaciones, NotaVozRepository notasVoz,
                          EvaluacionService evaluacionService, AlmacenamientoArchivosService almacenamiento) {
        this.evaluaciones = evaluaciones;
        this.notasVoz = notasVoz;
        this.evaluacionService = evaluacionService;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public NotaVozDto crear(Long evaluacionId, MultipartFile audio, String transcripcion, Integer duracionSegundos,
                            UsuarioAutenticado actor) {
        Evaluacion e = exigirEvaluador(evaluacionId, actor);
        if (notasVoz.findByEvaluacionId(evaluacionId).isPresent()) {
            throw new ConflictoException("Esta evaluación ya tiene una nota de voz. Usa PUT para corregir el texto "
                    + "o vuelve a grabar reemplazando el archivo.");
        }
        if (duracionSegundos == null || duracionSegundos <= 0) {
            throw new ReglaNegocioException("La duración de la nota de voz debe ser mayor que 0 segundos.");
        }
        String ruta = almacenamiento.guardar("notas-voz", audio, EXTENSIONES_PERMITIDAS,
                AlmacenamientoArchivosService.TAMANO_MAXIMO_AUDIO);
        NotaVoz n = new NotaVoz();
        n.setEvaluacion(e);
        n.setArchivoRuta(ruta);
        n.setDuracionSegundos(duracionSegundos);
        n.setTranscripcion(limpiar(transcripcion));
        n.setTranscripcionCorregida(false);
        return NotaVozDto.desde(notasVoz.save(n));
    }

    @Transactional
    public NotaVozDto corregirTranscripcion(Long evaluacionId, String transcripcionCorregida, UsuarioAutenticado actor) {
        exigirEvaluador(evaluacionId, actor);
        NotaVoz n = buscarPorEvaluacion(evaluacionId);
        n.setTranscripcion(transcripcionCorregida.trim());
        n.setTranscripcionCorregida(true);
        return NotaVozDto.desde(notasVoz.save(n));
    }

    @Transactional(readOnly = true)
    public NotaVozDto obtener(Long evaluacionId, UsuarioAutenticado actor) {
        Evaluacion e = buscarEvaluacion(evaluacionId);
        exigirVer(e, actor);
        return NotaVozDto.desde(buscarPorEvaluacion(evaluacionId));
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargarAudio(Long evaluacionId, UsuarioAutenticado actor) {
        Evaluacion e = buscarEvaluacion(evaluacionId);
        exigirVer(e, actor);
        NotaVoz n = buscarPorEvaluacion(evaluacionId);
        return new ArchivoDescarga(almacenamiento.cargar(n.getArchivoRuta()), "nota-voz-" + evaluacionId, "audio/webm");
    }

    // ---------------------------------------------------------------------------------------------

    private Evaluacion exigirEvaluador(Long evaluacionId, UsuarioAutenticado actor) {
        Evaluacion e = buscarEvaluacion(evaluacionId);
        if (!e.getEvaluador().getId().equals(actor.id())) {
            throw new AccesoDenegadoException("Solo el profesor guía evaluador de esta evaluación puede grabar o corregir su nota de voz.");
        }
        return e;
    }

    private void exigirVer(Evaluacion e, UsuarioAutenticado actor) {
        if (!evaluacionService.puedeVer(e, actor)) {
            throw new AccesoDenegadoException("No tienes acceso a esta evaluación.");
        }
    }

    private NotaVoz buscarPorEvaluacion(Long evaluacionId) {
        return notasVoz.findByEvaluacionId(evaluacionId).orElseThrow(
                () -> new RecursoNoEncontradoException("Esta evaluación todavía no tiene una nota de voz."));
    }

    private Evaluacion buscarEvaluacion(Long id) {
        return evaluaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la evaluación con id " + id + "."));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
