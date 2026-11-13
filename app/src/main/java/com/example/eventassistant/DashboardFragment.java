package com.example.eventassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Dashboard tab: totals, quick actions and the next few upcoming events. */
public class DashboardFragment extends Fragment
        implements MainActivity.Refreshable, EventAdapter.OnEventActionListener {

    private static final int UPCOMING_LIMIT = 3;

    private EventAdapter adapter;
    private TextView tvStatEvents, tvStatSessions, tvStatNeed, tvEmpty;
    private List<Event> allEvents = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvStatEvents = view.findViewById(R.id.tvStatEvents);
        tvStatSessions = view.findViewById(R.id.tvStatSessions);
        tvStatNeed = view.findViewById(R.id.tvStatNeed);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        RecyclerView rv = view.findViewById(R.id.rvUpcoming);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventAdapter(this);
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnCloud).setOnClickListener(v -> main().startSync(true));
        view.findViewById(R.id.btnNewEvent).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), AddEditEventActivity.class)));
        view.findViewById(R.id.btnAddSession).setOnClickListener(v -> pickEventForSession());
        view.findViewById(R.id.tvSeeAll).setOnClickListener(v -> main().selectTab(R.id.nav_events));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        if (!isAdded() || getView() == null) {
            return;
        }
        allEvents = main().db().getEvents("");

        int sessions = 0;
        int need = 0;
        List<Event> upcoming = new ArrayList<>();
        Date today = startOfToday();
        for (Event e : allEvents) {
            sessions += e.getSessionCount();
            if (e.getSessionCount() == 0) {
                need++;
            }
            Date d = UiUtils.parseDate(e.getDate());
            if (d != null && !d.before(today) && upcoming.size() < UPCOMING_LIMIT) {
                upcoming.add(e);
            }
        }
        tvStatEvents.setText(String.valueOf(allEvents.size()));
        tvStatSessions.setText(String.valueOf(sessions));
        tvStatNeed.setText(String.valueOf(need));
        adapter.setEvents(upcoming);
        tvEmpty.setVisibility(allEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static Date startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** "+ Add session": choose which event the new session belongs to, then open the form. */
    private void pickEventForSession() {
        if (allEvents.isEmpty()) {
            Toast.makeText(requireContext(), R.string.create_event_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[allEvents.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = allEvents.get(i).getName();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_session_to)
                .setItems(names, (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), AddEditSessionActivity.class);
                    intent.putExtra(AddEditSessionActivity.EXTRA_EVENT_ID, allEvents.get(which).getId());
                    startActivity(intent);
                })
                .show();
    }

    private MainActivity main() {
        return (MainActivity) requireActivity();
    }

    // ---- EventAdapter.OnEventActionListener ----

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    @Override
    public void onEventEdit(Event event) {
        Intent intent = new Intent(requireContext(), AddEditEventActivity.class);
        intent.putExtra(AddEditEventActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    @Override
    public void onEventLongClick(Event event) {
        UiUtils.confirmDeleteEvent(requireContext(), main().db(), event, this::refresh);
    }
}
