package com.example.tarswapper.dataAdapter

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.data.Notification
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.TradeSwapRequestReceivedListBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.tarswapper.TradeProductDetail
import com.example.tarswapper.UserDetail

class TradeSwapRequestReceivedAdapter(private var swapRequestList: List<SwapRequest>, private val context: Context) :
    RecyclerView.Adapter<TradeSwapRequestReceivedAdapter.SwapRequestViewHolder>() {

    class SwapRequestViewHolder(val binding: TradeSwapRequestReceivedListBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwapRequestViewHolder {
        val binding = TradeSwapRequestReceivedListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SwapRequestViewHolder(binding)
    }

    override fun getItemCount(): Int = swapRequestList.size

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: SwapRequestViewHolder, position: Int) {
        val swapRequest = swapRequestList[position]

        // Access views via binding instead of findViewById
        with(holder.binding) {

            //set upper sender detail
            getUserBySenderProductId(swapRequest.senderProductID.toString()){
                if (it != null) {
                    //display user data
                    holder.binding.usernameTV.text = it.name.toString()
                    Glide.with(profileImgV.context).load(it.profileImage) // User Icon URL string
                        .into(profileImgV)

                    val userId = it.userID.toString()
                    holder.binding.usernameTV.setOnClickListener{
                        val fragment = UserDetail()

                        // Create a Bundle to pass data
                        val bundle = Bundle()
                        bundle.putString("UserID", userId) // Example data

                        // Set the Bundle as arguments for the fragment
                        fragment.arguments = bundle

                        val transaction = (context as AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        transaction?.replace(R.id.frameLayout, fragment)
                        transaction?.setCustomAnimations(
                            R.anim.fade_out,  // Enter animation
                            R.anim.fade_in  // Exit animation
                        )
                        transaction?.addToBackStack(null)
                        transaction?.commit()
                    }
                } else {
                    // Handle the case where the image URL is not retrieved
                    profileImgV.setImageResource(R.drawable.ai) // Set a placeholder
                }
            }

            holder.binding.dateTV.text = customizeDate(swapRequest.created_at.toString())
            //set content - you give
            getProductDetail(swapRequest.receiverProductID.toString()){ product ->
                holder.binding.userItemNameTV.text = product!!.name

                //use API to estimate product price
                getProductPrice(product.name.toString()) { priceEstimate ->
                    // Ensure this is updated on the main thread
                    holder.itemView.post {
                        holder.binding.userItemEstimatePriceTV.text = priceEstimate ?: "N/A"
                    }
                }

                // Load image from Firebase Storage
                getProductFirebaseImageUrl(product) { url ->
                    if (url != null) {
                        Glide.with(userItemImg.context)
                            .load(url)
                            .into(userItemImg)
                    } else {
                        // Handle the case where the image URL is not retrieved
                        userItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                    }
                }

                holder.binding.productGiveContainer.setOnClickListener{
                    val fragment = TradeProductDetail()

                    // Create a Bundle to pass data
                    val bundle = Bundle()
                    bundle.putString("ProductID", product.productID) // Example data

                    // Set the Bundle as arguments for the fragment
                    fragment.arguments = bundle

                    (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        ?.apply {
                            replace(R.id.frameLayout, fragment)
                            setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                            addToBackStack(null)
                            commit()
                        }
                }
            }

            //set content - you received
            getProductDetail(swapRequest.senderProductID.toString()){ product ->
                holder.binding.userReceiveItemNameTV.text = product!!.name

                //use API to estimate product price
                getProductPrice(product.name.toString()) { priceEstimate ->
                    // Ensure this is updated on the main thread
                    holder.itemView.post {
                        holder.binding.userReceiveItemEstimatePriceTV.text = priceEstimate ?: "N/A"
                    }
                }

                // Load image from Firebase Storage
                getProductFirebaseImageUrl(product) { url ->
                    if (url != null) {
                        Glide.with(userReceiveItemImg.context)
                            .load(url)
                            .into(userReceiveItemImg)
                    } else {
                        // Handle the case where the image URL is not retrieved
                        userReceiveItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                    }
                }

                holder.binding.productReceiveContainer.setOnClickListener{
                    val fragment = TradeProductDetail()

                    // Create a Bundle to pass data
                    val bundle = Bundle()
                    bundle.putString("ProductID", product.productID) // Example data

                    // Set the Bundle as arguments for the fragment
                    fragment.arguments = bundle

                    (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        ?.apply {
                            replace(R.id.frameLayout, fragment)
                            setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                            addToBackStack(null)
                            commit()
                        }
                }
            }



            holder.binding.acceptBtn.setOnClickListener{
                //update status
                updateSwapRequestStatus(swapRequest.swapRequestID.toString(), "Accepted", position)

                //update all the other swap request that contain senderProduct and receiverProduct into "Product Not Available"
                //not this swap request id
                updateOtherSwapRequestStatus(swapRequest.senderProductID.toString(), swapRequest.receiverProductID.toString(), swapRequest.swapRequestID.toString())
                
                //insert order record
                val order = Order(
                    tradeType = "Swap",
                    status = "OnGoing",
                    createdAt = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                    //productID = swapRequest.senderProductID,
                    meetUpID = swapRequest.meetUpID,
                    swapRequestID = swapRequest.swapRequestID,
                )
                val database = FirebaseDatabase.getInstance()
                val orderRef = database.getReference("Order")
                val senderProductRef = database.getReference("Product/${swapRequest.senderProductID}")
                val receiverProductRef = database.getReference("Product/${swapRequest.receiverProductID}")

                val newOrderRef = orderRef.push()
                order.orderID = newOrderRef.key

                // Push order to Firebase
                newOrderRef.setValue(order)
                    .addOnSuccessListener {println("Order added successfully") }
                    .addOnFailureListener { e -> println("Failed to add order: ${e.message}")}

                //update both product into booked
                val bookedStatus = "Booked"
                // Update the sender product
                senderProductRef.child("status").setValue(bookedStatus)
                    .addOnSuccessListener {
                        println("Sender product status updated to Booked successfully.")
                    }
                    .addOnFailureListener { e ->
                        println("Failed to update sender product status: ${e.message}")
                    }

                // Update the receiver product
                receiverProductRef.child("status").setValue(bookedStatus)
                    .addOnSuccessListener {
                        println("Receiver product status updated to Booked successfully.")
                    }
                    .addOnFailureListener { e ->
                        println("Failed to update receiver product status: ${e.message}")
                    }


                //push notification
                getUserBySenderProductId(swapRequest.senderProductID.toString()){ user ->
                    //Get User ID - From SharedPreference
                    val sharedPreferencesTARSwapper =
                        context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
                    val userID = sharedPreferencesTARSwapper.getString("userID", null)
                    val notification = Notification(
                        notificationType = "Trade",
                        notificationDateTime = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                        userID = user!!.userID
                        )
                    val database = FirebaseDatabase.getInstance().getReference("Notification")
                    // Generate a unique key for the new product
                    val newNotificationRef = database.push()
                    // Set the autogenerated product ID
                    notification.notificationID = newNotificationRef.key
                    getUserDetail(userID.toString()){user->
                        notification.notification = "${user!!.name.toString()} have accept your swap request and create Order: ${order.orderID}"
                    }

                    // Push data to Firebase
                    newNotificationRef.setValue(notification)
                        .addOnSuccessListener {
                            // Data successfully written
                            println("Notification added successfully")
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                            println("Failed to add Notification: ${e.message}")
                        }
                }

                notifyItemRemoved(position)

            }

            holder.binding.rejectBtn.setOnClickListener{
                //update status
                updateSwapRequestStatus(swapRequest.swapRequestID.toString(), "Rejected", position)

                //push notification
                getUserBySenderProductId(swapRequest.senderProductID.toString()){ user ->
                    //Get User ID - From SharedPreference
                    val sharedPreferencesTARSwapper =
                        context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
                    val userID = sharedPreferencesTARSwapper.getString("userID", null)
                    val notification = Notification(
                        notificationType = "Trade",
                        notificationDateTime = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                        userID = user?.userID
                    )
                    val database = FirebaseDatabase.getInstance().getReference("Notification")
                    // Generate a unique key for the new product
                    val newNotificationRef = database.push()
                    // Set the autogenerated product ID
                    notification.notificationID = newNotificationRef.key
                    getUserDetail(userID.toString()){user->
                        notification.notification = "${user!!.name.toString()} have reject your swap request"
                    }

                    // Push data to Firebase
                    newNotificationRef.setValue(notification)
                        .addOnSuccessListener {
                            // Data successfully written
                            println("Notification added successfully")
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                            println("Failed to add Notification: ${e.message}")
                        }
                }
                notifyItemRemoved(position)
            }


        }
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
    fun getUserBySenderProductId(senderProductID: String, onResult: (User?) -> Unit) {
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

    // Function to update swap request status -> Accepted or Rejected
    fun updateSwapRequestStatus(swapRequestID: String, newStatus: String, position : Int) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("SwapRequest/$swapRequestID")

        // Updating only the status field of the swap request
        val updateMap = mapOf("status" to newStatus)

        // Use updateChildren to update specific fields
        databaseRef.updateChildren(updateMap)
            .addOnSuccessListener {
                // Success
                Log.d("SwapRequest", "Status updated successfully")
                swapRequestList = swapRequestList.toMutableList().apply {
                    removeAt(position)  // Remove the item at the current position
                }
                notifyItemRemoved(position) // Update the RecyclerView
            }
            .addOnFailureListener { e ->
                // Handle error
                Log.e("SwapRequest", "Failed to update status: ${e.message}")
            }
    }

    private fun updateOtherSwapRequestStatus(
        senderProductID: String,
        receiverProductID: String,
        excludeSwapRequestID: String
    ) {
        val database = FirebaseDatabase.getInstance().getReference("SwapRequest")

        // Fetch all swap requests
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (swapRequestSnap in snapshot.children) {
                    val swapRequestID = swapRequestSnap.key ?: continue
                    val senderProduct = swapRequestSnap.child("senderProductID").getValue(String::class.java)
                    val receiverProduct = swapRequestSnap.child("receiverProductID").getValue(String::class.java)
                    val status = swapRequestSnap.child("status").getValue(String::class.java)

                    // Skip the swap request that matches the provided ID
                    if (swapRequestID == excludeSwapRequestID) continue

                    // Check if senderProduct or receiverProduct matches the IDs
                    if ((senderProduct == senderProductID || receiverProduct == receiverProductID) && status == "AwaitingResponse") {
                        // Update the status to "Product Not Available"
                        swapRequestSnap.ref.child("status").setValue("ProductNotAvailable")
                            .addOnSuccessListener {
                                Log.d("UpdateStatus", "SwapRequest $swapRequestID updated to Product Not Available")
                            }
                            .addOnFailureListener { e ->
                                Log.e("UpdateStatus", "Failed to update SwapRequest $swapRequestID: ${e.message}")
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching data: ${error.message}")
            }
        })
    }


    fun getProductPrice(productName: String, onResult: (String?) -> Unit) {
        val url = "https://api.upcitemdb.com/prod/trial/search"
        val client = OkHttpClient()

        val jsonBody = JSONObject().apply {
            put("s", productName)
            put("match_mode", "0")
            put("type", "product")
        }.toString()

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    onResult(null) // Pass null to indicate failure
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("API_ERROR", "Response Code: ${response.code} - ${response.message}")
                        val rateLimitRemaining = response.header("X-RateLimit-Remaining")
                        val rateLimitReset = response.header("X-RateLimit-Reset")
                        Log.d("API_RATE_LIMIT", "Remaining: $rateLimitRemaining, Reset: $rateLimitReset")
                        Handler(Looper.getMainLooper()).post {
                            onResult("N/A")
                        }
                        return
                    }

                    val responseString = response.body?.string()
                    val jsonResponse = JSONObject(responseString!!)
                    val items = jsonResponse.optJSONArray("items")

                    if (items != null && items.length() > 0) {
                        val firstItem = items.getJSONObject(0)
                        val lowestPrice = firstItem.optDouble("lowest_recorded_price", -1.0)
                        val highestPrice = firstItem.optDouble("highest_recorded_price", -1.0)

                        val priceEstimate = if (lowestPrice >= 0 && highestPrice >= 0) {
                            "Estimated Price Range: $${lowestPrice} - $${highestPrice}"
                        } else {
                            "Price not available"
                        }

                        // Ensure the UI is updated on the main thread
                        Handler(Looper.getMainLooper()).post {
                            onResult(priceEstimate)
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            onResult("No product information found")
                        }
                    }
                }
            }
        })
    }

}




