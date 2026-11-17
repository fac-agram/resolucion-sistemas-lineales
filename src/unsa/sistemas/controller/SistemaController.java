package unsa.sistemas.controller;

import java.util.Map;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.direct.Cholesky;
import unsa.sistemas.model.direct.Crout;
import unsa.sistemas.model.direct.EliminacionGaussiana;
import unsa.sistemas.model.direct.GaussJordan;
import unsa.sistemas.model.iterative.GaussSeidel;
import unsa.sistemas.model.iterative.Jacobi;
import unsa.sistemas.model.iterative.MejoramientoIterativo;
import unsa.sistemas.model.iterative.RelajacionSimple;
import unsa.sistemas.model.iterative.ResolutorIterativo;
import unsa.sistemas.model.iterative.SOR;
import unsa.sistemas.model.matrix.Matriz;
import unsa.sistemas.model.trace.Traza;
import unsa.sistemas.utils.ValidadorMatriz;

/**
 * Controlador central del sistema.
 * La vista solo necesita llamar a resolver() pasando la matriz, el vector b
 * y el nombre del método elegido por el usuario.
 *
 * <p><b>Contrato (registro de fábricas tipadas):</b> el controlador solo
 * despacha —construye el resolutor indicado en el registro, le pasa la
 * traza y reporta la convergencia—. Las precondiciones de cada algoritmo
 * (singularidad, simetría, definición positiva, ceros en la diagonal, ...)
 * las valida el propio resolutor dentro de {@code resolver()}, que lanza
 * {@link IllegalArgumentException} con mensaje en español; el controlador
 * lo convierte en {@link Resultado#error(String)}. Un resolutor jamás
 * debe devolver {@code null} desde {@code resolver()}: un {@code null}
 * indica que el método aún no está implementado, y el controlador lo
 * convierte en {@link Resultado#error(String)} antes de reportar cualquier
 * éxito.</p>
 *
 * <p>{@link ValidadorMatriz} sigue usándose para la validación estructural
 * básica (dimensiones) y para la advertencia de dominancia diagonal en
 * Jacobi y Gauss-Seidel: la dominancia es condición <i>suficiente</i>
 * —no necesaria— de convergencia, así que la falta de ella se comunica
 * como advertencia junto con la solución obtenida.</p>
 */
public class SistemaController {

    // Identificadores de métodos disponibles (la vista los usa para el menú)
    public static final String GAUSS            = "GAUSS";
    public static final String GAUSS_JORDAN     = "GAUSS_JORDAN";
    public static final String CROUT            = "CROUT";
    public static final String CHOLESKY         = "CHOLESKY";
    public static final String JACOBI           = "JACOBI";
    public static final String GAUSS_SEIDEL     = "GAUSS_SEIDEL";
    public static final String SOR_METODO       = "SOR";
    public static final String RELAJACION       = "RELAJACION";
    public static final String MEJORAMIENTO     = "MEJORAMIENTO";

    /**
     * Fábrica de resolutores: construye la clase concreta con los datos
     * del sistema, el orden y la traza compartida.
     */
    @FunctionalInterface
    private interface FactoriaResolutor {
        Resolutor crear(double[][] A, double[] b, int n, Traza traza);
    }

    /** Registro identificador de método → fábrica del resolutor. */
    private static final Map<String, FactoriaResolutor> registro = Map.of(
            GAUSS,        (A, b, n, t) -> new EliminacionGaussiana(A, b, n, t),
            GAUSS_JORDAN, (A, b, n, t) -> new GaussJordan(A, b, n, t),
            CROUT,        (A, b, n, t) -> new Crout(A, b, n, t),
            CHOLESKY,     (A, b, n, t) -> new Cholesky(A, b, n, t),
            JACOBI,       (A, b, n, t) -> new Jacobi(A, b, n, t),
            GAUSS_SEIDEL, (A, b, n, t) -> new GaussSeidel(A, b, n, t),
            SOR_METODO,   (A, b, n, t) -> new SOR(A, b, n, t),
            RELAJACION,   (A, b, n, t) -> new RelajacionSimple(A, b, n, t),
            MEJORAMIENTO, (A, b, n, t) -> new MejoramientoIterativo(A, b, n, t)
    );

    /**
     * Punto de entrada único para la vista.
     * Recibe la matriz A, el vector b y el método elegido por el usuario.
     * Valida dimensiones, despacha al resolutor registrado, pasa la traza
     * y reporta la convergencia (si es iterativo).
     *
     * @param datosA  matriz de coeficientes n x n
     * @param datosB  vector de términos independientes
     * @param metodo  identificador del método (usar las constantes de esta clase)
     * @return resultado con la solución o descripción del error
     */
    public Resultado resolver(double[][] datosA, double[] datosB, String metodo) {
        // 0. Guardia explícita: un lookup sobre null no tendría sentido
        if (metodo == null) {
            return Resultado.error("Método no reconocido: null");
        }

        try {
            // 1. Validación estructural básica (aplica a todos los métodos)
            Matriz A = new Matriz(datosA);

            if (!ValidadorMatriz.esDimensionValida(A, datosB)) {
                return Resultado.error("Dimensiones inválidas: la matriz debe ser cuadrada y el vector b debe tener el mismo número de filas.");
            }

            // 2. Despacho por registro de fábricas
            FactoriaResolutor fabrica = registro.get(metodo);
            if (fabrica == null) {
                return Resultado.error("Método no reconocido: " + metodo);
            }

            // 3. Construcción, ejecución y reporte
            Traza traza = new Traza();
            Resolutor r = fabrica.crear(datosA, datosB, datosA.length, traza);
            double[] solucion = r.resolver();

            // Guardia de contrato: un resolutor jamás devuelve null; un null
            // aquí significa que el método aún no está implementado (stub),
            // por lo que se reporta como error y no como un éxito falso.
            if (solucion == null) {
                return Resultado.error("Método aún no implementado: " + metodo + ". El resolutor devolvió null.");
            }

            Convergencia convergencia = (r instanceof ResolutorIterativo iter)
                    ? new Convergencia(iter.getIteraciones(), iter.getErrorFinal(), iter.convergio())
                    : null;
            String nota = convergencia != null
                    && (JACOBI.equals(metodo) || GAUSS_SEIDEL.equals(metodo))
                    && !ValidadorMatriz.esDiagonalDominante(A)
                    ? Resultado.NOTA_SIN_DOMINANCIA : null;
            return Resultado.clasificar(solucion, metodo, traza, convergencia, nota);
        } catch (Exception e) {
            String detalle = e.getMessage() != null ? e.getMessage() : e.toString();
            return Resultado.error("Error durante la ejecución de " + metodo + ": " + detalle);
        }
    }

    /**
     * Estados posibles de la resolución de un sistema lineal.
     */
    public enum Estado {
        OK,
        NO_CONVERGIO,
        NO_FINITO,
        ERROR
    }

    /**
     * Porta los textos asociados a la representación de un estado en la interfaz.
     */
    public record TextoEstado(String titulo, String barraEstado, String claseCss) {}

    /**
     * Mapea un estado a sus textos correspondientes para la interfaz de usuario.
     */
    public static TextoEstado textoDe(Estado estado) {
        return switch (estado) {
            case OK -> new TextoEstado(
                    "SOLUCIÓN DEL SISTEMA",
                    "Sistema resuelto correctamente.",
                    "estado-ok");
            case NO_CONVERGIO -> new TextoEstado(
                    "ÚLTIMA APROXIMACIÓN (NO CONVERGIÓ)",
                    "El método no convergió. Se muestra la última aproximación.",
                    "estado-advertencia");
            case NO_FINITO -> new TextoEstado(
                    "NO SE PUDO RESOLVER EL SISTEMA",
                    "Error: el resultado no es finito.",
                    "estado-error");
            case ERROR -> new TextoEstado(
                    "NO SE PUDO RESOLVER EL SISTEMA",
                    "Error durante la resolución.",
                    "estado-error");
        };
    }

    /**
     * Porta la convergencia reportada por un resolutor iterativo
     * después de {@code resolver()}. {@code null} en los métodos
     * directos y en los resultados con error.
     */
    public record Convergencia(int iteraciones, double errorFinal, boolean convergio) {}

    // ─────────────────────────────────────────────
    //  CLASE DE RESULTADO
    // ─────────────────────────────────────────────

    /**
     * Encapsula la respuesta que el controlador devuelve a la vista.
     * La vista no necesita saber qué ocurrió internamente: solo consulta
     * isExitoso(), isAdvertencia(), getSolucion(), getMensaje(),
     * getMetodoUsado(), getTraza() y getConvergencia().
     *
     * <p>En el caso de éxito con advertencia ({@link #exitoConAdvertencia}),
     * {@link #getMensaje()} devuelve el texto de la advertencia —no el
     * mensaje genérico de éxito— y {@link #getSolucion()} contiene la
     * solución obtenida.</p>
     *
     * <p>{@link #getTraza()} entrega la traza registrada por el resolutor
     * (nunca {@code null} en éxitos, posiblemente vacía) y
     * {@link #getConvergencia()} los datos de convergencia cuando el
     * método era iterativo ({@code null} para los directos).</p>
     */
    public static class Resultado {

        public static final String MENSAJE_EXITO = "Resuelto con éxito.";
        public static final String NOTA_SIN_DOMINANCIA = "Nota: la matriz no es estrictamente diagonal dominante.";
        public static final String MENSAJE_NO_FINITO = "El método produjo valores no finitos (Infinity o NaN). No hay solución que mostrar.";

        private final Estado estado;
        private final double[] solucion;
        private final String mensaje;
        private final String metodoUsado;
        private final Traza traza;
        private final Convergencia convergencia;

        private Resultado(Estado estado, double[] solucion, String mensaje,
                          String metodoUsado, Traza traza, Convergencia convergencia) {
            this.estado       = estado;
            this.solucion     = solucion;
            this.mensaje      = mensaje;
            this.metodoUsado  = metodoUsado;
            this.traza        = traza;
            this.convergencia = convergencia;
        }

        private static Resultado clasificar(double[] solucion, String metodo, Traza traza,
                                            Convergencia convergencia, String nota) {
            if (contieneNoFinitos(solucion)) {
                return new Resultado(Estado.NO_FINITO, null, MENSAJE_NO_FINITO, metodo, traza, convergencia);
            }
            if (convergencia != null && !convergencia.convergio()) {
                String mensaje = "El método no convergió en el máximo de iteraciones ("
                        + convergencia.iteraciones()
                        + "). Se muestra la última aproximación; no es una solución del sistema.";
                return new Resultado(Estado.NO_CONVERGIO, solucion, mensaje, metodo, traza, convergencia);
            }
            String mensaje = nota == null ? MENSAJE_EXITO : MENSAJE_EXITO + " " + nota;
            return new Resultado(Estado.OK, solucion, mensaje, metodo, traza, convergencia);
        }

        private static boolean contieneNoFinitos(double[] solucion) {
            for (double v : solucion) {
                if (!Double.isFinite(v)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Fallo: no se obtuvo solución. {@link #getSolucion()} es {@code null}.
         */
        public static Resultado error(String mensaje) {
            return new Resultado(Estado.ERROR, null, mensaje, null, null, null);
        }

        public Estado getEstado()       { return estado; }
        public boolean isExitoso()      { return estado == Estado.OK; }
        public boolean isAdvertencia()  { return estado == Estado.NO_CONVERGIO; }
        public double[] getSolucion()   { return solucion; }
        public String getMensaje()      { return mensaje; }
        public String getMetodoUsado()  { return metodoUsado; }

        /**
         * @return la traza registrada por el resolutor, o {@code null} si
         *         no se ejecutó ninguno (error o fábrica sin trazado)
         */
        public Traza getTraza() { return traza; }

        /**
         * @return los datos de convergencia si el método era iterativo,
         *         {@code null} para métodos directos o resultados con error
         */
        public Convergencia getConvergencia() { return convergencia; }
    }
}
