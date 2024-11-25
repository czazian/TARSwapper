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
import com.example.tarswapper.dataAdapter.TradeSwapRequestSentAdapter
import com.example.tarswapper.databinding.FragmentTradeSwapRequestReceivedBinding
import com.example.tarswapper.databinding.FragmentTradeSwapRequestSentBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TradeSwapRequestSent : Fragment() {
    //fragment name
    private lateinit var binding: FragmentTradeSwapRequestSentBinding
    private lateinit var userObj: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentTradeSwapRequestSentBinding.inflate(layoutInflater, container, false)

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

        Log.d("HAVE", "HAVE1")
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
                        Log.d("HAVE", "HAVE2")
                    }

                    // Step 2: Fetch swap requests where senderProductID matches any of user's product IDs and status is "AwaitingResponse"
                    fetchSwapRequestsForUserProducts(userProductIds) {swapRequestList ->
                        Log.d("list fetched", swapRequestList.toString())
                        binding.recyclerViewSRSent.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                        binding.recyclerViewSRSent.adapter = TradeSwapRequestSentAdapter(swapRequestList, requireContext())
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

                    // Check if receiverProductID matches any product ID owned by the user and status is "AwaitingResponse"
                    if (receiverProductID != null && userProductIds.contains(receiverProductID) && swapRequestStatus == getString(R.string.SWAP_REQUEST_AWAITING_RESPONSE)) {
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