import unsa.sistemas.controller.SistemaController;
import unsa.sistemas.controller.SistemaController.Resultado;

// Caso de prueba con verificación: comprueba el despacho del controlador
// a través del registro de fábricas tipadas (traza, convergencia,
// advertencias y manejo de errores).
//
// Sistema canónico (el mismo de JacobiTest):
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
//   java -cp out SistemaControllerTest
public class SistemaControllerTest {

    private static boolean ok = true;

    private static final double[][] A_CANONICA = {{2, 1}, {1, 3}};
    private static final double[] B_CANONICA = {3, 5};

    /** Mensaje exacto de advertencia del controlador (no debe cambiar). */
    private static final String ADVERTENCIA_JACOBI =
            "La matriz no es estrictamente diagonal dominante. Jacobi podría no converger.";

    public static void main(String[] args) {
        verificaJacobi();
        verificaGaussSeidel();
        verificaMetodoDesconocido();
        verificaMetodoNulo();
        verificaJacobiSinDominanciaDiagonal();
        verificaGaussDirecto();

        System.out.println(ok ? "TEST_OK" : "TEST_FALLO");
        System.exit(ok ? 0 : 1);
    }

    /**
     * JACOBI: éxito, solución EXACTA del sistema (calculada a mano con
     * Cramer, sin usar el propio resolutor — evita la tautología de
     * comparar el resultado consigo mismo), traza y convergencia.
     */
    private static void verificaJacobi() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI);

        chequea("jacobi exito", r.isExitoso());
        chequea("jacobi sin advertencia", !r.isAdvertencia());

        // Solución exacta por la regla de Cramer, sistema
        //   2x + y = 3
        //   x + 3y = 5
        // det    = 2*3 - 1*1 = 5
        // det_x  = 3*3 - 1*5 = 4  ->  x = 4/5 = 0.8
        // det_y  = 2*5 - 3*1 = 7  ->  y = 7/5 = 1.4
        // Solución exacta: [0.8, 1.4] (tolerancia 1e-6)
        double[] esperado = {0.8, 1.4};
        comparaVector("jacobi solucion", r.getSolucion(), esperado);

        var traza = r.getTraza();
        chequea("jacobi traza no nula", traza != null);
        if (traza != null) {
            chequea("jacobi traza con >= 1 paso", traza.totalPasos() >= 1);
        }

        var conv = r.getConvergencia();
        chequea("jacobi convergencia no nula", conv != null);
        if (conv != null) {
            chequea("jacobi convergio", conv.convergio());
            chequea("jacobi iteraciones > 0", conv.iteraciones() > 0);
        }
    }

    /** GAUSS_SEIDEL: éxito, convergencia reportada y más rápido que Jacobi. */
    private static void verificaGaussSeidel() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.GAUSS_SEIDEL);

        chequea("gauss-seidel exito", r.isExitoso());

        var conv = r.getConvergencia();
        chequea("gauss-seidel convergencia no nula", conv != null);
        if (conv != null) {
            chequea("gauss-seidel convergio", conv.convergio());
            chequea("gauss-seidel iteraciones > 0", conv.iteraciones() > 0);
        }

        Resultado rJacobi = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI);
        if (conv != null && rJacobi.getConvergencia() != null) {
            // Aserción clave: es la que detectaría un intercambio accidental
            // de los registros JACOBI y GAUSS_SEIDEL (si las fábricas
            // estuvieran invertidas, GS no sería más rápido que Jacobi).
            chequea("gauss-seidel mas rapido que jacobi",
                    conv.iteraciones() < rJacobi.getConvergencia().iteraciones());
        }
    }

    /** Método inexistente: sin éxito y mensaje con "no reconocido". */
    private static void verificaMetodoDesconocido() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, "METODO_FANTASMA");

        chequea("desconocido sin exito", !r.isExitoso());
        chequea("desconocido mensaje contiene 'no reconocido'",
                r.getMensaje() != null && r.getMensaje().contains("no reconocido"));
    }

    /** Método null: sin éxito (guardia explícita del controlador). */
    private static void verificaMetodoNulo() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, null);

        chequea("null sin exito", !r.isExitoso());
    }

    /**
     * JACOBI sin dominancia diagonal: la matriz se eligió porque NO es
     * estrictamente diagonal dominante (fila 0: |1| &lt;= |2|), de modo que
     * el controlador emite la advertencia. Con esta matriz Jacobi
     * DIVERGE (el radio espectral de la matriz de iteración es &gt; 1):
     * exactamente por eso existe la advertencia — la dominancia es solo
     * condición <i>suficiente</i> de convergencia. No se verifican
     * valores de solución, solo que el método no convergió.
     */
    private static void verificaJacobiSinDominanciaDiagonal() {
        double[][] A = {{1, 2}, {3, 4}};
        double[] b = {3, 7}; // Jacobi diverge: matriz no diagonalmente dominante

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.JACOBI);

        chequea("sin dominancia exito", r.isExitoso());
        chequea("sin dominancia es advertencia", r.isAdvertencia());
        chequea("sin dominancia mensaje exacto", ADVERTENCIA_JACOBI.equals(r.getMensaje()));
        chequea("sin dominancia traza no nula", r.getTraza() != null);
        chequea("sin dominancia convergencia no nula", r.getConvergencia() != null);
        if (r.getConvergencia() != null) {
            chequea("sin dominancia jacobi divergio", !r.getConvergencia().convergio());
        }
    }

    /**
     * GAUSS (directo, stub sin implementar): el resolutor devuelve null,
     * por lo que el controlador debe reportar ERROR —no un éxito falso—
     * con el mensaje de método no implementado y sin convergencia.
     */
    private static void verificaGaussDirecto() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.GAUSS);

        chequea("gauss sin exito (stub)", !r.isExitoso());
        chequea("gauss mensaje de no implementado",
                r.getMensaje() != null && r.getMensaje().contains("implementado"));
        chequea("gauss sin convergencia (no iterativo)", r.getConvergencia() == null);
    }

    // ── mini-framework de aserciones (sin dependencias) ──

    private static void comparaVector(String nombre, double[] obtenido, double[] esperado) {
        if (obtenido == null || esperado == null || obtenido.length != esperado.length) {
            System.out.println("FALLO " + nombre + " (longitudes incompatibles)");
            ok = false;
            return;
        }
        boolean igual = true;
        for (int i = 0; i < obtenido.length; i++) {
            if (!(Math.abs(obtenido[i] - esperado[i]) < 1e-6)) {
                System.out.printf("%s[%d] = %.6f FALLO (esperado %.6f)%n",
                        nombre, i, obtenido[i], esperado[i]);
                igual = false;
            }
        }
        if (igual) {
            System.out.printf("%s coincide con el resolutor (tolerancia 1e-6) OK%n", nombre);
        } else {
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
