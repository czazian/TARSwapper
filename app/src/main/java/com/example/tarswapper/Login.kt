package com.example.tarswapper

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.example.tarswapper.databinding.FragmentLoginBinding
import com.example.tarswapper.databinding.FragmentStartedBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.security.MessageDigest

class Login : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var storageRef: StorageReference

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)


        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE

        //Go to Register Page
        binding.loginregisterBtn.setOnClickListener() {
            val fragment = Register()

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

        //Go to Forget Password Page
        binding.forgetPasswordBtn.setOnClickListener() {
            val fragment = ForgetPassword()

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

        binding.signinBtn.setOnClickListener(){
            val userEmail = binding.txtEmailLogin.text.toString()
            val userPassword = binding.txtPasswordLogin.text.toString()

            //Verification Process
            accountVerification(userEmail, userPassword) {
                if (it != null) {
                    //Show Toast
                    Toast.makeText(
                        requireContext(),
                        "Login Successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    //Store the user id of logged on user into SharePreference (Session)
                    val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userID", it)
                    editor.apply()

                    //Redirect to UserProfile Page
                    val fragment = UserProfile()

                    //Update Bottom Navigation Selection
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

                } else {
                    //Create and show an AlertDialog to display errors
                    AlertDialog
                        .Builder(requireContext())
                        .setTitle("Errors")
                        .setMessage("Invalid email or password. Please double-check and try again!")
                        .setPositiveButton("OK") { dialog, _ ->
                            //Close dialog when OK is clicked
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                }
            }
        }

        //Control Password Visibility
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
        binding.showPassword.setOnClickListener() {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.txtPasswordLogin.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.showPassword.setImageResource(R.drawable.baseline_visibility_24)
                binding.txtPasswordLogin.typeface = typeface
            } else {
                binding.txtPasswordLogin.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.showPassword.setImageResource(R.drawable.baseline_visibility_off_24)
                binding.txtPasswordLogin.typeface = typeface
            }
        }


        return binding.root
    }

    private fun accountVerification(userEmail: String, userPassword: String, onResult: (String?) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("User")

        //Check if the user with the given email exists
        databaseRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //User exists, retrieve the user's hashed password
                    for (userSnapshot in snapshot.children) {
                        //Get the user password, from that email
                        val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java)

                        //Hash the provided password to compare
                        val hashedInputPassword = hashPassword(userPassword)

                        //Compare the hashed input password with the stored hashed password
                        if (storedHashedPassword == hashedInputPassword) {
                            val userId = userSnapshot.child("userID").getValue(String::class.java)
                            onResult(userId) // Verification successful, return user ID
                        } else {
                            //Not found the matching email + password
                            onResult(null)
                        }
                    }
                } else {
                    //No user found with the provided email
                    onResult(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null)
            }
        })
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

}