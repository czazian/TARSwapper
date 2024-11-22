package com.example.tarswapper.dataAdapter

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.TradeSwapRequest
import com.example.tarswapper.UserProfile
import com.example.tarswapper.data.Notification
import com.example.tarswapper.databinding.NotificationTradeItemBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class NotificationTradeAdapter (
    private val notificationList: MutableList<Notification>,
    private val context: Context,
    ) :
    RecyclerView.Adapter<NotificationTradeAdapter.NotificationViewHolder>() {
    class NotificationViewHolder(val binding: NotificationTradeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding =
            NotificationTradeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationList[position]

        // Access views via binding instead of findViewById
        with(holder.binding) {

            if(notification.notificationType == "Trade"){
                //redirect to swap request
                holder.binding.notificationImage.setImageResource(R.drawable.baseline_swap_horiz_24)

                //on click go to swap request
                holder.binding.messageCardItem.setOnClickListener{
                    val fragment = TradeSwapRequest()

                    //Bottom Navigation Indicator Update

                    //Back to previous page with animation
                    (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        ?.apply {
                            replace(R.id.frameLayout, fragment)
                            setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                            addToBackStack(null)
                            commit()
                        }
                }
            }else if(notification.notificationType == "Community"){
                //redirect to the community
                holder.binding.notificationImage.setImageResource(R.drawable.people_selector)
            }

            holder.binding.notificationTitle.text = notification.notificationType.toString()
            holder.binding.notificationContent.text = notification.notification.toString()
            holder.binding.notificationTime.text =
                customizeDate(notification.notificationDateTime.toString())
        }
    }

    override fun getItemCount(): Int = notificationList.size

    fun getItem(position: Int): Notification {
        return notificationList[position]
    }

    fun removeItem(position: Int) {
        notificationList.removeAt(position)
        notifyItemRemoved(position)
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