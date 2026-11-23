package cl.ubb.pem.practicas.controller;

import cl.ubb.pem.practicas.dto.CambioPasswordRequest;
import cl.ubb.pem.practicas.dto.LoginRequest;
import cl.ubb.pem.practicas.dto.LoginResponse;
import cl.ubb.pem.practicas.dto.MensajeDto;
import cl.ubb.pem.practicas.dto.RecuperarClaveRequest;
import cl.ubb.pem.practicas.dto.RestablecerClaveRequest;
import cl.ubb.pem.practicas.dto.UsuarioDto;
import cl.ubb.pem.practicas.security.UsuarioAutenticado;
import cl.ubb.pem.practicas.service.AuthService;
import cl.ubb.pem.practicas.service.RecuperacionClaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "HU-01 Autenticación", description = "Inicio de sesión, perfil, cambio de contraseña (HU-26) y recuperación por correo (S-97 a S-99)")
public class AuthController {

    private final AuthService authService;
    private final RecuperacionClaveService recuperacion;

    public AuthController(AuthService authService, RecuperacionClaveService recuperacion) {
        this.authService = authService;
        this.recuperacion = recuperacion;
    }

    @PostMapping("/login")
    @SecurityRequirements   // público: no exige token
    @Operation(summary = "Iniciar sesión",
            description = "Público. Devuelve un token JWT (8 horas) con el usuario y su rol. Credenciales incorrectas o "
                    + "usuario inactivo: 401 con el mismo mensaje. Tras app.login.intentos-maximos fallos seguidos con "
                    + "el mismo correo: 429 durante app.login.bloqueo-minutos, con la cabecera Retry-After.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", description = "Cualquier rol autenticado.")
    public UsuarioDto perfil(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return authService.perfil(actual);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "HU-26 Cambiar mi contraseña",
            description = "Cualquier rol autenticado, solo su propia contraseña. Pide la actual y la nueva (8 a 72 "
                    + "caracteres, distinta de la actual). Contraseña actual incorrecta: 400 (cuenta como intento "
                    + "fallido del límite de login; al superarlo, 429).")
    public void cambiarPassword(@AuthenticationPrincipal UsuarioAutenticado actual,
                                @Valid @RequestBody CambioPasswordRequest request) {
        authService.cambiarPassword(actual, request);
    }

    @PostMapping("/recuperar-clave")
    @SecurityRequirements   // público: no exige token
    @Operation(summary = "Pedir un enlace para restablecer la clave",
            description = "Público, para los 6 roles. Si el correo es de una cuenta activa, manda un enlace de un solo "
                    + "uso que vale app.recuperacion.minutos-validez (30) minutos; sin SMTP configurado, el enlace queda "
                    + "en el registro del servidor. Responde SIEMPRE 200 con el mismo mensaje, exista o no el correo y "
                    + "aunque se haya pasado el límite (app.recuperacion.solicitudes-maximas por ventana-minutos), para "
                    + "no revelar qué correos están registrados.")
    public MensajeDto recuperarClave(@Valid @RequestBody RecuperarClaveRequest request) {
        return new MensajeDto(recuperacion.solicitar(request.email()));
    }

    @PostMapping("/restablecer-clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements   // público: el token del correo reemplaza al inicio de sesión
    @Operation(summary = "Restablecer la clave con el enlace del correo",
            description = "Público. Pide el token del enlace y la clave nueva (8 a 72 caracteres, como HU-26). Enlace "
                    + "inexistente, ya usado o vencido: 400 con el mismo mensaje. Al restablecer, ese enlace y "
                    + "cualquier otro pendiente del usuario dejan de servir.")
    public void restablecerClave(@Valid @RequestBody RestablecerClaveRequest request) {
        recuperacion.restablecer(request.token(), request.nuevaClave());
    }
}
