package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.CommunityMyPostAdapter
import com.example.tarswapper.dataAdapter.TradeMyPostedProductAdapter
import com.example.tarswapper.databinding.FragmentCommunityMyPostBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommunityMyPost : Fragment() {
    //fragment name
    private lateinit var binding: FragmentCommunityMyPostBinding
    private lateinit var userObj: User
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCommunityMyPostBinding.inflate(layoutInflater, container, false)

        //bind recycle view

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

        //bind spinner
        // Initialize Spinner and Status List
        val displaySortList = listOf("Relevant", "Popularity", "Latest")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, displaySortList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusSpinner.adapter = adapter

        binding.statusSpinner.setSelection(0)

        // Set LayoutManager for RecyclerView & Call getCommunityFromFirebase to populate the RecyclerView
/*        binding.myPostRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        getUserCommunityFromFirebase { communityList ->
            binding.myPostRecyclerView.adapter = CommunityMyPostAdapter(communityList, requireContext())
        }*/

        binding.statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Call getUserProductsFromFirebase with the selected status
                getUserCommunityFromFirebase(position) { communityList ->
                    // Update RecyclerView with filtered products
                    binding.myPostRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
                    binding.myPostRecyclerView.adapter = CommunityMyPostAdapter(communityList, requireContext())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle no selection scenario if needed
            }
        }





        binding.addPostBtn.setOnClickListener{
            val fragment = CommunityCreatePost()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.viewReport.setOnClickListener{
            val fragment = ReportCommunity()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
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


    fun getUserCommunityFromFirebase(position: Int, onResult: (List<Community>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        // Reference to the "Community" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Community")
        val query = databaseRef.orderByChild("created_by_UserID").equalTo(userID)

        // List to hold communities retrieved from Firebase
        val communityList = mutableListOf<Community>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (communitySnapshot in snapshot.children) {
                        // Convert each child into a Community object
                        val community = communitySnapshot.getValue(Community::class.java)
                        if (community != null) {
                            communityList.add(community) // Add the community to the list
                        }
                    }

                    // Sort based on the position
                    val sortedCommunityList = when (position) {
                        0 -> communityList // No sorting (default order)
                        1 -> communityList.sortedByDescending { it.view } // Sort by views descending
                        2 -> communityList.sortedByDescending { it.created_at } // Sort by created_at descending
                        else -> communityList // Default, no sorting
                    }

                    // Return the sorted list
                    onResult(sortedCommunityList)
                    Log.d("Community list found", sortedCommunityList.size.toString())
                } else {
                    // Handle empty database
                    onResult(emptyList())
                    Log.d("Empty found", communityList.size.toString())
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