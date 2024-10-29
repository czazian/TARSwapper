package com.example.tarswapper

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.tarswapper.databinding.FragmentForgetPasswordBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import java.util.*


class ForgetPassword : Fragment() {
    private lateinit var binding: FragmentForgetPasswordBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentForgetPasswordBinding.inflate(layoutInflater, container, false)

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
        binding.sendOTPBtn.setOnClickListener() {
            val email = binding.txtEmailOTP.text.toString()

            //User exists leh
            //Inflate the custom layout
            val dialogView = layoutInflater.inflate(R.layout.progress_bar, null)

            //Create and show an AlertDialog with the custom layout
            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false) // Prevent dismissing the dialog when touching outside
                .create()

            alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)


            if (!email.isNullOrEmpty()) {
                //Verify the email existence first
                accountVerification(email) {
                    if (it) {
                        //Send Email Processing
                        lifecycleScope.launch {
                            //Show ProgressBar
                            alertDialog.show()
                            binding.txtEmailOTP.apply {
                                isFocusable = false
                                isClickable = false
                            }

                            sendOTPtoEmail(email)

                            //Hide ProgressBar after OTP sending completes
                            alertDialog.dismiss()
                            binding.txtEmailOTP.apply {
                                isFocusable = true
                                isClickable = true
                            }
                        }

                    } else {
                        //User does not exist leh
                        //Create and show an AlertDialog to display errors
                        AlertDialog
                            .Builder(requireContext())
                            .setTitle("Error")
                            .setMessage("Email address does not exist or invalid! Please enter a valid email address and try again!")
                            .setPositiveButton("OK") { dialog, _ ->
                                //Close dialog when OK is clicked
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                    }
                }
            } else {
                //Create and show an AlertDialog to display errors
                AlertDialog
                    .Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Email cannot be empty. Please enter a valid email address and try again!")
                    .setPositiveButton("OK") { dialog, _ ->
                        //Close dialog when OK is clicked
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }

        return binding.root
    }

    private suspend fun sendOTPtoEmail(email: String) {
        // Generate 6-digit OTP
        val otp = generateSixDigitOTP()

        // Make use of SendMailTask class with a callback
        sendEmail(email, otp) { sendStatus ->
            // Switch to Main context to show Toast messages
            lifecycleScope.launch(Dispatchers.Main) {
                if (sendStatus) {
                    // Show Successful Toast
                    Toast.makeText(
                        requireContext(), "OTP Sent Successfully!", Toast.LENGTH_SHORT
                    ).show()

                    // Go to Verify OTP, if no error
                    // Create bundle for passing value
                    val bundle = Bundle()
                    bundle.putInt("otpKey", otp)
                    bundle.putString("email", email)

                    // Store the bundle into the fragment
                    val fragment = EnterOTP()
                    fragment.arguments = bundle

                    // Back to previous page with animation
                    val transaction = activity?.supportFragmentManager?.beginTransaction()
                    transaction?.replace(R.id.frameLayout, fragment)
                    transaction?.setCustomAnimations(
                        R.anim.fade_out,  // Enter animation
                        R.anim.fade_in  // Exit animation
                    )
                    transaction?.commit()
                } else {
                    // Show Unsuccessful Toast
                    Toast.makeText(
                        requireContext(), "OTP Sent Unsuccessfully...", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    //Generate Random 6-digit OTP
    private fun generateSixDigitOTP(): Int {
        return Random.nextInt(100000, 1000000) // Generates a number between 100000 and 999999
    }

    //Send email
    private suspend fun sendEmail(email: String, otp: Int, onResult: (Boolean) -> Unit) {
        val userName = "tarswapper@gmail.com"
        val password = "jkovwtdmxizglilg"
        val subject = "Verification Code: $otp"
        val text = "Hi, your verification code for account recovery is $otp"

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : jakarta.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(userName, password)
            }
        })

        withContext(Dispatchers.IO) { // Switch to IO context for network operations
            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(userName))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                    this.subject = subject
                    setText(text)
                }
                Transport.send(message)
                onResult(true)
            } catch (e: Exception) {
                Log.e("EmailError", "Error sending email: ${e.message}")
                e.printStackTrace()
                onResult(false)
            }
        }
    }


    private fun accountVerification(userEmail: String, onResult: (Boolean) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("User")

        //Check if the user with the given email exists
        databaseRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //User found with the provided email
                    onResult(true)
                } else {
                    //No user found with the provided email
                    onResult(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(false)
            }
        })
    }

}