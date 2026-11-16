package unsa.sistemas.model.direct;

import unsa.sistemas.model.trace.Traza;

/**
 * Contrato de los métodos de resolución directa
 * (eliminación gaussiana, Gauss-Jordan, Crout, Cholesky...).
 *
 * <p>Resuelven Ax = b en un número finito de operaciones sobre la matriz.
 * Si el trazado está activo, registran en su {@link Traza} la evolución
 * de la matriz [A|b] (operaciones fila, pivotes, coeficientes usados).</p>
 */
public interface ResolutorDirecto {

    /**
     * Resuelve el sistema Ax = b con el método directo correspondiente.
     *
     * <p>Cada implementación valida aquí sus propias precondiciones
     * (p. ej. Cholesky: matriz simétrica y definida positiva) y registra
     * sus pasos en la traza si fue creada con una.</p>
     *
     * @return el vector solución x de longitud n
     * @throws IllegalArgumentException si no se cumplen las precondiciones
     *         del método
     */
    double[] resolver();

    /**
     * Traza de pasos registrados durante la ejecución.
     *
     * @return la traza cargada en el constructor, o {@code null} si el
     *         resolutor se creó sin trazado. Consultar después de
     *         {@link #resolver()}
     */
    Traza getTraza();
}
