package util.validators;

import java.util.regex.Pattern;

public final class MemberValidator {

    private MemberValidator() {
        // utility class
    }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    // Sentinel used in update prompts to keep existing value
    private static final String KEEP_CURRENT = "-";

    // ---------------------------
    // IDs
    // ---------------------------

    public static long requireValidMemberId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Member ID must be a positive number.");
        }
        return id;
    }

    // ---------------------------
    // Sentinels (controller consistency)
    // ---------------------------

    public static boolean isKeepCurrent(String input) {
        return input != null && input.trim().equals(KEEP_CURRENT);
    }

    // ---------------------------
    // Required fields
    // ---------------------------

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

    public static String normalizeOptionalEmail(String emailInput) {
        return normalizeOptionalText(emailInput);
    }

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

    public static String normalizeOptionalPhone(String phoneInput) {
        return normalizeOptionalText(phoneInput);
    }

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
    // Update helpers (optional, for cleaner controllers)
    // ---------------------------

    public static String resolveUpdatedName(String input, String currentValue) {
        if (isKeepCurrent(input)) return currentValue;
        return requireValidName(input);
    }

    public static String resolveUpdatedEmail(String input, String currentValue) {
        if (isKeepCurrent(input)) return currentValue;
        String normalized = normalizeOptionalEmail(input);   // NONE/blank -> null
        return validateOptionalEmail(normalized);
    }

    public static String resolveUpdatedPhone(String input, String currentValue) {
        if (isKeepCurrent(input)) return currentValue;
        String normalized = normalizeOptionalPhone(input);   // NONE/blank -> null
        return validateOptionalPhone(normalized);
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

    private static String normalizeOptionalText(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.equalsIgnoreCase("NONE")) return null;

        return trimmed;
    }
}
