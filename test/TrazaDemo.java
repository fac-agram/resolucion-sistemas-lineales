import unsa.sistemas.model.iterative.Jacobi;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

// Caso de prueba: genera y muestra la traza paso a paso de Jacobi.
//
// Sistema (el mismo del harness del proyecto):
//   4x + y + z = 9
//   x + 3y     = 7
//   x     + 2z = 7
// Solución esperada: [1, 2, 3] (la matriz es diagonalmente dominante).
//
// Cómo compilar y ejecutar desde la raíz del proyecto:
//   shopt -s globstar
//   javac -d out src/**/*.java
//   javac -cp out -d out test/*.java
//   java -cp out TrazaDemo
public class TrazaDemo {
    public static void main(String[] args) {
        double[][] A = {{4, 1, 1}, {1, 3, 0}, {1, 0, 2}};
        double[] b = {9, 7, 7};

        Traza traza = new Traza();
        Jacobi jacobi = new Jacobi(A, b, 3, traza);
        double[] x = jacobi.resolver();

        System.out.println("=== TRAZA DE JACOBI ===");
        for (Paso p : traza.getPasos()) {
            System.out.printf("Paso %2d | %-28s | x = [%.6f, %.6f, %.6f] | error = %s%n",
                    p.indice(),
                    p.operacion(),
                    p.x()[0], p.x()[1], p.x()[2],
                    Double.isNaN(p.error()) ? "  n/a" : String.format("%.3e", p.error()));
        }

        System.out.println();
        System.out.printf("Solucion: [%.6f, %.6f, %.6f] (esperado [1, 2, 3])%n", x[0], x[1], x[2]);
        System.out.printf("Iteraciones=%d  Error final=%.3e  Convergio=%b  Pasos=%d%n",
                jacobi.getIteraciones(), jacobi.getErrorFinal(), jacobi.convergio(), traza.totalPasos());
    }
}
