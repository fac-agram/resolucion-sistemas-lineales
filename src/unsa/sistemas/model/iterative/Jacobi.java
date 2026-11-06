package unsa.sistemas.model.iterative;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

/**
 * Método iterativo de Jacobi para resolver el sistema Ax = b.
 *
 * <p><b>Idea:</b> despejar la diagonal en cada fila. Partiendo de
 * {@code x0 = 0}, cada componente nueva se calcula con los valores
 * <i>viejos</i> del resto del vector — por eso se escribe en
 * {@code xNuevo} y recién después se reemplaza {@code x}.</p>
 *
 * <p><b>Precondición:</b> ningún elemento de la diagonal puede ser cero
 * (se divide por {@code A[i][i]}). La dominancia diagonal es condición
 * <i>suficiente</i> —no necesaria— de convergencia: sin ella el método
 * puede no converger; en ese caso se devuelve la última aproximación con
 * {@link #convergio()} {@code false}.</p>
 */
public class Jacobi extends Resolutor implements ResolutorIterativo {

    /** Tolerancia absoluta sobre la diferencia entre iteraciones sucesivas. */
    private static final double TOLERANCIA = 1e-7;

    /** Cota máxima de iteraciones antes de detenerse sin converger. */
    private static final int MAX_ITERACIONES = 1000;

    private int iteraciones;
    private double errorFinal;
    private boolean convergio;

    /**
     * @param A matriz de coeficientes n x n
     * @param b vector de términos independientes
     * @param n orden del sistema
     */
    public Jacobi(double[][] A, double[] b, int n) {
        super(A, b, n);
    }

    /**
     * Constructor con trazado.
     *
     * @param traza traza a ir llenando paso a paso, o {@code null} para
     *              ejecutar sin trazado
     */
    public Jacobi(double[][] A, double[] b, int n, Traza traza) {
        super(A, b, n, traza);
    }

    @Override
    public double[] resolver() {
        // 0. Precondición: la diagonal no puede tener ceros (se divide por ella)
        for (int i = 0; i < n; i++) {
            if (A[i][i] == 0.0) {
                throw new IllegalArgumentException(
                        "Jacobi requiere que ningún elemento de la diagonal sea cero (A[" + i + "][" + i + "] = 0).");
            }
        }

        iteraciones = 0;
        errorFinal = Double.NaN;
        convergio = false;

        // 1. Aproximación inicial x0 = vector cero
        double[] x = new double[n];
        if (traza != null) {
            traza.agregar(new Paso(0, "Aproximación inicial x0 = (0, ..., 0)", null, x.clone(), Double.NaN));
        }

        // 2. Bucle principal: hasta converger o agotar MAX_ITERACIONES
        for (int k = 1; k <= MAX_ITERACIONES; k++) {
            double[] xNuevo = new double[n];

            // 2a. Cada componente i se despeja de la diagonal, usando los
            //     valores VIEJOS de x (por eso se escribe en un vector aparte)
            for (int i = 0; i < n; i++) {
                double suma = 0.0;
                for (int j = 0; j < n; j++) {
                    if (j != i) {
                        suma += A[i][j] * x[j];
                    }
                }
                xNuevo[i] = (b[i] - suma) / A[i][i];
            }

            // 2b. Error = norma infinito (máximo valor absoluto de la diferencia
            //     entre aproximaciones sucesivas)
            double delta = 0.0;
            for (int i = 0; i < n; i++) {
                delta = Math.max(delta, Math.abs(xNuevo[i] - x[i]));
            }

            x = xNuevo;
            iteraciones = k;
            errorFinal = delta;

            if (traza != null) {
                traza.agregar(new Paso(k, "Iteración " + k, null, x.clone(), delta));
            }

            // 2c. Criterio de paro: la corrección ya no mueve la solución
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
