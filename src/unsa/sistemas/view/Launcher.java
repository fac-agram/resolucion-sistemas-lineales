package unsa.sistemas.view;

/**
 * Punto de entrada empaquetado de la aplicación.
 * No extiende {@link javafx.application.Application}: reenvía {@code main}
 * a {@link MainApp#main(String[])}, lo que permite lanzar la GUI desde el
 * classpath (jar con {@code Main-Class}, app-image jpackage), donde el
 * lanzador del JDK rechaza a {@code MainApp} si JavaFX no está en los
 * módulos de arranque.
 */
public class Launcher {

    /**
     * Reenvía el arranque a {@link MainApp#main(String[])}.
     *
     * @param args argumentos de línea de comandos, reenviados sin uso adicional
     */
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
