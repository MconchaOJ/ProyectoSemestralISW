package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.EstudianteCrearRequest;
import cl.ubb.pem.practicas.dto.EstudianteDto;
import cl.ubb.pem.practicas.entity.Estudiante;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.EstudianteRepository;
import cl.ubb.pem.practicas.security.Roles;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-02: registro y consulta de estudiantes. */
@Service
public class EstudianteService {

    private final EstudianteRepository estudiantes;
    private final UsuarioService usuarioService;

    public EstudianteService(EstudianteRepository estudiantes, UsuarioService usuarioService) {
        this.estudiantes = estudiantes;
        this.usuarioService = usuarioService;
    }

    /** Crea a la vez el usuario (rol ESTUDIANTE) y la ficha del estudiante, en una sola transacción. */
    @Transactional
    public EstudianteDto crear(EstudianteCrearRequest r) {
        String rut = Rut.normalizar(r.rut());
        if (!Rut.esValido(rut)) {
            throw new ReglaNegocioException("El RUT no es válido: revisa el formato (12345678-5) y el dígito verificador.");
        }
        if (estudiantes.existsByRut(rut)) {
            throw new ConflictoException("Ya existe un estudiante con el RUT " + rut + ".");
        }
        Usuario usuario = usuarioService.crear(r.nombre(), r.apellidos(), r.email(), r.password(), Roles.ESTUDIANTE);

        Estudiante e = new Estudiante();
        e.setUsuario(usuario);
        e.setRut(rut);
        e.setSemestreActual(r.semestreActual());
        e.setAnioIngreso(r.anioIngreso());
        return EstudianteDto.desde(estudiantes.save(e));
    }

    @Transactional(readOnly = true)
    public List<EstudianteDto> listar(String buscar) {
        String q = buscar == null ? "" : buscar.trim();
        return estudiantes.buscar(q).stream().map(EstudianteDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public EstudianteDto obtener(Long id) {
        return EstudianteDto.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public EstudianteDto obtenerPorUsuario(Long usuarioId) {
        return estudiantes.findByUsuarioId(usuarioId).map(EstudianteDto::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no tiene una ficha de estudiante."));
    }

    Estudiante buscar(Long id) {
        return estudiantes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el estudiante con id " + id + "."));
    }
}
