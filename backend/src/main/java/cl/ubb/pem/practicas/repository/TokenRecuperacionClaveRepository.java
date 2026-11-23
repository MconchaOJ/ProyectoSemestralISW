package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.TokenRecuperacionClave;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRecuperacionClaveRepository extends JpaRepository<TokenRecuperacionClave, Long> {

    @Query("SELECT t FROM TokenRecuperacionClave t JOIN FETCH t.usuario WHERE t.tokenHash = :hash")
    Optional<TokenRecuperacionClave> findByTokenHash(@Param("hash") String hash);

    /** S-98: cuántos enlaces pidió el usuario desde ese momento (usados o no: todos cuentan para el límite). */
    long countByUsuarioIdAndFechaCreacionAfter(Long usuarioId, LocalDateTime desde);

    /** Deja sin efecto todos los enlaces pendientes del usuario (al pedir uno nuevo y al restablecer la clave). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TokenRecuperacionClave t SET t.usado = true WHERE t.usuario.id = :usuarioId AND t.usado = false")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId);
}
