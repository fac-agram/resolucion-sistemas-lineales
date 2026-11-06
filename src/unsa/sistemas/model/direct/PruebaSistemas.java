package unsa.sistemas.model.direct;

import java.util.Arrays;
import unsa.sistemas.controller.SistemaController;

public class PruebaSistemas {

    public static void main(String[] args) {

        // Sistema de ecuaciones
        double[][] A = {
            { 4,  1, 1 },
            {1, 3,  0 },
            {1,  0,  2 }
        };

        double[] b = { 9, 7, 7 };

        int n = 3;

        // 1. Probar determinante
        System.out.println("===== DETERMINANTE =====");

        double det = SistemaController.determinante(A);

        System.out.printf("Determinante = %.2f%n",det);


        // 2. Probar método de Gauss
        System.out.println("\n===== GAUSS =====");

        EliminacionGaussiana gauss = new EliminacionGaussiana(A, b, n);

        double[] xGauss = gauss.Factorizar_x_Gauss();

        System.out.println("Solucion Gauss: " + Arrays.toString(xGauss));


        // 3. Probar método de Gauss-Jordan
        System.out.println("\n===== GAUSS-JORDAN =====");

        GaussJordan jordan = new GaussJordan(A, b, n);

        double[] xJordan = jordan.Jordan();

        System.out.println("Solucion Jordan: " + Arrays.toString(xJordan));
        
        // PROBAR CROUT

        Crout crout = new Crout(A, b, n);

        System.out.println("===== CROUT L1U =====");

        double[] x1 = crout.Crout_L1U();

        System.out.println(Arrays.toString(x1));


        System.out.println("\n===== CROUT LU1 =====");

        double[] x2 = crout.Crout_LU1();

        System.out.println(Arrays.toString(x2));

    }
}