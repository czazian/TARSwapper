package com.example.tarswapper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tarswapper.databinding.FragmentCoinShopBinding
import com.example.tarswapper.databinding.FragmentLoginBinding
import com.example.tarswapper.databinding.FragmentStartedBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class CoinShop : Fragment() {
    private lateinit var binding: FragmentCoinShopBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCoinShopBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE

        //Manage RecyclerView
        val recyclerView = binding.cardRecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)


        //Go to User Profile Page
        binding.backUserProfileBtn.setOnClickListener() {
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
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        return binding.root
    }

}