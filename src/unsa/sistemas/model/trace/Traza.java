package unsa.sistemas.model.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Registro ordenado de los pasos ejecutados por un resolutor.
 *
 * <p>Cada resolutor va agregando {@link Paso} mientras corre su algoritmo.
 * Si el resolutor no recibió una traza (campo {@code traza == null}), no
 * registra nada y el costo del trazado es cero.</p>
 *
 * <p>La vista consume los pasos después de {@code resolver()} para mostrar
 * el paso a paso (operaciones, coeficientes, evolución de la matriz o del
 * vector solución).</p>
 */
public class Traza {

    private final List<Paso> pasos = new ArrayList<>();

    /**
     * Agrega un paso al final de la traza.
     *
     * @param paso paso a registrar; no puede ser {@code null}
     * @throws NullPointerException si {@code paso} es {@code null}
     */
    public void agregar(Paso paso) {
        pasos.add(Objects.requireNonNull(paso, "El paso no puede ser null"));
    }

    /**
     * @return los pasos en orden de ejecución, en una lista inmutable
     */
    public List<Paso> getPasos() {
        return Collections.unmodifiableList(pasos);
    }

    /**
     * @return cantidad de pasos registrados hasta el momento
     */
    public int totalPasos() {
        return pasos.size();
    }

    /**
     * @return {@code true} si aún no se registró ningún paso
     */
    public boolean estaVacia() {
        return pasos.isEmpty();
    }
}
