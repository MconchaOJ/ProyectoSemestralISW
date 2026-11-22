package cl.ubb.pem.practicas.service;

import cl.ubb.pem.practicas.exception.AccesoDenegadoException;
import cl.ubb.pem.practicas.exception.RecursoNoEncontradoException;
import cl.ubb.pem.practicas.exception.ReglaNegocioException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda y sirve los archivos adjuntos (documentos de planificaciones/evaluaciones y audios de nota de voz) en
 * disco, fuera del control de versiones. La base de datos solo guarda la ruta relativa devuelta por {@link #guardar}.
 */
@Service
public class AlmacenamientoArchivosService {

    /** Tamaño máximo de un documento adjunto (planificación o evaluación), en bytes. Coincide con el CHECK de la base. */
    public static final long TAMANO_MAXIMO_DOCUMENTO = 10L * 1024 * 1024;

    /** Tamaño máximo de un audio de nota de voz, en bytes (SUPUESTO S-43: no lo pidió el cliente). */
    public static final long TAMANO_MAXIMO_AUDIO = 25L * 1024 * 1024;

    private final Path base;
    private final ParametroService parametros;

    public AlmacenamientoArchivosService(@Value("${app.adjuntos.directorio:adjuntos}") String directorio,
                                         ParametroService parametros) {
        this.base = Paths.get(directorio).toAbsolutePath().normalize();
        this.parametros = parametros;
    }

    /** Guarda un documento adjunto con el tamaño máximo vigente (HU-27; nunca sobre 10 MB). */
    public String guardarDocumento(String subcarpeta, MultipartFile archivo, Set<String> extensionesPermitidas) {
        return guardar(subcarpeta, archivo, extensionesPermitidas, parametros.tamanoMaximoDocumento());
    }

    /** Guarda el audio de una nota de voz con el tamaño máximo vigente (HU-27; nunca sobre 25 MB). */
    public String guardarAudio(String subcarpeta, MultipartFile archivo, Set<String> extensionesPermitidas) {
        return guardar(subcarpeta, archivo, extensionesPermitidas, parametros.tamanoMaximoAudio());
    }

    /**
     * Valida extensión y tamaño, y guarda el archivo con un nombre único dentro de {@code subcarpeta}.
     *
     * @return la ruta relativa (subcarpeta/nombre-unico.ext) que se guarda en la base de datos.
     */
    public String guardar(String subcarpeta, MultipartFile archivo, Set<String> extensionesPermitidas, long tamanoMaximo) {
        validar(archivo, extensionesPermitidas, tamanoMaximo);
        try {
            Path carpeta = base.resolve(subcarpeta).normalize();
            Files.createDirectories(carpeta);
            String extension = extension(archivo.getOriginalFilename());
            String nombreUnico = UUID.randomUUID() + extension;
            Path destino = carpeta.resolve(nombreUnico);
            archivo.transferTo(destino);
            return subcarpeta + "/" + nombreUnico;
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo guardar el archivo en el servidor: " + e.getMessage());
        }
    }

    /** Carga el archivo ya guardado para descargarlo o transmitirlo. */
    public Resource cargar(String rutaRelativa) {
        Path ruta = base.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(base)) {
            throw new AccesoDenegadoException("Ruta de archivo inválida.");
        }
        try {
            Resource recurso = new UrlResource(ruta.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new RecursoNoEncontradoException("El archivo ya no está disponible en el servidor.");
            }
            return recurso;
        } catch (java.net.MalformedURLException e) {
            throw new RecursoNoEncontradoException("El archivo ya no está disponible en el servidor.");
        }
    }

    /**
     * Borra un archivo ya guardado (HU-11: el audio viejo al reemplazar la nota de voz). No falla si ya no existe;
     * devuelve si lo borró. Nunca sale de la carpeta de adjuntos.
     */
    public boolean borrar(String rutaRelativa) {
        Path ruta = base.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(base) || ruta.equals(base)) {
            throw new AccesoDenegadoException("Ruta de archivo inválida.");
        }
        try {
            return Files.deleteIfExists(ruta);
        } catch (IOException e) {
            return false;   // queda un archivo huérfano en disco; la base ya no lo referencia
        }
    }

    private void validar(MultipartFile archivo, Set<String> extensionesPermitidas, long tamanoMaximo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debes adjuntar un archivo.");
        }
        if (archivo.getSize() > tamanoMaximo) {
            throw new ReglaNegocioException("El archivo supera el tamaño máximo permitido ("
                    + (tamanoMaximo / (1024 * 1024)) + " MB).");
        }
        String extension = extension(archivo.getOriginalFilename());
        if (!extensionesPermitidas.contains(extension)) {
            throw new ReglaNegocioException("Extensión de archivo no permitida (" + extension
                    + "). Se aceptan: " + extensionesPermitidas + ".");
        }
    }

    private String extension(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }
        int punto = nombreOriginal.lastIndexOf('.');
        return punto < 0 ? "" : nombreOriginal.substring(punto).toLowerCase(Locale.ROOT);
    }
}
