package com.example.tarswapper

import VideoCommentBottomSheet
import android.content.Context
import android.graphics.Color
import android.net.Uri
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
import com.example.tarswapper.data.ShortVideo
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.TradeProductDetailImagesAdapter
import com.example.tarswapper.databinding.FragmentTradeProductDetailBinding
import com.example.tarswapper.databinding.VideoExploreVideoListBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
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

class VideoPreview : Fragment() {

    //fragment name
    private lateinit var binding: VideoExploreVideoListBinding
    private lateinit var userObj: User
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val args = arguments
        val shortVideoID = args?.getString("ShortVideoID")

        binding = VideoExploreVideoListBinding.inflate(layoutInflater, container, false)

        //show back button
        binding.btnBack.visibility = View.VISIBLE

        val sharedPreferencesTARSwapper =
            requireContext().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)



        getShortVideo(shortVideoID.toString()){ shortVideo ->
            getUserRecord(shortVideo.created_by_UserID.toString()) {
                if (it != null) {
                    //Meaning to say the user has record, and store as "it"
                    //Display user data
                    Glide.with(requireContext()).load(it.profileImage) // User Icon URL string
                        .into(binding.profileImgV)
                }
            }


            binding.titleTV.text = shortVideo.title
            // Setup ExoPlayer
            val player = SimpleExoPlayer.Builder(requireContext()).build()
            binding.videoPlayerView.player = player

            //like, comment, product tag
            val database = FirebaseDatabase.getInstance()
            val shortVideoRef = database.getReference("ShortVideo")
            val commentRef = shortVideoRef.child(shortVideo.shortVideoID.toString()).child("Comment")
            val likeRef = shortVideoRef.child(shortVideo.shortVideoID.toString()).child("Like")
            val productTagRef =
                shortVideoRef.child(shortVideo.shortVideoID.toString()).child("ShortVideo_ProductTag")

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

            likeRef.orderByValue().equalTo(userID).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // If userID exists in Likes, set the button tint to red
                        binding.likeBtn.setColorFilter(Color.parseColor("#FF0000"))
                    } else {
                        // If userID does not exist in Likes, set the button tint to default (e.g., black)
                        binding.likeBtn.setColorFilter(Color.parseColor("#FFFFFF"))
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
                        binding.productTagContainer.visibility = View.VISIBLE
                        binding.hideTV.visibility = View.VISIBLE

                        getProductFromFirebase(productID = productTag) { product ->
                            binding.productTagNameTV.text = product.name
                            getFirebaseImageUrl(product) { url ->
                                if (url != null) {
                                    Glide.with(binding.productTagImgV.context)
                                        .load(url)
                                        .into(binding.productTagImgV)
                                } else {
                                    // Handle the case where the image URL is not retrieved
                                    binding.productTagImgV.setImageResource(R.drawable.ai) // Set a place }
                                }
                                if (product.tradeType == "Sale") {
                                    binding.tradeDetailTV.text = "RM ${product.price}"
                                    binding.tradeImg.setImageResource(R.drawable.baseline_attach_money_24)
                                } else {
                                    binding.tradeDetailTV.text = product.swapCategory
                                    binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(
                                        R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0
                                    )
                                    binding.tradeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)
                                }

                                binding.productTagContainer.setOnClickListener {
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
                    }
                    } else {
                        Log.d("Firebase", "No product tags found for this community.")
                        binding.productTagContainer.visibility = View.GONE
                        binding.hideTV.visibility = View.GONE
                    }
                }.addOnFailureListener { e ->
                    Log.e("Firebase", "Error fetching data: ${e.message}")
                }

                binding.hideTV.setOnClickListener {
                    if (binding.hideTV.text == "Hide") {
                        binding.productTagContainer.visibility = View.GONE
                        binding.hideTV.text = "Show"
                    } else if (binding.hideTV.text == "Show") {
                        binding.productTagContainer.visibility = View.VISIBLE
                        binding.hideTV.text = "Hide"
                    }
                }

                binding.userProfileLayout.setOnClickListener {
                    val fragment = UserDetail()

                    // Create a Bundle to pass data
                    val bundle = Bundle()
                    bundle.putString("UserID", shortVideo.created_by_UserID) // Example data

                    // Set the Bundle as arguments for the fragment
                    fragment.arguments = bundle

                    val transaction =
                        (context as AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                    transaction?.replace(R.id.frameLayout, fragment)
                    transaction?.setCustomAnimations(
                        R.anim.fade_out,  // Enter animation
                        R.anim.fade_in  // Exit animation
                    )
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                }

                binding.likeBtn.setOnClickListener {
                    // Check if the current user has already liked the post
                    likeRef.orderByValue().equalTo(userID)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
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

                binding.commentBtn.setOnClickListener {
                    //show bottom sheet
                    val bottomSheet = VideoCommentBottomSheet(shortVideo.shortVideoID.toString())
                    bottomSheet.show(requireFragmentManager(), bottomSheet.tag)
                }

            // Load and play video
            getVideoUri(shortVideoID.toString()) { uri ->
                if (uri != null) {
                    val mediaItem = MediaItem.fromUri(uri)
                    player?.setMediaItem(mediaItem)
                    player?.prepare()
                    player?.playWhenReady = true
                } else {
                    Log.e("VideoAdapter", "Failed to retrieve video URI")
                }
            }

            }

        binding.btnBack.setOnClickListener{
            activity?.supportFragmentManager?.popBackStack()
        }



        return binding.root
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

    fun getShortVideo(shortVideoID: String? = null, onResult: (ShortVideo) -> Unit) {

        val databaseRef = FirebaseDatabase.getInstance().getReference("ShortVideo")
        val query =
            databaseRef.child(shortVideoID.toString()) // Query for a specific product by its productID

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Retrieve the product from the snapshot
                    val shortVideo = snapshot.getValue(ShortVideo::class.java)
                    if (shortVideo != null) {
                        onResult(shortVideo) // Wrap it in a list to match the expected result format
                        Log.d("shortVideo found", shortVideo.toString())
                    } else {
                        // If the product doesn't match criteria
                        Log.d(
                            "No matching shortVideo",
                            "shortVideo either doesn't exist or is created by the user."
                        )
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





}