package com.example.tarswapper

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.TradeAdapter
import com.example.tarswapper.databinding.FragmentTradeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Trade : Fragment() {
    //fragment name
    private lateinit var binding: FragmentTradeBinding
    private lateinit var userObj: User
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentTradeBinding.inflate(layoutInflater, container, false)

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
                //Meaning to say the user has record, and store as "it"
                //Display user data
                binding.usernameTV.text = it.name.toString()
                Glide.with(requireContext()).load(it.profileImage) // User Icon URL string
                    .into(binding.profileImgV)

            }
        }

        // Set LayoutManager for RecyclerView & Call getProductsFromFirebase to populate the RecyclerView
        binding.ProductRV.layoutManager = GridLayoutManager(requireContext(), 2)
        //by default is filter sale only
        getProductsFromFirebase(tradeType = "Sale", status = "Available") { productList ->
            binding.ProductRV.adapter = TradeAdapter(productList, requireContext())
        }

        //on click
        binding.userProfileLayout.setOnClickListener{
            val fragment = UserDetail()

            // Create a Bundle to pass data
            val bundle = Bundle()
            bundle.putString("UserID", userID) // Example data

            // Set the Bundle as arguments for the fragment
            fragment.arguments = bundle

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

//        binding.meetUpBtn.setOnClickListener{
//            val fragment = MeetUpCalender()
//
//            val transaction = activity?.supportFragmentManager?.beginTransaction()
//            transaction?.replace(R.id.frameLayout, fragment)
//            transaction?.setCustomAnimations(
//                R.anim.fade_out,  // Enter animation
//                R.anim.fade_in  // Exit animation
//            )
//            transaction?.addToBackStack(null)
//            transaction?.commit()
//        }

        binding.myProductBtn.setOnClickListener(){

            val fragment = TradeMyShop()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.requestReceivedBtn.setOnClickListener{
            val fragment = TradeSwapRequestReceived()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.swapRequestBtn.setOnClickListener{
            val fragment = TradeSwapRequest()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.orderBtn.setOnClickListener{
            val fragment = TradeOrder()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.searchBtn.setOnClickListener{
            val fragment = TradeSearch()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }



        //sale and swap button design
        val selectedColor = "#AC87C5" // Selected color
        val unselectedColor = "#E8E8E9" // Unselected color
        binding.SaleBtn.setOnClickListener{
            binding.SaleBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(selectedColor))
            binding.SwapBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(unselectedColor))
            //get product sale
            getProductsFromFirebase(tradeType = "Sale", status = "Available") { productList ->
                binding.ProductRV.adapter = TradeAdapter(productList,  requireContext())
            }
        }

        binding.SwapBtn.setOnClickListener{
            binding.SaleBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(unselectedColor))
            binding.SwapBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(selectedColor))
            //get product swap
            getProductsFromFirebase(tradeType = "Swap", status = "Available") { productList ->
                binding.ProductRV.adapter = TradeAdapter(productList, requireContext())
            }
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

    //get product by trade type and not own product
    fun getProductsFromFirebase(tradeType: String? = null, status: String? = null, onResult: (List<Product>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)
        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Product")
        val query       = databaseRef.orderByChild("tradeType").equalTo(tradeType)

        // List to hold products retrieved from Firebase
        val productList = mutableListOf<Product>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (productSnapshot in snapshot.children) {
                        val product = productSnapshot.getValue(Product::class.java)
                        //filter out own product
                        if (product != null && product.created_by_UserID != userID && product.status == status) {
                            productList.add(product) // Add the product to the list
                        }
                    }
                    onResult(productList) // Return the list of products
                    Log.d("product list found", productList.size.toString())
                } else {
                    // Handle empty database
                    onResult(emptyList())
                    Log.d("Empty found", productList.size.toString())
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