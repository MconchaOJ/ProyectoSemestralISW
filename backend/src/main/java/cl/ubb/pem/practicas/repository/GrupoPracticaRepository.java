package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.GrupoPractica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GrupoPracticaRepository extends JpaRepository<GrupoPractica, Long> {

    /** Grupos con filtros opcionales (nulo = sin filtro), del periodo más reciente al más antiguo. */
    @Query("""
            select g from GrupoPractica g
            where (:asignaturaId is null or g.asignatura.id = :asignaturaId)
              and (:periodo is null or g.periodo = :periodo)
            order by g.periodo desc, g.asignatura.orden, g.id""")
    List<GrupoPractica> buscar(@Param("asignaturaId") Integer asignaturaId, @Param("periodo") String periodo);
}
