package util.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared validation utility for service-layer input checks.
 *
 * <p>This class provides small, reusable validation helpers that enforce
 * common invariants across services (e.g., non-null values, non-blank strings,
 * and simple domain constraints).</p>
 *
 * <p><strong>Design intent:</strong></p>
 * <ul>
 *   <li>Used primarily in the <b>service layer</b>, not controllers</li>
 *   <li>Focused on generic, cross-entity rules (not entity-specific logic)</li>
 *   <li>Throws {@link IllegalArgumentException} on validation failure</li>
 *   <li>Logs validation failures at DEBUG level for troubleshooting</li>
 * </ul>
 *
 * <p>This class is stateless and cannot be instantiated.</p>
 */
public final class ValidationUtil {

    /**
     * Logger for validation-related debug output.
     */
    private static final Logger log = LoggerFactory.getLogger(ValidationUtil.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private ValidationUtil() {}

    /**
     * Ensures that a required value is not {@code null}.
     *
     * @param value     the value to check
     * @param fieldName logical field name used in error messages
     * @throws IllegalArgumentException if {@code value} is {@code null}
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            log.debug("Validation failed: {} cannot be null.", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
    }

    /**
     * Ensures that a required string value is not {@code null} or blank.
     *
     * <p>A string is considered invalid if it is {@code null}, empty,
     * or contains only whitespace.</p>
     *
     * @param value     the string to check
     * @param fieldName logical field name used in error messages
     * @throws IllegalArgumentException if the value is {@code null} or blank
     */
    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.debug("Validation failed: {} is required (blank or null).", fieldName);
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    /**
     * Validates an optional ISBN value against the database constraint.
     *
     * <p>Matches the SQL CHECK constraint:</p>
     * <pre>
     * ^[0-9Xx-]{10,20}$
     * </pre>
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>{@code null} is allowed</li>
     *   <li>10–20 characters</li>
     *   <li>Digits, {@code X/x}, and hyphens only</li>
     * </ul>
     *
     * @param isbn the ISBN value to validate (nullable)
     * @throws IllegalArgumentException if the ISBN format is invalid
     */
    public static void validateOptionalIsbn(String isbn) {
        if (isbn == null) return;

        if (!isbn.matches("^[0-9Xx-]{10,20}$")) {
            // ISBN is not sensitive, but guard against logging extreme input
            String safe = (isbn.length() > 50)
                    ? isbn.substring(0, 50) + "..."
                    : isbn;

            log.debug("Validation failed: isbn format invalid. value='{}'", safe);
            throw new IllegalArgumentException(
                    "isbn must be 10-20 chars (digits/X/-) or null."
            );
        }
    }

    /**
     * Validates an optional publication year against database constraints.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>{@code null} is allowed</li>
     *   <li>Year must be between 1400 and 3000 (inclusive)</li>
     * </ul>
     *
     * @param year the publication year to validate (nullable)
     * @throws IllegalArgumentException if the year is out of range
     */
    public static void validateOptionalPublicationYear(Integer year) {
        if (year == null) return;

        if (year < 1400 || year > 3000) {
            log.debug("Validation failed: publicationYear out of range. value={}", year);
            throw new IllegalArgumentException(
                    "publicationYear must be between 1400 and 3000 (or null)."
            );
        }
    }
}
