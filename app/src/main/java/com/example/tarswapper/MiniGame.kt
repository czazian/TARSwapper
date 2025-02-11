package com.example.tarswapper

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentMiniGameBinding
import com.example.tarswapper.game.GameView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MiniGame : Fragment() {
    private lateinit var binding: FragmentMiniGameBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMiniGameBinding.inflate(layoutInflater, container, false)


        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Prevent back navigation when this fragment is visible
        (activity as MainActivity).setBackPressPrevented(true)



        binding.btnBackUserProfileFromGame.setOnClickListener() {
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

        //Check game chance and reset it if necessary
        checkGameChanceAndUpdate()

        //Display current chance status
        updateGameChanceDisplay()

        //Start Game
        binding.btnStartGame.setOnClickListener() {
            //Check Chance to Play
            checkChance() { hasChance ->
                if (hasChance) {
                    //Disable the start button to prevent re-click
                    binding.btnStartGame.isEnabled = false

                    //Prevent back navigation once the game starts
                    (activity as MainActivity).setBackPressPrevented(true)

                    val gameView = GameView(requireContext())
                    binding.gameViewContainer.visibility = View.VISIBLE
                    binding.gameViewContainer.addView(gameView)
                } else {
                    //Create and show an AlertDialog to display errors
                    AlertDialog
                        .Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("You do not have chance to play the game today. Please try again tomorrow!")
                        .setPositiveButton("OK") { dialog, _ ->
                            //Close dialog when OK is clicked
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                }
            }
        }

        return binding.root
    }

    //Update the UI with the current game chance status
    private fun updateGameChanceDisplay() {
        checkChance { hasChance ->
            binding.remainingChance.text = if (hasChance) "1" else "0"
        }
    }

    //Check GameChance
    private fun checkGameChanceAndUpdate() {
        //Retrieve all necessary data from SharedPreference (TARSwapperPreferences)
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Get Today Date
        val dateNow = getCurrentDateString()

        if (userID != null) {
            getUserById(userID) { user ->
                if (dateNow != user!!.lastPlayDate && user!!.userID == userID) {
                    //Specify the specific column of a record to update
                    val updates =
                        mapOf("lastPlayDate" to getCurrentDateString(), "gameChance" to true)

                    //Update lastPlayDate & chance
                    updateGamePlay(userID, updates)
                }
            }
        }
    }

    //Update lastPlayDate & chance when a new day
    private fun updateGamePlay(userID: String, updates: Map<String, Any>) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User")

        userRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        userSnapshot.ref.updateChildren(updates).addOnCompleteListener { task ->
                            if(task.isSuccessful){
                                updateGameChanceDisplay()
                            }
                        }
                    }
                } else {
                    Log.d("UpdateUser", "Error when updating game chance & lastPlayDate")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UpdateUser", "Database error: ${error.message}")
            }
        })
    }

    //Check Daily Chance to Play
    private fun checkChance(onResult: (Boolean) -> Unit) {
        //Retrieve all necessary data from SharedPreference (TARSwapperPreferences)
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userIDNow = sharedPreferencesTARSwapper.getString("userID", null)

        //Testing
        Log.e("CheckSharedPreference", "TARSwapperPreferences userIDNow = $userIDNow")

        if (userIDNow != null) {
            //Retrieve the user object
            getUserById(userIDNow!!) { user ->
                //Processing
                if (user!!.userID == userIDNow && user.gameChance == true) {
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        }

    }

    private fun getUserById(userId: String, onResult: (User?) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("User").child(userId)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Check if the user exists
                if (snapshot.exists()) {
                    //Map the snapshot to the User data class
                    val user = snapshot.getValue(User::class.java)
                    onResult(user)
                } else {
                    //User not found
                    onResult(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                //Handle potential errors
                println("Database error: ${error.message}")
                onResult(null)
            }
        })
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //Allow back navigation again when fragment is no longer visible
        (activity as MainActivity).setBackPressPrevented(false)
    }

    //Get Today Date
    private fun getCurrentDateString(): String {
        val currentDate = Date() // Get the current date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(currentDate)
    }
}