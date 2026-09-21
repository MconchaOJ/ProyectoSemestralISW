package cl.ubb.pem.practicas.service;

import java.util.regex.Pattern;

/** Normalización y validación del RUT chileno (formato "12345678-5" y dígito verificador con módulo 11). */
public final class Rut {

    private static final Pattern FORMATO = Pattern.compile("^[0-9]{7,8}-[0-9K]$");

    private Rut() {
    }

    /** Quita puntos y espacios, pasa a mayúsculas y deja el guion antes del dígito verificador. */
    public static String normalizar(String rut) {
        if (rut == null) {
            return null;
        }
        String limpio = rut.replace(".", "").replace(" ", "").toUpperCase();
        if (!limpio.contains("-") && limpio.length() >= 2) {
            limpio = limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
        }
        return limpio;
    }

    /** Recibe un RUT ya normalizado. Comprueba el formato que exige la base y el dígito verificador. */
    public static boolean esValido(String rutNormalizado) {
        if (rutNormalizado == null || !FORMATO.matcher(rutNormalizado).matches()) {
            return false;
        }
        String cuerpo = rutNormalizado.substring(0, rutNormalizado.length() - 2);
        return digitoVerificador(cuerpo) == rutNormalizado.charAt(rutNormalizado.length() - 1);
    }

    static char digitoVerificador(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        return resto == 10 ? 'K' : (char) ('0' + resto);
    }
}
