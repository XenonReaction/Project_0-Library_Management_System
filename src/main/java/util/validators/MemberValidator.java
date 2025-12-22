package util.validators;

import java.util.regex.Pattern;

/**
 * Validates Member-related user input to match database constraints:
 * - name: NOT NULL (and non-blank), VARCHAR(255)
 * - email: nullable, VARCHAR(320), UNIQUE, and if present must match the SQL regex
 * - phone: nullable, VARCHAR(30)
 *
 * This class is designed for inline per-field validation (prompt -> validate -> reprompt).
 */
public final class MemberValidator {

    private MemberValidator() {
        // utility class
    }

    // Matches the SQL CHECK constraint:
    // CHECK (email IS NULL OR email ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$')
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    // ---------------------------
    // Required fields
    // ---------------------------

    /**
     * Ensures name is not null/blank and returns a trimmed version.
     * Enforces DB VARCHAR(255).
     */
    public static String requireValidName(String name) {
        String n = normalizeRequiredText(name, "name");

        if (n.length() > 255) {
            throw new IllegalArgumentException("name must be 255 characters or less.");
        }
        return n;
    }

    // ---------------------------
    // Optional fields (normalize + validate)
    // ---------------------------

    /**
     * Normalizes user input for an optional email field.
     * - null/blank/"NONE" => null
     * - otherwise => trimmed
     */
    public static String normalizeOptionalEmail(String emailInput) {
        return normalizeOptionalText(emailInput);
    }

    /**
     * Validates optional email against DB constraints.
     * - null is allowed
     * - length <= 320
     * - must match SQL email regex when not null
     *
     * Note: uniqueness is enforced by the DB (UNIQUE constraint), not here.
     */
    public static String validateOptionalEmail(String email) {
        if (email == null) return null;

        if (email.length() > 320) {
            throw new IllegalArgumentException("email must be 320 characters or less.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("email must be a valid email address format (example: name@example.com).");
        }
        return email;
    }

    /**
     * Normalizes user input for an optional phone field.
     * - null/blank/"NONE" => null
     * - otherwise => trimmed
     */
    public static String normalizeOptionalPhone(String phoneInput) {
        return normalizeOptionalText(phoneInput);
    }

    /**
     * Validates optional phone against DB constraints.
     * - null is allowed
     * - length <= 30
     * - must not be blank when present
     *
     * (No DB regex/check beyond length, so we keep it simple here.)
     */
    public static String validateOptionalPhone(String phone) {
        if (phone == null) return null;

        if (phone.isBlank()) {
            throw new IllegalArgumentException("phone cannot be blank (or type NONE to leave it empty).");
        }
        if (phone.length() > 30) {
            throw new IllegalArgumentException("phone must be 30 characters or less.");
        }
        return phone;
    }

    // ---------------------------
    // Shared helpers
    // ---------------------------

    private static String normalizeRequiredText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return trimmed;
    }

    /**
     * For optional text fields:
     * - null/blank/"NONE" (case-insensitive) => null
     * - otherwise => trimmed
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.equalsIgnoreCase("NONE")) return null;

        return trimmed;
    }
}
