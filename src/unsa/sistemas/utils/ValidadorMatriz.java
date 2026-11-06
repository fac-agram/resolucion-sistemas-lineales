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
     * Nota: una matriz no cuadrada devuelve {@code true} (se trata como no apta/usable por todos los llamadores).
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
     * <p>Para una matriz simétrica, los pivotes de la eliminación sin pivoteo
     * cumplen {@code p_k = Δ_k / Δ_{k-1}}. Por tanto {@code p_k > 0} para
     * todo k es equivalente al Criterio de Sylvester {@code Δ_k > 0} para
     * todo k, pero en O(n^3) en lugar de O(n^4).</p>
     * <p>Requisitos: la matriz debe ser simétrica (A = A^T) y todos los
     * pivotes deben ser estrictamente positivos.</p>
     */
    public static boolean esDefinidaPositiva(Matriz matriz) {
        if (!esSimetrica(matriz)) {
            return false;
        }

        double[] pivotes = pivotesSinPivoteo(matriz);
        if (pivotes == null) {
            return false;
        }
        for (double pivote : pivotes) {
            if (pivote <= TOLERANCIA) {
                return false;
            }
        }
        return true;
    }

    /**
     * Comprueba si la matriz es apta para factorización LU / Crout sin pivoteo.
     * <p>Una matriz admite factorización LU sin pivoteo si y solo si todos
     * sus menores principales líderes son no nulos; esto es equivalente a
     * que la eliminación sin pivoteo no encuentre ningún pivote nulo.
     * La verificación se realiza en O(n^3) mediante
     * {@link #pivotesSinPivoteo(Matriz)} en lugar de calcular los k
     * determinantes de las submatrices principales.</p>
     *
     * @return {@code false} si la matriz no es cuadrada o algún pivote es
     *          prácticamente nulo; {@code true} en caso contrario
     */
    public static boolean esAptaParaLU(Matriz matriz) {
        if (!esCuadrada(matriz)) {
            return false;
        }
        return pivotesSinPivoteo(matriz) != null;
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

    /**
     * Eliminación gaussiana SIN pivoteo.
     * Devuelve los pivotes de la diagonal en un arreglo de longitud n.
     * Si en alguna etapa el pivote es prácticamente nulo, devuelve null
     * (la factorización no existe sin pivoteo).
     *
     * <p>Complejidad: O(n^3). La matriz de entrada no se modifica; se
     * trabaja sobre una copia interna. Precondición: la matriz debe ser
     * cuadrada (la garantizan los llamadores).</p>
     *
     * @param matriz matriz cuadrada de entrada
     * @return los pivotes de la diagonal, o {@code null} si la eliminación
     *          falla por un pivote prácticamente nulo
     */
    private static double[] pivotesSinPivoteo(Matriz matriz) {
        int n = matriz.getF();
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = matriz.getEleMatriz(i, j);
            }
        }

        for (int k = 0; k < n; k++) {
            if (Math.abs(a[k][k]) < TOLERANCIA) {
                return null;
            }
            for (int i = k + 1; i < n; i++) {
                double factor = a[i][k] / a[k][k];
                a[i][k] = 0.0;
                for (int j = k + 1; j < n; j++) {
                    a[i][j] -= factor * a[k][j];
                }
            }
        }

        double[] pivotes = new double[n];
        for (int i = 0; i < n; i++) {
            pivotes[i] = a[i][i];
        }
        return pivotes;
    }
}
