package unsa.sistemas.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validación pura (headless, cero JavaFX) de los parámetros ajustables
 * de los métodos iterativos: factor de relajación ω, tolerancia ε y
 * máximo de iteraciones.
 *
 * <p>Es la <b>única fuente</b> de los límites, de las copias españolas
 * del contrato y del formato canónico de números: el registro
 * ({@code unsa.sistemas.model.iterative.Parametros}) reutiliza estos
 * mensajes al construirse, de modo que la validación de la interfaz y la
 * del modelo no pueden divergir. Este validador trabaja solo con
 * primitivos y texto: jamás importa {@code Parametros} (dependencia
 * unidireccional {@code model.iterative → utils}).</p>
 *
 * <p>Dos puertas, según el llamador:</p>
 * <ul>
 *   <li>{@link #validarDominio(double, double, int)} — para el modelo:
 *       lanza {@link IllegalArgumentException} con la copia exacta en la
 *       primera violación.</li>
 *   <li>{@link #errores(String, String, String)} — para la interfaz:
 *       acumula todos los problemas como texto legible sin lanzar nada,
 *       aceptando {@code null} para campos ocultos que no deben validarse.</li>
 * </ul>
 *
 * <p>El patrón de métodos estáticos sigue el precedente de
 * {@link ValidadorMatriz}.</p>
 */
public class ValidadorParametros {

    /** Límite inferior <b>inclusivo</b> de la tolerancia ε. */
    public static final double LIMITE_INFERIOR_EPSILON = 1e-12;

    /** Límite superior <b>inclusivo</b> de la tolerancia ε. */
    public static final double LIMITE_SUPERIOR_EPSILON = 1e-3;

    /** Umbral estricto: con {@code ε > EPSILON_LAXA} la resolución se anota como laxa. */
    public static final double EPSILON_LAXA = 1e-5;

    /** Cota inferior inclusiva del máximo de iteraciones. */
    private static final int MIN_ITERACIONES = 1;

    /** Cota superior inclusiva del máximo de iteraciones. */
    private static final int MAX_ITERACIONES_PERMITIDO = 10000;

    // ── Copias del contrato (byte-locked; único lugar donde aparecen) ──

    private static final String COPIA_TOLERANCIA =
            "La tolerancia debe estar entre 1e-12 y 1e-3. Valor recibido: %s.";

    private static final String COPIA_OMEGA =
            "El factor ω debe ser un número mayor que 0 y menor que 2. Valor recibido: %s.";

    private static final String COPIA_ITERACIONES =
            "El máximo de iteraciones debe ser un entero entre 1 y 10000. Valor recibido: %s.";

    private static final String COPIA_NUMERO_INVALIDO =
            "El campo %s no contiene un número válido.";

    private static final String COPIA_TOLERANCIA_LAXA =
            "Tolerancia laxa (%s): el error por componente puede acercarse a la tolerancia.";

    private static final String COPIA_ECO_TOLERANCIA =
            "ε utilizado: %s";

    /** Nombre de la copia 4 para el campo ω. */
    private static final String NOMBRE_OMEGA = "Factor de relajación ω";

    /** Nombre de la copia 4 para el campo ε. */
    private static final String NOMBRE_TOLERANCIA = "Tolerancia ε";

    /** Nombre de la copia 4 para el campo de iteraciones. */
    private static final String NOMBRE_ITERACIONES = "Máximo de iteraciones";

    /**
     * Valida el dominio de los tres parámetros para el modelo.
     *
     * <p>Límites: ε en {@code [1e-12, 1e-3]} (inclusivos), ω en
     * {@code (0, 2)} (abierto, espejo del constructor histórico de SOR)
     * y N en {@code [1, 10000]} (inclusivo). Cualquier NaN o ±Infinity
     * cae en el mensaje de su propio parámetro, con el valor renderizado
     * por {@link #formatearNumero(double)}.</p>
     *
     * @param omega          factor de relajación, debe cumplir 0 &lt; ω &lt; 2
     * @param epsilon        tolerancia de parada, en {@code [1e-12, 1e-3]}
     * @param maxIteraciones cota de iteraciones, en {@code [1, 10000]}
     * @throws IllegalArgumentException si algún parámetro viola su dominio,
     *         con la copia española exacta de ese parámetro como mensaje
     */
    public static void validarDominio(double omega, double epsilon, int maxIteraciones) {
        // Comparaciones con NaN dan false: el !(...) atrapa NaN e Infinity
        // y el mensaje muestra "NaN"/"Infinito" vía formatearNumero.
        if (!(omega > 0.0 && omega < 2.0)) {
            throw new IllegalArgumentException(String.format(COPIA_OMEGA, formatearNumero(omega)));
        }
        if (!(epsilon >= LIMITE_INFERIOR_EPSILON && epsilon <= LIMITE_SUPERIOR_EPSILON)) {
            throw new IllegalArgumentException(String.format(COPIA_TOLERANCIA, formatearNumero(epsilon)));
        }
        if (maxIteraciones < MIN_ITERACIONES || maxIteraciones > MAX_ITERACIONES_PERMITIDO) {
            throw new IllegalArgumentException(
                    String.format(COPIA_ITERACIONES, formatearNumero(maxIteraciones)));
        }
    }

    /**
     * Valida el texto crudo de los tres campos de la interfaz y devuelve
     * todos los errores encontrados (vacío si todo es válido).
     *
     * <p>Un argumento {@code null} significa campo oculto para el método
     * elegido y se omite sin validar. El texto vacío o no numérico
     * produce la copia 4 con el nombre del campo. Un N no entero
     * (p. ej. {@code "2.5"}) se parsea como double y cae en la copia de
     * rango —no en la de formato—; también {@code |x| > Integer.MAX_VALUE}.</p>
     *
     * @param omegaTexto       texto del campo ω, o {@code null} si está oculto
     * @param toleranciaTexto  texto del campo ε, o {@code null} si está oculto
     * @param iteracionesTexto texto del campo N, o {@code null} si está oculto
     * @return los mensajes de error en el orden ω, ε, N; nunca {@code null}
     */
    public static List<String> errores(String omegaTexto, String toleranciaTexto, String iteracionesTexto) {
        List<String> errores = new ArrayList<>();

        if (omegaTexto != null) {
            Double omega = parsear(omegaTexto);
            if (omega == null) {
                errores.add(String.format(COPIA_NUMERO_INVALIDO, NOMBRE_OMEGA));
            } else if (!(omega > 0.0 && omega < 2.0)) {
                errores.add(String.format(COPIA_OMEGA, formatearNumero(omega)));
            }
        }

        if (toleranciaTexto != null) {
            Double epsilon = parsear(toleranciaTexto);
            if (epsilon == null) {
                errores.add(String.format(COPIA_NUMERO_INVALIDO, NOMBRE_TOLERANCIA));
            } else if (!(epsilon >= LIMITE_INFERIOR_EPSILON && epsilon <= LIMITE_SUPERIOR_EPSILON)) {
                errores.add(String.format(COPIA_TOLERANCIA, formatearNumero(epsilon)));
            }
        }

        if (iteracionesTexto != null) {
            Double iteraciones = parsear(iteracionesTexto);
            if (iteraciones == null) {
                errores.add(String.format(COPIA_NUMERO_INVALIDO, NOMBRE_ITERACIONES));
            } else if (Math.abs(iteraciones) > Integer.MAX_VALUE
                    || iteraciones != Math.rint(iteraciones)
                    || iteraciones < MIN_ITERACIONES
                    || iteraciones > MAX_ITERACIONES_PERMITIDO) {
                // NaN != Math.rint(NaN) es true (NaN no es igual a nada),
                // así que no finito también cae aquí con su copia de rango.
                errores.add(String.format(COPIA_ITERACIONES, formatearNumero(iteraciones)));
            }
        }

        return errores;
    }

    /**
     * Nota de tolerancia laxa (copia 5) para la resolución con
     * {@code ε > 1e-5} estricto, o {@code null} si ε es suficientemente
     * estricta como para no advertir nada.
     *
     * @param epsilon tolerancia efectivamente usada
     * @return la nota byte-exact con ε formateado, o {@code null}
     */
    public static String notaToleranciaLaxa(double epsilon) {
        if (epsilon > EPSILON_LAXA) {
            return String.format(COPIA_TOLERANCIA_LAXA, formatearNumero(epsilon));
        }
        return null;
    }

    /**
     * Línea de eco de la tolerancia usada (copia 6), obligatoria en todo
     * resultado iterativo para que el éxito siga siendo honesto: sin eco,
     * ε no estaría realmente expuesto.
     *
     * @param epsilon tolerancia efectivamente usada
     * @return {@code "ε utilizado: X"} con X en el formato de {@link #formatearNumero(double)}
     */
    public static String ecoTolerancia(double epsilon) {
        return String.format(COPIA_ECO_TOLERANCIA, formatearNumero(epsilon));
    }

    /**
     * Formato canónico de números para los mensajes del contrato:
     * {@code BigDecimal.stripTrailingZeros} — locale-independent — con
     * guards para los valores que el dominio rechaza.
     *
     * <p>Pines verificados por test: {@code 1e-7 → "1E-7"},
     * {@code 1e-12 → "1E-12"}, {@code 1e-4 → "0.0001"},
     * {@code 2.5 → "2.5"}, {@code 10001 → "10001"}.</p>
     *
     * @param valor número a formatear
     * @return {@code "NaN"} / {@code "Infinito"} / {@code "-Infinito"} /
     *         {@code "0"} para los guards; el resto, el número sin ceros
     *         sobrantes
     */
    public static String formatearNumero(double valor) {
        if (Double.isNaN(valor)) {
            return "NaN";
        }
        if (Double.isInfinite(valor)) {
            return valor > 0 ? "Infinito" : "-Infinito";
        }
        if (valor == 0.0) {
            return "0";
        }
        return BigDecimal.valueOf(valor).stripTrailingZeros().toString();
    }

    /**
     * Intenta interpretar el texto como número (se tolera {@code 1e-7},
     * espacios y mayúsculas de {@code Double}); {@code null} si no es
     * numérico o está vacío.
     */
    private static Double parsear(String texto) {
        try {
            return Double.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
