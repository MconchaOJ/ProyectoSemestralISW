package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.ConversacionChatbotDto;
import cl.ubb.pem.practicas.dto.HistorialDto;
import cl.ubb.pem.practicas.dto.MensajeChatbotDto;
import cl.ubb.pem.practicas.dto.MensajeChatbotRequest;
import cl.ubb.pem.practicas.dto.RespuestaChatbotDto;
import cl.ubb.pem.practicas.dto.UsuarioResumenDto;
import cl.ubb.pem.practicas.entity.AutorMensaje;
import cl.ubb.pem.practicas.entity.DocumentoPlanificacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.MensajeChatbot;
import cl.ubb.pem.practicas.entity.Planificacion;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.DocumentoPlanificacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.MensajeChatbotRepository;
import cl.ubb.pem.practicas.repository.PlanificacionRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.ModeloLenguaje.Solicitud;
import cl.ubb.pem.practicas.service.ModeloLenguaje.Turno;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * HU-15: chatbot que retroalimenta la planificación que el estudiante ya escribió, con su historial como contexto
 * (HU-05: semestre, prácticas aprobadas, evaluaciones). HU-16: el profesor guía de la inscripción lee la conversación.
 *
 * Reglas del sistema (las del flujo dialógico las sigue el modelo, ver {@link InstruccionesChatbot}):
 * - Solo en las asignaturas con chatbot habilitado (las dos prácticas finales) y con la inscripción EN_CURSO; cerrada,
 *   la conversación queda para consulta (SUPUESTO S-63).
 * - Escribe solo el estudiante dueño. Leen el estudiante y su profesor guía (S-60, {@link AccesoService#puedeVerChat}).
 * - El estudiante "adjunta" su planificación eligiéndola: el chatbot lee su formulario y sus documentos (HU-21); los
 *   Word y PDF se convierten a texto, porque el proveedor solo recibe texto (SUPUESTO S-61).
 * - Si el proveedor del modelo falla, responde 503 y NO se guarda nada: el estudiante reintenta sin dejar un mensaje
 *   huérfano. Por eso la llamada al modelo va fuera de cualquier transacción (no deja una conexión a la base tomada
 *   mientras se espera la respuesta).
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    /** Tope de documentos por mensaje (cada documento ya pesa máximo 10 MB, HU-21). */
    static final int MAXIMO_DOCUMENTOS = 5;
    /**
     * Tope del texto de TODOS los documentos juntos (unos 4 000 tokens). La capa gratuita de Groq limita los tokens
     * por minuto, y cada mensaje vuelve a enviar las instrucciones, la conversación y la planificación.
     */
    static final int MAXIMO_CARACTERES_DOCUMENTOS = 14_000;

    static final String SOLO_PRACTICAS_FINALES = "El asistente de planificación solo está disponible en Práctica "
            + "Pedagógica y Práctica Profesional.";
    static final String PRACTICA_CERRADA = "Esta práctica ya está cerrada: la conversación queda solo para consulta.";

    private final InscripcionRepository inscripciones;
    private final PlanificacionRepository planificaciones;
    private final DocumentoPlanificacionRepository documentos;
    private final MensajeChatbotRepository mensajes;
    private final HistorialService historial;
    private final AccesoService acceso;
    private final AlmacenamientoArchivosService almacenamiento;
    private final ModeloLenguaje modelo;
    private final TransactionTemplate lectura;
    private final TransactionTemplate escritura;

    public ChatbotService(InscripcionRepository inscripciones, PlanificacionRepository planificaciones,
                          DocumentoPlanificacionRepository documentos, MensajeChatbotRepository mensajes,
                          HistorialService historial, AccesoService acceso,
                          AlmacenamientoArchivosService almacenamiento, ModeloLenguaje modelo,
                          PlatformTransactionManager transacciones) {
        this.inscripciones = inscripciones;
        this.planificaciones = planificaciones;
        this.documentos = documentos;
        this.mensajes = mensajes;
        this.historial = historial;
        this.acceso = acceso;
        this.almacenamiento = almacenamiento;
        this.modelo = modelo;
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
        this.escritura = new TransactionTemplate(transacciones);
    }

    /** La conversación de una inscripción (HU-15 para el estudiante, HU-16 para su profesor guía). */
    @Transactional(readOnly = true)
    public ConversacionChatbotDto conversacion(Long inscripcionId, UsuarioAutenticado actor) {
        Inscripcion i = buscarInscripcion(inscripcionId);
        acceso.exigirVerChat(i, actor);
        String motivo = motivoNoDisponible(i);
        return new ConversacionChatbotDto(i.getId(),
                i.getEstudiante().getUsuario().getNombre() + " " + i.getEstudiante().getUsuario().getApellidos(),
                i.getAsignatura().getNombre(), i.getPeriodo(), motivo == null, motivo,
                motivo == null && esElEstudiante(i, actor), UsuarioResumenDto.desde(i.getProfesorGuia()),
                mensajes.findConversacion(i.getId()).stream().map(MensajeChatbotDto::desde).toList());
    }

    /** El estudiante escribe; el chatbot responde y se guardan los dos mensajes (criterio 9). */
    public RespuestaChatbotDto enviar(Long inscripcionId, MensajeChatbotRequest req, UsuarioAutenticado actor) {
        LocalDateTime enviado = LocalDateTime.now();
        Preparado p = lectura.execute(estado -> preparar(inscripcionId, req, actor));
        String respuesta = modelo.responder(p.solicitud());   // fuera de la transacción; si falla, 503 y nada guardado
        return escritura.execute(estado -> guardar(p, req.texto().trim(), respuesta, enviado));
    }

    // ---------------------------------------------------------------------------------------------

    /** Lo que se reunió antes de llamar al modelo. {@code planificacionAdjunta} es la que vino en ESTE mensaje. */
    private record Preparado(Long inscripcionId, Long planificacionAdjunta, Solicitud solicitud) {
    }

    private Preparado preparar(Long inscripcionId, MensajeChatbotRequest req, UsuarioAutenticado actor) {
        Inscripcion i = buscarInscripcion(inscripcionId);
        if (!esElEstudiante(i, actor)) {
            throw new AccesoDenegadoException("Solo el estudiante de esta práctica puede escribir en su conversación.");
        }
        String motivo = motivoNoDisponible(i);
        if (motivo != null) {
            throw new ConflictoException(motivo);
        }

        List<MensajeChatbot> anteriores = mensajes.findConversacion(i.getId());
        Planificacion enRevision = null;
        if (req.planificacionId() != null) {
            enRevision = planificaciones.findById(req.planificacionId()).orElseThrow(() ->
                    new RecursoNoEncontradoException("No existe la planificación con id " + req.planificacionId() + "."));
            if (!enRevision.getInscripcion().getId().equals(i.getId())) {
                throw new ReglaNegocioException("La planificación elegida no es de esta práctica.");
            }
        } else {
            // sigue con la última que el estudiante adjuntó en esta conversación
            for (MensajeChatbot m : anteriores) {
                if (m.getPlanificacion() != null) {
                    enRevision = m.getPlanificacion();
                }
            }
        }

        // Contexto: el historial del estudiante (HU-05), con las mismas reglas de acceso del endpoint de historial
        HistorialDto h = historial.historialDe(i.getEstudiante().getId(), actor);
        List<Turno> historia = anteriores.stream()
                .map(m -> new Turno(m.getAutor() == AutorMensaje.ESTUDIANTE, m.getContenido()))
                .toList();
        String mensaje = (enRevision == null
                ? "(El estudiante no ha adjuntado ninguna planificación en esta conversación.)\n\n"
                : planificacionComoTexto(enRevision))
                + "Mensaje del estudiante:\n" + req.texto().trim();
        return new Preparado(i.getId(), req.planificacionId(),
                new Solicitud(InstruccionesChatbot.armar(h, i.getId()), historia, mensaje));
    }

    private RespuestaChatbotDto guardar(Preparado p, String texto, String respuesta, LocalDateTime enviado) {
        Inscripcion i = inscripciones.getReferenceById(p.inscripcionId());
        MensajeChatbot delEstudiante = new MensajeChatbot();
        delEstudiante.setInscripcion(i);
        delEstudiante.setAutor(AutorMensaje.ESTUDIANTE);
        delEstudiante.setContenido(texto);
        delEstudiante.setFecha(enviado);
        if (p.planificacionAdjunta() != null) {
            delEstudiante.setPlanificacion(planificaciones.getReferenceById(p.planificacionAdjunta()));
        }
        MensajeChatbot delChatbot = new MensajeChatbot();
        delChatbot.setInscripcion(i);
        delChatbot.setAutor(AutorMensaje.CHATBOT);
        delChatbot.setContenido(respuesta);
        LocalDateTime ahora = LocalDateTime.now();
        delChatbot.setFecha(ahora.isAfter(enviado) ? ahora : enviado.plusNanos(1_000_000));
        mensajes.save(delEstudiante);
        mensajes.save(delChatbot);
        return new RespuestaChatbotDto(MensajeChatbotDto.desde(delEstudiante), MensajeChatbotDto.desde(delChatbot));
    }

    /**
     * La planificación como texto para el modelo: el formulario del sistema y el texto de sus documentos (Word y PDF;
     * el proveedor solo recibe texto). Los documentos comparten un tope de caracteres por la cuota de tokens del
     * proveedor: lo que sobra se recorta y se avisa.
     */
    private String planificacionComoTexto(Planificacion p) {
        StringBuilder s = new StringBuilder("=== PLANIFICACIÓN EN REVISIÓN (adjuntada por el estudiante desde el sistema) ===\n");
        s.append("Título: ").append(p.getTitulo()).append('\n');
        s.append("Estado en el sistema: ").append(p.getEstado()).append('\n');
        s.append("Fecha de la clase: ").append(valor(p.getFechaClase())).append('\n');
        s.append("Duración: ").append(p.getDuracionMinutos() == null ? "(sin completar)" : p.getDuracionMinutos() + " minutos").append('\n');
        s.append("Objetivo: ").append(valor(p.getObjetivo())).append('\n');
        s.append("Recursos: ").append(valor(p.getRecursos())).append('\n');
        s.append("Tiempos: ").append(valor(p.getTiempos())).append('\n');
        s.append("Evaluación del aprendizaje: ").append(valor(p.getEvaluacionAprendizaje())).append('\n');
        s.append("Interacción de los participantes con los recursos: ").append(valor(p.getInteraccionParticipantes())).append('\n');

        List<DocumentoPlanificacion> docs = documentos.findByPlanificacionIdOrderByFechaSubidaAsc(p.getId());
        if (docs.isEmpty()) {
            s.append("Documentos adjuntos: ninguno.\n");
        } else {
            if (docs.size() > MAXIMO_DOCUMENTOS) {
                s.append("(Tiene ").append(docs.size()).append(" documentos; se leen los ")
                        .append(MAXIMO_DOCUMENTOS).append(" más recientes.)\n");
                docs = docs.subList(docs.size() - MAXIMO_DOCUMENTOS, docs.size());
            }
            int disponibles = MAXIMO_CARACTERES_DOCUMENTOS;
            for (DocumentoPlanificacion d : docs) {
                String nombre = d.getNombreArchivo();
                boolean esPdf = nombre.toLowerCase(Locale.ROOT).endsWith(".pdf");
                try {
                    String texto;
                    try (InputStream in = almacenamiento.cargar(d.getRutaArchivo()).getInputStream()) {
                        texto = esPdf ? TextoPdf.extraer(in.readAllBytes()) : TextoDocx.extraer(in);
                    }
                    if (texto.length() > disponibles) {
                        texto = texto.substring(0, Math.max(0, disponibles))
                                + "\n[… texto recortado: los documentos superan lo que el asistente puede leer de una vez]";
                    }
                    disponibles -= Math.min(texto.length(), disponibles);
                    s.append("--- Texto del documento ").append(esPdf ? "PDF" : "Word").append(" \"").append(nombre)
                            .append("\" ---\n")
                            .append(texto.isBlank() ? "(sin texto legible: puede ser un documento escaneado o solo con imágenes)" : texto)
                            .append("\n--- Fin de \"").append(nombre).append("\" ---\n");
                } catch (IOException | RuntimeException e) {
                    log.warn("No se pudo leer el documento {} de la planificación {}: {}", d.getId(), p.getId(), e.toString());
                    s.append("Documento \"").append(nombre).append("\": no se pudo leer.\n");
                }
            }
        }
        return s.append("=== FIN DE LA PLANIFICACIÓN ===\n\n").toString();
    }

    private static String valor(Object v) {
        return v == null || v.toString().isBlank() ? "(sin completar)" : v.toString();
    }

    private static String motivoNoDisponible(Inscripcion i) {
        if (!Boolean.TRUE.equals(i.getAsignatura().getChatbotHabilitado())) {
            return SOLO_PRACTICAS_FINALES;
        }
        return i.getEstado() == EstadoInscripcion.EN_CURSO ? null : PRACTICA_CERRADA;
    }

    private static boolean esElEstudiante(Inscripcion i, UsuarioAutenticado actor) {
        return actor.tieneRol(Roles.ESTUDIANTE) && i.getEstudiante().getUsuario().getId().equals(actor.id());
    }

    private Inscripcion buscarInscripcion(Long id) {
        return inscripciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la inscripción con id " + id + "."));
    }
}
