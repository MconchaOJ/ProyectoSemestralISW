package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EstudianteResumenDto;
import cl.ubb.pem.practicas.dto.ValidacionChatbotDto;
import cl.ubb.pem.practicas.entity.CalibracionChatbot;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.CalibracionChatbotRepository;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-30: el coordinador marca a un grupo de estudiantes como validadores y abre la validación; mientras está abierta,
 * el chatbot solo responde a ellos (el resto recibe 409). Los validadores responden la Escala SUS y la rúbrica de
 * juicio experto en el sistema ({@link RespuestasValidacionService}, S-75; reemplaza a S-71).
 *
 * La marca vive en la fila de configuración del chatbot (instrucciones_chatbot.validacion_abierta). Si esa fila no
 * existe (producción recién instalada), abrir la validación la crea con el texto base sin editar.
 */
@Service
public class ValidacionChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ValidacionChatbotService.class);

    private final CalibracionChatbotRepository configuracion;
    private final EstudianteRepository estudiantes;

    public ValidacionChatbotService(CalibracionChatbotRepository configuracion, EstudianteRepository estudiantes) {
        this.configuracion = configuracion;
        this.estudiantes = estudiantes;
    }

    @Transactional(readOnly = true)
    public boolean abierta() {
        return configuracion.findById(CalibracionChatbot.ID_UNICO)
                .map(c -> Boolean.TRUE.equals(c.getValidacionAbierta())).orElse(false);
    }

    /** ¿Este estudiante puede conversar con el chatbot? Sí, salvo con la validación abierta y sin ser validador. */
    @Transactional(readOnly = true)
    public boolean puedeUsar(Estudiante e) {
        return !abierta() || Boolean.TRUE.equals(e.getValidadorChatbot());
    }

    @Transactional(readOnly = true)
    public ValidacionChatbotDto estado() {
        return new ValidacionChatbotDto(abierta(), estudiantes.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getValidadorChatbot()))
                .map(EstudianteResumenDto::desde)
                .sorted(Comparator.comparing(EstudianteResumenDto::nombreCompleto))
                .toList());
    }

    @Transactional
    public ValidacionChatbotDto cambiarApertura(boolean abierta, UsuarioAutenticado actor) {
        CalibracionChatbot c = configuracion.findById(CalibracionChatbot.ID_UNICO).orElseGet(() -> {
            CalibracionChatbot nueva = new CalibracionChatbot();
            nueva.setId(CalibracionChatbot.ID_UNICO);
            nueva.setTexto(InstruccionesChatbot.TEXTO_BASE);
            return nueva;
        });
        c.setValidacionAbierta(abierta);
        configuracion.save(c);
        log.info("El coordinador {} {} la validación del chatbot", actor.id(), abierta ? "abrió" : "cerró");
        return estado();
    }

    @Transactional
    public ValidacionChatbotDto marcarValidador(Long estudianteId, boolean validador) {
        Estudiante e = estudiantes.findById(estudianteId).orElseThrow(() ->
                new RecursoNoEncontradoException("No existe el estudiante con id " + estudianteId + "."));
        e.setValidadorChatbot(validador);
        return estado();
    }
}
