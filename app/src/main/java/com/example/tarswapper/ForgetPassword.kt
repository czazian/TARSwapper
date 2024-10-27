package com.example.tarswapper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tarswapper.databinding.FragmentForgetPasswordBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.random.Random

class ForgetPassword : Fragment() {
    private lateinit var binding: FragmentForgetPasswordBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentForgetPasswordBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Go to Login Page
        binding.backLoginBtn.setOnClickListener() {
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
        binding.sendOTPBtn.setOnClickListener(){
            val email = binding.txtEmailOTP.text.toString()
            sendOTPtoEmail(email)
        }


        return binding.root
    }

    private fun sendOTPtoEmail(email: String) {
        //Generate 6-digit OTP
        val otp = generateSixDigitOTP()


//        val email = emailBuilder {
//            from("no-reply@example.com")
//            to("foo@bar.com")
//
//            withSubject("Important question")
//            withPlainText("Hey, how are you doing?")
//        }



        //Make use of SendMailTask class with a callback
//        SendMailTask(email, otp) { sendStatus ->
//            if (sendStatus) {
//                //Show Successful Toast
//                Toast.makeText(
//                    requireContext(), "OTP Sent Successfully!", Toast.LENGTH_SHORT
//                ).show()
//
//                //Go to Verify OTP, if no error
//                //Create bundle for passing value
//                val bundle = Bundle()
//                bundle.putInt("otpKey", otp)
//
//                //Store the bundle into the fragment
//                val fragment = Login()
//                fragment.arguments = bundle
//
//                //Back to previous page with animation
//                val transaction = activity?.supportFragmentManager?.beginTransaction()
//                transaction?.replace(R.id.frameLayout, fragment)
//                transaction?.setCustomAnimations(
//                    R.anim.fade_out,  // Enter animation
//                    R.anim.fade_in  // Exit animation
//                )
//                transaction?.addToBackStack(null)
//                transaction?.commit()
//
//            } else {
//                //Show Unsuccessful Toast
//                Toast.makeText(
//                    requireContext(), "OTP Sent Unsuccessfully...", Toast.LENGTH_SHORT
//                ).show()
//            }
//        }.execute()


    }

    //Generate Random 6-digit OTP
    private fun generateSixDigitOTP(): Int{
        return Random.nextInt(100000, 1000000) // Generates a number between 100000 and 999999
    }


}