
package unsa.sistemas.model.direct;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

public class Crout extends Resolutor implements ResolutorDirecto {

    private static final double EPS = 1e-12;

    public Crout(double[][] A, double[] b, int n) {
        super(A, b, n);
    }

    public Crout(double[][] A, double[] b, int n, Traza traza) {
        super(A, b, n, traza);
    }

    @Override
    public double[] resolver() {

        double[][] L = new double[n][n];
        double[][] U = new double[n][n];

        // Inicializar diagonal de U con unos
        for (int i = 0; i < n; i++) {
            U[i][i] = 1;
        }

        int indice = 0;

        registrarPaso(
            indice++,
            "Matrices L y U iniciales",
            unirLU(L, U)
        );

        // FACTORIZACIÓN DE CROUT
        for (int j = 0; j < n; j++) {

            // Calcular columna j de L
            for (int i = j; i < n; i++) {

                double suma = 0;

                for (int k = 0; k < j; k++) {
                    suma += L[i][k] * U[k][j];
                }

                L[i][j] = A[i][j] - suma;
            }

            registrarPaso(
                indice++,
                "Calcular columna " + (j + 1) + " de L",
                unirLU(L, U)
            );

            // Verificar pivote
            if (Math.abs(L[j][j]) <= EPS) {
                throw new ArithmeticException(
                    "Pivote nulo en Crout. No se puede continuar sin pivoteo."
                );
            }

            // Calcular fila j de U
            for (int i = j + 1; i < n; i++) {

                double suma = 0;

                for (int k = 0; k < j; k++) {
                    suma += L[j][k] * U[k][i];
                }

                U[j][i] = (A[j][i] - suma) / L[j][j];
            }

            registrarPaso(
                indice++,
                "Calcular fila " + (j + 1) + " de U",
                unirLU(L, U)
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
                unirLU(L, U),
                y,
                Double.NaN
            ));
        }

        // SUSTITUCIÓN REGRESIVA: U x = y
        double[] x = new double[n];

        for (int i = n - 1; i >= 0; i--) {

            double suma = 0;

            for (int j = i + 1; j < n; j++) {
                suma += U[i][j] * x[j];
            }

            if (Math.abs(U[i][i]) <= EPS) {
                throw new ArithmeticException(
                    "No se puede realizar la sustitución regresiva."
                );
            }

            x[i] = (y[i] - suma) / U[i][i];
        }

        if (traza != null) {
            traza.agregar(new Paso(
                indice,
                "Solución final",
                unirLU(L, U),
                x,
                0
            ));
        }

        return x;
    }

    // UNIR LAS MATRICES L Y U PARA MOSTRARLAS EN LA TRAZA
    private double[][] unirLU(double[][] L, double[][] U) {

        double[][] resultado = new double[n][2 * n];

        for (int i = 0; i < n; i++) {

            for (int j = 0; j < n; j++) {
                resultado[i][j] = L[i][j];
                resultado[i][j + n] = U[i][j];
            }
        }

        return resultado;
    }

    // REGISTRAR PASOS
    private void registrarPaso(
            int indice, String operacion, double[][] matriz) {

        if (traza != null) {
            traza.agregar(new Paso(
                indice,operacion,matriz, null,Double.NaN
            ));
        }
    }
}
