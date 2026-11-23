package unsa.sistemas.utils;

import java.math.BigDecimal;

/**
 * Textos puros de la vista (headless, cero JavaFX): las cadenas exactas
 * que el controlador FX deposita en sus {@code TextArea}. Es la única
 * fuente de esos textos, de modo que el byte-pin de la vista puede
 * ejecutarse sin interfaz (precedente de pureza: {@link ValidadorParametros}).
 */
public final class TextosVista {

    private TextosVista() {
    }

    /**
     * Formatea un número para la vista: guardas para NaN/Infinity/0 y
     * {@code stripTrailingZeros().toPlainString()} para el resto (nunca
     * notación científica). Movido byte a byte desde
     * {@code SistemaFXController.formatear}.
     *
     * @param valor número a formatear
     * @return texto canónico de la vista
     */
    public static String formatear(double valor) {

        if (Double.isNaN(valor)) {
            return "NaN";
        }

        if (Double.isInfinite(valor)) {
            return valor > 0 ? "Infinity" : "-Infinity";
        }

        if (valor == 0.0) {
            return "0";
        }

        return BigDecimal.valueOf(valor)
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * Bloque de ecuaciones {@code "a·x1 + b·x2 ... = c"} fila a fila, con
     * etiquetas {@code x1..xn} en orden de columna (procedure-trace R3.1).
     * Extracción byte-identica del cuerpo de {@code mostrarSistema}:
     * coeficiente negativo en la primera posición lleva {@code "-"} inicial,
     * el resto usa los tokens {@code " + "} / {@code " - "} sobre el valor
     * absoluto formateado.
     *
     * @param a matriz de coeficientes (rectangular, garantizada por el lector)
     * @param b términos independientes
     * @return texto con {@code \n} al final de cada fila
     */
    public static String textoEcuaciones(double[][] a, double[] b) {

        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < a.length; i++) {

            for (int j = 0; j < a[i].length; j++) {

                double coeficiente = a[i][j];

                // Agregar el signo correspondiente
                if (j > 0) {
                    if (coeficiente < 0) {
                        texto.append(" - ");
                    } else {
                        texto.append(" + ");
                    }
                } else if (coeficiente < 0) {
                    texto.append("-");
                }

                // Mostrar el valor absoluto del coeficiente
                texto.append(formatear(Math.abs(coeficiente))).append("·x").append(j + 1);
            }
            texto.append(" = ").append(formatear(b[i])).append("\n");
        }

        return texto.toString();
    }

    /**
     * Matriz aumentada {@code "[ A | b ]"} en un solo bloque de texto: una
     * línea por ecuación; cada celda con {@link #formatear(double)} (signo
     * incluido); ancho por columna = máximo ancho formateado de esa columna,
     * con relleno a la izquierda (derecha alineada); un separador {@code " | "}
     * ASCII entre las columnas de A y la celda de b. Alineación 100% texto:
     * no depende de CSS ni de fuente.
     *
     * @param a matriz de coeficientes (rectangular, garantizada por el lector)
     * @param b términos independientes
     * @return bloque con {@code \n} al final de cada fila
     */
    public static String textoMatrizAumentada(double[][] a, double[] b) {

        int filas = a.length;
        int columnas = a[0].length;

        String[][] celdas = new String[filas][columnas];
        int[] anchos = new int[columnas];

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                celdas[i][j] = formatear(a[i][j]);
                if (celdas[i][j].length() > anchos[j]) {
                    anchos[j] = celdas[i][j].length();
                }
            }
        }

        String[] celdasB = new String[filas];
        int anchoB = 0;

        for (int i = 0; i < filas; i++) {
            celdasB[i] = formatear(b[i]);
            if (celdasB[i].length() > anchoB) {
                anchoB = celdasB[i].length();
            }
        }

        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < filas; i++) {

            texto.append("[ ");

            for (int j = 0; j < columnas; j++) {
                texto.append(" ".repeat(anchos[j] - celdas[i][j].length()));
                texto.append(celdas[i][j]).append(" ");
            }

            texto.append("| ");
            texto.append(" ".repeat(anchoB - celdasB[i].length()));
            texto.append(celdasB[i]).append(" ]\n");
        }

        return texto.toString();
    }
}
