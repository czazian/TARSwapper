package com.example.tarswapper

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentRegisterBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Register : Fragment() {
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var storageRef: StorageReference

    private var isPassword1Visible = false
    private var isPassword2Visible = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE

        //Go to Login Page
        binding.registerloginBtn.setOnClickListener() {
            val fragment = Login()

            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.chat

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
        //Database Initialization
        dbRef = FirebaseDatabase.getInstance().getReference()
        storageRef = FirebaseStorage.getInstance().getReference()

        //Show Passwords
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
        binding.showPassword1.setOnClickListener() {
            isPassword1Visible = !isPassword1Visible
            if (isPassword1Visible) {
                binding.txtPasswordReg.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.showPassword1.setImageResource(R.drawable.baseline_visibility_24)
                binding.txtPasswordReg.typeface = typeface
            } else {
                binding.txtPasswordReg.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.showPassword1.setImageResource(R.drawable.baseline_visibility_off_24)
                binding.txtPasswordReg.typeface = typeface
            }
        }
        binding.showPassword2.setOnClickListener() {
            isPassword2Visible = !isPassword2Visible
            if (isPassword2Visible) {
                binding.txtPasswordConfirmReg.inputType =
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.showPassword2.setImageResource(R.drawable.baseline_visibility_24)
                binding.txtPasswordConfirmReg.typeface = typeface
            } else {
                binding.txtPasswordConfirmReg.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.showPassword2.setImageResource(R.drawable.baseline_visibility_off_24)
                binding.txtPasswordConfirmReg.typeface = typeface
            }
        }

        //On Click
        binding.signupBtn.setOnClickListener() {
            //Get Data
            val userName = binding.txtUsernameReg.text.toString()
            val userEmail = binding.txtEmailReg.text.toString()
            val userPassword = binding.txtPasswordReg.text.toString()
            val userPasswordConfirmation = binding.txtPasswordConfirmReg.text.toString()

            //Validation
            val errors = mutableListOf<String>()

            //Check if username is empty
            if (userName.isEmpty()) {
                errors.add("Username cannot be empty.")
            }

            //Check if email is valid, then check if it is exists in the database
            //Check Email Valid
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                errors.add("Please enter a valid email address.")
            } else if (userEmail.isEmpty()) {
                errors.add("Email address cannot be empty.")
            }

            //Check if password meets length requirements
            if (userPassword.length < 6) {
                errors.add("Password must be at least 6 characters.")
            } else if (userPassword.isEmpty()) {
                errors.add("Password cannot be empty.")
            }

            //Check if Confirm password
            if (userPasswordConfirmation.isEmpty()) {
                errors.add("Confirm password cannot be empty.")
            }


            //Check if passwords match
            if (userPassword != userPasswordConfirmation) {
                errors.add("Passwords do not match.")
            }


            //Search Email Existence
            searchEmail(userEmail) { isFound ->
                if (isFound) {
                    errors.add("Email has already exist.")
                }

                //Display errors if any exist
                if (errors.isNotEmpty()) {
                    val message = errors.joinToString("\n") { "• $it" }

                    // Create and show an AlertDialog to display errors
                    AlertDialog
                        .Builder(requireContext())
                        .setTitle("Errors")
                        .setMessage(message)
                        .setPositiveButton("OK") { dialog, _ ->
                            //Close dialog when OK is clicked
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                } else {
                    //Insert into database if all correct
                    registration(userName, userEmail, userPassword)
                }
            }
        }

        return binding.root
    }


    //Hash is one-way. For verification, hash the user entered password, then compared with the password in Database.
    private fun hashPassword(password: String): String {
        //Create a MessageDigest instance for SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        //Hash the password
        val hashedBytes = digest.digest(password.toByteArray())
        //Convert the hashed bytes to a hexadecimal string
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    //Search email existence
    private fun searchEmail(userEmail: String, onResult: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("User")

        database.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Check if any user with the given email exists
                if (snapshot.exists()) {
                    //User with the specified email found
                    onResult(true)
                } else {
                    //No user with the specified email found
                    onResult(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle potential errors
                println("Database error: ${error.message}")
                onResult(false) // You might want to handle this differently
            }
        })
    }

    //Register Process
    private fun registration(userName: String, userEmail: String, userPassword: String) {
        val database = FirebaseDatabase.getInstance()

        //Generate Random Key as ID
        val userID = database.getReference("generateKey").push().key

        //Hash Password
        var hashedPassword = hashPassword(userPassword)

        //Now Date
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val joinedDate = dateFormat.format(currentDate)


        //Get default user icon
        val defaultUserIcon =
            "https://firebasestorage.googleapis.com/v0/b/tarswapper-d4b2a.appspot.com/o/UserProfile%2Fdefaulticon.png?alt=media&token=a43ee24d-7d1b-411a-95b2-fa56ac5debef"

        //Create Object
        val user = User(
            userID,
            userName,
            userEmail,
            hashedPassword,
            defaultUserIcon,
            joinedDate,
            0,
            true
        )

        //Insert into DB under parent "User"
        dbRef = database.getReference("User")
        dbRef.child(userID.toString()).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "User has inserted successfully!",
                    Toast.LENGTH_LONG
                ).show()
            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "User has fail inserted!",
                    Toast.LENGTH_LONG
                ).show()
            }


        //After successful adding, redirect to Login
        val fragment = Login()

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
}
