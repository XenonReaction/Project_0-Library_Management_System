package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.InputUtil;

/**
 * Entry-point controller for the console-based Library Management System.
 *
 * <p>This controller is responsible for:
 * <ul>
 *   <li>Displaying the main application menu</li>
 *   <li>Routing user navigation to the appropriate sub-controllers</li>
 *   <li>Acting as the top-level safety net to prevent application crashes</li>
 * </ul>
 *
 * <p>All domain-specific behavior is delegated to:
 * <ul>
 *   <li>{@link BookController}</li>
 *   <li>{@link MemberController}</li>
 *   <li>{@link LoanController}</li>
 * </ul>
 *
 * <p>This class contains no business logic and no persistence logic.
 * Its sole responsibility is application flow control.
 */
public class MainMenuController {

    private static final Logger log = LoggerFactory.getLogger(MainMenuController.class);

    private final BookController bookController;
    private final MemberController memberController;
    private final LoanController loanController;

    /**
     * Constructs the {@code MainMenuController} and initializes all sub-controllers.
     *
     * <p>Each sub-controller internally initializes its own service dependencies.
     * This constructor is used in the standard application runtime flow.
     */
    public MainMenuController() {
        this.bookController = new BookController();
        this.memberController = new MemberController();
        this.loanController = new LoanController();

        log.debug("MainMenuController initialized (controllers constructed).");
    }

    /**
     * Starts the main application loop.
     *
     * <p>This method:
     * <ol>
     *   <li>Displays the application banner</li>
     *   <li>Continuously prompts the user for a menu choice</li>
     *   <li>Delegates control to the selected sub-controller</li>
     *   <li>Terminates cleanly when the user selects Exit</li>
     * </ol>
     *
     * <p>A controller-level {@code try/catch} ensures that unexpected exceptions
     * do not crash the application back to the operating system.
     */
    public void start() {
        log.info("Application started: Library Management System");
        System.out.println("=== LIBRARY MANAGEMENT SYSTEM ===");

        boolean running = true;
        while (running) {
            try {
                printMenu();
                int choice = InputUtil.readInt("Make a choice: ");
                log.debug("Main menu selection received: {}", choice);

                switch (choice) {
                    case 1 -> {
                        log.info("Navigating to Book Services");
                        bookController.handleInput();
                    }
                    case 2 -> {
                        log.info("Navigating to Member Services");
                        memberController.handleInput();
                    }
                    case 3 -> {
                        log.info("Navigating to Loan Services");
                        loanController.handleInput();
                    }
                    case 0 -> {
                        log.info("User selected Exit. Shutting down.");
                        System.out.println("Goodbye!");
                        running = false;
                    }
                    default -> {
                        log.warn("Invalid main menu option selected: {}", choice);
                        System.out.println("Invalid option. Please try again.");
                    }
                }

            } catch (Exception e) {
                /*
                 * Controller-level “catch-all” so the application does not terminate
                 * unexpectedly due to malformed input or runtime failures.
                 *
                 * User-facing output is intentionally minimal; detailed diagnostics
                 * are recorded in logs.
                 */
                log.error("Unhandled exception in main menu loop", e);
                System.out.println("Something went wrong. Please try again.");
            }
        }

        log.info("Application stopped: Library Management System");
    }

    /**
     * Prints the main menu options to the console.
     */
    private void printMenu() {
        log.debug("Printing main menu");
        System.out.println();
        System.out.println("=== MAIN MENU ===");
        System.out.println("1. Book Services");
        System.out.println("2. Member Services");
        System.out.println("3. Loan Services");
        System.out.println("0. Exit");
    }
}
