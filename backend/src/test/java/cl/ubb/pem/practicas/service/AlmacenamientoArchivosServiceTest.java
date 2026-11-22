package cl.ubb.pem.practicas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * HU-21/HU-22: el archivo de un documento borrado o reemplazado se borra del disco solo si la base confirma el
 * cambio. Las pruebas de integración se revierten siempre, así que esto se prueba aquí, simulando el fin de la
 * transacción (commit o rollback).
 */
class AlmacenamientoArchivosServiceTest {

    @TempDir
    Path carpeta;

    private AlmacenamientoArchivosService almacenamiento;

    @BeforeEach
    void preparar() {
        ParametroService parametros = mock(ParametroService.class);
        when(parametros.tamanoMaximoDocumento()).thenReturn(AlmacenamientoArchivosService.TAMANO_MAXIMO_DOCUMENTO);
        almacenamiento = new AlmacenamientoArchivosService(carpeta.toString(), parametros);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpiar() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private String guardar(String nombre) {
        return almacenamiento.guardarDocumento("planificaciones",
                new MockMultipartFile("archivo", nombre, "application/pdf", "%PDF-1.4".getBytes()), Set.of(".pdf"));
    }

    private static void terminarTransaccion(int estado) {
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCompletion(estado);
        }
    }

    @Test
    void alBorrarElArchivoSeVaSoloSiLaBaseConfirma() {
        String ruta = guardar("a.pdf");
        almacenamiento.borrarAlConfirmar(ruta);
        assertThat(carpeta.resolve(ruta)).exists();   // todavía no: la transacción sigue abierta
        terminarTransaccion(TransactionSynchronization.STATUS_COMMITTED);
        assertThat(carpeta.resolve(ruta)).doesNotExist();
    }

    @Test
    void siLaTransaccionSeRevierteElArchivoBorradoQueda() {
        String ruta = guardar("a.pdf");
        almacenamiento.borrarAlConfirmar(ruta);
        terminarTransaccion(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(carpeta.resolve(ruta)).exists();
    }

    @Test
    void alReemplazarQuedaSoloUnArchivoSegunComoTermine() {
        String vieja = guardar("vieja.pdf");
        String nueva = guardar("nueva.pdf");
        almacenamiento.reemplazarAlConfirmar(vieja, nueva);
        terminarTransaccion(TransactionSynchronization.STATUS_COMMITTED);
        assertThat(carpeta.resolve(vieja)).doesNotExist();
        assertThat(carpeta.resolve(nueva)).exists();

        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
        String otra = guardar("otra.pdf");
        almacenamiento.reemplazarAlConfirmar(nueva, otra);
        terminarTransaccion(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(carpeta.resolve(nueva)).exists();      // la base sigue apuntando a esta
        assertThat(carpeta.resolve(otra)).doesNotExist();
    }

    @Test
    void sinTransaccionSeBorraEnElActo() throws Exception {
        String ruta = guardar("a.pdf");
        TransactionSynchronizationManager.clearSynchronization();
        almacenamiento.borrarAlConfirmar(ruta);
        assertThat(Files.exists(carpeta.resolve(ruta))).isFalse();
        TransactionSynchronizationManager.initSynchronization();   // para el @AfterEach
    }
}
