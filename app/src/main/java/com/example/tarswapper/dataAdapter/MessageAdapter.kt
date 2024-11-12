package com.example.tarswapper.dataAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tarswapper.R
import com.example.tarswapper.data.Items
import com.example.tarswapper.data.Message
import com.example.tarswapper.databinding.NotificationMessageLayoutBinding
import com.example.tarswapper.databinding.NotificationTransactionLayoutBinding
import com.example.tarswapper.interfaces.OnChatHistoryItemClickListener

class MessageAdapter(
    private val context: Context,
    senderRoom: String,
    receiverRoom: String,
    oppositeUserID: String,
    roomID: String,
    private val listener: OnChatHistoryItemClickListener,
) : RecyclerView.Adapter<MessageAdapter.NotificationViewHolder>() {

    //ItemList
    private var messageList = emptyList<Message>()

    //Message Layout
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding = NotificationMessageLayoutBinding.bind(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.notification_message_layout, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    //Get the item list need to be bind
    fun setData(messageList: List<Message>) {
        this.messageList = messageList

        //Notify the adapter to refresh the view
        notifyDataSetChanged()
    }

}