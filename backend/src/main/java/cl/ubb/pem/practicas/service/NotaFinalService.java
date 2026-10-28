package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.ComponenteNotaDto;
import cl.ubb.pem.practicas.dto.EstructuraEvaluativaDto;
import cl.ubb.pem.practicas.dto.EstructuraEvaluativaRequest;
import cl.ubb.pem.practicas.dto.NotaFinalDto;
import cl.ubb.pem.practicas.dto.NotaFinalDto.LineaDto;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.ComponenteNota;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.NotaComponente;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.ComponenteNotaRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.NotaComponenteRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-09: nota final ponderada. El coordinador define la estructura evaluativa de cada asignatura (componentes y
 * pesos que suman 100, criterio 1); el profesor del curso de la inscripción registra la nota de cada componente y
 * el sistema recalcula y guarda la nota ponderada en la inscripción ({@code nota_calculada}, criterio 2). La nota de
 * cierre ({@code nota_final}) la sigue poniendo el coordinador al cerrar (S-22): la calculada es su referencia.
 * Escala 1,0 a 7,0 y redondeo a un decimal hacia arriba desde 0,05: SUPUESTO S-78 (criterio 3, sin confirmar).
 */
@Service
public class NotaFinalService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final AsignaturaPracticaRepository asignaturas;
    private final ComponenteNotaRepository componentes;
    private final NotaComponenteRepository notas;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final AccesoService acceso;
    private final Clock reloj;

    public NotaFinalService(AsignaturaPracticaRepository asignaturas, ComponenteNotaRepository componentes,
                            NotaComponenteRepository notas, InscripcionRepository inscripciones,
                            UsuarioRepository usuarios, AccesoService acceso, Clock reloj) {
        this.asignaturas = asignaturas;
        this.componentes = componentes;
        this.notas = notas;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.reloj = reloj;
    }

    // ---------- Criterio 1: estructura evaluativa por asignatura ----------

    @Transactional(readOnly = true)
    public EstructuraEvaluativaDto estructura(Integer asignaturaId) {
        return estructuraDto(buscarAsignatura(asignaturaId));
    }

    /**
     * Reemplaza la estructura de la asignatura (ver {@link EstructuraEvaluativaRequest}). Después recalcula la nota
     * de las inscripciones EN_CURSO; las cerradas conservan la que tenían.
     */
    @Transactional
    public EstructuraEvaluativaDto guardarEstructura(Integer asignaturaId, EstructuraEvaluativaRequest r) {
        AsignaturaPractica asignatura = buscarAsignatura(asignaturaId);
        List<EstructuraEvaluativaRequest.Componente> pedidos = r.componentes();
        validarEstructura(pedidos);

        Map<Integer, ComponenteNota> actuales = componentes.findByAsignaturaIdOrderByOrdenAscIdAsc(asignaturaId).stream()
                .collect(Collectors.toMap(ComponenteNota::getId, Function.identity()));
        Set<Integer> conservados = new HashSet<>();
        for (EstructuraEvaluativaRequest.Componente p : pedidos) {
            if (p.id() != null) {
                if (!actuales.containsKey(p.id())) {
                    throw new ReglaNegocioException("El componente " + p.id() + " no es de esta asignatura.");
                }
                if (!conservados.add(p.id())) {
                    throw new ReglaNegocioException("El componente " + p.id() + " viene repetido.");
                }
            }
        }
        // Primero se borran los que no vienen (así un nombre puede pasar de uno borrado a uno nuevo)
        for (ComponenteNota c : actuales.values()) {
            if (!conservados.contains(c.getId())) {
                if (notas.existsByComponenteId(c.getId())) {
                    throw new ConflictoException("El componente «" + c.getNombre() + "» ya tiene notas registradas: "
                            + "no se puede quitar (sí cambiar su nombre o su peso).");
                }
                componentes.delete(c);
            }
        }
        componentes.flush();
        int orden = 1;
        for (EstructuraEvaluativaRequest.Componente p : pedidos) {
            ComponenteNota c = p.id() == null ? new ComponenteNota() : actuales.get(p.id());
            c.setAsignatura(asignatura);
            c.setNombre(p.nombre().strip());
            c.setPeso(p.peso());
            c.setOrden(orden++);
            componentes.save(c);
        }
        componentes.flush();
        List<ComponenteNota> nuevos = componentes.findByAsignaturaIdOrderByOrdenAscIdAsc(asignaturaId);
        for (Inscripcion i : inscripciones.findByAsignaturaIdAndEstado(asignaturaId, EstadoInscripcion.EN_CURSO)) {
            i.setNotaCalculada(calcular(nuevos, notasPorComponente(i.getId())));
        }
        return estructuraDto(asignatura);
    }

    private static void validarEstructura(List<EstructuraEvaluativaRequest.Componente> pedidos) {
        Set<String> nombres = new HashSet<>();
        for (EstructuraEvaluativaRequest.Componente p : pedidos) {
            if (!nombres.add(p.nombre().strip().toLowerCase(Locale.ROOT))) {
                throw new ReglaNegocioException("Hay dos componentes llamados «" + p.nombre().strip() + "».");
            }
        }
        BigDecimal suma = pedidos.stream().map(EstructuraEvaluativaRequest.Componente::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!pedidos.isEmpty() && suma.compareTo(CIEN) != 0) {
            throw new ReglaNegocioException("Los pesos deben sumar 100 % y suman " + suma.stripTrailingZeros()
                    .toPlainString() + " %.");
        }
    }

    private EstructuraEvaluativaDto estructuraDto(AsignaturaPractica a) {
        List<ComponenteNota> lista = componentes.findByAsignaturaIdOrderByOrdenAscIdAsc(a.getId());
        BigDecimal suma = sumaPesos(lista);
        return new EstructuraEvaluativaDto(a.getId(), a.getNombre(), lista.stream().map(ComponenteNotaDto::desde).toList(),
                suma, completa(lista));
    }

    // ---------- Criterio 2: notas por componente y nota calculada en la inscripción ----------

    @Transactional(readOnly = true)
    public NotaFinalDto notaFinal(Long inscripcionId, UsuarioAutenticado actor) {
        Inscripcion i = buscarInscripcion(inscripcionId);
        acceso.exigirVerInscripcion(i, actor);
        return notaFinalDto(i, actor);
    }

    /**
     * Registra o corrige la nota de un componente. Solo el profesor del curso de esa inscripción (403 para cualquier
     * otro), con la inscripción EN_CURSO (409). Recalcula la nota de la inscripción en el mismo paso.
     */
    @Transactional
    public NotaFinalDto registrarNota(Long inscripcionId, Integer componenteId, BigDecimal nota,
                                      UsuarioAutenticado actor) {
        Inscripcion i = buscarInscripcion(inscripcionId);
        if (!esProfesorDelCurso(i, actor)) {
            throw new AccesoDenegadoException("Solo el profesor del curso de esta inscripción registra sus notas.");
        }
        if (i.getEstado() != EstadoInscripcion.EN_CURSO) {
            throw new ConflictoException("La inscripción ya está cerrada (" + i.getEstado() + "): sus notas no cambian.");
        }
        ComponenteNota c = componentes.findById(componenteId)
                .filter(x -> x.getAsignatura().getId().equals(i.getAsignatura().getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("La asignatura de esta inscripción no tiene el "
                        + "componente " + componenteId + "."));
        NotaComponente n = notas.findByInscripcionIdAndComponenteId(inscripcionId, componenteId).orElseGet(() -> {
            NotaComponente nueva = new NotaComponente();
            nueva.setInscripcion(i);
            nueva.setComponente(c);
            return nueva;
        });
        n.setNota(nota);
        n.setRegistradaPor(usuarios.getReferenceById(actor.id()));
        n.setFechaRegistro(LocalDateTime.now(reloj));
        notas.save(n);
        notas.flush();
        i.setNotaCalculada(calcular(componentes.findByAsignaturaIdOrderByOrdenAscIdAsc(i.getAsignatura().getId()),
                notasPorComponente(i.getId())));
        return notaFinalDto(i, actor);
    }

    private NotaFinalDto notaFinalDto(Inscripcion i, UsuarioAutenticado actor) {
        List<ComponenteNota> lista = componentes.findByAsignaturaIdOrderByOrdenAscIdAsc(i.getAsignatura().getId());
        Map<Integer, NotaComponente> registradas = notas.findByInscripcionId(i.getId()).stream()
                .collect(Collectors.toMap(n -> n.getComponente().getId(), Function.identity()));
        List<LineaDto> lineas = lista.stream().map(c -> {
            NotaComponente n = registradas.get(c.getId());
            return n == null ? new LineaDto(c.getId(), c.getNombre(), c.getPeso(), null, null, null, null)
                    : new LineaDto(c.getId(), c.getNombre(), c.getPeso(), n.getNota(),
                            n.getNota().multiply(c.getPeso()).divide(CIEN, 2, RoundingMode.HALF_UP),
                            n.getRegistradaPor().getNombreCompleto(), n.getFechaRegistro());
        }).toList();
        int conNota = (int) lineas.stream().filter(l -> l.nota() != null).count();
        boolean puede = esProfesorDelCurso(i, actor) && i.getEstado() == EstadoInscripcion.EN_CURSO;
        return new NotaFinalDto(i.getId(), i.getEstado(), lineas, sumaPesos(lista), completa(lista), conNota,
                i.getNotaCalculada(), i.getNotaFinal(), puede);
    }

    private Map<Integer, BigDecimal> notasPorComponente(Long inscripcionId) {
        return notas.findByInscripcionId(inscripcionId).stream()
                .collect(Collectors.toMap(n -> n.getComponente().getId(), NotaComponente::getNota));
    }

    /**
     * Σ nota × peso / 100, redondeada a un decimal (5,881 → 5,9; 3,95 → 4,0). Nula si la estructura está vacía, si
     * los pesos no suman 100 o si falta la nota de algún componente.
     */
    static BigDecimal calcular(List<ComponenteNota> estructura, Map<Integer, BigDecimal> notasPorComponente) {
        if (!completa(estructura)) {
            return null;
        }
        BigDecimal suma = BigDecimal.ZERO;
        for (ComponenteNota c : estructura) {
            BigDecimal nota = notasPorComponente.get(c.getId());
            if (nota == null) {
                return null;
            }
            suma = suma.add(nota.multiply(c.getPeso()));
        }
        return suma.divide(CIEN, 1, RoundingMode.HALF_UP);
    }

    private static boolean completa(List<ComponenteNota> estructura) {
        return !estructura.isEmpty() && sumaPesos(estructura).compareTo(CIEN) == 0;
    }

    private static BigDecimal sumaPesos(List<ComponenteNota> estructura) {
        return estructura.stream().map(ComponenteNota::getPeso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Escribir se comprueba por id (como evaluar o revisar): el profesor del curso de ESTA inscripción. */
    private static boolean esProfesorDelCurso(Inscripcion i, UsuarioAutenticado actor) {
        return actor.tieneRol(Roles.PROFESOR_CURSO) && Objects.equals(i.getProfesorCurso().getId(), actor.id());
    }

    private AsignaturaPractica buscarAsignatura(Integer id) {
        return asignaturas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la asignatura con id " + id + "."));
    }

    private Inscripcion buscarInscripcion(Long id) {
        return inscripciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la inscripción con id " + id + "."));
    }
}
