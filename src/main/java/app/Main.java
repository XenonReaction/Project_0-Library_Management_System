package app;

import controller.MainMenuController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary application entry point for the Library Management System.
 *
 * <p>This class is responsible only for bootstrapping the application and
 * handing control to the {@link MainMenuController}. All user interaction,
 * business logic, and persistence concerns are delegated to lower layers.</p>
 *
 * <p><strong>Responsibilities:</strong>
 * <ul>
 *   <li>Mark the start and end of an application session</li>
 *   <li>Initialize the main menu controller</li>
 *   <li>Provide a clean and minimal entry point</li>
 * </ul>
 * </p>
 *
 * <p><strong>Logging:</strong> Session banners are logged at {@code INFO}
 * level to clearly delineate application runs in log files.</p>
 */
public class Main {

    /**
     * Logger used to record application session boundaries.
     */
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point.
     *
     * <p>Starts a new application session and delegates execution to
     * {@link MainMenuController#start()}.</p>
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {

        // Visual separator between application runs (sessions)
        log.info("============================================================");
        log.info("               NEW APPLICATION SESSION STARTED              ");
        log.info("============================================================");

        new MainMenuController().start();

        // Visual separator between application runs (sessions)
        log.info("============================================================");
        log.info("                 APPLICATION SESSION ENDED                  ");
        log.info("============================================================\n");
    }
}
