package unsa.sistemas.model.direct;

import unsa.sistemas.model.Resolutor;
import unsa.sistemas.model.trace.Traza;

public class Crout extends Resolutor implements ResolutorDirecto {

    public Crout(double[][] A, double[] b) {
        super(A, b);
    }

    public Crout(double[][] A, double[] b, Traza traza) {
        super(A, b, traza);
    }

    /**
     * TODO (equipo): implementar la factorización LU de Crout
     * (L unitaria inferior, U superior) con sustituciones.
     *
     * <p>Las precondiciones del método (menores principales no nulos /
     * aptitud para LU sin pivoteo) deben validarse DENTRO de este
     * método según el contrato de {@link ResolutorDirecto}: el
     * controlador ya no las valida antes de invocar al resolutor.</p>
     *
     * @return {@code null} mientras el algoritmo no esté implementado
     */
    @Override
    public double[] resolver() {
        return null; // TODO: implementar
    }
}
