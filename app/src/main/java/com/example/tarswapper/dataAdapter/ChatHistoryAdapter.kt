package com.example.tarswapper.dataAdapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.data.Message
import com.example.tarswapper.databinding.ReceiveMessageBinding
import com.example.tarswapper.databinding.SendMessageBinding
import com.example.tarswapper.interfaces.OnChatHistoryItemClickListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatHistoryAdapter (
    private val context: Context,
    messages: ArrayList<Message>?,
    senderRoom: String,
    receiverRoom: String,
    oppositeUserID: String,
    roomID: String,
    private val listener: OnChatHistoryItemClickListener,
): RecyclerView.Adapter<RecyclerView.ViewHolder?>() {

    private var messageList = emptyList<Message>()

    //Creating variables to be Used / Assigned later
    private lateinit var messages: ArrayList<Message>
    private val senderRoom: String
    private val receiverRoom: String

    //Creating indicators
    private val SENT_INDICATOR = 0
    private val RECEIVED_INDICATOR = 1

    //Get From Chat
    private val oppositeUserID: String
    private val roomID: String

    //Initialize the variables
    init {
        if (messages != null) {
            this.messages = messages
        }
        this.senderRoom = senderRoom
        this.receiverRoom = receiverRoom
        this.oppositeUserID = oppositeUserID
        this.roomID = roomID
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
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Determine the message item from messages list one-by-one
        val chats = messages[position]

        //If the logged-on user is equal to the sender, use the Sender Layout (Current User), else use the Receiver Layout (Opposite User)
        return if (userID == chats.senderID) {
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
        //Get item one-by-one
        val currentItem = messages[position]

        ////Determined by getItemViewType & onCreateViewHolder method////
        //If the item uses SendChatViewHolder => Means it is the Current User
        if (holder.javaClass == SendChatViewHolder::class.java) {
            val viewHolder = holder as ChatHistoryAdapter.SendChatViewHolder

            //Bind the View Status - Only Sender Side needs to show view status
            if (currentItem.status!!) {
                viewHolder.binding.readMsgStateIcon.setImageResource(R.drawable.checkyes)
            } else {
                viewHolder.binding.readMsgStateIcon.setImageResource(R.drawable.checkno)
            }

            //Bind the Message Date Time
            viewHolder.binding.dateTimeMsgOwn.text = currentItem.dateTime

            //Bind the Message Text
            viewHolder.binding.msgItemOwn.text = currentItem.message

            //Bind the Image/Video (IF EXIST)
            bindImageVideoSender(viewHolder, currentItem) { }

            //Check weather the message is undone message. If yes, set it to ITALIC Style
            if (currentItem.message == "This message has been withdrawn") {
                viewHolder.binding.msgItemOwn.setTypeface(null, Typeface.ITALIC)
                viewHolder.binding.msgItemOwn.setTextColor(Color.LTGRAY)
            } else {
                viewHolder.binding.msgItemOwn.setTypeface(null, Typeface.NORMAL)
                viewHolder.binding.msgItemOwn.setTextColor(Color.WHITE)
            }

            //Set click listener
            viewHolder.itemView.setOnClickListener {
                listener.onItemClick(messages[position].messageID, oppositeUserID, roomID)
            }

        } else {
            //Else the item uses ReceiveChatViewHolder => Means it is the Opposite User
            val viewHolder = holder as ChatHistoryAdapter.ReceiveChatViewHolder

            //Bind the Image/Video (IF EXIST)
            bindImageVideoReceiver(viewHolder, currentItem) { }

            //Bind the Message Date Time
            viewHolder.binding.dateTimeMsg.text = currentItem.dateTime

            //Bind the Message Text
            viewHolder.binding.msgItem.text = currentItem.message

            //Check weather the message is undone message. If yes, set it to ITALIC Style
            if (currentItem.message == "This message has been withdrawn") {
                viewHolder.binding.msgItem.setTypeface(null, Typeface.ITALIC)
                viewHolder.binding.msgItem.setTextColor(Color.LTGRAY)
            } else {
                viewHolder.binding.msgItem.setTypeface(null, Typeface.NORMAL)
                viewHolder.binding.msgItem.setTextColor(Color.BLACK)
            }

            //Set click listener
            viewHolder.itemView.setOnClickListener {
                listener.onItemClick(messages[position].messageID, oppositeUserID, roomID)
            }
        }
    }

    private fun bindImageVideoReceiver(
        viewHolder: ChatHistoryAdapter.ReceiveChatViewHolder,
        currentItem: Message,
        onResult: (Boolean) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance().getReference("Message")

        //Retrieve data from Firebase using the senderRoom and messageID
        database.child(receiverRoom).child("message").child(currentItem.messageID!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Check if the message exists
                    if (snapshot.exists()) {
                        val message = snapshot.getValue(Message::class.java)
                        val media = message?.media
                        val mediaType = message?.mediaType

                        if (media != null) {
                            //Check if media is an image or video based on file extension or MIME type
                            if (mediaType == "image") {
                                //Display image
                                viewHolder.binding.messageImage.visibility = View.VISIBLE
                                viewHolder.binding.messageVideo.visibility = View.GONE

                                Glide.with(context)
                                    .load(media)
                                    .into(viewHolder.binding.messageImage)


                                onResult(true)
                            } else if (mediaType == "video") {
                                //Display video
                                viewHolder.binding.messageVideo.visibility = View.VISIBLE
                                viewHolder.binding.messageImage.visibility = View.GONE

                                //Load video URI into VideoView
                                viewHolder.binding.messageVideo.setVideoURI(Uri.parse(media))
                                viewHolder.binding.messageVideo.setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    viewHolder.binding.messageVideo.start()
                                }


                                onResult(true)
                            } else {
                                //Hide both if media type is unsupported
                                hideMedia(viewHolder)
                                onResult(false)
                            }
                        } else {
                            //Hide both if media is null
                            hideMedia(viewHolder)
                            onResult(false)
                        }
                    } else {
                        //Hide both if snapshot does not exist
                        hideMedia(viewHolder)
                        onResult(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Getting Media", "Error Getting Media: ${error.message}")
                    hideMedia(viewHolder)
                    onResult(false)
                }
            })
    }


    //Bind Image/Video for Sender
    private fun bindImageVideoSender(
        viewHolder: ChatHistoryAdapter.SendChatViewHolder,
        currentItem: Message,
        onResult: (Boolean) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance().getReference("Message")

        //Retrieve data from Firebase using the senderRoom and messageID
        database.child(senderRoom).child("message").child(currentItem.messageID!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Check if the message exists
                    if (snapshot.exists()) {
                        val message = snapshot.getValue(Message::class.java)
                        val media = message?.media
                        val mediaType = message?.mediaType

                        if (media != null) {
                            //Check if media is an image or video based on file extension or MIME type
                            if (mediaType == "image") {
                                //Display image
                                viewHolder.binding.ownMessageImage.visibility = View.VISIBLE
                                viewHolder.binding.ownMessageVideo.visibility = View.GONE

                                Glide.with(context)
                                    .load(media)
                                    .into(viewHolder.binding.ownMessageImage)

                                onResult(true)
                            } else if (mediaType == "video") {
                                //Display video

                                viewHolder.binding.ownMessageImage.visibility = View.GONE
                                viewHolder.binding.ownMessageVideo.visibility = View.VISIBLE

                                //Load video URI into VideoView
                                viewHolder.binding.ownMessageVideo.setVideoURI(Uri.parse(media))
                                viewHolder.binding.ownMessageVideo.setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    viewHolder.binding.ownMessageVideo.start()
                                }


                                onResult(true)
                            } else {
                                //Hide both if media type is unsupported
                                hideMedia(viewHolder)
                                onResult(false)
                            }
                        } else {
                            //Hide both if media is null
                            hideMedia(viewHolder)
                            onResult(false)
                        }
                    } else {
                        //Hide both if snapshot does not exist
                        hideMedia(viewHolder)
                        onResult(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Getting Media", "Error Getting Media: ${error.message}")
                    hideMedia(viewHolder)
                    onResult(false)
                }
            })
    }


    //Helper function to hide media views - for Sender
    private fun hideMedia(viewHolder: ChatHistoryAdapter.SendChatViewHolder) {
        viewHolder.binding.ownMessageImage.visibility = View.GONE
        viewHolder.binding.ownMessageVideo.visibility = View.GONE
    }

    //Helper function to hide media views - for Receiver
    private fun hideMedia(viewHolder: ChatHistoryAdapter.ReceiveChatViewHolder) {
        viewHolder.binding.messageImage.visibility = View.GONE
        viewHolder.binding.messageVideo.visibility = View.GONE
    }

}