package util.validators;

import java.time.LocalDate;

/**
 * Utility class responsible for validating Loan-related input against
 * database constraints and basic business rules.
 *
 * <p>This validator is intended for use primarily in controller-layer
 * workflows, where individual fields are validated inline before being
 * passed to the service layer.</p>
 *
 * <p><strong>Database-aligned constraints (table: {@code loans}):</strong></p>
 * <ul>
 *   <li><b>book_id</b>: NOT NULL, must be a positive number (FK to books.id)</li>
 *   <li><b>member_id</b>: NOT NULL, must be a positive number (FK to members.id)</li>
 *   <li><b>checkout_date</b>: NOT NULL (defaults to CURRENT_DATE in DB)</li>
 *   <li><b>due_date</b>: NOT NULL and must be on or after checkout_date</li>
 *   <li><b>return_date</b>: nullable; if present, must be on or after checkout_date</li>
 * </ul>
 *
 * <p><strong>Notes:</strong></p>
 * <ul>
 *   <li>Foreign key existence (book/member actually existing) is enforced
 *       in the service or repository layers, not here.</li>
 *   <li>This class is stateless and cannot be instantiated.</li>
 * </ul>
 */
public final class LoanValidator {

    /**
     * Private constructor to prevent instantiation.
     */
    private LoanValidator() {
        // utility class
    }

    // -------------------------------------------------------------------------
    // ID validation (book_id, member_id)
    // -------------------------------------------------------------------------

    /**
     * Ensures a valid book identifier.
     *
     * @param bookId book identifier
     * @return the same bookId if valid
     * @throws IllegalArgumentException if bookId is not positive
     */
    public static long requireValidBookId(long bookId) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("Book ID must be a positive number.");
        }
        return bookId;
    }

    /**
     * Ensures a valid member identifier.
     *
     * @param memberId member identifier
     * @return the same memberId if valid
     * @throws IllegalArgumentException if memberId is not positive
     */
    public static long requireValidMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Member ID must be a positive number.");
        }
        return memberId;
    }

    // -------------------------------------------------------------------------
    // Date validation (checkout_date, due_date, return_date)
    // -------------------------------------------------------------------------

    /**
     * Ensures the checkout date is present.
     *
     * @param checkoutDate checkout date
     * @return the same checkoutDate if valid
     * @throws IllegalArgumentException if checkoutDate is null
     */
    public static LocalDate requireValidCheckoutDate(LocalDate checkoutDate) {
        if (checkoutDate == null) {
            throw new IllegalArgumentException("Checkout date cannot be null.");
        }
        return checkoutDate;
    }

    /**
     * Ensures the due date is present and not before the checkout date.
     *
     * @param dueDate      due date
     * @param checkoutDate checkout date
     * @return the same dueDate if valid
     * @throws IllegalArgumentException if dueDate is null or before checkoutDate
     */
    public static LocalDate requireValidDueDate(LocalDate dueDate, LocalDate checkoutDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null.");
        }

        LocalDate cd = requireValidCheckoutDate(checkoutDate);
        if (dueDate.isBefore(cd)) {
            throw new IllegalArgumentException(
                    "Due date must be on or after the checkout date."
            );
        }
        return dueDate;
    }

    /**
     * Validates an optional return date.
     *
     * @param returnDate   return date (nullable)
     * @param checkoutDate checkout date
     * @return the same returnDate if valid, or {@code null}
     * @throws IllegalArgumentException if returnDate is before checkoutDate
     */
    public static LocalDate validateOptionalReturnDate(
            LocalDate returnDate,
            LocalDate checkoutDate
    ) {
        if (returnDate == null) {
            return null;
        }

        LocalDate cd = requireValidCheckoutDate(checkoutDate);
        if (returnDate.isBefore(cd)) {
            throw new IllegalArgumentException(
                    "Return date must be on or after the checkout date."
            );
        }
        return returnDate;
    }

    /**
     * Optional business-rule guard that rejects dates in the future.
     *
     * <p>This rule is <em>not</em> required by database constraints but may
     * be useful for enforcing real-world expectations.</p>
     *
     * @param date      date to validate
     * @param fieldName logical field name for error messages
     * @return the same date if valid
     * @throws IllegalArgumentException if date is null or in the future
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
    // Convenience helpers (controller-friendly)
    // -------------------------------------------------------------------------

    /**
     * Normalizes a checkout date by defaulting to today if null.
     *
     * @param checkoutDate checkout date (nullable)
     * @return checkoutDate or {@link LocalDate#now()}
     */
    public static LocalDate normalizeCheckoutDate(LocalDate checkoutDate) {
        return (checkoutDate == null) ? LocalDate.now() : checkoutDate;
    }

    /**
     * Normalizes loan length (in days) for checkout flows.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>{@code days <= 0} → defaultDays</li>
     *   <li>{@code days > maxDays} → rejected</li>
     * </ul>
     *
     * @param days         requested loan length
     * @param defaultDays default loan length
     * @param maxDays     maximum allowed loan length
     * @return normalized loan length
     * @throws IllegalArgumentException if days exceeds maxDays
     */
    public static int normalizeLoanLengthDays(
            int days,
            int defaultDays,
            int maxDays
    ) {
        if (days <= 0) return defaultDays;

        if (days > maxDays) {
            throw new IllegalArgumentException(
                    "Loan length is too large (max " + maxDays + " days)."
            );
        }
        return days;
    }
}
