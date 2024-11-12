package com.example.tarswapper.dataAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.data.ChatRoom
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.NotificationMessageLayoutBinding
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Locale


class MessageAdapter(
    private val context: Context,
    private val currentUserID: String,
    private val onMessageClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var chatRooms = mapOf<String, List<Message>>()
    private var allMessages = listOf<Message>()

    fun setData(chatRooms: Map<String, List<Message>>) {
        this.chatRooms = chatRooms
        //Flatten the messages after sorting
        allMessages = chatRooms.values.flatten().sortedByDescending { message ->
            try {
                val dateFormat = SimpleDateFormat("HH:mm • dd/M/yy", Locale.getDefault())
                dateFormat.parse(message.dateTime)?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Log.e("DateParseError", "Could not parse date: ${message.dateTime}")
                Long.MAX_VALUE
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = NotificationMessageLayoutBinding.inflate(LayoutInflater.from(context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        //Use the sorted flat list of all messages
        val message = allMessages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int {
        return allMessages.size
    }

    inner class MessageViewHolder(private val binding: NotificationMessageLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            getOppositeUserInfo(message.senderID) {
                if (it != null) {
                    binding.messageText.text = it.name.toString()
                    Glide.with(context)
                        .load(it.profileImage)
                        .into(binding.usersIcon)
                }
            }
            binding.dateTimeText.text = message.dateTime

            binding.root.setOnClickListener {
                onMessageClick(message)
            }
        }
    }

    private fun getOppositeUserInfo(senderID: String?, callback: (User?) -> Unit) {
        val database = Firebase.database
        val usersRef = database.getReference("User")

        usersRef.child(senderID.toString()).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                callback(user)
            }
            .addOnFailureListener { error ->
                Log.e("DB Error", "Error getting user: ${error.message}")
                callback(null)
            }
    }
}


