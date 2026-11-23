package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.ArchivoOficialPauta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchivoOficialPautaRepository extends JpaRepository<ArchivoOficialPauta, Long> {

    /** HU-25: el historial de una pauta, del más reciente (el vigente) al primero. */
    @Query("SELECT a FROM ArchivoOficialPauta a JOIN FETCH a.subidoPor WHERE a.pauta.id = :pautaId ORDER BY a.version DESC")
    List<ArchivoOficialPauta> findHistorial(@Param("pautaId") Integer pautaId);

    Optional<ArchivoOficialPauta> findFirstByPautaIdOrderByVersionDesc(Integer pautaId);

    /** El archivo vigente de cada pauta que tenga alguno (pantalla «Pautas de evaluación»), en una sola consulta. */
    @Query("SELECT a FROM ArchivoOficialPauta a JOIN FETCH a.subidoPor WHERE a.version = "
            + "(SELECT MAX(b.version) FROM ArchivoOficialPauta b WHERE b.pauta.id = a.pauta.id)")
    List<ArchivoOficialPauta> findVigentes();
}
