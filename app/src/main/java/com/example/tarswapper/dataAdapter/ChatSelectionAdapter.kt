package com.example.tarswapper.dataAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.ChatSelection
import com.example.tarswapper.R
import com.example.tarswapper.data.ChatRoom
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.User
import com.google.firebase.database.FirebaseDatabase
import com.example.tarswapper.databinding.ContactlistRecycleviewBinding
import com.example.tarswapper.interfaces.OnItemClickListener
import com.example.tarswapper.interfaces.OnUserContactClick
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ChatSelectionAdapter(
    private val context: Context,
    private val listener: OnUserContactClick,
) : RecyclerView.Adapter<ChatSelectionAdapter.ChatSelectionAdapter>() {

    private var chatList = emptyList<ChatRoom>()

    inner class ChatSelectionAdapter(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //Define All Elements From the RecyclerView Layout
        var binding = ContactlistRecycleviewBinding.bind(itemView)

        init {
            binding.contactBox.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(chatList[position])
                }
            }
        }
    }

    //Get the item list need to be bind
    fun setData(chatList: List<ChatRoom>) {
        //Get List of data
        this.chatList = chatList

        //Update the  recyclerview on every list assignment
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSelectionAdapter {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.contactlist_recycleview, parent, false)
        return ChatSelectionAdapter(itemView)
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: ChatSelectionAdapter, position: Int) {
        val currentItem = chatList[position]

        //Bind Last Sent Message
        holder.binding.lastMsg.text = currentItem.lastMsg.toString()

        //Get Current User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Get Each Opposite User ID
        val oppositeUserID = currentItem.oppositeUserID.toString()

        //Get User Info of the Opposite User
        getUserInfo(oppositeUserID) { user ->
            if (user != null) {
                //Bind User Profile Image
                Glide.with(context)
                    .load(user.profileImage.toString())
                    .into(holder.binding.userIcn)

                //Bind UserName
                holder.binding.contactName.text = user.name.toString()
            }
        }

        //Get Room ID
        val roomID = currentItem.roomID.toString()

        //Get Message obj using the Room ID => Get Last Message View Status & The numbers of Message's View Status is false
        getMessageByRoomID(roomID, userID) { status ->
            if (status != null) {
                if (status) {
                    holder.binding.checkMarkIcon.setImageResource(R.drawable.checkyes)
                } else {
                    holder.binding.checkMarkIcon.setImageResource(R.drawable.checkno)
                }
            }
        }

        //Get total number of messages haven't read
        getTotalMessagesNotRead(roomID, userID) { count ->
            Log.e("UserID", "UserID = $userID")
            Log.e("RoomID", "RoomID = $roomID")

            //Show unread message count only if the current user is the recipient
            if (count > 0) {
                holder.binding.msgCount.text = count.toString()
                holder.binding.msgCount.visibility = View.VISIBLE
            } else {
                holder.binding.msgCount.visibility = View.GONE
            }

        }
    }

    //Get Message by Room ID - For total number of messages haven't read
    private fun getTotalMessagesNotRead(roomID: String, userID: String?, onResult: (Int) -> Unit) {
        val messageRef = FirebaseDatabase.getInstance()
            .getReference("Message/$roomID/message")

        messageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unreadCount = 0

                for (messageSnapshot in snapshot.children) {
                    val senderID = messageSnapshot.child("senderID").getValue(String::class.java)
                    val status = messageSnapshot.child("status").getValue(Boolean::class.java)

                    //Count unread messages only if the message is sent by the opposite user
                    if (status == false && senderID != userID) {
                        unreadCount++
                    }
                }
                onResult(unreadCount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching data: ${error.message}")
                onResult(0)
            }
        })
    }

    //Get Message by Room ID - For Status
    private fun getMessageByRoomID(roomID: String, userID: String?, onResult: (Boolean?) -> Unit) {
        val lastMessageRef = FirebaseDatabase.getInstance()
            .getReference("Message/$roomID/message")
            .orderByKey()
            .limitToLast(1)
        lastMessageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val status = messageSnapshot.child("status").getValue(Boolean::class.java)
                    if (status != null) {
                        if (status) {
                            onResult(true)
                        } else {
                            onResult(false)
                        }
                    } else {
                        onResult(null)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching data: ${error.message}")
                onResult(null)
            }
        })
    }


    //Get User info given userID one-by-one
    private fun getUserInfo(oppositeUserID: String, onResult: (User?) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("User")

        databaseReference.child(oppositeUserID).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val oppositeUser = dataSnapshot.getValue(User::class.java)

                onResult(oppositeUser)
            } else {
                Log.e("Error Getting User Info", "Error Getting User Info")
                onResult(null)
            }
        }.addOnFailureListener {
            Log.e("Error Getting User Info", "Error Getting User Info")
            onResult(null)
        }
    }
}
