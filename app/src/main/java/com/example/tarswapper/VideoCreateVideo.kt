package com.example.tarswapper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.ShortVideo
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.ProductImageListAdapter
import com.example.tarswapper.databinding.FragmentCommunityCreatePostBinding
import com.example.tarswapper.databinding.FragmentVideoCreateVideoBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class VideoCreateVideo : Fragment() {
    //fragment name
    private lateinit var binding: FragmentVideoCreateVideoBinding
    private lateinit var userObj: User
    private var selectedVideoUri: Uri? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentVideoCreateVideoBinding.inflate(layoutInflater, container, false)


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
                    binding.productTagContainerContainer.visibility = View.VISIBLE
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
            binding.productTagContainerContainer.visibility = View.GONE
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
                    val newVideo = ShortVideo(
                        title = binding.titleED.text.toString(),
                        created_at = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                        created_by_UserID = userObj.userID,
                    )
                    // Add short video data to Firebase and upload video
                    addShortVideoToFirebase(newVideo)

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

    private fun addShortVideoToFirebase(video: ShortVideo) {
        val database = FirebaseDatabase.getInstance()
        val shortVideoRef = database.getReference("ShortVideo")
        val newRef = shortVideoRef.push()
        video.shortVideoID = newRef.key

        newRef.setValue(video)
            .addOnSuccessListener {
                //create product tag if have
                if(!binding.selectedProductID.text.isNullOrBlank()){
                    val shortVideoProductTagRef = database.getReference("ShortVideo/${video.shortVideoID}/ShortVideo_ProductTag")
                    shortVideoProductTagRef.push().setValue(binding.selectedProductID.text.toString())
                }

                // Upload video after data is successfully written
                uploadVideoToFirebase(selectedVideoUri!!, video.shortVideoID!!) {
                    // Only navigate after the upload is complete
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
        val fragment = Video()
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.frameLayout, fragment)
        transaction?.setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }


    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PICK_MULTIPLE_IMAGE_REQUEST = 2
        private const val REQUEST_CODE_PICK_VIDEO = 1001
    }



}