package com.example.tarswapper

import CommunityMoreOperationBottomSheet
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.CommunityComment
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.CommunityCommentAdapter
import com.example.tarswapper.dataAdapter.TradeProductDetailImagesAdapter
import com.example.tarswapper.databinding.FragmentCommunityBlogDetailBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class CommunityDetail : Fragment() {

    //fragment name
    private lateinit var binding: FragmentCommunityBlogDetailBinding
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
        val communityID = args?.getString("CommunityID")

        //every time access to this page update view number
        updateViewNum(communityID)

        binding = FragmentCommunityBlogDetailBinding.inflate(layoutInflater, container, false)
        setupViewPager(communityID)


        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        val database = FirebaseDatabase.getInstance()
        val communityRef = database.getReference("Community")
        val likeRef = communityRef.child(communityID.toString()).child("Like")
        val commentRef = communityRef.child(communityID.toString()).child("Comment")
        val productTagRef = communityRef.child(communityID.toString()).child("Community_ProductTag")

        //on click
        binding.btnBack.setOnClickListener{
            val fragment = Community()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.moreOperationBtn.setOnClickListener{
            //show user operaiton such as edit & delete
            val bottomSheet = CommunityMoreOperationBottomSheet(
                onEdit = {
                    // Handle Edit operation
                    //redirect to edit page
                    val fragment = CommunityEditPost()

                    val bundle = Bundle()
                    bundle.putString("CommunityID", communityID)  // Add data to the bundle
                    fragment.arguments = bundle


                    val transaction = activity?.supportFragmentManager?.beginTransaction()
                    transaction?.replace(R.id.frameLayout, fragment)
                    transaction?.setCustomAnimations(
                        R.anim.fade_out,  // Enter animation
                        R.anim.fade_in  // Exit animation
                    )
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                },
                onDelete = {
                    // Handle Delete operation
                    //show dialog first
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Post")
                        .setMessage("Are you sure to delete this post?")
                        .setPositiveButton("Yes") { dialog, _ ->
                            // Delete the record by ID
                            communityRef.child(communityID.toString()).removeValue()
                                .addOnSuccessListener {
                                    // Deletion was successful
                                    Log.d("Firebase", "Community record deleted successfully.")
                                    Toast.makeText(
                                        requireContext(),
                                        "Community record deleted.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Redirect to the Community fragment
                                    val fragment = Community()
                                    val transaction = requireActivity().supportFragmentManager.beginTransaction()
                                    transaction.replace(R.id.frameLayout, fragment)
                                    transaction.setCustomAnimations(
                                        R.anim.fade_out, // Enter animation
                                        R.anim.fade_in   // Exit animation
                                    )
                                    transaction.addToBackStack(null)
                                    transaction.commit()
                                }
                                .addOnFailureListener { exception ->
                                    // Handle the error
                                    Log.e("Firebase", "Failed to delete community record: ${exception.message}")
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to delete community record.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            // Reference to the specific CommunityImages/$CommunityID folder
                            val storageRef = FirebaseStorage.getInstance().reference
                            val communityImagesRef = storageRef.child("CommunityImages/$communityID")

                            //delete the community images file
                            // List all files under the specified folder
                            communityImagesRef.listAll()
                                .addOnSuccessListener { listResult ->
                                    // Iterate through and delete each file
                                    for (fileRef in listResult.items) {
                                        fileRef.delete()
                                            .addOnSuccessListener {
                                                Log.d("Firebase", "File ${fileRef.name} deleted successfully.")
                                            }
                                            .addOnFailureListener { exception ->
                                                Log.e("Firebase", "Error deleting file ${fileRef.name}: ${exception.message}")
                                            }
                                    }

                                    // If there are subdirectories, log or handle them as needed
                                    for (folderRef in listResult.prefixes) {
                                        Log.d("Firebase", "Subdirectory found: ${folderRef.name}")
                                        // Recursive deletion can be implemented if needed
                                    }

                                    Toast.makeText(context, "All files deleted successfully.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("Firebase", "Error listing files: ${exception.message}")
                                    Toast.makeText(context, "Failed to delete files.", Toast.LENGTH_SHORT).show()
                                }
                            dialog.dismiss()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            // User cancels the action
                            dialog.dismiss()
                        }
                        .show()
                }
            )
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)

        }

        binding.likeBtn.setOnClickListener {
            // Check if the current user has already liked the post
            likeRef.orderByValue().equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // If user ID matches, remove the like
                        for (likeSnapshot in snapshot.children) {
                            likeSnapshot.ref.removeValue()
                                .addOnSuccessListener {
                                    Log.d("Firebase", "Like removed successfully.")
                                    binding.likeBtn.setColorFilter(Color.parseColor("#000000")) // Turn button color to black
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firebase", "Failed to remove Like: ${e.message}")
                                }
                        }
                    } else {
                        // If no match, add the like
                        likeRef.push().setValue(userID)
                            .addOnSuccessListener {
                                Log.d("Firebase", "Like added successfully.")
                                binding.likeBtn.setColorFilter(Color.parseColor("#FF0000")) // Turn button color to red
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firebase", "Failed to add Like: ${e.message}")
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error checking Likes: ${error.message}")
                }
            })
        }

        //sent comment button
        binding.sentCommentBtn.setOnClickListener{
            if(!binding.commentED.text.isNullOrBlank()){
                //push data
                val comment = CommunityComment(
                    comment = binding.commentED.text.toString(),
                    userID = userID,
                )

                commentRef.push().setValue(comment)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Comment added successfully.")
                        binding.commentED.setText("")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Failed to add Like: ${e.message}")
                    }

            }
        }

        //check availability before showing
        var available = true

        if(available){

            getCommunityFromFirebase(communityID = communityID) { community ->
                //if ppost is belong to user
                if(userID == community.created_by_UserID) {
                    // show operation button
                    binding.moreOperationBtn.visibility = View.VISIBLE
                }

                if(community.status == getString(R.string.COMMUNITY_POST_HIDE) && community.created_by_UserID != userID){
                    //not allowed to view
                    //redirect back to community
                    val fragment = Community()

                    val transaction = activity?.supportFragmentManager?.beginTransaction()
                    transaction?.replace(R.id.frameLayout, fragment)
                    transaction?.setCustomAnimations(
                        R.anim.fade_out,  // Enter animation
                        R.anim.fade_in  // Exit animation
                    )
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                    Toast.makeText(context, "You are not allowed to view this post", Toast.LENGTH_LONG).show()

                }

                //get blog sender detail
                getUserRecord(community.created_by_UserID.toString()) {
                    if (it != null) {
                        userObj = it
                        //Meaning to say the user has record, and store as "it"
                        //Display user data
                        binding.usernameTV.text = it.name.toString()
                        Glide.with(requireContext()).load(it.profileImage) // User Icon URL string
                            .into(binding.profileImgV)
                    }
                }

                binding.dateTV.text = customizeDate(community.created_at.toString())
                binding.titleTV.text = community.title
                binding.descriptionTV.text = community.description

                // Count the number of comments
                commentRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val commentCount = snapshot.childrenCount.toInt()
                            binding.commentNumTV.text = commentCount.toString()
                        } else {
                            binding.commentNumTV.text = "0"
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        println("Error: ${error.message}")
                    }
                })

                likeRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val likeCount = snapshot.childrenCount.toInt()
                            binding.likeNumTV.text = likeCount.toString()
                        } else {
                            binding.likeNumTV.text = "0"
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        println("Error: ${error.message}")
                    }
                })

                likeRef.orderByValue().equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // If userID exists in Likes, set the button tint to red
                            binding.likeBtn.setColorFilter(Color.parseColor("#FF0000"))
                        } else {
                            // If userID does not exist in Likes, set the button tint to default (e.g., black)
                            binding.likeBtn.setColorFilter(Color.parseColor("#000000"))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error checking Likes: ${error.message}")
                    }
                })


                fetchComments(community.communityID.toString()){ commentList ->
                    binding.commentRV.layoutManager = LinearLayoutManager(requireContext())
                    binding.commentRV.adapter = CommunityCommentAdapter(commentList, requireContext())
                    binding.commentTV.text = "${commentList.size} comment"
                }

                productTagRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        // Iterate over the children if it's a list of tags
                        for (tagSnapshot in snapshot.children) {
                            val productTag = tagSnapshot.value.toString()
                            Log.d("Firebase", "Community Product Tag: $productTag")
                            binding.productTagContainer.visibility = View.VISIBLE

                            getProductFromFirebase(productID = productTag) { product ->
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
                                //get product owner
                                getUserRecord(product.created_by_UserID.toString()) {
                                    if (it != null) {
                                        userObj = it
                                        //Meaning to say the user has record, and store as "it"
                                        //Display user data
                                        binding.usernameTV.text = it.name.toString()

                                    }
                                }
                            }

                        }
                    } else {
                        Log.d("Firebase", "No product tags found for this community.")
                        binding.productTagContainer.visibility = View.GONE
                    }
                }.addOnFailureListener { e ->
                    Log.e("Firebase", "Error fetching data: ${e.message}")
                }

                }
        }else{
            //not available; prompt error message
        }

        return binding.root
    }

    //set slider page
    private fun setupViewPager(communityID : String?) {
        // Reference to Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference

        val communityImagesRef = storageRef.child("CommunityImages/$communityID/Images")

        //images
        fetchImageUrlsFromFolder(communityImagesRef)
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

            if (listResult.items.isEmpty()) {
                // No images found
                Log.d("Firebase", "No images found in this folder.")
                // Optionally, update the UI to reflect no images
                binding.viewPager2.adapter = null
                binding.slideIndicator.removeAllViews() // Clear indicators
                binding.viewPager2.visibility = View.GONE
                binding.slideIndicator.visibility = View.GONE
                return@addOnSuccessListener
            }

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
                        //can share no problem
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

    fun updateViewNum(communityID: String?) {
        if (communityID == null) return

        val databaseRef = FirebaseDatabase.getInstance().getReference("Community/$communityID/view")

        databaseRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentValue = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentValue + 1 // Increment the view count
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("Firebase", "Failed to update view count: ${error.message}")
                } else if (committed) {
                    Log.d("Firebase", "View count updated successfully.")
                }
            }
        })
    }

    fun fetchComments(communityID: String, onComplete: (List<com.example.tarswapper.data.CommunityComment>) -> Unit) {
        // Reference to the Community's Comment node
        val databaseRef = FirebaseDatabase.getInstance().getReference("Community/$communityID/Comment")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commentList = mutableListOf<CommunityComment>()
                if (snapshot.exists()) {
                    // Iterate through all children (comments)
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(CommunityComment::class.java)
                        comment?.let { commentList.add(it) }
                    }
                }
                // Pass the list of comments to the callback
                onComplete(commentList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to fetch comments: ${error.message}")
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





}
