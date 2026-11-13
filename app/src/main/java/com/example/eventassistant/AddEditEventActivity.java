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
 * Form used both to add a new event and to edit an existing one (feature a).
 * Flow: fill the form -> Review (validation + duplicate check) -> confirmation dialog
 * showing every detail -> Confirm saves, Edit returns to the form with the data kept.
 */
public class AddEditEventActivity extends BaseActivity {

    /** Pass the id of an existing event to edit it; leave out to add a new one. */
    public static final String EXTRA_EVENT_ID = "event_id";
    private static final long NO_ID = -1;

    private DatabaseHelper dbHelper;
    private long eventId = NO_ID;

    private TextInputLayout tilName, tilDate, tilLocation, tilOrganiser, tilEmail, tilPhone;
    private TextInputEditText etName, etDate, etLocation, etOrganiser, etDescription, etEmail, etPhone;

    /** The first field that failed validation; it receives focus so the user sees the error. */
    private TextInputEditText firstInvalid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        dbHelper = new DatabaseHelper(this);
        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, NO_ID);
        setupToolbar(eventId == NO_ID ? R.string.add_event_title : R.string.edit_event_title);

        bindViews();

        // The calendar icon opens a date picker; the date can also be typed and is validated
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

        // An error message disappears as soon as the user starts correcting the field
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

    /** Pre-fills the form when editing. */
    private void fillForm(Event event) {
        if (event == null) {          // the event was deleted in the meantime
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
    // Validate -> confirm -> save
    // ---------------------------------------------------------------------

    private void onReviewClicked() {
        Event event = readForm();

        if (!validate(event)) {
            return;
        }
        // Rule: no two events may have the same name and date
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
     * Checks required fields (name, date, location, organiser) and the format of the date
     * and of the optional email/phone. Every problem is shown at once.
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
        // Optional fields: only checked when something was entered
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

    /** Shows the accepted details back to the user: Confirm saves, Edit goes back to the form. */
    private void showConfirmationDialog(Event event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_title)
                .setMessage(buildSummary(event))
                .setNegativeButton(R.string.edit, null)          // just closes the dialog
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
                .append(value.isEmpty() ? "\u2014" : value)      // em dash for an empty optional field
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
            finish();                                          // back to the previous screen
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
        }
    }
}
