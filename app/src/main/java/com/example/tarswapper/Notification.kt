package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Message
import com.example.tarswapper.dataAdapter.CoinShopAdapter
import com.example.tarswapper.dataAdapter.MessageAdapter
import com.example.tarswapper.databinding.FragmentNotificationBinding
import com.example.tarswapper.databinding.FragmentUserProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

class Notification : Fragment() {
    private lateinit var binding: FragmentNotificationBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNotificationBinding.inflate(layoutInflater, container, false)

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE


        //Go to User Profile Page
        binding.btnBackUserProfile.setOnClickListener() {
            val fragment = UserProfile()

            //Bottom Navigation Indicator Update
            val navigationView =
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            navigationView.selectedItemId = R.id.setting

            //Back to previous page with animation
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in    // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }


        //Processing
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Tab selected
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                //Selected Position of Tab
                when(tab.position){
                    //Trade
                    0 -> {

                    }
                    //Community
                    1 -> {

                    }
                    //Message
                    2 -> {
                        bindMessage(userID!!)
                    }
                    //Transaction
                    3 -> {
                        bindTransaction()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }
            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        return binding.root
    }


    //When Message Tab is Selected
    private fun bindMessage(userID: String) {
        val database = FirebaseDatabase.getInstance()

        //Get List of Data
        val messageList = mutableListOf<Message>()




        //Adapt Data
//        val adapter = MessageAdapter(requireContext(),this)
//        adapter.setData(messageList)
//        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
//        binding.notificationRecyclerView.adapter = adapter
//        binding.notificationRecyclerView.setHasFixedSize(true)
//        adapter.notifyDataSetChanged()
    }


    //When Transaction Tab is Selected
    private fun bindTransaction() {

    }
}