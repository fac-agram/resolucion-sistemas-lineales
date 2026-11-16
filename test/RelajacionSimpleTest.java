import unsa.sistemas.model.iterative.RelajacionSimple;
import unsa.sistemas.model.trace.Traza;

// Caso de prueba con verificación: comprueba Relajación simple contra
// valores conocidos, valida el residual por fuera y su precondición.
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
//   java -cp out RelajacionSimpleTest
public class RelajacionSimpleTest {

    private static boolean ok = true;

    private static final double[][] A = {{2, 1}, {1, 3}};
    private static final double[] B = {3, 5};

    public static void main(String[] args) {
        verificaSolucion();
        verificaResidualPorFuera();
        verificaConvergencia();
        verificaPrecondicionDiagonalEnCero();

        System.out.println(ok ? "TEST_OK" : "TEST_FALLO");
        System.exit(ok ? 0 : 1);
    }

    /** La solución debe ser [0.8, 1.4] con tolerancia 1e-6. */
    private static void verificaSolucion() {
        double[] x = new RelajacionSimple(A, B, 2).resolver();

        compara("x1", x[0], 0.8);
        compara("x2", x[1], 1.4);
    }

    /** Independientemente del método: ‖b − A·x‖∞ debe ser chica. */
    private static void verificaResidualPorFuera() {
        double[] x = new RelajacionSimple(A, B, 2).resolver();

        double norma = 0.0;
        for (int i = 0; i < 2; i++) {
            double residuo = B[i] - (A[i][0] * x[0] + A[i][1] * x[1]);
            norma = Math.max(norma, Math.abs(residuo));
        }
        System.out.printf("‖b − A·x‖∞ = %.3e%n", norma);
        chequea("residual verificado por fuera < 1e-6", norma < 1e-6);
    }

    /** Debe converger con error = ‖r‖∞ < tolerancia y traza completa. */
    private static void verificaConvergencia() {
        Traza traza = new Traza();
        RelajacionSimple g = new RelajacionSimple(A, B, 2, traza);
        g.resolver();

        chequea("convergio", g.convergio());
        chequea("errorFinal < 1e-7", g.getErrorFinal() < 1e-7);
        chequea("iteraciones > 0", g.getIteraciones() > 0);
        // 1 paso extra por el residual inicial r = b
        chequea("traza = iteraciones + 1", traza.totalPasos() == g.getIteraciones() + 1);
    }

    /** Una diagonal con cero debe lanzar IllegalArgumentException. */
    private static void verificaPrecondicionDiagonalEnCero() {
        try {
            new RelajacionSimple(new double[][]{{0, 1}, {1, 1}}, new double[]{1, 1}, 2).resolver();
            chequea("diagonal en cero lanza excepcion", false);
        } catch (IllegalArgumentException e) {
            System.out.println("Precondicion OK: " + e.getMessage());
            chequea("diagonal en cero lanza excepcion", true);
        }
    }

    // ── mini-framework de aserciones (sin dependencias) ──

    private static void compara(String nombre, double obtenido, double esperado) {
        if (Math.abs(obtenido - esperado) < 1e-6) {
            System.out.printf("%s = %.6f (esperado %.6f) OK%n", nombre, obtenido, esperado);
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
