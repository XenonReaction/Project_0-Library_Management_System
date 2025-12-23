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

/**
 * Controller for all console-based Loan operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Display the Loan Services menu and route user choices to operations</li>
 *   <li>Prompt for user input using {@link InputUtil}</li>
 *   <li>Validate user-entered values (IDs and dates) using {@link LoanValidator}</li>
 *   <li>Delegate business rules and persistence coordination to {@link LoanService}</li>
 * </ul>
 *
 * <p>This class focuses on console I/O and per-field validation. Cross-field business rules
 * (e.g., "book cannot be checked out twice") should live in the service layer.
 */
public class LoanController {

    private static final Logger log = LoggerFactory.getLogger(LoanController.class);

    private final LoanService loanService;

    /**
     * Default loan length in days when the user chooses the default option during checkout.
     */
    private static final int DEFAULT_LOAN_DAYS = 14;

    /**
     * Upper bound on loan length (sanity cap) to prevent unrealistic due dates.
     */
    private static final int MAX_LOAN_DAYS = 3650;

    /**
     * Constructs a {@code LoanController} using a default {@link LoanService}.
     *
     * <p>Used in the normal application flow where dependency injection is not required.
     */
    public LoanController() {
        this.loanService = new LoanService();
        log.debug("LoanController initialized with default LoanService.");
    }

    /**
     * Constructs a {@code LoanController} using an injected {@link LoanService}.
     *
     * <p>Primarily intended for unit tests where {@code LoanService} may be mocked.
     *
     * @param loanService service instance to use (must not be null)
     * @throws IllegalArgumentException if {@code loanService} is null
     */
    // Optional: for unit tests (inject a mocked service)
    public LoanController(LoanService loanService) {
        if (loanService == null) {
            log.error("Attempted to initialize LoanController with null LoanService.");
            throw new IllegalArgumentException("loanService cannot be null.");
        }
        this.loanService = loanService;
        log.debug("LoanController initialized with injected LoanService.");
    }

    /**
     * Runs the Loan Services menu loop until the user chooses to exit back to the main menu.
     *
     * <p>Menu actions are protected by a try/catch to keep the application resilient against
     * invalid input or unexpected runtime failures.
     */
    public void handleInput() {
        log.info("Entered Loan Services menu.");
        boolean running = true;

        while (running) {
            try {
                printMenu();
                int choice = InputUtil.readInt("Make a choice: ");
                log.debug("Loan menu selection received: {}", choice);

                switch (choice) {
                    case 1 -> {
                        listAllLoans();
                        pressEnterToContinue();
                    }
                    case 2 -> {
                        checkoutBook();
                        pressEnterToContinue();
                    }
                    case 3 -> {
                        returnBook();
                        pressEnterToContinue();
                    }
                    case 4 -> {
                        findLoanById();
                        pressEnterToContinue();
                    }
                    case 5 -> {
                        deleteLoan();
                        pressEnterToContinue();
                    }
                    case 6 -> {
                        listActiveLoans();
                        pressEnterToContinue();
                    }
                    case 7 -> {
                        listLoansByMember();
                        pressEnterToContinue();
                    }
                    case 8 -> {
                        listOverdueLoans();
                        pressEnterToContinue();
                    }
                    case 0 -> {
                        log.info("Exiting Loan Services menu.");
                        running = false; // no pause here
                    }
                    default -> {
                        log.warn("Invalid Loan menu option selected: {}", choice);
                        System.out.println("Invalid option. Please try again.");
                        pressEnterToContinue();
                    }
                }
            } catch (Exception ex) {
                log.error("Unhandled exception in LoanController menu loop.", ex);
                System.out.println("An unexpected error occurred. Please try again.");
                pressEnterToContinue();
            }
        }
    }

    /**
     * Prints the Loan Services menu to the console.
     */
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
        System.out.println("0. Back to Main Menu");
    }

    /**
     * Prompts the user to press Enter so the UI doesn't immediately repaint the menu.
     *
     * <p>Uses {@link InputUtil#readLineAllowEmpty(String)} to allow an empty line without validation.
     */
    private void pressEnterToContinue() {
        InputUtil.readLineAllowEmpty("Press Enter to continue...");
    }

    /**
     * Retrieves all loans from the service layer and prints them to the console.
     *
     * <p>If no loans exist, prints a friendly message instead of printing nothing.
     */
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

    /**
     * Checks out a book by creating a new loan.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Prompt for and validate book ID and member ID</li>
     *   <li>Prompt for and normalize loan length (days)</li>
     *   <li>Derive checkout date (today) and due date (today + loanDays)</li>
     *   <li>Validate derived dates defensively using {@link LoanValidator}</li>
     *   <li>Delegate to {@link LoanService#checkout(Loan)}</li>
     * </ol>
     *
     * <p>Business rules (e.g., "book already checked out") are expected to be enforced by the service layer
     * and may surface as {@link IllegalStateException}.
     */
    private void checkoutBook() {
        log.info("Checkout Book operation started.");
        System.out.println();
        System.out.println("=== CHECKOUT BOOK ===");

        try {
            long bookId = promptValidBookIdCheckout();
            long memberId = promptValidMemberIdCheckout();

            // uses LoanValidator.normalizeLoanLengthDays(...)
            int loanDays = promptLoanLengthDaysCheckout();

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

        } catch (IllegalStateException ex) {
            // policy violations from LoanService (book already checked out, etc.)
            log.warn("Checkout blocked by business rule: {}", ex.getMessage());
            System.out.println("Checkout blocked: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Validation error during checkout: {}", ex.getMessage());
            System.out.println("Could not checkout book: " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error during checkout.", ex);
            System.out.println("Could not checkout book: " + ex.getMessage());
        }
    }

    /**
     * Returns a book by setting the return date on an existing loan.
     *
     * <p>Uses today's date as the return date and delegates to {@link LoanService#returnLoan(long, LocalDate)}.
     */
    private void returnBook() {
        System.out.println();
        System.out.println("=== RETURN BOOK ===");

        try {
            long loanId = promptValidLoanIdReturn();
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

        } catch (IllegalArgumentException ex) {
            log.warn("Validation error during return: {}", ex.getMessage());
            System.out.println("Could not return loan: " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error during return.", ex);
            System.out.println("Could not return loan: " + ex.getMessage());
        }
    }

    /**
     * Prompts for a loan ID, fetches the loan from the service layer, and prints it if found.
     */
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

    /**
     * Deletes a loan if permitted by the service layer.
     *
     * <p>Services may block deleting "active" loans (not yet returned), surfacing an {@link IllegalStateException}.
     */
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
                log.info("Loan not deleted for id={} (not found or blocked by policy).", id);
                System.out.println("Loan not deleted (not found or cannot delete an active loan). id=" + id);
            }
        } catch (IllegalStateException ex) {
            log.warn("Delete blocked by business rule: {}", ex.getMessage());
            System.out.println("Delete blocked: " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Error deleting loan with id={}", id, ex);
            System.out.println("Error deleting loan.");
        }
    }

    /**
     * Lists active loans (loans without a return date) by delegating to the service layer.
     */
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

    /**
     * Lists all loans for a specific member ID.
     *
     * <p>Prompts for the member ID and delegates the lookup to the service layer.
     */
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

        } catch (IllegalArgumentException ex) {
            log.warn("Validation error listing loans by member: {}", ex.getMessage());
            System.out.println("Invalid member id: " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Error retrieving loans for memberId={}", memberId, ex);
            System.out.println("Error retrieving loans for member.");
        }
    }

    /**
     * Lists overdue loans as of today by delegating to the service layer.
     *
     * <p>"Overdue" is typically defined as: return date is null and due date is before the provided date.
     */
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
    // Inline prompt + validation helpers
    // -------------------------------------------------------------------------

    /**
     * Prompts for a Book ID during checkout and validates it using {@link LoanValidator}.
     *
     * @return validated book ID (> 0)
     */
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

    /**
     * Prompts for a Member ID during checkout and validates it using {@link LoanValidator}.
     *
     * @return validated member ID (> 0)
     */
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
     * Prompts for loan length in days and normalizes it using {@link LoanValidator}.
     *
     * <p>Sentinel behavior:
     * <ul>
     *   <li>0 -> default loan length ({@link #DEFAULT_LOAN_DAYS})</li>
     * </ul>
     *
     * @return normalized, validated loan length in days
     */
    private int promptLoanLengthDaysCheckout() {
        while (true) {
            int input = InputUtil.readInt("Loan length in days (0 for default " + DEFAULT_LOAN_DAYS + "): ");
            try {
                return LoanValidator.normalizeLoanLengthDays(input, DEFAULT_LOAN_DAYS, MAX_LOAN_DAYS);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid loan length entered: {}", ex.getMessage());
                System.out.println("Invalid loan length: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for a Loan ID to return.
     *
     * @return validated loan ID (> 0)
     */
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

    /**
     * Prompts for a Loan ID to find.
     *
     * @return validated loan ID (> 0)
     */
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

    /**
     * Prompts for a Loan ID to delete.
     *
     * @return validated loan ID (> 0)
     */
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

    /**
     * Prompts for a Member ID when listing loans by member and validates it using {@link LoanValidator}.
     *
     * @return validated member ID (> 0)
     */
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
