
import unsa.sistemas.model.direct.Cholesky;
import unsa.sistemas.model.trace.Traza;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CholeskyTest {

    // Sistema con solución exacta [1, 2, 3]
    @Test
    void verificaSolucion() {

        double[][] A = {
            {4, 2, 2},
            {2, 5, 1},
            {2, 1, 3}
        };

        double[] b = {14, 15, 13};

        Cholesky cholesky = new Cholesky(A, b, 3);

        double[] x = cholesky.resolver();

        assertEquals(1.0, x[0], 1e-6, "x1");
        assertEquals(2.0, x[1], 1e-6, "x2");
        assertEquals(3.0, x[2], 1e-6, "x3");
    }

    // Comprueba que se registren los pasos
    @Test
    void verificaTraza() {

        Traza traza = new Traza();

        Cholesky cholesky = new Cholesky(
            new double[][] {
                {4, 2, 2},
                {2, 5, 1},
                {2, 1, 3}
            },
            new double[] {14, 15, 13},
            3,
            traza
        );

        cholesky.resolver();

        assertTrue(!traza.estaVacia(), "traza no vacia");
        assertTrue(traza.totalPasos() > 0, "cantidad de pasos > 0");
    }

    // Cholesky debe rechazar matrices no simétricas
    @Test
    void verificaMatrizNoSimetrica() {

        assertThrows(IllegalArgumentException.class, () -> {

            double[][] A = {
                {4, 2},
                {3, 5}
            };

            double[] b = {6, 8};

            new Cholesky(A, b, 2).resolver();

        }, "matriz no simetrica detectada");
    }

    // Cholesky debe rechazar matrices que no sean definidas positivas
    @Test
    void verificaMatrizNoDefinidaPositiva() {

        assertThrows(ArithmeticException.class, () -> {

            double[][] A = {
                {1, 2},
                {2, 1}
            };

            double[] b = {3, 3};

            new Cholesky(A, b, 2).resolver();

        }, "matriz no definida positiva detectada");
    }
}
