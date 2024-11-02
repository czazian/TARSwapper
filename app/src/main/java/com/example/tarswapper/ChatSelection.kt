package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.ChatRoom
import com.example.tarswapper.dataAdapter.ChatSelectionAdapter
import com.example.tarswapper.databinding.FragmentChatSelectionBinding
import com.example.tarswapper.interfaces.OnUserContactClick
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatSelection : Fragment(), OnUserContactClick {
    private lateinit var binding: FragmentChatSelectionBinding
    private var adapter: ChatSelectionAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentChatSelectionBinding.inflate(layoutInflater, container, false)

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE


        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        ////Processing////
        //Get List of Contact List
        if (userID != null) {
            getListOfContactList(userID)
        }

        //Listen for changes in the message rooms
        val roomsRef = FirebaseDatabase.getInstance().getReference("Message")
        roomsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (userID != null) {
                    val messageRooms = snapshot.children
                    var hasSentMessage = false

                    for (room in messageRooms) {
                        val messages = room.child("message").children
                        for (message in messages) {
                            val senderID = message.child("senderID").getValue(String::class.java)
                            if (senderID == userID) {
                                hasSentMessage = true
                                break
                            }
                        }
                        if (hasSentMessage) break
                    }

                    //Call getListOfContactList only if the current user has sent at least one message
                    if (hasSentMessage) {
                        getListOfContactList(userID)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })


        //Handle Contact Search
        binding.searchbtn.setOnClickListener() {
            val searchText = binding.searchtext.text.toString()

            //Clear the search text
            binding.searchtext.setText("")

            getListOfContactBySearch(userID, searchText)
        }


        return binding.root
    }


    //Get Only Matching Contact List Item
    private fun getListOfContactBySearch(userID: String?, searchText: String) {
        val roomsRef = FirebaseDatabase.getInstance().getReference("Message")
        val usersRef = FirebaseDatabase.getInstance().getReference("User")

        roomsRef.get().addOnSuccessListener { snapshot ->
            //Check if the fragment is attached before proceeding
            if (!isAdded) return@addOnSuccessListener

            val userRooms = mutableListOf<ChatRoom>()

            for (roomSnapshot in snapshot.children) {
                val roomID = roomSnapshot.key ?: ""

                //Check if the roomID starts with the CurrentUserID
                if (roomID.startsWith(userID!!)) {
                    val lastMsg = roomSnapshot.child("lastMsg").getValue(String::class.java)
                    val lastMsgTime = roomSnapshot.child("lastMsgTime").getValue(String::class.java)

                    val oppositeUserId = roomID.removePrefix("$userID")

                    //Now fetch the user's data to match the searchText
                    usersRef.child(oppositeUserId).get().addOnSuccessListener { userSnapshot ->
                        val userName = userSnapshot.child("name").getValue(String::class.java) ?: ""

                        //Check if the user's name contains the search text
                        if (userName.contains(searchText, ignoreCase = true)) {
                            val chatRoom = ChatRoom(roomID, oppositeUserId, lastMsg, lastMsgTime)
                            userRooms.add(chatRoom)
                        }

                        adapter = ChatSelectionAdapter(requireContext(), this)
                        adapter!!.setData(userRooms)
                        binding.contactRecyclerView.layoutManager =
                            LinearLayoutManager(requireContext())
                        binding.contactRecyclerView.adapter = adapter
                        binding.contactRecyclerView.setHasFixedSize(true)
                        adapter!!.notifyDataSetChanged()

                    }
                }
            }
        }
    }


    //Get All Contact List Items
    private fun getListOfContactList(userID: String?) {
        val roomsRef = FirebaseDatabase.getInstance().getReference("Message")

        roomsRef.get().addOnSuccessListener { snapshot ->
            //Check if the fragment is attached before proceeding
            if (!isAdded) return@addOnSuccessListener

            val userRooms = mutableListOf<ChatRoom>()

            for (roomSnapshot in snapshot.children) {
                val roomID = roomSnapshot.key ?: ""

                //Check if the roomID starts with the CurrentUserID
                if (roomID.startsWith(userID!!)) {
                    val lastMsg = roomSnapshot.child("lastMsg").getValue(String::class.java)
                    val lastMsgTime = roomSnapshot.child("lastMsgTime").getValue(String::class.java)

                    val oppositeUserId = roomID.removePrefix("$userID")

                    val chatRoom = ChatRoom(roomID, oppositeUserId, lastMsg, lastMsgTime)
                    userRooms.add(chatRoom)
                }
            }

            adapter = ChatSelectionAdapter(requireContext(), this)
            adapter!!.setData(userRooms)
            binding.contactRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.contactRecyclerView.adapter = adapter
            binding.contactRecyclerView.setHasFixedSize(true)
            adapter!!.notifyDataSetChanged()
        }
    }

    //When a contact is click, pass in the opposite user id
    override fun onItemClick(chatRoom: ChatRoom) {
        val bundle = Bundle().apply {
            putString("oppositeUserID", chatRoom.oppositeUserID)
            putString("roomID", chatRoom.roomID)
            putString("lastMessage", chatRoom.lastMsg)
            putString("lastMessageTime", chatRoom.lastMsgTime)
        }

        val fragment = Chat().apply {
            arguments = bundle
        }

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.frameLayout, fragment)
            setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
            addToBackStack(null)
            commit()
        }
    }

}