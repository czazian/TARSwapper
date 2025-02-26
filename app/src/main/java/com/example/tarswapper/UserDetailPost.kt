package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.CommunityMyPostAdapter
import com.example.tarswapper.databinding.FragmentCommunityMyPostBinding
import com.example.tarswapper.databinding.FragmentUserDetailPostBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserDetailPost : Fragment() {
    //fragment name
    private lateinit var binding: FragmentUserDetailPostBinding
    private lateinit var userObj: User
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentUserDetailPostBinding.inflate(layoutInflater, container, false)

        //bind recycle view

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

        //set username and image
        val args = arguments
        val userID = args?.getString("UserID")

        getUserRecord(userID.toString()) {
            if (it != null) {
                userObj = it
            }
        }

        // Set LayoutManager for RecyclerView & Call getCommunityFromFirebase to populate the RecyclerView
        binding.myPostRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        getUserCommunityFromFirebase(userID.toString()) { communityList ->
            binding.myPostRecyclerView.adapter = CommunityMyPostAdapter(communityList, requireContext())
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


    fun getUserCommunityFromFirebase(userID: String, onResult: (List<Community>) -> Unit) {
        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Community")
        val query       = databaseRef.orderByChild("created_by_UserID").equalTo(userID)

        // List to hold products retrieved from Firebase
        val communitylist = mutableListOf<com.example.tarswapper.data.Community>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (communitySnapshot in snapshot.children) {
                        // Convert each child into a Product object
                        val community = communitySnapshot.getValue(Community::class.java)
                        if (community != null) {
                            communitylist.add(community) // Add the product to the list
                        }
                    }
                    onResult(communitylist) // Return the list of products
                    Log.d("Community list found", communitylist.size.toString())
                } else {
                    // Handle empty database
                    onResult(emptyList())
                    Log.d("Empty found", communitylist.size.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
                onResult(emptyList())
            }
        })
    }


}