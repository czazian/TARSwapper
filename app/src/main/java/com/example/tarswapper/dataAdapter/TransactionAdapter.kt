package com.example.tarswapper.dataAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tarswapper.R
import com.example.tarswapper.data.MeetUp
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.databinding.NotificationTransactionLayoutBinding
import com.google.firebase.database.FirebaseDatabase
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val context: Context,
    private val onTransactionClick: (Order) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    //The Order list consisting for both Sale and Swap type
    private var transactions = listOf<Order>()

    fun setData(transactions: List<Order>) {
        //Sort transactions by datetime before binding data, in descending order (latest to oldest)
//        val sortedTransactions = transactions.sortedByDescending { transaction ->
//            val combinedDateTime = "${transaction.date}|${transaction.time}"
//            val dateFormat = SimpleDateFormat("yyyy-MM-dd|HH:mm", Locale.getDefault())
//            try {
//                dateFormat.parse(combinedDateTime) ?: Date(0)
//            } catch (e: Exception) {
//                Log.e("TransactionAdapter", "Error parsing date: $combinedDateTime", e)
//                Date(0)
//            }
//        }
        this.transactions = transactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = NotificationTransactionLayoutBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    inner class TransactionViewHolder(private val binding: NotificationTransactionLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Order) {
            getMeetUpObj(transaction.meetUpID!!) {
                if(it != null){

                    //Bind the Date Time
                    val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val outputFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                    try {
                        val dateString = it.date!!.trim()
                        val parsedDate = inputFormatter.parse(dateString)

                        val formattedDate = outputFormatter.format(parsedDate!!)

                        binding.transactionDateTime.text = "the scheduled date and time is $formattedDate, ${it.time}."
                    } catch (e: ParseException) {
                        Log.e("TransactionAdapter", "Failed to parse date: ${e.message}")
                        binding.transactionDateTime.text = "Invalid date format"
                    }



                    //Handle Button Status - If the Transaction is Over Transaction Date, or Already Successful, it should be Transaction Ended
                    val dateItem = "${it.date}"
                    if (dateItem.isNotEmpty()) {
                        try {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val transactionDate = dateFormat.parse(dateItem)
                            val currentDate = Date()

                            //Remove time part from both dates to compare only the date
                            val calendar = Calendar.getInstance()
                            calendar.time = currentDate
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            val currentDateOnly = calendar.time

                            if (transactionDate != null && transactionDate.before(currentDateOnly) || transaction.status.toString() != "OnGoing") {
                                binding.transactionBtn.text = "Transaction Ended"
                                binding.transactionBtn.isEnabled = false

                                val grayColor = ContextCompat.getColor(context, R.color.button_gray)
                                binding.transactionBtn.setBackgroundColor(grayColor)
                            } else {
                                binding.transactionBtn.text = "Start Navigation"
                                binding.transactionBtn.isEnabled = true

                                val purpleColor = ContextCompat.getColor(context, R.color.button_purple)
                                binding.transactionBtn.setBackgroundColor(purpleColor)
                            }
                        } catch (e: ParseException) {
                            Log.e("TransactionAdapter", "Error parsing date: ${e.message}")
                            binding.transactionBtn.text = "Invalid Date"
                            binding.transactionBtn.isEnabled = false
                        }
                    }

                }
            }


            //Transaction ID
            binding.transactionID.text = "Transaction ID: ${transaction.orderID}"


            //Only the future date time can be shown - On Click Event of Start Navigation
            binding.transactionBtn.setOnClickListener {
                if (binding.transactionBtn.isEnabled) {
                    Log.d("TransactionAdapter", "Button clicked for transaction ID: ${transaction.swapRequestID}")
                    onTransactionClick(transaction)
                } else {
                    Log.d("TransactionAdapter", "Button is disabled, click ignored")
                }
            }


        }
    }

    private fun getMeetUpObj(meetUpID: String, onResult: (MeetUp?) -> Unit) {
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
}