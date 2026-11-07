package unsa.sistemas.model.iterative;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

/**
 * Método de relajación simple para resolver el sistema Ax = b.
 *
 * <p><b>Idea (según el apunte del curso):</b> a diferencia de
 * Gauss-Seidel/SOR —que iteran fila por fila de forma ordenada— este
 * método trabaja directamente sobre el <b>vector residual</b>
 * {@code r = b − A·x} y en cada paso actualiza <b>únicamente</b> el
 * componente cuyo residuo tiene el mayor valor absoluto:</p>
 *
 * <ol>
 *   <li>Calcular el residual {@code r = b − A·x}.</li>
 *   <li>Si {@code ‖r‖ < ε} → convergió, terminar.</li>
 *   <li>Buscar el índice {@code m} del componente con mayor {@code |r_i|}.</li>
 *   <li>Actualizar solo esa posición: {@code x[m] += r[m] / a_mm}.</li>
 *   <li>Repetir.</li>
 * </ol>
 *
 * <p><b>Por qué funciona el paso 4:</b> al mover {@code x[m]} por
 * {@code Δ = r[m]/a_mm}, el componente m del residual nuevo queda
 * exactamente en cero: {@code r'[m] = r[m] − Δ·a_mm = 0}. Cada iteración
 * "apaga" el componente más grande; los demás cambian por las columnas
 * de la matriz y el proceso sigue hasta que todo el residual cae bajo
 * la tolerancia.</p>
 *
 * <p><b>Norma:</b> se usa la norma infinito (máximo valor absoluto del
 * residual), la misma que define al pivote {@code m} — un solo barrido
 * por iteración obtiene índice y norma a la vez.</p>
 *
 * <p><b>Precondición:</b> ningún elemento de la diagonal puede ser cero
 * (se divide por {@code a_mm}). La norma de error reportada es
 * {@code ‖r‖∞} del último residuo medido.</p>
 */
public class RelajacionSimple extends Resolutor implements ResolutorIterativo {

    /** Tolerancia sobre la norma infinito del residuo. */
    private static final double TOLERANCIA = 1e-7;

    /** Cota máxima de actualizaciones antes de detenerse sin converger. */
    private static final int MAX_ITERACIONES = 1000;

    private int iteraciones;
    private double errorFinal;
    private boolean convergio;

    /**
     * @param A matriz de coeficientes n x n
     * @param b vector de términos independientes
     * @param n orden del sistema
     */
    public RelajacionSimple(double[][] A, double[] b, int n) {
        super(A, b, n);
    }

    /**
     * Constructor con trazado.
     *
     * @param traza traza a ir llenando paso a paso, o {@code null} para
     *              ejecutar sin trazado
     */
    public RelajacionSimple(double[][] A, double[] b, int n, Traza traza) {
        super(A, b, n, traza);
    }

    @Override
    public double[] resolver() {
        // 0. Precondición: la diagonal no puede tener ceros (se divide por a_mm)
        for (int i = 0; i < n; i++) {
            if (A[i][i] == 0.0) {
                throw new IllegalArgumentException(
                        "Relajación simple requiere que ningún elemento de la diagonal sea cero (A[" + i + "][" + i + "] = 0).");
            }
        }

        iteraciones = 0;
        errorFinal = Double.NaN;
        convergio = false;

        // 1. x0 = vector cero  =>  r = b − A·x0 = b
        double[] x = new double[n];
        double[] r = b.clone();
        if (traza != null) {
            traza.agregar(new Paso(0, "Residual inicial r = b − A·x0 = b", null, x.clone(), Double.NaN));
        }

        // 2. Bucle: medir residuo → actualizar el componente máximo → repetir
        for (int k = 1; k <= MAX_ITERACIONES; k++) {

            // 2a. Un solo barrido: norma infinito del residual Y su índice máximo
            int m = 0;
            double norma = Math.abs(r[0]);
            for (int i = 1; i < n; i++) {
                if (Math.abs(r[i]) > norma) {
                    norma = Math.abs(r[i]);
                    m = i;
                }
            }
            errorFinal = norma;

            // 2b. Criterio de paro: todo el residual bajo la tolerancia
            if (norma < TOLERANCIA) {
                convergio = true;
                break;
            }

            // 2c. Actualizar SOLO la posición m: x[m] += r[m] / a_mm
            double delta = r[m] / A[m][m];
            x[m] += delta;

            // 2d. Mantener el residual al día sin recalcular A·x completo:
            //     x cambió solo en m por delta, así que r' = r − delta·A[:,m]
            //     (equivalente a r = b − A·x desde cero, en O(n) y no O(n²))
            for (int i = 0; i < n; i++) {
                r[i] -= delta * A[i][m];
            }

            iteraciones = k;

            if (traza != null) {
                // error del paso = norma del residual DESPUÉS de la actualización
                double normaDespues = 0.0;
                for (int i = 0; i < n; i++) {
                    normaDespues = Math.max(normaDespues, Math.abs(r[i]));
                }
                traza.agregar(new Paso(k,
                        "x[" + (m + 1) + "] += r[" + (m + 1) + "]/a" + (m + 1) + (m + 1)
                                + " (Δ = " + String.format("%.3e", delta) + ")",
                        null, x.clone(), normaDespues));
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
