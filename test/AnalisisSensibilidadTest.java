import unsa.sistemas.model.matrix.AnalisisSensibilidad;
import unsa.sistemas.model.matrix.Matriz;

// Caso de prueba con verificación: número de condición contra valores
// calculados a mano (las normas viven ahora en Normas — ver NormasTest).
//
// Matrices usadas:
//   A  = [[4,1],[1,3]]              -> ‖A‖∞ = 5,  A⁻¹ = (1/11)[[3,-1],[-1,4]]
//                                       ‖A⁻¹‖∞ = 5/11, cond = 25/11 ≈ 2.272727
//   I  = identidad 2x2              -> cond(I) = 1
//   Casi singular [[1,1],[1,1+1e-8]] -> cond > 1e8 (mal condicionada)
//
// Sale con código 0 si todo pasa (TEST_OK), 1 si falla (TEST_FALLO).
//
// Cómo compilar y ejecutar desde la raíz del proyecto:
//   shopt -s globstar
//   javac -d out src/**/*.java
//   javac -cp out -d out test/*.java
//   java -cp out AnalisisSensibilidadTest
public class AnalisisSensibilidadTest {

    private static boolean ok = true;

    public static void main(String[] args) {
        verificaCondicionIdentidad();
        verificaCondicionMatrizConocida();
        verificaMatrizCasiSingular();
        verificaValidaciones();

        System.out.println(ok ? "TEST_OK" : "TEST_FALLO");
        System.exit(ok ? 0 : 1);
    }

    /** Identidad: ‖I‖=1, ‖I⁻¹‖=1, cond = 1 (caso mínimo). */
    private static void verificaCondicionIdentidad() {
        Matriz i = new Matriz(new double[][]{{1, 0}, {0, 1}});
        double cond = AnalisisSensibilidad.calcularCondicion(i, i);
        compara("cond(I)", cond, 1.0);
    }

    /** A = [[4,1],[1,3]] con su inversa exacta: cond = 25/11 ≈ 2.272727. */
    private static void verificaCondicionMatrizConocida() {
        Matriz a = new Matriz(new double[][]{{4, 1}, {1, 3}});
        Matriz invA = new Matriz(new double[][]{
                {3.0 / 11, -1.0 / 11},
                {-1.0 / 11, 4.0 / 11}});

        double cond = AnalisisSensibilidad.calcularCondicion(a, invA);
        compara("cond([[4,1],[1,3]])", cond, 25.0 / 11.0);
        chequea("cond >= 1", cond >= 1.0);
    }

    /** [[1,1],[1,1+1e-8]]: det = 1e-8, casi singular -> cond enorme. */
    private static void verificaMatrizCasiSingular() {
        double e = 1e-8;
        Matriz a = new Matriz(new double[][]{{1, 1}, {1, 1 + e}});
        // Inversa exacta: (1/e)·[[1+e, -1], [-1, 1]]
        Matriz invA = new Matriz(new double[][]{
                {(1 + e) / e, -1 / e},
                {-1 / e, 1 / e}});

        double cond = AnalisisSensibilidad.calcularCondicion(a, invA);
        System.out.printf("cond(casi singular) = %.3e%n", cond);
        chequea("cond > 1e8 (mal condicionada)", cond > 1e8);
    }

    /** Null, no cuadradas, dimensiones distintas e inversa inconsistente. */
    private static void verificaValidaciones() {
        esperarIae("null", () -> AnalisisSensibilidad.calcularCondicion(
                null, new Matriz(new double[][]{{1, 0}, {0, 1}})));
        esperarIae("no cuadrada", () -> AnalisisSensibilidad.calcularCondicion(
                new Matriz(new double[][]{{1, 2, 3}, {4, 5, 6}}), new Matriz(new double[][]{{1, 0}, {0, 1}})));
        esperarIae("dimensiones distintas", () -> AnalisisSensibilidad.calcularCondicion(
                new Matriz(new double[][]{{4, 1}, {1, 3}}), new Matriz(new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}})));
        // Pasar A en lugar de A⁻¹: cond = ‖A‖² da < 1 si A escala chico (0.5·I)
        esperarIae("inversa inconsistente", () -> AnalisisSensibilidad.calcularCondicion(
                new Matriz(new double[][]{{0.5, 0}, {0, 0.5}}), new Matriz(new double[][]{{0.5, 0}, {0, 0.5}})));
    }

    // ── mini-framework de aserciones (sin dependencias) ──

    private static void compara(String nombre, double obtenido, double esperado) {
        if (Math.abs(obtenido - esperado) < 1e-9) {
            System.out.printf("%s = %.6f (esperado %.6f) OK%n", nombre, obtenido, esperado);
        } else {
            System.out.printf("%s = %.6f FALLO (esperado %.6f)%n", nombre, obtenido, esperado);
            ok = false;
        }
    }

    private static void chequea(String nombre, boolean condicion) {
        System.out.println((condicion ? "OK   " : "FALLO") + " " + nombre);
        if (!condicion) {
            ok = false;
        }
    }

    private static void esperarIae(String nombre, Runnable accion) {
        try {
            accion.run();
            System.out.println("FALLO " + nombre + " (no lanzo excepcion)");
            ok = false;
        } catch (IllegalArgumentException e) {
            System.out.println("Precondicion OK [" + nombre + "]: " + e.getMessage());
        }
    }
}
