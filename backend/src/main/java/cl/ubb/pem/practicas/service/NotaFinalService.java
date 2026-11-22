package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EstudianteResumenDto;
import cl.ubb.pem.practicas.dto.NotaFinalDto;
import cl.ubb.pem.practicas.dto.NotasAsignaturaDto;
import cl.ubb.pem.practicas.dto.UsuarioResumenDto;
import cl.ubb.pem.practicas.entity.ComponenteEvaluativo;
import cl.ubb.pem.practicas.entity.EstadoEvaluacion;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.NotaComponenteExterno;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.TipoComponente;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.ComponenteEvaluativoRepository;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.NotaComponenteExternoRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-09: nota final de una inscripción con la estructura evaluativa de su asignatura.
 *
 * - Componente de PAUTA: promedio de las notas de las evaluaciones FINALIZADAS de la inscripción hechas con esa pauta
 *   o con una versión anterior de ella (cada evaluación guarda su nota al finalizar, S-81).
 * - Componente EXTERNO: la nota que escribió el profesor del curso (o el coordinador como respaldo).
 * - SUPUESTO S-84: la nota de cada componente se redondea a un decimal, y la parcial y la final se calculan con esas
 *   notas redondeadas (así se pueden repetir a mano). Mientras falte alguna, la nota parcial se recalcula solo con lo
 *   que ya tiene nota ("parcial: 26 % evaluado"); la nota final (propuesta) aparece cuando todo el 100 % tiene nota.
 * - Al cerrar la inscripción, la pantalla propone esa nota y el coordinador la confirma o la ajusta: la que se
 *   guarda en inscripcion.nota_final es la que él confirma (PUT /api/inscripciones/{id}/cierre).
 * - SUPUESTO S-85: las notas externas las escriben el profesor del curso de ESA inscripción y el coordinador, solo con
 *   la inscripción EN_CURSO (409 si está cerrada).
 */
@Service
public class NotaFinalService {

    private static final Pattern PERIODO = Pattern.compile("\\d{4}-[12]");

    private final InscripcionRepository inscripciones;
    private final AsignaturaPracticaRepository asignaturas;
    private final ComponenteEvaluativoRepository componentes;
    private final NotaComponenteExternoRepository notasExternas;
    private final EvaluacionRepository evaluaciones;
    private final UsuarioRepository usuarios;
    private final AccesoService acceso;
    private final EstructuraEvaluativaService estructura;
    private final Clock reloj;

    public NotaFinalService(InscripcionRepository inscripciones, AsignaturaPracticaRepository asignaturas,
                            ComponenteEvaluativoRepository componentes, NotaComponenteExternoRepository notasExternas,
                            EvaluacionRepository evaluaciones, UsuarioRepository usuarios, AccesoService acceso,
                            EstructuraEvaluativaService estructura, Clock reloj) {
        this.inscripciones = inscripciones;
        this.asignaturas = asignaturas;
        this.componentes = componentes;
        this.notasExternas = notasExternas;
        this.evaluaciones = evaluaciones;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.estructura = estructura;
        this.reloj = reloj;
    }

    /** Quien ya puede ver la inscripción (también el estudiante, que ve su nota parcial). */
    @Transactional(readOnly = true)
    public NotaFinalDto deInscripcion(Long inscripcionId, UsuarioAutenticado actor) {
        Inscripcion i = buscar(inscripcionId);
        acceso.exigirVerInscripcion(i, actor);
        return calcular(List.of(i), actor).get(0);
    }

    /** Las inscripciones de una asignatura y período que ve quien consulta, cada una con su nota. */
    @Transactional(readOnly = true)
    public NotasAsignaturaDto deAsignatura(Integer asignaturaId, String periodo, UsuarioAutenticado actor) {
        if (periodo == null || !PERIODO.matcher(periodo).matches()) {
            throw new ReglaNegocioException("El período debe tener el formato AAAA-S, por ejemplo 2026-2.");
        }
        if (!asignaturas.existsById(asignaturaId)) {
            throw new RecursoNoEncontradoException("No existe la asignatura con id " + asignaturaId + ".");
        }
        AccesoService.Alcance alcance = acceso.alcanceDe(actor);
        List<Inscripcion> lista = inscripciones.buscar(null, asignaturaId, periodo, alcance.guiaId(), alcance.cursoId(),
                        alcance.usuarioEstudianteId()).stream()
                .filter(i -> acceso.puedeVerInscripcion(i, actor))
                .sorted(Comparator.comparing((Inscripcion i) -> i.getEstudiante().getUsuario().getApellidos())
                        .thenComparing(i -> i.getEstudiante().getUsuario().getNombre()))
                .toList();
        return new NotasAsignaturaDto(estructura.obtener(asignaturaId, actor), periodo, calcular(lista, actor));
    }

    @Transactional
    public NotaFinalDto registrarNotaExterna(Long inscripcionId, Integer componenteId, BigDecimal nota,
                                             UsuarioAutenticado actor) {
        Inscripcion i = buscar(inscripcionId);
        ComponenteEvaluativo c = componenteExternoDe(i, componenteId, actor);
        NotaComponenteExterno n = notasExternas.findByInscripcionIdAndComponenteId(inscripcionId, componenteId)
                .orElseGet(NotaComponenteExterno::new);
        n.setInscripcion(i);
        n.setComponente(c);
        n.setNota(nota.setScale(1, RoundingMode.HALF_UP));
        n.setRegistradaPor(usuarios.getReferenceById(actor.id()));
        n.setFechaRegistro(LocalDateTime.now(reloj));
        notasExternas.saveAndFlush(n);
        return calcular(List.of(i), actor).get(0);
    }

    @Transactional
    public NotaFinalDto borrarNotaExterna(Long inscripcionId, Integer componenteId, UsuarioAutenticado actor) {
        Inscripcion i = buscar(inscripcionId);
        componenteExternoDe(i, componenteId, actor);
        notasExternas.findByInscripcionIdAndComponenteId(inscripcionId, componenteId).ifPresent(notasExternas::delete);
        notasExternas.flush();
        return calcular(List.of(i), actor).get(0);
    }

    // ------------------------------------------------------------------------------------------ cálculo

    private List<NotaFinalDto> calcular(List<Inscripcion> lista, UsuarioAutenticado actor) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(Inscripcion::getId).toList();
        Map<Long, List<Evaluacion>> evaluacionesDe = evaluaciones.findPorInscripciones(ids, EstadoEvaluacion.FINALIZADA)
                .stream().collect(Collectors.groupingBy(e -> e.getInscripcion().getId()));
        Map<Long, Map<Integer, NotaComponenteExterno>> externasDe = new HashMap<>();
        for (NotaComponenteExterno n : notasExternas.findByInscripcionIdIn(ids)) {
            externasDe.computeIfAbsent(n.getInscripcion().getId(), k -> new HashMap<>()).put(n.getComponente().getId(), n);
        }
        Map<Integer, List<ComponenteEvaluativo>> estructuraDe = new HashMap<>();
        List<NotaFinalDto> resultado = new ArrayList<>();
        for (Inscripcion i : lista) {
            List<ComponenteEvaluativo> comps = estructuraDe.computeIfAbsent(i.getAsignatura().getId(),
                    componentes::findByAsignaturaIdOrderByOrdenAsc);
            resultado.add(calcular(i, comps, evaluacionesDe.getOrDefault(i.getId(), List.of()),
                    externasDe.getOrDefault(i.getId(), Map.of()), actor));
        }
        return resultado;
    }

    private NotaFinalDto calcular(Inscripcion i, List<ComponenteEvaluativo> comps, List<Evaluacion> finalizadas,
                                  Map<Integer, NotaComponenteExterno> externas, UsuarioAutenticado actor) {
        List<NotaFinalDto.ComponenteNota> notas = new ArrayList<>();
        int evaluado = 0;
        int total = 0;
        BigDecimal suma = BigDecimal.ZERO;
        for (ComponenteEvaluativo c : comps) {
            BigDecimal nota = null;
            int cuantas = 0;
            UsuarioResumenDto quien = null;
            LocalDateTime cuando = null;
            if (c.getTipo() == TipoComponente.PAUTA) {
                Set<Integer> familia = familia(c.getPauta());
                List<BigDecimal> deLaPauta = finalizadas.stream()
                        .filter(e -> familia.contains(e.getPauta().getId()) && e.getNota() != null)
                        .map(Evaluacion::getNota).toList();
                cuantas = deLaPauta.size();
                nota = deLaPauta.isEmpty() ? null : EscalaNotas.promedio(deLaPauta);
            } else {
                NotaComponenteExterno n = externas.get(c.getId());
                if (n != null) {
                    nota = n.getNota();
                    quien = UsuarioResumenDto.desde(n.getRegistradaPor());
                    cuando = n.getFechaRegistro();
                }
            }
            total += c.getPorcentaje();
            if (nota != null) {
                evaluado += c.getPorcentaje();
                suma = suma.add(nota.multiply(BigDecimal.valueOf(c.getPorcentaje())));
            }
            notas.add(new NotaFinalDto.ComponenteNota(c.getId(), c.getTipo(), c.getNombre(), c.getPorcentaje(), nota,
                    cuantas, quien, cuando));
        }
        BigDecimal parcial = evaluado == 0 ? null
                : EscalaNotas.redondear(suma.divide(BigDecimal.valueOf(evaluado), MathContext.DECIMAL64));
        boolean completa = !comps.isEmpty() && evaluado == total;
        return new NotaFinalDto(i.getId(), EstudianteResumenDto.desde(i.getEstudiante()), i.getAsignatura().getId(),
                i.getAsignatura().getNombre(), i.getPeriodo(), i.getEstado(), i.getNotaFinal(), !comps.isEmpty(), notas,
                evaluado, parcial, completa, completa ? parcial : null, puedeEditarNotas(i, actor));
    }

    /** La pauta y todas sus versiones anteriores: las evaluaciones hechas con una versión antigua también cuentan. */
    private static Set<Integer> familia(Pauta p) {
        Set<Integer> ids = new HashSet<>();
        for (Pauta v = p; v != null; v = v.getVersionAnterior()) {
            ids.add(v.getId());
        }
        return ids;
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private static boolean puedeEditarNotas(Inscripcion i, UsuarioAutenticado actor) {
        return i.getEstado() == EstadoInscripcion.EN_CURSO && escribeNotas(i, actor);
    }

    private static boolean escribeNotas(Inscripcion i, UsuarioAutenticado actor) {
        return actor.tieneRol(Roles.COORDINADOR)
                || (actor.tieneRol(Roles.PROFESOR_CURSO) && i.getProfesorCurso().getId().equals(actor.id()));
    }

    private ComponenteEvaluativo componenteExternoDe(Inscripcion i, Integer componenteId, UsuarioAutenticado actor) {
        ComponenteEvaluativo c = componentes.findById(componenteId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el componente con id " + componenteId + "."));
        if (!c.getAsignatura().getId().equals(i.getAsignatura().getId())) {
            throw new ReglaNegocioException("El componente «" + c.getNombre() + "» no es de "
                    + i.getAsignatura().getNombre() + ".");
        }
        if (c.getTipo() != TipoComponente.EXTERNO) {
            throw new ReglaNegocioException("La nota de «" + c.getNombre()
                    + "» sale de sus evaluaciones con pauta: no se escribe a mano.");
        }
        if (!escribeNotas(i, actor)) {
            throw new AccesoDenegadoException("Solo el profesor del curso de esta inscripción o el coordinador escriben sus notas.");
        }
        if (i.getEstado() != EstadoInscripcion.EN_CURSO) {
            throw new ConflictoException("La inscripción ya está cerrada (" + i.getEstado() + "): sus notas no se cambian.");
        }
        return c;
    }

    private Inscripcion buscar(Long id) {
        return inscripciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la inscripción con id " + id + "."));
    }
}
