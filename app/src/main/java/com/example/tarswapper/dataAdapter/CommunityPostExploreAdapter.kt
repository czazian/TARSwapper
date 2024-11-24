package com.example.tarswapper.dataAdapter

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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.CommunityDetail
import com.example.tarswapper.R
import com.example.tarswapper.TradeProductDetail
import com.example.tarswapper.UserDetail
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.CommunityBlogListBinding
import com.example.tarswapper.databinding.CommunityMyBlogListBinding
import com.example.tarswapper.databinding.FragmentCommunityBlogDetailBinding
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

class CommunityPostExploreAdapter(private val communityList: List<Community>, private val context: Context) :
    RecyclerView.Adapter<CommunityPostExploreAdapter.CommunityViewHolder>() {

    class CommunityViewHolder(val binding: CommunityBlogListBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val binding = CommunityBlogListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommunityViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        val community = communityList[position]
        var userObj: User
        val imageUrls = mutableListOf<String>()

        // A variable to keep track of the number of fetched URLs
        var fetchedImageCount = 0
        // Total expected images (You can count the number of images you expect from both folders)
        var totalExpectedImages = 0
        // Access views via binding instead of findViewById
        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)
        with(holder.binding) {
            //every time access to this page update view number
            setupViewPager(community.communityID, holder)

            holder.binding.titleTV.text = community.title
            holder.binding.descriptionTV.text = community.description
            holder.binding.dateTV.text = customizeDate(community.created_at.toString())

            val database = FirebaseDatabase.getInstance()
            val communityRef = database.getReference("Community")
            val commentRef = communityRef.child(community.communityID.toString()).child("Comment")
            val likeRef = communityRef.child(community.communityID.toString()).child("Like")
            val productTagRef = communityRef.child(community.communityID.toString()).child("Community_ProductTag")

            // Count the number of comments
            commentRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val commentCount = snapshot.childrenCount.toInt()
                        holder.binding.commentNumTV.text = commentCount.toString()
                    } else {
                        holder.binding.commentNumTV.text = "0"
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
                        holder.binding.likeNumTV.text = likeCount.toString()
                    } else {
                        holder.binding.likeNumTV.text = "0"
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
                        holder.binding.likeBtn.setColorFilter(Color.parseColor("#FF0000"))
                    } else {
                        // If userID does not exist in Likes, set the button tint to default (e.g., black)
                        holder.binding.likeBtn.setColorFilter(Color.parseColor("#000000"))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error checking Likes: ${error.message}")
                }
            })

            productTagRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Iterate over the children if it's a list of tags
                    for (tagSnapshot in snapshot.children) {
                        val productTag = tagSnapshot.value.toString()
                        Log.d("Firebase", "Community Product Tag: $productTag")
                        holder.binding.productTagContainer.visibility = View.VISIBLE

                        getProductFromFirebase(productID = productTag) { product ->
                            holder.binding.productTagNameTV.text = product.name
                            getFirebaseImageUrl(product) { url ->
                                if (url != null) {
                                    Glide.with(holder.binding.productTagImgV.context)
                                        .load(url)
                                        .into(holder.binding.productTagImgV)
                                } else {
                                    // Handle the case where the image URL is not retrieved
                                    holder.binding.productTagImgV.setImageResource(R.drawable.ai) // Set a placeholder
                                }
                            }
                            if(product.tradeType == "Sale"){
                                holder.binding.tradeDetailTV.text = "RM ${product.price}"
                                holder.binding.tradeImg.setImageResource(R.drawable.baseline_attach_money_24)
                            } else{
                                holder.binding.tradeDetailTV.text = product.swapCategory
                                holder.binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
                                holder.binding.tradeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)
                            }
                            //get product owner
                            getUserRecord(product.created_by_UserID.toString()) {
                                if (it != null) {
                                    userObj = it
                                    //Meaning to say the user has record, and store as "it"
                                    //Display user data
                                    holder.binding.usernameTV.text = it.name.toString()

                                }
                            }
                            holder.binding.productTagContainer.setOnClickListener{
                                val fragment = TradeProductDetail()

                                // Create a Bundle to pass data
                                val bundle = Bundle()
                                bundle.putString("ProductID", product.productID) // Example data

                                // Set the Bundle as arguments for the fragment
                                fragment.arguments = bundle

                                (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                                    ?.apply {
                                        replace(R.id.frameLayout, fragment)
                                        setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                                        addToBackStack(null)
                                        commit()
                                    }
                            }
                        }

                    }
                } else {
                    Log.d("Firebase", "No product tags found for this community.")
                    holder.binding.productTagContainer.visibility = View.GONE
                }
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error fetching data: ${e.message}")
            }

            //get blog sender detail
            getUserRecord(community.created_by_UserID.toString()) {
                if (it != null) {
                    userObj = it
                    //Meaning to say the user has record, and store as "it"
                    //Display user data
                    holder.binding.usernameTV.text = it.name.toString()
                    Glide.with(context).load(it.profileImage) // User Icon URL string
                        .into(holder.binding.profileImgV)
                }
            }

            //on click
            holder.binding.userProfileLayout.setOnClickListener{
                val fragment = UserDetail()

                // Create a Bundle to pass data
                val bundle = Bundle()
                bundle.putString("UserID", community.created_by_UserID) // Example data

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

            holder.binding.likeBtn.setOnClickListener {
                // Check if the current user has already liked the post
                likeRef.orderByValue().equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // If user ID matches, remove the like
                            for (likeSnapshot in snapshot.children) {
                                likeSnapshot.ref.removeValue()
                                    .addOnSuccessListener {
                                        Log.d("Firebase", "Like removed successfully.")
                                        holder.binding.likeBtn.setColorFilter(Color.parseColor("#000000")) // Turn button color to black
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
                                    holder.binding.likeBtn.setColorFilter(Color.parseColor("#FF0000")) // Turn button color to red
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

            holder.binding.constraintLayout2.setOnClickListener{
                val fragment = CommunityDetail()

                // Create a Bundle to pass data
                val bundle = Bundle()
                bundle.putString("CommunityID", community.communityID) // Example data

                // Set the Bundle as arguments for the fragment
                fragment.arguments = bundle

                (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                    ?.apply {
                        replace(R.id.frameLayout, fragment)
                        setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                        addToBackStack(null)
                        commit()
                    }
            }

        }
    }

    override fun getItemCount(): Int = communityList.size

    // Function to retrieve the download URL from Firebase Storage
    fun getFirebaseImageUrl(community : Community, onResult: (String?) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().getReference("CommunityImages/" + community.communityID + "/Images")

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

    private fun setupViewPager(communityID: String?, holder: CommunityViewHolder) {
        // Reference to Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference
        val communityImagesRef = storageRef.child("CommunityImages/$communityID/Images")

        fetchImageUrlsFromFolder(communityImagesRef, holder)
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
    private fun fetchImageUrlsFromFolder(ref: StorageReference, holder: CommunityViewHolder) {
        ref.listAll().addOnSuccessListener { listResult ->
            if (listResult.items.isEmpty()) {
                // No images found
                Log.d("Firebase", "No images found in this folder.")
                // Update UI for no images
                holder.binding.viewPager2.adapter = null
                holder.binding.viewPager2.visibility = View.GONE
                holder.binding.slideIndicator.visibility = View.GONE
                return@addOnSuccessListener
            }

            val imageUrls = mutableListOf<String>()
            val totalExpectedImages = listResult.items.size
            var fetchedImageCount = 0

            for (item in listResult.items) {
                item.downloadUrl.addOnSuccessListener { uri ->
                    imageUrls.add(uri.toString())
                    fetchedImageCount++

                    if (fetchedImageCount == totalExpectedImages) {
                        val imageAdapter = TradeProductDetailImagesAdapter(imageUrls)
                        holder.binding.viewPager2.adapter = imageAdapter
                        holder.binding.viewPager2.visibility = View.VISIBLE
                        holder.binding.slideIndicator.setViewPager2(holder.binding.viewPager2)
                        holder.binding.slideIndicator.visibility = View.VISIBLE
                        Log.d("images", "All images fetched and adapter updated")
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Firebase", "Error fetching image URL: ${exception.message}")
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Error listing images: ${exception.message}")
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
}