package com.example.eventassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

/**
 * Hosts the three bottom tabs (Dashboard, Events, Settings) and owns the cloud upload,
 * so the cloud button, the Settings button and auto-sync all share the same code.
 */
public class MainActivity extends AppCompatActivity {

    /** Menu id of the tab to open (used when coming back from the event details screen). */
    public static final String EXTRA_TAB = "tab";

    /** Tab fragments implement this so they reload after data changes or an upload. */
    public interface Refreshable {
        void refresh();
    }

    private DatabaseHelper dbHelper;
    private CloudSyncManager syncManager;
    private LinearProgressIndicator progressSync;
    private BottomNavigationView nav;
    private boolean uploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        syncManager = new CloudSyncManager(this);
        progressSync = findViewById(R.id.progressSync);
        nav = findViewById(R.id.nav);

        nav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            int tab = getIntent().getIntExtra(EXTRA_TAB, R.id.nav_dashboard);
            nav.setSelectedItemId(tab);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int tab = intent.getIntExtra(EXTRA_TAB, 0);
        if (tab != 0) {
            nav.setSelectedItemId(tab);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoSyncIfNeeded();      // push any changes made on other screens
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private void showTab(int menuId) {
        Fragment fragment;
        if (menuId == R.id.nav_events) {
            fragment = new EventsFragment();
        } else if (menuId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new DashboardFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    /** Switches tab from code (for example "See all" on the Dashboard). */
    public void selectTab(int menuId) {
        nav.setSelectedItemId(menuId);
    }

    /** Shared database connection for the tab fragments. */
    public DatabaseHelper db() {
        return dbHelper;
    }

    public boolean isUploading() {
        return uploading;
    }

    // ---------------------------------------------------------------------
    // Cloud upload (feature e)
    // ---------------------------------------------------------------------

    /**
     * Automatic sync: runs only if the admin switched Auto-sync on, has already done one manual
     * upload, has changes waiting, and the phone is online. Otherwise it does nothing, quietly.
     */
    public void autoSyncIfNeeded() {
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
     * @param manual true when the admin tapped an upload button, false for automatic sync
     */
    public void startSync(boolean manual) {
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

    /** Hides the progress bar and refreshes the current tab. Returns false if the screen closed. */
    private boolean finishSync() {
        uploading = false;
        if (isFinishing() || isDestroyed()) {
            return false;
        }
        progressSync.setVisibility(View.GONE);
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
        if (current instanceof Refreshable) {
            ((Refreshable) current).refresh();
        }
        return true;
    }

    public void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG)
                .setAnchorView(nav)         // keeps the message above the bottom tabs
                .show();
    }
}
