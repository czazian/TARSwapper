package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.TradeAdapter
import com.example.tarswapper.dataAdapter.TradeOrderOnGoingAdapter
import com.example.tarswapper.dataAdapter.TradeSwapRequestReceivedAdapter
import com.example.tarswapper.databinding.FragmentTradeOrderOngoingBinding
import com.example.tarswapper.databinding.FragmentTradeSwapRequestReceivedBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TradeOrderOnGoing : Fragment() {
    //fragment name
    private lateinit var binding: FragmentTradeOrderOngoingBinding
    private lateinit var userObj: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentTradeOrderOngoingBinding.inflate(layoutInflater, container, false)

        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        getUserRecord(userID.toString()) {
            if (it != null) {
                userObj = it
            }
        }


        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        fetchAllOrdersForUser(userID.toString(),
            onResult = { orders ->
                Log.d("AllOrders", "Fetched all orders: ${orders.size}")
                Log.d("Testing all orders", orders.toString())
                if (orders.isNotEmpty()) {
                    binding.recyclerView.adapter = TradeOrderOnGoingAdapter(orders, requireContext())
                } else {
                    Log.d("AllOrders", "No orders found for user.")
                }
            },
            onError = { error ->
                Log.e("FirebaseError", "Error fetching orders: ${error?.message}")
            }
        )

        return binding.root
    }

    private fun getUserRecord(userID: String, onResult: (User?) -> Unit) {
        //Get a reference to the database
        val databaseRef = FirebaseDatabase.getInstance().getReference("User").child(userID)

        //Add a listener to retrieve the user data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //Convert the snapshot to a User object
                    val user = snapshot.getValue(User::class.java)
                    onResult(user) //Return the user record
                } else {
                    onResult(null) //User not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null) //In case of error, return null
            }
        })
    }

    fun fetchAllOrdersForUser(
        userID: String,
        onResult: (List<Order>) -> Unit,
        onError: (DatabaseError?) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val ordersRef = database.getReference("Order")
        val swapRequestsRef = database.getReference("SwapRequest")
        val productsRef = database.getReference("Product")

        val allOrders = mutableListOf<Order>()
        var tasksRemaining = 2 // Two main tasks: sale orders and swap orders

        // Helper function to decrement tasks and invoke onResult when done
        fun onTaskComplete() {
            tasksRemaining -= 1
            Log.d("TaskStatus", "Tasks remaining: $tasksRemaining")
            if (tasksRemaining == 0) {
                onResult(allOrders)
            }
        }

        // Fetch Sale Orders
        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (orderSnap in snapshot.children) {
                    val tradeType = orderSnap.child("tradeType").getValue(String::class.java)
                    val status = orderSnap.child("status").getValue(String::class.java)
                    val buyerID = orderSnap.child("buyerID").getValue(String::class.java)
                    val sellerID = orderSnap.child("sellerID").getValue(String::class.java)

                    if (tradeType == "Sale" && status == getString(R.string.ORDER_ONGOING) &&
                        (buyerID == userID || sellerID == userID)
                    ) {
                        val orderID = orderSnap.key ?: ""
                        allOrders.add(Order(orderID, tradeType, status, buyerID, sellerID))
                    }
                }
                Log.d("SaleOrders", "Fetched sale orders: ${allOrders.size}")
                onTaskComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })

// Fetch Swap Orders
        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(orderSnapshot: DataSnapshot) {
                val swapTasks = mutableListOf<() -> Unit>()
                var tasksRemaining = 0

                for (orderSnap in orderSnapshot.children) {
                    val tradeType = orderSnap.child("tradeType").getValue(String::class.java)
                    val status = orderSnap.child("status").getValue(String::class.java)
                    val swapRequestID = orderSnap.child("swapRequestID").getValue(String::class.java)

                    if (tradeType == "Swap" && status == getString(R.string.ORDER_ONGOING) && swapRequestID != null) {
                        tasksRemaining++
                        swapRequestsRef.child(swapRequestID).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(swapRequestSnapshot: DataSnapshot) {
                                val receiverProductID = swapRequestSnapshot.child("receiverProductID").getValue(String::class.java)
                                val senderProductID = swapRequestSnapshot.child("senderProductID").getValue(String::class.java)

                                val productIDsToCheck = listOfNotNull(receiverProductID, senderProductID)
                                productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(productSnapshot: DataSnapshot) {
                                        for (productID in productIDsToCheck) {
                                            val product = productSnapshot.child(productID)
                                            val createdByUserID = product.child("created_by_UserID").getValue(String::class.java)

                                            if (createdByUserID == userID) {
                                                val orderID = orderSnap.key ?: ""
                                                allOrders.add(
                                                    Order(
                                                        orderID = orderID,
                                                        tradeType = tradeType ?: "",
                                                        status = status ?: "",
                                                        swapRequestID = swapRequestID
                                                    )
                                                )
                                            }
                                        }

                                        // Decrement remaining tasks and check if all tasks are completed
                                        tasksRemaining--
                                        if (tasksRemaining == 0) {
                                            onTaskComplete()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        onError(error)
                                        tasksRemaining--
                                        if (tasksRemaining == 0) {
                                            onTaskComplete()
                                        }
                                    }
                                })
                            }

                            override fun onCancelled(error: DatabaseError) {
                                onError(error)
                                tasksRemaining--
                                if (tasksRemaining == 0) {
                                    onTaskComplete()
                                }
                            }
                        })
                    }
                }

                // If no tasks were added, call onTaskComplete immediately
                if (tasksRemaining == 0) {
                    onTaskComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })


    }




}