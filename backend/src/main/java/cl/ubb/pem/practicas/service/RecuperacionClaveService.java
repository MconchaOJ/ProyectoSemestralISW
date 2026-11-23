package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.config.RecuperacionProperties;
import cl.ubb.pem.practicas.entity.TokenRecuperacionClave;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import cl.ubb.pem.practicas.repository.TokenRecuperacionClaveRepository;
import cl.ubb.pem.practicas.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Recuperación de la clave por correo (SUPUESTOS S-97 a S-99), para los 6 roles.
 *
 * <ul>
 *   <li>Pedir el enlace responde siempre lo mismo, exista o no el correo, esté activo o no, y aunque se haya pasado
 *       el límite: así no se puede averiguar qué correos están registrados.</li>
 *   <li>El token son 32 bytes de SecureRandom; en la base queda solo su SHA-256 (es aleatorio y largo, no necesita
 *       la sal ni la lentitud de BCrypt, y así se puede buscar directo por el hash).</li>
 *   <li>Un enlace nuevo deja sin efecto los anteriores del mismo usuario; restablecer la clave, todos.</li>
 * </ul>
 */
@Service
public class RecuperacionClaveService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionClaveService.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    static final String MENSAJE_SOLICITUD = "Si el correo está registrado, te enviamos un enlace para restablecer tu "
            + "clave. Revisa tu bandeja de entrada y la carpeta de spam.";
    static final String MENSAJE_ENLACE_INVALIDO = "El enlace para restablecer la clave no es válido o ya venció. "
            + "Pide uno nuevo.";

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionClaveRepository tokens;
    private final PasswordEncoder encoder;
    private final BloqueoLoginService bloqueo;
    private final EnvioCorreoService correo;
    private final RecuperacionProperties props;
    private final Clock reloj;
    private final TransactionTemplate transaccion;

    public RecuperacionClaveService(UsuarioRepository usuarios, TokenRecuperacionClaveRepository tokens,
                                    PasswordEncoder encoder, BloqueoLoginService bloqueo, EnvioCorreoService correo,
                                    RecuperacionProperties props, Clock reloj, PlatformTransactionManager tm) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.bloqueo = bloqueo;
        this.correo = correo;
        this.props = props;
        this.reloj = reloj;
        this.transaccion = new TransactionTemplate(tm);
    }

    private record Envio(String email, String nombre, String enlace) {
    }

    /**
     * Crea el enlace (si corresponde) y lo manda. El correo sale después de guardar el token: primero se confirma la
     * transacción y recién entonces se encola el envío (fuera de la transacción).
     *
     * @return el mensaje genérico, siempre el mismo
     */
    public String solicitar(String email) {
        Envio envio = transaccion.execute(estado -> crearEnlace(email == null ? "" : email.trim()));
        if (envio != null) {
            correo.enviarEnlaceRecuperacion(envio.email(), envio.nombre(), envio.enlace(), props.minutosValidez());
        }
        return MENSAJE_SOLICITUD;
    }

    private Envio crearEnlace(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            log.info("Solicitud de recuperación de clave para un correo no registrado o inactivo (no se manda nada)");
            return null;
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        long recientes = tokens.countByUsuarioIdAndFechaCreacionAfter(usuario.getId(),
                ahora.minusMinutes(props.ventanaMinutos()));
        if (recientes >= props.solicitudesMaximas()) {
            log.warn("Límite de recuperación de clave alcanzado para {} ({} enlaces en {} minutos): no se manda otro",
                    usuario.getEmail(), recientes, props.ventanaMinutos());
            return null;
        }
        tokens.invalidarPendientes(usuario.getId());
        String token = nuevoToken();
        TokenRecuperacionClave t = new TokenRecuperacionClave();
        t.setUsuario(usuario);
        t.setTokenHash(sha256(token));
        t.setFechaCreacion(ahora);
        t.setFechaExpiracion(ahora.plusMinutes(props.minutosValidez()));
        t.setUsado(false);
        tokens.save(t);
        String enlace = props.urlPublica() + "/restablecer-clave.html?token=" + token;
        return new Envio(usuario.getEmail(), usuario.getNombre(), enlace);
    }

    /**
     * Cambia la clave con un enlace vigente y no usado. Enlace inexistente, usado, vencido o de una cuenta inactiva:
     * 400 con el mismo mensaje. La clave nueva sigue la regla de HU-26 (8 a 72 caracteres, en el DTO).
     */
    @Transactional
    public void restablecer(String token, String nuevaClave) {
        TokenRecuperacionClave t = tokens.findByTokenHash(sha256(token.trim()))
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_ENLACE_INVALIDO));
        Usuario usuario = t.getUsuario();
        if (Boolean.TRUE.equals(t.getUsado()) || !LocalDateTime.now(reloj).isBefore(t.getFechaExpiracion())
                || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ReglaNegocioException(MENSAJE_ENLACE_INVALIDO);
        }
        usuario.setPasswordHash(encoder.encode(nuevaClave));
        usuarios.save(usuario);
        tokens.invalidarPendientes(usuario.getId());   // este enlace y cualquier otro pendiente
        bloqueo.registrarExito(usuario.getEmail());     // quien demostró tener el correo puede volver a entrar ya
        log.info("Clave restablecida con un enlace de recuperación: {}", usuario.getEmail());
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
