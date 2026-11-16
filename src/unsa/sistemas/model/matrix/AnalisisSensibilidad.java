package unsa.sistemas.model.matrix;

/**
 * Análisis de sensibilidad de sistemas lineales: número de condición.
 *
 * <p>Las normas que usa están en {@link Normas} (clase única de normas
 * del proyecto); este módulo solo orquesta el cálculo de condición.</p>
 *
 * <p><b>¿Qué mide el número de condición?</b> Cuánto se amplifica un
 * error relativo al resolver Ax = b:</p>
 *
 * <pre>
 *   ‖Δx‖ / ‖x‖  ≲  cond(A) · ‖Δb‖ / ‖b‖
 * </pre>
 *
 * <p>Es decir, un error de magnitud ε en los datos (redondeo, medición,
 * perturbación) puede llegar a la solución amplificado hasta
 * {@code cond(A)} veces. Reglas prácticas:</p>
 *
 * <ul>
 *   <li>{@code cond(A) ≈ 1} → matriz bien condicionada, los errores no
 *       se amplifican.</li>
 *   <li>{@code cond(A) = 10^k} → se pierden aproximadamente {@code k}
 *       dígitos decimales de precisión en la solución.</li>
 *   <li>{@code cond(A)} grande en coma flotante (p. ej. 10^16) → la
 *       solución puede ser basura aunque el método funcione "bien".</li>
 * </ul>
 *
 * <p>Se usa la norma infinito (la de la suma máxima de valores absolutos
 * por fila) tanto para A como para su inversa: es submultiplicativa, y
 * por eso se garantiza {@code cond(A) >= 1}.</p>
 */
public final class AnalisisSensibilidad {

    /** Evita que se instancie: es una clase de métodos estáticos. */
    private AnalisisSensibilidad() {
    }

    /**
     * Número de condición de A en norma infinito:
     * {@code cond(A) = ‖A‖∞ · ‖A⁻¹‖∞}.
     *
     * <p>El llamador es responsable de pasar la inversa correcta de
     * {@code matriz} (p. ej. obtenida por Gauss-Jordan sobre [A|I]).
     * Se valida que ambas matrices sean cuadradas de la misma dimensión
     * y que el producto dé un valor coherente ({@code cond >= 1}); si el
     * producto queda por debajo de 1 se asume que la "inversa" pasada no
     * es la inversa de A.</p>
     *
     * @param matriz        matriz cuadrada A
     * @param matrizInversa matriz cuadrada A⁻¹, misma dimensión que A
     * @return cond(A) >= 1; valores chicos = bien condicionada
     * @throws IllegalArgumentException si alguna es null, no son cuadradas,
     *         sus dimensiones difieren, o el resultado &lt; 1 (inversa
     *         inconsistente)
     */
    public static double calcularCondicion(Matriz matriz, Matriz matrizInversa) {
        if (matriz == null || matrizInversa == null) {
            throw new IllegalArgumentException("La matriz y su inversa no pueden ser null.");
        }
        if (matriz.getF() != matriz.getC()) {
            throw new IllegalArgumentException(
                    "El número de condición solo está definido para matrices cuadradas; recibida: "
                            + matriz.getF() + "x" + matriz.getC() + ".");
        }
        if (matrizInversa.getF() != matrizInversa.getC()) {
            throw new IllegalArgumentException(
                    "La inversa debe ser una matriz cuadrada; recibida: "
                            + matrizInversa.getF() + "x" + matrizInversa.getC() + ".");
        }
        if (matriz.getF() != matrizInversa.getF()) {
            throw new IllegalArgumentException(
                    "A y A⁻¹ deben tener la misma dimensión: A es " + matriz.getF() + "x" + matriz.getC()
                            + " y la inversa es " + matrizInversa.getF() + "x" + matrizInversa.getC() + ".");
        }

        double normaA = Normas.normaInfinito(matriz);
        double normaInv = Normas.normaInfinito(matrizInversa);
        double condicion = normaA * normaInv;

        // Para una inversa verdadera, cond = ‖A‖·‖A⁻¹‖ >= ‖A·A⁻¹‖ = ‖I‖ = 1
        // (la norma infinito es submultiplicativa). Si da < 1, la matriz
        // pasada como inversa no es la inversa de A.
        if (condicion < 1.0 - 1e-9) {
            throw new IllegalArgumentException(
                    "cond(A) = " + condicion + " < 1: la matriz pasada como inversa no parece ser A⁻¹ "
                            + "(para una inversa verdadera siempre se cumple cond(A) >= 1).");
        }

        return condicion;
    }
}
