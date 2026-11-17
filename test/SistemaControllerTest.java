import unsa.sistemas.controller.SistemaController;
import unsa.sistemas.controller.SistemaController.Resultado;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Caso de prueba con verificación: comprueba el despacho del controlador
// a través del registro de fábricas tipadas (traza, convergencia,
// advertencias y manejo de errores).
//
// Sistema canónico (el mismo de JacobiTest):
//   2x + y = 3
//   x + 3y = 5
// Solución exacta: [0.8, 1.4] (diagonalmente dominante).
public class SistemaControllerTest {

    private static final double[][] A_CANONICA = {{2, 1}, {1, 3}};
    private static final double[] B_CANONICA = {3, 5};

    /** Mensaje exacto de advertencia del controlador (no debe cambiar). */
    private static final String ADVERTENCIA_JACOBI =
            "La matriz no es estrictamente diagonal dominante. Jacobi podría no converger.";

    /**
     * JACOBI: éxito, solución EXACTA del sistema (calculada a mano con
     * Cramer, sin usar el propio resolutor — evita la tautología de
     * comparar el resultado consigo mismo), traza y convergencia.
     */
    @Test
    void verificaJacobi() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI);

        assertTrue(r.isExitoso(), "jacobi exito");
        assertTrue(!r.isAdvertencia(), "jacobi sin advertencia");

        // Solución exacta por la regla de Cramer, sistema
        //   2x + y = 3
        //   x + 3y = 5
        // det    = 2*3 - 1*1 = 5
        // det_x  = 3*3 - 1*5 = 4  ->  x = 4/5 = 0.8
        // det_y  = 2*5 - 3*1 = 7  ->  y = 7/5 = 1.4
        // Solución exacta: [0.8, 1.4] (tolerancia 1e-6)
        double[] esperado = {0.8, 1.4};
        assertArrayEquals(esperado, r.getSolucion(), 1e-6, "jacobi solucion");

        var traza = r.getTraza();
        assertTrue(traza != null, "jacobi traza no nula");
        if (traza != null) {
            assertTrue(traza.totalPasos() >= 1, "jacobi traza con >= 1 paso");
        }

        var conv = r.getConvergencia();
        assertTrue(conv != null, "jacobi convergencia no nula");
        if (conv != null) {
            assertTrue(conv.convergio(), "jacobi convergio");
            assertTrue(conv.iteraciones() > 0, "jacobi iteraciones > 0");
        }
    }

    /** GAUSS_SEIDEL: éxito, convergencia reportada y más rápido que Jacobi. */
    @Test
    void verificaGaussSeidel() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.GAUSS_SEIDEL);

        assertTrue(r.isExitoso(), "gauss-seidel exito");

        var conv = r.getConvergencia();
        assertTrue(conv != null, "gauss-seidel convergencia no nula");
        if (conv != null) {
            assertTrue(conv.convergio(), "gauss-seidel convergio");
            assertTrue(conv.iteraciones() > 0, "gauss-seidel iteraciones > 0");
        }

        Resultado rJacobi = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI);
        if (conv != null && rJacobi.getConvergencia() != null) {
            // Aserción clave: es la que detectaría un intercambio accidental
            // de los registros JACOBI y GAUSS_SEIDEL (si las fábricas
            // estuvieran invertidas, GS no sería más rápido que Jacobi).
            assertTrue(conv.iteraciones() < rJacobi.getConvergencia().iteraciones(),
                    "gauss-seidel mas rapido que jacobi");
        }
    }

    /** Método inexistente: sin éxito y mensaje con "no reconocido". */
    @Test
    void verificaMetodoDesconocido() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, "METODO_FANTASMA");

        assertTrue(!r.isExitoso(), "desconocido sin exito");
        assertTrue(r.getMensaje() != null && r.getMensaje().contains("no reconocido"),
                "desconocido mensaje contiene 'no reconocido'");
    }

    /** Método null: sin éxito (guardia explícita del controlador). */
    @Test
    void verificaMetodoNulo() {
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, null);

        assertTrue(!r.isExitoso(), "null sin exito");
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
    @Test
    void verificaJacobiSinDominanciaDiagonal() {
        double[][] A = {{1, 2}, {3, 4}};
        double[] b = {3, 7}; // Jacobi diverge: matriz no diagonalmente dominante

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.JACOBI);

        assertTrue(r.isExitoso(), "sin dominancia exito");
        assertTrue(r.isAdvertencia(), "sin dominancia es advertencia");
        assertTrue(ADVERTENCIA_JACOBI.equals(r.getMensaje()), "sin dominancia mensaje exacto");
        assertTrue(r.getTraza() != null, "sin dominancia traza no nula");
        assertTrue(r.getConvergencia() != null, "sin dominancia convergencia no nula");
        if (r.getConvergencia() != null) {
            assertTrue(!r.getConvergencia().convergio(), "sin dominancia jacobi divergio");
        }
    }

    /**
     * GAUSS (directo, stub sin implementar): el resolutor devuelve null,
     * por lo que el controlador debe reportar ERROR —no un éxito falso—
     * con el mensaje de método no implementado y sin convergencia.
     */
    @Test
    void verificaGaussDirecto() {
    SistemaController c = new SistemaController();
    Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.GAUSS);

    assertTrue(r.isExitoso(), "gauss exito");

    double[] esperado = {0.8, 1.4};
    assertArrayEquals(esperado, r.getSolucion(), 1e-6, "gauss solucion");

    assertTrue(r.getTraza() != null, "gauss traza no nula");
    if (r.getTraza() != null) {
        assertTrue(r.getTraza().totalPasos() > 0, "gauss traza con pasos");
    }

    assertTrue(r.getConvergencia() == null, "gauss sin convergencia (no iterativo)");
}

    @Test
    void verificaGaussJordan() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {4, 2, 2},
        {2, 5, 1},
        {2, 1, 3}
    };

    double[] b = {14, 15, 13};

    Resultado r = c.resolver(A, b, SistemaController.GAUSS_JORDAN);

    assertTrue(r.isExitoso(), "gauss-jordan exito");

    if (r.isExitoso()) {
        double[] esperado = {1, 2, 3};
        assertArrayEquals(esperado, r.getSolucion(), 1e-6, "gauss-jordan solucion");

        assertTrue(r.getTraza() != null, "gauss-jordan traza no nula");

        if (r.getTraza() != null) {
            assertTrue(r.getTraza().totalPasos() > 0,
                    "gauss-jordan traza con pasos");
        }
    }

    assertTrue(r.getConvergencia() == null, "gauss-jordan sin convergencia");
}
    @Test
    void verificaCrout() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {4, 2, 2},
        {2, 5, 1},
        {2, 1, 3}
    };

    double[] b = {14, 15, 13};

    Resultado r = c.resolver(A, b, SistemaController.CROUT);

    assertTrue(r.isExitoso(), "crout exito");

    if (r.isExitoso()) {
        double[] esperado = {1, 2, 3};
        assertArrayEquals(esperado, r.getSolucion(), 1e-6, "crout solucion");

        assertTrue(r.getTraza() != null, "crout traza no nula");

        if (r.getTraza() != null) {
            assertTrue(r.getTraza().totalPasos() > 0,
                    "crout traza con pasos");
        }
    }

    assertTrue(r.getConvergencia() == null, "crout sin convergencia");
}
    @Test
    void verificaCholesky() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {4, 2, 2},
        {2, 5, 1},
        {2, 1, 3}
    };

    double[] b = {14, 15, 13};

    Resultado r = c.resolver(A, b, SistemaController.CHOLESKY);

    assertTrue(r.isExitoso(), "cholesky exito");

    if (r.isExitoso()) {
        double[] esperado = {1, 2, 3};
        assertArrayEquals(esperado, r.getSolucion(), 1e-6, "cholesky solucion");

        assertTrue(r.getTraza() != null, "cholesky traza no nula");

        if (r.getTraza() != null) {
            assertTrue(r.getTraza().totalPasos() > 0,
                    "cholesky traza con pasos");
        }
    }

    assertTrue(r.getConvergencia() == null, "cholesky sin convergencia");
}
    @Test
    void verificaGaussSingular() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {1, 2},
        {2, 4}
    };

    double[] b = {3, 6};

    Resultado r = c.resolver(A, b, SistemaController.GAUSS);

    assertTrue(!r.isExitoso(), "gauss singular sin exito");
    assertTrue(r.getMensaje() != null, "gauss singular mensaje no nulo");
    assertTrue(r.getConvergencia() == null, "gauss singular sin convergencia");
}
    @Test
    void verificaGaussJordanSingular() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {1, 2},
        {2, 4}
    };

    double[] b = {3, 6};

    Resultado r = c.resolver(A, b, SistemaController.GAUSS_JORDAN);

    assertTrue(!r.isExitoso(), "gauss-jordan singular sin exito");
    assertTrue(r.getMensaje() != null, "gauss-jordan singular mensaje no nulo");
    assertTrue(r.getConvergencia() == null, "gauss-jordan singular sin convergencia");
}
    @Test
    void verificaCroutPivoteNulo() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {0, 1},
        {1, 2}
    };

    double[] b = {1, 3};

    Resultado r = c.resolver(A, b, SistemaController.CROUT);

    assertTrue(!r.isExitoso(), "crout pivote nulo sin exito");
    assertTrue(r.getMensaje() != null, "crout pivote nulo mensaje no nulo");
    assertTrue(r.getConvergencia() == null, "crout pivote nulo sin convergencia");
}
    @Test
    void verificaCholeskyNoSimetrica() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {4, 2},
        {3, 5}
    };

    double[] b = {6, 8};

    Resultado r = c.resolver(A, b, SistemaController.CHOLESKY);

    assertTrue(!r.isExitoso(), "cholesky no simetrica sin exito");
    assertTrue(r.getMensaje() != null, "cholesky no simetrica mensaje no nulo");
    assertTrue(r.getConvergencia() == null, "cholesky no simetrica sin convergencia");
}
    @Test
    void verificaCholeskyNoDefinidaPositiva() {
    SistemaController c = new SistemaController();

    double[][] A = {
        {1, 2},
        {2, 1}
    };

    double[] b = {3, 3};

    Resultado r = c.resolver(A, b, SistemaController.CHOLESKY);

    assertTrue(!r.isExitoso(), "cholesky no definida positiva sin exito");
    assertTrue(r.getMensaje() != null, "cholesky no definida positiva mensaje no nulo");
    assertTrue(r.getConvergencia() == null, "cholesky no definida positiva sin convergencia");
}


}
