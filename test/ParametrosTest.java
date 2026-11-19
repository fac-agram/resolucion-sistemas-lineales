import unsa.sistemas.controller.SistemaController;
import unsa.sistemas.controller.SistemaController.Estado;
import unsa.sistemas.controller.SistemaController.ParametroUi;
import unsa.sistemas.controller.SistemaController.Resultado;
import unsa.sistemas.model.iterative.Parametros;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Parametros: R1 (C1: dominio, defectos, copias byte-exact — el registro
// es la defensa del modelo, R5.5) y R2-R7 + D7 (C2: el plumbing real —
// defaults ≡ constructores viejos, ω por params, ε/N manejan el criterio,
// nota laxa, coexistencia byte-pinned A1, predicado de visibilidad).
// Cada assertEquals completo de mensaje es intencional: la cadena ES el
// contrato; no weaken a contains-checks.
public class ParametrosTest {

    // Sistema canónico (el mismo de SistemaControllerTest): [0.8, 1.4],
    // diagonalmente dominante.
    private static final double[][] A_CANONICA = {{2, 1}, {1, 3}};
    private static final double[] B_CANONICA = {3, 5};

    /** R1.1: defecto() reproduce exactamente las constantes históricas. */
    @Test
    void defectoIgualaLasConstantesActuales() {
        // Romper: cambiar 1.5 / 1e-7 / 1000 (los 82 tests viejos no lo cubren:
        // el dominio de ω sí, pero el valor exacto del default solo vive aquí).
        Parametros p = Parametros.defecto();
        assertEquals(1.5, p.omega(), 0.0, "ω defecto == ex SOR.OMEGA_DEFECTO");
        assertEquals(1e-7, p.epsilon(), 0.0, "ε defecto == ex TOLERANCIA");
        assertEquals(1000, p.maxIteraciones(), "N defecto == ex MAX_ITERACIONES");
        assertEquals(1.5, Parametros.OMEGA_POR_DEFECTO, 0.0, "constante pública ω");
        assertEquals(1e-7, Parametros.EPSILON_POR_DEFECTO, 0.0, "constante pública ε");
        assertEquals(1000, Parametros.MAX_ITERACIONES_POR_DEFECTO, "constante pública N");
    }

    /** R1.2: las fronteras inclusivas se aceptan (ε 1e-12/1e-3, N 1/10000, ω interno). */
    @Test
    void fronterasInclusivasSeAceptan() {
        // Romper: volver exclusivos los límites de ε o N.
        assertNotNull(new Parametros(1.5, 1e-12, 1000), "ε = piso 1e-12");
        assertNotNull(new Parametros(1.5, 1e-3, 1000), "ε = techo 1e-3");
        assertNotNull(new Parametros(1.0, 1e-7, 1000), "ω = 1.0");
        assertNotNull(new Parametros(1.5, 1e-7, 1), "N = 1");
        assertNotNull(new Parametros(1.5, 1e-7, 10000), "N = 10000");
    }

    /** R1.3 (ω): fuera de (0,2) —incluido el espejo SOR ω=2— lanza con la copia 2 exacta. */
    @Test
    void omegaFueraDeDominioLanzaCopiaExacta() {
        // Romper: relajar (0,2) o duplicar el mensaje en el modelo (deriva de copias).
        assertEquals("El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: 0.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(0.0, 1e-7, 1000)).getMessage(),
                "ω = 0: copia 2 byte-exact");
        assertEquals("El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: 2.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(2.0, 1e-7, 1000)).getMessage(),
                "ω = 2: copia 2 byte-exact");
    }

    /** R1.3 (ε): fuera de [1e-12, 1e-3] lanza con la copia 1 exacta. */
    @Test
    void epsilonFueraDeDominioLanzaCopiaExacta() {
        // Romper: ampliar el rango de ε o cambiar la copia 1.
        assertEquals("La tolerancia debe estar entre 1e-12 y 1e-3. Valor recibido: 1E-13.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(1.5, 1e-13, 1000)).getMessage(),
                "ε = 1e-13: copia 1 byte-exact");
        assertEquals("La tolerancia debe estar entre 1e-12 y 1e-3. Valor recibido: 0.01.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(1.5, 1e-2, 1000)).getMessage(),
                "ε = 1e-2: copia 1 byte-exact");
    }

    /** R1.3 (N): fuera de [1, 10000] lanza con la copia 3 exacta. */
    @Test
    void iteracionesFueraDeDominioLanzanCopiaExacta() {
        // Romper: ampliar el tope de N o cambiar la copia 3.
        assertEquals("El máximo de iteraciones debe ser un entero entre 1 y 10000. Valor recibido: 0.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(1.5, 1e-7, 0)).getMessage(),
                "N = 0: copia 3 byte-exact");
        assertEquals("El máximo de iteraciones debe ser un entero entre 1 y 10000. Valor recibido: 10001.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(1.5, 1e-7, 10001)).getMessage(),
                "N = 10001: copia 3 byte-exact");
    }

    /** R1.3 (NaN/±Inf): ningún no-finito sobrevive a la construcción; cada uno cae en su propia copia. */
    @Test
    void valoresNoFinitosSeRechazan() {
        // Romper: dejar pasar NaN/Inf (comparaciones sin el guardia !(...)).
        assertEquals("La tolerancia debe estar entre 1e-12 y 1e-3. Valor recibido: NaN.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(1.5, Double.NaN, 1000)).getMessage(),
                "ε = NaN: copia 1 con NaN");
        assertEquals("El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: Infinito.",
                assertThrows(IllegalArgumentException.class,
                        () -> new Parametros(Double.POSITIVE_INFINITY, 1e-7, 1000)).getMessage(),
                "ω = +Inf: copia 2 con Infinito");
        assertThrows(IllegalArgumentException.class,
                () -> new Parametros(Double.NEGATIVE_INFINITY, 1e-7, 1000),
                "ω = -Inf rechazado");
    }

    /** R2.2: defecto() reproduce bit a bit el comportamiento de los constructores viejos. */
    @Test
    void defectoEquivaleAConstructoresViejos() {
        // Romper: cambiar un default del record o un criterio de paro en la plomería.
        SistemaController c = new SistemaController();
        for (String metodo : new String[]{SistemaController.JACOBI, SistemaController.GAUSS_SEIDEL,
                SistemaController.SOR_METODO, SistemaController.RELAJACION, SistemaController.MEJORAMIENTO}) {
            Resultado viejo = c.resolver(A_CANONICA, B_CANONICA, metodo);
            Resultado nuevo = c.resolver(A_CANONICA, B_CANONICA, metodo, Parametros.defecto());
            assertEquals(viejo.getConvergencia().iteraciones(), nuevo.getConvergencia().iteraciones(),
                    metodo + ": mismas iteraciones con defecto()");
            assertEquals(viejo.getConvergencia().errorFinal(), nuevo.getConvergencia().errorFinal(), 0.0,
                    metodo + ": mismo errorFinal (bit a bit) con defecto()");
            assertEquals(viejo.getMensaje(), nuevo.getMensaje(), metodo + ": mismo mensaje con defecto()");
            assertArrayEquals(viejo.getSolucion(), nuevo.getSolucion(), 0.0, metodo + ": misma solución con defecto()");
        }
        // Equivalencia también por constructor: SOR legado (ω=1.5) ≡ SOR con defecto().
        unsa.sistemas.model.iterative.SOR legado =
                new unsa.sistemas.model.iterative.SOR(A_CANONICA, B_CANONICA, 2, 1.5);
        unsa.sistemas.model.iterative.SOR porParams =
                new unsa.sistemas.model.iterative.SOR(A_CANONICA, B_CANONICA, 2, Parametros.defecto());
        legado.resolver();
        porParams.resolver();
        assertEquals(legado.getIteraciones(), porParams.getIteraciones(), "SOR legado ≡ Parametros: iters");
        assertEquals(legado.getErrorFinal(), porParams.getErrorFinal(), 0.0, "SOR legado ≡ Parametros: error bit a bit");

        // Directos: el overload de 4 argumentos no les agrega ninguna diferencia.
        Resultado directoViejo = c.resolver(A_CANONICA, B_CANONICA, SistemaController.CROUT);
        Resultado directoNuevo = c.resolver(A_CANONICA, B_CANONICA, SistemaController.CROUT, Parametros.defecto());
        assertArrayEquals(directoViejo.getSolucion(), directoNuevo.getSolucion(), 0.0, "CROUT: misma solución bit a bit");
    }

    /** R2.3: los métodos directos aceptan un Parametros custom y lo ignoran (CROUT — GAUSS es stub). */
    @Test
    void directosIgnoranParametrosCustom() {
        // Romper: que algún camino directo consuma ε/N/ω del record.
        SistemaController c = new SistemaController();
        Parametros p = new Parametros(1.9, 1e-4, 5);
        Resultado tres = c.resolver(A_CANONICA, B_CANONICA, SistemaController.CROUT);
        Resultado cuatro = c.resolver(A_CANONICA, B_CANONICA, SistemaController.CROUT, p);
        assertEquals(Estado.OK, cuatro.getEstado(), "directo con p sigue OK");
        assertArrayEquals(tres.getSolucion(), cuatro.getSolucion(), 1e-6, "misma solución que el 3-arg");
        assertNull(cuatro.getConvergencia(), "directo sin convergencia, con o sin p");
    }

    /** R4.1: aflojar ε de 1e-7 a 1e-4 cuesta estrictamente menos iteraciones (Jacobi y SOR), ambas convergen. */
    @Test
    void escaneoDeToleranciaEsMonotonicoenIteraciones() {
        // Romper: que el criterio de paro use el literal 1e-7 en vez del campo (mutación M2).
        SistemaController c = new SistemaController();
        for (String metodo : new String[]{SistemaController.JACOBI, SistemaController.SOR_METODO}) {
            Resultado estricta = c.resolver(A_CANONICA, B_CANONICA, metodo, new Parametros(1.5, 1e-7, 1000));
            Resultado laxa = c.resolver(A_CANONICA, B_CANONICA, metodo, new Parametros(1.5, 1e-4, 1000));
            assertTrue(estricta.getConvergencia().convergio(), metodo + ": ε=1e-7 converge");
            assertTrue(laxa.getConvergencia().convergio(), metodo + ": ε=1e-4 converge");
            assertTrue(laxa.getConvergencia().iteraciones() < estricta.getConvergencia().iteraciones(),
                    metodo + ": ε=1e-4 necesita estrictamente menos iteraciones ("
                            + laxa.getConvergencia().iteraciones() + " < " + estricta.getConvergencia().iteraciones() + ")");
        }
    }

    /** R4.2/R8.1: el tope N=3 se honra — iteraciones==3, NO_CONVERGIO honesto y el mensaje nombra 3. */
    @Test
    void topeDeIteracionesSeHonra() {
        // Romper: off-by-one en el techo del bucle o reetiquetar el no-converger.
        // Nota (nuance de la tabla D): con MEJORAMIENTO una semilla Gauss que ya
        // satisface ε converge con iteraciones==0 legítimo — por eso el tope se
        // prueba sobre Jacobi, lento de convergencia en este fixture (necesita 20).
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI,
                new Parametros(1.5, 1e-7, 3));
        assertEquals(3, r.getConvergencia().iteraciones(), "iteraciones == tope 3");
        assertFalse(r.getConvergencia().convergio(), "convergio == false");
        assertEquals(Estado.NO_CONVERGIO, r.getEstado(), "estado NO_CONVERGIO");
        assertTrue(r.getMensaje().contains("(3)"), "el mensaje nombra el tope 3");
        assertNotNull(r.getSolucion(), "se entrega la última aproximación");
    }

    /** R3.1: ω=1.0 via Parametros se comporta exactamente como Gauss-Seidel (fixture SORTest:13-14). */
    @Test
    void omegaUnoPorParamsEquivaleAGaussSeidel() {
        // Romper: plomería de ω (que SOR ignore p.omega() o lo recocine).
        SistemaController c = new SistemaController();
        Resultado sor = c.resolver(A_CANONICA, B_CANONICA, SistemaController.SOR_METODO,
                new Parametros(1.0, 1e-7, 1000));
        Resultado gs = c.resolver(A_CANONICA, B_CANONICA, SistemaController.GAUSS_SEIDEL);
        assertEquals(Estado.OK, sor.getEstado(), "SOR(ω=1) OK");
        assertEquals(Estado.OK, gs.getEstado(), "GAUSS_SEIDEL OK");
        assertArrayEquals(gs.getSolucion(), sor.getSolucion(), 1e-6, "soluciones coinciden (1e-6)");
        assertEquals(gs.getConvergencia().iteraciones(), sor.getConvergencia().iteraciones(),
                "mismo conteo de iteraciones que GS");
    }

    /** R3.2 + D4: ω=2.0 viola el dominio en la construcción (copia 1-3 byte-exact); el null del 4-arg es ERROR. */
    @Test
    void defensasDelModeloYDelControlador() {
        // Romper: debilitar la validación del record o el guardia null del 4-arg.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Parametros(2.0, 1e-7, 1000), "ω = 2 no sobrevive a la construcción");
        assertEquals("El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: 2.",
                e.getMessage(), "copia 2 byte-exact en el modelo (R5.5)");

        SistemaController c = new SistemaController();
        Resultado nulo = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI, null);
        assertEquals(Estado.ERROR, nulo.getEstado(), "Parametros null → ERROR sin intentar resolver");
        assertEquals("Los parámetros no pueden ser null.", nulo.getMensaje(), "copia D4 byte-exact");
    }

    /** R6.1: ε=1e-4 converge, sigue OK y trae la nota laxa byte-exact nombrando al ε usado. */
    @Test
    void epsilonLaxaNotaYsigueOk() {
        // Romper: invertir la regla ε>1e-5 o cambiar la copia 5.
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI,
                new Parametros(1.5, 1e-4, 1000));
        assertEquals(Estado.OK, r.getEstado(), "ε laxa no degrada el estado");
        assertTrue(r.getMensaje().contains("Resuelto con éxito."), "éxito presente");
        assertTrue(r.getMensaje().contains(
                        "Tolerancia laxa (0.0001): el error por componente puede acercarse a la tolerancia."),
                "nota laxa byte-exact nombrando el ε usado");
    }

    /** R6.2: ε = 1e-5 exacta y ε = 1e-7 no producen ninguna nota laxa. */
    @Test
    void toleranciasEstrictasSilenciosas() {
        // Romper: usar >= en lugar de > contra EPSILON_LAXA.
        SistemaController c = new SistemaController();
        for (double eps : new double[]{1e-5, 1e-7}) {
            Resultado r = c.resolver(A_CANONICA, B_CANONICA, SistemaController.JACOBI,
                    new Parametros(1.5, eps, 1000));
            assertEquals(Estado.OK, r.getEstado(), "ε=" + eps + " converge OK");
            assertFalse(r.getMensaje().contains("Tolerancia laxa"), "ε=" + eps + " sin nota laxa");
        }
    }

    /**
     * R6.3 (A1, frágil a propósito): dominancia y laxa coexisten en el orden
     * dominancia→laxa unidas por espacio — la cadena combinada ES el contrato
     * (composición verificada en clasificar: MENSAJE_EXITO + " " + nota).
     * No soften a contains-checks.
     */
    @Test
    void coexistenciaDeNotasBytePinned() {
        // Romper: invertir el orden en unirNotas (mutación M3) o cambiar el joiner.
        double[][] aNoDominante = {{1, 1}, {1, 2}};
        double[] bNoDominante = {2, 3};
        SistemaController c = new SistemaController();
        Resultado r = c.resolver(aNoDominante, bNoDominante, SistemaController.JACOBI,
                new Parametros(1.5, 1e-4, 1000));
        assertEquals(Estado.OK, r.getEstado(), "coexistencia sigue OK");
        assertEquals("Resuelto con éxito. Nota: la matriz no es estrictamente diagonal dominante. Tolerancia laxa (0.0001): el error por componente puede acercarse a la tolerancia.",
                r.getMensaje(), "mensaje completo byte-pinned, orden dominancia→laxa");
    }

    /** D7: el predicado de visibilidad de los campos por método (tabla de 9 + null + desconocido). */
    @Test
    void predicadoDeVisibilidadSigueLaTabla() {
        // Romper: dar ω a un no-SOR, ocultárselo a SOR o mostrar algo a los directos (mutación M4).
        assertEquals(EnumSet.allOf(ParametroUi.class),
                SistemaController.parametrosVisiblesPara(SistemaController.SOR_METODO),
                "SOR ve ω, ε y N");
        EnumSet<ParametroUi> sinOmega = EnumSet.of(ParametroUi.TOLERANCIA, ParametroUi.MAX_ITERACIONES);
        for (String metodo : new String[]{SistemaController.JACOBI, SistemaController.GAUSS_SEIDEL,
                SistemaController.RELAJACION, SistemaController.MEJORAMIENTO}) {
            assertEquals(sinOmega, SistemaController.parametrosVisiblesPara(metodo),
                    metodo + " ve ε y N, sin ω");
        }
        EnumSet<ParametroUi> nada = EnumSet.noneOf(ParametroUi.class);
        for (String metodo : new String[]{SistemaController.GAUSS, SistemaController.GAUSS_JORDAN,
                SistemaController.CROUT, SistemaController.CHOLESKY}) {
            assertEquals(nada, SistemaController.parametrosVisiblesPara(metodo),
                    metodo + " (directo) no ve campos");
        }
        assertEquals(nada, SistemaController.parametrosVisiblesPara(null), "null no ve campos");
        assertEquals(nada, SistemaController.parametrosVisiblesPara("METODO_DESCONOCIDO"),
                "desconocido no ve campos");
    }
}
