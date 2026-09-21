package cl.ubb.pem.practicas.security;

import cl.ubb.pem.practicas.entity.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Genera y valida los tokens JWT (HS256). El token identifica al usuario y su rol y expira (S-06). */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int LARGO_MINIMO_SECRETO = 32;

    private final SecretKey clave;
    private final Duration duracion;

    public JwtService(JwtProperties props) {
        this.duracion = Duration.ofHours(props.expiracionHoras());
        if (props.secret() == null || props.secret().isBlank()) {
            byte[] aleatorio = new byte[LARGO_MINIMO_SECRETO];
            new SecureRandom().nextBytes(aleatorio);
            this.clave = Keys.hmacShaKeyFor(aleatorio);
            log.warn("ISW_JWT_SECRET no está definida: se generó un secreto temporal. "
                    + "Los tokens dejarán de valer al reiniciar la aplicación.");
        } else {
            byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
            if (bytes.length < LARGO_MINIMO_SECRETO) {
                throw new IllegalStateException(
                        "ISW_JWT_SECRET debe tener al menos " + LARGO_MINIMO_SECRETO + " caracteres.");
            }
            this.clave = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    /** Devuelve el id del usuario si el token está bien firmado y no ha expirado; vacío en cualquier otro caso. */
    public Optional<Long> extraerUsuarioId(String token) {
        try {
            String sujeto = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(Long.valueOf(sujeto));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long segundosDeVigencia() {
        return duracion.toSeconds();
    }
}
