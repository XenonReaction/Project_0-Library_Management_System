package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ValidationUtil {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtil.class);

    private ValidationUtil() {}

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            log.debug("Validation failed: {} cannot be null.", fieldName);
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.debug("Validation failed: {} is required (blank or null).", fieldName);
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
            // ISBN isn't secret, but avoid logging huge unexpected strings
            String safe = (isbn.length() > 50) ? isbn.substring(0, 50) + "..." : isbn;
            log.debug("Validation failed: isbn format invalid. value='{}'", safe);
            throw new IllegalArgumentException("isbn must be 10-20 chars (digits/X/-) or null.");
        }
    }

    public static void validateOptionalPublicationYear(Integer year) {
        if (year == null) return;

        if (year < 1400 || year > 3000) {
            log.debug("Validation failed: publicationYear out of range. value={}", year);
            throw new IllegalArgumentException("publicationYear must be between 1400 and 3000 (or null).");
        }
    }
}
