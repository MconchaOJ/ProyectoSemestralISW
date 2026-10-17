package cl.ubb.pem.practicas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.ubb.pem.practicas.entity.Rol;
import cl.ubb.pem.practicas.entity.Usuario;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRETO = "un-secreto-de-prueba-de-al-menos-32-caracteres!";

    private static Usuario usuario(long id) {
        Rol rol = new Rol();
        rol.setNombre("COORDINADOR");
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("x@demo.example");
        u.setRol(rol);
        return u;
    }

    @Test
    void tokenValidDevuelveElIdDelUsuario() {
        JwtService jwt = new JwtService(new JwtProperties(SECRETO, 8));
        String token = jwt.generarToken(usuario(42));
        assertThat(jwt.extraerUsuarioId(token)).contains(42L);
        assertThat(jwt.segundosDeVigencia()).isEqualTo(8 * 3600);
    }

    @Test
    void tokenExpiradoSeRechaza() {
        JwtService jwt = new JwtService(new JwtProperties(SECRETO, -1));   // ya nace vencido
        assertThat(jwt.extraerUsuarioId(jwt.generarToken(usuario(1)))).isEmpty();
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() {
        JwtService a = new JwtService(new JwtProperties(SECRETO, 8));
        JwtService b = new JwtService(new JwtProperties("otro-secreto-distinto-de-al-menos-32-caracteres", 8));
        assertThat(b.extraerUsuarioId(a.generarToken(usuario(1)))).isEmpty();
    }

    @Test
    void tokenAlteradoOBasuraSeRechaza() {
        JwtService jwt = new JwtService(new JwtProperties(SECRETO, 8));
        String token = jwt.generarToken(usuario(1));
        assertThat(jwt.extraerUsuarioId(token + "x")).isEmpty();
        assertThat(jwt.extraerUsuarioId("no-es-un-token")).isEmpty();
    }

    @Test
    void secretoCortoImpideArrancar() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("corto", 8)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinSecretoGeneraUnoTemporalYFunciona() {
        JwtService jwt = new JwtService(new JwtProperties("", 8));
        assertThat(jwt.extraerUsuarioId(jwt.generarToken(usuario(7)))).contains(7L);
    }

    @Test
    void conSecretoObligatorioYSinSecretoNoArranca() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("", 8, true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ISW_JWT_SECRET");
        assertThatThrownBy(() -> new JwtService(new JwtProperties(null, 8, true)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void conSecretoObligatorioYSecretoValidoFunciona() {
        JwtService jwt = new JwtService(new JwtProperties(SECRETO, 8, true));
        assertThat(jwt.extraerUsuarioId(jwt.generarToken(usuario(9)))).contains(9L);
    }
}
