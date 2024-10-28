package com.example.tarswapper

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.example.tarswapper.databinding.FragmentResetPasswordBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest

class ResetPassword : Fragment() {
    private lateinit var binding: FragmentResetPasswordBinding
    private var email: String? = null
    private var isPassword1Visible = false
    private var isPassword2Visible = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentResetPasswordBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Go to Login Page
        binding.backLoginBtn.setOnClickListener() {
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


        //Processing
        //Show Passwords
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
        binding.showPassword1.setOnClickListener() {
            isPassword1Visible = !isPassword1Visible
            if (isPassword1Visible) {
                binding.txtPasswordReset.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.showPassword1.setImageResource(R.drawable.baseline_visibility_24)
                binding.txtPasswordReset.typeface = typeface
            } else {
                binding.txtPasswordReset.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.showPassword1.setImageResource(R.drawable.baseline_visibility_off_24)
                binding.txtPasswordReset.typeface = typeface
            }
        }
        binding.showPassword2.setOnClickListener() {
            isPassword2Visible = !isPassword2Visible
            if (isPassword2Visible) {
                binding.txtPasswordResetConfirm.inputType =
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.showPassword2.setImageResource(R.drawable.baseline_visibility_24)
                binding.txtPasswordResetConfirm.typeface = typeface
            } else {
                binding.txtPasswordResetConfirm.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.showPassword2.setImageResource(R.drawable.baseline_visibility_off_24)
                binding.txtPasswordResetConfirm.typeface = typeface
            }
        }

        binding.resetPasswordBtn.setOnClickListener() {
            //Get Email to Update
            arguments?.let {
                //Get to know which email to update
                email = it.getString("email", null)
            }

            //If email is not empty/null, then proceed
            if (!email.isNullOrEmpty()) {
                val password = binding.txtPasswordReset.text.toString()
                val confirmPassword = binding.txtPasswordResetConfirm.text.toString()

                val match = verifyPasswordMatch(password, confirmPassword)
                if (!match) {
                    //Create and show an AlertDialog to display errors
                    AlertDialog
                        .Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("Password do not match! Please enter matching passwords and try again!")
                        .setPositiveButton("OK") { dialog, _ ->
                            //Close dialog when OK is clicked
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                } else if (password.isNullOrEmpty() || confirmPassword.isNullOrEmpty()) {
                    //Create and show an AlertDialog to display errors
                    AlertDialog
                        .Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("Password or Confirm Password cannot be empty.")
                        .setPositiveButton("OK") { dialog, _ ->
                            //Close dialog when OK is clicked
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                } else {
                    //All correct
                    //Hash password
                    val hashedPassword = hashPassword(password)

                    //Specify the specific column of a record to update
                    val update = mapOf("password" to hashedPassword)

                    //Update Password to Firebase
                    updatePassword(email!!, update)
                }
            }
        }

        return binding.root
    }

    //Update to Firebase
    private fun updatePassword(email: String, update: Map<String, String>) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User")

        userRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        userSnapshot.ref.updateChildren(update).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    requireContext(), "Password Update Successfully!", Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(), "Password Update Unsuccessfully!", Toast.LENGTH_SHORT
                                ).show()
                            }

                            //Redirect to login page after processing
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
                } else {
                    Log.d("UpdateUser", "No user found with the given email")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UpdateUser", "Database error: ${error.message}")
            }
        })
    }

    private fun verifyPasswordMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
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