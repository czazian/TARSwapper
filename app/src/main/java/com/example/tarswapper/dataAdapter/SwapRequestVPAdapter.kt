package com.example.tarswapper.dataAdapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tarswapper.TradeSwapRequestHistory
import com.example.tarswapper.TradeSwapRequestReceived
import com.example.tarswapper.TradeSwapRequestSent

class SwapRequestVPAdapter (fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3 // Number of tabs
    override fun createFragment(position: Int): Fragment {
        // Return a fragment based on the tab index
        return when (position) {
            0 -> TradeSwapRequestReceived()
            1 -> TradeSwapRequestSent()
            2 -> TradeSwapRequestHistory()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}