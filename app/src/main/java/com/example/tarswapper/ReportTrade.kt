package com.example.tarswapper

import android.content.Context
import android.graphics.Color
import androidx.fragment.app.Fragment
import com.example.tarswapper.data.User
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Community
import com.example.tarswapper.data.Order
import com.example.tarswapper.dataAdapter.CommunityMyPostAdapter
import com.example.tarswapper.dataAdapter.TradeOrderCompletedAdapter
import com.example.tarswapper.databinding.FragmentReportOrderCompletedBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportTrade : Fragment() {
    private lateinit var binding: FragmentReportOrderCompletedBinding
    private lateinit var userObj: User
    private lateinit var orderAdapter: TradeOrderCompletedAdapter

    private lateinit var database: DatabaseReference
    private val communityList = mutableListOf<com.example.tarswapper.data.Community>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportOrderCompletedBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)


        binding.btnBack.setOnClickListener{
            val fragment = UserProfile()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        fetchAllOrdersForUser(userID.toString(),
            onResult = { orders ->
                if (orders.isNotEmpty()) {
                    //start doing here
                    setupBarChart(orders)

                } else {
                    Log.d("AllOrders", "No orders found for user.")
                }
            },
            onError = { error ->
                Log.e("FirebaseError", "Error fetching orders: ${error?.message}")
            }
        )


    }

    private fun setupBarChart(orders: List<Order>) {
        val barChart = binding.barChart

        // Step 1: Extract and group data by date
        val dateOrderMap = mutableMapOf<String, MutableList<Order>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Track remaining Firebase calls
        var pendingTasks = orders.size

        if (pendingTasks == 0) {
            // No orders to process
            updateBarChart(barChart, dateOrderMap)
            return
        }

        for (order in orders) {
            getOrderDetail(order.orderID.toString()) { orderObj ->
                val date = orderObj?.createdAt?.let {
                    dateFormat.format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it) ?: Date())
                }
                if (date != null) {
                    dateOrderMap.getOrPut(date) { mutableListOf() }.add(order)
                }

                // Decrement the remaining task count
                pendingTasks--
                if (pendingTasks == 0) {
                    // All Firebase calls are complete, update the chart
                    updateBarChart(barChart, dateOrderMap)
                }
            }
        }
    }

    private fun updateBarChart(barChart: BarChart, dateOrderMap: Map<String, MutableList<Order>>) {
        // Step 2: Prepare BarEntry data
        val sortedDates = dateOrderMap.keys.sorted()
        val entries = sortedDates.mapIndexed { index, date ->
            BarEntry(index.toFloat(), dateOrderMap[date]?.size?.toFloat() ?: 0f)
        }

        // Step 3: Create BarDataSet and configure the chart
        val barDataSet = BarDataSet(entries, "Orders")
        barDataSet.color = Color.parseColor("#756AB6")
        barDataSet.valueTextSize = 12f
        barDataSet.valueTextColor = Color.BLACK

        // Display integers at the top of each bar
        barDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val barData = BarData(barDataSet)
        barData.barWidth = 0.4f

        // Configure the x-axis
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in sortedDates.indices) sortedDates[index] else ""
                }
            }
            granularity = 1f
        }

        // Configure the Y-axis to display integers
        barChart.axisLeft.apply {
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
            axisMinimum = 0f
        }
        barChart.axisRight.isEnabled = false

        // Configure the chart
        barChart.apply {
            data = barData
            description.isEnabled = false
            xAxis.setDrawGridLines(false)
            animateY(1000)
            invalidate()

            barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is BarEntry) {
                        val index = e.x.toInt()
                        if (index in sortedDates.indices) {
                            val date = sortedDates[index]
                            val filteredOrders = dateOrderMap[date] ?: emptyList()

                            // Update the TextView
                            binding.orderCompletedNum.text = filteredOrders.size.toString()

                            // Bind RecyclerView with filtered orders
                            bindRecyclerView(filteredOrders)
                        }
                    }
                }

                override fun onNothingSelected() {
                    binding.orderCompletedNum.text = "0"
                    bindRecyclerView(emptyList()) // Clear RecyclerView when nothing is selected
                }
            })
        }
    }



    private fun bindRecyclerView(orders: List<Order>) {
            orderAdapter = TradeOrderCompletedAdapter(orders, requireContext())
            binding.orderRV.layoutManager = LinearLayoutManager(requireContext())
            binding.orderRV.adapter = orderAdapter
    }
    fun fetchAllOrdersForUser(
        userID: String,
        onResult: (List<Order>) -> Unit,
        onError: (DatabaseError?) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val ordersRef = database.getReference("Order")
        val swapRequestsRef = database.getReference("SwapRequest")
        val productsRef = database.getReference("Product")

        val allOrders = mutableListOf<Order>()
        var tasksRemaining = 2 // Two main tasks: sale orders and swap orders

        // Helper function to decrement tasks and invoke onResult when done
        fun onTaskComplete() {
            tasksRemaining -= 1
            Log.d("TaskStatus", "Tasks remaining: $tasksRemaining")
            if (tasksRemaining == 0) {
                onResult(allOrders)
            }
        }

        // Fetch Sale Orders
        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (orderSnap in snapshot.children) {
                    val tradeType = orderSnap.child("tradeType").getValue(String::class.java)
                    val status = orderSnap.child("status").getValue(String::class.java)
                    val buyerID = orderSnap.child("buyerID").getValue(String::class.java)
                    val sellerID = orderSnap.child("sellerID").getValue(String::class.java)

                    if (tradeType == "Sale" && status == getString(R.string.ORDER_COMPLETED) &&
                        (buyerID == userID || sellerID == userID)
                    ) {
                        val orderID = orderSnap.key ?: ""
                        allOrders.add(Order(orderID, tradeType, status, buyerID, sellerID))
                    }
                }
                Log.d("SaleOrders", "Fetched sale orders: ${allOrders.size}")
                onTaskComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })

        // Fetch Swap Orders
        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(orderSnapshot: DataSnapshot) {
                val swapTasks = mutableListOf<() -> Unit>()
                var tasksRemaining = 0

                for (orderSnap in orderSnapshot.children) {
                    val tradeType = orderSnap.child("tradeType").getValue(String::class.java)
                    val status = orderSnap.child("status").getValue(String::class.java)
                    val swapRequestID = orderSnap.child("swapRequestID").getValue(String::class.java)

                    if (tradeType == "Swap" && status == getString(R.string.ORDER_COMPLETED) && swapRequestID != null) {
                        tasksRemaining++
                        swapRequestsRef.child(swapRequestID).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(swapRequestSnapshot: DataSnapshot) {
                                val receiverProductID = swapRequestSnapshot.child("receiverProductID").getValue(String::class.java)
                                val senderProductID = swapRequestSnapshot.child("senderProductID").getValue(String::class.java)

                                val productIDsToCheck = listOfNotNull(receiverProductID, senderProductID)
                                productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(productSnapshot: DataSnapshot) {
                                        for (productID in productIDsToCheck) {
                                            val product = productSnapshot.child(productID)
                                            val createdByUserID = product.child("created_by_UserID").getValue(String::class.java)

                                            if (createdByUserID == userID) {
                                                val orderID = orderSnap.key ?: ""
                                                allOrders.add(
                                                    Order(
                                                        orderID = orderID,
                                                        tradeType = tradeType ?: "",
                                                        status = status ?: "",
                                                        swapRequestID = swapRequestID,
                                                    )
                                                )
                                            }
                                        }

                                        // Decrement remaining tasks and check if all tasks are completed
                                        tasksRemaining--
                                        if (tasksRemaining == 0) {
                                            onTaskComplete()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        onError(error)
                                        tasksRemaining--
                                        if (tasksRemaining == 0) {
                                            onTaskComplete()
                                        }
                                    }
                                })
                            }

                            override fun onCancelled(error: DatabaseError) {
                                onError(error)
                                tasksRemaining--
                                if (tasksRemaining == 0) {
                                    onTaskComplete()
                                }
                            }
                        })
                    }
                }

                // If no tasks were added, call onTaskComplete immediately
                if (tasksRemaining == 0) {
                    onTaskComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })

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
