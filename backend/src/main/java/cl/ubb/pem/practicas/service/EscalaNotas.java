package cl.ubb.pem.practicas.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * HU-09: la escala de notas chilena de 1,0 a 7,0. SUPUESTO S-81 (confirmado por el usuario el 2026-11-22, falta el
 * cliente): 60 % de exigencia sobre el puntaje máximo de la pauta (indicadores x escala máxima). El 4,0 queda en el
 * 60 % del máximo; bajo eso la nota sube en línea recta de 1,0 (0 puntos) a 4,0, y sobre eso de 4,0 a 7,0 (el máximo).
 * Todas las notas se redondean a un decimal, con la mitad hacia arriba (5,45 -> 5,5).
 */
public final class EscalaNotas {

    public static final BigDecimal EXIGENCIA = new BigDecimal("0.6");
    public static final BigDecimal NOTA_MINIMA = new BigDecimal("1.0");
    public static final BigDecimal NOTA_MAXIMA = new BigDecimal("7.0");
    private static final BigDecimal CUATRO = new BigDecimal("4");
    private static final BigDecimal TRES = new BigDecimal("3");
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private EscalaNotas() {
    }

    /** Nota de un puntaje sobre un máximo, con el 60 % de exigencia. */
    public static BigDecimal nota(int puntaje, int maximo) {
        if (maximo <= 0 || puntaje < 0 || puntaje > maximo) {
            throw new IllegalArgumentException("Puntaje " + puntaje + " fuera de 0.." + maximo);
        }
        BigDecimal p = BigDecimal.valueOf(puntaje);
        BigDecimal max = BigDecimal.valueOf(maximo);
        BigDecimal corte = max.multiply(EXIGENCIA);
        BigDecimal nota = p.compareTo(corte) < 0
                ? BigDecimal.ONE.add(TRES.multiply(p).divide(corte, PRECISION))
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
