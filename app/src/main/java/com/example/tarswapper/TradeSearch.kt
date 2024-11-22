package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.MyOrderVPAdapter
import com.example.tarswapper.dataAdapter.TradeAdapter
import com.example.tarswapper.databinding.FragmentTradeOrderBinding
import com.example.tarswapper.databinding.FragmentTradeSearchBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TradeSearch : Fragment() {
    //fragment name
    private lateinit var binding: FragmentTradeSearchBinding
    private lateinit var userObj: User
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentTradeSearchBinding.inflate(layoutInflater, container, false)

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

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

        binding.ProductRV.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.searchBtn.setOnClickListener{
            val value = binding.searchInput.text.toString()
            getProductsFromFirebase(value = value, status = "Available") { productList ->
                binding.ProductRV.adapter = TradeAdapter(productList,  requireContext())
            }
        }



        binding.btnBack.setOnClickListener{
            val fragment = Trade()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }
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

    //get product by search key and not own product
    fun getProductsFromFirebase(value: String? = null, status: String? = null, onResult: (List<Product>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Product")

        // Construct the query for partial matching
        val query = if (!value.isNullOrEmpty()) {
            databaseRef.orderByChild("name").startAt(value).endAt(value + "\uf8ff")
        } else {
            databaseRef.orderByChild("name")
        }

        // List to hold products retrieved from Firebase
        val productList = mutableListOf<Product>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (productSnapshot in snapshot.children) {
                        val product = productSnapshot.getValue(Product::class.java)
                        // Filter out own products and check status
                        if (product != null && product.created_by_UserID != userID && (status == null || product.status == status)) {
                            productList.add(product) // Add the product to the list
                        }
                    }
                    onResult(productList) // Return the list of products
                    Log.d("Product List Found", productList.size.toString())
                } else {
                    // Handle empty database
                    onResult(emptyList())
                    Log.d("Empty Found", productList.size.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
                onResult(emptyList())
            }
        })
    }


}