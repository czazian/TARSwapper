package com.example.tarswapper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tarswapper.databinding.FragmentChatBinding
import com.example.tarswapper.databinding.FragmentChatHistoryBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChatHistory : Fragment() {
    private lateinit var binding: FragmentChatHistoryBinding

    private lateinit var oppositeUserID: String
    private lateinit var roomID: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatHistoryBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Get bundle values
        arguments?.let {
            oppositeUserID = it.getString("oppositeUserID") ?: ""
            roomID = it.getString("roomID") ?: ""
        }

        //Go Back to Chat Page
        binding.backChatButton.setOnClickListener() {
            val bundle = Bundle().apply {
                putString("oppositeUserID", oppositeUserID)
                putString("roomID", roomID)
            }

            val fragment = Chat().apply {
                arguments = bundle
            }

            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.frameLayout, fragment)
                setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                commit()
            }
        }





        return binding.root
    }
}