package com.example.eventassistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.Date;

/**
 * Settings tab fragment providing manual cloud upload trigger and auto-sync toggle
 */
public class SettingsFragment extends Fragment implements MainActivity.Refreshable {

    private TextView tvSyncStatus;
    private SwitchMaterial swAutoSync;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus);
        swAutoSync = view.findViewById(R.id.swAutoSync);

        swAutoSync.setChecked(SyncPrefs.isAutoSyncEnabled(requireContext()));
        swAutoSync.setOnCheckedChangeListener((button, enabled) -> {
            SyncPrefs.setAutoSyncEnabled(requireContext(), enabled);
            if (enabled) {
                main().autoSyncIfNeeded();
            }
        });

        view.findViewById(R.id.btnSync).setOnClickListener(v -> main().startSync(true));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    /** Updates sync status label showing pending state, last sync time, or un-synchronized state. */
    @Override
    public void refresh() {
        if (!isAdded() || getView() == null) {
            return;
        }
        if (!SyncPrefs.hasEverSynced(requireContext())) {
            tvSyncStatus.setText(R.string.sync_status_never);
        } else if (SyncPrefs.hasPendingChanges(requireContext())) {
            tvSyncStatus.setText(R.string.sync_status_pending);
        } else {
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(SyncPrefs.getLastSyncTime(requireContext())));
            tvSyncStatus.setText(getString(R.string.sync_status_last, time));
        }
    }

    private MainActivity main() {
        return (MainActivity) requireActivity();
    }
}
