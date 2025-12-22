package util.validators;

import java.time.LocalDate;

/**
 * Validates Loan-related user input to match database constraints:
 *
 * Table: loans
 * - book_id:   NOT NULL (FK to books.id)
 * - member_id: NOT NULL (FK to members.id)
 * - checkout_date: NOT NULL (defaults to CURRENT_DATE in DB)
 * - due_date:  NOT NULL and must be >= checkout_date
 * - return_date: nullable, but if present must be >= checkout_date
 *
 * Notes:
 * - FK existence (book_id / member_id actually existing) is typically enforced
 *   in the service/repository layer (via lookup) rather than a pure validator.
 * - This validator is designed for inline, per-field validation in controllers.
 */
public final class LoanValidator {

    private LoanValidator() {
        // utility class
    }

    // -------------------------------------------------------------------------
    // IDs (book_id, member_id)
    // -------------------------------------------------------------------------

    public static long requireValidBookId(long bookId) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("Book ID must be a positive number.");
        }
        return bookId;
    }

    public static long requireValidMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Member ID must be a positive number.");
        }
        return memberId;
    }

    // -------------------------------------------------------------------------
    // Dates (checkout_date, due_date, return_date)
    // -------------------------------------------------------------------------

    /**
     * checkout_date is NOT NULL in DB. If your controller always uses LocalDate.now(),
     * this still provides a consistent guard for tests / future changes.
     */
    public static LocalDate requireValidCheckoutDate(LocalDate checkoutDate) {
        if (checkoutDate == null) {
            throw new IllegalArgumentException("Checkout date cannot be null.");
        }
        return checkoutDate;
    }

    /**
     * due_date is NOT NULL and must be >= checkout_date.
     */
    public static LocalDate requireValidDueDate(LocalDate dueDate, LocalDate checkoutDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null.");
        }
        LocalDate cd = requireValidCheckoutDate(checkoutDate);

        if (dueDate.isBefore(cd)) {
            throw new IllegalArgumentException("Due date must be on or after the checkout date.");
        }
        return dueDate;
    }

    /**
     * return_date is nullable; if present must be >= checkout_date.
     */
    public static LocalDate validateOptionalReturnDate(LocalDate returnDate, LocalDate checkoutDate) {
        if (returnDate == null) {
            return null;
        }
        LocalDate cd = requireValidCheckoutDate(checkoutDate);

        if (returnDate.isBefore(cd)) {
            throw new IllegalArgumentException("Return date must be on or after the checkout date.");
        }
        return returnDate;
    }

    // -------------------------------------------------------------------------
    // Optional convenience helpers (useful for inline prompts)
    // -------------------------------------------------------------------------

    /**
     * Some flows may allow the user to specify a custom checkout date.
     * If the caller passes null, default to today.
     */
    public static LocalDate normalizeCheckoutDate(LocalDate checkoutDate) {
        return (checkoutDate == null) ? LocalDate.now() : checkoutDate;
    }
}

