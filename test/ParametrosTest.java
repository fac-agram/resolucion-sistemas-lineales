import unsa.sistemas.model.iterative.Parametros;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// R1 subset (C1): el record Parametros valida su dominio al construirse
// delegando a ValidadorParametros, con las copias españolas byte-exact
// (pines del contrato 1-3). La equivalencia con los constructores viejos
// y el resto de escenarios llegan en C2 junto con el plumbing.
public class ParametrosTest {

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
}
