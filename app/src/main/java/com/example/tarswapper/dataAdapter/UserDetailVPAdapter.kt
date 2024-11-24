package com.example.tarswapper.dataAdapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tarswapper.CommunityMyPost
import com.example.tarswapper.CommunityPostExplore
import com.example.tarswapper.UserDetailPost
import com.example.tarswapper.UserDetailProduct

class UserDetailVPAdapter  (fragment: FragmentActivity, private val userID: String) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2 // Number of tabs
    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle().apply {
            putString("UserID", userID) // Replace with your key-value pairs
        }

        // Return a fragment based on the tab index
        return when (position) {
            0 -> UserDetailPost().apply { arguments = bundle }
            1 -> UserDetailProduct().apply { arguments = bundle }
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}