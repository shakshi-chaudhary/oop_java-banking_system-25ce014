// Lab 4 – Validator (regex-based field validation)
package util;

import java.util.regex.Pattern;

/**
 * Utility class for validating common input fields using regular expressions.
 * Lab 4: demonstrates Pattern/Matcher and static utility methods.
 */
public final class Validator {

    // ── Patterns ─────────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$");   // Indian 10-digit mobile

    private static final Pattern PINCODE_PATTERN =
            Pattern.compile("^[1-9]\\d{5}$");    // Indian 6-digit PIN

    private static final Pattern IFSC_PATTERN =
            Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    private static final Pattern POSITIVE_NUMBER_PATTERN =
            Pattern.compile("^\\d+(\\.\\d+)?$");

    private Validator() {} // utility class – no instantiation

    // ── Public validators ─────────────────────────────────────────────────────
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.strip()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.strip()).matches();
    }

    public static boolean isValidPincode(String pin) {
        return pin != null && PINCODE_PATTERN.matcher(pin.strip()).matches();
    }

    public static boolean isValidIfsc(String ifsc) {
        return ifsc != null && IFSC_PATTERN.matcher(ifsc.strip()).matches();
    }

    /** Returns true if the string parses to a strictly positive number. */
    public static boolean isPositiveNumber(String s) {
        if (s == null || s.isBlank()) return false;
        if (!POSITIVE_NUMBER_PATTERN.matcher(s.strip()).matches()) return false;
        return Double.parseDouble(s.strip()) > 0;
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    public static boolean isMinLength(String s, int min) {
        return s != null && s.length() >= min;
    }

    public static boolean isMaxLength(String s, int max) {
        return s != null && s.length() <= max;
    }
}
