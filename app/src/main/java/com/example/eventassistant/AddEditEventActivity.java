package com.example.eventassistant;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity for creating a new event or modifying an existing event record.
 * <p>
 * Workflow sequence:
 * Form input -> Input validation & Duplicate check -> Confirmation summary dialog -> Save to SQLite.
 */
public class AddEditEventActivity extends BaseActivity {

    /** Intent extra key for passing an existing event ID to edit; omitted when creating a new event. */
    public static final String EXTRA_EVENT_ID = "event_id";
    private static final long NO_ID = -1;

    private DatabaseHelper dbHelper;
    private long eventId = NO_ID;

    private TextInputLayout tilName, tilDate, tilLocation, tilOrganiser, tilEmail, tilPhone;
    private TextInputEditText etName, etDate, etLocation, etOrganiser, etDescription, etEmail, etPhone;

    /** Reference to the first invalid input field to request focus upon validation failure. */
    private TextInputEditText firstInvalid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        dbHelper = new DatabaseHelper(this);
        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, NO_ID);
        setupToolbar(eventId == NO_ID ? R.string.add_event_title : R.string.edit_event_title);

        bindViews();

        // Calendar end-icon button launches DatePickerDialog
        tilDate.setEndIconOnClickListener(v -> UiUtils.showDatePicker(this, etDate));

        findViewById(R.id.btnReview).setOnClickListener(v -> onReviewClicked());

        View btnDelete = findViewById(R.id.btnDeleteEvent);
        if (eventId == NO_ID) {
            btnDelete.setVisibility(View.GONE);
            ((Button) findViewById(R.id.btnReview)).setText(R.string.create_event);
        } else {
            btnDelete.setOnClickListener(v ->
                    UiUtils.confirmDeleteEvent(this, dbHelper, dbHelper.getEvent(eventId), this::finish));
        }

        if (eventId != NO_ID) {
            fillForm(dbHelper.getEvent(eventId));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private void bindViews() {
        tilName = findViewById(R.id.tilName);
        tilDate = findViewById(R.id.tilDate);
        tilLocation = findViewById(R.id.tilLocation);
        tilOrganiser = findViewById(R.id.tilOrganiser);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);

        etName = findViewById(R.id.etName);
        etDate = findViewById(R.id.etDate);
        etLocation = findViewById(R.id.etLocation);
        etOrganiser = findViewById(R.id.etOrganiser);
        etDescription = findViewById(R.id.etDescription);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        // Reset error messages on text modification
        clearErrorWhenTyping(etName, tilName);
        clearErrorWhenTyping(etDate, tilDate);
        clearErrorWhenTyping(etLocation, tilLocation);
        clearErrorWhenTyping(etOrganiser, tilOrganiser);
        clearErrorWhenTyping(etEmail, tilEmail);
        clearErrorWhenTyping(etPhone, tilPhone);
    }

    private void clearErrorWhenTyping(TextInputEditText field, TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /** Pre-populates form fields when editing an existing event. */
    private void fillForm(Event event) {
        if (event == null) {          // Finish activity if the target event record no longer exists
            finish();
            return;
        }
        etName.setText(event.getName());
        etDate.setText(event.getDate());
        etLocation.setText(event.getLocation());
        etOrganiser.setText(event.getOrganiser());
        etDescription.setText(event.getDescription());
        etEmail.setText(event.getContactEmail());
        etPhone.setText(event.getContactPhone());
    }

    // ---------------------------------------------------------------------
    // Form Validation and Confirmation Logic
    // ---------------------------------------------------------------------

    private void onReviewClicked() {
        Event event = readForm();

        if (!validate(event)) {
            return;
        }
        // Unique constraint check: duplicate event name and date combination
        if (dbHelper.eventExists(event.getName(), event.getDate(), eventId)) {
            flag(tilName, etName, R.string.error_duplicate_event);
            etName.requestFocus();
            return;
        }
        showConfirmationDialog(event);
    }

    private Event readForm() {
        Event event = new Event();
        event.setId(eventId);
        event.setName(UiUtils.text(etName));
        event.setDate(UiUtils.text(etDate));
        event.setLocation(UiUtils.text(etLocation));
        event.setOrganiser(UiUtils.text(etOrganiser));
        event.setDescription(UiUtils.text(etDescription));
        event.setContactEmail(UiUtils.text(etEmail));
        event.setContactPhone(UiUtils.text(etPhone));
        return event;
    }

    /**
     * Validates required inputs (name, date, location, organiser) and format requirements for date, email, and phone.
     */
    private boolean validate(Event event) {
        firstInvalid = null;

        if (event.getName().isEmpty()) {
            flag(tilName, etName, R.string.error_name_required);
        }
        if (event.getDate().isEmpty()) {
            flag(tilDate, etDate, R.string.error_date_required);
        } else if (!ValidationUtils.isValidDate(event.getDate())) {
            flag(tilDate, etDate, R.string.error_date_format);
        }
        if (event.getLocation().isEmpty()) {
            flag(tilLocation, etLocation, R.string.error_location_required);
        }
        if (event.getOrganiser().isEmpty()) {
            flag(tilOrganiser, etOrganiser, R.string.error_organiser_required);
        }
        // Optional contact fields validation
        if (!event.getContactEmail().isEmpty() && !ValidationUtils.isValidEmail(event.getContactEmail())) {
            flag(tilEmail, etEmail, R.string.error_email_format);
        }
        if (!event.getContactPhone().isEmpty() && !ValidationUtils.isValidPhone(event.getContactPhone())) {
            flag(tilPhone, etPhone, R.string.error_phone_format);
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
            return false;
        }
        return true;
    }

    private void flag(TextInputLayout layout, TextInputEditText field, @StringRes int messageId) {
        layout.setError(getString(messageId));
        if (firstInvalid == null) {
            firstInvalid = field;
        }
    }

    /** Displays a dialog summarizing entered event details prior to database persistence. */
    private void showConfirmationDialog(Event event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_title)
                .setMessage(buildSummary(event))
                .setNegativeButton(R.string.edit, null)          // Dismiss dialog to allow editing
                .setPositiveButton(R.string.confirm, (dialog, which) -> saveEvent(event))
                .show();
    }

    private String buildSummary(Event event) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, R.string.label_name, event.getName());
        appendLine(sb, R.string.label_date, event.getDate());
        appendLine(sb, R.string.label_location, event.getLocation());
        appendLine(sb, R.string.label_organiser, event.getOrganiser());
        appendLine(sb, R.string.label_description, event.getDescription());
        appendLine(sb, R.string.label_email, event.getContactEmail());
        appendLine(sb, R.string.label_phone, event.getContactPhone());
        return sb.toString().trim();
    }

    private void appendLine(StringBuilder sb, @StringRes int labelId, String value) {
        sb.append(getString(labelId)).append(": ")
                .append(value.isEmpty() ? "\u2014" : value)      // Display em dash for empty optional fields
                .append('\n');
    }

    private void saveEvent(Event event) {
        boolean success;
        if (eventId == NO_ID) {
            success = dbHelper.insertEvent(event) != -1;
        } else {
            success = dbHelper.updateEvent(event) > 0;
        }

        if (success) {
            Toast.makeText(this, eventId == NO_ID ? R.string.event_saved : R.string.event_updated,
                    Toast.LENGTH_SHORT).show();
            finish();                                          // Return to caller screen
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
        }
    }
}
