package unsa.sistemas.controller;

public class SistemaController {

    protected double[][] A;
    protected double[] b;
    protected int n;

    private static final double EPS = 1e-12;

    /*  Constructor IA throw new IllegalArgumentException
Se utiliza cuando un método recibe argumentos o datos incorrectos."
    throw new ArithmeticException
Se utiliza cuando ocurre un problema durante una operación matemática.*/
    public SistemaController(double[][] A, double[] b, int n) {

        if (A == null || b == null) {
            throw new IllegalArgumentException("La matriz y el vector no pueden ser nulos.");
        }

        if (n <= 0 || A.length != n || b.length != n) {
            throw new IllegalArgumentException("Dimensiones incorrectas.");
        }

        for (int i = 0; i < n; i++) {
            if (A[i] == null || A[i].length != n) {
                throw new IllegalArgumentException("La matriz debe ser cuadrada.");
            }
        }

        this.n = n;
        this.A = new double[n][n];
        for (int i=0;i<n;i++) {
            for (int j =0;j<n;j++) {
                this.A[i][j]=A[i][j];
            }
        }
        this.b = b.clone();
    }
    public double[][] Construir_G() {
        double[][] G =new double[n][n+1];
        for (int i =0;i<n;i++) {
            for (int j =0;j<n;j++) {
                G[i][j] =A[i][j];
            }
            G[i][n] =b[i];
        }
        return G;
    }
    public boolean pivoteo_simple(double[][] G, int k) {
        if (G == null || G.length != n || k < 0 || k >= n) {
            throw new IllegalArgumentException("Matriz o índice incorrecto.");/*IA */
        }
        if (Math.abs(G[k][k]) > EPS) {
            return true;
        }
        for (int i =k+1;i<n;i++) {
            if (Math.abs(G[i][k]) > EPS) {
                double[] auxiliar =G[k];
                G[k]=G[i];
                G[i]=auxiliar;
                return true;
            }
        }
        return false;
    }
    public double[] factorizar_x_gauss() {
        double[][] G = Construir_G();
        for (int k =0;k<n;k++) {
            if (!pivoteo_simple(G, k)) {
                throw new ArithmeticException("El sistema no tiene solución única.");
            }
            for (int i =k+1;i<n;i++) {
                double m =G[i][k]/G[k][k];
                G[i][k]=0;
                for (int j=k+1;j<=n;j++) {
                    G[i][j] =G[i][j]-m*G[k][j];
                }
            }
        }
        return sust_Regresiva(G, n);
    }
    public double[] sust_Regresiva(double[][] G, int n) {

        if (G == null || n != this.n || G.length != n) {
            throw new IllegalArgumentException("Dimensiones incorrectas.");
        }
        double[] x =new double[n];
        for (int i =n-1;i>=0;i--) {
            double suma = 0;
            for (int j =i+1;j<n;j++) {
                suma =suma+G[i][j]*x[j];
            }
            if (Math.abs(G[i][i]) <= EPS) {
                throw new ArithmeticException("No se puede realizar la sustitución regresiva.");
            }
            x[i] =(G[i][n]-suma)/G[i][i];
        }
        return x;
    }
public static double determinante(double[][] M) {
    int n =M.length;
    double[][] C =new double[n][n];
    for (int i = 0; i < n; i++) {
        if (M[i] == null || M[i].length != n) {
            throw new IllegalArgumentException("La matriz debe ser cuadrada.");
        }
        for (int j =0;j<n;j++) {
            C[i][j] = M[i][j];
        }
    }
    double det = 1;
    int signo = 1;
    for (int k =0;k<n;k++) {
        int filaMayor =k;
        for (int i = k+1;i<n;i++) {
            if (Math.abs(C[i][k])>Math.abs(C[filaMayor][k])) {
                filaMayor = i;
            }
        }
        if (Math.abs(C[filaMayor][k]) < 1e-12) {
            return 0;
        }
        if (filaMayor!=k) {
            double[] aux =C[k];
            C[k] =C[filaMayor];
            C[filaMayor] =aux;

            signo =-signo;
        }
        double pivote =C[k][k];
        det = det*pivote;
        for (int i =k+1;i<n;i++) {
            double m =C[i][k]/pivote;
            for (int j=k+1;j<n;j++) {
                C[i][j] =C[i][j]-m*C[k][j];
            }
            C[i][k] = 0;
        }
    }
    return signo * det;
}
}