package com.example.tarswapper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.tarswapper.databinding.FragmentNavigationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior

class Navigation : Fragment() {
    private lateinit var binding: FragmentNavigationBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNavigationBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Go to Profile Page
        binding.backProfile.setOnClickListener() {
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



        //Bottom Sheet
        BottomSheetBehavior.from(binding.standardBottomSheet).apply {
            peekHeight=200
            this.state=BottomSheetBehavior.STATE_COLLAPSED
        }



        return binding.root
    }

}