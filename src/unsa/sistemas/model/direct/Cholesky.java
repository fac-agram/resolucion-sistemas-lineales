
package unsa.sistemas.model.direct;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

public class Cholesky extends Resolutor implements ResolutorDirecto {

    private static final double EPS = 1e-12;

    public Cholesky(double[][] A, double[] b, int n) {
        super(A, b, n);
    }

    public Cholesky(
            double[][] A, double[] b, int n, Traza traza) {
        super(A, b, n, traza);
    }

    @Override
    public double[] resolver() {

        // Verificar que A sea simétrica
        if (!esSimetrica()) {
            throw new IllegalArgumentException(
                "La matriz no es simétrica."
            );
        }

        double[][] L = new double[n][n];

        int indice = 0;

        registrarPaso(
            indice++,
            "Matriz L inicial",
            L
        );

        // FACTORIZACIÓN DE CHOLESKY: A = L * L^T
        for (int i = 0; i < n; i++) {

            for (int j = 0; j <= i; j++) {

                double suma = 0;

                for (int k = 0; k < j; k++) {
                    suma += L[i][k] * L[j][k];
                }

                if (i == j) {

                    double valor = A[i][i] - suma;

                    if (valor <= EPS) {
                        throw new ArithmeticException(
                            "La matriz no es definida positiva."
                        );
                    }

                    L[i][j] = Math.sqrt(valor);

                } else {

                    L[i][j] = (A[i][j] - suma) / L[j][j];
                }
            }

            registrarPaso(
                indice++,
                "Calcular fila " + (i + 1) + " de L",
                L
            );
        }

        // SUSTITUCIÓN PROGRESIVA: L y = b
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {

            double suma = 0;

            for (int j = 0; j < i; j++) {
                suma += L[i][j] * y[j];
            }

            if (Math.abs(L[i][i]) <= EPS) {
                throw new ArithmeticException(
                    "No se puede realizar la sustitución progresiva."
                );
            }

            y[i] = (b[i] - suma) / L[i][i];
        }

        if (traza != null) {
            traza.agregar(new Paso(
                indice++,
                "Resultado de sustitución progresiva (y)",
                L,
                y,
                Double.NaN
            ));
        }

        // SUSTITUCIÓN REGRESIVA: L^T x = y
        double[] x = new double[n];

        for (int i = n - 1; i >= 0; i--) {

            double suma = 0;

            for (int j = i + 1; j < n; j++) {
                suma += L[j][i] * x[j];
            }

            if (Math.abs(L[i][i]) <= EPS) {
                throw new ArithmeticException(
                    "No se puede realizar la sustitución regresiva."
                );
            }

            x[i] = (y[i] - suma) / L[i][i];
        }

        if (traza != null) {
            traza.agregar(new Paso(
                indice,
                "Solución final",
                L,
                x,
                0
            ));
        }

        return x;
    }

    // VERIFICAR SIMETRÍA
    private boolean esSimetrica() {

        for (int i = 0; i < n; i++) {

            for (int j = i + 1; j < n; j++) {

                if (Math.abs(A[i][j] - A[j][i]) > EPS) {
                    return false;
                }
            }
        }

        return true;
    }

    // REGISTRAR PASOS
    private void registrarPaso(
            int indice, String operacion, double[][] matriz) {

        if (traza != null) {
            traza.agregar(new Paso(indice,operacion,matriz,null,Double.NaN
            ));
        }
    }
}
