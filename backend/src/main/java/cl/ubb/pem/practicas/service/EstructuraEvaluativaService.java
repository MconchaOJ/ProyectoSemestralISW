package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EstructuraEvaluativaDto;
import cl.ubb.pem.practicas.dto.EstructuraGuardarRequest;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.ComponenteEvaluativo;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.entity.TipoComponente;
import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.ComponenteEvaluativoRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import cl.ubb.pem.practicas.repository.NotaComponenteExternoRepository;
import cl.ubb.pem.practicas.repository.PautaRepository;
import cl.ubb.pem.practicas.security.Roles;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-09: la estructura evaluativa de cada asignatura (componentes y su peso en la nota final).
 *
 * Dos tipos de componente: PAUTA (la nota sale de las evaluaciones finalizadas con esa pauta) y EXTERNO (la escribe
 * el profesor del curso, porque lo califica otro docente fuera del sistema). Los porcentajes de los dos tipos juntos
 * suman 100. SUPUESTO S-83: la editan, cuando quieran (también a mitad de semestre), el coordinador y cualquier
 * profesor del curso que tenga al menos una inscripción en esa asignatura. Quitar un componente externo borra las
 * notas que ya tenía; la nota final ya guardada de una inscripción cerrada no cambia.
 */
@Service
public class EstructuraEvaluativaService {

    private static final Logger LOG = LoggerFactory.getLogger(EstructuraEvaluativaService.class);

    private final AsignaturaPracticaRepository asignaturas;
    private final ComponenteEvaluativoRepository componentes;
    private final NotaComponenteExternoRepository notasExternas;
    private final PautaRepository pautas;
    private final InscripcionRepository inscripciones;

    public EstructuraEvaluativaService(AsignaturaPracticaRepository asignaturas, ComponenteEvaluativoRepository componentes,
                                       NotaComponenteExternoRepository notasExternas, PautaRepository pautas,
                                       InscripcionRepository inscripciones) {
        this.asignaturas = asignaturas;
        this.componentes = componentes;
        this.notasExternas = notasExternas;
        this.pautas = pautas;
        this.inscripciones = inscripciones;
    }

    @Transactional(readOnly = true)
    public EstructuraEvaluativaDto obtener(Integer asignaturaId, UsuarioAutenticado actor) {
        return dto(buscarAsignatura(asignaturaId), actor);
    }

    /** El coordinador, o un profesor del curso con alguna inscripción en la asignatura (S-83). */
    public boolean puedeEditar(AsignaturaPractica a, UsuarioAutenticado actor) {
        return actor.tieneRol(Roles.COORDINADOR) || (actor.tieneRol(Roles.PROFESOR_CURSO)
                && inscripciones.existsByAsignaturaIdAndProfesorCursoId(a.getId(), actor.id()));
    }

    @Transactional
    public EstructuraEvaluativaDto guardar(Integer asignaturaId, EstructuraGuardarRequest r, UsuarioAutenticado actor) {
        AsignaturaPractica a = buscarAsignatura(asignaturaId);
        if (!puedeEditar(a, actor)) {
            throw new AccesoDenegadoException("Solo el coordinador o un profesor del curso de " + a.getNombre()
                    + " puede cambiar su estructura evaluativa.");
        }
        Map<Integer, ComponenteEvaluativo> actuales = componentes.findByAsignaturaIdOrderByOrdenAsc(asignaturaId).stream()
                .collect(Collectors.toMap(ComponenteEvaluativo::getId, Function.identity()));
        List<EstructuraGuardarRequest.Componente> pedidos = r.componentes();

        // 1. Validar todo antes de cambiar nada
        Set<Integer> idsPedidos = new HashSet<>();
        Set<Integer> pautasPedidas = new HashSet<>();
        Pauta[] pautaDe = new Pauta[pedidos.size()];
        int suma = 0;
        int sumaPautas = 0;
        for (int i = 0; i < pedidos.size(); i++) {
            var c = pedidos.get(i);
            String donde = "Componente " + (i + 1) + ": ";
            if (c.id() != null) {
                ComponenteEvaluativo existente = actuales.get(c.id());
                if (existente == null) {
                    throw new ReglaNegocioException(donde + "el id " + c.id() + " no es un componente de " + a.getNombre() + ".");
                }
                if (!idsPedidos.add(c.id())) {
                    throw new ReglaNegocioException(donde + "el id " + c.id() + " viene repetido.");
                }
                if (existente.getTipo() != c.tipo()) {
                    throw new ReglaNegocioException(donde + "un componente no cambia de tipo: quítalo y agrega uno nuevo.");
                }
            }
            if (c.tipo() == TipoComponente.PAUTA) {
                pautaDe[i] = pautaValida(c.pautaId(), a, donde);
                if (!pautasPedidas.add(pautaDe[i].getId())) {
                    throw new ReglaNegocioException(donde + "la pauta «" + pautaDe[i].getNombre() + "» ya está en otro componente.");
                }
                sumaPautas += c.porcentaje();
            } else {
                if (c.pautaId() != null) {
                    throw new ReglaNegocioException(donde + "un componente externo no lleva pauta.");
                }
                if (c.nombre() == null || c.nombre().isBlank()) {
                    throw new ReglaNegocioException(donde
                            + "escribe el nombre del componente externo (por ejemplo «Producto 3: Informe final»).");
                }
            }
            suma += c.porcentaje();
        }
        if (!pedidos.isEmpty() && suma != 100) {
            throw new ReglaNegocioException("Los porcentajes suman " + suma + " % (pautas " + sumaPautas + " % y externos "
                    + (suma - sumaPautas) + " %): entre todos deben sumar 100 %.");
        }

        // 2. Quitar los que ya no vienen (con sus notas externas) y guardar los demás en el orden pedido
        for (ComponenteEvaluativo c : actuales.values()) {
            if (!idsPedidos.contains(c.getId())) {
                notasExternas.deleteByComponenteId(c.getId());
                componentes.delete(c);
            }
        }
        componentes.flush();
        for (int i = 0; i < pedidos.size(); i++) {
            var c = pedidos.get(i);
            ComponenteEvaluativo comp = c.id() != null ? actuales.get(c.id()) : new ComponenteEvaluativo();
            comp.setAsignatura(a);
            comp.setTipo(c.tipo());
            comp.setPauta(pautaDe[i]);
            comp.setNombre(nombre(c, pautaDe[i]));
            comp.setPorcentaje(c.porcentaje());
            comp.setOrden(i + 1);
            componentes.save(comp);
        }
        componentes.flush();
        LOG.info("El usuario {} guardó la estructura evaluativa de la asignatura {} ({} componentes)",
                actor.id(), asignaturaId, pedidos.size());
        return dto(a, actor);
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private Pauta pautaValida(Integer pautaId, AsignaturaPractica a, String donde) {
        if (pautaId == null) {
            throw new ReglaNegocioException(donde + "elige la pauta del componente.");
        }
        Pauta p = pautas.findById(pautaId)
                .orElseThrow(() -> new ReglaNegocioException(donde + "no existe la pauta con id " + pautaId + "."));
        if (!p.getAsignatura().getId().equals(a.getId())) {
            throw new ReglaNegocioException(donde + "la pauta «" + p.getNombre() + "» no es de " + a.getNombre() + ".");
        }
        if (pautas.findByVersionAnteriorId(p.getId()).isPresent()) {
            throw new ReglaNegocioException(donde + "la pauta «" + p.getNombre() + "» (versión " + p.getVersion()
                    + ") ya fue reemplazada: elige su versión vigente.");
        }
        return p;
    }

    private static String nombre(EstructuraGuardarRequest.Componente c, Pauta p) {
        String n = (c.nombre() == null || c.nombre().isBlank() ? p.getNombre() : c.nombre()).trim();
        return n.length() > 150 ? n.substring(0, 150) : n;
    }

    private AsignaturaPractica buscarAsignatura(Integer id) {
        return asignaturas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la asignatura con id " + id + "."));
    }

    private EstructuraEvaluativaDto dto(AsignaturaPractica a, UsuarioAutenticado actor) {
        List<EstructuraEvaluativaDto.ComponenteDto> lista = componentes.findByAsignaturaIdOrderByOrdenAsc(a.getId()).stream()
                .map(c -> new EstructuraEvaluativaDto.ComponenteDto(c.getId(), c.getTipo(), c.getNombre(),
                        c.getPauta() == null ? null : c.getPauta().getId(),
                        c.getPauta() == null ? null : c.getPauta().getNombre(),
                        c.getPauta() == null ? null : c.getPauta().getVersion(),
                        c.getPorcentaje(), c.getOrden(),
                        c.getTipo() == TipoComponente.EXTERNO ? notasExternas.countByComponenteId(c.getId()) : 0))
                .toList();
        int total = lista.stream().mapToInt(EstructuraEvaluativaDto.ComponenteDto::porcentaje).sum();
        int dePautas = lista.stream().filter(c -> c.tipo() == TipoComponente.PAUTA)
                .mapToInt(EstructuraEvaluativaDto.ComponenteDto::porcentaje).sum();
        List<Pauta> todas = pautas.findByAsignaturaIdOrderByNombreAscVersionAsc(a.getId());
        Set<Integer> reemplazadas = todas.stream().map(Pauta::getVersionAnterior).filter(Objects::nonNull)
                .map(Pauta::getId).collect(Collectors.toSet());
        List<EstructuraEvaluativaDto.PautaOpcion> vigentes = todas.stream().filter(p -> !reemplazadas.contains(p.getId()))
                .map(p -> new EstructuraEvaluativaDto.PautaOpcion(p.getId(), p.getNombre(), p.getVersion(),
                        Boolean.TRUE.equals(p.getActiva())))
                .toList();
        return new EstructuraEvaluativaDto(a.getId(), a.getNombre(), lista, total, dePautas, total - dePautas,
                puedeEditar(a, actor), vigentes);
    }
}
