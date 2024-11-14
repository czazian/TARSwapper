package com.example.tarswapper

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.TradeProductDetailImagesAdapter
import com.example.tarswapper.databinding.FragmentTradeProductDetailBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class TradeProductDetail : Fragment() {

    //fragment name
    private lateinit var binding: FragmentTradeProductDetailBinding
    private lateinit var userObj: User
    val imageUrls = mutableListOf<String>()

    // A variable to keep track of the number of fetched URLs
    var fetchedImageCount = 0
    // Total expected images (You can count the number of images you expect from both folders)
    var totalExpectedImages = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val args = arguments
        val productID = args?.getString("ProductID")

        binding = FragmentTradeProductDetailBinding.inflate(layoutInflater, container, false)
        setupViewPager(productID)


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

//        // Set LayoutManager for RecyclerView & Call getProductsFromFirebase to populate the RecyclerView
//        binding.ProductRV.layoutManager = GridLayoutManager(requireContext(), 2)
//        //by default is filter sale only
//        getProductsFromFirebase(tradeType = "Sale") { productList ->
//            binding.ProductRV.adapter = TradeAdapter(productList)
//        }

        //on click

        binding.btnBackMyPostedProduct.setOnClickListener{
            val fragment = Trade()

            //Bottom Navigation Indicator Update
            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.setting

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.submitBtn.setOnClickListener{
            //redirect to meet up detail page
            val fragment = TradeMeetUp()

            val bundle = Bundle()
            bundle.putString("ProductID", productID) // Add any data you want to pass
            fragment.arguments = bundle

            //Bottom Navigation Indicator Update
            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.setting

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        //check availability before showing
        var available = true

        if(available){

            getProductFromFirebase(productID = productID) { product ->
                binding.productNameTV.text = product.name
                binding.dateTV.text = customizeDate(product.created_at.toString())
                binding.descriptionTV.text = product.description
                binding.categoryTV.text = product.category
                binding.conditionTV.text = product.condition
                binding.tradeTV.text = product.tradeType

                //change the submit button text
                binding.submitBtn.text = product.tradeType!!.uppercase()

                if (product.tradeType == "Sale"){
                    //is sale
                    binding.tradeDetailTV.text = "RM ${product.price}"
                    binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    binding.tradeDetailTV.backgroundTintList = null
                    binding.swapCateTV1.visibility = View.GONE
                    binding.swapCateTV2.visibility = View.GONE
                    binding.swapRemarkTV1.visibility = View.GONE
                    binding.swapRemarkTV2.visibility = View.GONE
                } else if (product.tradeType == "Swap"){
                    //is swap
                    binding.swapCateTV1.visibility = View.VISIBLE
                    binding.swapCateTV2.visibility = View.VISIBLE
                    binding.swapRemarkTV1.visibility = View.VISIBLE
                    binding.swapRemarkTV2.visibility = View.VISIBLE
                    binding.swapCateTV2.text = product.swapCategory
                    binding.swapRemarkTV2.text = product.swapRemark
                    binding.tradeDetailTV.text = product.swapCategory
                    binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
                }

            }
        }else{
            //not available; prompt error message
        }

        return binding.root
    }

    //set slider page
    private fun setupViewPager(productID : String?) {
        // Reference to Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference

        val productThumbnailImageRef = storageRef.child("ProductImages/$productID/Thumbnail")
        val productImagesRef = storageRef.child("ProductImages/$productID/Images")

        //thumbnail
        fetchImageUrlsFromFolder(productThumbnailImageRef)
        //images
        fetchImageUrlsFromFolder(productImagesRef)

        // After fetching all image URLs, update the adapter
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

    //get the matcg product with ID
    fun getProductFromFirebase(productID: String? = null, onResult: (Product) -> Unit) {
//        val sharedPreferencesTARSwapper =
//            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
//        val userID = sharedPreferencesTARSwapper.getString("userID", null)
        // Reference to the "Product" node in Firebase Database

        val databaseRef = FirebaseDatabase.getInstance().getReference("Product")
        val query = databaseRef.child(productID.toString()) // Query for a specific product by its productID

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Retrieve the product from the snapshot
                    val product = snapshot.getValue(Product::class.java)
                    if (product != null) {
                        onResult(product) // Wrap it in a list to match the expected result format
                        Log.d("product found", product.toString())
                    } else {
                        // If the product doesn't match criteria
                        Log.d("No matching product", "Product either doesn't exist or is created by the user.")
                    }
                } else {
                    // Handle if the product is not found in the database
                    Log.d("Product not found", "No product exists for the given productID.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun customizeDate(dateString: String): String? {
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        val dateTime = LocalDateTime.parse(dateString, isoFormatter)

        // Step 2: Define the output format you want (12-hour time with AM/PM, day/month/year)
        val outputFormat = SimpleDateFormat("h:mm a dd/MM/yyyy", Locale.getDefault())

        // Convert the LocalDateTime to a Date object
        val date = Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())

        // Step 3: Format the Date object into the desired string format
        return outputFormat.format(date)
    }

    //use to ensure all image is load
    fun fetchImageUrlsFromFolder(ref: StorageReference) {
        ref.listAll().addOnSuccessListener { listResult ->
            totalExpectedImages += listResult.items.size // Add the number of images in this folder to the total expected
            // Loop through all items (images) in this folder
            for (item in listResult.items) {
                // Get the download URL for each image
                item.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    imageUrls.add(imageUrl) // Add the URL to the list
                    // Increment the fetched image count
                    fetchedImageCount++

                    // If all images are fetched, update the adapter
                    if (fetchedImageCount == totalExpectedImages) {
                        val imageAdapter = TradeProductDetailImagesAdapter(imageUrls)
                        binding.viewPager2.adapter = imageAdapter
                        binding.slideIndicator.setViewPager2(binding.viewPager2)
                        Log.d("images", "All images fetched and adapter updated")
                    }
                }.addOnFailureListener { exception ->
                    // Handle any errors
                    Log.e("Firebase", "Error fetching image URL: ${exception.message}")
                }
            }
        }.addOnFailureListener { exception ->
            // Handle errors while listing the files
            Log.e("Firebase", "Error listing images: ${exception.message}")
        }
    }





}