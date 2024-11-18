package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
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
                        bindTransaction(userID!!)
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
    private fun bindTransaction(currentUserID: String) {

        fetchData(currentUserID) { list ->
            //If the result is not null or empty
            if (list.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No transactions available", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("RESULT", list.toString())

                //Set up adapter
                val adapter = TransactionAdapter(requireContext()) { transaction ->

                    Log.d("TransactionAdapter", "Button clicked for transaction: ${transaction.swapRequestID}")

                    val bundle = Bundle().apply {
                        //Pass all data need to be used in Navigation
                        putSerializable("transaction", transaction)
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
                adapter.setData(list)
                binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.notificationRecyclerView.adapter = adapter
                binding.notificationRecyclerView.setHasFixedSize(true)
            }
        }
    }

    private fun fetchData(currentUserID: String, onResult: (List<SwapRequest>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val swapRequestsRef = database.getReference("SwapRequest")
        val productsRef = database.getReference("Product")

        productsRef.get().addOnSuccessListener { productSnapshot ->
            val products = productSnapshot.children.mapNotNull { it.getValue(Product::class.java) }
            val userProductIDs = products
                .filter { it.created_by_UserID == currentUserID }
                .map { it.productID }

            swapRequestsRef.get().addOnSuccessListener { swapRequestSnapshot ->
                val swapRequests = swapRequestSnapshot.children.mapNotNull { it.getValue(SwapRequest::class.java) }

                //Filter SwapRequests
                val userSwapRequests = swapRequests.filter { swapRequest ->
                    swapRequest.status == "Accepted" &&
                            (swapRequest.senderProductID in userProductIDs ||
                                    swapRequest.receiverProductID in userProductIDs)
                }

                //Print the results
                onResult(userSwapRequests)

            }.addOnFailureListener { e ->
                Log.e("Error", "Error fetching SwapRequest: ${e.message}")
            }
        }.addOnFailureListener { e ->
            Log.e("Error", "Error fetching Product: ${e.message}")
        }
    }

}