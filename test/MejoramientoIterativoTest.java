package unsa.sistemas.model.iterative;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import unsa.sistemas.controller.SistemaController;
import unsa.sistemas.controller.SistemaController.Estado;
import unsa.sistemas.controller.SistemaController.Resultado;
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
        // R1.2: el x0 de Gauss ya cumple la tolerancia, asi que 0 correcciones;
        // >= 0 era trivialmente verdadero y no discriminaba nada.
        assertEquals(0, m.getIteraciones(), "El x0 gaussiano converge sin correcciones");
        assertEquals(8.881784197001252e-16, m.getErrorFinal(), 1e-15,
                "‖r‖∞ inicial al orden del error hacia atras medido");
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

    /**
     * R2.1: con b = {1, NaN} el residuo medido es no finito, asi que el
     * flag de convergencia NUNCA debe ser true (el defecto shipped mentia
     * con convergio=true). El iterado calculado se devuelve sin sanitizar.
     */
    @Test
    void verificaEntradaNaNNoDeclaraConvergencia() {
        double[][] A = {{1.0, 2.0}, {3.0, 5.0}};
        double[] b = {1.0, Double.NaN};
        MejoramientoIterativo m = new MejoramientoIterativo(A, b, 2);
        double[] x = m.resolver();

        assertFalse(m.convergio(), "Un residuo NaN jamas puede declarar convergencia");
        assertNotNull(x, "El iterado calculado se devuelve sin sanitizar");
        assertEquals(2, x.length, "La solucion conserva el orden del sistema");
        for (double v : x) {
            assertTrue(Double.isNaN(v), "El iterado conserva los NaN calculados (sin sanitizar)");
        }
    }

    @Test
    void verificaSistema3x3ConTraza() {
        double[][] A = {
            {4.0, 1.0, 2.0},
            {2.0, 4.0, -1.0},
            {1.0, 1.0, 5.0}
        };
        double[] b = {9.0, -5.0, 7.0};
        Traza traza = new Traza();
        MejoramientoIterativo m = new MejoramientoIterativo(A, b, 3, traza);
        double[] x = m.resolver();

        assertNotNull(x);
        assertTrue(m.convergio());
        // Verdad de suelo derivada a mano (regla de Cramer, det = 69):
        //   x = (47/23, -133/69, 95/69). Se compara el vector contra la
        //   solucion exacta, no el proxy Ax ~ b (R4.1/R6.1).
        assertEquals(47.0 / 23.0, x[0], 1e-6, "x1 = 47/23");
        assertEquals(-133.0 / 69.0, x[1], 1e-6, "x2 = -133/69");
        assertEquals(95.0 / 69.0, x[2], 1e-6, "x3 = 95/69");
        assertTrue(traza.totalPasos() >= 1, "Debe registrar pasos en la traza");
    }

    /**
     * R1.3: cada llamada a resolver() reinicia el estado. Como Resolutor
     * clona A y b, "entradas cambiadas" en una misma instancia no es
     * alcanzable; la prueba observable equivalente es ejecutar dos veces
     * y exigir resultados idénticos (sin acumulación entre carreras).
     */
    @Test
    void verificaDobleEjecucionRepiteResultados() {
        MejoramientoIterativo m = new MejoramientoIterativo(A_CANONICA, B_CANONICA, 2);
        double[] x1 = m.resolver();
        int iter1 = m.getIteraciones();
        double err1 = m.getErrorFinal();
        boolean conv1 = m.convergio();

        double[] x2 = m.resolver();

        assertArrayEquals(x1, x2, 0.0, "La segunda carrera reproduce el iterado");
        assertEquals(iter1, m.getIteraciones(), "Las iteraciones no se acumulan");
        assertEquals(err1, m.getErrorFinal(), "El error final corresponde solo a la ultima carrera");
        assertEquals(conv1, m.convergio(), "La bandera corresponde solo a la ultima carrera");
    }

    /**
     * R2.2: desborde intermedio. Con x0 = {MAX_VALUE, 0} la corrección
     * 1 produce -Inf + MAX = NaN en el iterado; antes del fix la norma
     * mentia 0.0 y el solver declaraba true/1/0.0. Ahora: false/1000/NaN.
     */
    @Test
    void verificaDesbordeIntermedioNoDeclaraConvergencia() {
        double[][] A = {{10.0, 0.0}, {0.0, 1.0}};
        double[] b = {0.0, 0.0};
        double[] x0 = {Double.MAX_VALUE, 0.0};
        MejoramientoIterativo m = new MejoramientoIterativo(A, b, x0, 2);
        m.resolver();

        assertFalse(m.convergio(), "Un residuo no finito jamas declara convergencia");
        assertEquals(1000, m.getIteraciones(), "La carrera llega al tope de correcciones");
        assertTrue(Double.isNaN(m.getErrorFinal()), "El error final honesto es NaN");
    }

    /**
     * R4.2: {{1,2},{3,4}} / b={2,5} (solución exacta (1, 0.5) por Cramer,
     * det = −2) no es diagonal dominante: el Jacobi plano agota el tope
     * con bandera falsa, y el mejoramiento sí converge. El contraste con
     * Jacobi se ejecuta en el mismo test con el resolutor real.
     */
    @Test
    void verificaRefinamientoConvergeDondeJacobiDiverge() {
        double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
        double[] b = {2.0, 5.0};

        MejoramientoIterativo m = new MejoramientoIterativo(A, b, 2);
        double[] x = m.resolver();

        assertArrayEquals(new double[]{1.0, 0.5}, x, 1e-6, "Verdad de suelo (1, 0.5)");
        assertTrue(m.convergio(), "El refinamiento converge donde Jacobi no");

        Jacobi j = new Jacobi(A, b, 2);
        double[] xj = j.resolver();
        assertFalse(j.convergio(), "Jacobi plano no convergente en el mismo sistema");
        assertEquals(1000, j.getIteraciones(), "Jacobi agota el tope");
        assertEquals(2, xj.length, "La aproximacion de Jacobi conserva el orden");
        for (double v : xj) {
            assertTrue(Double.isFinite(v), "La aproximacion divergente de Jacobi sigue siendo finita");
        }
    }

    /**
     * R5.1: sistema singular → el ArithmeticException de Gauss (pivote
     * nulo en la aproximación inicial) propaga fuera de resolver() y la
     * bandera nunca declaró convergencia.
     */
    @Test
    void verificaSingularLanzaSinDeclararConvergencia() {
        double[][] A = {{1.0, 2.0}, {2.0, 4.0}};
        double[] b = {3.0, 6.0};
        MejoramientoIterativo m = new MejoramientoIterativo(A, b, 2);

        assertThrows(ArithmeticException.class, m::resolver,
                "El sistema singular debe fallar en voz alta");
        assertFalse(m.convergio(), "Tras el lanzamiento la bandera no declara convergencia");
    }

    /**
     * R5.2 (testigo sobre la carrera NaN): un residuo que nunca cumple
     * ‖r‖∞ ≤ 1e-7 agota el tope: bandera falsa, exactamente 1000
     * correcciones y el último iterado calculado se devuelve sin
     * modificar. (El tope con iterado finito es inalcanzable para este
     * algoritmo: la corrección resuelve A·z = r exactamente; ver design.)
     */
    @Test
    void verificaTopeAgotadoDevuelveUltimoIterado() {
        double[][] A = {{1.0, 2.0}, {3.0, 5.0}};
        double[] b = {1.0, Double.NaN};
        MejoramientoIterativo m = new MejoramientoIterativo(A, b, 2);
        double[] x = m.resolver();

        assertFalse(m.convergio(), "Tope agotado sin convergencia");
        assertEquals(1000, m.getIteraciones(), "Se ejecutaron las 1000 correcciones");
        assertNotNull(x, "Se devuelve el ultimo iterado calculado");
        assertEquals(2, x.length, "El iterado conserva el orden del sistema");
        for (double v : x) {
            assertTrue(Double.isNaN(v), "El ultimo iterado es el calculado, sin sanitizar");
        }
    }

    /**
     * R4.2 — el tope N sobre MEJORAMIENTO con iterado FINITO (hueco dejado por
     * la desviacion #5 del apply: el unico test de tope de la suite corrria
     * sobre Jacobi, y aqui el tope solo se veia con iterados NaN). La nuance
     * de la tabla D decia que la correccion resuelve A*z = r "exactamente";
     * en aritmetica IEEE el residuo corregido no puede bajar del piso-ulp del
     * producto A*x. Fixture: b = {3e11, 5e11+1} mantiene los numeros literales
     * del escenario (eps=1e-7, N=3): la solucion verdadera {79999999999.8,
     * 140000000000.4} no es representable, la semilla Gauss deja
     * ||r||inf = 2^-14 > eps (el bucle entra de verdad) y cada correccion
     * queda sub-½ulp de x: punto fijo con ||r||inf clavado en 2^-14
     * (6.103515625e-05), ~610 veces encima de eps. Sonda: con N=1000 agota
     * las 1000 correcciones con iterados finitos, bit-identica entre carreras.
     */
    @Test
    void verificaTopeDeIteracionesHonrado() {
        // Romper: `k <= maxIteraciones` -> `k < maxIteraciones` (off-by-one del tope).
        double[] b = {3e11, 5e11 + 1};
        Parametros parametros = new Parametros(1.5, 1e-7, 3);
        MejoramientoIterativo m = new MejoramientoIterativo(A_CANONICA, b, 2, parametros);
        double[] x = m.resolver();

        assertFalse(m.convergio(), "Con el tope en 3 la carrera no convergio");
        assertEquals(3, m.getIteraciones(), "Correcciones ejecutadas == tope 3 exacto");
        assertEquals(6.103515625e-05, m.getErrorFinal(), 0.0,
                "El residuo anclado es exactamente 2^-14, bit-a-bit");
        for (double v : x) {
            assertTrue(Double.isFinite(v), "El tope se alcanza con iterado finito, sin NaN/Inf");
        }
        // Verdad de suelo: el anclado ulp no destruye la solucion (error relativo ~1e-16).
        assertEquals(79999999999.8, x[0], 1e-3, "x1 = 79999999999.8 dentro de 1e-3 absoluto");
        assertEquals(140000000000.4, x[1], 1e-3, "x2 = 140000000000.4 dentro de 1e-3 absoluto");

        // Semantica de mensaje del escenario R4.2, en el controlador con el mismo fixture.
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, b, SistemaController.MEJORAMIENTO, parametros);
        assertEquals(Estado.NO_CONVERGIO, r.getEstado(), "Estado honesto: NO_CONVERGIO");
        assertTrue(r.getMensaje().contains("(3)"), "El mensaje nombra el tope 3");
        assertEquals(3, r.getConvergencia().iteraciones(), "El controlador refleja el tope agotado");
        assertNotNull(r.getSolucion(), "Se entrega la ultima aproximacion finita (R8.1)");
    }
}
