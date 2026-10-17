package cl.ubb.pem.practicas.security;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Herramienta de instalación (no arranca Spring): pide una clave y escribe su hash BCrypt, con el mismo codificador
 * que usa la aplicación. Sirve para crear el coordinador inicial con db/datos-iniciales.sql sin dejar la clave en
 * ningún archivo ni comando. Se ejecuta desde el jar ya compilado:
 *
 * <pre>
 * java -cp practicas-0.1.0-SNAPSHOT.jar -Dloader.main=cl.ubb.pem.practicas.security.GeneradorHashClave \
 *      org.springframework.boot.loader.launch.PropertiesLauncher
 * </pre>
 *
 * En una terminal la clave se escribe sin mostrarse y se pide dos veces; sin terminal (entrada redirigida) se lee
 * una sola línea de la entrada estándar.
 */
public final class GeneradorHashClave {

    private static final int LARGO_MINIMO = 10;

    private GeneradorHashClave() {
    }

    public static void main(String[] args) throws IOException {
        char[] clave = leerClave();
        if (clave == null || clave.length < LARGO_MINIMO) {
            System.err.println("La clave debe tener al menos " + LARGO_MINIMO + " caracteres.");
            System.exit(1);
        }
        System.out.println(new BCryptPasswordEncoder().encode(new String(clave)));
        Arrays.fill(clave, ' ');
    }

    private static char[] leerClave() throws IOException {
        Console consola = System.console();
        if (consola == null) {
            String linea = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            if (linea == null) {
                return null;
            }
            // PowerShell antepone un BOM UTF-8 a lo que entrega por tubería; nunca es parte de la clave
            return (linea.startsWith("﻿") ? linea.substring(1) : linea).toCharArray();
        }
        char[] primera = consola.readPassword("Clave del coordinador: ");
        char[] segunda = consola.readPassword("Repite la clave: ");
        if (!Arrays.equals(primera, segunda)) {
            System.err.println("Las claves no coinciden.");
            System.exit(1);
        }
        Arrays.fill(segunda, ' ');
        return primera;
    }
}
