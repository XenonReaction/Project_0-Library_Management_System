package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.InputUtil;

public class MainMenuController {

    private static final Logger log = LoggerFactory.getLogger(MainMenuController.class);

    private final BookController bookController;
    private final MemberController memberController;
    private final LoanController loanController;

    public MainMenuController() {
        this.bookController = new BookController();
        this.memberController = new MemberController();
        this.loanController = new LoanController();

        log.debug("MainMenuController initialized (controllers constructed).");
    }

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
                // Controller-level “catch-all” so the app doesn't crash back to the OS.
                // Keep user-facing output simple; keep details in logs.
                log.error("Unhandled exception in main menu loop", e);
                System.out.println("Something went wrong. Please try again.");
            }
        }

        log.info("Application stopped: Library Management System");
    }

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
