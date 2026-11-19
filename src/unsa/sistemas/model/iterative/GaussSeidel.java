package unsa.sistemas.model.iterative;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

/**
 * Método iterativo de Gauss-Seidel para resolver el sistema Ax = b.
 *
 * <p><b>Idea:</b> igual que Jacobi —despejar la diagonal en cada fila—
 * pero con una diferencia clave: cada componente se escribe <b>en el mismo
 * vector</b> (in-place), de modo que las filas siguientes ya usan los
 * valores <i>nuevos</i> calculados en esta misma iteración. Eso lo hace
 * converger generalmente más rápido que Jacobi.</p>
 *
 * <p><b>Precondición:</b> ningún elemento de la diagonal puede ser cero
 * (se divide por {@code A[i][i]}). La dominancia diagonal es condición
 * <i>suficiente</i> —no necesaria— de convergencia: sin ella el método
 * puede no converger; en ese caso se devuelve la última aproximación con
 * {@link #convergio()} {@code false}.</p>
 */
public class GaussSeidel extends Resolutor implements ResolutorIterativo {

    /** Tolerancia absoluta sobre la diferencia entre iteraciones sucesivas. */
    private final double tolerancia;

    /** Cota máxima de iteraciones antes de detenerse sin converger. */
    private final int maxIteraciones;

    private int iteraciones;
    private double errorFinal;
    private boolean convergio;

    /**
     * @param A matriz de coeficientes n x n
     * @param b vector de términos independientes
     * @param n orden del sistema
     */
    public GaussSeidel(double[][] A, double[] b, int n) {
        this(A, b, n, Parametros.defecto());
    }

    /**
     * Constructor con parámetros ajustables.
     *
     * @param parametros tolerancia y cota de iteraciones a usar (ω se ignora)
     */
    public GaussSeidel(double[][] A, double[] b, int n, Parametros parametros) {
        this(A, b, n, parametros, null);
    }

    /**
     * Constructor con trazado (parámetros por defecto).
     *
     * @param traza traza a ir llenando paso a paso, o {@code null} para
     *              ejecutar sin trazado
     */
    public GaussSeidel(double[][] A, double[] b, int n, Traza traza) {
        this(A, b, n, Parametros.defecto(), traza);
    }

    /**
     * Constructor completo: parámetros ajustables y trazado.
     *
     * <p><b>Trampa de aridad:</b> {@code new GaussSeidel(A, b, n, null)} queda
     * ambiguo entre {@link Traza} y {@link Parametros}; pase siempre el
     * tipo explícito ({@code (Traza) null} o un {@code Parametros}).
     * No existe ningún uso así en el proyecto.</p>
     *
     * @param parametros tolerancia y cota de iteraciones a usar (ω se ignora)
     * @param traza      traza a ir llenando paso a paso, o {@code null} para
     *                   ejecutar sin trazado
     */
    public GaussSeidel(double[][] A, double[] b, int n, Parametros parametros, Traza traza) {
        super(A, b, n, traza);
        this.tolerancia = parametros.epsilon();
        this.maxIteraciones = parametros.maxIteraciones();
    }

    @Override
    public double[] resolver() {
        // 0. Precondición: la diagonal no puede tener ceros (se divide por ella)
        for (int i = 0; i < n; i++) {
            if (A[i][i] == 0.0) {
                throw new IllegalArgumentException(
                        "Gauss-Seidel requiere que ningún elemento de la diagonal sea cero (A[" + i + "][" + i + "] = 0).");
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

        // 2. Bucle principal: hasta converger o agotar el máximo de iteraciones
        for (int k = 1; k <= maxIteraciones; k++) {
            double delta = 0.0;

            // 2a. Cada componente i se despeja de la diagonal y se escribe
            //     DIRECTO en x (sin xNuevo como Jacobi). Por eso en la suma:
            //       j < i  -> lee el valor NUEVO, ya calculado en esta iteración
            //       j > i  -> lee el valor VIEJO, todavía no actualizado
            for (int i = 0; i < n; i++) {
                double suma = 0.0;
                for (int j = 0; j < n; j++) {
                    if (j != i) {
                        suma += A[i][j] * x[j];
                    }
                }
                double xNuevo = (b[i] - suma) / A[i][i];

                // Error de esta componente: se mide ANTES de pisar x[i]
                delta = Math.max(delta, Math.abs(xNuevo - x[i]));

                x[i] = xNuevo;  // ← in-place: la diferencia con Jacobi
            }

            iteraciones = k;
            errorFinal = delta;

            if (traza != null) {
                // Paso copia x por dentro: el registro queda intacto
                // aunque x siga mutándose en la siguiente iteración.
                traza.agregar(new Paso(k, "Iteración " + k, null, x, delta));
            }

            // 2b. Criterio de paro: la corrección ya no mueve la solución
            if (delta < tolerancia) {
                convergio = true;
                break;
            }
        }

        // Si salió por agotar el máximo de iteraciones, convergio queda en false
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
