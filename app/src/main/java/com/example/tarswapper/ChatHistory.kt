package com.example.tarswapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Message
import com.example.tarswapper.dataAdapter.ChatHistoryAdapter
import com.example.tarswapper.databinding.FragmentChatHistoryBinding
import com.example.tarswapper.interfaces.OnChatHistoryItemClickListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class ChatHistory : Fragment(), OnChatHistoryItemClickListener {
    private lateinit var binding: FragmentChatHistoryBinding

    private lateinit var oppositeUserID: String
    private lateinit var roomID: String

    //Data
    private var date: String? = null
    private var media: String? = null


    var adapter: ChatHistoryAdapter? = null
    var messages: ArrayList<Message>? = null
    var senderRoom: String? = null
    var receiverRoom: String? = null
    var senderID: String? = null
    var receiverID: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatHistoryBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
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


        ////Pass data into Adapter////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Sender = Own
        senderID = userID

        //Receiver = Opposite
        receiverID = oppositeUserID

        ////GET LIST OF MESSAGES FROM DATABASE////
        messages = ArrayList()

        ////Database Instance////
        val database = FirebaseDatabase.getInstance()

        //Determine the Message is Sent By? and Received By?
        senderRoom = "$senderID$receiverID"
        receiverRoom = "$receiverID$senderID"

        ////Bind the messages to the RecyclerView////
        adapter = ChatHistoryAdapter(
            requireContext(),
            messages,
            senderRoom!!,
            receiverRoom!!,
            oppositeUserID!!,
            roomID!!,
            this
        )
        binding.chatHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatHistoryRecyclerView.adapter = adapter



        ////Processing////
        binding.charHistorySearch.setOnClickListener() {
            //Get Search Text
            val searchText = binding.chatHistorySearchBox.text.toString()

            //Get Messages Recycler View
            database.reference.child("Message")
                .child(senderRoom!!)
                .child("message")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //Clear all messages to avoid duplication from the previous view
                        val previousMessageCount = messages!!.size
                        messages!!.clear()

                        //Get all messages, add them into the messages list
                        for (itemSnapshot1 in snapshot.children) {
                            //Get each message as Message Obj
                            val message: Message? = itemSnapshot1.getValue(Message::class.java)

                            //Ensure message is not null
                            if (message != null) {
                                //Get the messageID under each room
                                message.messageID = itemSnapshot1.key

                                var includeMessage = true

                                //Check if searchText filter is applied and matches
                                if (searchText.isNotEmpty()) {
                                    includeMessage = includeMessage && message.message!!.contains(searchText, ignoreCase = true)
                                }

                                //Check if media filter is applied and matches
                                if (!media.isNullOrEmpty()) {
                                    val matchesMediaType = when (media) {
                                        "Image" -> message.mediaType.equals("image", ignoreCase = true)
                                        "Video" -> message.mediaType.equals("video", ignoreCase = true)
                                        else -> false
                                    }
                                    includeMessage = includeMessage && matchesMediaType
                                }

                                //Check if date filter is applied and matches
                                if (!date.isNullOrEmpty()) {
                                    val messageDate = message.dateTime?.split(" • ")?.getOrNull(1) ?: ""
                                    val matchesDate = messageDate == date
                                    includeMessage = includeMessage && matchesDate
                                }

                                //Add message if it meets all applied filters
                                if (includeMessage) {
                                    messages!!.add(message)
                                }

                                Log.e("History Message", messages.toString())
                            }
                        }

                        //After finishing adding, update the RecyclerView
                        adapter!!.notifyDataSetChanged()


                        //Update the total number of result count
                        binding.noOfResultText.text = messages!!.size.toString() + " Results"
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }


        //Calender View
        //Get today date
        val today = MaterialDatePicker.todayInUtcMilliseconds()

        //Set constraint to only allow dates up to today
        val constraints = CalendarConstraints.Builder()
            .setEnd(today)
            .build()

        //Create the date picker with the constraints
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(today)
            .setPositiveButtonText("Apply")
            .setNegativeButtonText("Remove")
            .setCalendarConstraints(constraints)
            .build()

        //Show Date Picker when calender button is clicked
        binding.chatHistoryCalender.setOnClickListener() {
            datePicker.show(requireActivity().supportFragmentManager, "tag")
        }

        //Store the selected date
        datePicker.addOnPositiveButtonClickListener { selectedDateInMilliSeconds ->
            val selectedDate = Date(selectedDateInMilliSeconds)
            val dateFormat = SimpleDateFormat("dd/M/yy", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedDate)
            date = formattedDate

            binding.txtDateSelected.visibility = View.VISIBLE
            binding.txtDateSelected.text = "Picked Date: ${date.toString()}"
        }

        //Chose to remove the date
        datePicker.addOnNegativeButtonClickListener {
            date = null
            binding.txtDateSelected.visibility = View.GONE
        }

        //Add option to spinner
        val options = listOf("", "Image", "Video")
        val spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.chatHistorySpinner.adapter = spinnerAdapter

        //When user spinner option change
        binding.chatHistorySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position) as String
                media = selectedItem
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                media = null
            }
        }

        return binding.root
    }

    override fun onItemClick(messageID: String?, oppositeUserID: String, roomID: String) {
        val bundle = Bundle().apply {
            putString("oppositeUserID", oppositeUserID)
            putString("roomID", roomID)
            putString("messageID", messageID)
        }

        val fragment = Chat().apply {
            arguments = bundle
        }

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.frameLayout, fragment)
            setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
            addToBackStack(null)
            commit()
        }
    }

}