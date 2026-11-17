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

        // PRIMERA ETAPA: eliminar por columnas con normalización (diagonal unitaria)
        for (int k = 0; k < n; k++) {

            // Pivoteo simple: si hay swap, registra el paso con indice y lo incrementa
            indice = pivoteo_simple(G, k, indice);

            if (indice < 0) {
                throw new ArithmeticException(
                    "No existe un pivote válido en la columna " + k
                );
            }

            // Normalizar la fila pivote (diagonal unitaria).
            // Si el pivote ya es 1 (|pivote - 1| <= EPS) la division es
            // no-op: se omite la aritmetica y el paso cosmetico, igual que
            // se omiten las eliminaciones con factor <= EPS mas abajo.
            // El indice solo se consume cuando hay paso registrado, asi la
            // numeracion sigue siendo monotonica y sin huecos.
            double pivote = G[k][k];

            if (Math.abs(pivote) <= EPS) {
                throw new ArithmeticException("Pivote nulo en fila " + k);
            }

            if (Math.abs(pivote - 1.0) > EPS) {
                for (int j = k; j <= n; j++) {
                    G[k][j] /= pivote;
                }

                registrarPaso(indice++, "F" + (k + 1) + " ← F" + (k + 1)
                    + " / " + pivote, G);
            }

            // Eliminar la columna k en todas las filas (i ≠ k)
            for (int i = 0; i < n; i++) {

                if (i == k) continue;

                double m = G[i][k];

                if (Math.abs(m) <= EPS) continue;

                G[i][k] = 0;

                for (int j = k + 1; j <= n; j++) {
                    G[i][j] -= m * G[k][j];
                }

                registrarPaso(
                    indice++,
                    "F" + (i + 1) + " ← F" + (i + 1)
                    + " - (" + m + ") F" + (k + 1),
                    G
                );
            }
        }

        // OBTENER LAS SOLUCIONES (diagonal ya es 1)
        double[] x = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = G[i][n];
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
    private int pivoteo_simple(
            double[][] G, int k, int indice) {

        if (Math.abs(G[k][k]) > EPS) {
            return indice;
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

                return indice + 1;
            }
        }

        return -1;
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
