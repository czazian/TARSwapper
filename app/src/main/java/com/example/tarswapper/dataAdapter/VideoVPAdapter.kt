package com.example.tarswapper.dataAdapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tarswapper.CommunityMyPost
import com.example.tarswapper.CommunityPostExplore
import com.example.tarswapper.VideoExplore
import com.example.tarswapper.VideoMyVideo

class VideoVPAdapter  (fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2 // Number of tabs
    override fun createFragment(position: Int): Fragment {
        // Return a fragment based on the tab index
        return when (position) {
            0 -> VideoExplore()
            1 -> VideoMyVideo()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}