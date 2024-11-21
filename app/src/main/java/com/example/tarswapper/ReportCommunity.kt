package com.example.tarswapper

import android.content.Context
import android.graphics.Color
import androidx.fragment.app.Fragment
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentReportCommunityEngagementBinding
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.data.Community
import com.example.tarswapper.dataAdapter.CommunityMyPostAdapter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.database.*

class ReportCommunity : Fragment() {
    private lateinit var binding: FragmentReportCommunityEngagementBinding
    private lateinit var userObj: User

    private lateinit var database: DatabaseReference
    private val communityList = mutableListOf<com.example.tarswapper.data.Community>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportCommunityEngagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().getReference("Community")

        // Load data and populate the report
        fetchCommunityData(userID.toString())

        binding.btnBack.setOnClickListener{
            val fragment = com.example.tarswapper.Community()

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.addToBackStack(null)
            transaction?.commit()
        }
    }

    private fun fetchCommunityData(userID: String) {
        // Query to filter communities by created_by_UserID
        val query = database.orderByChild("created_by_UserID").equalTo(userID)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                communityList.clear()
                for (communitySnap in snapshot.children) {
                    val community = communitySnap.getValue(Community::class.java)
                    community?.let { communityList.add(it) }
                }
                if (communityList.isNotEmpty()) {
                    populateReport()
                } else {
                    Toast.makeText(requireContext(), "No data available for the user.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", error.message)
            }
        })
    }

    private fun populateReport() {
        // Populate Summary Section
        val totalViews = communityList.sumOf { it.view ?: 0 }
        val averageViews = if (communityList.isNotEmpty()) totalViews / communityList.size else 0
        val topViewedCommunity = communityList.maxByOrNull { it.view ?: 0 }
        val leastViewedCommunity = communityList.minByOrNull { it.view ?: 0 }

        binding.totalViews.text = totalViews.toString()
        binding.averageViews.text = averageViews.toString()
        binding.totalPost.text = communityList.size.toString()

        setupPieChart()

        // Populate Bar Chart (Engagement Distribution)
        setupBarChart(binding.engagementBarChart)

        // Populate RecyclerView (Top 5 Most Viewed Communities)
        setupRecyclerView()

        // Populate Line Chart (Engagement Over Time)
        setupLineChart(binding.engagementLineChart)
    }

    private fun setupPieChart() {
        val pieChart = binding.pieChart

        // Calculate total views
        val totalViews = communityList.sumOf { it.view ?: 0 }

        // Create Pie Entries
        val entries = communityList.map { community ->
            PieEntry(
                (community.view ?: 0).toFloat(),
                community.title ?: "Unknown"
            )
        }

        // Set up Pie DataSet
        val dataSet = PieDataSet(entries, "Community Views")
        dataSet.colors = listOf(
            Color.parseColor("#FF6F61"),
            Color.parseColor("#6AB6FF"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#81C784"),
            Color.parseColor("#9575CD")
        ) // Customize colors

        // Customize Pie Chart Appearance
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        // Set up Pie Data
        val pieData = PieData(dataSet)
        pieData.setValueTextSize(12f)
        pieData.setValueFormatter(PercentFormatter(pieChart)) // Display percentages

        // Configure Pie Chart
        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.centerText = "Community Views"
        pieChart.setCenterTextSize(16f)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(Color.BLACK)

        pieChart.invalidate() // Refresh chart
    }

    private fun setupBarChart(barChart: BarChart) {
        val entries = communityList.mapIndexed { index, community ->
            BarEntry(index.toFloat(), (community.view ?: 0).toFloat())
        }
        val dataSet = BarDataSet(entries, "Community Views")
        val data = BarData(dataSet)
        barChart.data = data
        barChart.invalidate() // Refresh chart
    }

    private fun setupRecyclerView() {
        binding.topViewedCommunitiesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        val topCommunities = communityList.sortedByDescending { it.view ?: 0 }.take(5)
        binding.topViewedCommunitiesRecyclerView.adapter = CommunityMyPostAdapter(topCommunities, requireContext())
    }

    private fun setupLineChart(lineChart: LineChart) {
        val entries = communityList.mapIndexed { index, community ->
            com.github.mikephil.charting.data.Entry(index.toFloat(), (community.view ?: 0).toFloat())
        }
        val dataSet = LineDataSet(entries, "Engagement Over Time")
        val data = LineData(dataSet)
        lineChart.data = data
        lineChart.invalidate() // Refresh chart
    }


}
