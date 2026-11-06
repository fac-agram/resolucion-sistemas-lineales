package unsa.sistemas.model.direct;
import unsa.sistemas.controller.SistemaController;
public class GaussJordan extends SistemaController {

    public GaussJordan(double[][] A,double[] b,int n) {
        super(A,b,n);
    }
    public double[]Jordan(){

    	    double[][] G=Construir_G();
    	    double[] x = new double[n];
    for(int k=0;k<n;k++) {
    	  // Introducido por grok
        if (!pivoteo_simple(G, k)) {
            return null;
        }
        double pivote=G[k][k];

    	for(int j=0;j<n+1;j++) {
    		G[k][j]=G[k][j]/pivote;
    	}
    	for(int i=0;i<n;i++) {
    		if(i!=k) {
                double m = G[i][k];
    			for(int j=0;j<n+1;j++)
    				G[i][j]=G[i][j]-m*G[k][j];
    		}
    	}
    }
    for(int i=0;i<n;i++)
     x[i]=G[i][n];
    
    return x;
    }
}