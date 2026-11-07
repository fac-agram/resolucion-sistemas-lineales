package unsa.sistemas.model.direct;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Traza;

public class Cholesky extends Resolutor implements ResolutorDirecto {

    public Cholesky(double[][] A, double[] b) {
        super(A, b);
    }

    public Cholesky(double[][] A, double[] b, Traza traza) {
        super(A, b, traza);
    }

    /**
     * TODO (equipo): implementar la factorización de Cholesky
     * (A = L·Lᵀ con L triangular inferior) y las sustituciones.
     *
     * <p>Las precondiciones del método (matriz simétrica y definida
     * positiva) deben validarse DENTRO de este método según el contrato
     * de {@link ResolutorDirecto}: el controlador ya no las valida
     * antes de invocar al resolutor.</p>
     *
     * @return {@code null} mientras el algoritmo no esté implementado
     */
    @Override
    public double[] resolver() {
        return null; // TODO: implementar
    }
}
