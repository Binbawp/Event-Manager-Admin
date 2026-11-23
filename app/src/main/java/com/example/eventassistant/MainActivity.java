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
 * Main container activity hosting bottom navigation tabs (Dashboard, Events, Settings)
 * and coordinating global cloud synchronization operations.
 */
public class MainActivity extends AppCompatActivity {

    /** Extra parameter indicating the bottom navigation menu ID to select upon startup or launch. */
    public static final String EXTRA_TAB = "tab";

    /**
     * Interface implemented by tab fragments to trigger dataset refresh when cloud sync or data changes occur.
     */
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
        autoSyncIfNeeded();      // Perform background sync check if pending changes exist
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

    /**
     * Programmatically switches the active bottom navigation tab.
     *
     * @param menuId the menu item resource ID of the target tab
     */
    public void selectTab(int menuId) {
        nav.setSelectedItemId(menuId);
    }

    /**
     * Provides access to the application's shared {@link DatabaseHelper} instance.
     *
     * @return the DatabaseHelper instance
     */
    public DatabaseHelper db() {
        return dbHelper;
    }

    /**
     * Returns whether a cloud upload is currently in progress.
     *
     * @return true if uploading; false otherwise
     */
    public boolean isUploading() {
        return uploading;
    }

    // ---------------------------------------------------------------------
    // Cloud Synchronization Operations
    // ---------------------------------------------------------------------

    /**
     * Triggers automatic synchronization if auto-sync is enabled, prior sync history exists,
     * un-uploaded changes are pending, and network connectivity is active.
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
     * Initiates asynchronous database payload upload to the cloud.
     *
     * @param manual true if triggered directly by user interaction; false for background auto-sync
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

    /** Hides progress indicator and refreshes the currently active fragment tab. */
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

    /**
     * Displays a Snackbar message anchored above the bottom navigation bar.
     *
     * @param message text message to display
     */
    public void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG)
                .setAnchorView(nav)         // Position above bottom navigation bar
                .show();
    }
}
