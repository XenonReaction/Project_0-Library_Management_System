package controller;

import util.InputUtil;

import java.time.LocalDate;

// TODO (later): import service.LoanService;
// TODO (later): import service.Loan;

public class LoanController {

    // TODO (later): private final LoanService loanService;

    public LoanController() {
        // TODO (later): this.loanService = new LoanService();
    }

    public void handleInput() {
        boolean running = true;

        while (running) {
            printMenu();
            int choice = InputUtil.readInt("Make a choice: ");

            switch (choice) {
                case 1 -> listAllLoans();
                case 2 -> checkoutBook();
                case 3 -> returnBook();
                case 4 -> findLoanById();
                case 5 -> deleteLoan();
                case 6 -> listActiveLoans();
                case 7 -> listLoansByMember();
                case 8 -> listOverdueLoans();
                case 0 -> running = false;
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== LOAN SERVICES ===");
        System.out.println("1. List all loans");
        System.out.println("2. Checkout book");
        System.out.println("3. Return book");
        System.out.println("4. Find loan by ID");
        System.out.println("5. Delete loan");
        System.out.println("6. List active loans");
        System.out.println("7. List loans by member");
        System.out.println("8. List overdue loans");
        System.out.println("0. Back");
    }

    private void listAllLoans() {
        System.out.println();
        System.out.println("=== ALL LOANS ===");

        // TODO (later):
        // loanService.findAll().forEach(System.out::println);

        System.out.println("(TODO) Service layer not implemented yet.");
    }

    private void checkoutBook() {
        System.out.println();
        System.out.println("=== CHECKOUT BOOK ===");

        long bookId = InputUtil.readInt("Book ID: ");
        long memberId = InputUtil.readInt("Member ID: ");

        String daysStr = InputUtil.readString("Loan length in days (default 14): ");
        int days = 14;
        if (daysStr != null && !daysStr.isBlank()) {
            try {
                days = Integer.parseInt(daysStr.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Using 14 days.");
            }
        }

        LocalDate checkoutDate = LocalDate.now();
        LocalDate dueDate = checkoutDate.plusDays(days);

        // TODO (later):
        // Loan loan = new Loan(bookId, memberId, checkoutDate, dueDate, null);
        // Loan created = loanService.checkout(loan);
        // System.out.println("Created: " + created);

        System.out.println("(TODO) Would checkout bookId=" + bookId + " to memberId=" + memberId +
                " due " + dueDate);
    }

    private void returnBook() {
        System.out.println();
        System.out.println("=== RETURN BOOK ===");

        int loanId = InputUtil.readInt("Loan ID to return: ");
        LocalDate returnDate = LocalDate.now();

        // TODO (later):
        // loanService.returnLoan(loanId, returnDate);

        System.out.println("(TODO) Would return loanId=" + loanId + " on " + returnDate);
    }

    private void findLoanById() {
        System.out.println();
        System.out.println("=== FIND LOAN ===");

        int id = InputUtil.readInt("Loan ID: ");

        // TODO (later):
        // loanService.findById(id)
        //     .ifPresentOrElse(
        //         System.out::println,
        //         () -> System.out.println("No loan found with id=" + id)
        //     );

        System.out.println("(TODO) Would look up loan with id=" + id);
    }

    private void deleteLoan() {
        System.out.println();
        System.out.println("=== DELETE LOAN ===");

        int id = InputUtil.readInt("Loan ID to delete: ");

        // TODO (later):
        // loanService.deleteById(id);

        System.out.println("(TODO) Would delete loan id=" + id);
    }

    private void listActiveLoans() {
        System.out.println();
        System.out.println("=== ACTIVE LOANS ===");

        // TODO (later):
        // loanService.findActiveLoans().forEach(System.out::println);

        System.out.println("(TODO) Would list active loans.");
    }

    private void listLoansByMember() {
        System.out.println();
        System.out.println("=== LOANS BY MEMBER ===");

        int memberId = InputUtil.readInt("Member ID: ");

        // TODO (later):
        // loanService.findLoansByMemberId(memberId).forEach(System.out::println);

        System.out.println("(TODO) Would list loans for memberId=" + memberId);
    }

    private void listOverdueLoans() {
        System.out.println();
        System.out.println("=== OVERDUE LOANS ===");

        LocalDate today = LocalDate.now();

        // TODO (later):
        // loanService.findOverdueLoans(today).forEach(System.out::println);

        System.out.println("(TODO) Would list overdue loans as of " + today);
    }
}
