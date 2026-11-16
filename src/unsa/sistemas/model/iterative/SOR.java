package unsa.sistemas.model.iterative;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

/**
 * Método iterativo SOR (Successive Over-Relaxation) para resolver Ax = b.
 *
 * <p><b>Idea:</b> es Gauss-Seidel con un paso extra de "relajación".
 * En lugar de adoptar directamente el valor que sugiere el despeje
 * ({@code gs}), se avanza una fracción {@code omega} desde el valor
 * actual hacia ese despeje:</p>
 *
 * <pre>
 *   xNuevo = x[i] + ω · (gs − x[i])     // equivalente a (1−ω)·x[i] + ω·gs
 * </pre>
 *
 * <ul>
 *   <li>{@code ω = 1} → exactamente Gauss-Seidel.</li>
 *   <li>{@code 1 < ω < 2} → sobrerelajación: avanza más allá del despeje;
 *       suele converger más rápido si ω se elige bien.</li>
 *   <li>{@code 0 < ω < 1} → subrelajación: avanza más despacio, más
 *       conservador.</li>
 * </ul>
 *
 * <p><b>Rango de convergencia:</b> para que SOR pueda converger se
 * requiere {@code 0 < ω < 2} (si Gauss-Seidel converge). El mejor ω
 * depende del sistema — no existe un valor óptimo universal.</p>
 *
 * <p><b>Precondición:</b> ningún elemento de la diagonal puede ser cero.</p>
 */
public class SOR extends Resolutor implements ResolutorIterativo {

    /** Tolerancia absoluta sobre la diferencia entre iteraciones sucesivas. */
    private static final double TOLERANCIA = 1e-7;

    /** Cota máxima de iteraciones antes de detenerse sin converger. */
    private static final int MAX_ITERACIONES = 1000;

    /**
     * Valor de ω usado cuando no se especifica otro. 1.5 es un punto de
     * partida razonable para sistemas diagonalmente dominantes; pase su
     * propio ω por el constructor para explorar otros valores.
     */
    private static final double OMEGA_DEFECTO = 1.5;

    /** Factor de relajación (0 &lt; ω &lt; 2). */
    private final double omega;

    private int iteraciones;
    private double errorFinal;
    private boolean convergio;

    /**
     * @param A matriz de coeficientes n x n
     * @param b vector de términos independientes
     * @param n orden del sistema
     */
    public SOR(double[][] A, double[] b, int n) {
        this(A, b, n, OMEGA_DEFECTO);
    }

    /**
     * Constructor con factor de relajación explícito.
     *
     * @param omega factor de relajación, debe cumplir 0 &lt; ω &lt; 2
     * @throws IllegalArgumentException si ω está fuera del rango
     */
    public SOR(double[][] A, double[] b, int n, double omega) {
        this(A, b, n, omega, null);
    }

    /**
     * Constructor con trazado (ω por defecto).
     *
     * @param traza traza a ir llenando paso a paso, o {@code null} para
     *              ejecutar sin trazado
     */
    public SOR(double[][] A, double[] b, int n, Traza traza) {
        this(A, b, n, OMEGA_DEFECTO, traza);
    }

    /**
     * Constructor completo: ω explícito y trazado.
     *
     * @param omega factor de relajación, debe cumplir 0 &lt; ω &lt; 2
     * @param traza traza a ir llenando paso a paso, o {@code null} para
     *              ejecutar sin trazado
     * @throws IllegalArgumentException si ω está fuera del rango
     */
    public SOR(double[][] A, double[] b, int n, double omega, Traza traza) {
        super(A, b, n, traza);
        if (omega <= 0.0 || omega >= 2.0) {
            throw new IllegalArgumentException(
                    "SOR requiere 0 < ω < 2, recibido: " + omega);
        }
        this.omega = omega;
    }

    /**
     * @return el factor de relajación con el que se ejecuta este resolutor
     */
    public double getOmega() {
        return omega;
    }

    @Override
    public double[] resolver() {
        // 0. Precondición: la diagonal no puede tener ceros (se divide por ella)
        for (int i = 0; i < n; i++) {
            if (A[i][i] == 0.0) {
                throw new IllegalArgumentException(
                        "SOR requiere que ningún elemento de la diagonal sea cero (A[" + i + "][" + i + "] = 0).");
            }
        }

        iteraciones = 0;
        errorFinal = Double.NaN;
        convergio = false;

        // 1. Aproximación inicial x0 = vector cero
        double[] x = new double[n];
        if (traza != null) {
            traza.agregar(new Paso(0, "Aproximación inicial x0 = (0, ..., 0)", null, x, Double.NaN));
        }

        // 2. Bucle principal: hasta converger o agotar MAX_ITERACIONES
        for (int k = 1; k <= MAX_ITERACIONES; k++) {
            double delta = 0.0;

            // 2a. Mismo esquema in-place que Gauss-Seidel, con el paso de
            //     relajación encima: j < i lee valores NUEVOS, j > i VIEJOS.
            for (int i = 0; i < n; i++) {
                double suma = 0.0;
                for (int j = 0; j < n; j++) {
                    if (j != i) {
                        suma += A[i][j] * x[j];
                    }
                }
                double gs = (b[i] - suma) / A[i][i];   // despeje (paso de Gauss-Seidel)
                double xNuevo = x[i] + omega * (gs - x[i]);  // relajación

                // Error de esta componente: se mide ANTES de pisar x[i]
                delta = Math.max(delta, Math.abs(xNuevo - x[i]));

                x[i] = xNuevo;  // ← in-place, como Gauss-Seidel
            }

            iteraciones = k;
            errorFinal = delta;

            if (traza != null) {
                // Paso copia x por dentro: el registro queda intacto
                // aunque x siga mutándose en la siguiente iteración.
                traza.agregar(new Paso(k, "Iteración " + k + " (ω = " + omega + ")", null, x, delta));
            }

            // 2b. Criterio de paro: la corrección ya no mueve la solución
            if (delta < TOLERANCIA) {
                convergio = true;
                break;
            }
        }

        // Si salió por agotar MAX_ITERACIONES, convergio queda en false
        // y se retorna la última aproximación obtenida.
        return x;
    }

    @Override
    public int getIteraciones() {
        return iteraciones;
    }

    @Override
    public double getErrorFinal() {
        return errorFinal;
    }

    @Override
    public boolean convergio() {
        return convergio;
    }
}
