package com.example.tarswapper.dataAdapter

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
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
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class TradeSwapRequestReceivedAdapter(private var swapRequestList: List<SwapRequest>) :
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
            getUserByReceiverProductId(swapRequest.senderProductID.toString()){
                if (it != null) {
                    //display user data
                    holder.binding.usernameTV.text = it.name.toString()
                    Glide.with(profileImgV.context).load(it.profileImage) // User Icon URL string
                        .into(profileImgV)
                } else {
                    // Handle the case where the image URL is not retrieved
                    profileImgV.setImageResource(R.drawable.ai) // Set a placeholder
                }
            }

            holder.binding.dateTV.text = customizeDate(swapRequest.created_at.toString())
            //set content - you give
            getProductDetail(swapRequest.receiverProductID.toString()){ product ->
                holder.binding.userItemNameTV.text = product!!.name
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
                //connect to API set product price
                holder.binding.userItemEstimatePriceTV.text = "RM 1"
            }

            //set content - you received
            getProductDetail(swapRequest.senderProductID.toString()){ product ->
                holder.binding.userReceiveItemNameTV.text = product!!.name
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
                //connect to API set product price
                holder.binding.userReceiveItemEstimatePriceTV.text = "RM 2"
            }

            holder.binding.acceptBtn.setOnClickListener{
                //update status
                updateSwapRequestStatus(swapRequest.swapRequestID.toString(), "Accepted", position)
                //insert order record
                val order = Order(
                    tradeType = "Swap",
                    status = "OnGoing",
                    createdAt = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur")).toString(),
                    productID = swapRequest.senderProductID,
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

            }

            holder.binding.rejectBtn.setOnClickListener{
                //update status
                updateSwapRequestStatus(swapRequest.swapRequestID.toString(), "Rejected", position)
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
}




