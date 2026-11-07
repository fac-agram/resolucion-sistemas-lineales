import unsa.sistemas.model.iterative.GaussSeidel;
import unsa.sistemas.model.iterative.SOR;
import unsa.sistemas.model.trace.Traza;

// Caso de prueba con verificación: comprueba SOR contra valores conocidos,
// su equivalencia con Gauss-Seidel cuando ω = 1 y sus validaciones.
//
// Sistema:
//   2x + y = 3
//   x + 3y = 5
// Solución exacta: [0.8, 1.4] (diagonalmente dominante).
//
// Sale con código 0 si todo pasa (TEST_OK), 1 si falla (TEST_FALLO).
//
// Cómo compilar y ejecutar desde la raíz del proyecto:
//   shopt -s globstar
//   javac -d out src/**/*.java
//   javac -cp out -d out test/*.java
//   java -cp out SORTest
public class SORTest {

    private static boolean ok = true;

    public static void main(String[] args) {
        verificaSolucion();
        verificaEquivalenciaConGaussSeidel();
        verificaConvergencia();
        verificaValidaciones();

        System.out.println(ok ? "TEST_OK" : "TEST_FALLO");
        System.exit(ok ? 0 : 1);
    }

    /** La solución debe ser [0.8, 1.4] con tolerancia 1e-6. */
    private static void verificaSolucion() {
        double[][] A = {{2, 1}, {1, 3}};
        double[] b = {3, 5};

        double[] x = new SOR(A, b, 2, 1.5).resolver();

        compara("x1", x[0], 0.8);
        compara("x2", x[1], 1.4);
    }

    /** Con ω = 1, SOR debe comportarse EXACTAMENTE como Gauss-Seidel. */
    private static void verificaEquivalenciaConGaussSeidel() {
        double[][] A = {{2, 1}, {1, 3}};
        double[] b = {3, 5};

        GaussSeidel gs = new GaussSeidel(A, b, 2);
        double[] xGs = gs.resolver();

        SOR sor = new SOR(A, b, 2, 1.0);
        double[] xSor = sor.resolver();

        compara("SOR(ω=1).x1 == GaussSeidel.x1", xSor[0], xGs[0]);
        compara("SOR(ω=1).x2 == GaussSeidel.x2", xSor[1], xGs[1]);
        chequea("SOR(ω=1) iters == GaussSeidel iters",
                sor.getIteraciones() == gs.getIteraciones());
    }

    /** Debe converger antes de 1000 iteraciones con error < tolerancia. */
    private static void verificaConvergencia() {
        Traza traza = new Traza();
        SOR s = new SOR(new double[][]{{2, 1}, {1, 3}}, new double[]{3, 5}, 2, 1.5, traza);
        s.resolver();

        chequea("convergio", s.convergio());
        chequea("errorFinal < 1e-7", s.getErrorFinal() < 1e-7);
        chequea("iteraciones > 0", s.getIteraciones() > 0);
        // 1 paso extra por la aproximación inicial x0
        chequea("traza = iteraciones + 1", traza.totalPasos() == s.getIteraciones() + 1);
        chequea("omega registrado", s.getOmega() == 1.5);
    }

    /** ω fuera de (0,2) y diagonal con cero deben lanzar excepciones. */
    private static void verificaValidaciones() {
        try {
            new SOR(new double[][]{{2, 1}, {1, 3}}, new double[]{3, 5}, 2, 2.0);
            chequea("ω = 2 lanza excepcion", false);
        } catch (IllegalArgumentException e) {
            System.out.println("Precondicion OK: " + e.getMessage());
            chequea("ω = 2 lanza excepcion", true);
        }

        try {
            new SOR(new double[][]{{2, 1}, {1, 3}}, new double[]{3, 5}, 2, 0.0);
            chequea("ω = 0 lanza excepcion", false);
        } catch (IllegalArgumentException e) {
            System.out.println("Precondicion OK: " + e.getMessage());
            chequea("ω = 0 lanza excepcion", true);
        }

        try {
            new SOR(new double[][]{{0, 1}, {1, 1}}, new double[]{1, 1}, 2, 1.5).resolver();
            chequea("diagonal en cero lanza excepcion", false);
        } catch (IllegalArgumentException e) {
            System.out.println("Precondicion OK: " + e.getMessage());
            chequea("diagonal en cero lanza excepcion", true);
        }
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

    private static void chequea(String nombre, boolean condicion) {
        System.out.println((condicion ? "OK   " : "FALLO") + " " + nombre);
        if (!condicion) {
            ok = false;
        }
    }
}
