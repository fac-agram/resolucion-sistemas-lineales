package unsa.sistemas.model.iterative;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.direct.EliminacionGaussiana;
import unsa.sistemas.model.matrix.Normas;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

/**
 * Método de Mejoramiento Iterativo (refinamiento iterativo de soluciones).
 *
 * <p>Algoritmo:
 * <ul>
 *   <li>Aproximación inicial: {@code x0} (dado o calculado con Eliminación Gaussiana).</li>
 *   <li>Residuo inicial: {@code r = b - A * x0}.</li>
 *   <li>Mientras {@code ||r|| > ε}:
 *     <ol>
 *       <li>Calcular corrección {@code z} resolviendo {@code A * z = r} mediante Gauss.</li>
 *       <li>Actualizar {@code x1 = x0 + z}.</li>
 *       <li>{@code x0 = x1}.</li>
 *       <li>Recalcular residuo: {@code r = b - A * x0}.</li>
 *     </ol>
 *   </li>
 * </ul>
 * </p>
 */
public class MejoramientoIterativo extends Resolutor implements ResolutorIterativo {

    /** Tolerancia sobre la norma infinito del residuo (criterio {@code <=}). */
    private final double tolerancia;

    /** Cota máxima de iteraciones de mejoramiento antes de detenerse sin converger. */
    private final int maxIteraciones;

    private final double[] x0Inicial;
    private int iteraciones;
    private double errorFinal;
    private boolean convergio;

    /**
     * @param A matriz de coeficientes n x n
     * @param b vector de términos independientes
     * @param n orden del sistema
     */
    public MejoramientoIterativo(double[][] A, double[] b, int n) {
        this(A, b, null, n, Parametros.defecto(), null);
    }

    /**
     * Constructor con trazado (parámetros por defecto, x0 = Gauss).
     *
     * <p><b>Trampa de aridad:</b> {@code new MejoramientoIterativo(A, b, n, null)}
     * queda ambiguo entre {@link Traza} y {@link Parametros}; pase siempre el
     * tipo explícito. No existe ningún uso así en el proyecto.</p>
     *
     * @param traza traza a ir llenando paso a paso, o {@code null} sin trazado
     */
    public MejoramientoIterativo(double[][] A, double[] b, int n, Traza traza) {
        this(A, b, null, n, Parametros.defecto(), traza);
    }

    /**
     * Constructor con parámetros ajustables (x0 = Gauss).
     *
     * @param parametros tolerancia y cota de iteraciones a usar (ω se ignora)
     */
    public MejoramientoIterativo(double[][] A, double[] b, int n, Parametros parametros) {
        this(A, b, null, n, parametros, null);
    }

    /**
     * Constructor completo con parámetros ajustables y trazado (x0 = Gauss).
     *
     * @param parametros tolerancia y cota de iteraciones a usar (ω se ignora)
     * @param traza      traza a ir llenando paso a paso, o {@code null} sin trazado
     */
    public MejoramientoIterativo(double[][] A, double[] b, int n, Parametros parametros, Traza traza) {
        this(A, b, null, n, parametros, traza);
    }

    /**
     * @param A  matriz de coeficientes n x n
     * @param b  vector de términos independientes
     * @param x0 aproximación inicial, o {@code null} para calcularla con Gauss
     * @param n  orden del sistema
     */
    public MejoramientoIterativo(double[][] A, double[] b, double[] x0, int n) {
        this(A, b, x0, n, Parametros.defecto(), null);
    }

    /**
     * Constructor con aproximación inicial, trazado y parámetros por defecto.
     *
     * @param x0    aproximación inicial, o {@code null} para calcularla con Gauss
     * @param traza traza a ir llenando paso a paso, o {@code null} sin trazado
     */
    public MejoramientoIterativo(double[][] A, double[] b, double[] x0, int n, Traza traza) {
        this(A, b, x0, n, Parametros.defecto(), traza);
    }

    /**
     * Constructor completo: semilla opcional, parámetros ajustables y trazado.
     *
     * @param x0         aproximación inicial, o {@code null} para calcularla con Gauss
     * @param parametros tolerancia y cota de iteraciones a usar (ω se ignora)
     * @param traza      traza a ir llenando paso a paso, o {@code null} sin trazado
     */
    private MejoramientoIterativo(double[][] A, double[] b, double[] x0, int n,
                                  Parametros parametros, Traza traza) {
        super(A, b, n, traza);
        this.x0Inicial = (x0 != null) ? x0.clone() : null;
        this.tolerancia = parametros.epsilon();
        this.maxIteraciones = parametros.maxIteraciones();
    }

    @Override
    public double[] resolver() {
        iteraciones = 0;
        convergio = false;
        errorFinal = Double.NaN;

        // 1. Obtener solución inicial x0
        double[] x;
        if (x0Inicial != null) {
            x = x0Inicial.clone();
        } else {
            EliminacionGaussiana solver = new EliminacionGaussiana(A, b, n);
            x = solver.resolver();
            if (x == null) {
                throw new ArithmeticException("No se pudo calcular la aproximación inicial por Gauss.");
            }
        }

        if (traza != null) {
            traza.agregar(new Paso(0, "Aproximación inicial x0", null, x.clone(), Double.NaN));
        }

        // 2. Calcular residuo r = b - A * x0
        double[] r = calcularResiduo(x);
        double normaR = Normas.normaInfinito(r);
        errorFinal = normaR;

        if (normaR <= tolerancia) {
            convergio = true;
            return x;
        }

        // 3. Iteraciones de corrección: mientras ||r|| > ε
        for (int k = 1; k <= maxIteraciones; k++) {
            iteraciones = k;

            // Sea z = Gauss(A, r, n)
            EliminacionGaussiana solverResiduo = new EliminacionGaussiana(A, r, n);
            double[] z = solverResiduo.resolver();
            if (z == null) {
                throw new ArithmeticException("No se pudo resolver el sistema de corrección para el residuo.");
            }

            // Sea x1 = x0 + z
            double[] x1 = new double[n];
            for (int i = 0; i < n; i++) {
                x1[i] = x[i] + z[i];
            }

            // Sea x0 = x1
            x = x1;

            // Sea r = b - A * x0
            r = calcularResiduo(x);
            normaR = Normas.normaInfinito(r);
            errorFinal = normaR;

            if (traza != null) {
                traza.agregar(new Paso(k, "Iteración " + k + " (corrección)", null, x.clone(), normaR));
            }

            if (normaR <= tolerancia) {
                convergio = true;
                break;
            }
        }

        return x;
    }

    private double[] calcularResiduo(double[] v) {
        double[] res = new double[n];
        for (int i = 0; i < n; i++) {
            double ax = 0.0;
            for (int j = 0; j < n; j++) {
                ax += A[i][j] * v[j];
            }
            res[i] = b[i] - ax;
        }
        return res;
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
