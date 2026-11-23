package com.example.eventassistant;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Common UI helper utilities for user input components, dialog displays, and date/time operations.
 */
public final class UiUtils {

    private UiUtils() { }

    /**
     * Extracts and trims the text contents of a {@link TextView} or {@link android.widget.EditText}.
     *
     * @param view the input field
     * @return non-null trimmed String contents
     */
    public static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    /**
     * Displays a DatePickerDialog and populates the target view with the selected date in DD/MM/YYYY format.
     * If the field already contains a valid date, the picker initializes with that date.
     *
     * @param context the context to display the dialog
     * @param target  the view to receive the formatted date string
     */
    public static void showDatePicker(Context context, TextView target) {
        Calendar cal = Calendar.getInstance();
        String current = text(target);
        if (ValidationUtils.isValidDate(current)) {          // Initialize picker with existing date value
            String[] parts = current.split("/");
            cal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[0]));
        }
        new DatePickerDialog(context,
                (view, year, month, day) -> target.setText(
                        String.format(Locale.UK, "%02d/%02d/%04d", day, month + 1, year)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /**
     * Displays a 24-hour TimePickerDialog and populates the target view with the selected time in HH:mm format.
     *
     * @param context the context to display the dialog
     * @param target  the view to receive the formatted time string
     */
    public static void showTimePicker(Context context, TextView target) {
        Calendar cal = Calendar.getInstance();
        String current = text(target);
        if (ValidationUtils.isValidTime(current)) {
            String[] parts = current.split(":");
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        }
        new TimePickerDialog(context,
                (view, hour, minute) -> target.setText(
                        String.format(Locale.UK, "%02d:%02d", hour, minute)),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                .show();
    }

    /**
     * Extracts the day component from a DD/MM/YYYY date string.
     *
     * @param date the formatted date string
     * @return the day as a string (e.g. "12"), or "--" if invalid
     */
    public static String dayOf(String date) {
        return ValidationUtils.isValidDate(date) ? String.valueOf(Integer.parseInt(date.split("/")[0])) : "--";
    }

    /**
     * Extracts the short uppercase month name from a DD/MM/YYYY date string.
     *
     * @param date the formatted date string
     * @return three-letter uppercase month (e.g. "NOV"), or empty string if invalid
     */
    public static String monthOf(String date) {
        if (!ValidationUtils.isValidDate(date)) {
            return "";
        }
        int month = Integer.parseInt(date.split("/")[1]);
        return new DateFormatSymbols(Locale.ENGLISH).getShortMonths()[month - 1].toUpperCase(Locale.ENGLISH);
    }

    /**
     * Parses a date string in DD/MM/YYYY format into a {@link Date} object.
     *
     * @param date the formatted date string
     * @return parsed Date instance, or null if parsing fails
     */
    public static Date parseDate(String date) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
            f.setLenient(false);
            return f.parse(date);
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }

    /**
     * Displays a confirmation dialog before deleting an event and cascading deletion of its sessions.
     * Runs the provided {@link Runnable} callback upon successful deletion.
     *
     * @param context the context to display the dialog
     * @param db      the DatabaseHelper instance
     * @param event   the event to delete
     * @param after   callback executed after deletion completes
     */
    public static void confirmDeleteEvent(Context context, DatabaseHelper db, Event event, Runnable after) {
        if (event == null) {
            return;
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.delete_title)
                .setMessage(context.getString(R.string.delete_message, event.getName(), event.getSessionCount()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.deleteEvent(event.getId());
                    if (after != null) {
                        after.run();
                    }
                })
                .show();
    }
}
