package test2;

import unsa.sistemas.model.direct.EliminacionGaussiana;
import unsa.sistemas.model.trace.Traza;

public class EliminacionGaussianaTest {

    private static boolean ok = true;

    public static void main(String[] args) {

        verificaSolucion();
        verificaTraza();
        verificaSistemaSingular();

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

        double[] x = new EliminacionGaussiana(A, b, 3).resolver();

        compara("x1", x[0], 1.0);
        compara("x2", x[1], 2.0);
        compara("x3", x[2], 3.0);
    }

    // Comprueba que se registren los pasos
    private static void verificaTraza() {

        Traza traza = new Traza();

        EliminacionGaussiana gauss = new EliminacionGaussiana(
            new double[][] {
                {4, 2, 2},
                {2, 5, 1},
                {2, 1, 3}
            },
            new double[] {14, 15, 13},
            3,
            traza
        );

        gauss.resolver();

        chequea("traza no vacia", !traza.estaVacia());
        chequea("cantidad de pasos > 0", traza.totalPasos() > 0);
    }

    // Una matriz singular no debe permitir resolver el sistema
    private static void verificaSistemaSingular() {

        try {

            double[][] A = {
                {1, 2},
                {2, 4}
            };

            double[] b = {3, 6};

            new EliminacionGaussiana(A, b, 2).resolver();

            chequea("matriz singular detectada", false);

        } catch (ArithmeticException e) {

            chequea("matriz singular detectada", true);

        }
    }

    // Comparación numérica
    private static void compara(String nombre, double obtenido, double esperado) {

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

        System.out.println((condicion ? "OK   " : "FALLO") + " " + nombre);

        if (!condicion) {
            ok = false;
        }
    }
}
