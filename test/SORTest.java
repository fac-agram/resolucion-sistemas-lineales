import unsa.sistemas.model.iterative.GaussSeidel;
import unsa.sistemas.model.iterative.SOR;
import unsa.sistemas.model.trace.Traza;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SORTest {

    /** La solución debe ser [0.8, 1.4] con tolerancia 1e-6. */
    @Test
    void verificaSolucion() {
        double[][] A = {{2, 1}, {1, 3}};
        double[] b = {3, 5};

        double[] x = new SOR(A, b, 2, 1.5).resolver();

        assertEquals(0.8, x[0], 1e-6, "x1");
        assertEquals(1.4, x[1], 1e-6, "x2");
    }

    /** Con ω = 1, SOR debe comportarse EXACTAMENTE como Gauss-Seidel. */
    @Test
    void verificaEquivalenciaConGaussSeidel() {
        double[][] A = {{2, 1}, {1, 3}};
        double[] b = {3, 5};

        GaussSeidel gs = new GaussSeidel(A, b, 2);
        double[] xGs = gs.resolver();

        SOR sor = new SOR(A, b, 2, 1.0);
        double[] xSor = sor.resolver();

        assertEquals(xGs[0], xSor[0], 1e-6, "SOR(ω=1).x1 == GaussSeidel.x1");
        assertEquals(xGs[1], xSor[1], 1e-6, "SOR(ω=1).x2 == GaussSeidel.x2");
        assertTrue(sor.getIteraciones() == gs.getIteraciones(),
                "SOR(ω=1) iters == GaussSeidel iters");
    }

    /** Debe converger antes de 1000 iteraciones con error < tolerancia. */
    @Test
    void verificaConvergencia() {
        Traza traza = new Traza();
        SOR s = new SOR(new double[][]{{2, 1}, {1, 3}}, new double[]{3, 5}, 2, 1.5, traza);
        s.resolver();

        assertTrue(s.convergio(), "convergio");
        assertTrue(s.getErrorFinal() < 1e-7, "errorFinal < 1e-7");
        assertTrue(s.getIteraciones() > 0, "iteraciones > 0");
        // 1 paso extra por la aproximación inicial x0
        assertTrue(traza.totalPasos() == s.getIteraciones() + 1, "traza = iteraciones + 1");
        assertTrue(s.getOmega() == 1.5, "omega registrado");
    }

    /** ω fuera de (0,2) y diagonal con cero deben lanzar excepciones. */
    @Test
    void verificaValidaciones() {
        assertThrows(IllegalArgumentException.class,
                () -> new SOR(new double[][]{{2, 1}, {1, 3}}, new double[]{3, 5}, 2, 2.0),
                "ω = 2 lanza excepcion");

        assertThrows(IllegalArgumentException.class,
                () -> new SOR(new double[][]{{2, 1}, {1, 3}}, new double[]{3, 5}, 2, 0.0),
                "ω = 0 lanza excepcion");

        assertThrows(IllegalArgumentException.class,
                () -> new SOR(new double[][]{{0, 1}, {1, 1}}, new double[]{1, 1}, 2, 1.5).resolver(),
                "diagonal en cero lanza excepcion");
    }
}
