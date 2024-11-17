package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.ChatRoom
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.ChatSelectionAdapter
import com.example.tarswapper.dataAdapter.CommunityVPAdapter
import com.example.tarswapper.dataAdapter.SwapRequestVPAdapter
import com.example.tarswapper.databinding.FragmentChatSelectionBinding
import com.example.tarswapper.databinding.FragmentCommunityBinding
import com.example.tarswapper.databinding.FragmentTradeBinding
import com.example.tarswapper.databinding.FragmentTradeSwapRequestBinding
import com.example.tarswapper.databinding.FragmentVideoExploreBinding
import com.example.tarswapper.interfaces.OnUserContactClick
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Community : Fragment() {
    //fragment name
    private lateinit var binding: FragmentCommunityBinding
    private lateinit var userObj: User
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCommunityBinding.inflate(layoutInflater, container, false)

        // Set the adapter for ViewPager2
        val adapter = CommunityVPAdapter(requireActivity())
        binding.viewPager.adapter = adapter

        // Use TabLayoutMediator to bind TabLayout with ViewPager2
        TabLayoutMediator(binding.headerTab, binding.viewPager) { tab, position ->
            // Set tab text from TabItem
            tab.text = when (position) {
                0 -> "Explore"
                1 -> "My Post"
                else -> null
            }
        }.attach()

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


}