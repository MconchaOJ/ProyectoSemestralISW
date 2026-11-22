package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.ProductoPractica;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoPracticaRepository extends JpaRepository<ProductoPractica, Long> {

    @Query("SELECT p FROM ProductoPractica p JOIN FETCH p.subidoPor WHERE p.inscripcion.id = :inscripcionId "
            + "ORDER BY p.fechaSubida, p.id")
    List<ProductoPractica> findDeInscripcion(@Param("inscripcionId") Long inscripcionId);

    /** HU-18: los productos de todo un recorrido en una sola consulta (historial). */
    @Query("SELECT p FROM ProductoPractica p JOIN FETCH p.subidoPor WHERE p.inscripcion.id IN :ids "
            + "ORDER BY p.fechaSubida, p.id")
    List<ProductoPractica> findPorInscripciones(@Param("ids") Collection<Long> ids);
}
