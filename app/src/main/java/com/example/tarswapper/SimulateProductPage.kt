package com.example.tarswapper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tarswapper.databinding.FragmentSimulateProductPageBinding

class SimulateProductPage : Fragment() {
    private lateinit var binding: FragmentSimulateProductPageBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Inflate the layout for this fragment
        binding = FragmentSimulateProductPageBinding.inflate(layoutInflater, container, false)











        ////TO BE INTEGRATED IN PRODUCT PAGE////
        //This is to simulate the "Chat" button in Product Page
        binding.goChat.setOnClickListener(){
            //When ready to integrate replace this to the Product Owner User ID (It is the Opposite User ID)
            val oppositeUserID = "-OAQyscTlsEQdw_3lITE"

            val bundle = Bundle().apply {
                putString("oppositeUserID", oppositeUserID)
            }

            val fragment = Chat().apply {
                arguments = bundle
            }

            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.frameLayout, fragment)
                setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                addToBackStack(null)
                commit()
            }
        }
        ////END OF TO BE INTEGRATED IN PRODUCT PAGE////












        return binding.root
    }
}