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

    private static final double TOLERANCIA = 1e-7;
    private static final int MAX_ITERACIONES = 1000;

    private final double[] x0Inicial;
    private int iteraciones;
    private double errorFinal;
    private boolean convergio;

    public MejoramientoIterativo(double[][] A, double[] b, int n) {
        this(A, b, null, n, null);
    }

    public MejoramientoIterativo(double[][] A, double[] b, int n, Traza traza) {
        this(A, b, null, n, traza);
    }

    public MejoramientoIterativo(double[][] A, double[] b, double[] x0, int n) {
        this(A, b, x0, n, null);
    }

    public MejoramientoIterativo(double[][] A, double[] b, double[] x0, int n, Traza traza) {
        super(A, b, n, traza);
        this.x0Inicial = (x0 != null) ? x0.clone() : null;
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

        if (normaR <= TOLERANCIA) {
            convergio = true;
            return x;
        }

        // 3. Iteraciones de corrección: mientras ||r|| > ε
        for (int k = 1; k <= MAX_ITERACIONES; k++) {
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

            if (normaR <= TOLERANCIA) {
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
