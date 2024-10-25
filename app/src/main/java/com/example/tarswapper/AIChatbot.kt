package com.example.tarswapper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tarswapper.databinding.ActivityMainBinding
import com.example.tarswapper.databinding.FragmentAIChatbotBinding
import com.example.tarswapper.databinding.FragmentChatHistoryBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class AIChatbot : Fragment() {
    private lateinit var binding: FragmentAIChatbotBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAIChatbotBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        return binding.root
    }

}