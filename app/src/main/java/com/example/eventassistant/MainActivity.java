package com.example.eventassistant;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Home screen: lists every event, lets the admin search them, delete them (long press)
 * add events (+), open an event's details (tap), run advanced search and (later) upload to the cloud.
 */
public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventActionListener {

    private DatabaseHelper dbHelper;
    private EventAdapter adapter;
    private TextView tvEmpty;
    private String currentQuery = "";

    // Cloud upload (feature e)
    private CloudSyncManager syncManager;
    private LinearProgressIndicator progressSync;
    private FloatingActionButton fabAdd;
    private boolean uploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        dbHelper = new DatabaseHelper(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressSync = findViewById(R.id.progressSync);
        syncManager = new CloudSyncManager(this);

        RecyclerView rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(this);
        rvEvents.setAdapter(adapter);

        // Filter the list live as the user types
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;   // list is already filtered while typing
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                loadEvents();
                return true;
            }
        });

        findViewById(R.id.btnAdvancedSearch).setOnClickListener(
                v -> startActivity(new Intent(this, AdvancedSearchActivity.class)));
        fabAdd = findViewById(R.id.fabAddEvent);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddEditEventActivity.class)));
    }

    /** Reload when returning from other screens so changes appear straight away. */
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
        autoSyncIfNeeded();      // push any changes made on other screens
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    /** Reads events from SQLite (filtered by the search text) and refreshes the list. */
    private void loadEvents() {
        List<Event> events = dbHelper.getEvents(currentQuery);
        adapter.setEvents(events);
        updateSyncStatus();

        if (events.isEmpty()) {
            tvEmpty.setText(currentQuery.trim().isEmpty() ? R.string.empty_events : R.string.empty_search);
            tvEmpty.setVisibility(TextView.VISIBLE);
        } else {
            tvEmpty.setVisibility(TextView.GONE);
        }
    }

    // ---------------------------------------------------------------------
    // Menu
    // ---------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** Shows the current state of the Auto-sync tick box every time the menu opens. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem autoSync = menu.findItem(R.id.action_auto_sync);
        if (autoSync != null) {
            autoSync.setChecked(SyncPrefs.isAutoSyncEnabled(this));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_upload) {
            startSync(true);
            return true;
        } else if (id == R.id.action_auto_sync) {
            boolean enabled = !item.isChecked();
            item.setChecked(enabled);
            SyncPrefs.setAutoSyncEnabled(this, enabled);
            if (enabled) {
                autoSyncIfNeeded();
            }
            return true;
        } else if (id == R.id.action_sample_data) {
            dbHelper.insertSampleData();                 // temporary - remove before submitting
            loadEvents();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------
    // EventAdapter.OnEventActionListener
    // ---------------------------------------------------------------------

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    @Override
    public void onEventLongClick(Event event) {
        // Ask before deleting, and warn that the event's sessions go with it
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, event.getName(), event.getSessionCount()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteEvent(event.getId());
                    loadEvents();
                })
                .show();
    }

    // ---------------------------------------------------------------------
    // Cloud upload (feature e)
    // ---------------------------------------------------------------------

    /**
     * Automatic sync: runs only if the admin switched Auto-sync on, has already done one manual
     * upload, has changes waiting, and the phone is online. Otherwise it does nothing, quietly.
     */
    private void autoSyncIfNeeded() {
        if (SyncPrefs.isAutoSyncEnabled(this)
                && SyncPrefs.hasEverSynced(this)
                && SyncPrefs.hasPendingChanges(this)
                && NetworkUtils.isOnline(this)) {
            startSync(false);
        }
    }

    /**
     * Uploads everything to the cloud on a background thread and shows the result.
     *
     * @param manual true when the admin tapped the upload icon, false for automatic sync
     */
    private void startSync(boolean manual) {
        if (uploading) {
            return;
        }
        uploading = true;
        progressSync.setVisibility(View.VISIBLE);

        syncManager.syncNow(new CloudSyncManager.SyncCallback() {
            @Override
            public void onSuccess(int eventCount, int sessionCount) {
                if (finishSync()) {
                    showSnackbar(manual
                            ? getString(R.string.upload_success, eventCount, sessionCount)
                            : getString(R.string.auto_sync_success));
                }
            }

            @Override
            public void onError(String message) {
                if (finishSync()) {
                    showSnackbar(message);
                }
            }
        });
    }

    /**
     * Hides the progress bar and refreshes the list (the "not uploaded" icons) and status line.
     *
     * @return false if the screen was closed while the upload was running
     */
    private boolean finishSync() {
        uploading = false;
        if (isFinishing() || isDestroyed()) {
            return false;
        }
        progressSync.setVisibility(View.GONE);
        loadEvents();
        return true;
    }

    /** Shows "Not uploaded yet", "Changes not uploaded yet" or the time of the last upload. */
    private void updateSyncStatus() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        if (!SyncPrefs.hasEverSynced(this)) {
            actionBar.setSubtitle(R.string.sync_status_never);
        } else if (SyncPrefs.hasPendingChanges(this)) {
            actionBar.setSubtitle(R.string.sync_status_pending);
        } else {
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(SyncPrefs.getLastSyncTime(this)));
            actionBar.setSubtitle(getString(R.string.sync_status_last, time));
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG)
                .setAnchorView(fabAdd)          // keeps the message above the + button
                .show();
    }

    private void showMessage(@StringRes int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
}