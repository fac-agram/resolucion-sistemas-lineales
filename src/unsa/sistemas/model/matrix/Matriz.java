package unsa.sistemas.model.matrix;

public class Matriz {

    /**
     * Umbral de singularidad de pivote utilizado en {@link #determinante()}.
     * Si el pivote máximo de una etapa de la eliminación es menor que este
     * valor, la matriz se considera singular y el determinante se devuelve
     * como 0.0.
     *
     * <p>Es una tolerancia de <b>singularidad de pivote</b> y es
     * intencionalmente distinta de {@code ValidadorMatriz.TOLERANCIA}
     * (1e-9), que es una tolerancia de <b>igualdad</b> para comparar
     * determinantes y simetría. No unificarlas: cumplen funciones
     * distintas.</p>
     */
    private static final double TOLERANCIA_PIVOTE = 1e-12;

    private double[][] matriz;
    private int f, c;

    public int getF() {
        return f;
    }

    public int getC() {
        return c;
    }

    /**
     * Construye una matriz a partir de un arreglo 2D.
     * Realiza una copia profunda de los datos: mutaciones posteriores del
     * arreglo del llamador no afectan a esta matriz.
     *
     * @param m arreglo 2D rectangular (todas las filas deben tener la misma
     *          longitud); se acepta {@code null} o un arreglo vacío, que se
     *          convierten en una matriz 0x0
     * @throws IllegalArgumentException si las filas tienen longitudes
     *          distintas (matriz irregular)
     */
    public Matriz(double[][] m) {
        if (m == null || m.length == 0) {
            this.f = 0;
            this.c = 0;
            this.matriz = new double[0][0];
            return;
        }
        if (m[0] == null) {
            throw new IllegalArgumentException("Matriz irregular: la fila 0 es nula.");
        }
        int numColumnas = m[0].length;
        this.f = m.length;
        this.c = numColumnas;
        this.matriz = new double[f][c];
        for (int i = 0; i < f; i++) {
            if (m[i] == null || m[i].length != numColumnas) {
                int columnasFila = (m[i] == null) ? 0 : m[i].length;
                throw new IllegalArgumentException(
                        "Matriz irregular: la fila " + i + " tiene " + columnasFila
                                + " columnas, se esperaban " + numColumnas + ".");
            }
            this.matriz[i] = m[i].clone();
        }
    }

    public Matriz(int f, int c) {
        this.f = f;
        this.c = c;
        this.matriz = new double[f][c];
    }

    public Matriz() {
        this.matriz = new double[0][0];
        this.f = 0;
        this.c = 0;
    }

    /**
     * Devuelve el arreglo 2D interno de la matriz.
     * <b>Advertencia:</b> es la matriz interna, no una copia; los llamadores
     * no deben modificarlo directamente, porque rompería los invariantes de
     * dimensiones de esta clase. Para una copia propia usar {@link #clonar()}.
     *
     * @return el arreglo interno (sin copiar)
     */
    public double[][] getMatriz() {
        return matriz;
    }

    /**
     * Devuelve el elemento de la posición indicada.
     *
     * @param fila    índice de fila (base 0)
     * @param columna índice de columna (base 0)
     * @return el elemento en (fila, columna)
     * @throws IndexOutOfBoundsException si los índices están fuera del rango
     *          válido de la matriz
     */
    public double getEleMatriz(int fila, int columna) {
        if (fila < 0 || fila >= getF() || columna < 0 || columna >= getC()) {
            throw new IndexOutOfBoundsException(
                    "Índice fuera de rango: fila=" + fila + ", columna=" + columna
                            + ". Rango válido: fila en [0, " + (getF() - 1)
                            + "], columna en [0, " + (getC() - 1) + "].");
        }
        return this.matriz[fila][columna];
    }

    /**
     * Asigna el elemento en la posición indicada.
     *
     * @param ele     valor a asignar
     * @param fila    índice de fila (base 0)
     * @param columna índice de columna (base 0)
     * @throws IndexOutOfBoundsException si los índices están fuera del rango
     *          válido de la matriz
     */
    public void setEleMatriz(double ele, int fila, int columna) {
        if (fila < 0 || fila >= getF() || columna < 0 || columna >= getC()) {
            throw new IndexOutOfBoundsException(
                    "Índice fuera de rango: fila=" + fila + ", columna=" + columna
                            + ". Rango válido: fila en [0, " + (getF() - 1)
                            + "], columna en [0, " + (getC() - 1) + "].");
        }
        this.matriz[fila][columna] = ele;
    }

    // Mejora 1: Multiplicación que devuelve una nueva Matriz
    public Matriz multiplicar(Matriz B) {
        if (B == null || this.c != B.getF()) {
            throw new IllegalArgumentException("Dimensiones incompatibles: columnas de A (" + this.c + ") deben coincidir con filas de B (" + (B != null ? B.getF() : "null") + ").");
        }
        Matriz resultado = new Matriz(this.f, B.getC());
        for (int i = 0; i < this.f; i++) {
            for (int j = 0; j < B.getC(); j++) {
                double suma = 0.0;
                for (int k = 0; k < this.c; k++) {
                    suma += this.matriz[i][k] * B.getEleMatriz(k, j);
                }
                resultado.setEleMatriz(suma, i, j);
            }
        }
        return resultado;
    }

    // Mejora 2: Multiplicación Matriz-Vector (A * x = b)
    public double[] multiplicarPorVector(double[] x) {
        if (x == null || this.c != x.length) {
            throw new IllegalArgumentException("La longitud del vector (" + (x != null ? x.length : "null") + ") debe coincidir con las columnas de la matriz (" + this.c + ").");
        }
        double[] b = new double[this.f];
        for (int i = 0; i < this.f; i++) {
            double suma = 0.0;
            for (int j = 0; j < this.c; j++) {
                suma += this.matriz[i][j] * x[j];
            }
            b[i] = suma;
        }
        return b;
    }

    // Mejora 3: Matriz Transpuesta (A^T)
    public Matriz transpuesta() {
        Matriz t = new Matriz(this.c, this.f);
        for (int i = 0; i < this.f; i++) {
            for (int j = 0; j < this.c; j++) {
                t.setEleMatriz(this.matriz[i][j], j, i);
            }
        }
        return t;
    }

    // Mejora 4: Clonar matriz para preservar la original
    public Matriz clonar() {
        Matriz copia = new Matriz(this.f, this.c);
        for (int i = 0; i < this.f; i++) {
            for (int j = 0; j < this.c; j++) {
                copia.setEleMatriz(this.matriz[i][j], i, j);
            }
        }
        return copia;
    }

    /**
     * Extrae la submatriz principal líder de tamaño k x k (desde la fila y columna 0 hasta k-1).
     */
    public Matriz submatrizPrincipal(int k) {
        if (k <= 0 || k > this.f || k > this.c) {
            throw new IllegalArgumentException("Dimensión de submatriz fuera de rango: k = " + k);
        }
        Matriz sub = new Matriz(k, k);
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                sub.setEleMatriz(this.matriz[i][j], i, j);
            }
        }
        return sub;
    }

    /**
     * Calcula el determinante de la matriz cuadrada mediante eliminación Gaussiana con pivoteo parcial.
     * Complejidad: O(n^3).
     */
    public double determinante() {
        if (this.f != this.c || this.f == 0) {
            throw new IllegalStateException("El determinante solo está definido para matrices cuadradas.");
        }

        int n = this.f;
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = this.matriz[i][j];
            }
        }

        double det = 1.0;
        int signo = 1;

        for (int k = 0; k < n; k++) {
            int filaMax = k;
            double maxVal = Math.abs(a[k][k]);
            for (int i = k + 1; i < n; i++) {
                double absVal = Math.abs(a[i][k]);
                if (absVal > maxVal) {
                    maxVal = absVal;
                    filaMax = i;
                }
            }

            if (maxVal < TOLERANCIA_PIVOTE) {
                return 0.0;
            }

            if (filaMax != k) {
                double[] temp = a[k];
                a[k] = a[filaMax];
                a[filaMax] = temp;
                signo = -signo;
            }

            det *= a[k][k];

            for (int i = k + 1; i < n; i++) {
                double factor = a[i][k] / a[k][k];
                for (int j = k + 1; j < n; j++) {
                    a[i][j] -= factor * a[k][j];
                }
            }
        }

        return det * signo;
    }
}
