import unsa.sistemas.model.iterative.RelajacionSimple;
import unsa.sistemas.model.trace.Traza;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RelajacionSimpleTest {

    private static final double[][] A = {{2, 1}, {1, 3}};
    private static final double[] B = {3, 5};

    /** La solución debe ser [0.8, 1.4] con tolerancia 1e-6. */
    @Test
    void verificaSolucion() {
        double[] x = new RelajacionSimple(A, B, 2).resolver();

        assertEquals(0.8, x[0], 1e-6, "x1");
        assertEquals(1.4, x[1], 1e-6, "x2");
    }

    /** Independientemente del método: ‖b − A·x‖∞ debe ser chica. */
    @Test
    void verificaResidualPorFuera() {
        double[] x = new RelajacionSimple(A, B, 2).resolver();

        double norma = 0.0;
        for (int i = 0; i < 2; i++) {
            double residuo = B[i] - (A[i][0] * x[0] + A[i][1] * x[1]);
            norma = Math.max(norma, Math.abs(residuo));
        }
        assertTrue(norma < 1e-6, "residual verificado por fuera < 1e-6");
    }

    /** Debe converger con error = ‖r‖∞ < tolerancia y traza completa. */
    @Test
    void verificaConvergencia() {
        Traza traza = new Traza();
        RelajacionSimple g = new RelajacionSimple(A, B, 2, traza);
        g.resolver();

        assertTrue(g.convergio(), "convergio");
        assertTrue(g.getErrorFinal() < 1e-7, "errorFinal < 1e-7");
        assertTrue(g.getIteraciones() > 0, "iteraciones > 0");
        // snapshot inicial + una actualizacion ejecutada por paso = conteo unificado (R4.3)
        assertTrue(traza.totalPasos() == g.getIteraciones(), "traza = iteraciones");
    }

    /** La pasada que demuestra convergencia se cuenta: iteraciones == k (R4.1). */
    @Test
    void verificaIteracionesCuentanPasadaConvergente() {
        RelajacionSimple g = new RelajacionSimple(A, B, 2, new Traza());

        g.resolver();

        assertTrue(g.convergio(), "convergio");
        assertEquals(21, g.getIteraciones(),
                "iteraciones debe ser la pasada k donde el residuo cae bajo la tolerancia (medido: 21)");
    }

    /** Caracterizacion del tope: sin convergencia, 1000 iteraciones y 1001 pasos (R4.2). */
    @Test
    void verificaTopeDeIteracionesSinConvergencia() {
        double[][] aCap = {{1, 1}, {1, 1}};
        double[] bCap = {1, 0};

        Traza traza = new Traza();
        RelajacionSimple g = new RelajacionSimple(aCap, bCap, 2, traza);
        g.resolver();

        assertFalse(g.convergio(), "no converge en el tope");
        assertEquals(1000, g.getIteraciones(), "tope de iteraciones = 1000");
        assertEquals(1001, traza.totalPasos(), "snapshot + 1000 updates = 1001 pasos");
    }

    /** Una diagonal con cero debe lanzar IllegalArgumentException. */
    @Test
    void verificaPrecondicionDiagonalEnCero() {
        assertThrows(IllegalArgumentException.class,
                () -> new RelajacionSimple(new double[][]{{0, 1}, {1, 1}}, new double[]{1, 1}, 2).resolver(),
                "diagonal en cero lanza excepcion");
    }
}
