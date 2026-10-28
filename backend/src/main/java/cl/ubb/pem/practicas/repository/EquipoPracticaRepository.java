package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.EquipoPractica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoPracticaRepository extends JpaRepository<EquipoPractica, Long> {

    /** Equipos con filtros opcionales (parámetro nulo = sin filtro), del más reciente al más antiguo. */
    @Query("""
            select e from EquipoPractica e
            where (:asignaturaId is null or e.asignatura.id = :asignaturaId)
              and (:periodo is null or e.periodo = :periodo)
            order by e.periodo desc, e.asignatura.orden, e.id""")
    List<EquipoPractica> buscar(@Param("asignaturaId") Integer asignaturaId, @Param("periodo") String periodo);
}
