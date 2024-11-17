package com.example.tarswapper.dataAdapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tarswapper.CommunityMyPost
import com.example.tarswapper.CommunityPostExplore

class CommunityVPAdapter  (fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2 // Number of tabs
    override fun createFragment(position: Int): Fragment {
        // Return a fragment based on the tab index
        return when (position) {
            0 -> CommunityPostExplore()
            1 -> CommunityMyPost()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}