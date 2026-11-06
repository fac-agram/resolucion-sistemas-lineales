package unsa.sistemas.model.iterative;

public class SOR {
    protected double[][] A;
    protected double[] b;
    protected int n;

    public SOR(double[][] A, double[] b, int n) {
        this.n = n;
        this.A = new double[n][n];
        for (int i = 0; i < n; i++) this.A[i] = A[i].clone();
        this.b = b.clone();
    }

    public double[] resolver() {
        return null; // TODO: implementar
    }
}
