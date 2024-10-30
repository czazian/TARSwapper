package com.example.tarswapper.dataAdapter


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.data.Items
import com.example.tarswapper.data.PurchasedItem
import com.example.tarswapper.interfaces.OnItemClickListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference

class CoinShopAdapter(
    private val context: Context,
    private val listener: OnItemClickListener,
) : RecyclerView.Adapter<CoinShopAdapter.CoinShopViewHolder>() {

    //Item List
    private var itemList = emptyList<Items>()

    //The user purchased items
    private val purchasedItemIDs = mutableListOf<Int>()

    //Firebase Storage
    private lateinit var storageRef: StorageReference

    //Firebase Realtime-Database
    private lateinit var dbRef: DatabaseReference

    //ViewHolder
    inner class CoinShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //Define All Elements From the RecyclerView Layout
        val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
        val cardTitle: TextView = itemView.findViewById(R.id.title)
        val cardCoinAmount: TextView = itemView.findViewById(R.id.coinAmount)

        //Handle Click Button Event
        private val cardPurchaseButton: Button = itemView.findViewById(R.id.purchaseButton)
        init {
            cardPurchaseButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(itemList[position])
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinShopViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)

        return CoinShopViewHolder(itemView)
    }


    //Bind Item Data into ViewHolder One-by-One with Looping
    override fun onBindViewHolder(holder: CoinShopViewHolder, position: Int) {
        val currentItem = itemList[position]

        // Check if the current item has been purchased by the user
        if (purchasedItemIDs.contains(currentItem.itemID)) {
            // Skip binding and make the view gone
            holder.itemView.visibility = View.GONE
        } else {
            //Bind Image
            Glide.with(holder.cardImage)
                .load(currentItem.itemURL)
                .into(holder.cardImage)
            //Bind others
            holder.cardCoinAmount.text = currentItem.itemRequireCoin.toString()
            holder.cardTitle.text = currentItem.itemName.toString()

            //Ensure the item is visible
            holder.itemView.visibility = View.VISIBLE

        }
    }

    //Get the item list need to be bind
    fun setData(itemList: List<Items>) {
        //Get purchased items and filter the item list
        getPurchasedItems { purchasedItems ->
            this.purchasedItemIDs.clear() // Clear any previous IDs
            this.purchasedItemIDs.addAll(purchasedItems) // Add new IDs

            //Filter out the items that have been purchased
            this.itemList = itemList.filter { !purchasedItemIDs.contains(it.itemID) }

            //Notify the adapter to refresh the view
            notifyDataSetChanged()
        }
    }

    //Specifically to get a list of PurchasedItem (by checking itemID existence of the userID) by the logged-in user, add into a list.
    private fun getPurchasedItems(onResult: (List<Int>) -> Unit) {
        val purchasedItemIDs = mutableListOf<Int>()
        val sharedPreferencesTARSwapper = context.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        if (userID != null) {
            val database = FirebaseDatabase.getInstance()
            val purchasedItemRef = database.getReference("PurchasedItem")

            purchasedItemRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (itemSnapshot in dataSnapshot.children) {
                        val purchasedItem = itemSnapshot.getValue(PurchasedItem::class.java)
                        if (purchasedItem != null) {
                            //Ensure itemID is not null
                            purchasedItemIDs.add(purchasedItem.itemID!!)
                        }
                    }
                    onResult(purchasedItemIDs)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("getPurchasedItems Error", "Error: ${databaseError.message}")
                    onResult(emptyList())
                }
            })
        }
    }
}