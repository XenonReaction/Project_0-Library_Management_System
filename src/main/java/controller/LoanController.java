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
 * <p><b>Role in architecture:</b> The controller layer is responsible for console I/O
 * (menus, prompts, printing results) and per-field validation of user input. It delegates
 * cross-field rules and persistence coordination to the {@link LoanService}.</p>
 *
 * <h2>Primary responsibilities</h2>
 * <ul>
 *   <li>Display the Loan Services menu and route user choices to operations</li>
 *   <li>Prompt for user input using {@link InputUtil}</li>
 *   <li>Validate user-entered values (IDs and dates) using {@link LoanValidator}</li>
 *   <li>Delegate business rules and persistence coordination to {@link LoanService}</li>
 * </ul>
 *
 * <h2>Error handling</h2>
 * <p>{@link LoanService} allows {@link IllegalArgumentException} to bubble up for “clean”
 * DB constraint violations translated by the DAO (e.g., FK violations from invalid bookId/memberId).
 * This controller catches those and prints the message for user-friendly feedback.</p>
 */
public class LoanController {

    /** Logger for controller-level flow and user-visible operations. */
    private static final Logger log = LoggerFactory.getLogger(LoanController.class);

    /** Service dependency containing business rules and persistence coordination. */
    private final LoanService loanService;

    /** Default loan duration (days) used when user enters 0. */
    private static final int DEFAULT_LOAN_DAYS = 14;

    /** Upper bound for a loan duration (days) to prevent unrealistic values. */
    private static final int MAX_LOAN_DAYS = 3650;

    /**
     * Constructs a {@code LoanController} using the default {@link LoanService}.
     */
    public LoanController() {
        this.loanService = new LoanService();
        log.debug("LoanController initialized with default LoanService.");
    }

    /**
     * Constructs a {@code LoanController} with an injected {@link LoanService}.
     *
     * <p>Useful for unit testing (e.g., providing a mocked service).</p>
     *
     * @param loanService service dependency (must not be null)
     * @throws IllegalArgumentException if {@code loanService} is null
     */
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
     * <p>This method is the entry point called by the main menu controller.
     * It handles menu display, routing, and a general exception boundary so the
     * application stays interactive even if an unexpected error occurs.</p>
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
                        running = false;
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
     * Pauses the console until the user presses Enter.
     *
     * <p>This improves the UX by letting the user read results before the menu redraws.</p>
     */
    private void pressEnterToContinue() {
        InputUtil.readLineAllowEmpty("Press Enter to continue...");
    }

    /**
     * Lists all loans currently stored (returned + active).
     *
     * <p>Calls {@link LoanService#getAll()} and prints each loan using its {@code toString()}.</p>
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
     * <p><b>User flow:</b> Prompts for book ID, member ID, and loan length (days), then uses the
     * current date as checkout date and computes the due date.</p>
     *
     * <p><b>Re-prompt behavior:</b> If the user enters IDs that look valid but do not exist,
     * the DAO (via service) throws an {@link IllegalArgumentException} with a specific message.
     * For those “missing ID” messages, this method loops and re-prompts so the user can correct
     * the entry without being kicked back to the menu.</p>
     *
     * <p><b>Business rule violations</b> (e.g., book already checked out) are typically surfaced
     * as {@link IllegalStateException} and cause a return back to the menu.</p>
     */
    private void checkoutBook() {
        log.info("Checkout Book operation started.");
        System.out.println();
        System.out.println("=== CHECKOUT BOOK ===");

        while (true) {
            try {
                long bookId = promptValidBookIdCheckout();
                long memberId = promptValidMemberIdCheckout();
                int loanDays = promptLoanLengthDaysCheckout();

                LocalDate checkoutDate = LocalDate.now();
                LocalDate dueDate = checkoutDate.plusDays(loanDays);

                // Defensive validation of derived dates
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
                return;

            } catch (IllegalStateException ex) {
                log.warn("Checkout blocked by business rule: {}", ex.getMessage());
                System.out.println("Checkout blocked: " + ex.getMessage());
                return;

            } catch (IllegalArgumentException ex) {
                String msg = ex.getMessage() == null ? "" : ex.getMessage();

                log.warn("Checkout rejected: {}", msg);
                System.out.println("Could not checkout book: " + msg);

                if (isLikelyMissingIdMessage(msg)) {
                    System.out.println("Please re-enter the IDs.");
                    System.out.println();
                    continue;
                }

                return;

            } catch (RuntimeException ex) {
                log.error("Unexpected error during checkout.", ex);
                System.out.println("Could not checkout book due to an unexpected error.");
                return;
            }
        }
    }

    /**
     * Heuristic helper to detect DAO/service messages that indicate missing FK targets.
     *
     * <p>This keeps the controller UX smooth by re-prompting IDs for known, correctable cases.</p>
     *
     * @param msg exception message (typically from service/DAO)
     * @return true if the message likely indicates a missing book/member record
     */
    private boolean isLikelyMissingIdMessage(String msg) {
        String m = msg.toLowerCase();
        return m.contains("no book exists")
                || m.contains("no member exists")
                || (m.contains("invalid") && m.contains("does not exist"))
                || (m.contains("invalid") && (m.contains("bookid") || m.contains("memberid")));
    }

    /**
     * Returns a book by closing an active loan (sets return date).
     *
     * <p>The return date is set to {@link LocalDate#now()}.</p>
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
            System.out.println("Could not return loan due to an unexpected error.");
        }
    }

    /**
     * Finds and prints a loan by its ID.
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
     * Deletes a loan by its ID.
     *
     * <p>Service policy typically allows deletion only for returned loans
     * (active loans are preserved).</p>
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

        } catch (RuntimeException ex) {
            log.error("Error deleting loan with id={}", id, ex);
            System.out.println("Error deleting loan.");
        }
    }

    /**
     * Lists all active (unreturned) loans.
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
     * Lists all overdue loans as of today.
     *
     * <p>Overdue rules are enforced in the service/repository query:
     * typically {@code return_date IS NULL AND due_date < currentDate}.</p>
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
     * Prompts until the user enters a valid positive book ID for checkout.
     *
     * @return validated book ID
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
     * Prompts until the user enters a valid positive member ID for checkout.
     *
     * @return validated member ID
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
     * Prompts for loan length in days.
     *
     * <p>If the user enters {@code 0}, the controller uses {@link #DEFAULT_LOAN_DAYS}.
     * The final result is validated and normalized by {@link LoanValidator}.</p>
     *
     * @return normalized loan length in days
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
     * Prompts until a positive loan ID is entered for the return flow.
     *
     * @return positive loan ID
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
     * Prompts until a positive loan ID is entered for the find flow.
     *
     * @return positive loan ID
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
     * Prompts until a positive loan ID is entered for the delete flow.
     *
     * @return positive loan ID
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
     * Prompts until the user enters a valid positive member ID for listing loans by member.
     *
     * @return validated member ID
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
