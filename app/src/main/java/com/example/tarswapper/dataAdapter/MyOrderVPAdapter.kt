package com.example.tarswapper.dataAdapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tarswapper.TradeOrderCancelled
import com.example.tarswapper.TradeOrderCompleted
import com.example.tarswapper.TradeOrderOnGoing

class MyOrderVPAdapter (fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 3 // Number of tabs

        override fun createFragment(position: Int): Fragment {
            // Return a fragment based on the tab index
            return when (position) {
                0 -> TradeOrderOnGoing()
                1 -> TradeOrderCompleted()
                2 -> TradeOrderCancelled()
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
