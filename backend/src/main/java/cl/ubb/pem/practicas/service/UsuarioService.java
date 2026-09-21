package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.entity.Rol;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.repository.RolRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creación de usuarios (la comparten estudiantes y profesores). La contraseña se guarda solo como hash BCrypt. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional
    public Usuario crear(String nombre, String apellidos, String email, String password, String nombreRol) {
        String correo = email.trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmailIgnoreCase(correo)) {
            throw new ConflictoException("Ya existe un usuario con el correo " + correo + ".");
        }
        Rol rol = roles.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombreRol + " en la tabla rol."));
        Usuario u = new Usuario();
        u.setRol(rol);
        u.setNombre(nombre.trim());
        u.setApellidos(apellidos.trim());
        u.setEmail(correo);
        u.setPasswordHash(encoder.encode(password));
        u.setActivo(true);
        return usuarios.save(u);
    }
}
