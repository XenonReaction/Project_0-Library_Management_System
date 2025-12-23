package util.validators;

import java.util.regex.Pattern;

/**
 * Utility class responsible for validating Member-related user input
 * against database constraints and controller-level business rules.
 *
 * <p>This validator is designed for use primarily in controller flows
 * to ensure input is clean, normalized, and safe before being passed
 * to the service layer.</p>
 *
 * <p><strong>Database-aligned constraints (table: {@code members}):</strong></p>
 * <ul>
 *   <li><b>id</b>: BIGINT, must be positive</li>
 *   <li><b>name</b>: NOT NULL, max 255 characters</li>
 *   <li><b>email</b>: nullable, UNIQUE, max 320 characters, valid format</li>
 *   <li><b>phone</b>: nullable, max 30 characters</li>
 * </ul>
 *
 * <p><strong>Controller conventions supported:</strong></p>
 * <ul>
 *   <li>{@code "NONE"} → normalize to {@code null}</li>
 *   <li>{@code "-"} → keep the current value during update flows</li>
 * </ul>
 *
 * <p>This class is stateless and cannot be instantiated.</p>
 */
public final class MemberValidator {

    /**
     * Private constructor to prevent instantiation.
     */
    private MemberValidator() {
        // utility class
    }

    /**
     * Case-insensitive email validation pattern aligned with the DB CHECK constraint.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * Sentinel value used during update flows to indicate
     * that the existing value should be preserved.
     */
    private static final String KEEP_CURRENT = "-";

    // -------------------------------------------------------------------------
    // ID validation
    // -------------------------------------------------------------------------

    /**
     * Ensures a valid member identifier.
     *
     * @param id member identifier
     * @return the same id if valid
     * @throws IllegalArgumentException if id is not positive
     */
    public static long requireValidMemberId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Member ID must be a positive number.");
        }
        return id;
    }

    // -------------------------------------------------------------------------
    // Sentinel helpers (controller consistency)
    // -------------------------------------------------------------------------

    /**
     * Checks whether the input represents the "keep current value" sentinel.
     *
     * @param input raw user input
     * @return {@code true} if input equals {@code "-"} (after trimming)
     */
    public static boolean isKeepCurrent(String input) {
        return input != null && input.trim().equals(KEEP_CURRENT);
    }

    // -------------------------------------------------------------------------
    // Required fields
    // -------------------------------------------------------------------------

    /**
     * Validates and normalizes a required member name.
     *
     * @param name raw name input
     * @return trimmed name
     * @throws IllegalArgumentException if name is null, blank, or too long
     */
    public static String requireValidName(String name) {
        String n = normalizeRequiredText(name, "name");

        if (n.length() > 255) {
            throw new IllegalArgumentException("name must be 255 characters or less.");
        }
        return n;
    }

    // -------------------------------------------------------------------------
    // Optional fields (normalize + validate)
    // -------------------------------------------------------------------------

    /**
     * Normalizes optional email input.
     *
     * <ul>
     *   <li>{@code null}, blank, or {@code "NONE"} → {@code null}</li>
     *   <li>Otherwise returns trimmed value</li>
     * </ul>
     *
     * @param emailInput raw email input
     * @return normalized email or {@code null}
     */
    public static String normalizeOptionalEmail(String emailInput) {
        return normalizeOptionalText(emailInput);
    }

    /**
     * Validates an optional email value.
     *
     * @param email normalized email (nullable)
     * @return the same email if valid, or {@code null}
     * @throws IllegalArgumentException if email exceeds length or format constraints
     */
    public static String validateOptionalEmail(String email) {
        if (email == null) return null;

        if (email.length() > 320) {
            throw new IllegalArgumentException("email must be 320 characters or less.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException(
                    "email must be a valid email address format (example: name@example.com)."
            );
        }
        return email;
    }

    /**
     * Normalizes optional phone input.
     *
     * @param phoneInput raw phone input
     * @return normalized phone or {@code null}
     */
    public static String normalizeOptionalPhone(String phoneInput) {
        return normalizeOptionalText(phoneInput);
    }

    /**
     * Validates an optional phone value.
     *
     * @param phone normalized phone (nullable)
     * @return the same phone if valid, or {@code null}
     * @throws IllegalArgumentException if phone is blank or too long
     */
    public static String validateOptionalPhone(String phone) {
        if (phone == null) return null;

        if (phone.isBlank()) {
            throw new IllegalArgumentException(
                    "phone cannot be blank (or type NONE to leave it empty)."
            );
        }
        if (phone.length() > 30) {
            throw new IllegalArgumentException("phone must be 30 characters or less.");
        }
        return phone;
    }

    // -------------------------------------------------------------------------
    // Update helpers (controller-friendly)
    // -------------------------------------------------------------------------

    /**
     * Resolves a name update using sentinel rules.
     *
     * @param input        raw input
     * @param currentValue existing name
     * @return resolved name
     */
    public static String resolveUpdatedName(String input, String currentValue) {
        if (isKeepCurrent(input)) return currentValue;
        return requireValidName(input);
    }

    /**
     * Resolves an email update using sentinel rules.
     *
     * @param input        raw input
     * @param currentValue existing email
     * @return resolved email (nullable)
     */
    public static String resolveUpdatedEmail(String input, String currentValue) {
        if (isKeepCurrent(input)) return currentValue;
        String normalized = normalizeOptionalEmail(input);   // NONE/blank → null
        return validateOptionalEmail(normalized);
    }

    /**
     * Resolves a phone update using sentinel rules.
     *
     * @param input        raw input
     * @param currentValue existing phone
     * @return resolved phone (nullable)
     */
    public static String resolveUpdatedPhone(String input, String currentValue) {
        if (isKeepCurrent(input)) return currentValue;
        String normalized = normalizeOptionalPhone(input);   // NONE/blank → null
        return validateOptionalPhone(normalized);
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    /**
     * Normalizes and validates required text fields.
     */
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
     * Normalizes optional text fields.
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.equalsIgnoreCase("NONE")) return null;

        return trimmed;
    }
}
