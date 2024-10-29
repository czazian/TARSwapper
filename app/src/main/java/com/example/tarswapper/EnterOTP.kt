package com.example.tarswapper

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tarswapper.databinding.FragmentEnterOTPBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class EnterOTP : Fragment() {
    private lateinit var binding: FragmentEnterOTPBinding
    private var otp: Int? = null
    private var email: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEnterOTPBinding.inflate(layoutInflater, container, false)

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
            transaction?.commit()
        }


        //Processing
        //Get Arguments
        arguments?.let {
            //Set -1 / null as default otp key, when no value is received
            otp = it.getInt("otpKey", -1)
            email = it.getString("email", null)
        }

        //Set the email text
        binding.enteredEmailTv.text = email

        //Verify otp correctness
        binding.verifyOTPButton.setOnClickListener() {
            if (otp != null && otp != -1) {
                val userEnteredOtpString = binding.txtVerifyOTP.text.toString()

                //Check if the text is empty
                if (userEnteredOtpString.isEmpty()) {
                    //Create and show an AlertDialog to display errors
                    AlertDialog
                        .Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("OTP cannot be empty. Please enter a valid OTP and try again!")
                        .setPositiveButton("OK") { dialog, _ ->
                            //Close dialog when OK is clicked
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                } else {
                    //Convert the String to Int
                    val userEnteredOtp: Int = userEnteredOtpString.toInt()

                    //Verify OTP
                    val correctOTP = verifyOTP(otp!!, userEnteredOtp)
                    if (correctOTP) {
                        Toast.makeText(
                            requireContext(), "Valid OTP", Toast.LENGTH_SHORT
                        ).show()

                        //Redirect to Reset Password Page when OTP is correct
                        val bundle = Bundle()
                        bundle.putString("email", email)

                        val fragment = ResetPassword()
                        fragment.arguments = bundle

                        //Back to previous page with animation
                        val transaction = activity?.supportFragmentManager?.beginTransaction()
                        transaction?.replace(R.id.frameLayout, fragment)
                        transaction?.setCustomAnimations(
                            R.anim.fade_out,  // Enter animation
                            R.anim.fade_in  // Exit animation
                        )
                        transaction?.commit()
                    } else {
                        //Create and show an AlertDialog to display errors
                        AlertDialog
                            .Builder(requireContext())
                            .setTitle("Error")
                            .setMessage("Please enter a valid OTP and try again!")
                            .setPositiveButton("OK") { dialog, _ ->
                                //Close dialog when OK is clicked
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                    }
                }
            }
        }

        return binding.root
    }


    //Verify OTP Processing
    private fun verifyOTP(otp: Int, userEnteredOtp: Int): Boolean {
        return otp == userEnteredOtp
    }

}