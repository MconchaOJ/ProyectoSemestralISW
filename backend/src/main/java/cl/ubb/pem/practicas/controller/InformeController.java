package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.InformeService;
import cl.ubb.pem.practicas.service.InformeService.Formato;
import cl.ubb.pem.practicas.service.InformeService.Informe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "HU-08 Informes por asignatura y por estudiante")
public class InformeController {

    private final InformeService informes;

    public InformeController(InformeService informes) {
        this.informes = informes;
    }

    // Sin "produces" a propósito, como la cartola: así los errores (400, 403, 404) siguen saliendo en JSON.
    @GetMapping("/estudiantes/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR','JEFE_CARRERA')")
    @Operation(summary = "Informe por estudiante (PDF o Excel)",
            description = "Solo COORDINADOR y JEFE_CARRERA. Todas las prácticas del estudiante con sus evaluaciones "
                    + "finalizadas y sus planificaciones, y una síntesis por práctica (cantidad y promedio de "
                    + "evaluaciones, planificaciones autorizadas). El PDF es el mismo del historial (HU-28); el Excel "
                    + "trae una hoja por tema. 404 si el estudiante no existe; 400 si el formato no es pdf ni xlsx.")
    public ResponseEntity<byte[]> estudiante(
            @PathVariable Long id,
            @Parameter(description = "pdf o xlsx", example = "pdf") @RequestParam(defaultValue = "pdf") String formato,
            @AuthenticationPrincipal UsuarioAutenticado actor) {
        return descarga(informes.informeEstudiante(id, Formato.de(formato), actor));
    }

    @GetMapping("/asignaturas/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR','JEFE_CARRERA')")
    @Operation(summary = "Informe por asignatura y periodo (PDF o Excel)",
            description = "Solo COORDINADOR y JEFE_CARRERA. Resumen de las inscripciones de la asignatura en el periodo "
                    + "(por estado, promedio de notas finales, evaluaciones y planificaciones), distribución de las notas "
                    + "finales, puntajes por pauta usada con su distribución (% del puntaje máximo) y una fila por "
                    + "estudiante. Solo cuenta evaluaciones finalizadas. 404 si la asignatura no existe; 400 si el "
                    + "periodo no es AAAA-S o el formato no es pdf ni xlsx.")
    public ResponseEntity<byte[]> asignatura(
            @PathVariable Integer id,
            @Parameter(description = "Periodo AAAA-S", example = "2025-2") @RequestParam String periodo,
            @Parameter(description = "pdf o xlsx", example = "pdf") @RequestParam(defaultValue = "pdf") String formato) {
        return descarga(informes.informeAsignatura(id, periodo, Formato.de(formato)));
    }

    private static ResponseEntity<byte[]> descarga(Informe informe) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(informe.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(informe.nombreArchivo()).build().toString())
                .body(informe.contenido());
    }
}
