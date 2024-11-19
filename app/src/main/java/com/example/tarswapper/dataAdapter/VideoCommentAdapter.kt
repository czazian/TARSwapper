package com.example.tarswapper.dataAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.data.CommunityComment
import com.example.tarswapper.data.ShortVideoComment
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.CommunityCommentListBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class VideoCommentAdapter(private val commentList: List<ShortVideoComment>, private val context: Context) : RecyclerView.Adapter<VideoCommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(val binding: CommunityCommentListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = CommunityCommentListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun getItemCount() = commentList.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        holder.binding.commentTV.text = comment.comment

        //load user profile and username
        getUserRecord(comment.userID.toString()) { user ->
            //Meaning to say the user has record, and store as "it"
            //Display user data
            holder.binding.usernameTV.text = user!!.name.toString()
            Glide.with(context).load(user.profileImage) // User Icon URL string
                .into(holder.binding.profileImgV)
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
}

