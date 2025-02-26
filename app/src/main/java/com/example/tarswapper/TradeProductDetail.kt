package com.example.tarswapper

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

//        // Set LayoutManager for RecyclerView & Call getProductsFromFirebase to populate the RecyclerView
//        binding.ProductRV.layoutManager = GridLayoutManager(requireContext(), 2)
//        //by default is filter sale only
//        getProductsFromFirebase(tradeType = "Sale") { productList ->
//            binding.ProductRV.adapter = TradeAdapter(productList)
//        }

        //on click

        binding.btnBackMyPostedProduct.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        binding.submitBtn.setOnClickListener {
            //check product availability first before submit
            checkProductStatus(
                productID = productID.toString(),
                onResult = { status ->
                    if (status != null) {
                        when (status) {
                            getString(R.string.PRODUCT_AVAILABLE) -> redirectToTradeMeetUp(productID)
                            else -> redirectToProduct404()
                        }
                    } else {
                        Log.d("ProductStatus", "Product not found.")
                    }
                },
                onError = { errorMessage ->
                    Log.e("ProductStatus", "Error: $errorMessage")
                }
            )

        }

        //check availability before showing
        var available = true

        if (available) {

            getProductFromFirebase(productID = productID) { product ->
                //hide trade button if the owner is viewing the product
                if(product.created_by_UserID == userID){
                    binding.submitBtn.visibility = View.GONE
                    binding.submitBtn.isEnabled = false
                    binding.btnChatStart.visibility = View.GONE
                    binding.youTV.visibility = View.VISIBLE
                }
                //get product owner
                getUserRecord(product.created_by_UserID.toString()) {
                    if (it != null) {
                        userObj = it
                        //Meaning to say the user has record, and store as "it"
                        //Display user data
                        binding.usernameTV.text = it.name.toString()
                        Glide.with(requireContext()).load(it.profileImage) // User Icon URL string
                            .into(binding.profileImgV)
                    }
                }

                binding.productNameTV.text = product.name
                binding.dateTV.text = customizeDate(product.created_at.toString())
                binding.descriptionTV.text = product.description
                binding.categoryTV.text = product.category
                binding.conditionTV.text = product.condition
                binding.tradeTV.text = product.tradeType

                //change the submit button text
                binding.submitBtn.text = product.tradeType!!.uppercase()

                if (product.tradeType == "Sale") {
                    //is sale
                    binding.tradeDetailTV.text = "RM ${product.price}"
                    binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    binding.tradeDetailTV.backgroundTintList = null
                    binding.swapCateTV1.visibility = View.GONE
                    binding.swapCateTV2.visibility = View.GONE
                    binding.swapRemarkTV1.visibility = View.GONE
                    binding.swapRemarkTV2.visibility = View.GONE
                } else if (product.tradeType == "Swap") {
                    //is swap
                    binding.swapCateTV1.visibility = View.VISIBLE
                    binding.swapCateTV2.visibility = View.VISIBLE
                    binding.swapRemarkTV1.visibility = View.VISIBLE
                    binding.swapRemarkTV2.visibility = View.VISIBLE
                    binding.swapCateTV2.text = product.swapCategory
                    binding.swapRemarkTV2.text = product.swapRemark
                    binding.tradeDetailTV.text = product.swapCategory
                    binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.baseline_wifi_protected_setup_24,
                        0,
                        0,
                        0
                    )
                }

                //if product is available show submit button, else make it disabled
                if(product.status != getString(R.string.PRODUCT_AVAILABLE)){
                    binding.submitBtn.text = "Not Available"
                    binding.submitBtn.isEnabled = false
                    binding.submitBtn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_font)
                }

                binding.userProfileLayout.setOnClickListener{
                    val fragment = UserDetail()

                    // Create a Bundle to pass data
                    val bundle = Bundle()
                    bundle.putString("UserID", product.created_by_UserID) // Example data

                    // Set the Bundle as arguments for the fragment
                    fragment.arguments = bundle

                    val transaction = (context as AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                    transaction?.replace(R.id.frameLayout, fragment)
                    transaction?.setCustomAnimations(
                        R.anim.fade_out,  // Enter animation
                        R.anim.fade_in  // Exit animation
                    )
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                }

            }
        } else {
            //not available; prompt error message
        }


        //Chat button on click
        binding.btnChatStart.setOnClickListener() {

            getProductFromFirebase(productID = productID) { product ->
                //Check if the product is posted by the current user itself
                if(product.created_by_UserID != userID){
                    product.created_by_UserID?.let { it1 -> startChat(it1) }
                } else {
                    //Created by the current user itself
                    Toast.makeText(requireContext(), "Sorry, this product is posted by yourself.", Toast.LENGTH_SHORT).show()
                }
            }
        }


        return binding.root
    }


    //set slider page
    private fun setupViewPager(productID: String?) {
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
        val query =
            databaseRef.child(productID.toString()) // Query for a specific product by its productID

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
                        Log.d(
                            "No matching product",
                            "Product either doesn't exist or is created by the user."
                        )
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


    ////Chat Function////
    private fun startChat(oppositeUserID: String) {

        ////TO BE INTEGRATED IN PRODUCT PAGE////
        //This is to simulate the "Chat" button in Product Page
        //When ready to integrate replace this to the Product Owner User ID (It is the Opposite User ID)
        //val oppositeUserID = "-OAQyscTlsEQdw_3lITE"

        val bundle = Bundle().apply {
            putString("oppositeUserID", oppositeUserID)
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
        ////END OF TO BE INTEGRATED IN PRODUCT PAGE////

    }

    fun checkProductStatus(productID: String, onResult: (String?) -> Unit, onError: (String) -> Unit) {
        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Product").child(productID)

        // Add a listener to retrieve the product data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Retrieve the status field from the product
                    val status = snapshot.child("status").getValue(String::class.java)
                    onResult(status) // Pass the status to the callback
                } else {
                    // Product not found
                    onResult(null)
                    Log.e("ProductStatus", "Product not found for ID: $productID")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur during the read operation
                onError(error.message)
                Log.e("Firebase", "Error fetching product status: ${error.message}")
            }
        })
    }

    fun redirectToTradeMeetUp(productID: String?){
        //redirect to meet up detail page
        val fragment = TradeMeetUp()

        val bundle = Bundle()
        bundle.putString("ProductID", productID)
        fragment.arguments = bundle

        //Bottom Navigation Indicator Update
        val navigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        navigationView.selectedItemId = R.id.tag

        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.frameLayout, fragment)
        transaction?.setCustomAnimations(
            R.anim.fade_out,  // Enter animation
            R.anim.fade_in  // Exit animation
        )
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    fun redirectToProduct404(){
        //redirect to meet up detail page
        val fragment = Product404()

        //val bundle = Bundle()
        //bundle.putString("ProductID", productID)
        //fragment.arguments = bundle

        //Bottom Navigation Indicator Update
        val navigationView =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        navigationView.selectedItemId = R.id.tag

        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.frameLayout, fragment)
        transaction?.setCustomAnimations(
            R.anim.fade_out,  // Enter animation
            R.anim.fade_in  // Exit animation
        )
        transaction?.addToBackStack(null)
        transaction?.commit()
    }



}