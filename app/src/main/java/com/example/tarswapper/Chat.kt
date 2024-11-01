package com.example.tarswapper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.ChatAdapter
import com.example.tarswapper.databinding.FragmentChatBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.net.Inet4Address
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale


class Chat() : Fragment() {
    private lateinit var binding: FragmentChatBinding
    var adapter: ChatAdapter? = null
    var messages: ArrayList<Message>? = null
    var senderRoom:String? = null
    var receiverRoom: String? = null
    var senderID:String? = null
    var receiverID:String? = null
    private var isInitialScroll = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
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
            val fragment = ChatHistory()

            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.chat

            //Back to previous page with animation
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,
                R.anim.fade_in
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        //---------------------------------------------------------------------------------------------------------------------------------

        ////GET SENDER AND RECEIVER////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Opposite UserID - Need to change to true value after development + testing
        //TEST FOR INFINIX (Use this when run on the Infinix)
        val oppositeUserID = "-OAMvTvnFh2hqUW_HEdJ"
        //TEST FOR OPPO F9 (Use this when run on OPPO F9)
        //val oppositeUserID = "-OADY-HMy72rY1Mg1Cl5"

        //Update the header info to Opposite User
        updateHeaderInfo(oppositeUserID) { user ->
            if(user != null){
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
        adapter = ChatAdapter(requireContext(), messages, senderRoom!!, receiverRoom!!)
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.messageRecyclerView.adapter = adapter

        //Get Messages Recycler View
        database.reference.child("Message")
            .child(senderRoom!!)
            .child("message")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Clear all message to avoid duplication from the previous view
                    messages!!.clear()

                    //Get all messages, add them into the messages list
                    for(itemSnapshot1 in snapshot.children){
                        //Get each message as Message Obj
                        val message: Message? = itemSnapshot1.getValue(Message::class.java)
                        //Get the messageID under each room
                        message!!.messageID = itemSnapshot1.key
                        //Add the every message into messages list one-by-one
                        messages!!.add(message)
                    }
                    //After finish adding, update the RecyclerView
                    adapter!!.notifyDataSetChanged()

                    //Scroll to last item only on initial load
                    if (isInitialScroll) {
                        binding.messageRecyclerView.scrollToPosition(adapter!!.itemCount - 1)
                        isInitialScroll = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })


        ////Send Message////
        binding.sendMsgBtn.setOnClickListener(){
            //Get message text and date => Use them to create Message obj
            val text = binding.sendMsgText.text.toString()
            val dateTime = getCurrentDateTime()
            val message = Message(text, senderID, dateTime)

            //After creating an object, set the text input field to empty
            binding.sendMsgText.setText("")

            //First, get a random ID for the message
            val randomID = database.reference.push().key

            //Update the Last Message & Last DateTime for each Room (Both site of users)
            val lastMessageObj = HashMap<String,Any>()
            lastMessageObj["lastMsg"] = message.message!!
            lastMessageObj["lastMsgTime"] = dateTime

            database.reference.child("Message").child(senderRoom!!).updateChildren(lastMessageObj)
            database.reference.child("Message").child(receiverRoom!!).updateChildren(lastMessageObj)

            //Insert the new message for each Room (Both site of users)
            //Insert for SenderRoom
            database!!.reference.child("Message").child(senderRoom!!)
                .child("message")
                .child(randomID!!)
                .setValue(message).addOnSuccessListener {

                    //Insert for ReceiverRoom
                    database.reference.child("Message")
                        .child(receiverRoom!!)
                        .child("message")
                        .child(randomID)
                        .setValue(message)
                        .addOnSuccessListener {  }

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

        return binding.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 50 && resultCode == Activity.RESULT_OK) {
            if(data != null){
                //
                val image = data.data
                val calendar = Calendar.getInstance()

                val storage = FirebaseStorage.getInstance().reference.child("Media")
                    .child(calendar.timeInMillis.toString()+"")

                storage.putFile(image!!)
                    .addOnCompleteListener(){ task ->
                        if(task.isSuccessful){
                            storage.downloadUrl.addOnSuccessListener { uri ->
                                val filePath = uri.toString()
                                val text: String = binding.sendMsgText.text.toString()
                                val dateTime = getCurrentDateTime()
                                val message = Message(text,senderID, dateTime)
                                message.message = "photo"
                                message.media = filePath
                                binding.sendMsgText.setText("")

                                val database = FirebaseDatabase.getInstance()
                                val randomID = database.reference.push().key
                                val lastMessageObj = HashMap<String,Any>()
                                lastMessageObj["lastMsg"] = message.message!!   //String = Any value
                                lastMessageObj["lastMsgTime"] = dateTime        //String = Any value

                                database.reference.child("Message").updateChildren(lastMessageObj)
                                database.reference.child("Message").child(receiverRoom!!).updateChildren(lastMessageObj)

                                database.reference.child("Message").child(senderRoom!!)
                                    .child("messages")
                                    .child(randomID!!)
                                    .setValue(message).addOnSuccessListener {
                                        database.reference.child("Message")
                                            .child(receiverRoom!!)
                                            .child("messages")
                                            .child(randomID)
                                            .setValue(message)
                                            .addOnSuccessListener {  }
                                    }

                            }
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
    private fun getCurrentDateTime(): String{
        val dateFormat = SimpleDateFormat("HH:mm • dd/M/yy", Locale.getDefault())
        return dateFormat.format(Date())
    }
}