package com.example.eventassistant;

import android.util.Patterns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class providing validation methods for user input across application forms.
 */
public final class ValidationUtils {

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{7,15}$");

    private ValidationUtils() { }

    /**
     * Validates whether a date string represents a valid calendar date in DD/MM/YYYY format.
     *
     * @param date the date string to validate
     * @return true if the format is correct and represents a valid calendar date; false otherwise
     */
    public static boolean isValidDate(String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) {
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        format.setLenient(false);   // Strict parsing to prevent invalid calendar dates (e.g., 31/02/2026)
        try {
            format.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Validates whether a time string matches the 24-hour time format (HH:mm).
     *
     * @param time the time string to validate
     * @return true if the format is valid (00:00 to 23:59); false otherwise
     */
    public static boolean isValidTime(String time) {
        return time != null && TIME_PATTERN.matcher(time).matches();
    }

    /**
     * Validates whether an email string matches the standard email format.
     *
     * @param email the email string to validate
     * @return true if valid; false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validates whether a phone number contains between 7 and 15 digits, allowing an optional leading '+'.
     * Spaces, hyphens, and parentheses are stripped before validation.
     *
     * @param phone the phone string to validate
     * @return true if valid; false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        String digitsOnly = phone.replaceAll("[\\s\\-()]", "");
        return PHONE_PATTERN.matcher(digitsOnly).matches();
    }
}
