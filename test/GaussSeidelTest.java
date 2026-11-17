import unsa.sistemas.model.iterative.GaussSeidel;
import unsa.sistemas.model.trace.Traza;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GaussSeidelTest {

    /** La solución debe ser [0.8, 1.4] con tolerancia 1e-6. */
    @Test
    void verificaSolucion() {
        double[][] A = {{2, 1}, {1, 3}};
        double[] b = {3, 5};

        double[] x = new GaussSeidel(A, b, 2).resolver();

        assertEquals(0.8, x[0], 1e-6, "x1");
        assertEquals(1.4, x[1], 1e-6, "x2");
    }

    /** Debe converger antes de 1000 iteraciones con error < tolerancia. */
    @Test
    void verificaConvergencia() {
        Traza traza = new Traza();
        GaussSeidel g = new GaussSeidel(new double[][]{{2, 1}, {1, 3}}, new double[]{3, 5}, 2, traza);
        g.resolver();

        assertTrue(g.convergio(), "convergio");
        assertTrue(g.getErrorFinal() < 1e-7, "errorFinal < 1e-7");
        assertTrue(g.getIteraciones() > 0, "iteraciones > 0");
        // 1 paso extra por la aproximación inicial x0
        assertTrue(traza.totalPasos() == g.getIteraciones() + 1, "traza = iteraciones + 1");
    }

    /** Una diagonal con cero debe lanzar IllegalArgumentException. */
    @Test
    void verificaPrecondicionDiagonalEnCero() {
        assertThrows(IllegalArgumentException.class,
                () -> new GaussSeidel(new double[][]{{0, 1}, {1, 1}}, new double[]{1, 1}, 2).resolver(),
                "diagonal en cero lanza excepcion");
    }
}
