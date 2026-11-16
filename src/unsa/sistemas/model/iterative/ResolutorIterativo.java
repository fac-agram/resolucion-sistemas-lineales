package unsa.sistemas.model.iterative;

import unsa.sistemas.model.trace.Traza;

/**
 * Contrato de los métodos de resolución iterativa
 * (Jacobi, Gauss-Seidel, SOR, relajación simple, mejoramiento iterativo).
 *
 * <p>Parten de una aproximación inicial (por defecto el vector cero) y
 * generan sucesiones de aproximaciones hasta cumplir un criterio de
 * paro (tolerancia o máximo de iteraciones). Si el trazado está activo,
 * registran en su {@link Traza} el vector x y el error de cada iteración.</p>
 *
 * <p>La tolerancia, el máximo de iteraciones y la aproximación inicial
 * los define cada implementación de forma visible en su propio código.</p>
 */
public interface ResolutorIterativo {

    /**
     * Resuelve el sistema Ax = b iterativamente hasta converger o agotar
     * el máximo de iteraciones.
     *
     * <p>Cada implementación valida aquí sus propias precondiciones
     * (p. ej. ningún cero en la diagonal) y registra sus pasos en la
     * traza si fue creada con una.</p>
     *
     * @return el mejor vector solución x encontrado (última aproximación)
     * @throws IllegalArgumentException si no se cumplen las precondiciones
     *         del método
     */
    double[] resolver();

    /**
     * @return cantidad de iteraciones ejecutadas en la última llamada a
     *         {@link #resolver()}
     */
    int getIteraciones();

    /**
     * @return error final medido en la última iteración (norma de la
     *         diferencia entre aproximaciones sucesivas, o residuo).
     *         {@code NaN} si aún no se ejecutó {@link #resolver()}
     */
    double getErrorFinal();

    /**
     * @return {@code true} si la última ejecución detuvo el bucle por
     *         cumplir la tolerancia; {@code false} si agotó el máximo de
     *         iteraciones o aún no se ejecutó {@link #resolver()}
     */
    boolean convergio();

    /**
     * Traza de pasos registrados durante la ejecución.
     *
     * @return la traza cargada en el constructor, o {@code null} si el
     *         resolutor se creó sin trazado. Consultar después de
     *         {@link #resolver()}
     */
    Traza getTraza();
}
