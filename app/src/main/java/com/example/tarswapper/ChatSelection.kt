package com.example.tarswapper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tarswapper.databinding.FragmentChatSelectionBinding
import com.example.tarswapper.databinding.FragmentEditProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChatSelection : Fragment() {
    private lateinit var binding: FragmentChatSelectionBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentChatSelectionBinding.inflate(layoutInflater, container, false)


        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE



        return binding.root
    }

}