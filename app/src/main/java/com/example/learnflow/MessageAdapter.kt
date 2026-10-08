package com.example.learnflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon

class MessageAdapter(
    private val messages: List<MessageAdapterModel>,
    private val markwon: Markwon
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val USER_MESSAGE = 1
        private const val AI_MESSAGE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) {
            USER_MESSAGE
        } else {
            AI_MESSAGE
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == USER_MESSAGE) {
            val view = inflater.inflate(
                R.layout.message_user,
                parent,
                false
            )
            UserMessageViewHolder(view)
        } else {
            val view = inflater.inflate(
                R.layout.message_ai,
                parent,
                false
            )
            AiMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = messages[position]

        when (holder) {
            is UserMessageViewHolder -> {
                holder.message.text = message.message
            }

            is AiMessageViewHolder -> {
                markwon.setMarkdown(
                    holder.message,
                    message.message
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class UserMessageViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val message: TextView = itemView.findViewById(R.id.txtMessage)
    }

    class AiMessageViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val message: TextView = itemView.findViewById(R.id.txtMessage)
    }
}