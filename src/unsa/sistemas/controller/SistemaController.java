package unsa.sistemas.controller;

import unsa.sistemas.model.direct.Cholesky;
import unsa.sistemas.model.direct.Crout;
import unsa.sistemas.model.direct.EliminacionGaussiana;
import unsa.sistemas.model.direct.GaussJordan;
import unsa.sistemas.model.iterative.GaussSeidel;
import unsa.sistemas.model.iterative.Jacobi;
import unsa.sistemas.model.iterative.MejoramientoIterativo;
import unsa.sistemas.model.iterative.RelajacionSimple;
import unsa.sistemas.model.iterative.SOR;
import unsa.sistemas.model.matrix.Matriz;
import unsa.sistemas.utils.ValidadorMatriz;

/**
 * Controlador central del sistema.
 * La vista solo necesita llamar a resolver() pasando la matriz, el vector b
 * y el nombre del método elegido por el usuario. El controlador se encarga
 * de validar y ejecutar el algoritmo correspondiente.
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
     * Punto de entrada único para la vista.
     * Recibe la matriz A, el vector b y el método elegido por el usuario.
     * Valida, ejecuta y retorna el resultado.
     *
     * @param datosA  matriz de coeficientes n x n
     * @param datosB  vector de términos independientes
     * @param metodo  identificador del método (usar las constantes de esta clase)
     * @return resultado con la solución o descripción del error
     */
    public Resultado resolver(double[][] datosA, double[] datosB, String metodo) {
        // 0. Guardia explícita: un switch sobre null lanzaría NullPointerException
        if (metodo == null) {
            return Resultado.error("Método no reconocido: null");
        }

        try {
            // 1. Construcción y validación estructural básica (aplica a todos los métodos)
            Matriz A = new Matriz(datosA);

            if (!ValidadorMatriz.esDimensionValida(A, datosB)) {
                return Resultado.error("Dimensiones inválidas: la matriz debe ser cuadrada y el vector b debe tener el mismo número de filas.");
            }

            // 2. Despacho al método elegido
            return switch (metodo) {
                case GAUSS        -> ejecutarGauss(datosA, datosB, A);
                case GAUSS_JORDAN -> ejecutarGaussJordan(datosA, datosB, A);
                case CROUT        -> ejecutarCrout(datosA, datosB, A);
                case CHOLESKY     -> ejecutarCholesky(datosA, datosB, A);
                case JACOBI       -> ejecutarJacobi(datosA, datosB, A);
                case GAUSS_SEIDEL -> ejecutarGaussSeidel(datosA, datosB, A);
                case SOR_METODO   -> ejecutarSOR(datosA, datosB, A);
                case RELAJACION   -> ejecutarRelajacion(datosA, datosB, A);
                case MEJORAMIENTO -> ejecutarMejoramiento(datosA, datosB, A);
                default           -> Resultado.error("Método no reconocido: " + metodo);
            };
        } catch (Exception e) {
            String detalle = e.getMessage() != null ? e.getMessage() : e.toString();
            return Resultado.error("Error durante la ejecución de " + metodo + ": " + detalle);
        }
    }

    // ─────────────────────────────────────────────
    //  MÉTODOS DIRECTOS
    // ─────────────────────────────────────────────

    private Resultado ejecutarGauss(double[][] A, double[] b, Matriz matrizA) {
        if (!ValidadorMatriz.esNoSingular(matrizA)) {
            return Resultado.error("La matriz es singular (det = 0). El sistema no tiene solución única.");
        }
        double[] solucion = new EliminacionGaussiana(A, b, A.length).resolver();
        return Resultado.exito(solucion, GAUSS);
    }

    private Resultado ejecutarGaussJordan(double[][] A, double[] b, Matriz matrizA) {
        if (!ValidadorMatriz.esNoSingular(matrizA)) {
            return Resultado.error("La matriz es singular (det = 0). El sistema no tiene solución única.");
        }
        double[] solucion = new GaussJordan(A, b, A.length).resolver();
        return Resultado.exito(solucion, GAUSS_JORDAN);
    }

    private Resultado ejecutarCrout(double[][] A, double[] b, Matriz matrizA) {
        if (!ValidadorMatriz.esAptaParaLU(matrizA)) {
            return Resultado.error("La factorización de Crout requiere que todos los menores principales sean no nulos.");
        }
        double[] solucion = new Crout(A, b, A.length).resolver();
        return Resultado.exito(solucion, CROUT);
    }

    private Resultado ejecutarCholesky(double[][] A, double[] b, Matriz matrizA) {
        if (!ValidadorMatriz.esSimetrica(matrizA)) {
            return Resultado.error("Cholesky requiere que la matriz sea simétrica (A = Aᵀ).");
        }
        if (!ValidadorMatriz.esDefinidaPositiva(matrizA)) {
            return Resultado.error("Cholesky requiere que la matriz sea definida positiva (todos los menores principales > 0).");
        }
        double[] solucion = new Cholesky(A, b, A.length).resolver();
        return Resultado.exito(solucion, CHOLESKY);
    }

    // ─────────────────────────────────────────────
    //  MÉTODOS ITERATIVOS
    // ─────────────────────────────────────────────

    private Resultado ejecutarJacobi(double[][] A, double[] b, Matriz matrizA) {
        if (ValidadorMatriz.tieneCerosEnDiagonal(matrizA)) {
            return Resultado.error("Jacobi requiere que ningún elemento de la diagonal sea cero.");
        }
        // La dominancia diagonal es condición SUFICIENTE (no necesaria) de
        // convergencia: el método se ejecuta siempre y la falta de dominancia
        // se comunica como advertencia junto con la solución obtenida.
        double[] solucion = new Jacobi(A, b, A.length).resolver();
        if (!ValidadorMatriz.esDiagonalDominante(matrizA)) {
            return Resultado.exitoConAdvertencia(solucion, JACOBI,
                    "La matriz no es estrictamente diagonal dominante. Jacobi podría no converger.");
        }
        return Resultado.exito(solucion, JACOBI);
    }

    private Resultado ejecutarGaussSeidel(double[][] A, double[] b, Matriz matrizA) {
        if (ValidadorMatriz.tieneCerosEnDiagonal(matrizA)) {
            return Resultado.error("Gauss-Seidel requiere que ningún elemento de la diagonal sea cero.");
        }
        // Misma lógica que Jacobi: la dominancia diagonal es solo suficiente.
        double[] solucion = new GaussSeidel(A, b, A.length).resolver();
        if (!ValidadorMatriz.esDiagonalDominante(matrizA)) {
            return Resultado.exitoConAdvertencia(solucion, GAUSS_SEIDEL,
                    "La matriz no es estrictamente diagonal dominante. Gauss-Seidel podría no converger.");
        }
        return Resultado.exito(solucion, GAUSS_SEIDEL);
    }

    private Resultado ejecutarSOR(double[][] A, double[] b, Matriz matrizA) {
        if (ValidadorMatriz.tieneCerosEnDiagonal(matrizA)) {
            return Resultado.error("SOR requiere que ningún elemento de la diagonal sea cero.");
        }
        double[] solucion = new SOR(A, b, A.length).resolver();
        return Resultado.exito(solucion, SOR_METODO);
    }

    private Resultado ejecutarRelajacion(double[][] A, double[] b, Matriz matrizA) {
        if (ValidadorMatriz.tieneCerosEnDiagonal(matrizA)) {
            return Resultado.error("Relajación simple requiere que ningún elemento de la diagonal sea cero.");
        }
        double[] solucion = new RelajacionSimple(A, b, A.length).resolver();
        return Resultado.exito(solucion, RELAJACION);
    }

    private Resultado ejecutarMejoramiento(double[][] A, double[] b, Matriz matrizA) {
        if (!ValidadorMatriz.esNoSingular(matrizA)) {
            return Resultado.error("El mejoramiento iterativo requiere que la matriz sea no singular.");
        }
        double[] solucion = new MejoramientoIterativo(A, b, A.length).resolver();
        return Resultado.exito(solucion, MEJORAMIENTO);
    }

    // ─────────────────────────────────────────────
    //  CLASE DE RESULTADO
    // ─────────────────────────────────────────────

    /**
     * Encapsula la respuesta que el controlador devuelve a la vista.
     * La vista no necesita saber qué ocurrió internamente: solo consulta
     * isExitoso(), isAdvertencia(), getSolucion(), getMensaje() y
     * getMetodoUsado().
     *
     * <p>En el caso de éxito con advertencia ({@link #exitoConAdvertencia}),
     * {@link #getMensaje()} devuelve el texto de la advertencia —no el
     * mensaje genérico de éxito— y {@link #getSolucion()} contiene la
     * solución obtenida.</p>
     */
    public static class Resultado {
        private final boolean exitoso;
        private final boolean esAdvertencia;
        private final double[] solucion;
        private final String mensaje;
        private final String metodoUsado;

        private Resultado(boolean exitoso, boolean esAdvertencia, double[] solucion, String mensaje, String metodoUsado) {
            this.exitoso      = exitoso;
            this.esAdvertencia = esAdvertencia;
            this.solucion     = solucion;
            this.mensaje      = mensaje;
            this.metodoUsado  = metodoUsado;
        }

        /**
         * Éxito sin observaciones: la solución se obtuvo sin advertencias.
         */
        public static Resultado exito(double[] solucion, String metodo) {
            return new Resultado(true, false, solucion, "Resuelto con éxito.", metodo);
        }

        /**
         * Éxito con advertencia: el método se ejecutó y devolvió una
         * solución, pero con una observación (p. ej. falta de dominancia
         * diagonal). La solución sí está disponible en {@link #getSolucion()}
         * y el texto de la advertencia en {@link #getMensaje()}.
         *
         * @param solucion   solución obtenida por el método
         * @param metodo     identificador del método ejecutado
         * @param advertencia texto de la advertencia para la vista
         */
        public static Resultado exitoConAdvertencia(double[] solucion, String metodo, String advertencia) {
            return new Resultado(true, true, solucion, advertencia, metodo);
        }

        /**
         * Fallo: no se obtuvo solución. {@link #getSolucion()} es {@code null}.
         */
        public static Resultado error(String mensaje) {
            return new Resultado(false, false, null, mensaje, null);
        }

        public boolean isExitoso()      { return exitoso; }
        public boolean isAdvertencia()  { return esAdvertencia; }
        public double[] getSolucion()   { return solucion; }
        public String getMensaje()      { return mensaje; }
        public String getMetodoUsado()  { return metodoUsado; }
    }
}