package com.example.tarswapper.dataAdapter

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.data.MeetUp
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.TradeCompleteOrderListBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class TradeOrderCompletedAdapter(private var orderList: List<Order>, private val context : Context) :
    RecyclerView.Adapter<TradeOrderCompletedAdapter.OrderViewHolder>() {

    class OrderViewHolder(val binding: TradeCompleteOrderListBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = TradeCompleteOrderListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun getItemCount(): Int = orderList.size

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]

        val sharedPreferencesTARSwapper =
            context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        // Access views via binding instead of findViewById
        with(holder.binding) {
            getOrderDetail(order.orderID.toString()) { order ->
                holder.binding.orderStatusTV.text = order!!.status.toString()
                holder.binding.orderStatusTV.setTextColor(Color.parseColor("#0077CC"))

                if(order?.tradeType == "Sale"){
                    holder.binding.tradeTypeImg.setImageResource(R.drawable.baseline_attach_money_24)

                    //determine who is buyer and seller
                    //user is buyer
                    holder.binding.swapContainer.visibility = View.GONE
                    if(userID == order.buyerID){
                        getUserDetail(order.sellerID.toString()){
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

                        holder.binding.youGiveContainer.visibility = View.GONE
                        holder.binding.youReceiveContainer.visibility = View.VISIBLE

                        getProductDetail(order.productID.toString()){ product ->
                            holder.binding.userReceiveItemNameTV.text = "${product!!.name}  (RM ${product.price!!.format(2)})"
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
                        }



                    }else{
                        //user is seller
                        getUserDetail(order.buyerID.toString()){
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

                        holder.binding.youGiveContainer.visibility = View.VISIBLE
                        holder.binding.youReceiveContainer.visibility = View.GONE

                        getProductDetail(order.productID.toString()){ product ->
                            holder.binding.userItemNameTV.text = "${product!!.name}  (${product.price!!.format(2)})"
                            // Load image from Firebase Storage
                            getProductFirebaseImageUrl(product!!) { url ->
                                if (url != null) {
                                    Glide.with(userItemImg.context)
                                        .load(url)
                                        .into(userItemImg)
                                } else {
                                    // Handle the case where the image URL is not retrieved
                                    userItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                                }
                            }
                        }

                    }

                }else if(order?.tradeType == "Swap"){
                    holder.binding.tradeTypeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)
                    holder.binding.swapContainer.visibility = View.VISIBLE
                    holder.binding.youReceiveContainer.visibility = View.VISIBLE
                    holder.binding.youGiveContainer.visibility = View.VISIBLE

                    //determine which product is belong to user and other user
                    getSwapRequest(order.swapRequestID.toString()){ swapRequest ->
                        getProductDetail(swapRequest!!.senderProductID.toString()){ product ->
                            //user product = sender product
                            if(product!!.created_by_UserID == userID){
                                holder.binding.userItemNameTV.text = "${product!!.name}"
                                // Load image from Firebase Storage
                                getProductFirebaseImageUrl(product!!) { url ->
                                    if (url != null) {
                                        Glide.with(userItemImg.context)
                                            .load(url)
                                            .into(userItemImg)
                                    } else {
                                        // Handle the case where the image URL is not retrieved
                                        userItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                                    }
                                }
                                getProductDetail(swapRequest.receiverProductID.toString()){ product ->
                                    holder.binding.userReceiveItemNameTV.text = "${product!!.name}"
                                    getUserDetail(product.created_by_UserID.toString()){
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
                                }

                            }else {
                                //user product = receiver product
                                getProductDetail(swapRequest!!.receiverProductID.toString()) { product ->
                                    holder.binding.userItemNameTV.text = "${product!!.name}"
                                    // Load image from Firebase Storage
                                    getProductFirebaseImageUrl(product!!) { url ->
                                        if (url != null) {
                                            Glide.with(userItemImg.context)
                                                .load(url)
                                                .into(userItemImg)
                                        } else {
                                            // Handle the case where the image URL is not retrieved
                                            userItemImg.setImageResource(R.drawable.ai) // Set a placeholder
                                        }
                                    }
                                }

                                getProductDetail(swapRequest.senderProductID.toString()) { product ->
                                    holder.binding.userReceiveItemNameTV.text = "${product!!.name}"
                                    getUserDetail(product.created_by_UserID.toString()){
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
                                }


                            }

                        }
                    }
                }
                getMeetUp(order!!.meetUpID.toString()){ meetUp ->
                    holder.binding.locationTV.text = meetUp!!.location
                    holder.binding.venueTV.text = meetUp!!.venue
                    holder.binding.timeTV.text = meetUp!!.time
                    holder.binding.dateTV.text = meetUp!!.date
                }

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

}




