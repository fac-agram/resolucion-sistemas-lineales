package unsa.sistemas.model.iterative;

import unsa.sistemas.utils.ValidadorParametros;

/**
 * Parámetros ajustables de los métodos iterativos: factor de relajación
 * ω, tolerancia ε y máximo de iteraciones.
 *
 * <p>Record inmutable: una vez construido, ningún resolutor puede
 * cambiar sus valores y dos instancias con los mismos componentes son
 * intercambiables. La igualdad es la generada por el record (basada en
 * los componentes) — no se redefine {@code equals}/{@code hashCode},
 * y los tests del proyecto dependen de ese comportamiento de valor.</p>
 *
 * <p><b>Autovalidación:</b> el constructor compacto delega
 * íntegramente a {@link ValidadorParametros#validarDominio(double, double, int)},
 * de modo que es imposible tener en mano un {@code Parametros} fuera de
 * dominio: la defensa del modelo (R5.5) y los mensajes de la interfaz
 * comparten una única fuente, sin deriva posible. El dominio es
 * {@code 0 &lt; ω &lt; 2}, {@code 1e-12 ≤ ε ≤ 1e-3} y
 * {@code 1 ≤ N ≤ 10000}; NaN y ±Infinity se rechazan con el mensaje
 * del propio parámetro.</p>
 *
 * <p><b>Sin {@code toString} propio a propósito:</b> la copia visible de
 * la tolerancia no vive en el registro sino en el echo
 * {@code "ε utilizado: X"} del controlador, que es la única fuente de
 * ese texto en la interfaz.</p>
 *
 * @param omega          factor de relajación (solo lo consume SOR; el
 *                       resto de los iterativos lo ignora)
 * @param epsilon        tolerancia del criterio de paro
 * @param maxIteraciones cota máxima de iteraciones
 * @throws IllegalArgumentException si algún componente viola el dominio,
 *         con la copia española exacta de ese parámetro como mensaje
 */
public record Parametros(double omega, double epsilon, int maxIteraciones) {

    /**
     * ω por defecto (1.5): valor del extinto {@code SOR.OMEGA_DEFECTO},
     * punto de partida razonable para sistemas diagonalmente dominantes.
     */
    public static final double OMEGA_POR_DEFECTO = 1.5;

    /**
     * ε por defecto (1e-7): valor de la extinta constante {@code TOLERANCIA}
     * que compartían los cinco métodos iterativos.
     */
    public static final double EPSILON_POR_DEFECTO = 1e-7;

    /**
     * N por defecto (1000): valor de la extinta constante
     * {@code MAX_ITERACIONES} que compartían los cinco métodos iterativos.
     */
    public static final int MAX_ITERACIONES_POR_DEFECTO = 1000;

    /**
     * Constructor compacto: valida el dominio completo delegando en
     * {@link ValidadorParametros#validarDominio(double, double, int)}
     * y no agrega ninguna otra lógica.
     */
    public Parametros {
        ValidadorParametros.validarDominio(omega, epsilon, maxIteraciones);
    }

    /**
     * Parámetros canónicos por defecto: {@code 1.5 / 1e-7 / 1000},
     * exactamente las constantes que usaban los resolutores antes de que
     * los parámetros fueran ajustables.
     *
     * @return un {@code Parametros} con los valores por defecto
     */
    public static Parametros defecto() {
        return new Parametros(OMEGA_POR_DEFECTO, EPSILON_POR_DEFECTO, MAX_ITERACIONES_POR_DEFECTO);
    }
}
