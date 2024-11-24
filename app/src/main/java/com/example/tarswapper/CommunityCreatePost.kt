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
import com.example.tarswapper.databinding.FragmentCommunityCreatePostBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class CommunityCreatePost : Fragment() {
    //fragment name
    private lateinit var binding: FragmentCommunityCreatePostBinding
    private lateinit var userObj: User

    private val selectedImages: MutableList<Uri>          = mutableListOf()  // List to store selected images
    private lateinit var multi_image_adapter: ProductImageListAdapter
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCommunityCreatePostBinding.inflate(layoutInflater, container, false)

        // Set up RecyclerView with GridLayoutManager
        val layoutManager = GridLayoutManager(requireContext(), 4) // 4 columns in the grid
        binding.multiImagesRecyclerView.layoutManager = layoutManager
        multi_image_adapter = ProductImageListAdapter(selectedImages)
        binding.multiImagesRecyclerView.adapter = multi_image_adapter

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

        binding.btnBack.setOnClickListener{
            activity?.supportFragmentManager?.popBackStack()
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
                    binding.productOfferSwapContainer.visibility = View.VISIBLE
                    binding.productSwapNameTV.text = selectedProduct.name
                    if (selectedProduct.tradeType == "Sale"){
                        //is sale
                        binding.tradeSwapDetailTV.text = "RM ${selectedProduct.price}"
                        binding.tradeSwapDetailTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        binding.tradeSwapDetailTV.backgroundTintList = null
                        binding.tradeSwapImg.setImageResource(R.drawable.baseline_attach_money_24)

                    } else if (selectedProduct.tradeType == "Swap"){
                        //is swap
                        binding.tradeSwapDetailTV.text = selectedProduct.swapCategory
                        binding.tradeSwapDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
                        binding.tradeSwapDetailTV.backgroundTintList = null
                        binding.tradeSwapImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)

                    }

                    getProductThumnailImageUrl(selectedProduct) { url ->
                        if (url != null) {
                            Glide.with(binding.productSwapImgV.context)
                                .load(url)
                                .into(binding.productSwapImgV)
                        } else {
                            // Handle the case where the image URL is not retrieved
                            binding.productSwapImgV.setImageResource(R.drawable.ai) // Set a placeholder
                        }
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.create().show()
            }

        }

        binding.tradeSwapClearBtn.setOnClickListener{
            //gone the container
            binding.productOfferSwapContainer.visibility = View.GONE
            //store empty in hidden field
            binding.selectedProductID.setText(null)
        }

        binding.submitBtn.setOnClickListener(){
            var isValid = true
            // Check if name field is empty
            if (binding.titleED.text.isNullOrBlank()) {
                binding.titleED.error = "Title is required"
                isValid = false
            }

            if (binding.descriptionED.text.isNullOrBlank()) {
                binding.descriptionED.error = "Description is required"
                isValid = false
            }

            if (isValid) {
                //push data into firebase
                var newCommunity = Community(
                    title = binding.titleED.text.toString(),
                    description = binding.descriptionED.text.toString(),
                    view = 0,
                    //status is available by default
                    status = getString(R.string.COMMUNITY_POST_PUBLIC),
                    created_at = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                    created_by_UserID = userObj.userID,
                )

                addCommunityPostToFirebase(newCommunity)
                Toast.makeText(context, "Product created successfully", Toast.LENGTH_SHORT).show()

                //back to previous UI
                val fragment = CommunityMyPost()

                //Back to previous page with animation
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

    fun addCommunityPostToFirebase(community: Community) {
        // Get a reference to the Firebase database
        //under community add product tag
        val database = FirebaseDatabase.getInstance()
        val communityRef = database.getReference("Community")

        // Generate a unique key for the new product
        val newCommunityRef = communityRef.push()
        // Set the autogenerated product ID
        community.communityID = newCommunityRef.key

        // Push data to Firebase
        newCommunityRef.setValue(community)
            .addOnSuccessListener {
                // Data successfully written
                println("Community added successfully")

                if(!binding.selectedProductID.text.isNullOrBlank()){
                    val communityProductTagRef = database.getReference("Community/${community.communityID}/Community_ProductTag")
                    communityProductTagRef.push().setValue(binding.selectedProductID.text.toString())
                }

                //push product image to Storage
                uploadCommunityImages(community)
            }
            .addOnFailureListener { e ->
                // Handle failure
                println("Failed to add community: ${e.message}")
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


    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PICK_MULTIPLE_IMAGE_REQUEST = 2
    }



}