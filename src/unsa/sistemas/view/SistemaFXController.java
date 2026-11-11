package unsa.sistemas.view;

import unsa.sistemas.controller.SistemaController;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class SistemaFXController {

    // Componentes de la interfaz
    @FXML
    private Label labelArchivo;

    @FXML
    private TextArea areaSistema;

    @FXML
    private TextArea areaMatrizA;
    
    @FXML
    private TextArea areaVectorB;

    @FXML
    private ComboBox<String> comboTipoMetodo;

    @FXML
    private ComboBox<String> comboMetodo;

    @FXML
    private TextArea areaResultado;

    @FXML
    private Label labelEstado;

    // Datos del sistema cargado
    private double[][] matrizA;
    private double[] vectorB;

    private final SistemaController sistemaController = new SistemaController();

    @FXML
    public void initialize() {

        // Configurar tipos de métodos
        comboTipoMetodo.getItems().addAll(
            "Directo",
            "Iterativo"
        );

        comboMetodo.setDisable(true);

        // Actualizar los métodos según el tipo seleccionado
        comboTipoMetodo.getSelectionModel()
            .selectedItemProperty()
            .addListener((observable, anterior, nuevo) -> {

                comboMetodo.getItems().clear();
                comboMetodo.getSelectionModel().clearSelection();

                if (nuevo == null) {
                    comboMetodo.setDisable(true);
                    return;
                }

                comboMetodo.setDisable(false);

                if (nuevo.equals("Directo")) {

                    comboMetodo.getItems().addAll(
                        "Eliminación Gaussiana",
                        "Gauss-Jordan",
                        "Crout",
                        "Cholesky"
                    );

                } else if (nuevo.equals("Iterativo")) {

                    comboMetodo.getItems().addAll(
                        "Jacobi",
                        "Gauss-Seidel",
                        "Relajación Simple",
                        "SOR",
                        "Mejoramiento Iterativo"
                    );
                }
            });
    }

    // ==========================================
    // CARGA DEL ARCHIVO TXT
    // ==========================================

    @FXML
    private void cargarArchivo() {

        FileChooser selector = new FileChooser();

        selector.setTitle("Seleccionar sistema de ecuaciones");

        selector.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Archivos de texto (*.txt)",
                "*.txt"
            )
        );

        Window ventana = labelArchivo.getScene().getWindow();

        File archivo = selector.showOpenDialog(ventana);

        if (archivo == null) {
            return;
        }

        try {

            List<String> lineas = Files.readAllLines(
                archivo.toPath(),
                StandardCharsets.UTF_8
            );

            // Analizar el contenido sin modificar todavía
            // los datos del sistema actualmente cargado.
            double[][] nuevaA;
            double[] nuevoB;

            double[][] datos = interpretarArchivo(lineas);

            int filas = datos.length;
            int columnas = datos[0].length - 1;

            nuevaA = new double[filas][columnas];
            nuevoB = new double[filas];

            for (int i = 0; i < filas; i++) {

                for (int j = 0; j < columnas; j++) {
                    nuevaA[i][j] = datos[i][j];
                }

                nuevoB[i] = datos[i][columnas];
            }

            // Guardar los datos después de validar correctamente.
            matrizA = nuevaA;
            vectorB = nuevoB;

            labelArchivo.setText(archivo.getName());

            mostrarSistema();
            mostrarFormaMatricial();

            areaResultado.clear();

            labelEstado.setText(
                "Sistema cargado correctamente: "
                + filas + " incógnitas."
            );

        } catch (IOException | IllegalArgumentException e) {

            mostrarError("No se pudo cargar el sistema:\n" + e.getMessage());
        }
    }

    // ==========================================
    // INTERPRETAR ARCHIVO TXT
    // ==========================================

    private double[][] interpretarArchivo(List<String> lineas) {

        List<double[]> filas = new ArrayList<>();

        int cantidadColumnas = -1;
        int numeroLinea = 0;

        for (String linea : lineas) {

            numeroLinea++;

            linea = linea.trim();

            // Ignorar líneas vacías
            if (linea.isEmpty()) {
                continue;
            }

            // Permitir comentarios que comiencen con #
            if (linea.startsWith("#")) {
                continue;
            }

            // Separar los números por espacios o tabulaciones
            String[] valores = linea.split("\\s+");

            if (cantidadColumnas == -1) {
                cantidadColumnas = valores.length;
            }

            if (valores.length != cantidadColumnas) {
                throw new IllegalArgumentException(
                    "La línea " + numeroLinea
                    + " tiene una cantidad diferente de valores."
                );
            }

            if (cantidadColumnas < 2) {
                throw new IllegalArgumentException(
                    "Cada fila debe contener al menos un coeficiente "
                    + "y un término independiente."
                );
            }

            double[] fila = new double[cantidadColumnas];

            for (int i = 0; i < valores.length; i++) {

                try {

                    fila[i] = Double.parseDouble(valores[i]);

                } catch (NumberFormatException e) {

                    throw new IllegalArgumentException(
                        "Valor numérico inválido en la línea "
                        + numeroLinea + ": " + valores[i]
                    );
                }

                if (!Double.isFinite(fila[i])) {
                    throw new IllegalArgumentException(
                        "Se encontró un valor no finito en la línea "
                        + numeroLinea + "."
                    );
                }
            }

            filas.add(fila);
        }

        if (filas.isEmpty()) {
            throw new IllegalArgumentException(
                "El archivo está vacío o no contiene datos."
            );
        }

        int cantidadIncognitas = cantidadColumnas - 1;

        if (filas.size() != cantidadIncognitas) {
            throw new IllegalArgumentException(
                "El sistema debe ser cuadrado.\n"
                + "Se encontraron " + filas.size() + " ecuaciones y "
                + cantidadIncognitas + " incógnitas."
            );
        }

        return filas.toArray(new double[0][]);
    }

    // ==========================================
    // MOSTRAR SISTEMA DE ECUACIONES
    // ==========================================

    private void mostrarSistema() {

        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < matrizA.length; i++) {

            for (int j = 0; j < matrizA[i].length; j++) {

                if (j > 0) {
                    texto.append(" + ");
                }

                texto.append(formatear(matrizA[i][j]))
                     .append("·x")
                     .append(matrizA[i].length - j);
            }

            texto.append(" = ")
                 .append(formatear(vectorB[i]))
                 .append("\n");
        }

        areaSistema.setText(texto.toString());
    }

    // ==========================================
    // MOSTRAR FORMA MATRICIAL
    // ==========================================

    private void mostrarFormaMatricial() {

    // Construir la representación de la matriz A
    StringBuilder textoMatriz = new StringBuilder();

    for (double[] fila : matrizA) {

        textoMatriz.append("[ ");

        for (double valor : fila) {
            textoMatriz.append(formatear(valor)).append("  ");
        }

        textoMatriz.append("]\n");
    }

    // Construir la representación del vector b
    StringBuilder textoVector = new StringBuilder();

    for (double valor : vectorB) {

        textoVector.append("[ ")
                   .append(formatear(valor))
                   .append(" ]\n");
    }

    // Mostrar cada estructura en su propio TextArea
    areaMatrizA.setText(textoMatriz.toString());
    areaVectorB.setText(textoVector.toString());
}

    // ==========================================
    // FORMATEAR NÚMEROS
    // ==========================================

    private String formatear(double valor) {

    if (Double.isNaN(valor)) {
        return "NaN";
    }

    if (Double.isInfinite(valor)) {
        return valor > 0 ? "Infinity" : "-Infinity";
    }

    if (valor == 0.0) {
        return "0";
    }

    return BigDecimal.valueOf(valor)
            .stripTrailingZeros()
            .toPlainString();
}

    // ==========================================
    // RESOLVER SISTEMA
    // ==========================================

    private String obtenerMetodoSeleccionado() {

    return switch (comboMetodo.getValue()) {

        // Métodos directos
        case "Eliminación Gaussiana" -> SistemaController.GAUSS;
        case "Gauss-Jordan" -> SistemaController.GAUSS_JORDAN;
        case "Crout" -> SistemaController.CROUT;
        case "Cholesky" -> SistemaController.CHOLESKY;

        // Métodos iterativos
        case "Jacobi" -> SistemaController.JACOBI;
        case "Gauss-Seidel" -> SistemaController.GAUSS_SEIDEL;
        case "Relajación Simple" -> SistemaController.RELAJACION;
        case "SOR" -> SistemaController.SOR_METODO;
        case "Mejoramiento Iterativo" -> SistemaController.MEJORAMIENTO;

        default -> null;
    };
}

    @FXML
    private void resolverSistema() {
        // Verificar que exista un sistema cargado
        if (matrizA == null || vectorB == null) {
            labelEstado.setText("Primero tenés que cargar un archivo TXT.");
            return;
        }

    // Verificar que se haya seleccionado un método
        if (comboTipoMetodo.getValue() == null|| comboMetodo.getValue() == null) {
        labelEstado.setText(    "Seleccioná el tipo y el método de resolución.");
        return;
    }

    // Obtener el identificador correspondiente
    String metodo = obtenerMetodoSeleccionado();

    if (metodo == null) {
        labelEstado.setText("Método de resolución no reconocido.");
        return;
    }

    // Enviar los datos al controlador matemático.
    // Se utilizan copias para conservar los datos originales.
    double[][] matrizCopia = copiarMatriz(matrizA);
    double[] vectorCopia = vectorB.clone();
    SistemaController.Resultado resultado = sistemaController.resolver(matrizCopia, vectorCopia, metodo);
    // Mostrar el resultado
    mostrarResultado(resultado);

    }
    private double[][] copiarMatriz(double[][] matriz) {

    double[][] copia = new double[matriz.length][];

    for (int i = 0; i < matriz.length; i++) {
        copia[i] = matriz[i].clone();
    }

    return copia;
}
    private void mostrarResultado(SistemaController.Resultado resultado) {

    StringBuilder texto = new StringBuilder();

    // Si ocurrió un error
    if (!resultado.isExitoso()) {

        texto.append("NO SE PUDO RESOLVER EL SISTEMA\n\n");
        texto.append(resultado.getMensaje());

        areaResultado.setText(texto.toString());

        labelEstado.setText("Error durante la resolución.");

        return;
    }

    // Mostrar la solución
    texto.append("SOLUCIÓN DEL SISTEMA\n\n");

    double[] solucion = resultado.getSolucion();

    for (int i = 0; i < solucion.length; i++) {

        texto.append("x")
             .append(i + 1)
             .append(" = ")
             .append(formatear(solucion[i]))
             .append("\n");
    }

    // Mostrar información del método
    texto.append("\nMétodo utilizado: ")
         .append(resultado.getMetodoUsado());

    texto.append("\n\n")
         .append(resultado.getMensaje());

    // Mostrar información adicional si es iterativo
    SistemaController.Convergencia convergencia =
        resultado.getConvergencia();

    if (convergencia != null) {

        texto.append("\n\nINFORMACIÓN DE CONVERGENCIA");

        texto.append("\nIteraciones: ")
             .append(convergencia.iteraciones());

        texto.append("\nError final: ")
             .append(formatear(convergencia.errorFinal()));

        texto.append("\nConvergió: ")
             .append(convergencia.convergio() ? "Sí" : "No");
    }

    // Agregar el procedimiento paso a paso
    mostrarTraza(resultado.getTraza(), texto);
    // Mostrar todo en la interfaz
    areaResultado.setText(texto.toString());
    labelEstado.setText(
        resultado.isAdvertencia()
            ? "Sistema resuelto con advertencias."
            : "Sistema resuelto correctamente."
    );
}
    
    private void mostrarTraza(Traza traza, StringBuilder texto) {

    if (traza == null || traza.estaVacia()) {
        texto.append("\n\nNo hay pasos registrados.");
        return;
    }

    texto.append("\n\n================================");
    texto.append("\nPROCEDIMIENTO PASO A PASO");
    texto.append("\n================================\n");

    for (Paso paso : traza.getPasos()) {

        texto.append("\n");

        // Número del paso
        if (paso.indice() > 0) {
            texto.append("Paso ").append(paso.indice());
        } else {
            texto.append("Inicio");
        }

        texto.append("\n");
        texto.append(paso.operacion()).append("\n");

        // Mostrar matriz si existe
        double[][] matriz = paso.matriz();

        if (matriz != null) {

            texto.append("\n");

            for (double[] fila : matriz) {

                texto.append("[ ");

                for (int j = 0; j < fila.length; j++) {

                    if (j == fila.length - 1) {
                        texto.append("| ");
                    }

                    texto.append(formatear(fila[j])).append("  ");
                }

                texto.append("]\n");
            }
        }

        // Mostrar vector solución si existe
        double[] x = paso.x();

        if (x != null) {

            texto.append("\nSolución obtenida:\n");

            for (int i = 0; i < x.length; i++) {

                texto.append("x")
                     .append(i + 1)
                     .append(" = ")
                     .append(formatear(x[i]))
                     .append("\n");
            }
        }

        // Mostrar error si corresponde
        if (!Double.isNaN(paso.error())) {

            texto.append("Error: ")
                 .append(formatear(paso.error()))
                 .append("\n");
        }

        texto.append("--------------------------------\n");
    }
}


    // ==========================================
    // LIMPIAR INTERFAZ
    // ==========================================

    @FXML
    private void limpiarCampos() {

        matrizA = null;
        vectorB = null;

        labelArchivo.setText("Ningún archivo seleccionado");

        areaSistema.clear();
        areaMatrizA.clear();
        areaVectorB.clear();
        areaResultado.clear();

        comboTipoMetodo.getSelectionModel().clearSelection();
        comboMetodo.getItems().clear();
        comboMetodo.getSelectionModel().clearSelection();
        comboMetodo.setDisable(true);

        labelEstado.setText(
            "Listo para cargar un sistema."
        );
    }

    // ==========================================
    // MOSTRAR ERRORES
    // ==========================================

    private void mostrarError(String mensaje) {

        Alert alerta = new Alert(Alert.AlertType.ERROR);

        alerta.setTitle("Error al cargar el sistema");
        alerta.setHeaderText("No se pudo procesar el archivo");
        alerta.setContentText(mensaje);

        alerta.showAndWait();

        labelEstado.setText("Error al cargar el archivo.");
    }
}
