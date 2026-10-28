package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EquipoCrearRequest;
import cl.ubb.pem.practicas.dto.EquipoDto;
import cl.ubb.pem.practicas.entity.AsignaturaPractica;
import cl.ubb.pem.practicas.entity.EquipoPractica;
import cl.ubb.pem.practicas.entity.EstadoInscripcion;
import cl.ubb.pem.practicas.entity.Inscripcion;
import cl.ubb.pem.practicas.entity.Modalidad;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EquipoPracticaRepository;
import cl.ubb.pem.practicas.repository.InscripcionRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-10: prácticas en dupla y en grupo. El coordinador arma el equipo con inscripciones que ya existen (cada
 * integrante conserva la suya, con sus propias evaluaciones: criterio 2) y el equipo comparte guía, colegio y curso
 * (criterio 1). Reglas, todas SUPUESTO S-79 / S-80 salvo la modalidad, que ya estaba en la asignatura:
 * <ul>
 *   <li>DUPLA: exactamente 2 integrantes; GRUPO: de 2 a 4; INDIVIDUAL no forma equipos.</li>
 *   <li>Misma asignatura y periodo, todas EN_CURSO y ninguna en otro equipo.</li>
 *   <li>El mismo profesor guía en todas (o ninguno, en el ciclo inicial).</li>
 *   <li>Si la asignatura va a un colegio, el colegio y el curso son obligatorios.</li>
 * </ul>
 */
@Service
public class EquipoService {

    private static final int MAXIMO_GRUPO = 4;

    private final EquipoPracticaRepository equipos;
    private final InscripcionRepository inscripciones;

    public EquipoService(EquipoPracticaRepository equipos, InscripcionRepository inscripciones) {
        this.equipos = equipos;
        this.inscripciones = inscripciones;
    }

    @Transactional(readOnly = true)
    public List<EquipoDto> listar(Integer asignaturaId, String periodo) {
        String filtro = periodo == null || periodo.isBlank() ? null : periodo.strip();
        return equipos.buscar(asignaturaId, filtro).stream().map(EquipoDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public EquipoDto obtener(Long id) {
        return EquipoDto.desde(buscar(id));
    }

    @Transactional
    public EquipoDto crear(EquipoCrearRequest r) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(r.inscripcionIds()));
        if (ids.size() != r.inscripcionIds().size()) {
            throw new ReglaNegocioException("Hay inscripciones repetidas en el equipo.");
        }
        List<Inscripcion> integrantes = ids.stream().map(id -> inscripciones.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la inscripción con id " + id + "."))).toList();

        Inscripcion primera = integrantes.get(0);
        AsignaturaPractica asignatura = primera.getAsignatura();
        for (Inscripcion i : integrantes) {
            if (!i.getAsignatura().getId().equals(asignatura.getId()) || !i.getPeriodo().equals(primera.getPeriodo())) {
                throw new ReglaNegocioException("Todas las inscripciones deben ser de la misma asignatura y periodo.");
            }
        }
        validarTamano(asignatura, integrantes.size());
        for (Inscripcion i : integrantes) {
            if (i.getEstado() != EstadoInscripcion.EN_CURSO) {
                throw new ConflictoException("La inscripción " + i.getId() + " ya está cerrada (" + i.getEstado() + ").");
            }
            if (i.getEquipo() != null) {
                throw new ConflictoException("La inscripción " + i.getId() + " ya está en otro equipo ("
                        + i.getEquipo().getId() + ").");
            }
            if (!Objects.equals(idDe(i.getProfesorGuia()), idDe(primera.getProfesorGuia()))) {
                throw new ReglaNegocioException("Los integrantes deben tener el mismo profesor guía (criterio 1).");
            }
        }

        EquipoPractica e = new EquipoPractica();
        e.setAsignatura(asignatura);
        e.setPeriodo(primera.getPeriodo());
        if (Boolean.TRUE.equals(asignatura.getRequiereCentroEducativo())) {
            e.setEstablecimiento(obligatorio(r.establecimiento(), "el colegio"));
            e.setCurso(obligatorio(r.curso(), "el curso"));
        }
        EquipoPractica guardado = equipos.save(e);
        for (Inscripcion i : integrantes) {
            i.setEquipo(guardado);
            guardado.getIntegrantes().add(i);
        }
        return EquipoDto.desde(guardado);
    }

    /** Deshace el equipo: cada integrante vuelve a ser individual. Solo si todos siguen EN_CURSO (409 si no). */
    @Transactional
    public void disolver(Long id) {
        EquipoPractica e = buscar(id);
        if (e.getIntegrantes().stream().anyMatch(i -> i.getEstado() != EstadoInscripcion.EN_CURSO)) {
            throw new ConflictoException("El equipo tiene inscripciones cerradas: queda como registro histórico.");
        }
        e.getIntegrantes().forEach(i -> i.setEquipo(null));
        inscripciones.flush();
        equipos.delete(e);
    }

    private static void validarTamano(AsignaturaPractica asignatura, int cantidad) {
        Modalidad m = asignatura.getModalidad();
        if (m == Modalidad.INDIVIDUAL) {
            throw new ReglaNegocioException(asignatura.getNombre() + " es individual: no forma duplas ni grupos.");
        }
        if (m == Modalidad.DUPLA && cantidad != 2) {
            throw new ReglaNegocioException(asignatura.getNombre() + " se trabaja en dupla: el equipo debe tener "
                    + "exactamente 2 integrantes.");
        }
        if (m == Modalidad.GRUPO && cantidad > MAXIMO_GRUPO) {
            throw new ReglaNegocioException("Un grupo tiene como máximo " + MAXIMO_GRUPO + " integrantes.");
        }
    }

    private static String obligatorio(String valor, String que) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("Esta asignatura va a un colegio: indica " + que
                    + " que comparte el equipo (criterio 1).");
        }
        return valor.strip();
    }

    private static Long idDe(Usuario u) {
        return u == null ? null : u.getId();
    }

    private EquipoPractica buscar(Long id) {
        return equipos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo con id " + id + "."));
    }
}
