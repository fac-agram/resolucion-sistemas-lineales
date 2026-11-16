import unsa.sistemas.model.matrix.Matriz;
import unsa.sistemas.model.matrix.Normas;

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
//
// Sale con código 0 si todo pasa (TEST_OK), 1 si falla (TEST_FALLO).
//
// Cómo compilar y ejecutar desde la raíz del proyecto:
//   shopt -s globstar
//   javac -d out src/**/*.java
//   javac -cp out -d out test/*.java
//   java -cp out NormasTest
public class NormasTest {

    private static boolean ok = true;

    public static void main(String[] args) {
        verificaNormasVectoriales();
        verificaNormasMatriz2x2();
        verificaNormasMatriz3x3();
        verificaCasosEspeciales();
        verificaValidaciones();

        System.out.println(ok ? "TEST_OK" : "TEST_FALLO");
        System.exit(ok ? 0 : 1);
    }

    /** ‖(3,-4)‖₁=7, ‖·‖₂=5, ‖·‖∞=4; ‖(1,2,3)‖₁=6, ‖·‖₂=√14, ‖·‖∞=3. */
    private static void verificaNormasVectoriales() {
        double[] v = {3, -4};
        compara("‖(3,-4)‖₁", Normas.norma1(v), 7.0);
        compara("‖(3,-4)‖₂", Normas.norma2(v), 5.0);
        compara("‖(3,-4)‖∞", Normas.normaInfinito(v), 4.0);

        double[] u = {1, 2, 3};
        compara("‖(1,2,3)‖₁", Normas.norma1(u), 6.0);
        compara("‖(1,2,3)‖₂", Normas.norma2(u), Math.sqrt(14.0));
        compara("‖(1,2,3)‖∞", Normas.normaInfinito(u), 3.0);
    }

    /**
     * A = [[4,1],[1,3]]: ‖A‖₁=5, ‖A‖∞=5, ‖A‖_F=√27,
     * ‖A‖₂=√((27+√245)/2) — raíz de λmáx(AAᵀ), con AAᵀ=[[17,7],[7,10]].
     */
    private static void verificaNormasMatriz2x2() {
        Matriz a = new Matriz(new double[][]{{4, 1}, {1, 3}});
        compara("‖A‖₁ (max columna)", Normas.norma1(a), 5.0);
        compara("‖A‖∞ (max fila)", Normas.normaInfinito(a), 5.0);
        compara("‖A‖_F", Normas.frobenius(a), Math.sqrt(27.0));

        // Ecuación característica de AAᵀ: λ² − 27λ + 121 = 0 → disc = 729 − 484 = 245
        double esperado = Math.sqrt((27 + Math.sqrt(245)) / 2);
        compara("‖A‖₂ (espectral)", Normas.norma2(a), esperado);
        System.out.printf("   ‖A‖₂ = %.6f (con la raíz; sin ella sería %.6f = ‖A‖₂²)%n",
                Normas.norma2(a), (27 + Math.sqrt(245)) / 2);
    }

    /** B = [[1,2,3],[4,5,6],[7,8,9]]: ‖B‖₁=18, ‖B‖∞=24, ‖B‖_F=√285. */
    private static void verificaNormasMatriz3x3() {
        Matriz b = new Matriz(new double[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        compara("‖B‖₁ (max columna)", Normas.norma1(b), 18.0);
        compara("‖B‖∞ (max fila)", Normas.normaInfinito(b), 24.0);
        compara("‖B‖_F", Normas.frobenius(b), Math.sqrt(285.0));
    }

    /** Identidad y matriz nula: casos cerrados de todas las normas. */
    private static void verificaCasosEspeciales() {
        Matriz i = new Matriz(new double[][]{{1, 0}, {0, 1}});
        compara("‖I‖₁", Normas.norma1(i), 1.0);
        compara("‖I‖∞", Normas.normaInfinito(i), 1.0);
        compara("‖I‖₂ (espectral)", Normas.norma2(i), 1.0);
        compara("‖I‖_F", Normas.frobenius(i), Math.sqrt(2.0));

        Matriz z = new Matriz(new double[][]{{0, 0}, {0, 0}});
        compara("‖Z‖₂ (nula)", Normas.norma2(z), 0.0);

        // diag(3,1): AAᵀ = diag(9,1) → λmáx = 9 → ‖A‖₂ = 3 exacto
        Matriz d = new Matriz(new double[][]{{3, 0}, {0, 1}});
        compara("‖diag(3,1)‖₂", Normas.norma2(d), 3.0);
    }

    /** Null en cualquier norma debe lanzar IllegalArgumentException. */
    private static void verificaValidaciones() {
        esperarIae("norma1 vector null", () -> Normas.norma1((double[]) null));
        esperarIae("norma2 vector null", () -> Normas.norma2((double[]) null));
        esperarIae("normaInf vector null", () -> Normas.normaInfinito((double[]) null));
        esperarIae("norma1 matriz null", () -> Normas.norma1((Matriz) null));
        esperarIae("norma2 matriz null", () -> Normas.norma2((Matriz) null));
        esperarIae("frobenius null", () -> Normas.frobenius(null));
    }

    // ── mini-framework de aserciones (sin dependencias) ──

    private static void compara(String nombre, double obtenido, double esperado) {
        if (Math.abs(obtenido - esperado) < 1e-6) {
            System.out.printf("%s = %.6f OK%n", nombre, obtenido);
        } else {
            System.out.printf("%s = %.6f FALLO (esperado %.6f)%n", nombre, obtenido, esperado);
            ok = false;
        }
    }

    private static void esperarIae(String nombre, Runnable accion) {
        try {
            accion.run();
            System.out.println("FALLO " + nombre + " (no lanzo excepcion)");
            ok = false;
        } catch (IllegalArgumentException e) {
            System.out.println("Precondicion OK [" + nombre + "]: " + e.getMessage());
        }
    }
}
