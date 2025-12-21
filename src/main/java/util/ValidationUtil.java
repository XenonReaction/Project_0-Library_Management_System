package util;

public final class ValidationUtil {

    private ValidationUtil() {}

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    /**
     * Matches your SQL-style constraint: ^[0-9Xx-]{10,20}$
     * Allows digits, X/x, hyphen, 10-20 chars.
     */
    public static void validateOptionalIsbn(String isbn) {
        if (isbn == null) return;

        if (!isbn.matches("^[0-9Xx-]{10,20}$")) {
            throw new IllegalArgumentException("isbn must be 10-20 chars (digits/X/-) or null.");
        }
    }

    public static void validateOptionalPublicationYear(Integer year) {
        if (year == null) return;
        if (year < 1400 || year > 3000) {
            throw new IllegalArgumentException("publicationYear must be between 1400 and 3000 (or null).");
        }
    }
}
