package cl.ubb.pem.practicas.repository;

import cl.ubb.pem.practicas.entity.MensajeChatbot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeChatbotRepository extends JpaRepository<MensajeChatbot, Long> {

    /** La conversación de una inscripción en orden (el id desempata dos mensajes guardados en el mismo instante). */
    @Query("""
            select m from MensajeChatbot m
              left join fetch m.planificacion
            where m.inscripcion.id = :inscripcionId
            order by m.fecha, m.id""")
    List<MensajeChatbot> findConversacion(@Param("inscripcionId") Long inscripcionId);
}
