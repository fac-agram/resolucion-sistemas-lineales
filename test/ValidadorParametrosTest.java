import unsa.sistemas.utils.ValidadorParametros;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Pines del contrato de ValidadorParametros: copias españolas byte-exact
// (Contract Pins 1-6), límites inclusivos/abiertos, parseo double del N y
// formato de número. Cada assertEquals completo es intencional: la cadena
// combinada ES el contrato (amendment A1); no weaken a contains-checks.
public class ValidadorParametrosTest {

    /** R5.3: ω fuera de (0,2) —incluido NaN— rechaza con la copia 2 exacta; ω válido pasa. */
    @Test
    void rechazaOmegaFueraDelRangoAbierto() {
        // Romper: abrir el rango (omega >= 0) o cambiar la copia 2.
        IllegalArgumentException e0 = assertThrows(IllegalArgumentException.class,
                () -> ValidadorParametros.validarDominio(0.0, 1e-7, 1000),
                "ω = 0 fuera de (0,2)");
        assertEquals("El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: 0.",
                e0.getMessage(), "ω = 0 usa la copia 2 byte-exact");

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> ValidadorParametros.validarDominio(2.0, 1e-7, 1000),
                "ω = 2 fuera de (0,2) (espejo de SOR)");
        assertEquals("El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: 2.",
                e2.getMessage(), "ω = 2 usa la copia 2 byte-exact");

        IllegalArgumentException eNaN = assertThrows(IllegalArgumentException.class,
                () -> ValidadorParametros.validarDominio(Double.NaN, 1e-7, 1000),
                "ω = NaN rechazado");
        assertEquals("El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: NaN.",
                eNaN.getMessage(), "ω = NaN renderiza NaN en la copia 2");

        ValidadorParametros.validarDominio(1.0, 1e-7, 1000);
        ValidadorParametros.validarDominio(1.999, 1e-7, 1000);
        ValidadorParametros.validarDominio(0.001, 1e-7, 1000);
    }

    /** R5.1/R5.2: ε = 1e-13 y ε = 1e-2 rechazan con la copia 1; fronteras inclusivas pasan. */
    @Test
    void rechazaToleranciaFueraDeLimitesInclusivos() {
        // Romper: volver los límites exclusivos o tocar la copia 1.
        IllegalArgumentException ePiso = assertThrows(IllegalArgumentException.class,
                () -> ValidadorParametros.validarDominio(1.5, 1e-13, 1000),
                "ε = 1e-13 bajo el piso 1e-12");
        assertEquals("La tolerancia debe estar entre 1e-12 y 1e-3. Valor recibido: 1E-13.",
                ePiso.getMessage(), "ε = 1e-13 usa la copia 1 con formatearNumero");

        IllegalArgumentException eTecho = assertThrows(IllegalArgumentException.class,
                () -> ValidadorParametros.validarDominio(1.5, 1e-2, 1000),
                "ε = 1e-2 sobre el techo 1e-3");
        assertEquals("La tolerancia debe estar entre 1e-12 y 1e-3. Valor recibido: 0.01.",
                eTecho.getMessage(), "ε = 1e-2 usa la copia 1 con formatearNumero");

        ValidadorParametros.validarDominio(1.5, 1e-12, 1000);
        ValidadorParametros.validarDominio(1.5, 1e-3, 1000);
    }

    /** R5.4: N = 0 y N = 10001 rechazan con la copia 3; el 2.5 llega a la copia 3 (no a la 4) por errores(). */
    @Test
    void rechazaIteracionesFueraDeRangoYNoEnteras() {
        // Romper: cerrar el rango [1,10000] o route-ear 2.5 a la copia 4 (D3).
        IllegalArgumentException e0 = assertThrows(IllegalArgumentException.class,
                () -> ValidadorParametros.validarDominio(1.5, 1e-7, 0),
                "N = 0 rechazado");
        assertEquals("El máximo de iteraciones debe ser un entero entre 1 y 10000. Valor recibido: 0.",
                e0.getMessage(), "N = 0 usa la copia 3 byte-exact");

        IllegalArgumentException e10001 = assertThrows(IllegalArgumentException.class,
                () -> ValidadorParametros.validarDominio(1.5, 1e-7, 10001),
                "N = 10001 rechazado");
        assertEquals("El máximo de iteraciones debe ser un entero entre 1 y 10000. Valor recibido: 10001.",
                e10001.getMessage(), "N = 10001 usa la copia 3 byte-exact");

        // Por la UI N llega como texto: 2.5 parsea como double y debe caer
        // en la copia de rango (D3), no en la de no-numérico (copia 4).
        assertEquals(
                java.util.List.of("El máximo de iteraciones debe ser un entero entre 1 y 10000. Valor recibido: 2.5."),
                ValidadorParametros.errores("1.5", "1e-7", "2.5"),
                "N = 2.5 (no entero) usa la copia 3");

        ValidadorParametros.validarDominio(1.5, 1e-7, 1);
        ValidadorParametros.validarDominio(1.5, 1e-7, 10000);
    }

    /** R5.4: vacío/no-numérico produce la copia 4 con su {nombre}; null = campo oculto, se omite. */
    @Test
    void textoInvalidoProduceCopiaDeCampo() {
        // Romper: perder un {nombre}, fusionar los mensajes o rechazar null.
        assertEquals(
                java.util.List.of("El campo Factor de relajación ω no contiene un número válido."),
                ValidadorParametros.errores("abc", "1e-7", "1000"),
                "ω no numérico → copia 4 con su nombre");

        assertEquals(
                java.util.List.of("El campo Tolerancia ε no contiene un número válido."),
                ValidadorParametros.errores("1.5", "", "1000"),
                "ε vacío → copia 4 con su nombre");

        assertEquals(
                java.util.List.of("El campo Máximo de iteraciones no contiene un número válido."),
                ValidadorParametros.errores("1.5", "1e-7", "mil"),
                "N no numérico → copia 4 con su nombre");

        assertTrue(ValidadorParametros.errores(null, "1e-7", "1000").isEmpty(),
                "ω null = campo oculto: se omite, no se valida");
        assertTrue(ValidadorParametros.errores(null, null, null).isEmpty(),
                "tres campos ocultos no producen errores");

        // Varios campos inválidos a la vez: un error por campo, en orden ω, ε, N.
        assertEquals(
                java.util.List.of(
                        "El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: 0.",
                        "La tolerancia debe estar entre 1e-12 y 1e-3. Valor recibido: 0.01.",
                        "El máximo de iteraciones debe ser un entero entre 1 y 10000. Valor recibido: 0."),
                ValidadorParametros.errores("0", "1e-2", "0"),
                "los tres errores aparecen juntos en orden de campo");
    }

    /** D6: formatearNumero — pines del probe (BigDecimal stripTrailingZeros + guards). */
    @Test
    void formatearNumeroUsaElFormatoCanonico() {
        // Romper: String.valueOf (da 1.0E-7), toPlainString (da 0.0000001) o perder guards.
        assertEquals("1E-7", ValidadorParametros.formatearNumero(1e-7), "1e-7 -> 1E-7");
        assertEquals("1E-12", ValidadorParametros.formatearNumero(1e-12), "1e-12 -> 1E-12");
        assertEquals("0.0001", ValidadorParametros.formatearNumero(1e-4), "1e-4 -> 0.0001");
        assertEquals("2.5", ValidadorParametros.formatearNumero(2.5), "2.5 -> 2.5");
        assertEquals("10001", ValidadorParametros.formatearNumero(10001.0), "10001 -> 10001");
        assertEquals("0", ValidadorParametros.formatearNumero(0.0), "0 -> 0");
        assertEquals("NaN", ValidadorParametros.formatearNumero(Double.NaN), "NaN -> NaN");
        assertEquals("Infinito", ValidadorParametros.formatearNumero(Double.POSITIVE_INFINITY), "+Inf -> Infinito");
        assertEquals("-Infinito", ValidadorParametros.formatearNumero(Double.NEGATIVE_INFINITY), "-Inf -> -Infinito");
    }

    /** R6: la nota laxa es la copia 5 byte-exact y sólo con ε estrictamente mayor que 1e-5. */
    @Test
    void notaLaxaEsByteExactaYSoloEstricta() {
        // Romper: invertir la comparación (>=), tocar la copia 5 o el umbral EPSILON_LAXA.
        assertEquals("Tolerancia laxa (0.0001): el error por componente puede acercarse a la tolerancia.",
                ValidadorParametros.notaToleranciaLaxa(1e-4),
                "ε = 1e-4 produce la copia 5 con ε formateado");
        assertNull(ValidadorParametros.notaToleranciaLaxa(1e-5),
                "ε = 1e-5 exacta NO es laxa (regla estricta ε > 1e-5)");
        assertNull(ValidadorParametros.notaToleranciaLaxa(1e-7),
                "ε = 1e-7 (defecto) silenciosa");
    }

    /** R4: el eco de tolerancia es la copia 6 aplicada a formatearNumero. */
    @Test
    void ecoToleranciaUsaLaCopiaSeis() {
        // Romper: tocar la copia 6 o el formato del número.
        assertEquals("ε utilizado: 1E-7", ValidadorParametros.ecoTolerancia(1e-7),
                "eco del defecto 1e-7");
        assertEquals("ε utilizado: 0.0001", ValidadorParametros.ecoTolerancia(1e-4),
                "eco de ε = 1e-4");
    }
}
