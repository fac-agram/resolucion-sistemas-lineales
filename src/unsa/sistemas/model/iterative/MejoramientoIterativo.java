package unsa.sistemas.model.iterative;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Traza;

public class MejoramientoIterativo extends Resolutor {

    public MejoramientoIterativo(double[][] A, double[] b, int n) {
        super(A, b, n);
    }

    public MejoramientoIterativo(double[][] A, double[] b, int n, Traza traza) {
        super(A, b, n, traza);
    }

    /**
     * TODO (equipo): implementar el método de mejoramiento iterativo.
     *
     * <p>Las precondiciones del método (matriz no singular, en su caso)
     * deben validarse DENTRO de este método: el controlador ya no las
     * valida antes de invocar al resolutor.</p>
     *
     * @return {@code null} mientras el algoritmo no esté implementado
     */
    @Override
    public double[] resolver() {
        return null; // TODO: implementar
    }
}
