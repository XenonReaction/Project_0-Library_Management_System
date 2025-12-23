package util.validators;

import java.util.regex.Pattern;

/**
 * Validates Book-related user input to match database constraints:
 * - title: NOT NULL (and non-blank)
 * - author: NOT NULL (and non-blank)
 * - isbn: nullable, but if present must match ^[0-9Xx-]{10,20}$
 * - publication_year: nullable, but if present must be between 1400 and 3000 (inclusive)
 *
 * Also includes small helpers for controller flows:
 * - requirePositiveId(...) for ID validation
 * - normalizeUpdatePublicationYear(...) for the update sentinel rules:
 *     -1 = keep current
 *      0 = clear (NULL)
 *    > 0 = set to year (validated)
 */
public final class BookValidator {

    private BookValidator() {
        // utility class
    }

    // Matches the SQL CHECK constraint: '^[0-9Xx-]{10,20}$'
    private static final Pattern ISBN_PATTERN = Pattern.compile("^[0-9Xx-]{10,20}$");

    // -------------------------------------------------------------------------
    // IDs
    // -------------------------------------------------------------------------

    /**
     * Ensures an ID is positive (> 0).
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
     * Ensures title is not null/blank and returns a trimmed version.
     */
    public static String requireValidTitle(String title) {
        String t = normalizeRequiredText(title, "title");
        if (t.length() > 255) {
            throw new IllegalArgumentException("Title must be 255 characters or fewer.");
        }
        return t;
    }

    /**
     * Ensures author is not null/blank and returns a trimmed version.
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
     * Normalizes an optional ISBN input:
     * - null/blank/"NONE" -> null
     * - otherwise trimmed
     */
    public static String normalizeOptionalIsbn(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.equalsIgnoreCase("NONE")) return null;
        return trimmed;
    }

    /**
     * Validates the optional ISBN against the SQL CHECK constraint.
     * Returns the same value if valid (may be null).
     */
    public static String validateOptionalIsbn(String isbn) {
        if (isbn == null) return null;

        String trimmed = isbn.trim();
        if (trimmed.isEmpty()) return null;

        if (!ISBN_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "ISBN must be 10–20 characters and contain only digits, X/x, or hyphens (e.g., 978-1-4028-9462-6)."
            );
        }

        return trimmed;
    }

    // -------------------------------------------------------------------------
    // Publication year (optional)
    // -------------------------------------------------------------------------

    /**
     * Normalizes an optional publication year input:
     * - year <= 0 -> null (matches your CREATE behavior: 0 means not applicable)
     * - otherwise returns year
     */
    public static Integer normalizeOptionalPublicationYear(int yearInput) {
        return (yearInput <= 0) ? null : yearInput;
    }

    /**
     * Validates the optional publication year against the SQL CHECK constraint.
     * Returns the same value if valid (may be null).
     */
    public static Integer validateOptionalPublicationYear(Integer year) {
        if (year == null) return null;

        if (year < 1400 || year > 3000) {
            throw new IllegalArgumentException("Publication year must be between 1400 and 3000 (or omitted).");
        }

        return year;
    }

    /**
     * Normalizes UPDATE flow input for publication year using controller sentinel rules:
     *  -1 => keep current
     *   0 => clear (NULL)
     *  >0 => set to that year (validated separately)
     *
     * @param input        raw int from InputUtil
     * @param currentValue current publication year (may be null)
     * @return normalized year value (may be null), or currentValue if -1
     */
    public static Integer normalizeUpdatePublicationYear(int input, Integer currentValue) {
        if (input == -1) return currentValue;
        if (input == 0) return null;

        if (input < -1) {
            throw new IllegalArgumentException("Use -1 to keep current, 0 to clear, or enter a valid year.");
        }

        return input; // validateOptionalPublicationYear(...) will enforce range
    }

    // -------------------------------------------------------------------------
    // Convenience method (optional)
    // -------------------------------------------------------------------------

    /**
     * Convenience method for "create" flows: validates and returns canonical values.
     */
    public static ValidatedBookFields validateForCreate(String title, String author, String rawIsbn, Integer year) {
        String t = requireValidTitle(title);
        String a = requireValidAuthor(author);

        String normalizedIsbn = normalizeOptionalIsbn(rawIsbn);
        String i = validateOptionalIsbn(normalizedIsbn);

        Integer y = validateOptionalPublicationYear(year);

        return new ValidatedBookFields(t, a, i, y);
    }

    /**
     * Small value object to return validated canonical fields in one shot.
     */
    public record ValidatedBookFields(String title, String author, String isbn, Integer publicationYear) { }

    // -----------------------------
    // Helpers
    // -----------------------------

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

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
