package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.ParametroSistema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, String> {

    List<ParametroSistema> findAllByOrderByClaveAsc();
}
