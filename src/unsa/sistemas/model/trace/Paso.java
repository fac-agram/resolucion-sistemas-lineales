package unsa.sistemas.model.trace;

/**
 * Un paso individual del trazado de un método resolutor.
 *
 * <p>Cada {@code Paso} congela el estado relevante en un momento dado de la
 * ejecución: la descripción de la operación realizada, el estado de la matriz
 * (métodos directos) y/o del vector solución (métodos iterativos).</p>
 *
 * <p>Los arreglos se copian de forma defensiva al construir: si el resolutor
 * sigue modificando su estado después, los pasos ya registrados no cambian.</p>
 *
 * @param indice    número de paso dentro de la ejecución (desde 1)
 * @param operacion descripción legible de lo hecho, p. ej.
 *                  {@code "F2 ← F2 - 3·F1"} o {@code "Iteración 5"}
 * @param matriz    snapshot de la matriz en ese paso, o {@code null} si el
 *                  método no modifica la matriz (iterativos puros)
 * @param x         vector solución actual en ese paso, o {@code null} si aún
 *                  no existe (métodos directos antes de la sustitución)
 * @param error     error asociado al paso (métodos iterativos).
 *                  {@code NaN} cuando no aplica (métodos directos)
 */
public record Paso(
        int indice,
        String operacion,
        double[][] matriz,
        double[] x,
        double error
) {
    public Paso {
        matriz = matriz == null ? null : copiarMatriz(matriz);
        x = x == null ? null : x.clone();
    }

    private static double[][] copiarMatriz(double[][] origen) {
        double[][] copia = new double[origen.length][];
        for (int i = 0; i < origen.length; i++) {
            copia[i] = origen[i].clone();
        }
        return copia;
    }
}
