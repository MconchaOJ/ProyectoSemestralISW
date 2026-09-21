package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.entity.Rol;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ConflictoException;
import cl.ubb.pem.practicas.repository.RolRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private RolRepository roles;

    @Test
    void guardaLaClaveSoloComoHashBCryptYElCorreoEnMinusculas() {
        String clave = UUID.randomUUID().toString();
        Rol rol = new Rol();
        rol.setNombre("PROFESOR_GUIA");
        when(roles.findByNombre("PROFESOR_GUIA")).thenReturn(Optional.of(rol));
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario u = new UsuarioService(usuarios, roles, encoder)
                .crear(" Marta ", "Lagos", " Marta.Lagos@Demo.Example ", clave, "PROFESOR_GUIA");

        assertThat(u.getEmail()).isEqualTo("marta.lagos@demo.example");
        assertThat(u.getNombre()).isEqualTo("Marta");
        assertThat(u.getPasswordHash()).isNotEqualTo(clave).startsWith("$2a$10$").hasSize(60);
        assertThat(encoder.matches(clave, u.getPasswordHash())).isTrue();
        assertThat(u.getActivo()).isTrue();
    }

    @Test
    void correoRepetidoDaConflicto() {
        when(usuarios.existsByEmailIgnoreCase("repetido@demo.example")).thenReturn(true);
        assertThatThrownBy(() -> new UsuarioService(usuarios, roles, encoder)
                .crear("A", "B", "repetido@demo.example", "una-clave-larga", "ESTUDIANTE"))
                .isInstanceOf(ConflictoException.class);
    }
}
