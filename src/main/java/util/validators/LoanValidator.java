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

    public static LocalDate requireValidCheckoutDate(LocalDate checkoutDate) {
        if (checkoutDate == null) {
            throw new IllegalArgumentException("Checkout date cannot be null.");
        }
        return checkoutDate;
    }

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

    /**
     * Optional business-rule guard: reject dates in the future.
     * (Not required by DB constraints.)
     */
    public static LocalDate requireNotFutureDate(LocalDate date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " cannot be in the future.");
        }
        return date;
    }

    // -------------------------------------------------------------------------
    // Optional convenience helpers (useful for inline prompts)
    // -------------------------------------------------------------------------

    public static LocalDate normalizeCheckoutDate(LocalDate checkoutDate) {
        return (checkoutDate == null) ? LocalDate.now() : checkoutDate;
    }

    /**
     * Optional: normalize loan length days for checkout flows.
     * - <= 0 => defaultDays
     * - > maxDays => reject (helps prevent fat-finger values)
     */
    public static int normalizeLoanLengthDays(int days, int defaultDays, int maxDays) {
        if (days <= 0) return defaultDays;
        if (days > maxDays) {
            throw new IllegalArgumentException("Loan length is too large (max " + maxDays + " days).");
        }
        return days;
    }
}
