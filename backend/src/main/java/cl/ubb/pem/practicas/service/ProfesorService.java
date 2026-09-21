package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.dto.ProfesorCrearRequest;
import cl.ubb.pem.practicas.dto.RolProfesor;
import cl.ubb.pem.practicas.dto.UsuarioDto;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-02: registro de profesores guía y profesores del curso. */
@Service
public class ProfesorService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarios;

    public ProfesorService(UsuarioService usuarioService, UsuarioRepository usuarios) {
        this.usuarioService = usuarioService;
        this.usuarios = usuarios;
    }

    @Transactional
    public UsuarioDto crear(ProfesorCrearRequest r) {
        return UsuarioDto.desde(
                usuarioService.crear(r.nombre(), r.apellidos(), r.email(), r.password(), r.rol().name()));
    }

    /** Sin filtro devuelve a todos los profesores (guías y del curso). */
    @Transactional(readOnly = true)
    public List<UsuarioDto> listar(RolProfesor rol) {
        if (rol != null) {
            return usuarios.findByRolNombreOrderByApellidosAscNombreAsc(rol.name()).stream()
                    .map(UsuarioDto::desde).toList();
        }
        return usuarios.findAllByOrderByApellidosAscNombreAsc().stream()
                .filter(u -> u.getRol().getNombre().startsWith("PROFESOR_"))
                .map(UsuarioDto::desde).toList();
    }
}
