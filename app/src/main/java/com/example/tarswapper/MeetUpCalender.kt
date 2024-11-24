package com.example.tarswapper

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Parcel
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentMeetupCalenderBinding

class MeetUpCalender : Fragment() {
    //fragment name
    private lateinit var binding: FragmentMeetupCalenderBinding
    private lateinit var userObj: User
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentMeetupCalenderBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

}