package com.example.tarswapper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.ShortVideo
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentVideoEditVideoBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class VideoEditVideo : Fragment() {
    //fragment name
    private lateinit var binding: FragmentVideoEditVideoBinding
    private lateinit var userObj: User
    private var selectedVideoUri: Uri? = null
    private var newVideo = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentVideoEditVideoBinding.inflate(layoutInflater, container, false)

        val args = arguments
        val shortVideoID = args?.getString("ShortVideoID")


        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //get short video id

        getUserRecord(userID.toString()) {
            if (it != null) {
                userObj = it
            }
        }

        getVideoUri(shortVideoID.toString()){ uri->
            selectedVideoUri = uri
            //holder.binding.videoImg.setImageBitmap(getFirstFrameFromVideo(context, videoUri = uri!!))
            Glide.with(requireContext())
                .asBitmap()
                .load(uri) // Video URI
                .frame(0) // Retrieves the frame at the 0ms mark (start of the video)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        binding.uploadVideoBtn.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle cleanup if needed
                    }
                })
        }


        getShortVideoFromFirebase(shortVideoID.toString()){ shortVideo ->
            binding.titleED.setText(shortVideo.title)

            val database = FirebaseDatabase.getInstance()
            val shortVideoProductTagRef = database.getReference("ShortVideo/${shortVideo.shortVideoID}/ShortVideo_ProductTag")

            // Fetch the product tags associated with the community
            shortVideoProductTagRef.get().addOnSuccessListener { dataSnapshot ->
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

        binding.btnBack.setOnClickListener{
            activity?.supportFragmentManager?.popBackStack()
        }

        binding.uploadVideoBtn.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "video/*"
            startActivityForResult(intent, REQUEST_CODE_PICK_VIDEO)
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

            // Validate that the title is not empty
            val title = binding.titleED.text.toString().trim()
            if (title.isEmpty()) {
                binding.titleED.error = "Title is required"
                isValid = false
            } else {
                binding.titleED.error = null
            }

            // Validate that a video is selected
            if (selectedVideoUri == null) {
                Toast.makeText(context, "Please select a video before submitting.", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            // Add your validation checks here
            if (isValid) {
                if (selectedVideoUri != null) {
                    val updatedVideo = ShortVideo(
                        shortVideoID = shortVideoID,
                        title = binding.titleED.text.toString(),
                    )
                    // Add short video data to Firebase and upload video
                    updateShortVideoToFirebase(updatedVideo)

                } else {
                    Toast.makeText(context, "Please select a video before submitting.", Toast.LENGTH_SHORT).show()
                }
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

    private fun updateShortVideoToFirebase(video: ShortVideo) {
        val database = FirebaseDatabase.getInstance()
        val shortVideoRef = database.getReference("ShortVideo/${video.shortVideoID}")

        // Update the community data
        val updatedData = hashMapOf<String, Any>(
            "title" to video.title.toString(),
        )

        shortVideoRef.updateChildren(updatedData)
            .addOnSuccessListener {
                //create product tag if have
                Toast.makeText(context, "Short Video edit successfully.", Toast.LENGTH_SHORT).show()
                val shortVideoProductTagRef = database.getReference("ShortVideo/${video.shortVideoID}/ShortVideo_ProductTag")
                if (!binding.selectedProductID.text.isNullOrBlank()) {
                    // Remove old tags and add the new one
                    shortVideoProductTagRef.removeValue()
                        .addOnSuccessListener {
                            val newProductTag = binding.selectedProductID.text.toString()
                            shortVideoProductTagRef.push().setValue(newProductTag)
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
                    shortVideoProductTagRef.removeValue()
                        .addOnSuccessListener {
                            Log.d("Firebase", "Community_ProductTag node removed successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Failed to remove Community_ProductTag node: ${e.message}")
                        }
                }

                // Upload video after data is successfully written

                if(newVideo == true){
                    uploadVideoToFirebase(selectedVideoUri!!, video.shortVideoID!!) {
                        // Only navigate after the upload is complete
                        navigateToPreviousFragment()
                    }
                }else{
                    navigateToPreviousFragment()
                }

            }
            .addOnFailureListener { e ->
                println("Failed to add video: ${e.message}")
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
    private fun uploadVideoToFirebase(videoUri: Uri, shortVideoID: String, onUploadComplete: () -> Unit) {
        val progressContainer = binding.progressContainer
        val progressBar = binding.uploadProgressBar
        val progressPercentage = binding.progressPercentage

        // Show progress container
        progressContainer.visibility = View.VISIBLE

        // Disable submit button during upload
        binding.submitBtn.isEnabled = false
        binding.submitBtn.text = "Video is uploading. Please wait..."

        // Get Firebase Storage reference
        val storage = FirebaseStorage.getInstance()
        val storageRef =
            storage.reference.child("ShortVideo/$shortVideoID/video.mp4") // File path based on shortVideoID

        // Start the upload
        val uploadTask = storageRef.putFile(videoUri)

        // Monitor the upload process
        uploadTask.addOnProgressListener { snapshot ->
            val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
            progressBar.progress = progress
            progressPercentage.text = "$progress%"
            Log.d("Firebase", "Upload is $progress% done")
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                Log.d("Firebase", "Video uploaded successfully: $downloadUri")
                Toast.makeText(context, "Video uploaded successfully!", Toast.LENGTH_SHORT).show()

                // Hide progress container and enable submit button
                progressContainer.visibility = View.GONE
                binding.submitBtn.isEnabled = true

                // Notify upload completion
                onUploadComplete()
            }
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Failed to upload video: ${exception.message}")
            Toast.makeText(context, "Failed to upload video.", Toast.LENGTH_SHORT).show()

            // Hide progress container and re-enable submit button
            progressContainer.visibility = View.GONE
            binding.submitBtn.isEnabled = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_VIDEO && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val videoUri: Uri? = data.data
            if (videoUri != null) {
                Log.d("VideoPicker", "Selected video URI: $videoUri")

                // Save the URI
                selectedVideoUri = videoUri

                // Extract and set the thumbnail to the ImageButton
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, videoUri)
                    val thumbnail = retriever.frameAtTime // Extract the first frame
                    binding.uploadVideoBtn.setImageBitmap(thumbnail) // Set the thumbnail
                    newVideo = true
                } catch (e: Exception) {
                    Log.e("VideoPicker", "Failed to retrieve video thumbnail: ${e.message}")
                } finally {
                    retriever.release()
                }
            } else {
                Log.e("VideoPicker", "Video URI is null")
            }
        }
    }

    private fun navigateToPreviousFragment() {
        activity?.supportFragmentManager?.popBackStack()
    }

    fun getShortVideoFromFirebase(shortVideoID : String, onResult: (ShortVideo) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("ShortVideo")
        val query = databaseRef.child(shortVideoID.toString()) // Query for a specific product by its productID

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Retrieve the product from the snapshot
                    val shortVideo = snapshot.getValue(com.example.tarswapper.data.ShortVideo::class.java)
                    if (shortVideo != null) {
                        onResult(shortVideo) // Wrap it in a list to match the expected result format
                        Log.d("community found", shortVideo.toString())
                    } else {
                        // If the product doesn't match criteria
                        Log.d("No matching shortVideo", "community either doesn't exist or is created by the user.")
                    }
                } else {
                    // Handle if the product is not found in the database
                    Log.d("shortVideo not found", "No product exists for the given productID.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
            }
        })
    }

    fun getVideoUri(shortVideoID: String, onComplete: (Uri?) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        val videoRef = storage.reference.child("ShortVideo/$shortVideoID/video.mp4")

        videoRef.downloadUrl
            .addOnSuccessListener { uri ->
                // Successfully retrieved the URI
                onComplete(uri)
            }
            .addOnFailureListener { exception ->
                // Handle the error
                exception.printStackTrace()
                onComplete(null)
            }
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
        private const val REQUEST_CODE_PICK_VIDEO = 1001
    }



}