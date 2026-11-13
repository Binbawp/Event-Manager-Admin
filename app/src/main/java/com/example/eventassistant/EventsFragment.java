package com.example.eventassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Events tab: every event, live search, All / Upcoming / Past filter, advanced search,
 * add (New event), edit (pencil), open details (tap) and delete (long press).
 */
public class EventsFragment extends Fragment
        implements MainActivity.Refreshable, EventAdapter.OnEventActionListener {

    private static final int FILTER_ALL = 0, FILTER_UPCOMING = 1, FILTER_PAST = 2;

    private EventAdapter adapter;
    private TextView tvEmpty, chipAll, chipUpcoming, chipPast;
    private String currentQuery = "";
    private int filterMode = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvEmpty = view.findViewById(R.id.tvEmpty);
        chipAll = view.findViewById(R.id.chipAll);
        chipUpcoming = view.findViewById(R.id.chipUpcoming);
        chipPast = view.findViewById(R.id.chipPast);

        RecyclerView rv = view.findViewById(R.id.rvEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventAdapter(this);
        rv.setAdapter(adapter);

        // Filter the list live as the user types
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;   // list is already filtered while typing
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                refresh();
                return true;
            }
        });

        view.findViewById(R.id.btnCloud).setOnClickListener(v -> main().startSync(true));
        view.findViewById(R.id.btnAdvancedSearch).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), AdvancedSearchActivity.class)));
        view.findViewById(R.id.fabAddEvent).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), AddEditEventActivity.class)));

        chipAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        chipUpcoming.setOnClickListener(v -> setFilter(FILTER_UPCOMING));
        chipPast.setOnClickListener(v -> setFilter(FILTER_PAST));
        paintChips();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void setFilter(int mode) {
        filterMode = mode;
        paintChips();
        refresh();
    }

    private void paintChips() {
        styleChip(chipAll, filterMode == FILTER_ALL);
        styleChip(chipUpcoming, filterMode == FILTER_UPCOMING);
        styleChip(chipPast, filterMode == FILTER_PAST);
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_on : R.drawable.bg_chip);
        chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.dark : R.color.white));
    }

    /** Reads events from SQLite (filtered by search text and chip) and refreshes the list. */
    @Override
    public void refresh() {
        if (!isAdded() || getView() == null) {
            return;
        }
        List<Event> events = applyFilter(main().db().getEvents(currentQuery));
        adapter.setEvents(events);

        if (events.isEmpty()) {
            boolean nothingAtAll = currentQuery.trim().isEmpty() && filterMode == FILTER_ALL;
            tvEmpty.setText(nothingAtAll ? R.string.empty_events : R.string.empty_search);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    /** Keeps only upcoming (today or later) or past events when a chip other than All is chosen. */
    private List<Event> applyFilter(List<Event> events) {
        if (filterMode == FILTER_ALL) {
            return events;
        }
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date today = c.getTime();
        List<Event> out = new ArrayList<>();
        for (Event e : events) {
            Date d = UiUtils.parseDate(e.getDate());
            if (d == null) {
                continue;
            }
            boolean upcoming = !d.before(today);
            if (upcoming == (filterMode == FILTER_UPCOMING)) {
                out.add(e);
            }
        }
        return out;
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
