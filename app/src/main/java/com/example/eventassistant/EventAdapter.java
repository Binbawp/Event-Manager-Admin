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
        // Cloud-off icon = this event (or one of its sessions) has not been uploaded yet
        holder.ivUnsynced.setVisibility(event.isSynced() ? View.GONE : View.VISIBLE);

        // "12/11/2026 · Greenwich Tech Society" (organiser is optional in the display)
        String meta = event.getDate();
        if (event.getOrganiser() != null && !event.getOrganiser().isEmpty()) {
            meta += " \u00B7 " + event.getOrganiser();
        }
        holder.tvMeta.setText(meta);

        int count = event.getSessionCount();
        holder.tvSessions.setText(holder.itemView.getResources()
                .getQuantityString(R.plurals.session_count, count, count));

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

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvMeta = itemView.findViewById(R.id.tvEventMeta);
            tvSessions = itemView.findViewById(R.id.tvSessionCount);
            ivUnsynced = itemView.findViewById(R.id.ivUnsynced);
        }
    }
}
