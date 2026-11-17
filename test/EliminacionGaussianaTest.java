
import unsa.sistemas.model.direct.EliminacionGaussiana;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EliminacionGaussianaTest {

    // Sistema con solución exacta [1, 2, 3]
    @Test
    void verificaSolucion() {

        double[][] A = {
            {4, 2, 2},
            {2, 5, 1},
            {2, 1, 3}
        };

        double[] b = {14, 15, 13};

        double[] x = new EliminacionGaussiana(A, b, 3).resolver();

        assertEquals(1.0, x[0], 1e-6, "x1");
        assertEquals(2.0, x[1], 1e-6, "x2");
        assertEquals(3.0, x[2], 1e-6, "x3");
    }

    // Comprueba que se registren los pasos
    @Test
    void verificaTraza() {

        Traza traza = new Traza();

        EliminacionGaussiana gauss = new EliminacionGaussiana(
            new double[][] {
                {4, 2, 2},
                {2, 5, 1},
                {2, 1, 3}
            },
            new double[] {14, 15, 13},
            3,
            traza
        );

        gauss.resolver();

        assertTrue(!traza.estaVacia(), "traza no vacia");
        assertTrue(traza.totalPasos() > 0, "cantidad de pasos > 0");
    }

    // Una matriz singular no debe permitir resolver el sistema
    @Test
    void verificaSistemaSingular() {

        assertThrows(ArithmeticException.class, () -> {

            double[][] A = {
                {1, 2},
                {2, 4}
            };

            double[] b = {3, 6};

            new EliminacionGaussiana(A, b, 2).resolver();

        }, "matriz singular detectada");
    }

    @Test
    void verificaPivoteo() {

        double[][] A = {
            {0, 2},
            {1, 1}
        };

        double[] b = {4, 3};

        double[] x = new EliminacionGaussiana(A, b, 2).resolver();

        assertEquals(1.0, x[0], 1e-6, "pivoteo x1");
        assertEquals(2.0, x[1], 1e-6, "pivoteo x2");
    }

    // La traza no debe contener pasos con índice negativo (ni siquiera en swaps)
    @Test
    void trazaSinIndicesNegativos() {

        Traza traza = new Traza();

        // Sistema que requiere pivoteo
        EliminacionGaussiana gauss = new EliminacionGaussiana(
            new double[][] {
                {0, 2, 1},
                {1, 0, 3},
                {2, 1, 0}
            },
            new double[] {5, 10, 7},
            3,
            traza
        );

        gauss.resolver();

        List<Paso> pasos = traza.getPasos();

        for (Paso p : pasos) {
            assertTrue(p.indice() >= 0,
                "Ningún paso debe tener índice negativo, encontrado: " + p.indice());
        }
    }
}
