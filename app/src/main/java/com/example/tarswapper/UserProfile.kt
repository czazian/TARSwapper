package com.example.tarswapper

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.marginTop
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentCoinShopBinding
import com.example.tarswapper.databinding.FragmentLoginBinding
import com.example.tarswapper.databinding.FragmentStartedBinding
import com.example.tarswapper.databinding.FragmentUserProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserProfile : Fragment() {
    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var userObj: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUserProfileBinding.inflate(layoutInflater, container, false)

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

        //Go to Edit Profile Page
        binding.btnEditProfile.setOnClickListener() {
            val fragment = EditProfile()

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
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        //Go to Notification Page
        binding.btnNotification.setOnClickListener() {
            val fragment = Notification()

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
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        //Go to Coin Shop Page
        binding.coinShop.setOnClickListener() {
            val fragment = CoinShop()

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
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        //Go to Chat bot Page
        binding.aichatbot.setOnClickListener() {
            val fragment = AIChatbot()

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
            transaction?.addToBackStack(null)
            transaction?.commit()
        }


        //Processing
        //Logout - Clear all SharePreference, including the UserID
        binding.logout.setOnClickListener() {
            //Show Toast
            Toast.makeText(
                requireContext(), "Logout Successfully!", Toast.LENGTH_LONG
            ).show()

            ///////////////////////////Clear SharedPreference Here/////////////////////////////
            //Clear all
            val sharedPreferences = requireContext().getSharedPreferences(
                "TARSwapperPreferences", Context.MODE_PRIVATE
            )
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            //After clearing, redirect user to login.
            val fragment = Login()

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
            //Clear all back stack
            activity?.supportFragmentManager?.popBackStack(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            transaction?.commit()
        }

        //Retrieve User Data
        //Get User ID first leh
        val userID: String = getUserID()

        //Get User Record as returned by obj
        getUserRecord(userID) {
            if (it != null) {
                userObj = it
                //Meaning to say the user has record, and store as "it"
                //Display user data
                binding.username.text = it.name.toString()
                binding.useremail.text = it.email.toString()
                Glide.with(requireContext()).load(it.profileImage) // User Icon URL string
                    .into(binding.userLoggedIcon)

            }
        }

        //Deactivate Account
        binding.deactivateAcc.setOnClickListener() {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Deactivate Account Confirmation")
                .setMessage("Are you sure you want to deactivate your account?")
                .setCancelable(false)
                .setPositiveButton("Confirm") { dialog: DialogInterface, which: Int ->

                    //Handle the confirm action
                    updateAccountStatusToDeactivate(userID) {
                        if (it) {
                            //Show Toast
                            Toast.makeText(
                                requireContext(), "Deactivated Successfully!", Toast.LENGTH_LONG
                            ).show()


                            ///////////////////////////Clear SharedPreference Here/////////////////////////////
                            //Clear all
                            val sharedPreferences = requireContext().getSharedPreferences(
                                "TARSwapperPreferences", Context.MODE_PRIVATE
                            )
                            val editor = sharedPreferences.edit()
                            editor.clear()
                            editor.apply()


                            //After clearing, redirect user to login.
                            val fragment = Login()

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
                            //Clear all back stack
                            transaction?.commit()
                        }
                    }

                }.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int ->
                    //Handle the cancel action
                    dialog.cancel()
                }

            // Create and show the AlertDialog
            val alertDialog = builder.create()
            alertDialog.show()
        }


        binding.playMiniGame.setOnClickListener() {
            //After clearing, redirect user to login.
            val fragment = MiniGame()

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
            //Clear all back stack
            transaction?.commit()
        }

        binding.orderStatistic.setOnClickListener(){
            val fragment = ReportTrade()

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
            //Clear all back stack
            transaction?.commit()

        }


        //Display selected user title and profile background
        queryUserTitleAndProfileBackground(userID)

        return binding.root
    }

    private fun queryUserTitleAndProfileBackground(userID: String) {
        val database = FirebaseDatabase.getInstance().reference
        val purchasedItemsRef = database.child("PurchasedItem")
        val itemRef = database.child("Items")

        //Step 1: Load items from the Items table
        itemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(itemsSnapshot: DataSnapshot) {
                //Create a map to store itemID and itemType for quick lookup
                val itemTypeMap = mutableMapOf<Int, String>()
                for (itemSnapshot in itemsSnapshot.children) {
                    val itemID = itemSnapshot.child("itemID").getValue(Int::class.java)
                    val itemType = itemSnapshot.child("itemType").getValue(String::class.java)
                    if (itemID != null && itemType != null) {
                        itemTypeMap[itemID] = itemType
                    }
                }

                //Step 2: Query purchased items by userID
                purchasedItemsRef.orderByChild("userID").equalTo(userID)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (itemSnapshot in snapshot.children) {
                                val itemID = itemSnapshot.child("itemID").getValue(Int::class.java)
                                val equipped = itemSnapshot.child("equipped").getValue(Boolean::class.java)

                                //Step 3: Check if the item is equipped and retrieve item type
                                if (equipped == true && itemID != null) {
                                    val itemType = itemTypeMap[itemID]
                                    //Check if the item type is "title"
                                    if (itemType == "title") {
                                        displayUserTitle(itemID)
                                        //Check if the item type is "background"
                                    } else if(itemType == "background"){
                                        displayUserProfileBackground(itemID)
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "FirebaseError",
                                "Error querying purchased items: ${error.message}"
                            )
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error loading items: ${error.message}")
            }
        })
    }

    private fun displayUserTitle(itemID: Int) {
        val database = FirebaseDatabase.getInstance().reference
        val itemsRef = database.child("Items")

        itemsRef.child(itemID.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val itemURL = snapshot.child("itemURL").getValue(String::class.java)
                    //Display the user title
                    if (itemURL != null) {
                        Glide.with(requireContext()).load(itemURL)
                            .into(binding.usertitle)
                        binding.usertitle.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching user title: ${error.message}")
                }
            })
    }

    private fun displayUserProfileBackground(itemID: Int) {
        val database = FirebaseDatabase.getInstance().reference
        val itemsRef = database.child("Items")

        itemsRef.child(itemID.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val itemURL = snapshot.child("itemURL").getValue(String::class.java)
                    //Display the user profile background
                    if (itemURL != null) {
                        Glide.with(requireContext()).load(itemURL)
                            .into(binding.userBg)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching user background: ${error.message}")
                }
            })
    }


    //Update the record of the account wanted to deactivate to all empty values, and set the isActivate to false
    private fun updateAccountStatusToDeactivate(userID: String, onResult: (Boolean) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("User")

        val updatedUser = User(
            userID = userID,
            name = "",
            email = "",
            password = "",
            profileImage = "",
            joinedDate = "",
            coinAmount = 0,
            isActive = false,
            gameChance = false,
            lastPlayDate = ""
        )

        dbRef.child(userID).setValue(updatedUser).addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }


    //Retrieve user record processing
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


    //Get User ID from SharePreference as String
    private fun getUserID(): String {
        val sharedPreferences =
            requireContext().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString(
            "userID", null
        ) // Retrieve user ID, default is null if not found

        return userId.toString()
    }


}