package com.example.tarswapper

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.Notification
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.dataAdapter.MessageAdapter
import com.example.tarswapper.dataAdapter.NotificationTradeAdapter
import com.example.tarswapper.dataAdapter.TransactionAdapter
import com.example.tarswapper.databinding.FragmentNotificationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        //default select first one
        binding.tabs.getTabAt(0)?.select()
        bindTrade()

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
                        bindTrade()
                    }
                    //Community
                    1 -> {
                        bindCommunity()
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

    private fun bindTrade() {

        fetchTradeNotification { list ->
            //If the result is not null or empty
            if (list.isNullOrEmpty()) {
                binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.notificationRecyclerView.adapter = NotificationTradeAdapter(list, requireContext())
                binding.notificationRecyclerView.setHasFixedSize(true)
            } else {
                Log.e("RESULT", list.toString())

                binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.notificationRecyclerView.adapter = NotificationTradeAdapter(list, requireContext())
                binding.notificationRecyclerView.setHasFixedSize(true)
            }
        }

        setupSwipeToRemove()
    }

    private fun bindCommunity() {

        fetchCommunityNotification { list ->
            //If the result is not null or empty
            if (list.isNullOrEmpty()) {
                binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.notificationRecyclerView.adapter = NotificationTradeAdapter(list, requireContext())
                binding.notificationRecyclerView.setHasFixedSize(true)
            } else {
                Log.e("RESULT", list.toString())

                binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.notificationRecyclerView.adapter = NotificationTradeAdapter(list, requireContext())
                binding.notificationRecyclerView.setHasFixedSize(true)
            }
        }
        setupSwipeToRemove()
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
                val adapter = TransactionAdapter(requireContext()){}
                adapter.setData(list)
                binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.notificationRecyclerView.adapter = adapter
                binding.notificationRecyclerView.setHasFixedSize(true)
            } else {
                Log.e("RESULT", list.toString())

                //Set up adapter + When "Start Navigation Button is Clicked"
                val adapter = TransactionAdapter(requireContext()) { transaction ->

                    Log.d("TransactionAdapter", "Button clicked for transaction: ${transaction.swapRequestID}")

                    //Start Count Time
                    val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    )
                    val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences("CountTime", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    // Store the date and time
                    editor.putString("dateTime", currentDateTime)
                    editor.apply()


                    //Redirection
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

    private fun fetchData(currentUserID: String, onResult: (List<Order>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val ordersRef = database.getReference("Order")
        val swapRequestsRef = database.getReference("SwapRequest")
        val productsRef = database.getReference("Product")

        ordersRef.get().addOnSuccessListener { orderSnapshot ->
            val orders = orderSnapshot.children.mapNotNull { it.getValue(Order::class.java) }

            ////Separate orders into Sale and Swap categories////
            //If the Order is Sale, Directly check if the Current Logged On User is buyer or seller
            val saleOrders = orders.filter { order ->
                order.tradeType == "Sale" &&
                        (order.buyerID == currentUserID || order.sellerID == currentUserID)
            }

            //If the Order is Swap, need to use SwapRequest ID (SwapRequest > Product) to know weather the Current Logged On User is either the buyer or seller
            val swapOrders = orders.filter { order ->
                order.tradeType == "Swap"
            }

            //Return only sale orders if no swap orders
            if (swapOrders.isEmpty()) {
                onResult(saleOrders)
                return@addOnSuccessListener
            }

            //Fetch related data for Swap orders
            productsRef.get().addOnSuccessListener { productSnapshot ->
                //Get all Product and Filter based on the current logged on user
                val products = productSnapshot.children.mapNotNull { it.getValue(Product::class.java) }
                val userProductIDs = products
                    .filter { it.created_by_UserID == currentUserID }
                    .map { it.productID }

                swapRequestsRef.get().addOnSuccessListener { swapRequestSnapshot ->
                    //Retrieve all SwapRequest objects from the database
                    val swapRequests = swapRequestSnapshot.children.mapNotNull { it.getValue(SwapRequest::class.java) }

                    //Filter swap orders related to the user
                    val userSwapOrders = swapOrders.filter { order ->
                        val relatedSwapRequest = swapRequests.find { it.swapRequestID == order.swapRequestID }
                        relatedSwapRequest != null &&
                                (relatedSwapRequest.senderProductID in userProductIDs ||
                                        relatedSwapRequest.receiverProductID in userProductIDs)
                    }

                    //Combine Sale and Swap orders
                    val allOrders = saleOrders + userSwapOrders
                    Log.e("Notification Check List", "Sale: $saleOrders")
                    Log.e("Notification Check List", "Swap: $userSwapOrders")

                    onResult(allOrders)

                }.addOnFailureListener { e ->
                    Log.e("Error", "Error fetching SwapRequest: ${e.message}")
                    onResult(saleOrders)
                }
            }.addOnFailureListener { e ->
                Log.e("Error", "Error fetching Product: ${e.message}")
                onResult(saleOrders)
            }
        }.addOnFailureListener { e ->
            Log.e("Error", "Error fetching Order: ${e.message}")
            onResult(emptyList())
        }
    }

    fun fetchTradeNotification( onResult: (MutableList<Notification>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Notification")
        val query = databaseRef.orderByChild("notificationType").equalTo("Trade")

        // List to hold products retrieved from Firebase
        val notificationList = mutableListOf<com.example.tarswapper.data.Notification>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        // Filter out own products and check status
                        if (notification != null && notification.userID == userID) {
                            notificationList.add(notification) // Add the product to the list
                        }
                    }
                    onResult(notificationList) // Return the list of products
                    Log.d("Product List Found", notificationList.size.toString())
                } else {
                    // Handle empty database
                    onResult(notificationList)
                    Log.d("Empty Found", notificationList.size.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
                onResult(notificationList)
            }
        })
    }

    fun fetchCommunityNotification( onResult: (MutableList<Notification>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Notification")

        val query = databaseRef.orderByChild("notificationType").equalTo("Community")

        // List to hold products retrieved from Firebase
        val notificationList = mutableListOf<com.example.tarswapper.data.Notification>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        // Filter out own products and check status
                        if (notification != null && notification.userID == userID) {
                            notificationList.add(notification) // Add the product to the list
                        }
                    }
                    onResult(notificationList) // Return the list of products
                    Log.d("Product List Found", notificationList.size.toString())
                } else {
                    // Handle empty database
                    onResult(notificationList)
                    Log.d("Empty Found", notificationList.size.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
                onResult(notificationList)
            }
        })
    }


    private fun setupSwipeToRemove() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Not needed for swipe
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Get the position of the swiped item
                val position = viewHolder.adapterPosition

                // Access the adapter and remove the item from the dataset
                val adapter = binding.notificationRecyclerView.adapter as NotificationTradeAdapter
                val removedItem = adapter.getItem(position)

                // Show confirmation for removal (optional)
                showRemoveConfirmationDialog(position, removedItem, adapter)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // Draw custom background for the swipe
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = Paint()
                    paint.color = Color.RED // Color for remove action

                    // Draw the background
                    c.drawRect(
                        itemView.right.toFloat() + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(),
                        paint
                    )

                    // Draw a "Remove" text or icon
                    val textPaint = Paint()
                    textPaint.color = Color.WHITE
                    textPaint.textSize = 40f
                    c.drawText(
                        "Remove",
                        itemView.right - 200f,  // Position of the text
                        itemView.top + itemView.height / 2f,
                        textPaint
                    )
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        // Attach the ItemTouchHelper to the RecyclerView
        itemTouchHelper.attachToRecyclerView(binding.notificationRecyclerView)
    }

    private fun showRemoveConfirmationDialog(
        position: Int,
        removedItem: Notification,
        adapter: NotificationTradeAdapter
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Notification")
            .setMessage("Are you sure you want to remove this notification?")
            .setPositiveButton("Yes") { dialog, _ ->
                // Remove the item from the list and notify adapter
                adapter.removeItem(position)

                // Optionally, delete from Firebase
                val databaseRef = FirebaseDatabase.getInstance().getReference("Notification")
                databaseRef.child(removedItem.notificationID.toString()).removeValue()

                Toast.makeText(requireContext(), "Notification removed", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                adapter.notifyItemChanged(position) // Restore item if canceled
                dialog.dismiss()
            }
            .show()
    }


}