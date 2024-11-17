package com.example.tarswapper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.ProductImageListAdapter
import com.example.tarswapper.databinding.FragmentCommunityEditPostBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class CommunityEditPost : Fragment() {
    //fragment name
    private lateinit var binding: FragmentCommunityEditPostBinding
    private lateinit var userObj: User

    private val selectedImages: MutableList<Uri> = mutableListOf()  // List to store selected images
    private lateinit var multi_image_adapter: ProductImageListAdapter
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val args = arguments
        val communityID = args?.getString("CommunityID")

        binding = FragmentCommunityEditPostBinding.inflate(layoutInflater, container, false)

        // Set up RecyclerView with GridLayoutManager
        val layoutManager = GridLayoutManager(requireContext(), 4) // 4 columns in the grid
        binding.multiImagesRecyclerView.layoutManager = layoutManager

        //load the uploaded selected images
        loadCommunityImages(communityID.toString())
        Log.d("RV resultr", selectedImages.toString())
        multi_image_adapter = ProductImageListAdapter(selectedImages)
        multi_image_adapter.notifyDataSetChanged()
        binding.multiImagesRecyclerView.adapter = multi_image_adapter
        Log.d("RV resultr", selectedImages.toString())

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

        //set form view value
        getCommunityFromFirebase(communityID = communityID) { community ->
            binding.titleED.setText(community.title)
            binding.descriptionED.setText(community.description)

            //get product tag id
            val database = FirebaseDatabase.getInstance()
            // Reference to the Community_ProductTag
            val communityProductTagRef = database.getReference("Community/$communityID/Community_ProductTag")

            // Fetch the product tags associated with the community
            communityProductTagRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // Retrieve the list of product tags
                    val productTags = dataSnapshot.children.map { it.getValue(String::class.java) }
                    binding.selectedProductID.setText(productTags.get(0))
                    binding.productTagContainer.visibility = View.VISIBLE

                    getProductFromFirebase(productID = binding.selectedProductID.text.toString()) { product ->
                        binding.productTagNameTV.text = product.name
                        getFirebaseImageUrl(product) { url ->
                            if (url != null) {
                                Glide.with(binding.productTagImgV.context)
                                    .load(url)
                                    .into(binding.productTagImgV)
                            } else {
                                // Handle the case where the image URL is not retrieved
                                binding.productTagImgV.setImageResource(R.drawable.ai) // Set a placeholder
                            }
                        }
                        if(product.tradeType == "Sale"){
                            binding.tradeDetailTV.text = "RM ${product.price}"
                            binding.tradeImg.setImageResource(R.drawable.baseline_attach_money_24)
                        } else{
                            binding.tradeDetailTV.text = product.swapCategory
                            binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
                            binding.tradeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)
                        }
                    }



                    // Use the product tags as needed
                    Log.d("Firebase", "Product Tags: $productTags")
                } else {
                    binding.selectedProductID.setText(null)
                    binding.productTagContainer.visibility = View.GONE
                    Log.d("Firebase", "No product tags found for this community.")
                }
            }.addOnFailureListener { e ->
                // Handle error
                Log.e("Firebase", "Error fetching product tags: ${e.message}")
            }
            binding.selectedProductID.setText(community.title)
        }

        binding.btnBack.setOnClickListener{
            //put bundle
            val fragment = com.example.tarswapper.Community()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        // Set up the  to open gallery for selecting multiple images
        binding.addImgBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)  // Allow multiple image selection
            startActivityForResult(intent, PICK_MULTIPLE_IMAGE_REQUEST)
        }

        //set onclick for select product
        binding.selectProductBtn.setOnClickListener{

            getUserProductsFromFirebase { productList ->

                val productNames = productList.map { it.name } // Extract the names from the products
                val namesArray = productNames.toTypedArray()  // Convert List<String> to Array<String>

                // Show the AlertDialog with the product names
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Select a Product")
                builder.setItems(namesArray) { _, position ->
                    // Handle item click, e.g., get the selected product
                    val selectedProduct = productList[position]
                    Log.d("Selected Product", selectedProduct.toString())

                    //store selected product in hidden field
                    binding.selectedProductID.setText(selectedProduct.productID)

                    //display the container
                    binding.productTagContainer.visibility = View.VISIBLE
                    binding.productTagNameTV.text = selectedProduct.name
                    if (selectedProduct.tradeType == "Sale"){
                        //is sale
                        binding.tradeDetailTV.text = "RM ${selectedProduct.price}"
                        binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        binding.tradeDetailTV.backgroundTintList = null
                        binding.tradeImg.setImageResource(R.drawable.baseline_attach_money_24)

                    } else if (selectedProduct.tradeType == "Swap"){
                        //is swap
                        binding.tradeDetailTV.text = selectedProduct.swapCategory
                        binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
                        binding.tradeDetailTV.backgroundTintList = null
                        binding.tradeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)

                    }

                    getProductThumnailImageUrl(selectedProduct) { url ->
                        if (url != null) {
                            Glide.with(binding.productTagImgV.context)
                                .load(url)
                                .into(binding.productTagImgV)
                        } else {
                            // Handle the case where the image URL is not retrieved
                            binding.productTagImgV.setImageResource(R.drawable.ai) // Set a placeholder
                        }
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.create().show()
            }

        }

        binding.tradeClearBtn.setOnClickListener{
            //gone the container
            binding.productTagContainer.visibility = View.GONE
            //store empty in hidden field
            binding.selectedProductID.setText(null)
        }

        binding.submitBtn.setOnClickListener {
            var isValid = true

            // Validate fields
            if (binding.titleED.text.isNullOrBlank()) {
                binding.titleED.error = "Title is required"
                isValid = false
            }

            if (binding.descriptionED.text.isNullOrBlank()) {
                binding.descriptionED.error = "Description is required"
                isValid = false
            }

            if (isValid) {
                val updatedCommunity = Community(
                    communityID = communityID,
                    title = binding.titleED.text.toString(),
                    description = binding.descriptionED.text.toString(),
                    //example dulu
                    status = getString(R.string.COMMUNITY_POST_PUBLIC), // Assuming status remains the same or updated based on your logic
                )

                updateCommunityInFirebase(updatedCommunity)
                Toast.makeText(context, "Community updated successfully", Toast.LENGTH_SHORT).show()

                // Optionally, navigate back or to another fragment
                val fragment = com.example.tarswapper.Community()
                val transaction = activity?.supportFragmentManager?.beginTransaction()
                transaction?.replace(R.id.frameLayout, fragment)
                transaction?.setCustomAnimations(
                    R.anim.fade_out,  // Enter animation
                    R.anim.fade_in  // Exit animation
                )
                transaction?.addToBackStack(null)
                transaction?.commit()
            } else {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
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

    fun updateCommunityInFirebase(updatedCommunity: Community) {
        // Get reference to Firebase database
        val database = FirebaseDatabase.getInstance()
        val communityRef = database.getReference("Community")

        // Use the communityID to locate the existing community post
        val communityID = updatedCommunity.communityID
        val communityPostRef = communityRef.child(communityID.toString())

        // Update the community data
        val updatedData = hashMapOf<String, Any>(
            "title" to updatedCommunity.title.toString(),
            "description" to updatedCommunity.description.toString(),
            "status" to updatedCommunity.status.toString(),
        )

        communityPostRef.updateChildren(updatedData)
            .addOnSuccessListener {
                // Data successfully updated
                Log.d("Firebase", "Community updated successfully")
                val communityProductTagRef = database.getReference("Community/${updatedCommunity.communityID}/Community_ProductTag")
                if (!binding.selectedProductID.text.isNullOrBlank()) {
                    // Remove old tags and add the new one
                    communityProductTagRef.removeValue()
                        .addOnSuccessListener {
                            val newProductTag = binding.selectedProductID.text.toString()
                            communityProductTagRef.push().setValue(newProductTag)
                                .addOnSuccessListener {
                                    Log.d("Firebase", "New product tag added successfully.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firebase", "Failed to add new product tag: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Failed to remove existing product tags: ${e.message}")
                        }
                }
                else {
                    // Remove the entire Community_ProductTag node
                    communityProductTagRef.removeValue()
                        .addOnSuccessListener {
                            Log.d("Firebase", "Community_ProductTag node removed successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Failed to remove Community_ProductTag node: ${e.message}")
                        }
                }
                // Optionally, upload new images if any (not implemented here)
                uploadCommunityImages(updatedCommunity)
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e("Firebase", "Failed to update community: ${e.message}")
            }
    }

    fun uploadCommunityImages(community: Community) {
        // Get a reference to Firebase Storage
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReference("CommunityImages")
        val imagesFolder         = storageRef.child(community.communityID + "/Images")     // Use productID to organize images in folders

        // Iterate over selected images and upload each one
        selectedImages.forEach { imageUri ->
            val imageRef = imagesFolder.child(UUID.randomUUID().toString())  // Use a unique name for each image

            // Upload the image
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    // Get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Update the product with the image URL
                        //updateProductWithImageUrl(product.productID, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    // Handle image upload failure
                    println("Image upload failed: ${e.message}")
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                // Multiple image selection
                PICK_MULTIPLE_IMAGE_REQUEST -> {
                    data?.clipData?.let { clipData ->
                        // If multiple images are selected
                        for (i in 0 until clipData.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            selectedImages.add(imageUri)
                        }
                    } ?: run {
                        // If only one image is selected (not in a ClipData)
                        data?.data?.let { imageUri ->
                            selectedImages.add(imageUri)
                        }
                    }

                    // Notify adapter that the data has changed
                    multi_image_adapter.notifyDataSetChanged()
                }
            }
        }
    }

    fun loadCommunityImages(communityID: String) {
        // Clear the list to avoid duplicates
        selectedImages.clear()

        // Get a reference to Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference
        val communityImagesRef = storageRef.child("CommunityImages/$communityID/Images")

        communityImagesRef.listAll().addOnSuccessListener { listResult ->
            if (listResult.items.isEmpty()) {
                Log.d("Firebase", "No images found in this community.")
                // Optionally, handle the case where there are no images
                return@addOnSuccessListener
            }

            // Fetch download URLs for all images
            listResult.items.forEach { item ->
                item.downloadUrl.addOnSuccessListener { uri ->
                    selectedImages.add(uri) // Add the URI to the list

                    // Check if all items are loaded
                    if (selectedImages.size == listResult.items.size) {
                        // All images loaded, now set up the adapter
                        multi_image_adapter = ProductImageListAdapter(selectedImages)
                        binding.multiImagesRecyclerView.adapter = multi_image_adapter
                        multi_image_adapter.notifyDataSetChanged() // Notify adapter about data change
                        Log.d("RV result", "Images loaded and adapter updated: $selectedImages")
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error fetching image URL: ${exception.message}")
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Error listing images: ${exception.message}")
        }
    }

    fun getUserProductsFromFirebase(onResult: (List<Product>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)
        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Product")
        val query       = databaseRef.orderByChild("created_by_UserID").equalTo(userID)

        // List to hold products retrieved from Firebase
        val productList = mutableListOf<Product>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (productSnapshot in snapshot.children) {
                        // Convert each child into a Product object
                        val product = productSnapshot.getValue(Product::class.java)
                        //get available product only
                        if (product != null && product.status == "Available") {
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


    // Function to retrieve the download URL from Firebase Storage
    fun getProductThumnailImageUrl(product: Product, onResult: (String?) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().getReference("ProductImages/" + product.productID + "/Thumbnail")

        // List all items in the folder
        storageReference.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    // Get the first file from the list of items
                    val firstImageRef = listResult.items[0]

                    // Get the download URL for the first image
                    firstImageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            onResult(uri.toString())  // Return the URL of the first image
                        }
                        .addOnFailureListener { e ->
                            onResult(null)  // In case of failure, return null
                        }
                } else {
                    // If no images exist in the folder
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                onResult(null)  // Handle any failure when listing items
            }
    }

    fun getCommunityFromFirebase(communityID: String? = null, onResult: (com.example.tarswapper.data.Community) -> Unit) {
//        val sharedPreferencesTARSwapper =
//            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
//        val userID = sharedPreferencesTARSwapper.getString("userID", null)
        // Reference to the "Product" node in Firebase Database

        val databaseRef = FirebaseDatabase.getInstance().getReference("Community")
        val query = databaseRef.child(communityID.toString()) // Query for a specific product by its productID

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Retrieve the product from the snapshot
                    val community = snapshot.getValue(com.example.tarswapper.data.Community::class.java)
                    if (community != null) {
                        onResult(community) // Wrap it in a list to match the expected result format
                        Log.d("community found", community.toString())
                    } else {
                        // If the product doesn't match criteria
                        Log.d("No matching product", "community either doesn't exist or is created by the user.")
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

    fun getFirebaseImageUrl(product: Product, onResult: (String?) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().getReference("ProductImages/" + product.productID + "/Thumbnail")

        // List all items in the folder
        storageReference.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    // Get the first file from the list of items
                    val firstImageRef = listResult.items[0]

                    // Get the download URL for the first image
                    firstImageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            onResult(uri.toString())  // Return the URL of the first image
                        }
                        .addOnFailureListener { e ->
                            onResult(null)  // In case of failure, return null
                        }
                } else {
                    // If no images exist in the folder
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                onResult(null)  // Handle any failure when listing items
            }
    }


    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PICK_MULTIPLE_IMAGE_REQUEST = 2
    }



}