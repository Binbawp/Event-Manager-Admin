package com.example.eventassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying session items in list views.
 */
public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    /**
     * Listener interface for handling click and long click actions on session items.
     */
    public interface OnSessionActionListener {
        void onSessionClick(Session session);
        void onSessionLongClick(Session session);
    }

    private final List<Session> sessions = new ArrayList<>();
    private final OnSessionActionListener listener;

    public SessionAdapter(OnSessionActionListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the session dataset and refreshes list binding.
     *
     * @param newSessions list of new Session items
     */
    public void setSessions(List<Session> newSessions) {
        sessions.clear();
        sessions.addAll(newSessions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessions.get(position);

        holder.tvName.setText(session.getName());
        holder.tvTime.setText(session.getTime());
        holder.tvType.setText(session.getType());
        holder.tvType.setVisibility(notEmpty(session.getType()) ? View.VISIBLE : View.GONE);

        // Format speaker and room details line
        StringBuilder people = new StringBuilder();
        if (notEmpty(session.getSpeaker())) {
            people.append(holder.itemView.getContext().getString(R.string.speaker_line, session.getSpeaker()));
        }
        if (notEmpty(session.getRoom())) {
            if (people.length() > 0) {
                people.append(" \u00B7 ");
            }
            people.append(holder.itemView.getContext().getString(R.string.room_line, session.getRoom()));
        }
        holder.tvPeople.setText(people);
        holder.tvPeople.setVisibility(people.length() > 0 ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onSessionClick(session));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onSessionLongClick(session);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvTime;
        final TextView tvType;
        final TextView tvPeople;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSessionName);
            tvTime = itemView.findViewById(R.id.tvSessionTime);
            tvType = itemView.findViewById(R.id.tvSessionType);
            tvPeople = itemView.findViewById(R.id.tvSessionPeople);
        }
    }
}
