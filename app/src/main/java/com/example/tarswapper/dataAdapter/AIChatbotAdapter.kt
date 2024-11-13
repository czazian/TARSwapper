package com.example.tarswapper.dataAdapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tarswapper.R
import com.example.tarswapper.data.Message
import com.example.tarswapper.databinding.ReceiveMessageBinding
import com.example.tarswapper.databinding.SendMessageBinding

class AIChatbotAdapter(
    private val context: Context,
    messages: ArrayList<Message>?,
) : RecyclerView.Adapter<RecyclerView.ViewHolder?>()  {

    //Creating variables to be Used / Assigned later
    private lateinit var messages: ArrayList<Message>

    //Creating indicators
    private val SENT_INDICATOR = 0
    private val RECEIVED_INDICATOR = 1

    //Initialize the variables
    init {
        if (messages != null) {
            this.messages = messages
        }
    }

    //Get Sender Layout Elements
    inner class SendChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //Bind all elements from Sender View Layout
        var binding = SendMessageBinding.bind(itemView)
    }

    //Get Receiver Layout Elements
    inner class ReceiveChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //Bind all elements from Receiver View Layout
        var binding = ReceiveMessageBinding.bind(itemView)
    }

    //Determine for each item, to use which layout (0=Sender Layout, 1=Receiver Layout)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SENT_INDICATOR -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.send_message, parent, false)
                SendChatViewHolder(itemView)
            }

            else -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.receive_message, parent, false)
                ReceiveChatViewHolder(itemView)
            }
        }
    }

    //Override the getItemViewType to make it customized
    override fun getItemViewType(position: Int): Int {
        //Determine the message item from messages list one-by-one
        val chats = messages[position]

        //If the logged-on user is equal to the sender, use the Sender Layout (Current User), else use the Receiver Layout (Opposite User)
        return if (chats.senderID == "User") {
            SENT_INDICATOR
        } else {
            RECEIVED_INDICATOR
        }
    }

    //Get total number of message
    override fun getItemCount(): Int {
        return messages.size
    }

    //Bind item to RecyclerView one-by-one
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //Get the current item (message)
        val currentItem = messages[position]

        //Check the ViewHolder type (SENT or RECEIVED)
        if (holder is SendChatViewHolder) {
            //This is the SendChatViewHolder for the current user
            //Bind the message data to the SendChatViewHolder
            holder.binding.readMsgStateIcon.visibility = View.GONE
            holder.binding.dateTimeMsgOwn.visibility = View.GONE
            holder.binding.msgItemOwn.text = currentItem.message
        } else if (holder is ReceiveChatViewHolder) {
            //This is the ReceiveChatViewHolder for the other user
            //Bind the message data to the ReceiveChatViewHolder
            holder.binding.dateTimeMsg.visibility = View.GONE
            holder.binding.msgItem.text = currentItem.message
        }
    }

}