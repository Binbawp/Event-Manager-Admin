package com.example.eventassistant;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Form used both to add a session to an event and to edit an existing session (feature b).
 * The session ID is generated automatically by the database and is shown read-only.
 */
public class AddEditSessionActivity extends BaseActivity {

    public static final String EXTRA_EVENT_ID = "event_id";        // required: the parent event
    public static final String EXTRA_SESSION_ID = "session_id";    // only when editing
    private static final long NO_ID = -1;

    private DatabaseHelper dbHelper;
    private long eventId;
    private long sessionId = NO_ID;

    private TextInputLayout tilName, tilTime;
    private TextInputEditText etSessionId, etName, etTime, etSpeaker, etRoom, etDescription;
    private MaterialAutoCompleteTextView actType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_session);

        dbHelper = new DatabaseHelper(this);
        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, NO_ID);
        sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, NO_ID);
        setupToolbar(sessionId == NO_ID ? R.string.add_session_title : R.string.edit_session_title);

        bindViews();

        // Session type is chosen from a list instead of being typed
        String[] types = getResources().getStringArray(R.array.session_types);
        actType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types));
        actType.setText(types[0], false);        // sensible default; false = do not filter the list

        tilTime.setEndIconOnClickListener(v -> UiUtils.showTimePicker(this, etTime));

        findViewById(R.id.btnSave).setOnClickListener(v -> saveSession());

        if (sessionId == NO_ID) {
            etSessionId.setText(R.string.session_id_auto);
        } else {
            fillForm(dbHelper.getSession(sessionId));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private void bindViews() {
        tilName = findViewById(R.id.tilSessionName);
        tilTime = findViewById(R.id.tilSessionTime);
        etSessionId = findViewById(R.id.etSessionId);
        etName = findViewById(R.id.etSessionName);
        etTime = findViewById(R.id.etSessionTime);
        etSpeaker = findViewById(R.id.etSpeaker);
        etRoom = findViewById(R.id.etRoom);
        etDescription = findViewById(R.id.etSessionDescription);
        actType = findViewById(R.id.actType);

        clearErrorWhenTyping(etName, tilName);
        clearErrorWhenTyping(etTime, tilTime);
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

    private void fillForm(Session session) {
        if (session == null) {          // the session was deleted in the meantime
            finish();
            return;
        }
        etSessionId.setText(getString(R.string.session_id_value, session.getId()));
        etName.setText(session.getName());
        etTime.setText(session.getTime());
        etSpeaker.setText(session.getSpeaker());
        etRoom.setText(session.getRoom());
        etDescription.setText(session.getDescription());
        actType.setText(session.getType(), false);
    }

    private void saveSession() {
        String name = UiUtils.text(etName);
        String time = UiUtils.text(etTime);

        // Required fields: session name and time
        TextInputEditText firstInvalid = null;
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_session_name_required));
            firstInvalid = etName;
        }
        if (time.isEmpty()) {
            tilTime.setError(getString(R.string.error_time_required));
            if (firstInvalid == null) firstInvalid = etTime;
        } else if (!ValidationUtils.isValidTime(time)) {
            tilTime.setError(getString(R.string.error_time_format));
            if (firstInvalid == null) firstInvalid = etTime;
        }
        if (firstInvalid != null) {
            firstInvalid.requestFocus();
            return;
        }

        Session session = new Session();
        session.setId(sessionId);
        session.setEventId(eventId);
        session.setName(name);
        session.setTime(time);
        session.setSpeaker(UiUtils.text(etSpeaker));
        session.setRoom(UiUtils.text(etRoom));
        session.setType(UiUtils.text(actType));
        session.setDescription(UiUtils.text(etDescription));

        boolean success;
        if (sessionId == NO_ID) {
            success = dbHelper.insertSession(session) != -1;
        } else {
            success = dbHelper.updateSession(session) > 0;
        }

        if (success) {
            showToast(sessionId == NO_ID ? R.string.session_saved : R.string.session_updated);
            finish();
        } else {
            showToast(R.string.save_failed);
        }
    }

    private void showToast(@StringRes int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
}
