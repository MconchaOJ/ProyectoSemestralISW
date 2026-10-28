package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.InstruccionesChatbotDto;
import cl.ubb.pem.practicas.dto.InstruccionesChatbotRequest;
import cl.ubb.pem.practicas.dto.UsuarioResumenDto;
import cl.ubb.pem.practicas.entity.CalibracionChatbot;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.CalibracionChatbotRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-17: el coordinador calibra las instrucciones del chatbot (tono, expresiones, reglas del flujo y respuestas
 * modelo) sin tocar código. Solo el coordinador llega aquí (@PreAuthorize en el controlador).
 *
 * - Hay un solo texto para todo el sistema (fila id 1 de instrucciones_chatbot). Si la tabla está vacía (una base de
 *   producción recién instalada) o el texto está en blanco, el chatbot usa el texto base de RetroPlani Mate: nunca se
 *   queda sin instrucciones.
 * - SUPUESTO S-68: un cambio rige desde el SIGUIENTE mensaje, también en las conversaciones en curso; los mensajes ya
 *   guardados no se tocan (criterio 3). No se guarda el historial de versiones: solo quién y cuándo editó por última vez.
 * - SUPUESTO S-67: entre 200 y 12 000 caracteres. El mínimo evita guardar por error un texto vacío o de prueba; el
 *   máximo protege la cuota de tokens por minuto de Groq, porque el texto viaja en cada mensaje.
 */
@Service
public class CalibracionChatbotService {

    private static final Logger log = LoggerFactory.getLogger(CalibracionChatbotService.class);

    static final int MINIMO_CARACTERES = 200;
    static final int MAXIMO_CARACTERES = 12_000;

    private final CalibracionChatbotRepository calibraciones;
    private final UsuarioRepository usuarios;

    public CalibracionChatbotService(CalibracionChatbotRepository calibraciones, UsuarioRepository usuarios) {
        this.calibraciones = calibraciones;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public InstruccionesChatbotDto obtener() {
        return calibraciones.findById(CalibracionChatbot.ID_UNICO)
                .filter(c -> !c.getTexto().isBlank())
                .map(c -> dto(c.getTexto(), UsuarioResumenDto.desde(c.getEditadoPor()), c.getFechaEdicion()))
                .orElseGet(() -> dto(InstruccionesChatbot.TEXTO_BASE, null, null));
    }

    @Transactional
    public InstruccionesChatbotDto actualizar(InstruccionesChatbotRequest req, UsuarioAutenticado actor) {
        String texto = InstruccionesChatbot.normalizar(req.texto()).strip();
        if (texto.length() < MINIMO_CARACTERES || texto.length() > MAXIMO_CARACTERES) {
            throw new ReglaNegocioException("Las instrucciones deben tener entre " + MINIMO_CARACTERES + " y "
                    + MAXIMO_CARACTERES + " caracteres (tienen " + texto.length() + ").");
        }
        CalibracionChatbot c = calibraciones.findById(CalibracionChatbot.ID_UNICO).orElseGet(() -> {
            CalibracionChatbot nueva = new CalibracionChatbot();
            nueva.setId(CalibracionChatbot.ID_UNICO);
            return nueva;
        });
        c.setTexto(texto);
        c.setEditadoPor(usuarios.getReferenceById(actor.id()));
        c.setFechaEdicion(LocalDateTime.now());
        calibraciones.save(c);
        log.info("El coordinador {} actualizó las instrucciones del chatbot ({} caracteres)", actor.id(), texto.length());
        return dto(texto, UsuarioResumenDto.desde(usuarios.findById(actor.id()).orElseThrow()), c.getFechaEdicion());
    }

    /** Las reglas que el chatbot usa en el próximo mensaje: las del coordinador o, si no hay, el texto base. */
    @Transactional(readOnly = true)
    public String textoVigente() {
        return calibraciones.findById(CalibracionChatbot.ID_UNICO)
                .map(CalibracionChatbot::getTexto)
                .filter(t -> !t.isBlank())
                .map(InstruccionesChatbot::normalizar)
                .orElse(InstruccionesChatbot.TEXTO_BASE);
    }

    private static InstruccionesChatbotDto dto(String texto, UsuarioResumenDto editadoPor, LocalDateTime fecha) {
        String normalizado = InstruccionesChatbot.normalizar(texto).strip();
        return new InstruccionesChatbotDto(normalizado, normalizado.equals(InstruccionesChatbot.TEXTO_BASE), editadoPor,
                fecha, InstruccionesChatbot.TEXTO_BASE, MINIMO_CARACTERES, MAXIMO_CARACTERES);
    }
}
