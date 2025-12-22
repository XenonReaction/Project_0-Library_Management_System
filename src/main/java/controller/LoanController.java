package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.LoanService;
import service.models.Loan;
import util.InputUtil;
import util.validators.LoanValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class LoanController {

    private static final Logger log = LoggerFactory.getLogger(LoanController.class);

    private final LoanService loanService;

    public LoanController() {
        this.loanService = new LoanService();
        log.debug("LoanController initialized with default LoanService.");
    }

    // Optional: for unit tests (inject a mocked service)
    public LoanController(LoanService loanService) {
        if (loanService == null) {
            log.error("Attempted to initialize LoanController with null LoanService.");
            throw new IllegalArgumentException("loanService cannot be null.");
        }
        this.loanService = loanService;
        log.debug("LoanController initialized with injected LoanService.");
    }

    public void handleInput() {
        log.info("Entered Loan Services menu.");
        boolean running = true;

        while (running) {
            try {
                printMenu();
                int choice = InputUtil.readInt("Make a choice: ");
                log.debug("Loan menu selection received: {}", choice);

                switch (choice) {
                    case 1 -> listAllLoans();
                    case 2 -> checkoutBook();
                    case 3 -> returnBook();
                    case 4 -> findLoanById();
                    case 5 -> deleteLoan();
                    case 6 -> listActiveLoans();
                    case 7 -> listLoansByMember();
                    case 8 -> listOverdueLoans();
                    case 0 -> {
                        log.info("Exiting Loan Services menu.");
                        running = false;
                    }
                    default -> {
                        log.warn("Invalid Loan menu option selected: {}", choice);
                        System.out.println("Invalid option. Please try again.");
                    }
                }
            } catch (Exception ex) {
                log.error("Unhandled exception in LoanController menu loop.", ex);
                System.out.println("An unexpected error occurred. Please try again.");
            }
        }
    }

    private void printMenu() {
        log.debug("Printing Loan Services menu.");
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
        log.info("Listing all loans.");
        System.out.println();
        System.out.println("=== ALL LOANS ===");

        try {
            List<Loan> loans = loanService.getAll();
            log.debug("Retrieved {} loans.", loans.size());

            if (loans.isEmpty()) {
                System.out.println("No loans found.");
                return;
            }

            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            log.error("Failed to retrieve loans.", ex);
            System.out.println("Error retrieving loans.");
        }
    }

    private void checkoutBook() {
        log.info("Checkout Book operation started.");
        System.out.println();
        System.out.println("=== CHECKOUT BOOK ===");

        try {
            // Inline validation per field (BookController style)
            long bookId = promptValidBookIdCheckout();
            long memberId = promptValidMemberIdCheckout();
            int loanDays = promptLoanLengthDaysCheckout(); // normalized (defaults to 14)

            LocalDate checkoutDate = LocalDate.now();
            LocalDate dueDate = checkoutDate.plusDays(loanDays);

            // Validate derived dates against constraints (defensive)
            LoanValidator.requireValidCheckoutDate(checkoutDate);
            LoanValidator.requireValidDueDate(dueDate, checkoutDate);

            log.debug(
                    "Checkout validated input - bookId={}, memberId={}, loanDays={}, checkoutDate={}, dueDate={}",
                    bookId, memberId, loanDays, checkoutDate, dueDate
            );

            Loan loan = new Loan();
            loan.setBookId(bookId);
            loan.setMemberId(memberId);
            loan.setCheckoutDate(checkoutDate);
            loan.setDueDate(dueDate);
            loan.setReturnDate(null);

            Long id = loanService.checkout(loan);

            log.info("Checkout successful. Created loan id={}", id);
            System.out.println("Checkout successful. Created loan id=" + id);
            System.out.println("Due date: " + dueDate);

        } catch (RuntimeException ex) {
            // Note: inline validators throw IllegalArgumentException, which is a RuntimeException
            log.error("Unexpected error during checkout.", ex);
            System.out.println("Could not checkout book: " + ex.getMessage());
        }
    }

    private void returnBook() {
        System.out.println();
        System.out.println("=== RETURN BOOK ===");

        try {
            long loanId = promptValidLoanIdReturn();

            // If you later choose to let users enter a custom return date, validate it
            // against the loan's checkout date in the service layer (because you need
            // the existing loan to compare).
            LocalDate returnDate = LocalDate.now();

            log.debug("Return requested for loanId={} on {}", loanId, returnDate);

            boolean returned = loanService.returnLoan(loanId, returnDate);
            if (returned) {
                log.info("Loan returned successfully for loanId={}", loanId);
                System.out.println("Return successful for loan id=" + loanId + " on " + returnDate);
            } else {
                log.info("No active loan found to return for loanId={}", loanId);
                System.out.println("No active loan found with id=" + loanId + " (nothing returned).");
            }

        } catch (RuntimeException ex) {
            log.error("Unexpected error during return.", ex);
            System.out.println("Could not return loan: " + ex.getMessage());
        }
    }

    private void findLoanById() {
        System.out.println();
        System.out.println("=== FIND LOAN ===");

        long id = promptValidLoanIdFind();
        log.debug("Find Loan requested for id={}", id);

        try {
            Optional<Loan> maybeLoan = loanService.getById(id);
            if (maybeLoan.isEmpty()) {
                log.info("No loan found with id={}", id);
                System.out.println("No loan found with id=" + id);
                return;
            }

            log.info("Loan found with id={}", id);
            System.out.println(maybeLoan.get());

        } catch (RuntimeException ex) {
            log.error("Error finding loan with id={}", id, ex);
            System.out.println("Error finding loan.");
        }
    }

    private void deleteLoan() {
        System.out.println();
        System.out.println("=== DELETE LOAN ===");

        long id = promptValidLoanIdDelete();
        log.debug("Delete Loan requested for id={}", id);

        try {
            boolean deleted = loanService.delete(id);
            if (deleted) {
                log.info("Loan deleted successfully with id={}", id);
                System.out.println("Deleted loan id=" + id);
            } else {
                log.info("No loan found to delete with id={}", id);
                System.out.println("No loan found with id=" + id + " (nothing deleted).");
            }
        } catch (RuntimeException ex) {
            log.error("Error deleting loan with id={}", id, ex);
            System.out.println("Error deleting loan.");
        }
    }

    private void listActiveLoans() {
        log.info("Listing active loans.");
        System.out.println();
        System.out.println("=== ACTIVE LOANS ===");

        try {
            List<Loan> loans = loanService.getActiveLoans();
            log.debug("Retrieved {} active loans.", loans.size());

            if (loans.isEmpty()) {
                System.out.println("No active loans found.");
                return;
            }

            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            log.error("Failed to retrieve active loans.", ex);
            System.out.println("Error retrieving active loans.");
        }
    }

    private void listLoansByMember() {
        System.out.println();
        System.out.println("=== LOANS BY MEMBER ===");

        long memberId = promptValidMemberIdListByMember();
        log.debug("Listing loans for memberId={}", memberId);

        try {
            List<Loan> loans = loanService.getLoansByMemberId(memberId);
            log.debug("Retrieved {} loans for memberId={}", loans.size(), memberId);

            if (loans.isEmpty()) {
                System.out.println("No loans found for memberId=" + memberId);
                return;
            }

            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            log.error("Error retrieving loans for memberId={}", memberId, ex);
            System.out.println("Error retrieving loans for member.");
        }
    }

    private void listOverdueLoans() {
        System.out.println();
        System.out.println("=== OVERDUE LOANS ===");

        try {
            LocalDate today = LocalDate.now();
            log.debug("Listing overdue loans as of {}", today);

            List<Loan> loans = loanService.getOverdueLoans(today);
            log.debug("Retrieved {} overdue loans.", loans.size());

            if (loans.isEmpty()) {
                System.out.println("No overdue loans as of " + today);
                return;
            }

            System.out.println("Overdue as of " + today + ":");
            loans.forEach(System.out::println);

        } catch (RuntimeException ex) {
            log.error("Error retrieving overdue loans.", ex);
            System.out.println("Error retrieving overdue loans.");
        }
    }

    // -------------------------------------------------------------------------
    // Inline prompt + validation helpers (BookController-style)
    // -------------------------------------------------------------------------

    private long promptValidBookIdCheckout() {
        while (true) {
            long input = InputUtil.readInt("Book ID: ");
            try {
                return LoanValidator.requireValidBookId(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid bookId input: {}", ex.getMessage());
                System.out.println("Invalid Book ID: " + ex.getMessage());
            }
        }
    }

    private long promptValidMemberIdCheckout() {
        while (true) {
            long input = InputUtil.readInt("Member ID: ");
            try {
                return LoanValidator.requireValidMemberId(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid memberId input: {}", ex.getMessage());
                System.out.println("Invalid Member ID: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for the loan length:
     * - 0 or less => default 14
     * - positive => use as-is
     *
     * (Not a DB constraint, but matches your existing controller behavior.)
     */
    private int promptLoanLengthDaysCheckout() {
        while (true) {
            int input = InputUtil.readInt("Loan length in days (0 for default 14): ");
            if (input <= 0) return 14;

            // Optional sanity cap (feel free to remove). Helps prevent fat-finger errors.
            if (input > 3650) {
                log.warn("Unreasonably large loan length entered: {}", input);
                System.out.println("That loan length seems too large. Please enter a smaller number.");
                continue;
            }

            return input;
        }
    }

    private long promptValidLoanIdReturn() {
        while (true) {
            long input = InputUtil.readInt("Loan ID to return: ");
            if (input <= 0) {
                log.warn("Invalid loanId input (return): {}", input);
                System.out.println("Invalid Loan ID: must be a positive number.");
                continue;
            }
            return input;
        }
    }

    private long promptValidLoanIdFind() {
        while (true) {
            long input = InputUtil.readInt("Loan ID: ");
            if (input <= 0) {
                log.warn("Invalid loanId input (find): {}", input);
                System.out.println("Invalid Loan ID: must be a positive number.");
                continue;
            }
            return input;
        }
    }

    private long promptValidLoanIdDelete() {
        while (true) {
            long input = InputUtil.readInt("Loan ID to delete: ");
            if (input <= 0) {
                log.warn("Invalid loanId input (delete): {}", input);
                System.out.println("Invalid Loan ID: must be a positive number.");
                continue;
            }
            return input;
        }
    }

    private long promptValidMemberIdListByMember() {
        while (true) {
            long input = InputUtil.readInt("Member ID: ");
            try {
                return LoanValidator.requireValidMemberId(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid memberId input (list by member): {}", ex.getMessage());
                System.out.println("Invalid Member ID: " + ex.getMessage());
            }
        }
    }
}
