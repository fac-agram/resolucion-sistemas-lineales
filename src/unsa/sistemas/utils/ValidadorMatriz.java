package unsa.sistemas.utils;

import unsa.sistemas.model.matrix.Matriz;

public class ValidadorMatriz {

    private static final double TOLERANCIA = 1e-9;

    /**
     * Valida si la matriz no es nula y es cuadrada (filas == columnas).
     */
    public static boolean esCuadrada(Matriz matriz) {
        return matriz != null && matriz.getF() > 0 && matriz.getF() == matriz.getC();
    }

    /**
     * Valida que las dimensiones del sistema A * x = b sean compatibles:
     * A debe ser cuadrada y la longitud del vector b debe coincidir con sus filas.
     */
    public static boolean esDimensionValida(Matriz matriz, double[] b) {
        if (!esCuadrada(matriz) || b == null) {
            return false;
        }
        return matriz.getF() == b.length;
    }

    /**
     * Comprueba si algún elemento en la diagonal principal es prácticamente nulo (cercano a 0).
     * Requisito de alerta para métodos iterativos (Jacobi, Gauss-Seidel) donde se divide por A[i][i].
     */
    public static boolean tieneCerosEnDiagonal(Matriz matriz) {
        if (!esCuadrada(matriz)) {
            return true;
        }
        int n = matriz.getF();
        for (int i = 0; i < n; i++) {
            if (Math.abs(matriz.getEleMatriz(i, i)) < TOLERANCIA) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valida si una matriz es simétrica (A = A^T).
     * Requisito fundamental para el Método de Cholesky.
     */
    public static boolean esSimetrica(Matriz matriz) {
        if (!esCuadrada(matriz)) {
            return false;
        }

        int n = matriz.getF();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(matriz.getEleMatriz(i, j) - matriz.getEleMatriz(j, i)) > TOLERANCIA) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Valida si una matriz es estrictamente diagonal dominante por filas.
     * Condición suficiente de convergencia para los métodos de Jacobi y Gauss-Seidel:
     * |a_ii| > sum_{j != i} |a_ij| para todo i.
     */
    public static boolean esDiagonalDominante(Matriz matriz) {
        if (!esCuadrada(matriz)) {
            return false;
        }

        int n = matriz.getF();
        for (int i = 0; i < n; i++) {
            double sumaFila = 0.0;
            double elementoDiagonal = Math.abs(matriz.getEleMatriz(i, i));

            for (int j = 0; j < n; j++) {
                if (i != j) {
                    sumaFila += Math.abs(matriz.getEleMatriz(i, j));
                }
            }

            if (elementoDiagonal <= sumaFila) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validación de matriz definida positiva (Requisito fundamental para Cholesky).
     * Aplica el Criterio de Sylvester:
     * 1. La matriz debe ser simétrica (A = A^T).
     * 2. Todos los menores principales líderes (submatrices de orden 1 hasta n)
     *    deben tener determinantes estrictamente positivos (Delta_k > 0).
     */
    public static boolean esDefinidaPositiva(Matriz matriz) {
        if (!esSimetrica(matriz)) {
            return false;
        }

        int n = matriz.getF();
        for (int k = 1; k <= n; k++) {
            Matriz submatriz = matriz.submatrizPrincipal(k);
            if (submatriz.determinante() <= TOLERANCIA) {
                return false;
            }
        }
        return true;
    }

    /**
     * Comprueba si la matriz es apta para factorización LU / Crout sin pivoteo.
     * Requiere que todos sus menores principales líderes sean no singulares:
     * det(A_k) != 0 para k = 1, 2, ..., n.
     */
    public static boolean esAptaParaLU(Matriz matriz) {
        if (!esCuadrada(matriz)) {
            return false;
        }

        int n = matriz.getF();
        for (int k = 1; k <= n; k++) {
            Matriz submatriz = matriz.submatrizPrincipal(k);
            if (Math.abs(submatriz.determinante()) < TOLERANCIA) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valida si la matriz no es singular (det(A) != 0), lo que asegura solución única.
     */
    public static boolean esNoSingular(Matriz matriz) {
        if (!esCuadrada(matriz)) {
            return false;
        }
        return Math.abs(matriz.determinante()) > TOLERANCIA;
    }
}
