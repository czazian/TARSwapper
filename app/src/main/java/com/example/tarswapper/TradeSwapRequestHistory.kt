package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.TradeAdapter
import com.example.tarswapper.dataAdapter.TradeSwapRequestHistoryAdapter
import com.example.tarswapper.databinding.FragmentTradeSwapRequestHistoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TradeSwapRequestHistory : Fragment() {
    //fragment name
    private lateinit var binding: FragmentTradeSwapRequestHistoryBinding
    private lateinit var userObj: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentTradeSwapRequestHistoryBinding.inflate(layoutInflater, container, false)

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

        //bind adapter
        val productRef = FirebaseDatabase.getInstance().getReference("Product")
        // Step 1: Get all product IDs created by the user

        // Step 1: Get all product IDs created by the user with status "Available"
        productRef.orderByChild("created_by_UserID").equalTo(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userProductIds = mutableListOf<String>()

                    for (productSnapshot in snapshot.children) {
                        val productStatus = productSnapshot.child("status").value as? String
                        val productId = productSnapshot.key // Product ID

                        // Only add product if status is "Available"
                        if (productId != null && productStatus == getString(R.string.PRODUCT_AVAILABLE)) {
                            userProductIds.add(productId)
                        }
                    }

                    // Step 2: Fetch swap requests where receiverProductID matches any of user's product IDs and status is "AwaitingResponse"
                    fetchSwapRequestsForUserProducts(userProductIds) {swapRequestList ->
                        binding.recyclerViewSRHistory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                        binding.recyclerViewSRHistory.adapter = TradeSwapRequestHistoryAdapter(swapRequestList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching user's products: ${error.message}")
                }
            })

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

    //get own swap request with Awaiting status
    fun getSwapRequestFromFirebase(status: String? = null, onResult: (List<SwapRequest>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        // Reference to the "SwapRequest" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("SwapRequest")
        val query       = databaseRef.orderByChild("receiverProductID").equalTo(userID)

        // List to hold products retrieved from Firebase
        val swapRequestList = mutableListOf<SwapRequest>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (swapRequestSnapshot in snapshot.children) {
                        val swapRequest = swapRequestSnapshot.getValue(SwapRequest::class.java)
                        //filter out own product
                        if (swapRequest != null && swapRequest.status == status) {
                            swapRequestList.add(swapRequest) // Add the product to the list
                        }
                    }
                    onResult(swapRequestList) // Return the list of products
                    Log.d("swap request list found", swapRequestList.size.toString())
                } else {
                    // Handle empty database
                    onResult(emptyList())
                    Log.d("Empty found", swapRequestList.size.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
                onResult(emptyList())
            }
        })
    }

    //get own swap request with Awaiting status
    fun fetchSwapRequestsForUserProducts(userProductIds: List<String>, onResult: (List<SwapRequest>) -> Unit) {
        val swapRequestRef = FirebaseDatabase.getInstance().getReference("SwapRequest")

        Log.d("result", userProductIds.toString())
        swapRequestRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val matchingSwapRequests = mutableListOf<SwapRequest>()

                for (swapSnapshot in snapshot.children) {
                    val swapRequest = swapSnapshot.getValue(SwapRequest::class.java)
                    val receiverProductID = swapSnapshot.child("senderProductID").value as? String
                    val swapRequestStatus = swapSnapshot.child("status").value as? String

                    // Check if receiverProductID matches any product ID owned by the user and status is "accept" or reject
                    if (receiverProductID != null && userProductIds.contains(receiverProductID) && swapRequestStatus in listOf(getString(R.string.SWAP_REQUEST_ACCEPTED), getString(R.string.SWAP_REQUEST_REJECTED), getString(R.string.SWAP_REQUEST_EXPIRED), getString(R.string.SWAP_REQUEST_PRODUCT_NOT_AVAILABLE))) {
                        if (swapRequest != null) {
                            matchingSwapRequests.add(swapRequest)
                        }
                    }
                }
                onResult(matchingSwapRequests)

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching swap requests: ${error.message}")
            }
        })
    }
}