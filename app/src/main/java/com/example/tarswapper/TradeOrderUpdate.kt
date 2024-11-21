package com.example.tarswapper

import CommunityMoreOperationBottomSheet
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.CommunityComment
import com.example.tarswapper.data.MeetUp
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.CommunityCommentAdapter
import com.example.tarswapper.dataAdapter.TradeProductDetailImagesAdapter
import com.example.tarswapper.databinding.FragmentCommunityBlogDetailBinding
import com.example.tarswapper.databinding.FragmentTradeMeetUpBinding
import com.example.tarswapper.databinding.FragmentTradeUpdateOrderBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TradeOrderUpdate : Fragment() {

    //fragment name
    private lateinit var binding: FragmentTradeUpdateOrderBinding
    private lateinit var userObj: User

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val args = arguments
        val orderID = args?.getString("OrderID")

        binding = FragmentTradeUpdateOrderBinding.inflate(layoutInflater, container, false)

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //bind the spinner
        val adapter1 = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.meetUp_location))
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.locationSpinner.adapter = adapter1

        getOrderDetail(orderID.toString()){order ->
            binding.usernameTV.text = order?.orderID.toString()

            if(order?.tradeType == "Sale"){
                binding.tradeTypeImg.setImageResource(R.drawable.baseline_attach_money_24)

                //determine who is buyer and seller
                //user is buyer
                binding.swapContainer.visibility = View.GONE
                if(userID == order.buyerID){
                    getUserDetail(order.sellerID.toString()){
                        if (it != null) {
                            //display user data
                            binding.usernameTV.text = it.name.toString()
                            Glide.with(binding.profileImgV.context).load(it.profileImage) // User Icon URL string
                                .into(binding.profileImgV)
                        } else {
                            // Handle the case where the image URL is not retrieved
                            binding.profileImgV.setImageResource(R.drawable.ai) // Set a placeholder
                        }
                    }

                    binding.youGiveContainer.visibility = View.GONE
                    binding.youReceiveContainer.visibility = View.VISIBLE

                    getProductDetail(order.productID.toString()){ product ->
                        binding.userReceiveItemNameTV.text = "${product!!.name}  (RM ${product.price!!.format(2)})"
                        // Load image from Firebase Storage
                        getProductFirebaseImageUrl(product) { url ->
                            if (url != null) {
                                Glide.with(binding.userReceiveItemImg.context)
                                    .load(url)
                                    .into(binding.userReceiveItemImg)
                            } else {
                                // Handle the case where the image URL is not retrieved
                                binding.userReceiveItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                            }
                        }
                    }



                }else{
                    //user is seller
                    getUserDetail(order.buyerID.toString()){
                        if (it != null) {
                            //display user data
                            binding.usernameTV.text = it.name.toString()
                            Glide.with(binding.profileImgV.context).load(it.profileImage) // User Icon URL string
                                .into(binding.profileImgV)
                        } else {
                            // Handle the case where the image URL is not retrieved
                            binding.profileImgV.setImageResource(R.drawable.ai) // Set a placeholder
                        }
                    }

                    binding.youGiveContainer.visibility = View.VISIBLE
                    binding.youReceiveContainer.visibility = View.GONE

                    getProductDetail(order.productID.toString()){ product ->
                        binding.userItemNameTV.text = "${product!!.name}  (${product.price!!.format(2)})"
                        // Load image from Firebase Storage
                        getProductFirebaseImageUrl(product!!) { url ->
                            if (url != null) {
                                Glide.with(binding.userItemImg.context)
                                    .load(url)
                                    .into(binding.userItemImg)
                            } else {
                                // Handle the case where the image URL is not retrieved
                                binding.userItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                            }
                        }
                    }

                }

            }else if(order?.tradeType == "Swap"){
                binding.tradeTypeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)
                binding.swapContainer.visibility = View.VISIBLE
                binding.youReceiveContainer.visibility = View.VISIBLE
                binding.youGiveContainer.visibility = View.VISIBLE

                //determine which product is belong to user and other user
                getSwapRequest(order.swapRequestID.toString()){ swapRequest ->
                    getProductDetail(swapRequest!!.senderProductID.toString()){ product ->
                        //user product = sender product
                        if(product!!.created_by_UserID == userID){
                            binding.userItemNameTV.text = "${product!!.name}"
                            // Load image from Firebase Storage
                            getProductFirebaseImageUrl(product!!) { url ->
                                if (url != null) {
                                    Glide.with(binding.userItemImg.context)
                                        .load(url)
                                        .into(binding.userItemImg)
                                } else {
                                    // Handle the case where the image URL is not retrieved
                                    binding.userItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                                }
                            }
                            getProductDetail(swapRequest.receiverProductID.toString()){ product ->
                                binding.userReceiveItemNameTV.text = "${product!!.name}"
                                getUserDetail(product.created_by_UserID.toString()){
                                    if (it != null) {
                                        //display user data
                                        binding.usernameTV.text = it.name.toString()
                                        Glide.with(binding.profileImgV.context).load(it.profileImage) // User Icon URL string
                                            .into(binding.profileImgV)
                                    } else {
                                        // Handle the case where the image URL is not retrieved
                                        binding.profileImgV.setImageResource(R.drawable.ai) // Set a placeholder
                                    }
                                }
                                // Load image from Firebase Storage
                                getProductFirebaseImageUrl(product) { url ->
                                    if (url != null) {
                                        Glide.with(binding.userReceiveItemImg.context)
                                            .load(url)
                                            .into(binding.userReceiveItemImg)
                                    } else {
                                        // Handle the case where the image URL is not retrieved
                                        binding.userReceiveItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                                    }
                                }
                            }

                        }else {
                            //user product = receiver product
                            getProductDetail(swapRequest!!.receiverProductID.toString()) { product ->
                                binding.userItemNameTV.text = "${product!!.name}"
                                // Load image from Firebase Storage
                                getProductFirebaseImageUrl(product!!) { url ->
                                    if (url != null) {
                                        Glide.with(binding.userItemImg.context)
                                            .load(url)
                                            .into(binding.userItemImg)
                                    } else {
                                        // Handle the case where the image URL is not retrieved
                                        binding.userItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                                    }
                                }
                            }

                            getProductDetail(swapRequest.senderProductID.toString()) { product ->
                                binding.userReceiveItemNameTV.text = "${product!!.name}"
                                getUserDetail(product.created_by_UserID.toString()){
                                    if (it != null) {
                                        //display user data
                                        binding.usernameTV.text = it.name.toString()
                                        Glide.with(binding.profileImgV.context).load(it.profileImage) // User Icon URL string
                                            .into(binding.profileImgV)
                                    } else {
                                        // Handle the case where the image URL is not retrieved
                                        binding.profileImgV.setImageResource(R.drawable.ai) // Set a placeholder
                                    }
                                }
                                // Load image from Firebase Storage
                                getProductFirebaseImageUrl(product) { url ->
                                    if (url != null) {
                                        Glide.with(binding.userReceiveItemImg.context)
                                            .load(url)
                                            .into(binding.userReceiveItemImg)
                                    } else {
                                        // Handle the case where the image URL is not retrieved
                                        binding.userReceiveItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                                    }
                                }
                            }


                        }

                    }
                }
            }
            getMeetUp(order!!.meetUpID.toString()){ meetUp ->
                binding.locationSpinner.selectedItem.equals(meetUp!!.location)
                binding.venueTV.setText(meetUp.venue)
                binding.timeTV.setText(meetUp.time)
                binding.dateTV.setText(meetUp.date)
            }

            binding.updateBtn.setOnClickListener{
                val  meetUp = MeetUp(
                    meetUpID = order.meetUpID.toString(),
                    location = binding.locationSpinner.selectedItem.toString(),
                    venue = binding.venueTV.text.toString(),
                    time = binding.timeTV.text.toString(),
                    date = binding.dateTV.text.toString()
                )
                updateOrderMeetUpInFirebase(meetUp)

                Toast.makeText(context, "Order Meet Up detail updated successfully!!", Toast.LENGTH_SHORT).show()

                //put bundle
                val fragment = TradeOrder()

                val transaction = activity?.supportFragmentManager?.beginTransaction()
                transaction?.replace(R.id.frameLayout, fragment)
                transaction?.setCustomAnimations(
                    R.anim.fade_out,  // Enter animation
                    R.anim.fade_in  // Exit animation
                )
                transaction?.addToBackStack(null)
                transaction?.commit()


            }

            binding.cancelBtn.setOnClickListener{
                //show dialog first
                MaterialAlertDialogBuilder(requireContext()) // use context or activity context
                    .setTitle("Cancel Order")
                    .setMessage("Are you sure you want to cancel this order?")
                    .setPositiveButton("Yes") { dialog, which ->
                        Toast.makeText(requireContext(), "Order cancelled", Toast.LENGTH_SHORT).show()

                        //make the order cancelled and change product into "available"
                        //update status
                        updateOrderStatus(order.orderID.toString(), getString(R.string.ORDER_CANCELLED))

                        val database = FirebaseDatabase.getInstance()
                        if(order.tradeType == "Sale"){
                            val productRef = database.getReference("Product/${order.productID}")

                            productRef.child("status").setValue(getString(R.string.PRODUCT_AVAILABLE))
                                .addOnSuccessListener {
                                    println("Sender product status updated to Booked successfully.")
                                }
                                .addOnFailureListener { e ->
                                    println("Failed to update sender product status: ${e.message}")
                                }

                        }else if(order.tradeType == "Swap"){
                            getSwapRequest(order.swapRequestID.toString()){swapRequest ->
                                val senderProductRef = database.getReference("Product/${swapRequest!!.senderProductID}")
                                val receiverProductRef = database.getReference("Product/${swapRequest.receiverProductID}")

                                senderProductRef.child("status").setValue(getString(R.string.PRODUCT_AVAILABLE))
                                    .addOnSuccessListener {
                                        println("Sender product status updated to Booked successfully.")
                                    }
                                    .addOnFailureListener { e ->
                                        println("Failed to update sender product status: ${e.message}")
                                    }

                                // Update the receiver product
                                receiverProductRef.child("status").setValue(getString(R.string.PRODUCT_AVAILABLE))
                                    .addOnSuccessListener {
                                        println("Receiver product status updated to Booked successfully.")
                                    }
                                    .addOnFailureListener { e ->
                                        println("Failed to update receiver product status: ${e.message}")
                                    }
                            }
                        }
                        //redirect to
                        //put bundle
                        val fragment = TradeOrder()

                        val transaction = activity?.supportFragmentManager?.beginTransaction()
                        transaction?.replace(R.id.frameLayout, fragment)
                        transaction?.setCustomAnimations(
                            R.anim.fade_out,  // Enter animation
                            R.anim.fade_in  // Exit animation
                        )
                        transaction?.addToBackStack(null)
                        transaction?.commit()
                    }
                    .setNegativeButton("No") { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
            }


        }

        binding.btnBack.setOnClickListener{
            val fragment = TradeOrder()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.dateTV.setOnClickListener{
            // Initialize the calendar with the current date
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Create a DatePickerDialog
            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                // Handle the selected date
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                binding.dateTV.setText(selectedDate) // Update a TextView or any UI element with the selected date
            }, year, month, day)

            // Show the DatePickerDialog
            datePickerDialog.datePicker.minDate = calendar.timeInMillis // Set today as the minimum date
            datePickerDialog.show()
        }

        binding.timeTV.setOnClickListener{
            // Get the current time to set as default
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            // Create and show TimePickerDialog
            val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                // Format the time as 12-hour format with AM/PM
                val amPm = if (selectedHour < 12) "AM" else "PM"
                val hourFormatted = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val selectedTime = String.format("%02d:%02d %s", hourFormatted, selectedMinute, amPm)

                // Display the selected time in a TextView or use it as needed
                binding.timeTV.setText(selectedTime) // Replace with your TextView to display the time

            }, hour, minute, false)

            // Show the TimePickerDialog
            timePickerDialog.show()
        }



        return binding.root
    }


    fun getProductDetail(productID : String, onResult: (Product?) -> Unit) {
        //Get a reference to the database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Product").child(productID)

        //Add a listener to retrieve the user data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //Convert the snapshot to a User object
                    val product = snapshot.getValue(Product::class.java)
                    onResult(product) //Return the product record
                } else {
                    onResult(null) //User not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null) //In case of error, return null
            }
        })
    }

    fun getSwapRequest(swapRequestID : String, onResult: (SwapRequest?) -> Unit) {
        //Get a reference to the database
        val databaseRef = FirebaseDatabase.getInstance().getReference("SwapRequest").child(swapRequestID)

        //Add a listener to retrieve the user data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //Convert the snapshot to a User object
                    val swapRequest = snapshot.getValue(SwapRequest::class.java)
                    onResult(swapRequest) //Return the product record
                } else {
                    onResult(null) //User not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null) //In case of error, return null
            }
        })
    }

    fun getMeetUp(meetUpID : String, onResult: (MeetUp?) -> Unit) {
        //Get a reference to the database
        val databaseRef = FirebaseDatabase.getInstance().getReference("MeetUp").child(meetUpID)

        //Add a listener to retrieve the user data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //Convert the snapshot to a User object
                    val meetUp = snapshot.getValue(MeetUp::class.java)
                    onResult(meetUp) //Return the product record
                } else {
                    onResult(null) //User not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null) //In case of error, return null
            }
        })
    }

    fun getUserDetail(userID: String, onResult: (User?) -> Unit) {
        //Get a reference to the database
        val databaseRef = FirebaseDatabase.getInstance().getReference("User").child(userID)

        //Add a listener to retrieve the user data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //Convert the snapshot to a User object
                    val user = snapshot.getValue(User::class.java)
                    onResult(user) //Return the user record
                } else {
                    onResult(null) //User not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null) //In case of error, return null
            }
        })
    }

    // Function to retrieve the download URL from Firebase Storage for product
    fun getProductFirebaseImageUrl(product: Product, onResult: (String?) -> Unit) {
        val storageReference = FirebaseStorage.getInstance()
            .getReference("ProductImages/" + product.productID + "/Thumbnail")

        // List all items in the folder
        storageReference.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    // Get the first file from the list of items
                    val firstImageRef = listResult.items[0]

                    // Get the download URL for the first image
                    firstImageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            onResult(uri.toString())  // Return the URL of the first image
                        }
                        .addOnFailureListener { e ->
                            onResult(null)  // In case of failure, return null
                        }
                } else {
                    // If no images exist in the folder
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                onResult(null)  // Handle any failure when listing items
            }
    }


    // Function to retrieve user by receiver product ID
    //IMPORTANT
    fun getUserByReceiverProductId(senderProductID: String, onResult: (User?) -> Unit) {
        // 1. Get reference to the SwapRequest node in Firebase
        val swapRequestRef = FirebaseDatabase.getInstance().getReference("SwapRequest")

        // 2. Query SwapRequest to find the swap request with the given receiverProductID
        swapRequestRef.orderByChild("senderProductID").equalTo(senderProductID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 3. Retrieve the first SwapRequest matching the receiverProductID
                    val swapRequestSnapshot = snapshot.children.firstOrNull()
                    val receiverProductID = swapRequestSnapshot?.child("receiverProductID")?.getValue(String::class.java)
                    val senderProductID = swapRequestSnapshot?.child("senderProductID")?.getValue(String::class.java)

                    // 4. Get product details from senderProductID (assuming sender's product contains created_by_UserID)
                    getProductDetail(senderProductID!!) { product ->
                        product?.let {
                            // 5. Retrieve user information based on product's created_by_UserID
                            getUserDetail(it.created_by_UserID.toString()) { user ->
                                onResult(user) // Return the user
                            }
                        }
                    }
                } else {
                    onResult(null) // No swap request found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                println("Database error: ${error.message}")
                onResult(null)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun customizeDate(dateString: String): String? {
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        val dateTime = LocalDateTime.parse(dateString, isoFormatter)

        // Step 2: Define the output format you want (12-hour time with AM/PM, day/month/year)
        val outputFormat = SimpleDateFormat("h:mm a dd/MM/yyyy", Locale.getDefault())

        // Convert the LocalDateTime to a Date object
        val date = Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())

        // Step 3: Format the Date object into the desired string format
        return outputFormat.format(date)
    }

    fun getOrderDetail(orderID : String, onResult: (Order?) -> Unit) {
        //Get a reference to the database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Order").child(orderID)

        //Add a listener to retrieve the user data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //Convert the snapshot to a User object
                    val order = snapshot.getValue(Order::class.java)
                    onResult(order) //Return the product record
                } else {
                    onResult(null) //User not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null) //In case of error, return null
            }
        })
    }

    fun updateOrderMeetUpInFirebase(updatedMeetUp: MeetUp) {
        // Get reference to Firebase database
        val database = FirebaseDatabase.getInstance()
        val meetUpRef = database.getReference("MeetUp/${updatedMeetUp.meetUpID}")

        // Update the community data
        val updatedData = hashMapOf<String, Any>(
            "meetUpID" to updatedMeetUp.meetUpID.toString(),
            "location" to updatedMeetUp.location.toString(),
            "venue" to updatedMeetUp.venue.toString(),
            "time" to updatedMeetUp.time.toString(),
            "date" to updatedMeetUp.date.toString(),
        )

        meetUpRef.updateChildren(updatedData)
            .addOnSuccessListener {
                // Data successfully updated
                Log.d("Firebase", "Order Meet Up updated successfully")
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e("Firebase", "Failed to update community: ${e.message}")
            }
    }

    // Function to update swap request status -> Accepted or Rejected
    fun updateOrderStatus(orderID: String, newStatus: String) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Order/$orderID")

        // Updating only the status field of the swap request
        val updateMap = mapOf("status" to newStatus)

        // Use updateChildren to update specific fields
        databaseRef.updateChildren(updateMap)
            .addOnSuccessListener {
                // Success
                Log.d("Order", "Status updated successfully")
            }
            .addOnFailureListener { e ->
                // Handle error
                Log.e("Order", "Failed to update status: ${e.message}")
            }
    }

}






