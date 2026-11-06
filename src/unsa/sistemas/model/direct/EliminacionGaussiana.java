package unsa.sistemas.model.direct;

import unsa.sistemas.controller.SistemaController;

public class EliminacionGaussiana extends SistemaController{

    public EliminacionGaussiana (double[][] A,double[] b,int n) {
        super(A,b,n);
    }
    public double[] Factorizar_x_Gauss() {
    	    double[][] G=Construir_G();
    	
        for (int k=0;k<n;k++) {
        	  if (!pivoteo_simple(G, k)) {
                  return null;
              }
              double pivote=G[k][k];
            for (int i =k+1;i<n;i++) {
                double m =G[i][k]/pivote;
                G[i][k] = 0;
                for (int j = k+1;j<=n;j++) {
                    G[i][j]=G[i][j]-m*G[k][j];
                }
            }
        }
        return sust_Regresiva(G,n);

    }
    }
   