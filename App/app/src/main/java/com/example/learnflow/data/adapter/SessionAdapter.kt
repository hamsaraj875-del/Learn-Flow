
package com.example.learnflow.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnflow.R
import com.example.learnflow.data.model.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val sessions: MutableList<Session>,
    private val onDelete: (Session) -> Unit
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val sessionName: TextView =
            itemView.findViewById(R.id.txtSessionName)

        val sessionDate: TextView =
            itemView.findViewById(R.id.txtSessionDate)

        val sessionDuration: TextView =
            itemView.findViewById(R.id.txtSessionDuration)

        val sessionStatus: TextView =
            itemView.findViewById(R.id.txtSessionStatus)

        val btnDelete: ImageButton =
            itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.session_card, parent, false)

        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SessionViewHolder,
        position: Int
    ) {

        val session = sessions[position]

        holder.sessionName.text = "Study Session"

        val dateFormat = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )

        holder.sessionDate.text =
            dateFormat.format(Date(session.startTime))

        val minutes = session.actualDuration / 60000

        holder.sessionDuration.text =
            "Duration: $minutes min"

        holder.sessionStatus.text = session.status

        holder.btnDelete.setOnClickListener {
            onDelete(session)
        }
    }

    override fun getItemCount(): Int {
        return sessions.size
    }

    // Add a new session
    fun addSession(session: Session) {
        sessions.add(0, session)
        notifyItemInserted(0)
    }

    // Delete a session
    fun removeSession(session: Session) {
        val position = sessions.indexOf(session)

        if (position != -1) {
            sessions.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Update an existing session
    fun updateSession(session: Session) {
        val position = sessions.indexOfFirst {
            it.id == session.id
        }

        if (position != -1) {
            sessions[position] = session
            notifyItemChanged(position)
        }
    }

    // Clear all sessions
    fun clearSessions() {
        val size = sessions.size
        sessions.clear()
        notifyItemRangeRemoved(0, size)
    }
}
