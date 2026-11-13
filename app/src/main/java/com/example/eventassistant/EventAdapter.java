package com.example.eventassistant;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Binds a list of Event objects to the cards shown on the home screen. */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /** The activity implements this so the adapter stays free of navigation/database code. */
    public interface OnEventActionListener {
        void onEventClick(Event event);
        void onEventLongClick(Event event);
        void onEventEdit(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final OnEventActionListener listener;

    public EventAdapter(OnEventActionListener listener) {
        this.listener = listener;
    }

    /** Replaces the displayed events and refreshes the list. */
    public void setEvents(List<Event> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvName.setText(event.getName());
        holder.tvDay.setText(UiUtils.dayOf(event.getDate()));
        holder.tvMonth.setText(UiUtils.monthOf(event.getDate()));
        // Cloud-off icon = this event (or one of its sessions) has not been uploaded yet
        holder.ivUnsynced.setVisibility(event.isSynced() ? View.GONE : View.VISIBLE);

        boolean hasLocation = event.getLocation() != null && !event.getLocation().isEmpty();
        holder.rowLocation.setVisibility(hasLocation ? View.VISIBLE : View.GONE);
        holder.tvLocation.setText(event.getLocation());

        // Organiser is optional in the display
        boolean hasOrganiser = event.getOrganiser() != null && !event.getOrganiser().isEmpty();
        holder.tvMeta.setVisibility(hasOrganiser ? View.VISIBLE : View.GONE);
        holder.tvMeta.setText(event.getOrganiser());

        int count = event.getSessionCount();
        holder.tvSessions.setText(holder.itemView.getResources()
                .getQuantityString(R.plurals.session_count, count, count));

        holder.btnEdit.setOnClickListener(v -> listener.onEventEdit(event));
        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onEventLongClick(event);
            return true;   // true = long press handled, so it is not also treated as a click
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvMeta;
        final TextView tvSessions;
        final View ivUnsynced;
        final TextView tvDay;
        final TextView tvMonth;
        final TextView tvLocation;
        final View rowLocation;
        final View btnEdit;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvMeta = itemView.findViewById(R.id.tvEventMeta);
            tvSessions = itemView.findViewById(R.id.tvSessionCount);
            ivUnsynced = itemView.findViewById(R.id.ivUnsynced);
            tvDay = itemView.findViewById(R.id.tvEventDay);
            tvMonth = itemView.findViewById(R.id.tvEventMonth);
            tvLocation = itemView.findViewById(R.id.tvEventLocation);
            rowLocation = itemView.findViewById(R.id.rowEventLocation);
            btnEdit = itemView.findViewById(R.id.btnEditEvent);
        }
    }
}
