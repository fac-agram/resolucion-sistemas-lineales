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
     * JACOBI sin dominancia diagonal y diverge: la matriz {{1,2},{3,4}}
     * tiene radio espectral sqrt(1.5) &gt; 1, así que al llegar al tope de
     * 1000 iteraciones la aproximación es finita (~1e88) pero NO es una
     * solución del sistema. La aproximación se conserva y se etiqueta como
     * tal; el resultado nunca debe declararse exitoso.
     */
    @Test
    void verificaJacobiSinDominanciaDiagonal() {
        double[][] A = {{1, 2}, {3, 4}};
        double[] b = {3, 7};

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.JACOBI);

        assertFalse(r.isExitoso(), "sin dominancia no debe ser exitoso");
        assertTrue(r.isAdvertencia(), "sin dominancia es advertencia");
        assertEquals(SistemaController.Estado.NO_CONVERGIO, r.getEstado(),
                "sin dominancia estado NO_CONVERGIO");

        assertNotNull(r.getSolucion(), "sin dominancia solucion no nula");
        for (double v : r.getSolucion()) {
            assertTrue(Double.isFinite(v), "sin dominancia solucion finita");
        }

        assertNotNull(r.getConvergencia(), "sin dominancia convergencia no nula");
        assertFalse(r.getConvergencia().convergio(), "sin dominancia no convergio");

        assertTrue(r.getMensaje().contains("no es una solución del sistema"),
                "sin dominancia mensaje etiqueta la aproximacion");
        assertFalse(r.getMensaje().contains("Resuelto con éxito."),
                "sin dominancia mensaje no afirma exito");
        assertNotNull(r.getTraza(), "sin dominancia traza no nula");
    }

    /**
     * JACOBI converge parcialmente pero agota el tope de iteraciones con
     * una aproximación finita (radio espectral 0.999): el resultado tampoco
     * es una solución del sistema.
     */
    @Test
    void verificaJacobiNoConvergeConAproximacionFinita() {
        double[][] A = {{1, 0.999}, {0.999, 1}};
        double[] b = {1, 2};

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.JACOBI);

        assertFalse(r.isExitoso(), "no convergido no debe ser exitoso");
        assertEquals(SistemaController.Estado.NO_CONVERGIO, r.getEstado(),
                "no convergido estado NO_CONVERGIO");

        assertNotNull(r.getSolucion(), "no convergido solucion no nula");
        for (double v : r.getSolucion()) {
            assertTrue(Double.isFinite(v), "no convergido solucion finita");
        }

        assertNotNull(r.getConvergencia(), "no convergido convergencia no nula");
        assertTrue(r.getConvergencia().iteraciones() > 0, "no convergido iteraciones > 0");
        assertFalse(r.getConvergencia().convergio(), "no convergido no convergio");
    }

    /**
     * JACOBI desborda a -Infinity antes del tope: exponer esos valores como
     * solución sería deshonesto, así que no debe haber solución.
     */
    @Test
    void verificaJacobiValoresNoFinitos() {
        double[][] A = {{1, 3}, {2, 1}};
        double[] b = {4, 3};

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.JACOBI);

        assertFalse(r.isExitoso(), "no finitos no debe ser exitoso");
        assertEquals(SistemaController.Estado.NO_FINITO, r.getEstado(),
                "no finitos estado NO_FINITO");
        assertNull(r.getSolucion(), "no finitos no expone solucion");
        assertTrue(r.getMensaje().contains("no finitos"),
                "no finitos mensaje nombra la condicion");
    }

    /**
     * SOR sobre la misma matriz produce NaN: tampoco expone solución.
     */
    @Test
    void verificaSORValoresNoFinitos() {
        double[][] A = {{1, 3}, {2, 1}};
        double[] b = {4, 3};

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.SOR_METODO);

        assertFalse(r.isExitoso(), "SOR no finito no debe ser exitoso");
        assertEquals(SistemaController.Estado.NO_FINITO, r.getEstado(),
                "SOR no finito estado NO_FINITO");
        assertNull(r.getSolucion(), "SOR no finito no expone solucion");
    }

    /**
     * Método directo con término independiente NaN: el solucionador devuelve
     * [NaN, NaN] y el controlador debe rechazarlo sin solución ni convergencia.
     */
    @Test
    void verificaMetodoDirectoNoFinito() {
        double[][] A = {{1, 2}, {3, 5}};
        double[] b = {1, Double.NaN};

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.GAUSS);

        assertFalse(r.isExitoso(), "directo no finito no debe ser exitoso");
        assertEquals(SistemaController.Estado.NO_FINITO, r.getEstado(),
                "directo no finito estado NO_FINITO");
        assertNull(r.getSolucion(), "directo no finito no expone solucion");
        assertNull(r.getConvergencia(), "directo no finito sin convergencia");
    }

    /**
     * JACOBI sobre una matriz no dominante que SÍ converge: el resultado es
     * una solución legítima (no una advertencia) y el aviso de dominancia se
     * agrega como nota al mensaje de éxito.
     */
    @Test
    void verificaJacobiConvergeConNotaSinDominancia() {
        double[][] A = {{1, 1}, {1, 2}};
        double[] b = {2, 3};

        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A, b, SistemaController.JACOBI);

        assertTrue(r.isExitoso(), "OK con nota es exitoso");
        assertEquals(SistemaController.Estado.OK, r.getEstado(), "OK con nota estado OK");
        assertFalse(r.isAdvertencia(), "OK con nota no es advertencia");
        assertEquals("Resuelto con éxito. Nota: la matriz no es estrictamente diagonal dominante.",
                r.getMensaje(), "OK con nota mensaje exacto");
        assertArrayEquals(new double[]{1, 1}, r.getSolucion(), 1e-6, "OK con nota solucion");
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



    /**
     * Un resultado por estado: cada estado expone un mensaje propio y solo el
     * estado OK usa el texto de éxito. ERROR y NO_FINITO nunca exponen solución.
     */
    @Test
    void verificaMensajesPorEstado() {
        SistemaController c = new SistemaController();

        Resultado ok = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI);
        Resultado noConvergio = c.resolver(new double[][]{{1, 2}, {3, 4}}, new double[]{3, 7},
                SistemaController.JACOBI);
        Resultado noFinito = c.resolver(new double[][]{{1, 2}, {3, 5}}, new double[]{1, Double.NaN},
                SistemaController.GAUSS);
        Resultado error = c.resolver(A_CANONICA, B_CANONICA, "NOPE");

        assertEquals(SistemaController.Estado.OK, ok.getEstado(), "canonica estado OK");
        assertEquals(SistemaController.Estado.NO_CONVERGIO, noConvergio.getEstado(),
                "sin dominancia estado NO_CONVERGIO");
        assertEquals(SistemaController.Estado.NO_FINITO, noFinito.getEstado(),
                "no finito estado NO_FINITO");
        assertEquals(SistemaController.Estado.ERROR, error.getEstado(), "desconocido estado ERROR");

        assertFalse(ok.getMensaje().isBlank(), "OK mensaje no vacio");
        assertFalse(noConvergio.getMensaje().isBlank(), "NO_CONVERGIO mensaje no vacio");
        assertFalse(noFinito.getMensaje().isBlank(), "NO_FINITO mensaje no vacio");
        assertFalse(error.getMensaje().isBlank(), "ERROR mensaje no vacio");

        assertTrue(ok.getMensaje().contains("Resuelto con éxito."), "solo OK usa el texto de exito");
        assertFalse(noConvergio.getMensaje().contains("Resuelto con éxito."),
                "NO_CONVERGIO no usa el texto de exito");
        assertFalse(noFinito.getMensaje().contains("Resuelto con éxito."),
                "NO_FINITO no usa el texto de exito");
        assertFalse(error.getMensaje().contains("Resuelto con éxito."),
                "ERROR no usa el texto de exito");

        assertNull(noFinito.getSolucion(), "NO_FINITO sin solucion");
        assertNull(error.getSolucion(), "ERROR sin solucion");
    }

    /**
     * El mapeo estado → textos es puro y fija los encabezados, la barra de
     * estado y la clase CSS de las tres representaciones.
     */
    @Test
    void verificaTextosDeEstado() {
        var ok = SistemaController.textoDe(SistemaController.Estado.OK);
        assertEquals("SOLUCIÓN DEL SISTEMA", ok.titulo(), "OK titulo");
        assertEquals("Sistema resuelto correctamente.", ok.barraEstado(), "OK barra de estado");
        assertEquals("estado-ok", ok.claseCss(), "OK clase css");

        var noConvergio = SistemaController.textoDe(SistemaController.Estado.NO_CONVERGIO);
        assertEquals("ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)", noConvergio.titulo(),
                "NO_CONVERGIO titulo");
        assertEquals("El método no convergió. Se muestra la última aproximación.",
                noConvergio.barraEstado(), "NO_CONVERGIO barra de estado");
        assertEquals("estado-advertencia", noConvergio.claseCss(), "NO_CONVERGIO clase css");

        var noFinito = SistemaController.textoDe(SistemaController.Estado.NO_FINITO);
        assertEquals("NO SE PUDO RESOLVER EL SISTEMA", noFinito.titulo(), "NO_FINITO titulo");
        assertEquals("Error: el resultado no es finito.", noFinito.barraEstado(),
                "NO_FINITO barra de estado");
        assertEquals("estado-error", noFinito.claseCss(), "NO_FINITO clase css");

        var error = SistemaController.textoDe(SistemaController.Estado.ERROR);
        assertEquals("NO SE PUDO RESOLVER EL SISTEMA", error.titulo(), "ERROR titulo");
        assertEquals("Error durante la resolución.", error.barraEstado(), "ERROR barra de estado");
        assertEquals("estado-error", error.claseCss(), "ERROR clase css");

        assertNotEquals(ok.titulo(), noConvergio.titulo(), "advertencia y OK con encabezado distinto");
        assertNotEquals(noConvergio.claseCss(), noFinito.claseCss(),
                "advertencia y error con clase css distinta");
    }

}
