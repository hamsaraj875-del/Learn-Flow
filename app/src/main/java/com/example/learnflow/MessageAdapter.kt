
package com.example.learnflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: List<MessageAdapterModel>
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

        return when (viewType) {
            USER_MESSAGE -> {
                val view = inflater.inflate(
                    R.layout.message_user,
                    parent,
                    false
                )
                UserMessageViewHolder(view)
            }

            else -> {
                val view = inflater.inflate(
                    R.layout.message_ai,
                    parent,
                    false
                )
                AiMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = messages[position]

        when (holder) {
            is UserMessageViewHolder -> {
                holder.bind(message.message)
            }

            is AiMessageViewHolder -> {
                holder.bind(message.message)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserMessageViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val message: TextView =
            itemView.findViewById(R.id.txtMessage)

        fun bind(text: String) {
            message.text = text
        }
    }

    class AiMessageViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val message: WebView =
            itemView.findViewById(R.id.txtMessage)

        private val renderer = MathMessageRenderer(
            itemView.context,
            message
        )

        fun bind(text: String) {
            renderer.render(text)
        }
    }
}