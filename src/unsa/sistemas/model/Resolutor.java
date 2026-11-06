package unsa.sistemas.model;

import unsa.sistemas.model.trace.Traza;

/**
 * Base común de todos los resolutores (directos e iterativos).
 *
 * <p>Solo aporta los datos del sistema Ax = b y el clonado defensivo:
 * cada resolutor trabaja sobre su propia copia, de modo que el estado
 * original que pasa el controlador no se altera nunca.</p>
 *
 * <p>No contiene lógica de ningún algoritmo: cada clase concreta
 * implementa su {@code resolver()} completo y visible.</p>
 */
public abstract class Resolutor {

    /** Matriz de coeficientes n x n (copia propia). */
    protected final double[][] A;

    /** Vector de términos independientes de longitud n (copia propia). */
    protected final double[] b;

    /** Orden del sistema (dimensión n). */
    protected final int n;

    /**
     * Traza de pasos del trazado opcional.
     * {@code null} cuando no se pidió trazado: en ese caso el resolutor
     * no registra nada.
     */
    protected final Traza traza;

    /**
     * Constructor sin trazado.
     *
     * @param A matriz de coeficientes n x n
     * @param b vector de términos independientes de longitud n
     * @param n orden del sistema
     * @throws IllegalArgumentException si A, b son null, n <= 0 o las
     *         dimensiones no coinciden con n
     */
    protected Resolutor(double[][] A, double[] b, int n) {
        this(A, b, n, null);
    }

    /**
     * Constructor con trazado opcional.
     *
     * @param A     matriz de coeficientes n x n
     * @param b     vector de términos independientes de longitud n
     * @param n     orden del sistema
     * @param traza traza a ir llenando durante la ejecución,
     *              o {@code null} para no trazar
     * @throws IllegalArgumentException si A, b son null, n <= 0 o las
     *         dimensiones no coinciden con n
     */
    protected Resolutor(double[][] A, double[] b, int n, Traza traza) {
        if (A == null) {
            throw new IllegalArgumentException("La matriz A no puede ser null.");
        }
        if (b == null) {
            throw new IllegalArgumentException("El vector b no puede ser null.");
        }
        if (n <= 0) {
            throw new IllegalArgumentException("El orden n debe ser mayor que cero, recibido: " + n);
        }
        if (A.length < n || b.length < n) {
            throw new IllegalArgumentException(
                    "Dimensiones incompatibles: se esperaban al menos " + n + " filas en A y " + n + " elementos en b.");
        }

        this.n = n;
        this.A = new double[n][n];
        for (int i = 0; i < n; i++) {
            if (A[i] == null || A[i].length < n) {
                throw new IllegalArgumentException("La fila " + i + " de A debe tener al menos " + n + " columnas.");
            }
            this.A[i] = A[i].clone();
        }
        this.b = b.clone();
        this.traza = traza;
    }

    /**
     * Traza cargada durante la última ejecución.
     *
     * @return la traza pasada en el constructor, o {@code null} si el
     *         resolutor se creó sin trazado
     */
    public Traza getTraza() {
        return traza;
    }
}
