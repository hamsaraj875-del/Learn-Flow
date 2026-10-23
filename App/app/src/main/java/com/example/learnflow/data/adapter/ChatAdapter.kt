package com.example.learnflow.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnflow.R
import com.example.learnflow.data.model.ChatHistory

class ChatAdapter(
    private val chatList: List<ChatHistory>,
    private val onChatClick: (ChatHistory) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView =
            view.findViewById(R.id.txtChatTitle)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_card, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val chat = chatList[position]

        holder.txtMessage.text = chat.title

        holder.itemView.setOnClickListener {
            onChatClick(chat)
        }
    }

    override fun getItemCount(): Int = chatList.size
}