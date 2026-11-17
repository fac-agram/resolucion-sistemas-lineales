package unsa.sistemas.model.iterative;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import unsa.sistemas.model.trace.Traza;

public class MejoramientoIterativoTest {

    private static final double[][] A_CANONICA = {
        {2.0, 1.0},
        {1.0, 3.0}
    };
    private static final double[] B_CANONICA = {3.0, 5.0};
    private static final double[] SOLUCION_ESPERADA = {0.8, 1.4};

    @Test
    void verificaSolucionDirectaCanonica() {
        MejoramientoIterativo m = new MejoramientoIterativo(A_CANONICA, B_CANONICA, 2);
        double[] x = m.resolver();

        assertNotNull(x, "La solución no debe ser null");
        assertArrayEquals(SOLUCION_ESPERADA, x, 1e-6, "Solución debe coincidir con la solución esperada");
        assertTrue(m.convergio(), "Debe marcar convergencia exitosa");
        assertTrue(m.getIteraciones() >= 0, "Iteraciones no negativas");
    }

    @Test
    void verificaMejoramientoConX0InicialPerturbado() {
        // x0 inicial alejado de la solución real
        double[] x0 = {0.0, 0.0};
        Traza traza = new Traza();
        MejoramientoIterativo m = new MejoramientoIterativo(A_CANONICA, B_CANONICA, x0, 2, traza);
        double[] x = m.resolver();

        assertNotNull(x, "La solución no debe ser null");
        assertArrayEquals(SOLUCION_ESPERADA, x, 1e-6, "Solución corregida debe coincidir con la solución esperada");
        assertTrue(m.convergio(), "Debe converger");
        assertTrue(m.getIteraciones() > 0, "Debe haber realizado al menos 1 iteración de corrección");
        assertTrue(m.getErrorFinal() <= 1e-7, "Error final dentro de la tolerancia");
        assertTrue(traza.totalPasos() > 0, "Traza debe contener pasos");
    }

    @Test
    void verificaSistema3x3ConTraza() {
        double[][] A = {
            {4.0, 1.0, 2.0},
            {2.0, 4.0, -1.0},
            {1.0, 1.0, 5.0}
        };
        double[] b = {9.0, -5.0, 7.0};
        // Solución esperada calculada por Gauss: x = [3.0, -2.0, 1.0] (4*3 - 2 + 2 = 12? Wait: 4*3 - 2 + 2*1 = 12 != 9)
        // Resolvamos con eliminación gaussiana estándar:
        Traza traza = new Traza();
        MejoramientoIterativo m = new MejoramientoIterativo(A, b, 3, traza);
        double[] x = m.resolver();

        assertNotNull(x);
        assertTrue(m.convergio());
        // Verificamos que A * x ≈ b:
        for (int i = 0; i < 3; i++) {
            double ax = 0.0;
            for (int j = 0; j < 3; j++) {
                ax += A[i][j] * x[j];
            }
            org.junit.jupiter.api.Assertions.assertEquals(b[i], ax, 1e-6, "Fila " + i + " satisface Ax = b");
        }
        assertTrue(traza.totalPasos() >= 1, "Debe registrar pasos en la traza");
    }
}
