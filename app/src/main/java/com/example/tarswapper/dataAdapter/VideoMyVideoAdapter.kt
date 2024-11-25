package com.example.tarswapper.dataAdapter

import CommunityMoreOperationBottomSheet
import android.content.Context
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
import androidx.compose.animation.core.Transition
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.tarswapper.Community
import com.example.tarswapper.CommunityEditPost
import com.example.tarswapper.R
import com.example.tarswapper.TradeProductDetail
import com.example.tarswapper.VideoEditVideo
import com.example.tarswapper.VideoExplore
import com.example.tarswapper.VideoPreview
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.ShortVideo
import com.example.tarswapper.databinding.VideoMyVideoListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class VideoMyVideoAdapter(private val videoList: List<ShortVideo>, private val context: Context, private val fragment: Fragment) :
    RecyclerView.Adapter<VideoMyVideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(val binding: VideoMyVideoListBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = VideoMyVideoListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val shortVideo = videoList[position]
        // Access views via binding instead of findViewById
        with(holder.binding) {
            holder.binding.videoTitleTV.text = shortVideo.title.toString()
            holder.binding.uploadTV.text = customizeDate(shortVideo.created_at.toString())

            getVideoUri(shortVideo.shortVideoID.toString()){ uri->
                //holder.binding.videoImg.setImageBitmap(getFirstFrameFromVideo(context, videoUri = uri!!))
                Glide.with(context)
                    .asBitmap()
                    .load(uri) // Video URI
                    .frame(0) // Retrieves the frame at the 0ms mark (start of the video)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                        ) {
                            holder.binding.videoImg.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Handle cleanup if needed
                        }
                    })

                val durationMillis: Long = getVideoDuration(context, uri!!)

                val durationFormatted = formatDuration(durationMillis)
                holder.binding.videoDurationTV.text = durationFormatted
            }

            val database = FirebaseDatabase.getInstance()
            val shortVideoRef = database.getReference("ShortVideo")
            val commentRef = shortVideoRef.child(shortVideo.shortVideoID.toString()).child("Comment")
            val likeRef = shortVideoRef.child(shortVideo.shortVideoID.toString()).child("Like")
            val productTagRef = shortVideoRef.child(shortVideo.shortVideoID.toString()).child("ShortVideo_ProductTag")

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


            productTagRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Iterate over the children if it's a list of tags
                    for (tagSnapshot in snapshot.children) {
                        val productTag = tagSnapshot.value.toString()
                        Log.d("Firebase", "Short Video Product Tag: $productTag")
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


            holder.binding.myVideoLayout.setOnClickListener{
                //wan clicable or no, better no
                val fragment = VideoPreview()

                // Create a Bundle to pass data
                val bundle = Bundle()
                bundle.putString("ShortVideoID", shortVideo.shortVideoID) // Example data

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

            holder.binding.videoMoreOperationImgBtn.setOnClickListener{
                //show user operaiton such as edit & delete
                val bottomSheet = CommunityMoreOperationBottomSheet(
                    onEdit = {
                        // Handle Edit operation
                        //redirect to edit page
                        val fragment = VideoEditVideo()

                        val bundle = Bundle()
                        bundle.putString("ShortVideoID", shortVideo.shortVideoID)  // Add data to the bundle
                        fragment.arguments = bundle


                        val transaction =  (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
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
                        MaterialAlertDialogBuilder(context)
                            .setTitle("Delete Video")
                            .setMessage("Are you sure to delete: ${shortVideo.title}?")
                            .setPositiveButton("Yes") { dialog, _ ->
                                // Delete the record by ID
                                val shortVideoRef = database.getReference("ShortVideo")
                                shortVideoRef.child(shortVideo.shortVideoID.toString()).removeValue()
                                    .addOnSuccessListener {
                                        // Deletion was successful
                                        Log.d("Firebase", "Community record deleted successfully.")
                                        Toast.makeText(
                                            context,
                                            "Video ${shortVideo.title} is deleted.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    }
                                    .addOnFailureListener { exception ->
                                        // Handle the error
                                        Log.e("Firebase", "Failed to delete video record: ${exception.message}")
                                        Toast.makeText(
                                            context,
                                            "Failed to delete Video ${shortVideo.title}.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                // Reference to the specific CommunityImages/$CommunityID folder
                                val storageRef = FirebaseStorage.getInstance().reference
                                val shortVideoFileRef = storageRef.child("ShortVideo/${shortVideo.shortVideoID}")

                                //delete the community images file
                                // List all files under the specified folder
                                shortVideoFileRef.listAll()
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
                bottomSheet.show(fragment.parentFragmentManager, bottomSheet.tag)

            }

        }
    }

    override fun getItemCount(): Int = videoList.size


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

    fun getVideoUri(shortVideoID: String, onComplete: (Uri?) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        val videoRef = storage.reference.child("ShortVideo/$shortVideoID/video.mp4")

        videoRef.downloadUrl
            .addOnSuccessListener { uri ->
                Log.d("VideoURI", "Retrieved URI: $uri") // Debug log
                onComplete(uri)
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                Log.e("VideoURI", "Failed to retrieve URI: ${exception.message}")
                onComplete(null)
            }
    }


    fun getFirstFrameFromVideo(context: Context, videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            // Set the data source using the video URI
            retriever.setDataSource(context, videoUri)
            // Retrieve the first frame (time = 0)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release() // Always release the retriever
        }
    }

    fun getVideoDuration(context: Context, videoUri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            // Set the data source using the video URI
            retriever.setDataSource(context, videoUri)
            // Retrieve the duration in milliseconds
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L // Return 0 if an error occurs
        } finally {
            retriever.release() // Always release the retriever
        }
    }

    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

}