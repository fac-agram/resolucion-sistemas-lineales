package unsa.sistemas.model.matrix;

/**
 * Normas vectoriales y de matrices definidas en el apunte del curso.
 *
 * <h2>Normas vectoriales — v ∈ ℝⁿ</h2>
 * <ul>
 *   <li>Norma 1: {@code ‖v‖₁ = Σ |vᵢ|}</li>
 *   <li>Norma 2 (euclidiana): {@code ‖v‖₂ = √(Σ vᵢ²)}</li>
 *   <li>Norma infinito (máximo): {@code ‖v‖∞ = max |vᵢ|}</li>
 * </ul>
 *
 * <h2>Normas de matrices</h2>
 * <ul>
 *   <li>Norma 1 (subordinada de columnas): máxima suma de valores
 *       absolutos por columna</li>
 *   <li>Norma 2 (espectral subordinada): {@code √(λmáx(A·Aᵀ))} — la raíz
 *       del mayor autovalor de {@code A·Aᵀ}; sin la raíz sería
 *       {@code ‖A‖₂²} y NO cumpliría los axiomas de norma
 *       ({@code ‖2I‖₂ = 2}, no 4)</li>
 *   <li>Norma infinito (subordinada de filas): máxima suma de valores
 *       absolutos por fila</li>
 *   <li>Norma de Frobenius: {@code √(Σᵢ Σⱼ aᵢⱼ²)}</li>
 * </ul>
 *
 * <p>La espectral se calcula con el <b>método de potencia</b> sobre
 * {@code A·Aᵀ} (aplicado como {@code Aᵀ(A·v)} para no materializar el
 * producto), porque no existe fórmula cerrada para λmáx en dimensión
 * arbitraria. Arranque determinista {@code v = (1, 2, ..., n)}.</p>
 */
public final class Normas {

    /** Evita que se instancie: es una clase de métodos estáticos. */
    private Normas() {
    }

    /** Tolerancia de convergencia del método de potencia (norma 2 de matrices). */
    private static final double TOLERANCIA_POTENCIA = 1e-12;

    /** Iteraciones máximas del método de potencia. */
    private static final int MAX_POTENCIA = 10_000;

    // ─────────────────────────────────────────────
    //  NORMAS VECTORIALES
    // ─────────────────────────────────────────────

    /**
     * Norma 1 de un vector: suma de valores absolutos.
     *
     * @param v vector
     * @return Σ|vᵢ|
     * @throws IllegalArgumentException si {@code v} es {@code null}
     */
    public static double norma1(double[] v) {
        if (v == null) {
            throw new IllegalArgumentException("El vector no puede ser null.");
        }
        double suma = 0.0;
        for (double valor : v) {
            suma += Math.abs(valor);
        }
        return suma;
    }

    /**
     * Norma 2 (euclidiana) de un vector.
     *
     * @param v vector
     * @return √(Σ vᵢ²)
     * @throws IllegalArgumentException si {@code v} es {@code null}
     */
    public static double norma2(double[] v) {
        if (v == null) {
            throw new IllegalArgumentException("El vector no puede ser null.");
        }
        double suma = 0.0;
        for (double valor : v) {
            suma += valor * valor;
        }
        return Math.sqrt(suma);
    }

    /**
     * Norma infinito (del máximo) de un vector.
     *
     * @param v vector
     * @return max|vᵢ|; 0 para vector vacío
     * @throws IllegalArgumentException si {@code v} es {@code null}
     */
    public static double normaInfinito(double[] v) {
        if (v == null) {
            throw new IllegalArgumentException("El vector no puede ser null.");
        }
        double max = 0.0;
        for (double valor : v) {
            double abs = Math.abs(valor);
            if (abs > max) {
                max = abs;
            }
        }
        return max;
    }

    // ─────────────────────────────────────────────
    //  NORMAS DE MATRICES
    // ─────────────────────────────────────────────

    /**
     * Norma 1 de matriz (subordinada de columnas): la mayor de las sumas
     * de valores absolutos por columna.
     *
     * @param A matriz (puede ser rectangular)
     * @return max sobre columnas j de Σᵢ |aᵢⱼ|
     * @throws IllegalArgumentException si {@code A} es {@code null}
     */
    public static double norma1(Matriz A) {
        if (A == null) {
            throw new IllegalArgumentException("La matriz no puede ser null.");
        }
        double maxSuma = 0.0;
        for (int j = 0; j < A.getC(); j++) {
            double sumaColumna = 0.0;
            for (int i = 0; i < A.getF(); i++) {
                sumaColumna += Math.abs(A.getEleMatriz(i, j));
            }
            if (sumaColumna > maxSuma) {
                maxSuma = sumaColumna;
            }
        }
        return maxSuma;
    }

    /**
     * Norma infinito de matriz (subordinada de filas): la mayor de las
     * sumas de valores absolutos por fila.
     *
     * <p>Es la norma usada por {@link AnalisisSensibilidad#calcularCondicion}
     * para medir el número de condición.</p>
     *
     * @param A matriz (puede ser rectangular)
     * @return max sobre filas i de Σⱼ |aᵢⱼ|
     * @throws IllegalArgumentException si {@code A} es {@code null}
     */
    public static double normaInfinito(Matriz A) {
        if (A == null) {
            throw new IllegalArgumentException("La matriz no puede ser null.");
        }
        double maxSuma = 0.0;
        for (int i = 0; i < A.getF(); i++) {
            double sumaFila = 0.0;
            for (int j = 0; j < A.getC(); j++) {
                sumaFila += Math.abs(A.getEleMatriz(i, j));
            }
            if (sumaFila > maxSuma) {
                maxSuma = sumaFila;
            }
        }
        return maxSuma;
    }

    /**
     * Norma de Frobenius: la raíz de la suma de todos los elementos
     * al cuadrado.
     *
     * @param A matriz (puede ser rectangular)
     * @return √(Σᵢ Σⱼ aᵢⱼ²)
     * @throws IllegalArgumentException si {@code A} es {@code null}
     */
    public static double frobenius(Matriz A) {
        if (A == null) {
            throw new IllegalArgumentException("La matriz no puede ser null.");
        }
        double suma = 0.0;
        for (int i = 0; i < A.getF(); i++) {
            for (int j = 0; j < A.getC(); j++) {
                double valor = A.getEleMatriz(i, j);
                suma += valor * valor;
            }
        }
        return Math.sqrt(suma);
    }

    /**
     * Norma 2 espectral de matriz: la mayor singularidad de A, es decir
     * {@code √(λmáx(A·Aᵀ))}.
     *
     * <p><b>Nota sobre el apunte:</b> si el apunte la escribe como
     * "máximo autovalor de A·Aᵀ" SIN la raíz, eso es en realidad
     * {@code ‖A‖₂²} y no cumple los axiomas de norma
     * ({@code ‖2I‖₂} daría 4 en vez de 2). Acá se implementa la norma
     * correcta, con la raíz.</p>
     *
     * <p>Se usa el método de potencia sobre {@code A·Aᵀ}: en cada paso
     * {@code v ← Aᵀ(A·v)} (dos productos matriz-vector, sin construir
     * {@code A·Aᵀ}), hasta que el vector deja de moverse. Costo por paso:
     * O(Σ filas·columnas).</p>
     *
     * @param A matriz
     * @return √(λmáx(A·Aᵀ)); 0 para la matriz nula
     * @throws IllegalArgumentException si {@code A} es {@code null}
     */
    public static double norma2(Matriz A) {
        if (A == null) {
            throw new IllegalArgumentException("La matriz no puede ser null.");
        }
        int f = A.getF();
        int c = A.getC();
        if (f == 0 || c == 0) {
            return 0.0;
        }

        // Matriz nula: λmáx = 0 (y el método de potencia no arrancaría)
        boolean esNula = true;
        for (int i = 0; i < f && esNula; i++) {
            for (int j = 0; j < c; j++) {
                if (A.getEleMatriz(i, j) != 0.0) {
                    esNula = false;
                    break;
                }
            }
        }
        if (esNula) {
            return 0.0;
        }

        // v inicial determinista (1, 2, ..., f), normalizado
        double[] v = new double[f];
        for (int i = 0; i < f; i++) {
            v[i] = i + 1.0;
        }
        normalizar(v);

        // Método de potencia sobre M = A·Aᵀ
        double lambda = 0.0;
        for (int k = 0; k < MAX_POTENCIA; k++) {
            double[] w = aTranspuestaPorA(A, v);   // w = A·Aᵀ·v
            double normW = norma2(w);
            if (normW == 0.0) {
                break;   // degenerado: v cayó al subespacio nulo
            }

            // Cociente de Rayleigh con v unitario: λ ≈ vᵀ·M·v = vᵀ·w
            lambda = 0.0;
            for (int i = 0; i < f; i++) {
                lambda += v[i] * w[i];
            }

            double[] vNuevo = new double[f];
            for (int i = 0; i < f; i++) {
                vNuevo[i] = w[i] / normW;
            }

            double movio = 0.0;
            for (int i = 0; i < f; i++) {
                movio += (vNuevo[i] - v[i]) * (vNuevo[i] - v[i]);
            }
            v = vNuevo;
            if (Math.sqrt(movio) < TOLERANCIA_POTENCIA) {
                break;
            }
        }

        // A·Aᵀ es semidefinida positiva: λ >= 0 (el corte por debajo de 0
        // es solo para error de redondeo)
        return Math.sqrt(Math.max(lambda, 0.0));
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────

    /** Producto Aᵀ·(A·v) = (A·Aᵀ)·v sin materializar A·Aᵀ. */
    private static double[] aTranspuestaPorA(Matriz A, double[] v) {
        int f = A.getF();
        int c = A.getC();

        double[] u = new double[c];           // u = Aᵀ·v
        for (int j = 0; j < c; j++) {
            double suma = 0.0;
            for (int i = 0; i < f; i++) {
                suma += A.getEleMatriz(i, j) * v[i];
            }
            u[j] = suma;
        }

        double[] w = new double[f];           // w = A·u = A·Aᵀ·v
        for (int i = 0; i < f; i++) {
            double suma = 0.0;
            for (int j = 0; j < c; j++) {
                suma += A.getEleMatriz(i, j) * u[j];
            }
            w[i] = suma;
        }
        return w;
    }

    /** Normaliza v dividiéndolo por su norma 2 (en su sitio). */
    private static void normalizar(double[] v) {
        double n = norma2(v);
        if (n == 0.0) {
            return;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] /= n;
        }
    }
}
