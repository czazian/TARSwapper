package com.example.tarswapper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.tarswapper.data.MeetUp
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.Transaction
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentReceiptBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class Receipt : Fragment() {
    private lateinit var binding: FragmentReceiptBinding

    private var name: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReceiptBinding.inflate(inflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Back to the User Profile Page
        binding.backProfileBtn.setOnClickListener() {
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


        ////Processing////

        //Get Current User ID
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)


        //Total Duration
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val recordedDateTime = getStoredDateTime()
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
            Date()
        )
        val recordedDate: Date? = recordedDateTime?.let { dateFormat.parse(it) }
        val currentDate: Date = dateFormat.parse(currentDateTime)!!
        val totalDurationInMillis = abs(recordedDate!!.time - currentDate.time)

        //Conversion
        val totalSeconds = totalDurationInMillis / 1000
        val totalMinutes = totalSeconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val seconds = totalSeconds % 60

        val hourLabel = if (hours == 1L || hours == 0L) "Hour" else "Hours"
        val minuteLabel = if (minutes == 1L || minutes == 0L) "Min" else "Mins"
        val secondLabel = if (seconds == 1L || minutes == 0L) "Second" else "Seconds"

        binding.receiptTotalDuration.text =
            "$hours $hourLabel $minutes $minuteLabel $seconds $secondLabel"


        //Completion Date Time
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val formattedDateTime = try {
            val date = inputFormat.parse(currentDateTime)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        binding.receiptCmpleteDateTime.text = formattedDateTime


        val transaction = arguments?.getSerializable("order") as? Order
        transaction?.let {
            //Get Meet Up Object
            getMeetUpObject(transaction.meetUpID!!) { meetUp ->

                if (meetUp != null) {

                    //Order ID
                    binding.receiptTransactionID.text = "Transaction ID: ${transaction.orderID}"

                    //Destination
                    binding.receiptAddress.text = meetUp!!.location

                    //Schedule Date Time
                    val originalDateFormat =
                        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                    val dateToFormat = "${meetUp.date} ${meetUp.time}"

                    //Parse the date and time
                    val date: Date? = try {
                        originalDateFormat.parse(dateToFormat)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    //Desired output format
                    val desiredDateFormat =
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    val dateTime = date?.let { desiredDateFormat.format(it) } ?: ""

                    //Set the formatted date and time to the TextView
                    binding.receiptDateTime.text = dateTime


                    //Swappers
                    val database = FirebaseDatabase.getInstance()
                    val swapRequestsRef = database.getReference("SwapRequest")
                    val productsRef = database.getReference("Product")


                    val duration = "$hours $hourLabel $minutes $minuteLabel $seconds $secondLabel"


                    val names = mutableListOf<String>()
                    when (transaction.tradeType) {
                        "Sale" -> {
                            //Fetch seller name
                            getUser(transaction.sellerID.toString()) { seller ->
                                seller?.let { names.add(it.name!!) }
                                //Fetch buyer name
                                getUser(transaction.buyerID.toString()) { buyer ->
                                    buyer?.let { names.add(it.name!!) }
                                    //Update the name when both are fetched
                                    binding.receiptReceiptSeller.text = names.joinToString(", ")
                                    Log.e("Sale Names", names.toString())
                                }
                            }

                            if (userID == transaction!!.sellerID) {
                                //Store record into DB
                                addIntoFirebase(
                                    transaction.orderID,
                                    meetUp.location,
                                    transaction.sellerID,
                                    transaction.buyerID,
                                    dateTime,
                                    formattedDateTime!!,
                                    duration
                                )
                            }


                        }

                        "Swap" -> {
                            //Fetch SwapRequest data
                            swapRequestsRef.child(transaction.swapRequestID!!).get()
                                .addOnSuccessListener { swapRequestSnapshot ->
                                    Log.d("Firebase", "SwapRequest fetched successfully.")

                                    val swapRequest =
                                        swapRequestSnapshot.getValue(SwapRequest::class.java)
                                    swapRequest?.let { request ->
                                        //Fetch sender product and name
                                        productsRef.child(request.senderProductID!!).get()
                                            .addOnSuccessListener { senderProductSnapshot ->
                                                val senderProduct =
                                                    senderProductSnapshot.getValue(Product::class.java)
                                                processProduct(senderProduct) { senderUser ->
                                                    senderUser?.let {
                                                        names.add(it.name!!)
                                                    }
                                                    //Fetch receiver product and name
                                                    productsRef.child(request.receiverProductID!!)
                                                        .get()
                                                        .addOnSuccessListener { receiverProductSnapshot ->
                                                            val receiverProduct =
                                                                receiverProductSnapshot.getValue(
                                                                    Product::class.java
                                                                )
                                                            processProduct(receiverProduct) { receiverUser ->
                                                                receiverUser?.let {
                                                                    names.add(it.name!!)
                                                                }
                                                                //Update the name when both are fetched
                                                                binding.receiptReceiptSeller.text =
                                                                    names.joinToString(", ")
                                                                Log.e(
                                                                    "Swap Names",
                                                                    names.toString()
                                                                )

                                                                if (userID == senderUser!!.userID) {
                                                                    //Store record into DB
                                                                    addIntoFirebase(
                                                                        transaction.orderID,
                                                                        meetUp.location,
                                                                        senderUser!!.userID,
                                                                        receiverUser!!.userID,
                                                                        dateTime,
                                                                        formattedDateTime!!,
                                                                        duration
                                                                    )
                                                                }
                                                            }
                                                        }
                                                }
                                            }.addOnFailureListener { e ->
                                                Log.e(
                                                    "Firebase Error",
                                                    "Failed to fetch SwapRequest",
                                                    e
                                                )
                                            }
                                    }
                                }
                        }

                        else -> {
                            binding.receiptReceiptSeller.text = "Error"
                        }
                    }

                    binding.btnDownloadPdf.setOnClickListener() {
                        createPdf(
                            transaction.orderID,
                            meetUp.location,
                            names.joinToString(", "),
                            dateTime,
                            formattedDateTime!!,
                            duration
                        )
                    }

                }
            }


        }




        return binding.root
    }

    private fun addIntoFirebase(
        orderID: String?,
        location: String?,
        giverID: String?,
        takerID: String?,
        scheduledDateTime: String?,
        completionDateTime: String,
        duration: String
    ) {
        val transaction = Transaction(
            transactionID = orderID,
            scheduledDateTime = scheduledDateTime,
            completedDateTime = completionDateTime,
            destination = location,
            totalDuration = duration,
            giverID = giverID,
            takerID = takerID,
        )


        //Reference to the Firebase Realtime Database
        val databaseReference = FirebaseDatabase.getInstance().getReference("Transactions")

        //Generate a random unique key using push()
        val randomKey = databaseReference.push().key

        randomKey?.let {
            databaseReference.child(it).setValue(transaction)
                .addOnSuccessListener {
                    //Successfully added data
                    Log.e("Success DB", "Success to add transaction to Firebase")
                }
                .addOnFailureListener { error ->
                    Log.e("Failed DB", "Failed to add transaction to Firebase: ${error.message}")
                }
        }
    }

    private fun getUser(createdByUserid: String, onResult: (User?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User").child(createdByUserid)

        userRef.get().addOnSuccessListener { dataSnapshot ->
            val user = dataSnapshot.getValue(User::class.java)
            onResult(user)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }

    private fun getStoredDateTime(): String? {
        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("CountTime", Context.MODE_PRIVATE)
        return sharedPreferences.getString("dateTime", "N/A")
    }

    private fun getMeetUpObject(meetUpID: String, onResult: (MeetUp?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val meetUpRef = database.getReference("MeetUp").child(meetUpID)

        meetUpRef.get().addOnSuccessListener { dataSnapshot ->
            val meetUp = dataSnapshot.getValue(MeetUp::class.java)
            onResult(meetUp)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }

    private fun processProduct(product: Product?, onResult: (User?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User")

        product?.let {
            val userID = product.created_by_UserID!!

            userRef.child(userID).get().addOnSuccessListener { dataSnapshot ->
                val user = dataSnapshot.getValue(User::class.java)
                onResult(user)
            }.addOnFailureListener { e ->
                println("Error fetching MeetUp: ${e.message}")
                onResult(null)
            }
        }
    }

    private fun createPdf(
        orderID: String?,
        location: String?,
        names: String,
        scheduledDateTime: String?,
        completionDateTime: String,
        duration: String
    ) {
        //Create a new document
        val pdfDocument = PdfDocument()

        //Page info and dimensions
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        //Paint object for text and shapes
        val paint = Paint()
        paint.isAntiAlias = true

        //Draw the green check circle
        paint.color = Color.parseColor("#4CAF50") // Green color
        canvas.drawCircle(300f, 80f, 50f, paint)

        paint.color = Color.WHITE
        paint.textSize = 40f
        canvas.drawText("✔", 285f, 95f, paint)

        //Draw "Transaction Successful!" text
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Transaction Successful!", 170f, 160f, paint)

        //Draw transaction details
        paint.isFakeBoldText = false
        paint.textSize = 18f

        canvas.drawText("Transaction ID: $orderID", 50f, 200f, paint)
        canvas.drawText("Destination", 50f, 240f, paint)

        //Address
        paint.textSize = 16f
        paint.color = Color.DKGRAY
        canvas.drawText(
            location.toString(),
            50f,
            260f,
            paint
        )

        paint.color = Color.BLACK
        paint.textSize = 18f
        canvas.drawText("Swappers / Seller or Buyer", 50f, 300f, paint)

        //Swappers names
        paint.textSize = 16f
        paint.color = Color.DKGRAY
        canvas.drawText(names, 50f, 320f, paint)

        //Scheduled and Completed Date & Time
        paint.color = Color.BLACK
        paint.textSize = 18f
        canvas.drawText("Scheduled Date & Time", 50f, 360f, paint)

        paint.textSize = 16f
        paint.color = Color.DKGRAY
        canvas.drawText(scheduledDateTime.toString(), 50f, 380f, paint)

        paint.color = Color.BLACK
        paint.textSize = 18f
        canvas.drawText("Completed Date & Time", 50f, 420f, paint)

        paint.textSize = 16f
        paint.color = Color.DKGRAY
        canvas.drawText(completionDateTime, 50f, 440f, paint)

        //Total Duration
        paint.color = Color.BLACK
        paint.textSize = 18f
        canvas.drawText("Total Duration", 50f, 480f, paint)

        paint.textSize = 16f
        paint.color = Color.DKGRAY
        canvas.drawText(duration, 50f, 500f, paint)

        //Finish the page
        pdfDocument.finishPage(page)

        //Save PDF file to device
        val pdfFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "TransactionReceipt.pdf"
        )

        try {
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            Toast.makeText(requireContext(), "PDF saved to Downloads!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG)
                .show()
        } finally {
            pdfDocument.close()
        }
    }

}