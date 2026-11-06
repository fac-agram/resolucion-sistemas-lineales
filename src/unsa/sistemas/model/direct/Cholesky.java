package unsa.sistemas.model.direct;

import unsa.sistemas.controller.SistemaController;

public class Cholesky extends SistemaController {

    public Cholesky(double[][] A, double[] b, int n) {
        super(A, b, n);
    }
/*CONDICION DE CHOLESKY LA MATRIZ A DEBE SER DEFINIDA POSITIVA */
    public static boolean esDefinidaPositiva(double[][] A, int n) {
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (Math.abs(A[i][j] - A[j][i]) > 1e-10) {
                    return false;
                }
            }
        }
        for (int k =1;k<=n;k++) {
            double[][] M=new double[k+1][k+1];
            for (int i =1;i<=k;i++) {
                for (int j=1;j<=k;j++) {
                    M[i][j]=A[i][j];
                }
            }
            if (determinante(M)<=0) {
                return false;
            }
        }
        return true;
    }

    public double[] Factorizar_Cholesky() {

        // Verifico si A es definida positiva
        if (!esDefinidaPositiva(A, n)) {
            return null;
        }

        double[][] L =new double[n+1][n+1];
        for (int k =1; k<=n;k++) {
            double suma=0;

            for (int p =1;p<k;p++) {
                suma +=L[k][p]*L[k][p];
            }
            L[k][k]=Math.sqrt(A[k][k]-suma);
            for (int i =k+1;i<=n; i++) {
                suma=0;

                for (int p=1;p<k;p++) {
                    suma +=L[i][p]*L[k][p];
                }
                L[i][k] =(A[i][k]-suma)/L[k][k];
            }
        }
        double[][] LT=new double[n+1][n+1];
        for (int i =1;i<=n;i++) {
            for (int j =1;j<=n;j++) {
                LT[i][j]=L[j][i];
            }
        }
        double[] y=new double[n+1];
        for (int i =1;i<=n;i++) {
            double suma =0;
            for (int j =1;j<i;j++) {
                suma +=L[i][j]*y[j];
            }
            y[i] =(b[i]-suma)/L[i][i];
        }
        double[] x =new double[n+1];
        for (int i =n;i>=1;i--) {
            double suma=0;
            for (int j =i+1;j<=n;j++) {
                suma +=LT[i][j]*x[j];
            }
            x[i] =(y[i]-suma)/LT[i][i];
        }
        return x;
    }
}
