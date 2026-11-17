import unsa.sistemas.model.direct.GaussJordan;
import unsa.sistemas.model.trace.Traza;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GaussJordanTest {

    // Sistema con solución exacta [1, 2, 3]
    @Test
    void verificaSolucion() {

        double[][] A = {
            {4, 2, 2},
            {2, 5, 1},
            {2, 1, 3}
        };

        double[] b = {14, 15, 13};

        GaussJordan jordan = new GaussJordan(A, b, 3);

        double[] x = jordan.resolver();

        assertEquals(1.0, x[0], 1e-6, "x1");
        assertEquals(2.0, x[1], 1e-6, "x2");
        assertEquals(3.0, x[2], 1e-6, "x3");
    }

    // Comprueba que se registren los pasos del algoritmo
    @Test
    void verificaTraza() {

        Traza traza = new Traza();

        GaussJordan jordan = new GaussJordan(
            new double[][] {
                {4, 2, 2},
                {2, 5, 1},
                {2, 1, 3}
            },
            new double[] {14, 15, 13},
            3,
            traza
        );

        jordan.resolver();

        assertTrue(!traza.estaVacia(), "traza no vacia");
        assertTrue(traza.totalPasos() > 0, "cantidad de pasos > 0");
    }

    // Comprueba que detecte una matriz singular
    @Test
    void verificaSistemaSingular() {

        assertThrows(ArithmeticException.class, () -> {

            double[][] A = {
                {1, 2},
                {2, 4}
            };

            double[] b = {3, 6};

            new GaussJordan(A, b, 2).resolver();

        }, "matriz singular detectada");
    }

    @Test
    void verificaPivoteo() {

    double[][] A = {
        {0, 2},
        {1, 1}
    };

    double[] b = {4, 3};

    GaussJordan jordan = new GaussJordan(A, b, 2);

    double[] x = jordan.resolver();

    assertEquals(1.0, x[0], 1e-6, "pivoteo x1");
    assertEquals(2.0, x[1], 1e-6, "pivoteo x2");
}


}
