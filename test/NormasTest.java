import unsa.sistemas.model.matrix.Matriz;
import unsa.sistemas.model.matrix.Normas;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Caso de prueba con verificación: normas vectoriales y de matrices
// contra valores calculados a mano (formulas del apunte).
//
// Vectores usados:
//   v = (3, -4)  -> ‖v‖₁ = 7,  ‖v‖₂ = 5,   ‖v‖∞ = 4
//   u = (1, 2, 3)-> ‖u‖₁ = 6,  ‖u‖₂ = √14, ‖u‖∞ = 3
//
// Matrices usadas:
//   A = [[4,1],[1,3]]               -> ‖A‖₁ = 5 (col 1),
//                                       ‖A‖∞ = 5 (fila 1),
//                                       ‖A‖_F = √27,
//                                       ‖A‖₂ = √((27+√245)/2) ≈ 4.618034
//       (AAᵀ = [[17,7],[7,10]]; tr=27, det=121, disc=245)
//   B = [[1,2,3],[4,5,6],[7,8,9]]   -> ‖B‖₁ = 18, ‖B‖∞ = 24, ‖B‖_F = √285
//   I = identidad 2x2               -> ‖I‖₁ = ‖I‖₂ = ‖I‖∞ = 1, ‖I‖_F = √2
//   Z = matriz nula 2x2             -> ‖Z‖₂ = 0
public class NormasTest {

    /** ‖(3,-4)‖₁=7, ‖·‖₂=5, ‖·‖∞=4; ‖(1,2,3)‖₁=6, ‖·‖₂=√14, ‖·‖∞=3. */
    @Test
    void verificaNormasVectoriales() {
        double[] v = {3, -4};
        assertEquals(7.0, Normas.norma1(v), 1e-6, "‖(3,-4)‖₁");
        assertEquals(5.0, Normas.norma2(v), 1e-6, "‖(3,-4)‖₂");
        assertEquals(4.0, Normas.normaInfinito(v), 1e-6, "‖(3,-4)‖∞");

        double[] u = {1, 2, 3};
        assertEquals(6.0, Normas.norma1(u), 1e-6, "‖(1,2,3)‖₁");
        assertEquals(Math.sqrt(14.0), Normas.norma2(u), 1e-6, "‖(1,2,3)‖₂");
        assertEquals(3.0, Normas.normaInfinito(u), 1e-6, "‖(1,2,3)‖∞");
    }

    /**
     * A = [[4,1],[1,3]]: ‖A‖₁=5, ‖A‖∞=5, ‖A‖_F=√27,
     * ‖A‖₂=√((27+√245)/2) — raíz de λmáx(AAᵀ), con AAᵀ=[[17,7],[7,10]].
     */
    @Test
    void verificaNormasMatriz2x2() {
        Matriz a = new Matriz(new double[][]{{4, 1}, {1, 3}});
        assertEquals(5.0, Normas.norma1(a), 1e-6, "‖A‖₁ (max columna)");
        assertEquals(5.0, Normas.normaInfinito(a), 1e-6, "‖A‖∞ (max fila)");
        assertEquals(Math.sqrt(27.0), Normas.frobenius(a), 1e-6, "‖A‖_F");

        // Ecuación característica de AAᵀ: λ² − 27λ + 121 = 0 → disc = 729 − 484 = 245
        double esperado = Math.sqrt((27 + Math.sqrt(245)) / 2);
        assertEquals(esperado, Normas.norma2(a), 1e-6, "‖A‖₂ (espectral)");
    }

    /** B = [[1,2,3],[4,5,6],[7,8,9]]: ‖B‖₁=18, ‖B‖∞=24, ‖B‖_F=√285. */
    @Test
    void verificaNormasMatriz3x3() {
        Matriz b = new Matriz(new double[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        assertEquals(18.0, Normas.norma1(b), 1e-6, "‖B‖₁ (max columna)");
        assertEquals(24.0, Normas.normaInfinito(b), 1e-6, "‖B‖∞ (max fila)");
        assertEquals(Math.sqrt(285.0), Normas.frobenius(b), 1e-6, "‖B‖_F");
    }

    /** Identidad y matriz nula: casos cerrados de todas las normas. */
    @Test
    void verificaCasosEspeciales() {
        Matriz i = new Matriz(new double[][]{{1, 0}, {0, 1}});
        assertEquals(1.0, Normas.norma1(i), 1e-6, "‖I‖₁");
        assertEquals(1.0, Normas.normaInfinito(i), 1e-6, "‖I‖∞");
        assertEquals(1.0, Normas.norma2(i), 1e-6, "‖I‖₂ (espectral)");
        assertEquals(Math.sqrt(2.0), Normas.frobenius(i), 1e-6, "‖I‖_F");

        Matriz z = new Matriz(new double[][]{{0, 0}, {0, 0}});
        assertEquals(0.0, Normas.norma2(z), 1e-6, "‖Z‖₂ (nula)");

        // diag(3,1): AAᵀ = diag(9,1) → λmáx = 9 → ‖A‖₂ = 3 exacto
        Matriz d = new Matriz(new double[][]{{3, 0}, {0, 1}});
        assertEquals(3.0, Normas.norma2(d), 1e-6, "‖diag(3,1)‖₂");
    }

    /**
     * ‖v‖∞ debe propagar los valores no finitos en lugar de omitirlos:
     * NaN domina sobre cualquier acotado, ±Infinity dan +Infinity, y
     * (+Inf, NaN) colapsa a NaN. El vacío sigue siendo 0.0 (invariante).
     * Caza el bug del salto silencioso: {@code NaN > max} siempre es
     * falso, así que la comparación {@code if (abs > max)} descarta NaN.
     */
    @Test
    void verificaNormaInfinitoPropagaNoFinitos() {
        assertEquals(Double.NaN, Normas.normaInfinito(new double[]{3.0, -4.0, Double.NaN}),
                "‖(3,-4,NaN)‖∞ debe ser NaN, no 4.0");
        assertEquals(Double.NaN, Normas.normaInfinito(new double[]{Double.NaN, Double.NaN, Double.NaN}),
                "‖(NaN,NaN,NaN)‖∞ debe ser NaN, no 0.0");
        assertEquals(Double.POSITIVE_INFINITY,
                Normas.normaInfinito(new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}),
                "‖(+Inf,-Inf)‖∞ debe ser +Inf");
        assertEquals(Double.NaN,
                Normas.normaInfinito(new double[]{Double.POSITIVE_INFINITY, Double.NaN}),
                "‖(+Inf,NaN)‖∞ debe ser NaN, no +Inf");
        assertEquals(0.0, Normas.normaInfinito(new double[]{}), "‖()‖∞ debe seguir siendo 0.0");
    }

    /** Null en cualquier norma debe lanzar IllegalArgumentException. */
    @Test
    void verificaValidaciones() {
        assertThrows(IllegalArgumentException.class,
                () -> Normas.norma1((double[]) null), "norma1 vector null");
        assertThrows(IllegalArgumentException.class,
                () -> Normas.norma2((double[]) null), "norma2 vector null");
        assertThrows(IllegalArgumentException.class,
                () -> Normas.normaInfinito((double[]) null), "normaInf vector null");
        assertThrows(IllegalArgumentException.class,
                () -> Normas.norma1((Matriz) null), "norma1 matriz null");
        assertThrows(IllegalArgumentException.class,
                () -> Normas.norma2((Matriz) null), "norma2 matriz null");
        assertThrows(IllegalArgumentException.class,
                () -> Normas.frobenius(null), "frobenius null");
    }
}
