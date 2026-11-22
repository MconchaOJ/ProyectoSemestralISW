package cl.ubb.pem.practicas.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * HU-09: la escala de notas chilena. SUPUESTO S-81 (60 % de exigencia aprobado por el usuario; la pauta impresa de
 * Práctica Profesional trae su escala de escaladenotas.cl con nota mínima 2,0, nota de aprobación 4,0, exigencia 60 % y
 * puntaje máximo 133): el 4,0 queda en el 60 % del puntaje máximo de la pauta (indicadores x escala máxima). Bajo eso,
 * la nota sube en línea recta desde la NOTA MÍNIMA de la pauta (0 puntos) hasta 4,0; sobre eso, de 4,0 a 7,0 (el
 * máximo). La nota mínima es la escala mínima de la pauta (2 en las de Práctica Pedagógica y Profesional, 1 en la de
 * observación de clase), acotada entre 1,0 y 3,0 (corrección del 2026-11-22: antes se asumía 1,0 para todas).
 * Todas las notas se redondean a un decimal, con la mitad hacia arriba (5,45 -> 5,5).
 */
public final class EscalaNotas {

    public static final BigDecimal EXIGENCIA = new BigDecimal("0.6");
    public static final BigDecimal NOTA_MINIMA = new BigDecimal("1.0");
    public static final BigDecimal NOTA_MAXIMA = new BigDecimal("7.0");
    private static final BigDecimal NOTA_MINIMA_TOPE = new BigDecimal("3.0");
    private static final BigDecimal CUATRO = new BigDecimal("4");
    private static final BigDecimal TRES = new BigDecimal("3");
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private EscalaNotas() {
    }

    /** Nota mínima de una pauta: su escala mínima, entre 1,0 y 3,0. */
    public static BigDecimal notaMinima(int escalaMin) {
        return BigDecimal.valueOf(escalaMin).setScale(1, RoundingMode.HALF_UP).max(NOTA_MINIMA).min(NOTA_MINIMA_TOPE);
    }

    /** Nota de un puntaje sobre un máximo, con el 60 % de exigencia y la nota mínima dada (la de 0 puntos). */
    public static BigDecimal nota(int puntaje, int maximo, BigDecimal notaMinima) {
        return nota(puntaje, maximo, notaMinima, EXIGENCIA);
    }

    /**
     * Nota de un puntaje sobre un máximo con la exigencia dada (fracción: 0,60 = 60 %; desde el 22-nov es el parámetro
     * evaluacion.exigencia_pct de HU-27) y la nota mínima dada (la de 0 puntos).
     */
    public static BigDecimal nota(int puntaje, int maximo, BigDecimal notaMinima, BigDecimal exigencia) {
        if (maximo <= 0 || puntaje < 0 || puntaje > maximo) {
            throw new IllegalArgumentException("Puntaje " + puntaje + " fuera de 0.." + maximo);
        }
        BigDecimal p = BigDecimal.valueOf(puntaje);
        BigDecimal max = BigDecimal.valueOf(maximo);
        BigDecimal corte = max.multiply(exigencia);
        BigDecimal nota = p.compareTo(corte) < 0
                ? notaMinima.add(CUATRO.subtract(notaMinima).multiply(p).divide(corte, PRECISION))
                : CUATRO.add(TRES.multiply(p.subtract(corte)).divide(max.subtract(corte), PRECISION));
        return redondear(nota);
    }

    /** Promedio simple de varias notas, redondeado a un decimal (las evaluaciones de una misma pauta). */
    public static BigDecimal promedio(Collection<BigDecimal> notas) {
        BigDecimal suma = notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return redondear(suma.divide(BigDecimal.valueOf(notas.size()), PRECISION));
    }

    public static BigDecimal redondear(BigDecimal nota) {
        return nota.setScale(1, RoundingMode.HALF_UP).max(NOTA_MINIMA).min(NOTA_MAXIMA);
    }
}
