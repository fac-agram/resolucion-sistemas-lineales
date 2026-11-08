package unsa.sistemas.view;

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
    private TextArea areaFormaMatricial;

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

        StringBuilder texto = new StringBuilder();

        texto.append("Matriz A:\n");

        for (double[] fila : matrizA) {

            texto.append("[ ");

            for (double valor : fila) {
                texto.append(formatear(valor)).append("  ");
            }

            texto.append("]\n");
        }

        texto.append("\nVector b:\n");

        for (double valor : vectorB) {

            texto.append("[ ")
                 .append(formatear(valor))
                 .append(" ]\n");
        }

        texto.append("\nSistema: A · x = b");

        areaFormaMatricial.setText(texto.toString());
    }

    // ==========================================
    // FORMATEAR NÚMEROS
    // ==========================================

    private String formatear(double valor) {

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

    @FXML
    private void resolverSistema() {

        if (matrizA == null || vectorB == null) {

            labelEstado.setText(
                "Primero tenés que cargar un archivo TXT."
            );

            return;
        }

        if (comboTipoMetodo.getValue() == null
                || comboMetodo.getValue() == null) {

            labelEstado.setText(
                "Seleccioná el tipo y el método de resolución."
            );

            return;
        }

        // La conexión con SistemaController se implementará
        // en la siguiente etapa.

        labelEstado.setText(
            "Sistema cargado. Próximamente se conectará el resolutor."
        );
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
        areaFormaMatricial.clear();
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
