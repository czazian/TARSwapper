package com.example.tarswapper.dataAdapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.data.Message
import com.example.tarswapper.databinding.HideTranslateMessageBinding
import com.example.tarswapper.databinding.ReceiveMessageBinding
import com.example.tarswapper.databinding.SendMessageBinding
import com.example.tarswapper.databinding.TranslateMessageBinding
import com.example.tarswapper.databinding.UndoMessageBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    //Get All Values when Creating + Assigning this Adapter
    private val context: Context,
    messages: ArrayList<Message>?,
    senderRoom: String,
    receiverRoom: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {

    /*
    * Sender = Me
    * Receiver = Opposite User
    * */

    //Creating variables to be Used / Assigned later
    private lateinit var messages: ArrayList<Message>
    private val senderRoom: String
    private val receiverRoom: String

    //Creating indicators
    private val SENT_INDICATOR = 0
    private val RECEIVED_INDICATOR = 1

    //Initialize the variables
    init {
        if (messages != null) {
            this.messages = messages
        }
        this.senderRoom = senderRoom
        this.receiverRoom = receiverRoom
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
            val viewHolder = holder as SendChatViewHolder

            //
            if (currentItem.message.equals("photo")) {
                viewHolder.binding.msgItemOwn.visibility = View.GONE
                viewHolder.binding.ownMessageImage.visibility = View.VISIBLE
                Glide.with(context)
                    .load(currentItem.media)
                    .into(viewHolder.binding.ownMessageImage)
            }

            ////Update Message View Status////
            updateMessageReadStatus(currentItem)

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

            //Check weather the message is undone message. If yes, set it to ITALIC Style
            if (currentItem.message == "This message has been withdrawn") {
                viewHolder.binding.msgItemOwn.setTypeface(null, Typeface.ITALIC)
                viewHolder.binding.msgItemOwn.setTextColor(Color.LTGRAY)
            } else {
                viewHolder.binding.msgItemOwn.setTypeface(null, Typeface.NORMAL)
                viewHolder.binding.msgItemOwn.setTextColor(Color.WHITE)
            }

            //When the Message Box is Long Clicked => Pop up the selection box
            viewHolder.itemView.setOnLongClickListener() {
                //Check if it is undone message. If yes, don't allow long click
                if (currentItem.message == "This message has been withdrawn") {
                    return@setOnLongClickListener true
                }

                val optionView = LayoutInflater.from(context).inflate(R.layout.undo_message, null)
                val binding = UndoMessageBinding.bind(optionView)
                val optionDialog = AlertDialog
                    .Builder(context)
                    .setView(binding.root)
                    .create()
                optionDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded)

                //When the Undo Message Button is selected - Deny when the message is sent more than 5 minutes
                binding.undoMessage.setOnClickListener() {

                    checkUndoValidity(currentItem.dateTime.toString()) {
                        if (!it) {
                            //Update the undone Message to "This message has been withdrawn." for both Room (Sender & Receiver) for consistency
                            currentItem.message = "This message has been withdrawn"

                            //Update for Sender Room
                            currentItem.messageID?.let { itOne ->
                                FirebaseDatabase.getInstance().reference.child("Message")
                                    .child(senderRoom)
                                    .child("message")
                                    .child(itOne)
                                    .setValue(currentItem)
                            }

                            //Update for Receiver Room
                            currentItem.messageID?.let { itTwo ->
                                FirebaseDatabase.getInstance().reference.child("Message")
                                    .child(receiverRoom)
                                    .child("message")
                                    .child(itTwo)
                                    .setValue(currentItem)
                            }

                            optionDialog.dismiss()
                        } else {
                            AlertDialog
                                .Builder(context)
                                .setTitle("Error")
                                .setMessage("Message cannot be undone after 5 minutes")
                                .setPositiveButton("OK") { dialog, _ ->
                                    optionDialog.dismiss()
                                }
                                .create()
                                .show()
                        }
                    }

                }

                //When the Cancel Button is selected
                binding.undoCancel.setOnClickListener() {
                    optionDialog.dismiss()
                }

                optionDialog.show()
                false
            }

        } else {
            //Else the item uses ReceiveChatViewHolder => Means it is the Opposite User
            val viewHolder = holder as ReceiveChatViewHolder

            //
            if (currentItem.message.equals("photo")) {
                viewHolder.binding.msgItem.visibility = View.GONE
                viewHolder.binding.messageImage.visibility = View.VISIBLE
                Glide.with(context)
                    .load(currentItem.media)
                    .into(viewHolder.binding.messageImage)
            }

            ////Update Message View Status////
            updateMessageReadStatus(currentItem)


            ////Bind the translated text if got////
            var hasTranslation = false
            bindTranslatedText(viewHolder, currentItem) {
                hasTranslation = it
            }

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

            //When the Message Box is Long Clicked => Pop up the selection box
            viewHolder.itemView.setOnLongClickListener() {
                //Check if it is undone message. If yes, don't allow long click
                if (currentItem.message == "This message has been withdrawn") {
                    return@setOnLongClickListener true
                }

                if(!hasTranslation){
                    val optionView =
                        LayoutInflater.from(context).inflate(R.layout.translate_message, null)
                    val binding = TranslateMessageBinding.bind(optionView)
                    val optionDialog = AlertDialog
                        .Builder(context)
                        .setView(binding.root)
                        .create()
                    optionDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded)

                    //When the Translate Message Button is selected
                    binding.translateMessage.setOnClickListener() {
                        //Identify and Translate the Message
                        identifyLanguage(currentItem.message.toString(), currentItem)

                        optionDialog.dismiss()
                    }

                    //When the Cancel Button is selected
                    binding.translateCancel.setOnClickListener() {
                        optionDialog.dismiss()
                    }

                    optionDialog.show()
                    false

                } else {
                    val optionView =
                        LayoutInflater.from(context).inflate(R.layout.hide_translate_message, null)
                    val binding = HideTranslateMessageBinding.bind(optionView)
                    val optionDialog = AlertDialog
                        .Builder(context)
                        .setView(binding.root)
                        .create()
                    optionDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded)


                    //When the Hide Translated Message Button is selected
                    binding.hideTranslate.setOnClickListener() {
                        //Identify and Translate the Message
                        removeTranslation(currentItem)

                        optionDialog.dismiss()
                    }


                    //When the Cancel Button is selected
                    binding.translateCancel.setOnClickListener() {
                        optionDialog.dismiss()
                    }

                    optionDialog.show()
                    false
                }
            }
        }
    }

    //When Hide Translate Button is clicked
    private fun removeTranslation(currentItem: Message) {
        //Remove the translated text from the Firebase
        //Update for Sender Room - itOne = messageID
        currentItem.messageID?.let { itOne ->
            FirebaseDatabase.getInstance().reference
                .child("Message")
                .child(senderRoom)
                .child("message")
                .child(itOne)
                .child("translatedText")
                .removeValue()
        }

        //Update for Receiver Room
        currentItem.messageID?.let { itTwo ->
            FirebaseDatabase.getInstance().reference
                .child("Message")
                .child(receiverRoom)
                .child("message")
                .child(itTwo)
                .child("translatedText")
                .removeValue()
        }
    }

    private fun bindTranslatedText(viewHolder: ChatAdapter.ReceiveChatViewHolder, currentItem: Message, onResult: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("Message")

        //Retrieve data from Firebase using the receiverRoom and messageID
        database.child(receiverRoom).child("message").child(currentItem.messageID!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Check if the message exists
                if (snapshot.exists()) {
                    val message = snapshot.getValue(Message::class.java)
                    val translatedText = message?.translatedText

                    //Update UI based on the availability of translatedText
                    if (translatedText != null) {
                        viewHolder.binding.view.visibility = View.VISIBLE
                        viewHolder.binding.translatedText.visibility = View.VISIBLE
                        viewHolder.binding.translatedText.text = "Translated: $translatedText"

                        onResult(true)
                    } else {
                        //Hide the translated text view if not available
                        viewHolder.binding.view.visibility = View.GONE
                        viewHolder.binding.translatedText.visibility = View.GONE
                        onResult(false)
                    }
                } else {
                    //Handle case where the message snapshot does not exist
                    viewHolder.binding.view.visibility = View.GONE
                    viewHolder.binding.translatedText.visibility = View.GONE
                    onResult(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Getting Translated Text", "Error Getting Translated Text: ${error.message}")
                viewHolder.binding.view.visibility = View.GONE
                viewHolder.binding.translatedText.visibility = View.GONE
            }
        })
    }


    //Update the message view status one-by-one to true, when the message is viewed by the opposite user
    private fun updateMessageReadStatus(message: Message) {
        val sharedPreferencesTARSwapper =
            context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Only update if the message is unread and it was sent by the other user
        if (!message.status!! && message.senderID != userID) {
            Log.d("Message Status", "Updating status for message ID: ${message.messageID}")
            val databaseReceiver =
                FirebaseDatabase.getInstance().getReference("Message").child(receiverRoom)
                    .child("message").child(message.messageID!!)
            val databaseSender =
                FirebaseDatabase.getInstance().getReference("Message").child(senderRoom)
                    .child("message").child(message.messageID!!)

            //Update for Receiver Room
            databaseReceiver.child("status").setValue(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Update for Sender Room
                    databaseSender.child("status").setValue(true).addOnCompleteListener { task2 ->
                        if (task2.isSuccessful) {
                            message.status = true
                            notifyItemChanged(messages.indexOf(message))
                        }
                    }
                } else {
                    Log.e("Firebase Update", "Failed to update status: ${task.exception?.message}")
                }
            }
        }
    }

    private fun checkUndoValidity(messageSentTime: String, onResult: (Boolean) -> Unit) {
        //Convert message sent time to Date
        val sentTime = getOnlyDateTimeToCompare(messageSentTime)
        val currentTime = getOnlyDateTimeToCompare(getCurrentDateTime())

        Log.e("sentTime", "$sentTime")
        Log.e("currentTime", "$currentTime")

        //Calculate the difference in milliseconds
        val differenceInMillis = currentTime.time - sentTime.time

        //Check if the difference is greater than 5 minutes (300000 milliseconds)
        val isOverFiveMinutes = differenceInMillis > 5 * 60 * 1000

        //Call the onResult callback with the result
        onResult(isOverFiveMinutes)
    }

    //Convert DateTime to Date object
    private fun getOnlyDateTimeToCompare(dateTime: String): Date {
        val dateFormat = SimpleDateFormat("HH:mm • dd/M/yy", Locale.getDefault())
        return dateFormat.parse(dateTime) ?: Date() // Return current date if parsing fails
    }

    //Get the Formatted Date Time for Compare
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm • dd/M/yy", Locale.getDefault())
        return dateFormat.format(Date())
    }


    //Identify the language of the message text
    private fun identifyLanguage(text: String, currentItem: Message) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    //In the case that language could not be identified
                    Log.d("Language Detection", "Language could not be identified.")

                    translateText(text, languageCode, currentItem)
                } else {
                    //Handle the detected language code
                    Log.d("Language Detection", "Detected language: $languageCode")

                    //Use the detected message text language for transaction
                    translateText(text, languageCode, currentItem)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "Language Detection Error",
                    exception.message ?: "Error during language detection"
                )
            }
    }

    //Translate the message text to target language
    private fun translateText(
        text: String,
        sourceLanguage: String,
        currentItem: Message,
    ) {
        //Get this device used language
        val targetLanguage = Locale.getDefault().language

        //Create the language model from source lan to target lan
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()

        val translator: Translator = Translation.getClient(options)
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.e("Translation", "Language model downloaded")
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        Log.d("Translation", translatedText)

                        currentItem.translatedText = translatedText.toString()

                        //Store the translated text into Firebase
                        //Update for Sender Room
                        currentItem.messageID?.let { itOne ->
                            FirebaseDatabase.getInstance().reference.child("Message")
                                .child(senderRoom)
                                .child("message")
                                .child(itOne)
                                .setValue(currentItem)
                        }

                        //Update for Receiver Room
                        currentItem.messageID?.let { itTwo ->
                            FirebaseDatabase.getInstance().reference.child("Message")
                                .child(receiverRoom)
                                .child("message")
                                .child(itTwo)
                                .setValue(currentItem)
                        }

                        translator.close()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Translation Error", exception.message ?: "Error during translation")
                        translator.close()
                    }
            }
            .addOnFailureListener {
                currentItem.translatedText = "Cannot identify language"

                //Update for Sender Room
                currentItem.messageID?.let { itOne ->
                    FirebaseDatabase.getInstance().reference.child("Message")
                        .child(senderRoom)
                        .child("message")
                        .child(itOne)
                        .setValue(currentItem)
                }

                //Update for Receiver Room
                currentItem.messageID?.let { itTwo ->
                    FirebaseDatabase.getInstance().reference.child("Message")
                        .child(receiverRoom)
                        .child("message")
                        .child(itTwo)
                        .setValue(currentItem)
                }

                Log.e("Translation Error", "Failed to download language model")
                translator.close()
            }

    }

}