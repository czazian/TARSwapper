package com.example.tarswapper

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Items
import com.example.tarswapper.data.PurchasedItem
import com.example.tarswapper.dataAdapter.CoinShopAdapter
import com.example.tarswapper.databinding.FragmentCoinShopBinding
import com.example.tarswapper.interfaces.OnItemClickListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class CoinShop : Fragment(), OnItemClickListener {

    private lateinit var binding: FragmentCoinShopBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCoinShopBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE

        //Manage RecyclerView
        val recyclerView = binding.cardRecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)


        //Go to User Profile Page
        binding.backUserProfileBtn.setOnClickListener() {
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


        //Set the initial view of item list
        getListOfUserTitle {
            if (it != null) {
                updateRecyclerView(it)
            }
        }

        //Get the coin
        getCurrentCoin() {
            if (it != null) {
                binding.coinAmt.text = it.toString()
            } else {
                binding.coinAmt.text = "NaN"
            }
        }


        //Determine which Tab is Selected, weather User Titles or Profile Background
        binding.tabId.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // his method is called when a tab is selected
                val position = tab?.position
                if (position != null) {
                    when (tab.position) {
                        //When User Titles Tab is selected
                        0 -> {
                            //Get list of user titles
                            getListOfUserTitle() {
                                if (it != null) {
                                    updateRecyclerView(it)
                                }
                            }
                        }
                        //When Profile Background Tab is selected
                        1 -> {
                            //Get list of profile backgrounds
                            getListOfProfileBackground() {
                                if (it != null) {
                                    updateRecyclerView(it)
                                }
                            }
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                //This method is called when a tab is unselected
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                //This method is called when a tab is reselected (clicked again)
            }
        })

        return binding.root
    }

    //Get user current coin
    private fun getCurrentCoin(onResult: (Int?) -> Unit) {
        //Retrieve all necessary data from SharedPreference (TARSwapperPreferences)
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        if (userID != null) {
            //Firebase Processing
            val database = FirebaseDatabase.getInstance()
            val usersRef = database.getReference("User")

            usersRef.child(userID).child("coinAmount")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val coinAmount = dataSnapshot.getValue(Long::class.java)?.toInt()
                        onResult(coinAmount)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("getCoinAmount Error", "Error: ${databaseError.message}")
                        onResult(null)
                    }
                })
        }
    }


    //Bind the Data into ViewHolder (RecyclerView)
    private fun updateRecyclerView(mutableList: MutableList<Items>) {

        //this = listener
        val adapter = CoinShopAdapter(requireContext(),this)
        adapter.setData(mutableList)
        binding.cardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.cardRecyclerView.adapter = adapter
        binding.cardRecyclerView.setHasFixedSize(true)
        adapter.notifyDataSetChanged()

    }


    //When Profile Background Tab is selected, get data from Firebase
    private fun getListOfProfileBackground(onResult: (MutableList<Items>?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val itemsRef = database.getReference("Items")

        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val itemList = mutableListOf<Items>()
                for (itemSnapshot in dataSnapshot.children) {
                    val item = itemSnapshot.getValue(Items::class.java)
                    if (item?.itemType == "background") {
                        itemList.add(item)
                    }
                }
                onResult(itemList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("getListOfProfileBackground Error", "Error: ${databaseError.message}")
                onResult(null)
            }
        })
    }

    //When User Titles Tab is selected, get data from Firebase
    private fun getListOfUserTitle(onResult: (MutableList<Items>?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val profilesRef = database.getReference("Items")

        profilesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val itemList = mutableListOf<Items>()
                for (itemSnapshot in dataSnapshot.children) {
                    val item = itemSnapshot.getValue(Items::class.java)
                    if (item?.itemType == "title") {
                        itemList.add(item)
                    }
                }
                onResult(itemList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("getListOfUserTitle Error", "Error: ${error.message}")
                onResult(null)
            }
        })
    }


    //Handle onItemClick for different items in the RecyclerView (Worked) - knows which item's button is clicked
    override fun onItemClick(item: Items) {
        //Get user's coin
        getCurrentCoin {
            //If coin not null
            if (it != null) {
                //Create and show an AlertDialog to display errors
                AlertDialog.Builder(requireContext())
                    .setTitle("Purchase Confirmation")
                    .setMessage("Are you sure you want to purchase this item?")
                    .setPositiveButton("Confirm") { dialog, _ ->
                        //Confirm Action
                        updateData(item, it)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        //Cancel Action => Close Dialog
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Error: coin is null",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    //User purchases an item
    private fun updateData(item: Items, coin: Int) {
        //First of all, check weather user has the sufficient amount of coin for the item
        val hasSufficientCoin = checkSufficientAmt(item, coin)

        //If the user has the sufficient amount of coin for the item, then continue
        if (hasSufficientCoin) {
            //Prepare Data - UserID
            val sharedPreferencesTARSwapper =
                requireActivity().getSharedPreferences(
                    "TARSwapperPreferences",
                    Context.MODE_PRIVATE
                )
            val userID = sharedPreferencesTARSwapper.getString("userID", null)

            //Execute a method only when userID is not null
            if (userID != null) {
                //Create Object for Insertion
                insertNewPurchasedItem(userID, item);

                //Add the purchased item to the PurchasedItem table
                updateUserOwnedCoin(item, coin, userID)

            }
        } else {
            //Create and show an AlertDialog to display errors
            AlertDialog
                .Builder(requireContext())
                .setTitle("Error")
                .setMessage("You do not have sufficient amount of coin to purchase this item!")
                .setPositiveButton("OK") { dialog, _ ->
                    //Close dialog when OK is clicked
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }



    //Check sufficient amount of coin
    private fun checkSufficientAmt(item: Items, coin: Int): Boolean {
        val itemRequireCoin = item.itemRequireCoin
        return coin >= itemRequireCoin!!
    }

    //Insert new PurchasedItem record
    private fun insertNewPurchasedItem(userID: String, item: Items) {
        //Create Object
        val purchasedItem = PurchasedItem(
            userID = userID,
            itemID = item.itemID,
            isEquipped = false,
        )

        //Firebase
        val database = FirebaseDatabase.getInstance()
        val purchasedItemRef = database.getReference("PurchasedItem")

        //Generate Random Key as ID
        val purchasedItemID = database.getReference("generateKey").push().key

        //Insertion
        purchasedItemRef.child(purchasedItemID.toString()).setValue(purchasedItem)
            .addOnSuccessListener {
                //Display Successful Message
                Toast.makeText(
                    requireContext(),
                    "Item Purchased Successfully",
                    Toast.LENGTH_SHORT
                ).show()


                //Reset the list after purchase
                if(item.itemType == "title") {
                    getListOfUserTitle {
                        if (it != null) {
                            updateRecyclerView(it)
                        }
                    }
                } else if (item.itemType == "background"){
                    getListOfProfileBackground {
                        if (it != null) {
                            updateRecyclerView(it)
                        }
                    }
                }


            }.addOnFailureListener {
                //Display Error Message
                Toast.makeText(
                    requireContext(),
                    "Item Purchased Unsuccessfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    //Process update of latest coin amount
    private fun updateUserOwnedCoin(item: Items, coin: Int, userID: String) {
        //Firebase
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User")

        //Calculate latest coin
        val latestCoin = coin - item.itemRequireCoin!!.toInt()

        //Prepare Update Mapping
        val update = mapOf("coinAmount" to latestCoin)

        //Update latest coin into Firebase given a userID
        userRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        userSnapshot.ref.updateChildren(update).addOnCompleteListener { task ->
                            if (task.isSuccessful){
                                //Update Coin Status
                                binding.coinAmt.text = latestCoin.toString();
                            }
                        }
                    }
                } else {
                    Log.e("Update Coin", "Update coin unsuccessful!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UpdateUser", "Database error: ${error.message}")
            }
        })
    }
}