package unsa.sistemas.view;

import unsa.sistemas.controller.SistemaController;
import unsa.sistemas.controller.SistemaController.ParametroUi;
import unsa.sistemas.model.iterative.Parametros;
import unsa.sistemas.model.trace.Paso;
import unsa.sistemas.model.trace.Traza;
import unsa.sistemas.utils.TextosVista;
import unsa.sistemas.utils.ValidadorParametros;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;

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
    private HBox filaParametros;

    @FXML
    private VBox celdaOmega;

    @FXML
    private TextField campoOmega;

    @FXML
    private TextField campoTolerancia;

    @FXML
    private TextField campoIteraciones;

    @FXML
    private TextArea areaResultado;

    @FXML
    private TextArea areaProcedimiento;

    @FXML
    private TitledPane paneProcedimiento;

    @FXML
    private Label labelEstado;

    // Datos del sistema cargado
    private double[][] matrizA;
    private double[] vectorB;

    /**
     * Parámetros efectivamente usados en la última resolución: alimentan
     * el eco obligatorio {@code "ε utilizado: X"} del panel de resultados.
     */
    private Parametros ultimosParametros;

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

        // Primera y única escucha del método elegido: aplica el predicado
        // puro de visibilidad (D7) a la fila de parámetros. Sin este
        // listener el combo no tenía ninguna (verificado sobre 59f5777).
        comboMetodo.valueProperty().addListener((observable, anterior, nuevo) -> {

            String metodoId = (nuevo == null) ? null : obtenerMetodoSeleccionado();

            aplicarVisibilidadParametros(SistemaController.parametrosVisiblesPara(metodoId));
        });
    }

    /**
     * Aplica el conjunto visible de parámetros (predicado puro de
     * {@link SistemaController#parametrosVisiblesPara(String)}) a la fila
     * completa y a la celda de ω, que solo SOR muestra. El listener no
     * decide nada: solo traduce el conjunto a {@code visible}/{@code managed}.
     */
    private void aplicarVisibilidadParametros(EnumSet<ParametroUi> visibles) {

        boolean mostrarFila = !visibles.isEmpty();
        boolean mostrarOmega = visibles.contains(ParametroUi.OMEGA);

        filaParametros.setVisible(mostrarFila);
        filaParametros.setManaged(mostrarFila);

        celdaOmega.setVisible(mostrarOmega);
        celdaOmega.setManaged(mostrarOmega);
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
            areaProcedimiento.clear();
            paneProcedimiento.setExpanded(false);
            paneProcedimiento.setDisable(true);

            labelEstado.setText(
                "Sistema cargado correctamente: "
                + filas + " incógnitas."
            );

        } catch (IOException | IllegalArgumentException e) {

            mostrarError("No se pudo cargar el sistema:\n" + e.getMessage());
        }
    }
    @FXML
    private void cargarManual() {

        // Crear ventana emergente
        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Ingreso manual del sistema");
        dialog.setHeaderText("Ingresar un sistema de ecuaciones");

        // Área donde se ingresarán los coeficientes
        Label ejemplo = new Label(
            "Ejemplo de ingreso:\n" +
            "4 1 1 6\n" +
            "1 3 1 5\n" +
            "1 1 3 5");

        TextArea entrada = new TextArea();

        entrada.setPromptText("Escribí aquí los coeficientes...");
        entrada.setPrefRowCount(10);
        entrada.setPrefColumnCount(30);
        entrada.setWrapText(true);

        Label instrucciones = new Label("Ingresá una fila por ecuación.\n" +"Separá los valores mediante espacios.\n" +"El último valor de cada fila es el término independiente.");

        // Organizar los componentes
        VBox contenido = new VBox(10,instrucciones,ejemplo,entrada);

        dialog.getDialogPane().setContent(contenido);

        // Botones
        ButtonType botonCargar = new ButtonType("Cargar sistema",ButtonBar.ButtonData.OK_DONE);

        ButtonType botonCancelar = new ButtonType("Cancelar",ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(
            botonCargar,
            botonCancelar
        );

        // Asociar la ventana con la ventana principal
        dialog.initOwner(labelArchivo.getScene().getWindow());

        Optional<ButtonType> respuesta = dialog.showAndWait();

        // Comprobar si se presionó Cargar sistema
        if (respuesta.isEmpty() || respuesta.get() != botonCargar) {
            return;
        }

        try {

        // Interpretar y validar los datos ingresados
        List<String> lineas = entrada.getText().lines().toList();

        double[][] datos = interpretarArchivo(lineas);

        int filas = datos.length;
        int columnas = datos[0].length - 1;

        double[][] nuevaA = new double[filas][columnas];
        double[] nuevoB = new double[filas];

        // Separar matriz A y vector b
        for (int i = 0; i < filas; i++) {

            for (int j = 0; j < columnas; j++) {
                nuevaA[i][j] = datos[i][j];
            }

            nuevoB[i] = datos[i][columnas];
        }

        // Guardar el nuevo sistema
        matrizA = nuevaA;
        vectorB = nuevoB;

        labelArchivo.setText("Sistema ingresado manualmente");

        // Actualizar la interfaz principal
        mostrarSistema();
        mostrarFormaMatricial();

        areaResultado.clear();
        areaProcedimiento.clear();
        paneProcedimiento.setExpanded(false);
        paneProcedimiento.setDisable(true);

        labelEstado.setText(
            "Sistema ingresado correctamente: "
            + filas + " incógnitas."
        );

        }catch (IllegalArgumentException e) {

        Alert alerta = new Alert(Alert.AlertType.ERROR);

        alerta.setTitle("Error al ingresar el sistema");
        alerta.setHeaderText("No se pudo cargar el sistema");
        alerta.setContentText(e.getMessage());

        alerta.showAndWait();

        labelEstado.setText("Error al ingresar el sistema.");
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

        areaSistema.setText(TextosVista.textoEcuaciones(matrizA, vectorB));
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

        return TextosVista.formatear(valor);
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

    // Estado de visibilidad ANTES de leer: se aplican los campos que el
    // predicado marca como visibles para este método. Los campos ocultos
    // (p. ej. un ω con texto viejo tras cambiar de método) se leen null y
    // el validador los omite: nunca se parsean.
    EnumSet<ParametroUi> visibles = SistemaController.parametrosVisiblesPara(metodo);
    aplicarVisibilidadParametros(visibles);

    String omegaTexto = visibles.contains(ParametroUi.OMEGA) ? campoOmega.getText() : null;
    String toleranciaTexto = visibles.contains(ParametroUi.TOLERANCIA) ? campoTolerancia.getText() : null;
    String iteracionesTexto = visibles.contains(ParametroUi.MAX_ITERACIONES) ? campoIteraciones.getText() : null;

    // R5.1: Rechazo en la interfaz ANTES de cualquier intento de resolución.
    List<String> errores = ValidadorParametros.errores(omegaTexto, toleranciaTexto, iteracionesTexto);

    if (!errores.isEmpty()) {
        labelEstado.setText(errores.get(0));
        labelEstado.getStyleClass().removeAll("estado-ok", "estado-advertencia", "estado-error");
        labelEstado.getStyleClass().add("estado-error");
        return;
    }

    // Campos ocultos toman el default canónico; los visibles ya validan.
    // El record re-valida su dominio (doble defensa R5.5): imposible llegar
    // a construir un Parametros inválido desde aquí.
    Parametros parametros = new Parametros(
        omegaTexto == null ? Parametros.OMEGA_POR_DEFECTO : Double.parseDouble(omegaTexto),
        toleranciaTexto == null ? Parametros.EPSILON_POR_DEFECTO : Double.parseDouble(toleranciaTexto),
        iteracionesTexto == null ? Parametros.MAX_ITERACIONES_POR_DEFECTO : (int) Double.parseDouble(iteracionesTexto));

    ultimosParametros = parametros;

    // Enviar los datos al controlador matemático.
    // Se utilizan copias para conservar los datos originales.
    double[][] matrizCopia = copiarMatriz(matrizA);
    double[] vectorCopia = vectorB.clone();
    SistemaController.Resultado resultado = sistemaController.resolver(matrizCopia, vectorCopia, metodo, parametros);
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
        // Ocultar y limpiar el procedimiento anterior.
        areaProcedimiento.clear();
        paneProcedimiento.setExpanded(false);
        paneProcedimiento.setDisable(true);

        SistemaController.Estado estado = resultado.getEstado();
        SistemaController.TextoEstado textos = SistemaController.textoDe(estado);
        StringBuilder texto = new StringBuilder(textos.titulo()).append("\n\n");

        switch (estado) {
            case OK, NO_CONVERGIO -> agregarSolucion(texto, resultado);
            case NO_FINITO, ERROR  -> texto.append(resultado.getMensaje());
        }

        areaResultado.setText(texto.toString());

        // Mostrar el procedimiento si existe una traza.
        Traza traza = resultado.getTraza();
        if (traza != null && !traza.estaVacia()) {
            StringBuilder procedimiento = new StringBuilder();
            mostrarTraza(traza, procedimiento);
            areaProcedimiento.setText(procedimiento.toString());
            paneProcedimiento.setDisable(false);
            paneProcedimiento.setExpanded(false);
        }

        labelEstado.setText(textos.barraEstado());
        labelEstado.getStyleClass().removeAll("estado-ok", "estado-advertencia", "estado-error");
        labelEstado.getStyleClass().add(textos.claseCss());
    }

    private void agregarSolucion(StringBuilder texto, SistemaController.Resultado resultado) {
        double[] solucion = resultado.getSolucion();
        if (solucion != null) {
            for (int i = 0; i < solucion.length; i++) {
                texto.append("x")
                     .append(i + 1)
                     .append(" = ")
                     .append(formatear(solucion[i]))
                     .append("\n");
            }
        }

        texto.append("\nMétodo utilizado: ")
             .append(resultado.getMetodoUsado());

        texto.append("\n\n")
             .append(resultado.getMensaje());

        SistemaController.Convergencia convergencia = resultado.getConvergencia();
        if (convergencia != null) {
            texto.append("\n\nINFORMACIÓN DE CONVERGENCIA");
            texto.append("\nIteraciones: ")
                 .append(convergencia.iteraciones());
            texto.append("\nError final: ")
                 .append(formatear(convergencia.errorFinal()));
            // Eco obligatorio (R4): el éxito con ε ajustable solo es honesto
            // si el usuario ve con qué ε se resolvió. Dentro del guardia de
            // convergencia: nunca aparece para directos ni para ERROR.
            if (ultimosParametros != null) {
                texto.append("\n")
                     .append(SistemaController.ecoTolerancia(ultimosParametros.epsilon()));
            }
            texto.append("\nConvergió: ")
                 .append(convergencia.convergio() ? "Sí" : "No");
        }
    }
    
    private void mostrarTraza(Traza traza, StringBuilder texto) {

    if (traza == null || traza.estaVacia()) {
        return;
    }

    texto.append("PROCEDIMIENTO PASO A PASO\n");
    texto.append("================================\n");

    for (Paso paso : traza.getPasos()) {

        texto.append("\n");

        if (paso.indice() > 0) {
            texto.append("Paso ").append(paso.indice());
        } else {
            texto.append("Inicio");
        }

        texto.append("\n");
        texto.append(paso.operacion()).append("\n");

        // Mostrar matriz si existe.
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

        // Mostrar vector solución si existe.
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

        // Mostrar error si corresponde.
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
        areaProcedimiento.clear();
        paneProcedimiento.setExpanded(false);
        paneProcedimiento.setDisable(true);

        comboTipoMetodo.getSelectionModel().clearSelection();
        comboMetodo.getItems().clear();
        comboMetodo.getSelectionModel().clearSelection();
        comboMetodo.setDisable(true);

        // R7.4: los campos vuelven a los valores por defecto visibles y la
        // fila se oculta hasta que se elija un método iterativo.
        campoOmega.setText("1.5");
        campoTolerancia.setText("1e-7");
        campoIteraciones.setText("1000");
        aplicarVisibilidadParametros(EnumSet.noneOf(ParametroUi.class));

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
