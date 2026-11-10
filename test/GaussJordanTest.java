import unsa.sistemas.model.direct.GaussJordan;
import unsa.sistemas.model.trace.Traza;

public class GaussJordanTest {

    private static boolean ok = true;

    public static void main(String[] args) {

        verificaSolucion();
        verificaTraza();
        verificaSistemaSingular();
        verificaPivoteo();
        System.out.println(ok ? "TEST_OK" : "TEST_FALLO");
        System.exit(ok ? 0 : 1);
    }

    // Sistema con solución exacta [1, 2, 3]
    private static void verificaSolucion() {

        double[][] A = {
            {4, 2, 2},
            {2, 5, 1},
            {2, 1, 3}
        };

        double[] b = {14, 15, 13};

        GaussJordan jordan = new GaussJordan(A, b, 3);

        double[] x = jordan.resolver();

        compara("x1", x[0], 1.0);
        compara("x2", x[1], 2.0);
        compara("x3", x[2], 3.0);
    }

    // Comprueba que se registren los pasos del algoritmo
    private static void verificaTraza() {

        Traza traza = new Traza();

        GaussJordan jordan = new GaussJordan(
            new double[][] {
                {4, 2, 2},
                {2, 5, 1},
                {2, 1, 3}
            },
            new double[] {14, 15, 13},
            3,
            traza
        );

        jordan.resolver();

        chequea("traza no vacia", !traza.estaVacia());
        chequea("cantidad de pasos > 0", traza.totalPasos() > 0);
    }

    // Comprueba que detecte una matriz singular
    private static void verificaSistemaSingular() {

        try {

            double[][] A = {
                {1, 2},
                {2, 4}
            };

            double[] b = {3, 6};

            new GaussJordan(A, b, 2).resolver();

            chequea("matriz singular detectada", false);

        } catch (ArithmeticException e) {

            chequea("matriz singular detectada", true);

        }
    }

    // Comparación numérica
    private static void compara(
        String nombre,
        double obtenido,
        double esperado
    ) {

        if (Math.abs(obtenido - esperado) < 1e-6) {

            System.out.printf("%s = %.6f OK%n", nombre, obtenido);

        } else {

            System.out.printf(
                "%s = %.6f FALLO (esperado %.6f)%n",
                nombre, obtenido, esperado
            );

            ok = false;
        }
    }

    // Verificación lógica
    private static void chequea(String nombre, boolean condicion) {

        System.out.println(
            (condicion ? "OK   " : "FALLO") + " " + nombre
        );

        if (!condicion) {
            ok = false;
        }
    }
    private static void verificaPivoteo() {

    double[][] A = {
        {0, 2},
        {1, 1}
    };

    double[] b = {4, 3};

    GaussJordan jordan = new GaussJordan(A, b, 2);

    double[] x = jordan.resolver();

    compara("pivoteo x1", x[0], 1.0);
    compara("pivoteo x2", x[1], 2.0);
}


}