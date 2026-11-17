
import unsa.sistemas.model.direct.Crout;
import unsa.sistemas.model.trace.Traza;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CroutTest {

    // Sistema con solución exacta [1, 2, 3]
    @Test
    void verificaSolucion() {

        double[][] A = {
            {4, 2, 2},
            {2, 5, 1},
            {2, 1, 3}
        };

        double[] b = {14, 15, 13};

        Crout crout = new Crout(A, b, 3);

        double[] x = crout.resolver();

        assertEquals(1.0, x[0], 1e-6, "x1");
        assertEquals(2.0, x[1], 1e-6, "x2");
        assertEquals(3.0, x[2], 1e-6, "x3");
    }

    // Comprueba que se registren los pasos
    @Test
    void verificaTraza() {

        Traza traza = new Traza();

        Crout crout = new Crout(
            new double[][] {
                {4, 2, 2},
                {2, 5, 1},
                {2, 1, 3}
            },
            new double[] {14, 15, 13},
            3,
            traza
        );

        crout.resolver();

        assertTrue(!traza.estaVacia(), "traza no vacia");
        assertTrue(traza.totalPasos() > 0, "cantidad de pasos > 0");
    }

    // Crout sin pivoteo debe detectar un pivote nulo
    @Test
    void verificaPivoteNulo() {

        assertThrows(ArithmeticException.class, () -> {

            double[][] A = {
                {0, 1},
                {1, 2}
            };

            double[] b = {1, 3};

            new Crout(A, b, 2).resolver();

        }, "pivote nulo detectado");
    }
}
