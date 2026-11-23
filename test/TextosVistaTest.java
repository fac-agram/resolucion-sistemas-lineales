import unsa.sistemas.utils.TextosVista;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Pines byte-exact de los textos puros de la vista (TextosVista): el bloque
// de ecuaciones (R2.1/R2.2, primer pin headless del orden x1..xn de
// procedure-trace R3.1), la matriz aumentada [A|b] con alineación por
// columna (R3.1–R3.4) y el contrato de formateo sin notación científica
// (T7). Cada assertEquals completo es intencional: la cadena ES el contrato
// (estilo ValidadorParametrosTest); no weaken a contains-checks. Cero
// JavaFX: esta clase solo importa TextosVista.
public class TextosVistaTest {

    /** R2.1: el bloque canónico 3x3 es idéntico byte a byte al render actual de mostrarSistema. */
    @Test
    void textoEcuaciones3x3EsIdenticoAlRenderActual() {
        // Romper: invertir el orden de etiquetas (x_n..x_1), tocar el token
        // "·" (U+00B7) o los separadores " + ", " = ", o perder el \n final.
        double[][] a = {{4, 1, 1}, {1, 3, 1}, {1, 1, 3}};
        double[] b = {6, 5, 5};
        assertEquals("4·x1 + 1·x2 + 1·x3 = 6\n1·x1 + 3·x2 + 1·x3 = 5\n1·x1 + 1·x2 + 3·x3 = 5\n",
                TextosVista.textoEcuaciones(a, b),
                "R2.1 canónico de 69 caracteres, byte a byte");
    }

    /** R2.2: negativos, ceros y decimales usan los tokens de signo sobre el valor absoluto. */
    @Test
    void textoEcuacionesMuestraSignosYceros() {
        // Romper: suprimir el término cero, formatear el valor con signo en
        // lugar del absoluto tras el token " - ", o el "-" líder sin espacio.
        double[][] a = {{-2, 0, 1.5}};
        double[] b = {4};
        assertEquals("-2·x1 + 0·x2 + 1.5·x3 = 4\n",
                TextosVista.textoEcuaciones(a, b),
                "R2.2: cero visible, signo antes del valor absoluto");
    }

    /** R3.1: celdas de anchos desiguales se alinean a la derecha por columna (relleno a la izquierda). */
    @Test
    void textoMatrizAumentadaAlineaColumnasDesiguales() {
        // Romper: alinear a la izquierda, usar ancho global (no por columna),
        // cambiar la barra ASCII "|" o el relleno del lado del pad.
        double[][] a = {{1}, {3e11}, {0.5}};
        double[] b = {3, 4, 5};
        assertEquals("[            1 | 3 ]\n[ 300000000000 | 4 ]\n[          0.5 | 5 ]\n",
                TextosVista.textoMatrizAumentada(a, b),
                "R3.1: anchos de columna 1, 12, 3 alinean a 12 con pad izquierdo");

        double[][] mezcla = {{-2, 0, 1.5}, {1, 3e11, 0.5}, {7, -3, 12.25}};
        double[] bMezcla = {4, 3, -0.5};
        assertEquals("[ -2            0   1.5 |    4 ]\n[  1 300000000000   0.5 |    3 ]\n[  7           -3 12.25 | -0.5 ]\n",
                TextosVista.textoMatrizAumentada(mezcla, bMezcla),
                "R3.1: magnitudes mezcladas, negativos con signo incluido en el ancho");
    }

    /** R3.2: el sistema canónico produce tres líneas de igual longitud con una sola barra cada una. */
    @Test
    void textoMatrizAumentada3x3TieneLineasIgualesConBarra() {
        // Romper: cambiar " | " por "|" sin espacios, agregar espacios dobles
        // o emitir más de una barra por línea.
        double[][] a = {{4, 1, 1}, {1, 3, 1}, {1, 1, 3}};
        double[] b = {6, 5, 5};
        String texto = TextosVista.textoMatrizAumentada(a, b);
        assertEquals("[ 4 1 1 | 6 ]\n[ 1 3 1 | 5 ]\n[ 1 1 3 | 5 ]\n", texto,
                "R3.2 canónico: una barra ASCII por línea");
        String[] lineas = texto.split("\n");
        assertEquals(3, lineas.length, "tres filas, sin línea vacía extra");
        assertEquals(13, lineas[0].length(), "línea 1 de longitud 13");
        assertEquals(lineas[0].length(), lineas[1].length(), "todas las líneas miden lo mismo");
        assertEquals(lineas[1].length(), lineas[2].length(), "todas las líneas miden lo mismo");
        for (String linea : lineas) {
            assertEquals(1, linea.chars().filter(c -> c == '|').count(),
                    "exactamente un separador '|' por línea");
        }
    }

    /** R3.3: la celda NaN se renderiza "NaN" (ancho 3) sin excepción, coherente con formatear. */
    @Test
    void textoMatrizAumentadaCelulaNaNUsaNan() {
        // Romper: dejar que NaN caiga a BigDecimal (String "NaN" distinto o
        // excepción) o paddear la celda con espacios internos.
        double[][] a = {{1, Double.NaN}, {3, 4}};
        double[] b = {5, 6};
        assertEquals("[ 1 NaN | 5 ]\n[ 3   4 | 6 ]\n",
                TextosVista.textoMatrizAumentada(a, b),
                "R3.3: NaN ocupa ancho 3 y la columna se alinea a ese ancho");
    }

    /** R3.4: una sola columna de A conserva plantilla, separador y offsets. */
    @Test
    void textoMatrizAumentadaUnaColumna() {
        // Romper: especial-casear columnas=1 o mover el separador " | ".
        double[][] a = {{2}, {3}};
        double[] b = {1, 6};
        assertEquals("[ 2 | 1 ]\n[ 3 | 6 ]\n",
                TextosVista.textoMatrizAumentada(a, b),
                "R3.4: celda A + barra + celda b, offsets consistentes");
    }

    /** T7: formatear nunca emite notación científica (contraste con formatearNumero de ValidadorParametros). */
    @Test
    void formatearNuncaUsaNotacionCientifica() {
        // Romper: fusionar este helper con ValidadorParametros.formatearNumero
        // (que sí da "1E-7"), perder una guarda o usar String.valueOf.
        assertEquals("0", TextosVista.formatear(0.0), "0.0 -> 0");
        assertEquals("0", TextosVista.formatear(-0.0), "-0.0 -> 0 (guarda valor == 0.0)");
        assertEquals("NaN", TextosVista.formatear(Double.NaN), "NaN -> NaN");
        assertEquals("Infinity", TextosVista.formatear(Double.POSITIVE_INFINITY), "+Inf -> Infinity");
        assertEquals("-Infinity", TextosVista.formatear(Double.NEGATIVE_INFINITY), "-Inf -> -Infinity");
        assertEquals("0.0000001", TextosVista.formatear(1e-7), "1e-7 -> decimal plano, NUNCA 1E-7");
        assertEquals("0.5", TextosVista.formatear(0.5), "0.5 -> 0.5");
        assertEquals("300000000000", TextosVista.formatear(3e11), "3e11 -> decimal plano de 12 dígitos");
        assertEquals("100", TextosVista.formatear(100.0), "100.0 -> 100 (ceros trailing fuera)");
        assertEquals("2.5", TextosVista.formatear(2.50), "2.50 -> 2.5");
        assertEquals("1000000000000000", TextosVista.formatear(1e15), "1e15 -> decimal plano");
        assertEquals("0.00001", TextosVista.formatear(1e-5), "1e-5 -> decimal plano");
        // Divergencia bloqueada a propósito: el contrato de la vista NO es el
        // del validador; merging los helpers rompería estos pines.
        assertEquals("1E-7", unsa.sistemas.utils.ValidadorParametros.formatearNumero(1e-7),
                "formatearNumero mantiene su contrato científico: helpers separados");
    }
}
