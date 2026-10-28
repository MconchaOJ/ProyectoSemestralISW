package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.DocumentoBaseConocimiento;
import cl.ubb.pem.practicas.entity.TipoDocumentoBase;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoBaseConocimientoRepository extends JpaRepository<DocumentoBaseConocimiento, Long> {

    Optional<DocumentoBaseConocimiento> findByTipo(TipoDocumentoBase tipo);
}
