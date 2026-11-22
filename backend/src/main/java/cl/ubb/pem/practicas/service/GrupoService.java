package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.GrupoDto;
import cl.ubb.pem.practicas.dto.GrupoRequest;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.GrupoPractica;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Modalidad;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.GrupoPracticaRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-10: duplas y grupos de práctica. Solo el coordinador los crea, cambia o deshace (lo exige @PreAuthorize en el
 * controlador); el coordinador y el jefe de carrera los listan. Reglas (S-78): misma asignatura y periodo (también lo
 * exige la base con la clave foránea compuesta), práctica en DUPLA (exactamente 2) o GRUPO (2 a 4), todas EN_CURSO,
 * ninguna en otro grupo y el mismo profesor guía (criterio 1: la dupla comparte guía, colegio y curso). El grupo solo
 * agrupa: cada integrante conserva su inscripción, su pauta y sus planificaciones (S-77).
 */
@Service
public class GrupoService {

    private final GrupoPracticaRepository grupos;
    private final InscripcionRepository inscripciones;

    public GrupoService(GrupoPracticaRepository grupos, InscripcionRepository inscripciones) {
        this.grupos = grupos;
        this.inscripciones = inscripciones;
    }

    @Transactional(readOnly = true)
    public List<GrupoDto> listar(Integer asignaturaId, String periodo) {
        return grupos.buscar(asignaturaId, periodo).stream().map(GrupoDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public GrupoDto obtener(Long id) {
        return GrupoDto.desde(buscar(id));
    }

    @Transactional
    public GrupoDto crear(GrupoRequest r) {
        List<Inscripcion> miembros = validarIntegrantes(r, null);
        Inscripcion primera = miembros.get(0);
        GrupoPractica g = new GrupoPractica();
        g.setAsignatura(primera.getAsignatura());
        g.setPeriodo(primera.getPeriodo());
        copiarColegio(g, r);
        grupos.saveAndFlush(g);
        for (Inscripcion i : miembros) {
            i.setGrupo(g);
            g.getIntegrantes().add(i);
        }
        inscripciones.saveAllAndFlush(miembros);
        return GrupoDto.desde(g);
    }

    /** Reemplaza los integrantes (los que salen quedan sin grupo) y el establecimiento y curso. */
    @Transactional
    public GrupoDto actualizar(Long id, GrupoRequest r) {
        GrupoPractica g = buscar(id);
        List<Inscripcion> miembros = validarIntegrantes(r, g);
        if (!miembros.get(0).getAsignatura().getId().equals(g.getAsignatura().getId())
                || !miembros.get(0).getPeriodo().equals(g.getPeriodo())) {
            throw new ReglaNegocioException("Los integrantes deben ser de " + g.getAsignatura().getNombre() + " "
                    + g.getPeriodo() + ", como el grupo.");
        }
        Set<Long> nuevos = miembros.stream().map(Inscripcion::getId).collect(Collectors.toSet());
        for (Inscripcion sale : List.copyOf(g.getIntegrantes())) {
            if (!nuevos.contains(sale.getId())) {
                sale.setGrupo(null);
                g.getIntegrantes().remove(sale);
                inscripciones.save(sale);
            }
        }
        for (Inscripcion entra : miembros) {
            if (entra.getGrupo() == null) {
                entra.setGrupo(g);
                g.getIntegrantes().add(entra);
                inscripciones.save(entra);
            }
        }
        copiarColegio(g, r);
        grupos.saveAndFlush(g);
        return GrupoDto.desde(g);
    }

    /** Deshace el grupo: sus integrantes quedan sin grupo (sus inscripciones, evaluaciones y planificaciones no cambian). */
    @Transactional
    public void eliminar(Long id) {
        GrupoPractica g = buscar(id);
        if (g.getIntegrantes().stream().anyMatch(i -> i.getEstado() != EstadoInscripcion.EN_CURSO)) {
            throw new ConflictoException("El grupo tiene integrantes con la práctica cerrada: queda como registro histórico.");
        }
        for (Inscripcion i : g.getIntegrantes()) {
            i.setGrupo(null);
            inscripciones.save(i);
        }
        inscripciones.flush();
        grupos.delete(g);
    }

    private GrupoPractica buscar(Long id) {
        return grupos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el grupo con id " + id + "."));
    }

    private List<Inscripcion> validarIntegrantes(GrupoRequest r, GrupoPractica actual) {
        Set<Long> ids = new HashSet<>(r.inscripcionIds());
        if (ids.size() != r.inscripcionIds().size()) {
            throw new ReglaNegocioException("Una inscripción aparece dos veces en el grupo.");
        }
        List<Inscripcion> miembros = inscripciones.findAllById(ids);
        if (miembros.size() != ids.size()) {
            ids.removeAll(miembros.stream().map(Inscripcion::getId).toList());
            throw new RecursoNoEncontradoException("No existen las inscripciones " + ids + ".");
        }
        Inscripcion primera = miembros.get(0);
        AsignaturaPractica a = primera.getAsignatura();
        if (miembros.stream().anyMatch(i -> !i.getAsignatura().getId().equals(a.getId())
                || !i.getPeriodo().equals(primera.getPeriodo()))) {
            throw new ReglaNegocioException("Todos los integrantes deben ser de la misma asignatura y periodo.");
        }
        if (a.getModalidad() == Modalidad.INDIVIDUAL) {
            throw new ReglaNegocioException(a.getNombre() + " es individual: no lleva duplas ni grupos.");
        }
        if (a.getModalidad() == Modalidad.DUPLA && miembros.size() != 2) {
            throw new ReglaNegocioException(a.getNombre() + " es en dupla: el grupo debe tener exactamente 2 integrantes.");
        }
        if (miembros.stream().anyMatch(i -> i.getEstado() != EstadoInscripcion.EN_CURSO)) {
            throw new ConflictoException("Solo se agrupan inscripciones EN_CURSO.");
        }
        List<String> enOtro = miembros.stream()
                .filter(i -> i.getGrupo() != null && (actual == null || !i.getGrupo().getId().equals(actual.getId())))
                .map(i -> i.getEstudiante().getUsuario().getNombreCompleto()).toList();
        if (!enOtro.isEmpty()) {
            throw new ConflictoException("Ya están en otra dupla o grupo: " + String.join(", ", enOtro) + ".");
        }
        Set<Long> guias = miembros.stream().map(Inscripcion::getProfesorGuia)
                .map(u -> u == null ? null : u.getId()).collect(Collectors.toCollection(HashSet::new));
        if (guias.size() > 1) {
            throw new ReglaNegocioException("Los integrantes deben tener el mismo profesor guía (la dupla comparte guía, "
                    + "colegio y curso): " + miembros.stream().map(GrupoService::conGuia).collect(Collectors.joining("; ")) + ".");
        }
        if (!Boolean.TRUE.equals(a.getRequiereCentroEducativo()) && (tieneTexto(r.establecimiento()) || tieneTexto(r.curso()))) {
            throw new ReglaNegocioException(a.getNombre() + " no va a un colegio: deja vacíos el establecimiento y el curso.");
        }
        return miembros;
    }

    private static String conGuia(Inscripcion i) {
        Usuario g = i.getProfesorGuia();
        return i.getEstudiante().getUsuario().getNombreCompleto() + " con " + (g == null ? "sin guía" : g.getNombreCompleto());
    }

    private static void copiarColegio(GrupoPractica g, GrupoRequest r) {
        g.setEstablecimiento(tieneTexto(r.establecimiento()) ? r.establecimiento().trim() : null);
        g.setCurso(tieneTexto(r.curso()) ? r.curso().trim() : null);
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }
}
