package unsa.sistemas.model.direct;

import unsa.sistemas.controller.SistemaController;

public class Crout extends SistemaController {
 	
	  public Crout(double[][] A,double[] b,int n) {
	        super(A,b,n);
	    }
            /* 
	  public double Crout_L1U[] {
  	    double[][] G=Construir_G();
  	    for(int k=1;k<=n;k++) {
  	    	for(int j=k;j<=n+1;j++) {
  	    		G[k][j]=G[k][j]-sumatoria(G[k][p]*G[p][j]);
  	    	}
  	    	for(int i=k+1;i<=n;i++) {
  	    		G[i][k]=(G[i][k]-sumatoria(G[i][p]*G[p][k])/G[k][k])
  	    	}

  	    }
	      return Sust_Regresiva(G,n);
	  }
	  public double Crout_LU1[] {
		  double[][] G=Construir_G();
	  	    for(int k=1;k<=n;k++) {
	  	    	for(int j=k;j<=n;j++) {
	  	    		G[i][k]=G[i][k]-sumatoria(G[i][p]*G[p][k]);
	  	    	}
	  	    	for(int i=k+1;i<=n+1;i++) {
	  	    		G[i][k]=(G[k][j]-sumatoria(G[k][p]*G[p][j])/G[k][k])
	  	    	}
	  	    }
	  	    for(i=1;i<=n;i++)
	  	    	G[i][i]=1
	  	      return Sust_Regresiva(G,n);
	  }
}
 */
	public double[] Crout_L1U() {

    double[][] L = new double[n][n];
    double[][] U = new double[n][n];
    for (int i =0;i<n;i++) {
        L[i][i]=1;
    }
    for (int k =0;k<n;k++) {
        for (int j =k;j <n;j++) {
            double suma =0;
            for (int p =0;p<k;p++) {
                suma +=L[k][p]*U[p][j];
            }
            U[k][j] =A[k][j]-suma;
        }
        if (Math.abs(U[k][k])<1e-12) {
            return null;
        }
        for (int i =k+1;i<n;i++) {
            double suma =0;
            for (int p =0;p<k;p++) {
                suma +=L[i][p]*U[p][k];
            }
            L[i][k] =(A[i][k]-suma)/U[k][k];
        }
    }
    double[] y = new double[n];
    for (int i =0;i<n;i++) {
        double suma = 0;
        for (int j =0;j<i;j++) {
            suma += L[i][j]*y[j];
        }
        y[i] =b[i]-suma;
    }
    double[] x = new double[n];
    for (int i =n-1;i>=0;i--) {
        double suma =0;
        for (int j =i+1;j<n;j++) {
            suma +=U[i][j]*x[j];
        }
        x[i] =(y[i]-suma)/U[i][i];
    }
    return x;
}
	public double[] Crout_LU1() {
    double[][] L =new double[n][n];
    double[][] U =new double[n][n];
    for (int i =0;i<n;i++) {
        U[i][i]=1;
    }
    for (int k =0;k<n;k++) {
        for (int i =k;i<n;i++) {
            double suma =0;
            for (int p =0;p<k;p++) {
                suma+=L[i][p]*U[p][k];
            }
            L[i][k] =A[i][k]-suma;
        }
        if (Math.abs(L[k][k])<1e-12) {
            return null;
        }
        for (int j = k+1;j<n;j++) {
            double suma=0;
            for (int p =0; p<k;p++) {
                suma +=L[k][p]*U[p][j];
            }
            U[k][j] =(A[k][j]-suma)/L[k][k];
        }
    }
    double[] y =new double[n];
    for (int i =0;i<n;i++) {
        double suma=0;
        for (int j =0;j<i;j++) {
            suma +=L[i][j]*y[j];
        }
        y[i] =(b[i]-suma)/L[i][i];
    }
    double[] x = new double[n];
    for (int i =n-1;i>=0;i--) {
        double suma=0;
        for (int j =i+1;j<n;j++) {
            suma +=U[i][j]*x[j];
        }
        x[i]=y[i]-suma;
    }
    return x;
}
}