package controller;

import service.LoanService;
import service.models.Loan;
import util.InputUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class LoanController {

    private final LoanService loanService;

    public LoanController() {
        this.loanService = new LoanService();
    }

    // Optional: for unit tests (inject a mocked service)
    public LoanController(LoanService loanService) {
        if (loanService == null) throw new IllegalArgumentException("loanService cannot be null.");
        this.loanService = loanService;
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

        try {
            List<Loan> loans = loanService.getAll();
            if (loans.isEmpty()) {
                System.out.println("No loans found.");
                return;
            }

            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            System.out.println("Error retrieving loans: " + ex.getMessage());
        }
    }

    private void checkoutBook() {
        System.out.println();
        System.out.println("=== CHECKOUT BOOK ===");

        try {
            long bookId = InputUtil.readInt("Book ID: ");
            long memberId = InputUtil.readInt("Member ID: ");

            System.out.println("Loan length rules:");
            System.out.println("- Enter 0 to use the default (14 days).");
            System.out.println("- Enter a positive number of days.");

            int daysInput = InputUtil.readInt("Loan length in days: ");
            int days = (daysInput <= 0) ? 14 : daysInput;

            LocalDate checkoutDate = LocalDate.now();
            LocalDate dueDate = checkoutDate.plusDays(days);

            // Service-layer-friendly model: create a Loan model and let the service persist it
            Loan loan = new Loan();
            loan.setBookId(bookId);
            loan.setMemberId(memberId);
            loan.setCheckoutDate(checkoutDate);
            loan.setDueDate(dueDate);
            loan.setReturnDate(null);

            Long id = loanService.checkout(loan);

            System.out.println("Checkout successful. Created loan id=" + id);
            System.out.println("Due date: " + dueDate);

        } catch (IllegalArgumentException ex) {
            System.out.println("Could not checkout book: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.println("Error checking out book: " + ex.getMessage());
        }
    }

    private void returnBook() {
        System.out.println();
        System.out.println("=== RETURN BOOK ===");

        try {
            long loanId = InputUtil.readInt("Loan ID to return: ");
            LocalDate returnDate = LocalDate.now();

            boolean returned = loanService.returnLoan(loanId, returnDate);
            if (returned) {
                System.out.println("Return successful for loan id=" + loanId + " on " + returnDate);
            } else {
                System.out.println("No active loan found with id=" + loanId + " (nothing returned).");
            }

        } catch (IllegalArgumentException ex) {
            System.out.println("Could not return loan: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.println("Error returning book: " + ex.getMessage());
        }
    }

    private void findLoanById() {
        System.out.println();
        System.out.println("=== FIND LOAN ===");

        long id = InputUtil.readInt("Loan ID: ");

        try {
            Optional<Loan> maybeLoan = loanService.getById(id);
            if (maybeLoan.isEmpty()) {
                System.out.println("No loan found with id=" + id);
                return;
            }

            System.out.println(maybeLoan.get());

        } catch (RuntimeException ex) {
            System.out.println("Error finding loan: " + ex.getMessage());
        }
    }

    private void deleteLoan() {
        System.out.println();
        System.out.println("=== DELETE LOAN ===");

        long id = InputUtil.readInt("Loan ID to delete: ");

        try {
            boolean deleted = loanService.delete(id);
            if (deleted) {
                System.out.println("Deleted loan id=" + id);
            } else {
                System.out.println("No loan found with id=" + id + " (nothing deleted).");
            }
        } catch (RuntimeException ex) {
            System.out.println("Error deleting loan: " + ex.getMessage());
        }
    }

    private void listActiveLoans() {
        System.out.println();
        System.out.println("=== ACTIVE LOANS ===");

        try {
            List<Loan> loans = loanService.getActiveLoans();
            if (loans.isEmpty()) {
                System.out.println("No active loans found.");
                return;
            }

            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            System.out.println("Error retrieving active loans: " + ex.getMessage());
        }
    }

    private void listLoansByMember() {
        System.out.println();
        System.out.println("=== LOANS BY MEMBER ===");

        long memberId = InputUtil.readInt("Member ID: ");

        try {
            List<Loan> loans = loanService.getLoansByMemberId(memberId);
            if (loans.isEmpty()) {
                System.out.println("No loans found for memberId=" + memberId);
                return;
            }

            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            System.out.println("Error retrieving loans for member: " + ex.getMessage());
        }
    }

    private void listOverdueLoans() {
        System.out.println();
        System.out.println("=== OVERDUE LOANS ===");

        try {
            LocalDate today = LocalDate.now();
            List<Loan> loans = loanService.getOverdueLoans(today);

            if (loans.isEmpty()) {
                System.out.println("No overdue loans as of " + today);
                return;
            }

            System.out.println("Overdue as of " + today + ":");
            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            System.out.println("Error retrieving overdue loans: " + ex.getMessage());
        }
    }
}
