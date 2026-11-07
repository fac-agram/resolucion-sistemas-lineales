package unsa.sistemas.model.direct;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Traza;

public class GaussJordan extends Resolutor implements ResolutorDirecto {

    public GaussJordan(double[][] A, double[] b) {
        super(A, b);
    }

    public GaussJordan(double[][] A, double[] b, Traza traza) {
        super(A, b, traza);
    }

    /**
     * TODO (equipo): implementar la eliminación de Gauss-Jordan con
     * matriz aumentada completa.
     *
     * <p>Las precondiciones del método (matriz no singular, en su caso)
     * deben validarse DENTRO de este método según el contrato de
     * {@link ResolutorDirecto}: el controlador ya no las valida antes de
     * invocar al resolutor.</p>
     *
     * @return {@code null} mientras el algoritmo no esté implementado
     */
    @Override
    public double[] resolver() {
        return null; // TODO: implementar
    }
}
