package unsa.sistemas.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Cargar la interfaz diseñada en Scene Builder
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("sistema.fxml")
        );

        Parent raiz = loader.load();

        // Crear la escena
        Scene escena = new Scene(raiz);

        // Configurar la ventana
        primaryStage.setTitle("Resolución de Sistemas Lineales");
        primaryStage.setScene(escena);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}