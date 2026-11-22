package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.ubb.pem.practicas.entity.Evaluacion;
import cl.ubb.pem.practicas.entity.NotaVoz;
import cl.ubb.pem.practicas.entity.Usuario;
import cl.ubb.pem.practicas.repository.EvaluacionRepository;
import cl.ubb.pem.practicas.repository.NotaVozRepository;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * HU-11: volver a grabar la nota de voz. Comprueba qué archivo se borra según cómo termina la transacción: el viejo
 * si la base confirmó el cambio, el nuevo si se revirtió (así nunca queda una nota de voz sin su audio). Las pruebas
 * de integración no lo pueden ver, porque cada una se revierte al terminar.
 */
@ExtendWith(MockitoExtension.class)
class NotaVozServiceTest {

    @Mock private EvaluacionRepository evaluaciones;
    @Mock private NotaVozRepository notasVoz;
    @Mock private EvaluacionService evaluacionService;
    @Mock private AlmacenamientoArchivosService almacenamiento;

    private NotaVozService servicio;
    private final UsuarioAutenticado guia = new UsuarioAutenticado(2L, "g@demo.example", "PROFESOR_GUIA");
    private final MockMultipartFile audio = new MockMultipartFile("audio", "nueva.webm", "audio/webm", "nuevo".getBytes());

    @BeforeEach
    void preparar() {
        servicio = new NotaVozService(evaluaciones, notasVoz, evaluacionService, almacenamiento);
        Usuario evaluador = new Usuario();
        evaluador.setId(2L);
        Evaluacion e = new Evaluacion();
        e.setId(1L);
        e.setEvaluador(evaluador);
        NotaVoz n = new NotaVoz();
        n.setEvaluacion(e);
        n.setArchivoRuta("notas-voz/vieja.webm");
        when(evaluaciones.findById(1L)).thenReturn(Optional.of(e));
        when(notasVoz.findByEvaluacionId(1L)).thenReturn(Optional.of(n));
        when(almacenamiento.guardarAudio(anyString(), any(), anySet())).thenReturn("notas-voz/nueva.webm");
        when(notasVoz.save(any(NotaVoz.class))).thenAnswer(inv -> inv.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpiar() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void terminarTransaccion(int estado) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(estado));
    }

    @Test
    void siLaBaseConfirmaElCambioSeBorraElAudioViejo() {
        var dto = servicio.reemplazar(1L, audio, "Texto nuevo", 30, guia);
        assertThat(dto.duracionSegundos()).isEqualTo(30);
        verify(almacenamiento, never()).borrar(anyString());   // nada se borra antes de terminar la transacción
        terminarTransaccion(TransactionSynchronization.STATUS_COMMITTED);
        verify(almacenamiento).borrar("notas-voz/vieja.webm");
        verify(almacenamiento, never()).borrar("notas-voz/nueva.webm");
    }

    @Test
    void siLaTransaccionSeRevierteSeBorraElAudioNuevoYElViejoQueda() {
        servicio.reemplazar(1L, audio, "Texto nuevo", 30, guia);
        terminarTransaccion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(almacenamiento).borrar("notas-voz/nueva.webm");
        verify(almacenamiento, never()).borrar("notas-voz/vieja.webm");
    }
}
