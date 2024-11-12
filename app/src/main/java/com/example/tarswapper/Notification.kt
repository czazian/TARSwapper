package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Message
import com.example.tarswapper.dataAdapter.MessageAdapter
import com.example.tarswapper.dataAdapter.TransactionAdapter
import com.example.tarswapper.databinding.FragmentNotificationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable

class Notification : Fragment() {
    private lateinit var binding: FragmentNotificationBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNotificationBinding.inflate(layoutInflater, container, false)

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE


        //Go to User Profile Page
        binding.btnBackUserProfile.setOnClickListener() {
            val fragment = UserProfile()

            //Bottom Navigation Indicator Update
            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.setting

            //Back to previous page with animation
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in    // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }


        //Processing
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Tab selected
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                //Selected Position of Tab
                when(tab.position){
                    //Trade
                    0 -> {

                    }
                    //Community
                    1 -> {

                    }
                    //Message
                    2 -> {
                        bindMessage(userID!!)
                    }
                    //Transaction
                    3 -> {
                        bindTransaction()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }
            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        return binding.root
    }


    //When Message Tab is Selected
    private fun bindMessage(userID: String) {
        val roomsRef = FirebaseDatabase.getInstance().getReference("Message")


        roomsRef.get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener

            val chatRooms = mutableMapOf<String, MutableList<Message>>()

            for (roomSnapshot in snapshot.children) {
                val roomID = roomSnapshot.key ?: continue

                //Check if the user is in this room
                val isUserInRoom = roomID.startsWith(userID) || roomID.endsWith(userID)
                if (!isUserInRoom) continue

                //Determine the oppositeUserID and create a canonical room ID
                val oppositeUserID = if (roomID.startsWith(userID)) {
                    roomID.removePrefix(userID)
                } else {
                    roomID.removeSuffix(userID)
                }
                val canonicalRoomID = if (userID < oppositeUserID) userID + oppositeUserID else oppositeUserID + userID

                //Check if we already processed this room
                if (chatRooms.containsKey(canonicalRoomID)) continue

                for (messageSnapshot in roomSnapshot.child("message").children) {
                    val senderID = messageSnapshot.child("senderID").getValue(String::class.java)

                    // Only include messages from the opposite user
                    if (senderID != oppositeUserID) continue

                    val messageText = messageSnapshot.child("message").getValue(String::class.java)
                    val dateTime = messageSnapshot.child("dateTime").getValue(String::class.java)
                    val media = messageSnapshot.child("media").getValue(String::class.java)
                    val mediaType = messageSnapshot.child("mediaType").getValue(String::class.java)

                    val message = Message(
                        messageText,
                        senderID,
                        dateTime,
                        media,
                        mediaType
                    )

                    //Group messages by the canonical room ID
                    chatRooms.getOrPut(canonicalRoomID) { mutableListOf() }.add(message)
                }
            }

            //Set up adapter with the chat rooms data
            val adapter = MessageAdapter(requireContext(), userID) { message ->
                val oppositeUserID = if (message.senderID == userID) userID else message.senderID
                val roomID = if (userID < oppositeUserID.toString()) userID + oppositeUserID else oppositeUserID + userID

                // Redirect to the message room (Chat fragment)
                redirectToMessageRoom(message, oppositeUserID, roomID)
            }
            adapter.setData(chatRooms)
            binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.notificationRecyclerView.adapter = adapter
            binding.notificationRecyclerView.setHasFixedSize(true)
        }
    }

    //Message Tab Click Event
    private fun redirectToMessageRoom(
        message: Message,
        oppositeUserID: String?,
        roomID: String,
    ) {
        val bundle = Bundle().apply {
            putString("oppositeUserID", oppositeUserID)
            putString("roomID", roomID)
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


    //When Transaction Tab is Selected
    private fun bindTransaction() {
        //Temporary Data
        var date = "2024-11-12"
        var time = "23:50"
        var ownUserID = "-OAQyscTlsEQdw_3lITE"
        var oppositeUserID = "-OADY-HMy72rY1Mg1Cl5"
        var senderProductID = "P01"
        var receiveProductID = "P02"
        var location = "TAR UMT Dewan Tunku Abdul Rahman (DTAR), 53100 Kuala Lumpur, Selangor"
        var tradeType = "swap"

        val tempItem = Temp(date, time, ownUserID, oppositeUserID, senderProductID, receiveProductID, location, tradeType)
        val tempItem2 = Temp("2024-11-12", "12:20", "-OAQyscTlsEQdw_3lITE", "-OADY-HMy72rY1Mg1Cl5", "P01", "P02", "3357, Jalan 18/31, Taman Sri Serdang, 43300 Seri Kembangan, Selangor", "swap")

        val tempList = mutableListOf<Temp>()
        tempList.add(tempItem)
        tempList.add(tempItem2)

        val adapter = TransactionAdapter(requireContext()) {
            val bundle = Bundle().apply {
                putSerializable("transaction", tempItem)
            }
            val fragment = Navigation().apply {
                arguments = bundle
            }
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.frameLayout, fragment)
                setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                addToBackStack(null)
                commit()
            }
        }
        adapter.setData(tempList)
        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationRecyclerView.adapter = adapter
        binding.notificationRecyclerView.setHasFixedSize(true)
    }

    data class Temp(
        var date: String? = null,
        var time: String? = null,
        var ownUserID: String? = null,
        var oppositeUserID: String? = null,
        var senderProductID: String? = null,
        var receiveProductID: String? = null,
        val location: String? = null,
        var tradeType: String? = null,
    ) : Serializable

}