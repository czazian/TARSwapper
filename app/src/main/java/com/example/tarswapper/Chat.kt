package com.example.tarswapper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.ChatAdapter
import com.example.tarswapper.databinding.FragmentChatBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale


class Chat() : Fragment() {
    private lateinit var binding: FragmentChatBinding
    var adapter: ChatAdapter? = null
    var messages: ArrayList<Message>? = null
    var senderRoom: String? = null
    var receiverRoom: String? = null
    var senderID: String? = null
    var receiverID: String? = null


    //Store temporary image
    private var tempImageUrl: String? = null
    private var mediaType: String? = null

    //From Previous Fragment
    private lateinit var oppositeUserID: String
    private lateinit var roomID: String
    private lateinit var lastMessage: String
    private lateinit var lastMessageTime: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE

        //Back to Select Contact Page
        binding.backContactListBtn.setOnClickListener() {
            val fragment = ChatSelection()

            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.chat

            //Back to previous page with animation
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        //Go to Chat History Page
        binding.moreOptionChatt.setOnClickListener() {
            val bundle = Bundle().apply {
                putString("oppositeUserID", oppositeUserID)
                putString("roomID", roomID)
            }

            val fragment = ChatHistory().apply {
                arguments = bundle
            }

            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.frameLayout, fragment)
                setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                addToBackStack(null)
                commit()
            }
        }

        //---------------------------------------------------------------------------------------------------------------------------------

        //Get FCM Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            //Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Registration Token: $token")
        }

        ////GET SENDER AND RECEIVER////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)


        //Get Arguments/Data from Previous Fragment
        arguments?.let { bundle ->
            oppositeUserID = bundle.getString("oppositeUserID", "")
            roomID = bundle.getString("roomID", "")
            lastMessage = bundle.getString("lastMessage", "")
            lastMessageTime = bundle.getString("lastMessageTime", "")
        }

        //Update the header info to Opposite User
        updateHeaderInfo(oppositeUserID) { user ->
            if (user != null) {
                binding.contactNameChat.text = user.name.toString()
                Glide.with(requireContext())
                    .load(user.profileImage)
                    .into(binding.userIcn)
            }
        }

        //Sender = Own
        senderID = userID

        //Receiver = Opposite
        receiverID = oppositeUserID

        ////GET LIST OF MESSAGES FROM DATABASE////
        messages = ArrayList()

        ////Database Instance////
        val database = FirebaseDatabase.getInstance()

        //Determine the Message is Sent By? and Received By?
        senderRoom = "$senderID$receiverID"
        receiverRoom = "$receiverID$senderID"

        ////Bind the messages to the RecyclerView////
        adapter = ChatAdapter(
            requireContext(),
            messages,
            senderRoom!!,
            receiverRoom!!,
            oppositeUserID!!,
            roomID!!
        )
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.messageRecyclerView.adapter = adapter

        //Get Messages Recycler View
        database.reference.child("Message")
            .child(senderRoom!!)
            .child("message")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Clear all message to avoid duplication from the previous view
                    val previousMessageCount = messages!!.size
                    messages!!.clear()

                    //Get all messages, add them into the messages list
                    for (itemSnapshot1 in snapshot.children) {
                        //Get each message as Message Obj
                        val message: Message? = itemSnapshot1.getValue(Message::class.java)
                        //Get the messageID under each room
                        message!!.messageID = itemSnapshot1.key
                        //Add the every message into messages list one-by-one
                        messages!!.add(message)
                    }

                    //Scroll to last item only on initial load
                    val newMessageCount = messages!!.size
                    if (newMessageCount > previousMessageCount) {
                        //Scroll to the last item if the message count has increased
                        binding.messageRecyclerView.scrollToPosition(newMessageCount - 1)
                    }


                    //After finish adding, update the RecyclerView
                    adapter!!.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })


        ////Send Message////
        binding.sendMsgBtn.setOnClickListener() {
            //Get message text and date => Use them to create Message obj
            val text = binding.sendMsgText.text.toString()

            //Check if text and image URL are valid
            if (text.isEmpty()) {
                AlertDialog
                    .Builder(context)
                    .setTitle("Error")
                    .setMessage("Message text cannot be empty")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()

                return@setOnClickListener
            }

            val dateTime = getCurrentDateTime()
            val message = Message(text, senderID, dateTime, tempImageUrl, mediaType)

            //After creating an object, set the text input field to empty
            binding.sendMsgText.setText("")

            //First, get a random ID for the message
            val randomID = database.reference.push().key

            //Update the Last Message & Last DateTime for each Room (Both site of users)
            val lastMessageObj = HashMap<String, Any>()
            lastMessageObj["lastMsg"] = message.message!!
            lastMessageObj["lastMsgTime"] = dateTime

            database.reference.child("Message").child(senderRoom!!).updateChildren(lastMessageObj)
            database.reference.child("Message").child(receiverRoom!!).updateChildren(lastMessageObj)

            //Insert the new message for each Room (Both site of users)

            //Insert for SenderRoom
            binding.keepImageBox.visibility = View.GONE
            database!!.reference.child("Message")
                .child(senderRoom!!)
                .child("message")
                .child(randomID!!)
                .setValue(message).addOnSuccessListener {
                    //Insert for ReceiverRoom
                    database.reference.child("Message")
                        .child(receiverRoom!!)
                        .child("message")
                        .child(randomID)
                        .setValue(message)
                        .addOnSuccessListener {
                            binding.messageRecyclerView.smoothScrollToPosition(adapter!!.itemCount - 1)
                            tempImageUrl = null
                            mediaType = null
                        }
                }
        }

        //Handle Attachment Upload (Image/Video)
        binding.attachFileBtn.setOnClickListener {
            val intent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                //Specify to allow all types first
                type = "*/*"
                //Then pass image and video
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            }
            startActivityForResult(intent, 50)
        }

        //When user chose to remove selected image
        binding.removeImageSelect.setOnClickListener() {
            binding.keepImageBox.visibility = View.GONE
            tempImageUrl = null
            mediaType = null
        }


        return binding.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 50 && resultCode == Activity.RESULT_OK) {
            //Show Progress Bar
            binding.progessLinear.visibility = View.VISIBLE
            binding.messages.visibility = View.GONE
            binding.bottomContainerView.visibility = View.GONE

            data?.data?.let { mediaUrl ->
                //Get Selected Image URI
                val media = mediaUrl

                // Get MIME type of selected media
                val mimeType = requireActivity().contentResolver.getType(mediaUrl)
                mediaType =
                    if (mimeType?.startsWith("image") == true) {
                        "image"
                    } else if (mimeType?.startsWith("video") == true) {
                        "video"
                    } else {
                        null
                    }

                //Retrieve the file name from the URI
                val cursor =
                    requireActivity().contentResolver.query(mediaUrl, null, null, null, null)
                var fileName = "unknown"
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex)
                        }
                    }
                }

                //Define the destination of storage in Firebase
                val calendar = Calendar.getInstance()
                val storage = FirebaseStorage.getInstance().reference.child("Media")
                    .child(calendar.timeInMillis.toString())

                //Store the image in Firebase Storage
                storage.putFile(media)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            storage.downloadUrl.addOnSuccessListener { uri ->
                                val filePath = uri.toString()
                                tempImageUrl = filePath

                                //Update View of Selected Image
                                binding.keepImageBox.visibility = View.VISIBLE
                                binding.imageName.text = fileName

                                //Disappear the Progress Bar
                                binding.progessLinear.visibility = View.GONE
                                binding.messages.visibility = View.VISIBLE
                                binding.bottomContainerView.visibility = View.VISIBLE
                            }
                        } else {
                            binding.keepImageBox.visibility = View.GONE
                            tempImageUrl = null
                            mediaType = null
                        }
                    }
            }
        }
    }


    private fun updateHeaderInfo(oppositeUserID: String, onResult: (User?) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("User")

        databaseReference.child(oppositeUserID).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val user = dataSnapshot.getValue(User::class.java)
                onResult(user)
            }
        }.addOnFailureListener {
            Log.e("Error Getting Opposite User Info", "Error Getting Opposite User Info")
        }
    }


    //Get the Formatted Date Time for Display
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm • dd/M/yy", Locale.getDefault())
        return dateFormat.format(Date())
    }
}