package com.example.tarswapper

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.bumptech.glide.Glide
import com.example.tarswapper.data.MeetUp
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.TradeMyPostedProductAdapter
import com.example.tarswapper.dataAdapter.TradeProductDetailImagesAdapter
import com.example.tarswapper.databinding.FragmentTradeMeetUpBinding
import com.example.tarswapper.databinding.FragmentTradeProductDetailBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TradeMeetUp : Fragment() {

    //fragment name
    private lateinit var binding: FragmentTradeMeetUpBinding
    private lateinit var userObj: User
    private lateinit var product_viewing: Product
    val imageUrls = mutableListOf<String>()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val args = arguments
        val productID = args?.getString("ProductID")

        binding = FragmentTradeMeetUpBinding.inflate(layoutInflater, container, false)

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

//        // Set LayoutManager for RecyclerView & Call getProductsFromFirebase to populate the RecyclerView
//        binding.ProductRV.layoutManager = GridLayoutManager(requireContext(), 2)
//        //by default is filter sale only
//        getProductsFromFirebase(tradeType = "Sale") { productList ->
//            binding.ProductRV.adapter = TradeAdapter(productList)
//        }

        //on click

        //bind the spinner
        val adapter1 = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.tarumt_kl_campus))
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.venueSpinner.adapter = adapter1

        //bind product spinner
//        val adapter2 = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.tarumt_kl_campus))
//        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.productSpinner.adapter = adapter2

        binding.btnBack.setOnClickListener{
            //put bundle
            val fragment = TradeProductDetail()
            val bundle = Bundle()
            bundle.putString("ProductID", productID) // Add any data you want to pass
            fragment.arguments = bundle

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        binding.selectDateBtn.setOnClickListener{
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

        binding.selectTimeBtn.setOnClickListener{
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

        binding.selectProductBtn.setOnClickListener{

            getUserProductsFromFirebase { productList ->

                val productNames = productList.map { it.name } // Extract the names from the products
                val namesArray = productNames.toTypedArray()  // Convert List<String> to Array<String>

                // Show the AlertDialog with the product names
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Select a Product")
                builder.setItems(namesArray) { _, position ->
                    // Handle item click, e.g., get the selected product
                    val selectedProduct = productList[position]
                    Log.d("Selected Product", selectedProduct.toString())

                    //store selected product in hidden field
                    binding.selectedProductID.setText(selectedProduct.productID)
                    binding.productSelectErrorMsg.visibility = View.INVISIBLE

                    //display the container
                    binding.productOfferSwapContainer.visibility = View.VISIBLE
                    binding.productSwapNameTV.text = selectedProduct.name
                    if (selectedProduct.tradeType == "Sale"){
                        //is sale
                        binding.tradeSwapDetailTV.text = "RM ${selectedProduct.price}"
                        binding.tradeSwapDetailTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        binding.tradeSwapDetailTV.backgroundTintList = null
                        binding.tradeSwapImg.setImageResource(R.drawable.baseline_attach_money_24)

                    } else if (selectedProduct.tradeType == "Swap"){
                        //is swap
                        binding.tradeSwapDetailTV.text = selectedProduct.swapCategory
                        binding.tradeSwapDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
                        binding.tradeSwapDetailTV.backgroundTintList = null
                        binding.tradeSwapImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)

                    }

                    getFirebaseImageUrl(selectedProduct) { url ->
                        if (url != null) {
                            Glide.with(binding.productSwapImgV.context)
                                .load(url)
                                .into(binding.productSwapImgV)
                        } else {
                            // Handle the case where the image URL is not retrieved
                            binding.productSwapImgV.setImageResource(R.drawable.ai) // Set a placeholder
                        }
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.create().show()
            }

        }

        binding.tradeSwapClearBtn.setOnClickListener{
            //gone the container
            binding.productOfferSwapContainer.visibility = View.GONE
            //store empty in hidden field
            binding.selectedProductID.setText(null)
        }

        binding.submitBtn.setOnClickListener{
            //date and time cannot be null
            var isValid = true
            var available = true
            // Check if name field is empty
            if (binding.timeTV.text.isNullOrBlank()) {
                binding.timeTV.error = "Please select time"
                isValid = false
            }

            if (binding.dateTV.text.isNullOrBlank()) {
                binding.dateTV.error = "Please select date"
                isValid = false
            }


            if(binding.swapContainer.visibility == View.VISIBLE && binding.selectedProductID.text.isNullOrBlank()){
                binding.productSelectErrorMsg.visibility = View.VISIBLE
                isValid = false
            }

            //all valid, insert and update data
            if(isValid && available){
                val viewing_prodID = productID

                getProductFromFirebase(productID = viewing_prodID) { product ->
                    val database = FirebaseDatabase.getInstance()
                    val productRef = database.getReference("Product/${product.productID}")
                    val meetUpRef = database.getReference("MeetUp")
                    val orderRef = database.getReference("Order")
                    val swapRequestRef = database.getReference("SwapRequest")

                    // create meet up
                    //both sale & swap also need to create meetUp
                    var meetUp = MeetUp(
                        date = binding.dateTV.text.toString(),
                        time = binding.timeTV.text.toString(),
                        location = binding.locationTV.text.toString(),
                        venue = binding.venueSpinner.selectedItem.toString(),
                    )
                    val newMeetUpRef = meetUpRef.push()
                    meetUp.meetUpID = newMeetUpRef.key
                    // Push meetUp to Firebase
                    newMeetUpRef.setValue(meetUp)
                        .addOnSuccessListener {println("Meet up added successfully") }
                        .addOnFailureListener { e -> println("Failed to add meet up: ${e.message}")}

                    if (product.tradeType == "Sale"){
                        //create order
                        //product status -> Booked
                        var order = Order(
                            tradeType = product.tradeType,
                            status = getString(R.string.ORDER_ONGOING),
                            createdAt = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                            productID = product.productID,
                            meetUpID = meetUp.meetUpID,
                        )
                        val newOrderRef = orderRef.push()
                        order.orderID = newOrderRef.key
                        // Push order to Firebase
                        newOrderRef.setValue(order)
                            .addOnSuccessListener {println("Order added successfully") }
                            .addOnFailureListener { e -> println("Failed to add order: ${e.message}")}

                        //update product to BOOKED
                        product.status = getString(R.string.PRODUCT_BOOKED)
                        productRef.setValue(product)
                            .addOnSuccessListener {println("Product status update to BOOKED successfully") }
                            .addOnFailureListener { e -> println("Failed to update product: ${e.message}")}

                    }
                    else if (product.tradeType == "Swap"){
                        //create swap request
                        //product
                        //if swap (involve 2 product)
                        //swap request with status AwaitingResponse

                        var swapRequest = SwapRequest(
                            status = getString(R.string.SWAP_REQUEST_AWAITING_RESPONSE),
                            receiverProductID = viewing_prodID,
                            //selected product id for swap
                            senderProductID = binding.selectedProductID.text.toString(),
                            created_at = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                            meetUpID = meetUp.meetUpID,
                        )

                        val newSwapRequestRef = swapRequestRef.push()
                        swapRequest.swapRequestID = newSwapRequestRef.key
                        // Push swap request to Firebase
                        newSwapRequestRef.setValue(swapRequest)
                            .addOnSuccessListener {println("Swap Request added successfully") }
                            .addOnFailureListener { e -> println("Failed to add Swap Request: ${e.message}")}
                    }
                }

                //redirect to puchase successful page
                val fragment = BuySuccess()
                val bundle = Bundle()
                bundle.putString("TradeType", product_viewing.tradeType) // Add any data you want to pass
                fragment.arguments = bundle

                //Bottom Navigation Indicator Update
                val navigationView =
                    requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                navigationView.selectedItemId = R.id.setting

                val transaction = activity?.supportFragmentManager?.beginTransaction()
                transaction?.replace(R.id.frameLayout, fragment)
                transaction?.setCustomAnimations(
                    R.anim.fade_out,  // Enter animation
                    R.anim.fade_in  // Exit animation
                )
                transaction?.addToBackStack(null)
                transaction?.commit()
            }


        }

        //check availability before showing
        var available = true

        if(available){

            //set product detail and owner
            getProductFromFirebase(productID = productID) { product ->

                product_viewing = product
                //get product owner
                getUserRecord(product.created_by_UserID.toString()) {
                    if (it != null) {
                        userObj = it
                        //Meaning to say the user has record, and store as "it"
                        //Display user data
                        binding.usernameTV.text = it.name.toString()
                        Glide.with(requireContext()).load(it.profileImage) // User Icon URL string
                            .into(binding.profileImgV)
                    }
                }

                binding.productNameTV.text = product.name

                if (product.tradeType == "Sale"){
                    //is sale
                    binding.title1TV.text = "Sale"
                    binding.title2TV.text = "Meet Up"
                    binding.tradeDetailTV.text = "RM ${product.price}"
                    binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    binding.tradeDetailTV.backgroundTintList = null
                    binding.tradeImg.setImageResource(R.drawable.baseline_attach_money_24)
                    binding.swapContainer.visibility = View.GONE
                    binding.submitBtn.text = "BUY"

                } else if (product.tradeType == "Swap"){
                    //is swap
                    binding.title1TV.text = "Swap"
                    binding.title2TV.text = "Wait for accepted and Meet Up!"
                    binding.tradeDetailTV.text = product.swapCategory
                    binding.tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
                    binding.tradeDetailTV.backgroundTintList = null
                    binding.tradeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)
                    binding.swapContainer.visibility = View.VISIBLE
                    binding.submitBtn.text = "Sent Swap Request"

                }

                getFirebaseImageUrl(product) { url ->
                    if (url != null) {
                        Glide.with(binding.productImgV.context)
                            .load(url)
                            .into(binding.productImgV)
                    } else {
                        // Handle the case where the image URL is not retrieved
                        binding.productImgV.setImageResource(R.drawable.ai) // Set a placeholder
                    }
                }

            }
        }else{
            //not available; prompt error message
        }
        return binding.root
    }

    // Function to retrieve the download URL from Firebase Storage
    fun getFirebaseImageUrl(product: Product, onResult: (String?) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().getReference("ProductImages/" + product.productID + "/Thumbnail")

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

    private fun getUserRecord(userID: String, onResult: (User?) -> Unit) {
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

    //get owner product and status = available
    fun getUserProductsFromFirebase(onResult: (List<Product>) -> Unit) {
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)
        // Reference to the "Product" node in Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("Product")
        val query       = databaseRef.orderByChild("created_by_UserID").equalTo(userID)

        // List to hold products retrieved from Firebase
        val productList = mutableListOf<Product>()

        // Add a listener to retrieve data
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Loop through the products in the snapshot
                    for (productSnapshot in snapshot.children) {
                        // Convert each child into a Product object
                        val product = productSnapshot.getValue(Product::class.java)
                        //get available product only
                        if (product != null && product.status == "Available") {
                            productList.add(product) // Add the product to the list
                        }
                    }
                    onResult(productList) // Return the list of products
                    Log.d("product list found", productList.size.toString())
                } else {
                    // Handle empty database
                    onResult(emptyList())
                    Log.d("Empty found", productList.size.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
                onResult(emptyList())
            }
        })
    }

    //get the matcg product with ID
    fun getProductFromFirebase(productID: String? = null, onResult: (Product) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Product")
        val query = databaseRef.child(productID.toString()) // Query for a specific product by its productID

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Retrieve the product from the snapshot
                    val product = snapshot.getValue(Product::class.java)
                    if (product != null) {
                        onResult(product) // Wrap it in a list to match the expected result format
                        Log.d("product found", product.toString())
                    } else {
                        // If the product doesn't match criteria
                        Log.d("No matching product", "Product either doesn't exist or is created by the user.")
                    }
                } else {
                    // Handle if the product is not found in the database
                    Log.d("Product not found", "No product exists for the given productID.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                println("Error fetching data: ${error.message}")
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





}