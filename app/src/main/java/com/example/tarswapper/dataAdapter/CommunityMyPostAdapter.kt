package com.example.tarswapper.dataAdapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.CommunityDetail
import com.example.tarswapper.R
import com.example.tarswapper.TradeProductDetail
import com.example.tarswapper.data.Community
import com.example.tarswapper.databinding.CommunityMyBlogListBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class CommunityMyPostAdapter(private val communityList: List<Community>, private val context: Context) :
    RecyclerView.Adapter<CommunityMyPostAdapter.CommunityViewHolder>() {

    class CommunityViewHolder(val binding: CommunityMyBlogListBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val binding = CommunityMyBlogListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommunityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        val community = communityList[position]
        // Access views via binding instead of findViewById
        with(holder.binding) {

            holder.binding.titleTV.text = community.title
            holder.binding.viewnumTV.text = community.view.toString()

            val database = FirebaseDatabase.getInstance()
            val communityRef = database.getReference("Community")
            val commentRef = communityRef.child(community.communityID.toString()).child("Comment")
            val likeRef = communityRef.child(community.communityID.toString()).child("Like")

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

            // Load image from Firebase Storage
            getFirebaseImageUrl(community) { url ->
                if (url != null) {
                    Glide.with(imgV.context)
                        .load(url)
                        .into(imgV)
                } else {
                    // Handle the case where the image URL is not retrieved
                    imgV.setImageResource(R.drawable.ai) // Set a placeholder
                }
            }

            holder.binding.myPostLayout.setOnClickListener{
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
}