package com.example.tarswapper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tarswapper.databinding.FragmentBuySuccessBinding
import com.example.tarswapper.databinding.FragmentMyPostedProductBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class BuySuccess : Fragment() {
    private lateinit var binding: FragmentBuySuccessBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentBuySuccessBinding.inflate(layoutInflater, container, false)

        val tradeType = arguments?.getString("TradeType")

        if(tradeType == "Sale"){
            binding.successMessage.text = "Buy Successfully!"
            binding.thankYouMessage.text = "Ready for meet up"
            binding.successAnimation.setAnimation(R.raw.check_animation)
        }else if (tradeType == "Swap"){
            binding.successMessage.text = "Swap Request Sent Successfully!"
            binding.thankYouMessage.text = "Wait for Accepted"
            binding.successAnimation.setAnimation(R.raw.sent_success)
        }

        binding.successAnimation.playAnimation()

        binding.backToHomeButton.setOnClickListener{
            //redirect to puchase successful page
            val fragment = Trade()

            //Bottom Navigation Indicator Update
            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.setting

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        return binding.root

    }

}