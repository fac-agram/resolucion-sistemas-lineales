package unsa.sistemas.controller;

import java.util.EnumSet;
import java.util.Map;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.direct.Cholesky;
import unsa.sistemas.model.direct.Crout;
import unsa.sistemas.model.direct.EliminacionGaussiana;
import unsa.sistemas.model.direct.GaussJordan;
import unsa.sistemas.model.iterative.GaussSeidel;
import unsa.sistemas.model.iterative.Jacobi;
import unsa.sistemas.model.iterative.MejoramientoIterativo;
import unsa.sistemas.model.iterative.Parametros;
import unsa.sistemas.model.iterative.RelajacionSimple;
import unsa.sistemas.model.iterative.ResolutorIterativo;
import unsa.sistemas.model.iterative.SOR;
import unsa.sistemas.model.matrix.Matriz;
import unsa.sistemas.model.trace.Traza;
import unsa.sistemas.utils.ValidadorMatriz;
import unsa.sistemas.utils.ValidadorParametros;

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
     * del sistema, el orden, la traza compartida y los parámetros
     * ajustables. Los métodos directos ignoran {@code parametros}.
     */
    @FunctionalInterface
    private interface FactoriaResolutor {
        Resolutor crear(double[][] A, double[] b, int n, Traza traza, Parametros parametros);
    }

    /** Registro identificador de método → fábrica del resolutor. */
    private static final Map<String, FactoriaResolutor> registro = Map.of(
            GAUSS,        (A, b, n, t, p) -> new EliminacionGaussiana(A, b, n, t),
            GAUSS_JORDAN, (A, b, n, t, p) -> new GaussJordan(A, b, n, t),
            CROUT,        (A, b, n, t, p) -> new Crout(A, b, n, t),
            CHOLESKY,     (A, b, n, t, p) -> new Cholesky(A, b, n, t),
            JACOBI,       (A, b, n, t, p) -> new Jacobi(A, b, n, p, t),
            GAUSS_SEIDEL, (A, b, n, t, p) -> new GaussSeidel(A, b, n, p, t),
            SOR_METODO,   (A, b, n, t, p) -> new SOR(A, b, n, p, t),
            RELAJACION,   (A, b, n, t, p) -> new RelajacionSimple(A, b, n, p, t),
            MEJORAMIENTO, (A, b, n, t, p) -> new MejoramientoIterativo(A, b, n, p, t)
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
        return resolver(datosA, datosB, metodo, Parametros.defecto());
    }

    /**
     * Punto de entrada con parámetros ajustables para la vista.
     * Resuelve con el dominio ya validado por {@link Parametros}; un
     * {@code null} aquí es un error del llamador directo y se reporta
     * como {@link Resultado#error(String)} sin intentar resolver.
     *
     * @param datosA     matriz de coeficientes n x n
     * @param datosB     vector de términos independientes
     * @param metodo     identificador del método (usar las constantes de esta clase)
     * @param parametros ω, ε y máximo de iteraciones; los métodos directos
     *                   los ignoran
     * @return resultado con la solución o descripción del error
     */
    public Resultado resolver(double[][] datosA, double[] datosB, String metodo, Parametros parametros) {
        // 0. Guardia explícita: un lookup sobre null no tendría sentido
        if (metodo == null) {
            return Resultado.error("Método no reconocido: null");
        }
        if (parametros == null) {
            return Resultado.error("Los parámetros no pueden ser null.");
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
            Resolutor r = fabrica.crear(datosA, datosB, datosA.length, traza, parametros);
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
            String notaDominancia = convergencia != null
                    && (JACOBI.equals(metodo) || GAUSS_SEIDEL.equals(metodo))
                    && !ValidadorMatriz.esDiagonalDominante(A)
                    ? Resultado.NOTA_SIN_DOMINANCIA : null;
            String nota = unirNotas(notaDominancia, notaLaxa(parametros.epsilon()));
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
     * Identifica los campos de parámetros de la interfaz, para que la
     * visibilidad condicional sea un predicado puro headlessly testeable
     * (D7) y el listener JavaFX solo aplique el conjunto resultante.
     */
    public enum ParametroUi {
        OMEGA,
        TOLERANCIA,
        MAX_ITERACIONES
    }

    /**
     * Predicado puro de visibilidad: qué campos de parámetros muestra la
     * interfaz para el método elegido.
     *
     * <p>SOR ve los tres (es el único que consume ω); los otros cuatro
     * iterativos ven ε y N; los directos, los métodos desconocidos y
     * {@code null} no ven ninguno (la fila completa se oculta).</p>
     *
     * @param metodoId identificador del método (constantes de esta clase) o {@code null}
     * @return el conjunto de campos visibles; nunca {@code null}
     */
    public static EnumSet<ParametroUi> parametrosVisiblesPara(String metodoId) {
        if (metodoId == null) {
            return EnumSet.noneOf(ParametroUi.class);
        }
        return switch (metodoId) {
            case SOR_METODO -> EnumSet.allOf(ParametroUi.class);
            case JACOBI, GAUSS_SEIDEL, RELAJACION, MEJORAMIENTO ->
                    EnumSet.of(ParametroUi.TOLERANCIA, ParametroUi.MAX_ITERACIONES);
            default -> EnumSet.noneOf(ParametroUi.class);
        };
    }

    /**
     * Une las notas del mensaje de éxito preservando el contrato byte-pinned
     * de coexistencia (R6.3): primero la de dominancia, después la de
     * tolerancia laxa, separadas por un espacio.
     *
     * @param notaDominancia nota por falta de dominancia diagonal, o {@code null}
     * @param notaLaxa       nota por ε laxa, o {@code null}
     * @return la nota combinada, una de las dos, o {@code null} si no hay notas
     */
    public static String unirNotas(String notaDominancia, String notaLaxa) {
        if (notaDominancia == null) {
            return notaLaxa;
        }
        if (notaLaxa == null) {
            return notaDominancia;
        }
        return notaDominancia + " " + notaLaxa;
    }

    /**
     * Nota de tolerancia laxa para la ε efectivamente usada.
     *
     * @param epsilon tolerancia de la resolución
     * @return la copia byte-exact de la nota si {@code ε > 1e-5} estricto;
     *         {@code null} en caso contrario
     */
    public static String notaLaxa(double epsilon) {
        return ValidadorParametros.notaToleranciaLaxa(epsilon);
    }

    /**
     * Línea de eco de la tolerancia usada ({@code "ε utilizado: X"}),
     * obligatoria en todo resultado iterativo (honestidad del éxito con ε
     * ajustable). Vive aquí por el precedente de {@link #textoDe}: la copia
     * de la vista se mantiene headlessly testeable.
     *
     * @param epsilon tolerancia de la resolución
     * @return la copia 6 con ε en el formato canónico de
     *         {@link ValidadorParametros#formatearNumero(double)}
     */
    public static String ecoTolerancia(double epsilon) {
        return ValidadorParametros.ecoTolerancia(epsilon);
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
