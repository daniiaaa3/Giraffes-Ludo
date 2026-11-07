package View;

/**
 * Entry point of the runnable JAR.
 *
 * JavaFX refuses to start from a class that extends Application
 * when the JavaFX libraries are packed inside the same JAR. This
 * small launcher does not extend Application, so
 * "java -jar giraffes-ludo-0.0.1-SNAPSHOT.jar" works on a computer
 * that only has Java installed.
 *
 * MVC Responsibility: Application startup
 * Iteration: 1
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts the game.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        LudoApplication.main(args);
    }
}
