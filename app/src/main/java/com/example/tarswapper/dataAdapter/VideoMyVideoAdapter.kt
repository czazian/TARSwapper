package com.example.tarswapper.dataAdapter

import android.content.Context
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
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.ShortVideo
import com.example.tarswapper.databinding.VideoMyVideoListBinding
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

class VideoMyVideoAdapter(private val videoList: List<ShortVideo>, private val context: Context) :
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
//                //wan clicable or no, better no
//                val fragment = CommunityDetail()
//
//                // Create a Bundle to pass data
//                val bundle = Bundle()
//                bundle.putString("CommunityID", community.communityID) // Example data
//
//                // Set the Bundle as arguments for the fragment
//                fragment.arguments = bundle
//
//                (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
//                    ?.apply {
//                        replace(R.id.frameLayout, fragment)
//                        setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
//                        addToBackStack(null)
//                        commit()
//                    }
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

}