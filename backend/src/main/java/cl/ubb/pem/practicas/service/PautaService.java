package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.PautaDto;
import cl.ubb.pem.practicas.dto.PautaGestionDto;
import cl.ubb.pem.practicas.dto.PautaGuardarRequest;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.AmbitoPauta;
import cl.ubb.pem.practicas.entity.DimensionPauta;
import cl.ubb.pem.practicas.entity.Indicador;
import cl.ubb.pem.practicas.entity.Pauta;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.AsignaturaPracticaRepository;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.PautaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-03, criterio 1: consulta de las pautas con sus dimensiones e indicadores, en orden.
 * HU-06: el coordinador crea, edita, activa/desactiva y borra pautas (el rol lo filtra @PreAuthorize).
 *
 * Regla central de HU-06 (criterio 2): una pauta que ya se usó en alguna evaluación (borrador incluido) NO se
 * modifica. Editarla crea una VERSIÓN NUEVA (otra fila, version + 1, que apunta a la anterior); la anterior queda
 * inactiva e intacta, y sus evaluaciones conservan sus indicadores, su escala y su puntaje máximo. Una pauta sin
 * uso se edita en el mismo lugar. Solo se edita la versión vigente (la última).
 */
@Service
@Transactional(readOnly = true)
public class PautaService {

    private final PautaRepository pautas;
    private final AsignaturaPracticaRepository asignaturas;
    private final EvaluacionRepository evaluaciones;

    public PautaService(PautaRepository pautas, AsignaturaPracticaRepository asignaturas, EvaluacionRepository evaluaciones) {
        this.pautas = pautas;
        this.asignaturas = asignaturas;
        this.evaluaciones = evaluaciones;
    }

    public List<PautaDto> listar(Integer asignaturaId) {
        var lista = asignaturaId == null
                ? pautas.findByActivaTrueOrderByIdAsc()
                : pautas.findByAsignaturaIdAndActivaTrueOrderByIdAsc(asignaturaId);
        return lista.stream().map(PautaDto::desde).toList();
    }

    public PautaDto obtener(Integer id) {
        return PautaDto.desde(buscar(id));
    }

    // ------------------------------------------------------------------------------------------ HU-06

    /** Todas las pautas y todas sus versiones (activas o no), con cuántas evaluaciones usa cada una. */
    public List<PautaGestionDto> gestion(Integer asignaturaId) {
        var lista = asignaturaId == null
                ? pautas.findAllByOrderByAsignaturaOrdenAscNombreAscVersionAsc()
                : pautas.findByAsignaturaIdOrderByNombreAscVersionAsc(asignaturaId);
        return lista.stream()
                .map(p -> PautaGestionDto.desde(p, esVigente(p), evaluaciones.countByPautaId(p.getId())))
                .toList();
    }

    @Transactional
    public PautaDto crear(PautaGuardarRequest r) {
        validar(r);
        AsignaturaPractica asignatura = asignaturas.findById(r.asignaturaId()).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la asignatura con id " + r.asignaturaId() + "."));
        exigirNombreLibre(asignatura.getId(), r.nombre(), Set.of());
        Pauta p = new Pauta();
        p.setAsignatura(asignatura);
        p.setVersion(1);
        p.setActiva(true);
        llenar(p, r);
        return PautaDto.desde(pautas.save(p));
    }

    /**
     * Edita la versión vigente. Sin evaluaciones: se modifica en el mismo lugar. Con evaluaciones: se crea la versión
     * siguiente (con el mismo estado activa/inactiva) y la actual queda inactiva. Si no cambió nada, no se crea nada.
     */
    @Transactional
    public PautaDto actualizar(Integer id, PautaGuardarRequest r) {
        Pauta actual = buscar(id);
        exigirVigente(actual);
        validar(r);
        if (!actual.getAsignatura().getId().equals(r.asignaturaId())) {
            throw new ReglaNegocioException("La asignatura de una pauta no se cambia: crea una pauta nueva para la otra asignatura.");
        }
        exigirNombreLibre(r.asignaturaId(), r.nombre(), idsDeLaFamilia(actual));
        if (sinCambios(actual, r)) {
            return PautaDto.desde(actual);
        }
        if (evaluaciones.countByPautaId(actual.getId()) > 0) {
            Pauta nueva = new Pauta();
            nueva.setAsignatura(actual.getAsignatura());
            nueva.setVersion(actual.getVersion() + 1);
            nueva.setVersionAnterior(actual);
            nueva.setActiva(actual.getActiva());
            actual.setActiva(false);
            llenar(nueva, r);
            return PautaDto.desde(pautas.save(nueva));
        }
        // Sin uso: se reemplazan dimensiones e indicadores. Se vacían y se envían a la base ANTES de agregar los
        // nuevos, porque Hibernate inserta antes de borrar y chocarían las restricciones UNIQUE (pauta, orden).
        actual.getDimensiones().clear();
        pautas.flush();
        llenar(actual, r);
        return PautaDto.desde(pautas.save(actual));
    }

    /** Solo la versión vigente: activar una versión antigua dejaría dos versiones activas de la misma pauta. */
    @Transactional
    public PautaDto cambiarActiva(Integer id, boolean activa) {
        Pauta p = buscar(id);
        exigirVigente(p);
        p.setActiva(activa);
        return PautaDto.desde(p);
    }

    /**
     * Una pauta usada no se borra nunca (409): se desactiva o se edita (y se versiona). Borrar una versión 2 o
     * posterior sin uso deshace esa versión: la anterior vuelve a ser la vigente, con el mismo estado activa/inactiva.
     */
    @Transactional
    public void eliminar(Integer id) {
        Pauta p = buscar(id);
        exigirVigente(p);
        long usos = evaluaciones.countByPautaId(id);
        if (usos > 0) {
            throw new ConflictoException("La pauta está en uso en " + usos + (usos == 1 ? " evaluación" : " evaluaciones")
                    + ": no se borra. Puedes desactivarla, o editarla (se creará una versión nueva).");
        }
        Pauta anterior = p.getVersionAnterior();
        if (anterior != null) {
            anterior.setActiva(p.getActiva());
        }
        pautas.delete(p);
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private Pauta buscar(Integer id) {
        return pautas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la pauta con id " + id + "."));
    }

    private boolean esVigente(Pauta p) {
        return pautas.findByVersionAnteriorId(p.getId()).isEmpty();
    }

    private void exigirVigente(Pauta p) {
        pautas.findByVersionAnteriorId(p.getId()).ifPresent(siguiente -> {
            throw new ConflictoException("Esta es la versión " + p.getVersion() + " y ya fue reemplazada por la versión "
                    + siguiente.getVersion() + " (id " + siguiente.getId() + "): solo se modifica la versión vigente.");
        });
    }

    /** Ids de esta versión y de todas las anteriores (la misma pauta a lo largo del tiempo). */
    private static Set<Integer> idsDeLaFamilia(Pauta p) {
        Set<Integer> ids = new HashSet<>();
        for (Pauta v = p; v != null; v = v.getVersionAnterior()) {
            ids.add(v.getId());
        }
        return ids;
    }

    /** Dos pautas distintas de una misma asignatura no pueden llamarse igual (sin distinguir mayúsculas). */
    private void exigirNombreLibre(Integer asignaturaId, String nombre, Set<Integer> idsPropios) {
        boolean ocupado = pautas.findByAsignaturaIdAndNombreIgnoreCase(asignaturaId, nombre.trim()).stream()
                .anyMatch(otra -> !idsPropios.contains(otra.getId()));
        if (ocupado) {
            throw new ConflictoException("Ya existe otra pauta llamada \"" + nombre.trim() + "\" en esa asignatura.");
        }
    }

    private static void validar(PautaGuardarRequest r) {
        boolean deObservacionDeClase = r.ambito() == AmbitoPauta.OBSERVACION_CLASE;
        if (deObservacionDeClase && r.numeroObservacion() != null) {
            throw new ReglaNegocioException("La pauta de observación de clase no lleva número de observación: deja numeroObservacion vacío.");
        }
        if (!deObservacionDeClase && r.numeroObservacion() == null) {
            throw new ReglaNegocioException("Las pautas de Especialidad y de Orientación son de la primera o de la segunda "
                    + "observación: indica cuál (numeroObservacion 1 o 2).");
        }
        if (r.escalaMax() <= r.escalaMin()) {
            throw new ReglaNegocioException("La escala máxima (" + r.escalaMax() + ") debe ser mayor que la mínima ("
                    + r.escalaMin() + ").");
        }
        Set<String> nombres = new HashSet<>();
        for (PautaGuardarRequest.Dimension d : r.dimensiones()) {
            if (!nombres.add(d.nombre().trim().toLowerCase(Locale.ROOT))) {
                throw new ReglaNegocioException("Hay dos dimensiones llamadas \"" + d.nombre().trim() + "\": cada una debe tener un nombre distinto.");
            }
        }
    }

    /** Copia los datos de la petición: dimensiones con orden 1, 2, 3... e indicadores numerados en toda la pauta. */
    private static void llenar(Pauta p, PautaGuardarRequest r) {
        p.setNombre(r.nombre().trim());
        p.setTipoEvaluador(r.tipoEvaluador());
        p.setAmbito(r.ambito());
        p.setNumeroObservacion(r.numeroObservacion());
        p.setEscalaMin(r.escalaMin());
        p.setEscalaMax(r.escalaMax());
        int numero = 1;
        for (int i = 0; i < r.dimensiones().size(); i++) {
            PautaGuardarRequest.Dimension datos = r.dimensiones().get(i);
            DimensionPauta d = new DimensionPauta();
            d.setPauta(p);
            d.setNombre(datos.nombre().trim());
            d.setOrden(i + 1);
            for (String texto : datos.indicadores()) {
                Indicador ind = new Indicador();
                ind.setDimension(d);
                ind.setNumero(numero++);
                ind.setDescripcion(texto.trim());
                d.getIndicadores().add(ind);
            }
            p.getDimensiones().add(d);
        }
    }

    /** ¿La petición describe exactamente la pauta que ya está? Entonces guardar no crea una versión sin motivo. */
    private static boolean sinCambios(Pauta p, PautaGuardarRequest r) {
        if (!p.getNombre().equals(r.nombre().trim()) || p.getTipoEvaluador() != r.tipoEvaluador()
                || p.getAmbito() != r.ambito() || !Objects.equals(p.getNumeroObservacion(), r.numeroObservacion())
                || !p.getEscalaMin().equals(r.escalaMin())
                || !p.getEscalaMax().equals(r.escalaMax()) || p.getDimensiones().size() != r.dimensiones().size()) {
            return false;
        }
        for (int i = 0; i < r.dimensiones().size(); i++) {
            DimensionPauta d = p.getDimensiones().get(i);
            PautaGuardarRequest.Dimension datos = r.dimensiones().get(i);
            if (!d.getNombre().equals(datos.nombre().trim())) {
                return false;
            }
            List<String> textos = d.getIndicadores().stream().map(Indicador::getDescripcion).toList();
            if (!textos.equals(datos.indicadores().stream().map(String::trim).toList())) {
                return false;
            }
        }
        return true;
    }
}
