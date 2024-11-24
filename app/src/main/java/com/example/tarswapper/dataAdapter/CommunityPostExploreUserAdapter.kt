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
import com.example.tarswapper.R
import com.example.tarswapper.TradeProductDetail
import com.example.tarswapper.UserDetail
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.UserListHoriBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommunityPostExploreUserAdapter(private val userList: List<User>, private val context: Context) :
    RecyclerView.Adapter<CommunityPostExploreUserAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: UserListHoriBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserListHoriBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        var userObj: User

        with(holder.binding) {
            getUserRecord(user.userID.toString()) {
                if (it != null) {
                    userObj = it
                    //Meaning to say the user has record, and store as "it"
                    //Display user data
                    holder.binding.usernameTV.text = it.name.toString()
                    Glide.with(context).load(it.profileImage) // User Icon URL string
                        .into(holder.binding.profileImgV)
                }
            }

            holder.binding.userProfileLayout.setOnClickListener {
                Log.d("User Profile is clicked", "yes clicked")

                val fragment = UserDetail()

                // Create a Bundle to pass data
                val bundle = Bundle()
                bundle.putString("UserID", user.userID)

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
        }
    }

    override fun getItemCount(): Int = userList.size

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