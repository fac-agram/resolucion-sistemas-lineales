

package unsa.sistemas.model.direct;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

public class GaussJordan extends Resolutor implements ResolutorDirecto {

    private static final double EPS = 1e-12;

    public GaussJordan(double[][] A, double[] b, int n) {
        super(A, b, n);
    }

    public GaussJordan(
            double[][] A, double[] b, int n, Traza traza) {
        super(A, b, n, traza);
    }

    @Override
    public double[] resolver() {

        // Construir matriz aumentada
        double[][] G = new double[n][n + 1];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                G[i][j] = A[i][j];
            }

            G[i][n] = b[i];
        }

        int indice = 0;

        // Registrar matriz inicial
        registrarPaso(indice++, "Matriz aumentada inicial", G);

        // PRIMERA ETAPA: ceros debajo de los pivotes
        for (int k = 0; k < n; k++) {

            if (!pivoteo_simple(G, k, indice)) {
                throw new ArithmeticException(
                    "No existe un pivote válido."
                );
            }

            double pivote = G[k][k];

            for (int i = k + 1; i < n; i++) {

                double m = G[i][k] / pivote;

                G[i][k] = 0;

                for (int j = k + 1; j <= n; j++) {
                    G[i][j] = G[i][j] - m * G[k][j];
                }

                registrarPaso(
                    indice++,
                    "F" + (i + 1) + " ← F" + (i + 1)
                    + " - (" + m + ") F" + (k + 1),
                    G
                );
            }
        }

        // SEGUNDA ETAPA: ceros encima de los pivotes
        for (int k = n - 1; k >= 0; k--) {

            double pivote = G[k][k];

            if (Math.abs(pivote) <= EPS) {
                throw new ArithmeticException("Pivote nulo.");
            }

            for (int i = k - 1; i >= 0; i--) {

                double m = G[i][k] / pivote;

                G[i][k] = 0;

                for (int j = k + 1; j <= n; j++) {
                    G[i][j] = G[i][j] - m * G[k][j];
                }

                registrarPaso(
                    indice++,
                    "F" + (i + 1) + " ← F" + (i + 1)
                    + " - (" + m + ") F" + (k + 1),
                    G
                );
            }
        }

        // OBTENER LAS SOLUCIONES
        double[] x = new double[n];

        for (int i = 0; i < n; i++) {

            if (Math.abs(G[i][i]) <= EPS) {
                throw new ArithmeticException(
                    "No se puede obtener la solución."
                );
            }

            x[i] = G[i][n] / G[i][i];
        }

        // Registrar solución final
        if (traza != null) {
            traza.agregar(new Paso(
                indice,
                "Solución final",
                G,
                x,
                0
            ));
        }

        return x;
    }

    // MÉTODO DE PIVOTEO SIMPLE
    private boolean pivoteo_simple(
            double[][] G, int k, int indice) {

        if (Math.abs(G[k][k]) > EPS) {
            return true;
        }

        for (int i = k + 1; i < n; i++) {

            if (Math.abs(G[i][k]) > EPS) {

                // Intercambiar filas
                double[] aux = G[k];
                G[k] = G[i];
                G[i] = aux;

                registrarPaso(
                    indice,
                    "Intercambio F" + (k + 1)
                    + " ↔ F" + (i + 1),
                    G
                );

                return true;
            }
        }

        return false;
    }

    // MÉTODO PARA REGISTRAR LOS PASOS
    private void registrarPaso(
            int indice, String operacion, double[][] G) {

        if (traza != null) {
            traza.agregar(new Paso(
                indice,
                operacion,
                G,
                null,
                Double.NaN
            ));
        }
    }
}
