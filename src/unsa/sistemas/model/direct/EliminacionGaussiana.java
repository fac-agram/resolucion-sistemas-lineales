package unsa.sistemas.model.direct;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

public class EliminacionGaussiana extends Resolutor implements ResolutorDirecto {

    public EliminacionGaussiana(double[][] A, double[] b, int n) {
        super(A, b, n);
    }

    public EliminacionGaussiana(
            double[][] A, double[] b, int n, Traza traza) {
        super(A, b, n, traza);
    }

    @Override
    public double[] resolver() {

        // 1. Construir matriz aumentada
        double[][] G = new double[n][n + 1];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                G[i][j] = A[i][j];
            }

            G[i][n] = b[i];
        }

        // Guardar matriz inicial
        if (traza != null) {
            traza.agregar(new Paso(
                0,
                "Matriz aumentada inicial",
                G,
                null,
                Double.NaN
            ));
        }

        int indice = 1;

        // 2. Eliminación Gaussiana
        for (int k = 0; k < n; k++) {

            // Pivoteo simple
            if (!pivoteo_simple(G, k)) {
                throw new ArithmeticException(
                    "No existe un pivote válido en la columna " + k
                );
            }

            double pivote = G[k][k];

            for (int i = k + 1; i < n; i++) {

                double m = G[i][k] / pivote;

                G[i][k] = 0;

                for (int j = k + 1; j <= n; j++) {

                    G[i][j] = G[i][j] - m * G[k][j];
                }

                // Registrar fotografía
                if (traza != null) {

                    traza.agregar(new Paso(
                        indice++,
                        "F" + (i + 1) + " ← F" + (i + 1)
                        + " - (" + m + ") F" + (k + 1),
                        G,
                        null,
                        Double.NaN
                    ));
                }
            }
        }

        // 3. Sustitución regresiva
        double[] x = sust_Regresiva(G);

        // 4. Registrar solución
        if (traza != null) {

            traza.agregar(new Paso(
                indice,
                "Solución por sustitución regresiva",
                G,
                x,
                Double.NaN
            ));
        }

        return x;
    }

    // Pivoteo simple
    private boolean pivoteo_simple(double[][] G, int k) {

        if (Math.abs(G[k][k]) > 1e-12) {
            return true;
        }

        for (int i = k + 1; i < n; i++) {

            if (Math.abs(G[i][k]) > 1e-12) {

                double[] aux = G[k];
                G[k] = G[i];
                G[i] = aux;

                if (traza != null) {
                    traza.agregar(new Paso(
                        -1,
                        "Intercambio F" + (k + 1)
                        + " ↔ F" + (i + 1),
                        G,
                        null,
                        Double.NaN
                    ));
                }

                return true;
            }
        }

        return false;
    }

    // Sustitución regresiva
    private double[] sust_Regresiva(double[][] G) {

        double[] x = new double[n];

        for (int i = n - 1; i >= 0; i--) {

            double suma = 0;

            for (int j = i + 1; j < n; j++) {
                suma += G[i][j] * x[j];
            }

            if (Math.abs(G[i][i]) <= 1e-12) {
                throw new ArithmeticException(
                    "No se puede realizar la sustitución regresiva."
                );
            }

            x[i] = (G[i][n] - suma) / G[i][i];
        }

        return x;
    }
}