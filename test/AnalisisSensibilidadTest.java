import unsa.sistemas.model.matrix.AnalisisSensibilidad;
import unsa.sistemas.model.matrix.Matriz;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AnalisisSensibilidadTest {

    /** Identidad: ‖I‖=1, ‖I⁻¹‖=1, cond = 1 (caso mínimo). */
    @Test
    void verificaCondicionIdentidad() {
        Matriz i = new Matriz(new double[][]{{1, 0}, {0, 1}});
        double cond = AnalisisSensibilidad.calcularCondicion(i, i);
        assertEquals(1.0, cond, 1e-9, "cond(I)");
    }

    /** A = [[4,1],[1,3]] con su inversa exacta: cond = 25/11 ≈ 2.272727. */
    @Test
    void verificaCondicionMatrizConocida() {
        Matriz a = new Matriz(new double[][]{{4, 1}, {1, 3}});
        Matriz invA = new Matriz(new double[][]{
                {3.0 / 11, -1.0 / 11},
                {-1.0 / 11, 4.0 / 11}});

        double cond = AnalisisSensibilidad.calcularCondicion(a, invA);
        assertEquals(25.0 / 11.0, cond, 1e-9, "cond([[4,1],[1,3]])");
        assertTrue(cond >= 1.0, "cond >= 1");
    }

    /** [[1,1],[1,1+1e-8]]: det = 1e-8, casi singular -> cond enorme. */
    @Test
    void verificaMatrizCasiSingular() {
        double e = 1e-8;
        Matriz a = new Matriz(new double[][]{{1, 1}, {1, 1 + e}});
        // Inversa exacta: (1/e)·[[1+e, -1], [-1, 1]]
        Matriz invA = new Matriz(new double[][]{
                {(1 + e) / e, -1 / e},
                {-1 / e, 1 / e}});

        double cond = AnalisisSensibilidad.calcularCondicion(a, invA);
        assertTrue(cond > 1e8, "cond > 1e8 (mal condicionada)");
    }

    /** Null, no cuadradas, dimensiones distintas e inversa inconsistente. */
    @Test
    void verificaValidaciones() {
        assertThrows(IllegalArgumentException.class,
                () -> AnalisisSensibilidad.calcularCondicion(
                        null, new Matriz(new double[][]{{1, 0}, {0, 1}})), "null");
        assertThrows(IllegalArgumentException.class,
                () -> AnalisisSensibilidad.calcularCondicion(
                        new Matriz(new double[][]{{1, 2, 3}, {4, 5, 6}}), new Matriz(new double[][]{{1, 0}, {0, 1}})),
                "no cuadrada");
        assertThrows(IllegalArgumentException.class,
                () -> AnalisisSensibilidad.calcularCondicion(
                        new Matriz(new double[][]{{4, 1}, {1, 3}}), new Matriz(new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}})),
                "dimensiones distintas");
        // Pasar A en lugar de A⁻¹: cond = ‖A‖² da < 1 si A escala chico (0.5·I)
        assertThrows(IllegalArgumentException.class,
                () -> AnalisisSensibilidad.calcularCondicion(
                        new Matriz(new double[][]{{0.5, 0}, {0, 0.5}}), new Matriz(new double[][]{{0.5, 0}, {0, 0.5}})),
                "inversa inconsistente");
    }
}
