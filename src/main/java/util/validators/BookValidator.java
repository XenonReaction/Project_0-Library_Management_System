package util.validators;

import java.util.regex.Pattern;

/**
 * Utility class responsible for validating Book-related input against
 * database constraints and application business rules.
 *
 * <p>This validator is intentionally stateless and designed to be used
 * by controllers and services prior to persistence.</p>
 *
 * <p><strong>Database-aligned rules:</strong></p>
 * <ul>
 *   <li><b>title</b>: required, non-blank, max 255 characters</li>
 *   <li><b>author</b>: required, non-blank, max 255 characters</li>
 *   <li><b>isbn</b>: optional; if present must match {@code ^[0-9Xx-]{10,20}$}</li>
 *   <li><b>publication_year</b>: optional; if present must be between 1400 and 3000</li>
 * </ul>
 *
 * <p><strong>Controller-friendly helpers:</strong></p>
 * <ul>
 *   <li>{@code requirePositiveId} for ID validation</li>
 *   <li>{@code normalizeUpdatePublicationYear} for update sentinel logic</li>
 * </ul>
 *
 * <p><strong>Update sentinel rules:</strong></p>
 * <ul>
 *   <li>{@code -1} → keep current value</li>
 *   <li>{@code  0} → clear value (set {@code NULL})</li>
 *   <li>{@code >0} → set to that year (validated)</li>
 * </ul>
 */
public final class BookValidator {

    /**
     * Private constructor to prevent instantiation.
     */
    private BookValidator() {
        // utility class
    }

    /**
     * Pattern matching the SQL CHECK constraint for ISBN values.
     */
    private static final Pattern ISBN_PATTERN =
            Pattern.compile("^[0-9Xx-]{10,20}$");

    // -------------------------------------------------------------------------
    // ID validation
    // -------------------------------------------------------------------------

    /**
     * Ensures an identifier is positive (> 0).
     *
     * @param id        numeric identifier
     * @param fieldName logical field name (used in error messages)
     * @return the same id if valid
     * @throws IllegalArgumentException if id is not positive
     */
    public static long requirePositiveId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive number.");
        }
        return id;
    }

    // -------------------------------------------------------------------------
    // Required text fields
    // -------------------------------------------------------------------------

    /**
     * Validates a required book title.
     *
     * @param title raw title input
     * @return trimmed, validated title
     * @throws IllegalArgumentException if null, blank, or too long
     */
    public static String requireValidTitle(String title) {
        String t = normalizeRequiredText(title, "title");
        if (t.length() > 255) {
            throw new IllegalArgumentException("Title must be 255 characters or fewer.");
        }
        return t;
    }

    /**
     * Validates a required author name.
     *
     * @param author raw author input
     * @return trimmed, validated author
     * @throws IllegalArgumentException if null, blank, or too long
     */
    public static String requireValidAuthor(String author) {
        String a = normalizeRequiredText(author, "author");
        if (a.length() > 255) {
            throw new IllegalArgumentException("Author must be 255 characters or fewer.");
        }
        return a;
    }

    // -------------------------------------------------------------------------
    // ISBN (optional)
    // -------------------------------------------------------------------------

    /**
     * Normalizes optional ISBN input.
     *
     * <p>Accepted "empty" values:</p>
     * <ul>
     *   <li>{@code null}</li>
     *   <li>blank string</li>
     *   <li>{@code "NONE"} (case-insensitive)</li>
     * </ul>
     *
     * @param raw raw ISBN input
     * @return trimmed ISBN or {@code null}
     */
    public static String normalizeOptionalIsbn(String raw) {
        if (raw == null) return null;

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.equalsIgnoreCase("NONE")) return null;

        return trimmed;
    }

    /**
     * Validates an optional ISBN against the database constraint.
     *
     * @param isbn normalized ISBN (may be {@code null})
     * @return the same ISBN if valid
     * @throws IllegalArgumentException if format is invalid
     */
    public static String validateOptionalIsbn(String isbn) {
        if (isbn == null) return null;

        String trimmed = isbn.trim();
        if (trimmed.isEmpty()) return null;

        if (!ISBN_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "ISBN must be 10–20 characters and contain only digits, X/x, or hyphens " +
                            "(e.g., 978-1-4028-9462-6)."
            );
        }

        return trimmed;
    }

    // -------------------------------------------------------------------------
    // Publication year (optional)
    // -------------------------------------------------------------------------

    /**
     * Normalizes publication year input for CREATE flows.
     *
     * @param yearInput raw integer input
     * @return {@code null} if {@code yearInput <= 0}, otherwise the same value
     */
    public static Integer normalizeOptionalPublicationYear(int yearInput) {
        return (yearInput <= 0) ? null : yearInput;
    }

    /**
     * Validates an optional publication year against database constraints.
     *
     * @param year publication year (nullable)
     * @return the same year if valid
     * @throws IllegalArgumentException if out of range
     */
    public static Integer validateOptionalPublicationYear(Integer year) {
        if (year == null) return null;

        if (year < 1400 || year > 3000) {
            throw new IllegalArgumentException(
                    "Publication year must be between 1400 and 3000 (or omitted)."
            );
        }

        return year;
    }

    /**
     * Normalizes UPDATE-flow publication year input using sentinel values.
     *
     * @param input        raw year input
     * @param currentValue existing publication year (nullable)
     * @return normalized year value
     */
    public static Integer normalizeUpdatePublicationYear(int input, Integer currentValue) {
        if (input == -1) return currentValue;
        if (input == 0) return null;

        if (input < -1) {
            throw new IllegalArgumentException(
                    "Use -1 to keep current, 0 to clear, or enter a valid year."
            );
        }

        return input;
    }

    // -------------------------------------------------------------------------
    // Convenience validation
    // -------------------------------------------------------------------------

    /**
     * Validates all book fields for CREATE flows and returns canonical values.
     *
     * @param title   raw title
     * @param author  raw author
     * @param rawIsbn raw ISBN input
     * @param year    publication year
     * @return container holding validated values
     */
    public static ValidatedBookFields validateForCreate(
            String title,
            String author,
            String rawIsbn,
            Integer year
    ) {
        String t = requireValidTitle(title);
        String a = requireValidAuthor(author);

        String normalizedIsbn = normalizeOptionalIsbn(rawIsbn);
        String i = validateOptionalIsbn(normalizedIsbn);

        Integer y = validateOptionalPublicationYear(year);

        return new ValidatedBookFields(t, a, i, y);
    }

    /**
     * Immutable value object holding validated book fields.
     *
     * @param title            validated title
     * @param author           validated author
     * @param isbn             validated ISBN (nullable)
     * @param publicationYear  validated publication year (nullable)
     */
    public record ValidatedBookFields(
            String title,
            String author,
            String isbn,
            Integer publicationYear
    ) { }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Normalizes and validates required text fields.
     */
    private static String normalizeRequiredText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(cap(fieldName) + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(cap(fieldName) + " is required.");
        }
        return trimmed;
    }

    /**
     * Capitalizes the first letter of a field name for error messages.
     */
    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
