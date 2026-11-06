package unsa.sistemas.model.matrix;

public class Matriz {
    private double[][] matriz;
    private int f, c;

    public int getF() {
        return f;
    }

    public void setF(int f) {
        this.f = f;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public Matriz(double[][] m) {
        this.matriz = m;
        if (m != null && m.length > 0) {
            this.f = m.length;
            this.c = m[0].length;
        } else {
            this.f = 0;
            this.c = 0;
        }
    }

    public Matriz(int f, int c) {
        this.f = f;
        this.c = c;
        this.matriz = new double[f][c];
    }

    public Matriz() {
        this.matriz = null;
        this.f = 0;
        this.c = 0;
    }

    public double[][] getMatriz() {
        return matriz;
    }

    public void setMatriz(double[][] matriz) {
        this.matriz = matriz;
        if (matriz != null && matriz.length > 0) {
            this.f = matriz.length;
            this.c = matriz[0].length;
        } else {
            this.f = 0;
            this.c = 0;
        }
    }

    public double getEleMatriz(int fila, int columna) {
        if (fila >= 0 && fila < getF() && columna >= 0 && columna < getC()) {
            return this.matriz[fila][columna];
        } else {
            return 0;
        }
    }

    public void setEleMatriz(double ele, int fila, int columna) {
        if (fila >= 0 && fila < getF() && columna >= 0 && columna < getC()) {
            this.matriz[fila][columna] = ele;
        }
    }

    public void productoMatrices(Matriz A, Matriz B) {
        if (A != null && B != null && A.getC() == B.getF()) {
            this.f = A.getF();
            this.c = B.getC();
            this.matriz = new double[this.f][this.c];

            for (int i = 0; i < this.f; i++) {
                for (int j = 0; j < this.c; j++) {
                    double suma = 0.0;
                    for (int k = 0; k < A.getC(); k++) {
                        suma += A.getEleMatriz(i, k) * B.getEleMatriz(k, j);
                    }
                    this.matriz[i][j] = suma;
                }
            }
        }
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

            if (maxVal < 1e-12) {
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
