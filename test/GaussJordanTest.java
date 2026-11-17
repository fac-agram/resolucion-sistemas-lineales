import unsa.sistemas.model.direct.GaussJordan;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

import java.util.List;

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

    // La traza no debe contener pasos con índice negativo
    @Test
    void trazaSinIndicesNegativos() {

        Traza traza = new Traza();

        // Sistema que requiere pivoteo (primera fila empieza en 0)
        GaussJordan jordan = new GaussJordan(
            new double[][] {
                {0, 2, 1},
                {1, 0, 3},
                {2, 1, 0}
            },
            new double[] {5, 10, 7},
            3,
            traza
        );

        jordan.resolver();

        List<Paso> pasos = traza.getPasos();

        for (Paso p : pasos) {
            assertTrue(p.indice() >= 0,
                "Ningún paso debe tener índice negativo, encontrado: " + p.indice());
        }
    }

    // Los índices de la traza deben ser monótonamente crecientes
    @Test
    void trazaIndicesMonotonicos() {

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

        List<Paso> pasos = traza.getPasos();

        for (int i = 1; i < pasos.size(); i++) {
            assertTrue(
                pasos.get(i).indice() > pasos.get(i - 1).indice(),
                "Índice debe crecer: paso " + i + " tiene índice "
                + pasos.get(i).indice() + " pero el anterior era "
                + pasos.get(i - 1).indice()
            );
        }
    }

    // Tras resolver, la diagonal de G debe ser 1 (diagonal unitaria)
    @Test
    void verificaDiagonalUnitaria() {

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

        // El último paso de la traza muestra la matriz final
        List<Paso> pasos = traza.getPasos();
        double[][] matrizFinal = pasos.get(pasos.size() - 1).matriz();

        for (int i = 0; i < 3; i++) {
            assertEquals(1.0, matrizFinal[i][i], 1e-6,
                "G[" + i + "][" + i + "] debe ser 1 (diagonal unitaria)");
        }
    }

    // R2.2: un pivote que ya vale 1 no debe generar el paso cosmetico "/ 1.0"
    @Test
    void trazaSinNormalizacionesNoOp() {

        Traza traza = new Traza();

        // Primera fila con pivote exacto 1: la normalizacion de k=0 es no-op
        GaussJordan jordan = new GaussJordan(
            new double[][] {
                {1, 2},
                {3, 4}
            },
            new double[] {5, 11},
            2,
            traza
        );

        jordan.resolver();

        for (Paso p : traza.getPasos()) {
            assertFalse(p.operacion().contains("/ 1.0"),
                "la traza no debe contener normalizaciones no-op, encontrado: "
                + p.operacion());
        }

        // Sin el paso no-op quedan 5: snapshot + normalizacion k=1
        // + 2 eliminaciones + solucion final (antes: 6 con "F1 <- F1 / 1.0")
        assertEquals(5, traza.totalPasos(),
            "los pasos cosmeticos de no-op deben suprimirse");
    }

    // R2: tras un swap cuyo pivote no es 1, la normalizacion SI debe registrarse
    // (guarda contra sobre-supresion: suprimir toda normalizacion rompe esto)
    @Test
    void verificaNormalizacionTrasSwapSeRegistra() {

        Traza traza = new Traza();

        GaussJordan jordan = new GaussJordan(
            new double[][] {
                {0, 1},
                {2, 1}
            },
            new double[] {1, 1},
            2,
            traza
        );

        jordan.resolver();

        List<Paso> pasos = traza.getPasos();

        // verificado a mano: snapshot=0, "Intercambio F1 ↔ F2"=1, "F1 ← F1 / 2.0"=2
        Paso swap = pasos.get(1);
        assertEquals(1, swap.indice(), "el swap consume el indice 1");
        assertEquals("Intercambio F1 ↔ F2", swap.operacion(),
            "el swap se registra como paso numerado");

        Paso normalizacion = pasos.get(2);
        assertEquals(2, normalizacion.indice(),
            "la normalizacion tras el swap consume el indice 2");
        assertEquals("F1 ← F1 / 2.0", normalizacion.operacion(),
            "un pivote 2.0 tras swap no es no-op: debe normalizarse y registrarse");
    }

    // R6.1: Paso copia la matriz al registrar; mutaciones posteriores no lo alteran
    @Test
    void verificaPasoInmuneAMutaciones() {

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

        List<Paso> pasos = traza.getPasos();

        double[][] snapshot = pasos.get(0).matriz();
        double[][] final_ = pasos.get(pasos.size() - 1).matriz();

        assertEquals(4.0, snapshot[0][0], 0.0,
            "el paso 0 conserva A[0][0] = 4 tal como se registro");
        assertEquals(1.0, final_[0][0], 1e-12,
            "la matriz final si mutó: pivote normalizado a 1");
    }
}
