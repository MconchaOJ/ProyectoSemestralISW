package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.InstrumentosValidacionDto;
import cl.ubb.pem.practicas.dto.MiValidacionDto;
import cl.ubb.pem.practicas.dto.RespuestaRubricaRequest;
import cl.ubb.pem.practicas.dto.RespuestaSusRequest;
import cl.ubb.pem.practicas.dto.ResultadosValidacionDto;
import cl.ubb.pem.practicas.dto.ResultadosValidacionDto.RespuestaRubricaDto;
import cl.ubb.pem.practicas.dto.ResultadosValidacionDto.RespuestaSusDto;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.RespuestaRubricaCriterio;
import cl.ubb.pem.practicas.entity.RespuestaRubricaExperto;
import cl.ubb.pem.practicas.entity.RespuestaSus;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import cl.ubb.pem.practicas.repository.RespuestaRubricaExpertoRepository;
import cl.ubb.pem.practicas.repository.RespuestaSusRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-30, criterio 2: la Escala SUS y la rúbrica de juicio experto de la propuesta RetroPlani Mate
 * ({@link InstrumentosValidacion}) se responden dentro del sistema (reemplaza a S-71, que las dejaba fuera).
 * - Responde solo un estudiante marcado como validador (403 el resto), una sola vez cada instrumento (409), con la
 *   validación abierta o cerrada: el coordinador puede cerrarla antes de que todos respondan. SUPUESTO S-75.
 * - El coordinador ve los resultados: promedios y cada respuesta con quién y cuándo respondió.
 */
@Service
public class RespuestasValidacionService {

    private final EstudianteRepository estudiantes;
    private final RespuestaSusRepository sus;
    private final RespuestaRubricaExpertoRepository rubricas;

    public RespuestasValidacionService(EstudianteRepository estudiantes, RespuestaSusRepository sus,
                                       RespuestaRubricaExpertoRepository rubricas) {
        this.estudiantes = estudiantes;
        this.sus = sus;
        this.rubricas = rubricas;
    }

    public static InstrumentosValidacionDto instrumentos() {
        return InstrumentosValidacion.DTO;
    }

    @Transactional(readOnly = true)
    public MiValidacionDto miEstado(UsuarioAutenticado actor) {
        return estadoDe(estudianteDe(actor));
    }

    @Transactional
    public MiValidacionDto responderSus(RespuestaSusRequest req, UsuarioAutenticado actor) {
        Estudiante e = validadorQueResponde(actor);
        if (sus.findByValidadorId(e.getId()).isPresent()) {
            throw new ConflictoException("Ya respondiste la Escala SUS: se responde una sola vez.");
        }
        RespuestaSus r = new RespuestaSus();
        r.setValidador(e);
        r.setItems(req.items());
        sus.save(r);
        return estadoDe(e);
    }

    @Transactional
    public MiValidacionDto responderRubrica(RespuestaRubricaRequest req, UsuarioAutenticado actor) {
        Estudiante e = validadorQueResponde(actor);
        if (rubricas.findByValidadorId(e.getId()).isPresent()) {
            throw new ConflictoException("Ya respondiste la rúbrica de juicio experto: se responde una sola vez.");
        }
        RespuestaRubricaExperto r = new RespuestaRubricaExperto();
        r.setValidador(e);
        r.setFortalezas(textoOpcional(req.fortalezas()));
        r.setDebilidades(textoOpcional(req.debilidades()));
        r.setMejoras(textoOpcional(req.mejoras()));
        r.setRecomendacion(textoOpcional(req.recomendacion()));
        for (int n = 0; n < req.criterios().size(); n++) {
            RespuestaRubricaCriterio c = new RespuestaRubricaCriterio();
            c.setRespuesta(r);
            c.setNumero(n + 1);
            c.setPuntaje(req.criterios().get(n).puntaje());
            c.setComentario(textoOpcional(req.criterios().get(n).comentario()));
            r.getCriterios().add(c);
        }
        rubricas.save(r);
        return estadoDe(e);
    }

    @Transactional(readOnly = true)
    public ResultadosValidacionDto resultados() {
        List<RespuestaSus> todasSus = sus.findAllByOrderByFechaAscIdAsc();
        List<RespuestaRubricaExperto> todasRubricas = rubricas.findAllByOrderByFechaAscIdAsc();
        List<BigDecimal> porItem = new ArrayList<>();
        for (int i = 0; i < InstrumentosValidacion.ITEMS_SUS.size(); i++) {
            int item = i;
            porItem.add(promedio(todasSus, r -> r.getItems().get(item)));
        }
        List<BigDecimal> porCriterio = IntStream.rangeClosed(1, InstrumentosValidacion.CRITERIOS.size())
                .mapToObj(n -> promedio(todasRubricas.stream()
                        .flatMap(r -> r.getCriterios().stream()).filter(c -> c.getNumero() == n).toList(),
                        RespuestaRubricaCriterio::getPuntaje))
                .toList();
        BigDecimal promedioSus = todasSus.isEmpty() ? null : todasSus.stream().map(RespuestaSus::getPuntaje)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(todasSus.size()), 2, RoundingMode.HALF_UP);
        return new ResultadosValidacionDto(instrumentos(),
                new ResultadosValidacionDto.ResumenSus(todasSus.size(), promedioSus, porItem,
                        todasSus.stream().map(RespuestaSusDto::desde).toList()),
                new ResultadosValidacionDto.ResumenRubrica(todasRubricas.size(), porCriterio,
                        todasRubricas.stream().map(RespuestaRubricaDto::desde).toList()));
    }

    // ---------------------------------------------------------------------------------------------

    private MiValidacionDto estadoDe(Estudiante e) {
        return new MiValidacionDto(Boolean.TRUE.equals(e.getValidadorChatbot()),
                sus.findByValidadorId(e.getId()).map(RespuestaSus::getFecha).orElse(null),
                rubricas.findByValidadorId(e.getId()).map(RespuestaRubricaExperto::getFecha).orElse(null),
                instrumentos());
    }

    private Estudiante estudianteDe(UsuarioAutenticado actor) {
        return estudiantes.findByUsuarioId(actor.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tu cuenta no tiene ficha de estudiante."));
    }

    private Estudiante validadorQueResponde(UsuarioAutenticado actor) {
        Estudiante e = estudianteDe(actor);
        if (!Boolean.TRUE.equals(e.getValidadorChatbot())) {
            throw new AccesoDenegadoException(
                    "Solo los estudiantes que el coordinador marcó como validadores responden la validación del asistente.");
        }
        return e;
    }

    private static <T> BigDecimal promedio(List<T> filas, ToIntFunction<T> valor) {
        if (filas.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(filas.stream().mapToInt(valor).sum())
                .divide(BigDecimal.valueOf(filas.size()), 2, RoundingMode.HALF_UP);
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
