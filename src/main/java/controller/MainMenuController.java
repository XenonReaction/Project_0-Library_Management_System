package controller;

import util.InputUtil;

public class MainMenuController {

//    private final BookController bookController;
//    private final MemberController memberController;
//    private final LoanController loanController;

    public MainMenuController() {
//        this.bookController = new BookController();
//        this.memberController = new MemberController();
//        this.loanController = new LoanController();
    }

    public void start() {
        System.out.println("=== LIBRARY MANAGEMENT SYSTEM ===");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = InputUtil.readInt("Make a choice: ");

            switch (choice) {
//                case 1 -> bookController.handleInput();
//                case 2 -> memberController.handleInput();
//                case 3 -> loanController.handleInput();
                case 0 -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== MAIN MENU ===");
        System.out.println("1. Book Services");
        System.out.println("2. Member Services");
        System.out.println("3. Loan Services");
        System.out.println("0. Exit");
    }
}
