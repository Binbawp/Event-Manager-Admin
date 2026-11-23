package com.example.eventassistant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced search activity allowing multi-criteria filtering of events by date and organiser.
 * Tapping a result navigates to the detailed view for that event.
 */
public class AdvancedSearchActivity extends BaseActivity implements EventAdapter.OnEventActionListener {

    private DatabaseHelper dbHelper;
    private EventAdapter adapter;
    private TextInputLayout tilDate;
    private TextInputEditText etDate, etOrganiser;
    private TextView tvResultsInfo;

    /** Tracks whether a query has been executed to allow auto-refreshing search results upon resume. */
    private boolean hasSearched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_search);
        setupToolbar(R.string.advanced_search_title);

        dbHelper = new DatabaseHelper(this);

        tilDate = findViewById(R.id.tilSearchDate);
        etDate = findViewById(R.id.etSearchDate);
        etOrganiser = findViewById(R.id.etSearchOrganiser);
        tvResultsInfo = findViewById(R.id.tvResultsInfo);

        tilDate.setEndIconOnClickListener(v -> UiUtils.showDatePicker(this, etDate));

        RecyclerView rvResults = findViewById(R.id.rvResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(this);
        rvResults.setAdapter(adapter);

        findViewById(R.id.btnSearch).setOnClickListener(v -> runSearch());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasSearched) {
            runSearch();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private void runSearch() {
        tilDate.setError(null);
        String date = UiUtils.text(etDate);
        String organiser = UiUtils.text(etOrganiser);

        if (date.isEmpty() && organiser.isEmpty()) {
            adapter.setEvents(new ArrayList<>());
            tvResultsInfo.setText(R.string.enter_criteria);
            return;
        }
        if (!date.isEmpty() && !ValidationUtils.isValidDate(date)) {
            tilDate.setError(getString(R.string.error_date_format));
            return;
        }

        hasSearched = true;
        List<Event> results = dbHelper.searchEvents(date, organiser);
        adapter.setEvents(results);

        if (results.isEmpty()) {
            tvResultsInfo.setText(R.string.no_results);
        } else {
            tvResultsInfo.setText(getResources().getQuantityString(
                    R.plurals.results_count, results.size(), results.size()));
        }
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    @Override
    public void onEventLongClick(Event event) {
        // Deletion operations are available from home or event detail screens
    }

    @Override
    public void onEventEdit(Event event) {
        Intent intent = new Intent(this, AddEditEventActivity.class);
        intent.putExtra(AddEditEventActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }
}
