package com.example.tarswapper.dataAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tarswapper.Notification
import com.example.tarswapper.R
import com.example.tarswapper.data.Message
import com.example.tarswapper.databinding.NotificationMessageLayoutBinding
import com.example.tarswapper.databinding.NotificationTransactionLayoutBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val context: Context,
    private val onTransactionClick: (Notification.Temp) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactions = listOf<Notification.Temp>()

    fun setData(transactions: List<Notification.Temp>) {
        //Sort transactions by datetime before binding data, in descending order (latest to oldest)
        val sortedTransactions = transactions.sortedByDescending { transaction ->
            val combinedDateTime = "${transaction.date}|${transaction.time}"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd|HH:mm", Locale.getDefault())
            try {
                dateFormat.parse(combinedDateTime) ?: Date(0)
            } catch (e: Exception) {
                Log.e("TransactionAdapter", "Error parsing date: $combinedDateTime", e)
                Date(0)
            }
        }
        this.transactions = sortedTransactions
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

        fun bind(transaction: Notification.Temp) {
            binding.transactionID.text = "Transaction ID: #123123"
            binding.transactionDateTime.text =
                "reached the scheduled date and time of ${transaction.date}, ${transaction.time}."

            val dateTime = "${transaction.time}|${transaction.date}"
            if (dateTime.isNotEmpty()) {
                val combinedDateTime = "${transaction.date}|${transaction.time}"
                val dateFormat = SimpleDateFormat("yyyy-MM-dd|HH:mm", Locale.getDefault())
                val transactionDate = dateFormat.parse(combinedDateTime)
                val currentDate = Date()

                if (transactionDate != null && transactionDate.before(currentDate)) {
                    //Transaction date is in the past, so disable the button and update the UI
                    binding.transactionBtn.text = "Transaction Ended"
                    binding.transactionBtn.isEnabled = false

                    val grayColor = ContextCompat.getColor(context, R.color.button_gray)
                    binding.transactionBtn.setBackgroundColor(grayColor)
                } else {
                    //Transaction date is not in the past, so allow starting navigation
                    binding.transactionBtn.text = "Start Navigation"
                    binding.transactionBtn.isEnabled = true

                    val purpleColor = ContextCompat.getColor(context, R.color.button_purple)
                    binding.transactionBtn.setBackgroundColor(purpleColor)
                }
            }

            //Only the future date time can be shown
            if (binding.transactionBtn.isEnabled) {
                binding.transactionBtn.setOnClickListener {
                    onTransactionClick(transaction)
                }
            } else {
                binding.transactionBtn.setOnClickListener(null)
            }
        }
    }
}