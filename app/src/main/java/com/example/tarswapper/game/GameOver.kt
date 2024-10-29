package com.example.tarswapper.game

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tarswapper.Login
import com.example.tarswapper.MainActivity
import com.example.tarswapper.R
import com.example.tarswapper.UserProfile
import com.example.tarswapper.databinding.FragmentGameOverBinding
import com.example.tarswapper.databinding.FragmentMiniGameBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GameOver : Fragment() {
    private lateinit var binding: FragmentGameOverBinding
    private var point: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGameOverBinding.inflate(layoutInflater, container, false)


        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Prevent back navigation when this fragment is visible
        (activity as MainActivity).setBackPressPrevented(true)


        //When is page is loaded, update the GameChance in SharedPreferences
        updateGameChance(){
            if(it){
                Toast.makeText(requireContext(), "Coin amount updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Something wrong when trying to update coin amount", Toast.LENGTH_SHORT).show()
            }
        }


        //Get Points
        arguments?.let {
            point = it.getInt("points", -1)
        }

        //Set Point
        binding.tvPoint.text = point.toString()

        //Update Coins
        val coin = updateCoin(point!!)
        binding.tvPlusCoin.text = "+${coin.toString()} coins"

        //Update User's Coin
        updateUserCoin(coin)

        //Back to User Profile
        binding.btnBackUserProfileFromGameOver.setOnClickListener(){
            val fragment = UserProfile()

            //Bottom Navigation Indicator Update
            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.setting

            //Back to previous page with animation
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.commit()
        }

        return binding.root
    }

    //Update the GameChance SharedPreference to indicate today chance is used
    private fun updateGameChance(onResult: (Boolean) -> Unit) {
        //Retrieve all necessary data from SharedPreference (TARSwapperPreferences)
        val sharedPreferencesTARSwapper = requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userIDNow = sharedPreferencesTARSwapper.getString("userID", null)

        //Firebase
        if(userIDNow != null){
            val database = FirebaseDatabase.getInstance().getReference("User").child(userIDNow)

            //Create a map for the updates
            val updates = mapOf("gameChance" to false)

            database.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Update successful
                    onResult(true)
                } else {
                    //Update failed
                    Log.e("Update Game Chance Failed","Update failed: ${task.exception?.message}")
                    onResult(false)
                }
            }
        }
    }

    //Update user coin to Firebase
    private fun updateUserCoin(coin: Int) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User")

        // Get User ID
        val userID = getUserID()

        // Get User Current Coin
        getCurrentCoin(userID) { currentCoin ->
            if (currentCoin != null) {
                val latestCoin = currentCoin + coin // Calculate latest coin amount

                // Specifies Update Column
                val update = mapOf("coinAmount" to latestCoin)

                // Check if the user with the given userID exists
                userRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnapshot in snapshot.children) {
                                // Update Process
                                userSnapshot.ref.updateChildren(update).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("UpdateUser", "Coin updated successfully")
                                    } else {
                                        Log.e("UpdateUser", "Failed to update coin: ${task.exception?.message}")
                                    }
                                }
                            }
                        } else {
                            Log.d("UpdateUser", "No user found with the given userID")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("UpdateUser", "Database error: ${error.message}")
                    }
                })
            } else {
                Log.e("UpdateUser", "Current coin retrieval failed")
            }
        }
    }



    //Get User Current Coin
    private fun getCurrentCoin(userID: String, onResult: (Long?) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("User")

        //Check if the user with the given userID exists
        databaseRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //User exists, retrieve the user's hashed password
                    for (userSnapshot in snapshot.children) {
                        val coinAmount:Long = userSnapshot.child("coinAmount").getValue(Long::class.java)!!.toLong()
                        onResult(coinAmount)
                    }
                } else {
                    //No user found with the provided email
                    onResult(0)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(0)
            }
        })
    }



    //Calculate Points
    private fun updateCoin(point: Int):Int {
        return point / 10
    }

    //Get User ID from SharePreference as String
    private fun getUserID(): String {
        val sharedPreferences =
            requireContext().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString(
            "userID", null
        )

        return userId.toString()
    }

    override fun onResume() {
        super.onResume()
        // Prevent back navigation when the GameOver fragment is visible
        (activity as MainActivity).setBackPressPrevented(true)
    }

    override fun onPause() {
        super.onPause()
        // Allow back navigation when leaving the GameOver fragment
        (activity as MainActivity).setBackPressPrevented(false)
    }
}