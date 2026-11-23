package com.example.eventassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Detailed view activity displaying comprehensive event information and associated session lists.
 * Provides functionality for editing/deleting the event, filtering sessions by keyword,
 * creating new sessions, and modifying or deleting existing sessions.
 */
public class EventDetailsActivity extends BaseActivity implements SessionAdapter.OnSessionActionListener {

    public static final String EXTRA_EVENT_ID = "event_id";

    private DatabaseHelper dbHelper;
    private SessionAdapter adapter;
    private long eventId;
    private Event event;
    private String currentQuery = "";

    private TextView tvName, tvSessionsTitle, tvNoSessions, tvDay, tvMonth, tvBadge;
    private TextView tvDate, tvLocation, tvOrganiser, tvDescription, tvEmail, tvPhone;
    private View rowLocation, rowOrganiser, rowDescription, rowEmail, rowPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        setupToolbar(R.string.event_details_title);

        dbHelper = new DatabaseHelper(this);
        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1);

        bindViews();

        RecyclerView rvSessions = findViewById(R.id.rvSessions);
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter(this);
        rvSessions.setAdapter(adapter);

        // Configure live session search filter listener
        SearchView searchView = findViewById(R.id.searchSessions);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                loadSessions();
                return true;
            }
        });

        findViewById(R.id.tvAddSession).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditSessionActivity.class);
            intent.putExtra(AddEditSessionActivity.EXTRA_EVENT_ID, eventId);
            startActivity(intent);
        });
        findViewById(R.id.btnEditEvent).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditEventActivity.class);
            intent.putExtra(AddEditEventActivity.EXTRA_EVENT_ID, eventId);
            startActivity(intent);
        });
        findViewById(R.id.btnDeleteEvent).setOnClickListener(
                v -> UiUtils.confirmDeleteEvent(this, dbHelper, event, this::finish));

        // Bottom navigation setup to navigate back to top-level tab screens
        BottomNavigationView nav = findViewById(R.id.navDetails);
        nav.setSelectedItemId(R.id.nav_events);
        nav.setOnItemSelectedListener(item -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TAB, item.getItemId());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        });
    }

    /** Reload data on resume to reflect edits made in child activities. */
    @Override
    protected void onResume() {
        super.onResume();
        if (loadEvent()) {
            loadSessions();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private void bindViews() {
        tvName = findViewById(R.id.tvEventName);
        tvDay = findViewById(R.id.tvEventDay);
        tvMonth = findViewById(R.id.tvEventMonth);
        tvBadge = findViewById(R.id.tvSessionBadge);
        tvDate = findViewById(R.id.tvDate);
        tvLocation = findViewById(R.id.tvLocation);
        tvOrganiser = findViewById(R.id.tvOrganiser);
        tvDescription = findViewById(R.id.tvDescription);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        rowLocation = findViewById(R.id.rowLocation);
        rowOrganiser = findViewById(R.id.rowOrganiser);
        rowDescription = findViewById(R.id.rowDescription);
        rowEmail = findViewById(R.id.rowEmail);
        rowPhone = findViewById(R.id.rowPhone);
        tvSessionsTitle = findViewById(R.id.tvSessionsTitle);
        tvNoSessions = findViewById(R.id.tvNoSessions);
    }

    /** Loads event details from SQLite. Returns false and finishes activity if deleted. */
    private boolean loadEvent() {
        event = dbHelper.getEvent(eventId);
        if (event == null) {
            finish();
            return false;
        }

        tvName.setText(event.getName());
        tvDate.setText(event.getDate());
        tvDay.setText(UiUtils.dayOf(event.getDate()));
        tvMonth.setText(UiUtils.monthOf(event.getDate()));
        tvBadge.setText(getResources().getQuantityString(
                R.plurals.session_count, event.getSessionCount(), event.getSessionCount()));
        bindRow(rowLocation, tvLocation, event.getLocation());
        bindRow(rowOrganiser, tvOrganiser, event.getOrganiser());
        bindRow(rowDescription, tvDescription, event.getDescription());
        bindRow(rowEmail, tvEmail, event.getContactEmail());
        bindRow(rowPhone, tvPhone, event.getContactPhone());

        tvSessionsTitle.setText(getString(R.string.sessions_title, event.getSessionCount()));
        return true;
    }

    /** Displays a row if its value is populated; hides it otherwise. */
    private void bindRow(View row, TextView valueView, String value) {
        if (value == null || value.isEmpty()) {
            row.setVisibility(View.GONE);
        } else {
            row.setVisibility(View.VISIBLE);
            valueView.setText(value);
        }
    }

    private void loadSessions() {
        List<Session> sessions = dbHelper.getSessions(eventId, currentQuery);
        adapter.setSessions(sessions);

        if (sessions.isEmpty()) {
            tvNoSessions.setText(currentQuery.trim().isEmpty()
                    ? R.string.no_sessions : R.string.no_sessions_match);
            tvNoSessions.setVisibility(View.VISIBLE);
        } else {
            tvNoSessions.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------------
    // SessionAdapter.OnSessionActionListener Implementation
    // ---------------------------------------------------------------------

    @Override
    public void onSessionClick(Session session) {
        Intent intent = new Intent(this, AddEditSessionActivity.class);
        intent.putExtra(AddEditSessionActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(AddEditSessionActivity.EXTRA_SESSION_ID, session.getId());
        startActivity(intent);
    }

    @Override
    public void onSessionLongClick(Session session) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_session_title)
                .setMessage(getString(R.string.delete_session_message, session.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteSession(session);
                    if (loadEvent()) {                    // Refresh session list and count badge
                        loadSessions();
                    }
                })
                .show();
    }
}
