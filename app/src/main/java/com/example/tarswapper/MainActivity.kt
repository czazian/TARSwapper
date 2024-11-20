package com.example.tarswapper

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tarswapper.Chat
import com.example.tarswapper.R
import com.example.tarswapper.Setting
import com.example.tarswapper.databinding.ActivityMainBinding
import com.example.tarswapper.game.GameOver
import com.example.tarswapper.game.GameView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val fragmentManager = supportFragmentManager
    private lateinit var binding: ActivityMainBinding
    private var isBackPressPrevented = false
    private var isUpdating = false

    //Check network connectivity - If network connection is not available, close the app
    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.sleep(1500)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //Check network connection at the start of the activity
        if (!isNetworkConnected()) {
            //When no internet connection
            AlertDialog
                .Builder(this)
                .setTitle("Internet Connection Error")
                .setMessage("No internet connection. Please try again!")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .create()
                .show()

            return
        }


        //Check if a day pass. If a day past, update the Product Data Store for AI Chat bot
        shouldRunDailyUpdate()


        //Set initial fragment
        if (savedInstanceState == null) {
            //Clear any existing fragments from the back stack
            val fragmentManager = supportFragmentManager
            while (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            }

            //Check for SharePreference (Permanent Session) is Empty. If not Empty, allow user to directly access the app.
            //SharePreference is removed only when the user "Logout" from their account.
            val sharedPreferences =
                this.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
            val userID = sharedPreferences.getString(
                "userID",
                null
            ) //Retrieve user ID, default is null if not found
            if (userID != null) {
                val navigationView =
                    this.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                navigationView.selectedItemId = R.id.setting

                val transaction = fragmentManager.beginTransaction()
                val initialFragment: Fragment = UserProfile()
                transaction.replace(binding.frameLayout.id, initialFragment)
                transaction.commit()
            } else {
                //Otherwise, if SharePreference really really is empty, so redirect user for login.
                //After "Login" successful, store the userID as SharePreference, so it is not empty now.
                val transaction = fragmentManager.beginTransaction()
                val initialFragment: Fragment = Started()
                transaction.replace(binding.frameLayout.id, initialFragment)
                transaction.commit()
            }
        }


        //Bottom Navigation Redirection Processing
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.video -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = Video()
                    transaction.replace(binding.frameLayout.id, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                R.id.community -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = Community()
                    transaction.replace(binding.frameLayout.id, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                R.id.tag -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = Trade()
                    transaction.replace(binding.frameLayout.id, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                R.id.chat -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = ChatSelection()
                    transaction.replace(binding.frameLayout.id, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                R.id.setting -> {
                    // After a successful login, show the frameLayout and hide the businessProfileLayout
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = UserProfile()
                    transaction.replace(binding.frameLayout.id, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
            }
            true
        }
    }

    override fun onBackPressed() {
        //Check if the current fragment is the one you want to prevent back navigation from
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)

        //Disable back button in these fragment/view
        if (currentFragment is MiniGame && isBackPressPrevented) {
            Log.e("BackButtonDisabled", "Back button disabled in this screen");
        } else if (currentFragment is GameView && isBackPressPrevented) {
            Log.e("BackButtonDisabled", "Back button disabled in this screen");
        } else if (currentFragment is GameOver && isBackPressPrevented) {
            Log.e("BackButtonDisabled", "Back button disabled in this screen");
        } else {
            //Call super to handle back navigation normally
            super.onBackPressed()
        }
    }

    fun setBackPressPrevented(prevent: Boolean) {
        isBackPressPrevented = prevent
    }

    //Check if a day past, if yes, update the product data store
    private fun shouldRunDailyUpdate() {
        val database = FirebaseDatabase.getInstance().reference
        val timestampRef = database.child("lastUpdateDate")

        timestampRef.get().addOnSuccessListener { snapshot ->
            val lastUpdateDateString = snapshot.getValue(String::class.java)

            val currentDate = getCurrentDateString()

            //Log both the current date and the last update date for debugging purposes
            Log.d("VertexAI", "Last update date: $lastUpdateDateString, Current date: $currentDate")

            //Compare current date with the last update date
            if (lastUpdateDateString == null || currentDate != lastUpdateDateString) {
                //If the last update date is null or not matching today's date, run the update task
                startUpdateDataStore()

                Log.e("VertexAI", "Product Data Store Updated!")

                //Update the last update date to today's date
                timestampRef.setValue(currentDate)
            } else {
                Log.d("VertexAI", "No update needed, last update is already today's date.")
            }
        }.addOnFailureListener { exception ->
            Log.e("FirebaseDatabase", "Error reading last update date: ${exception.message}")
        }
    }

    //Function to get current date as String in yyyy-MM-dd format
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    ////Start Update Data Store////
    private fun startUpdateDataStore() {
        //Prevent multiple simultaneous updates
        if (isUpdating) return
        isUpdating = true


        val storageRef = FirebaseStorage.getInstance().reference
        val pdfRef = storageRef.child("Dataset/products.pdf")

        pdfRef.downloadUrl.addOnSuccessListener { uri ->
            //Convert Firebase Storage URL to GCS format
            val gcsUri = "gs://tarswapper-d4b2a.appspot.com/Dataset/products.pdf"

            //Proceed to upload the GCS URI to Google Vertex AI
            uploadToVertexAI(gcsUri)
        }.addOnFailureListener { exception ->
            Log.e("FirebaseStorage", "Error generating URL for PDF: ${exception.message}")
        }
    }

    private fun uploadToVertexAI(gcsUri: String) {
        val projectID = "tarswapper-d4b2a"
        val location = "us"
        val collectionID = "default_collection"
        val dataStoreID = "products_1730897591056"
        val branchID = "default_branch"

        //Correct API endpoint
        val url = "https://us-discoveryengine.googleapis.com/v1/projects/$projectID/locations/$location/collections/$collectionID/dataStores/$dataStoreID/branches/$branchID/documents:import"

        //Build the request body
        val requestBody = JSONObject().apply {
            put("gcsSource", JSONObject().apply {
                put("inputUris", JSONArray().apply {
                    put(gcsUri)
                })
                put("dataSchema", "content")
            })
            put("reconciliationMode", "FULL")
            put("errorConfig", JSONObject().apply {
                put("gcsPrefix", "gs://tarswapper-d4b2a.appspot.com/errors/")
            })
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            url,
            requestBody,
            { response ->
                Log.d("VertexAI", "Data store update successful: $response")
            },
            { error ->
                Log.e("VertexAI", "Error updating data store", error)
                error.networkResponse?.let { networkResponse ->
                    Log.e("VertexAI", "Error response code: ${networkResponse.statusCode}")
                    val responseBody = String(networkResponse.data)
                    Log.e("VertexAI", "Error response body: $responseBody")
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = mutableMapOf<String, String>()
                headers["Authorization"] = "Bearer ${getAccessTokenDiscoveryEngine()}"
                headers["Content-Type"] = "application/json"
                Log.d("RequestHeaders", headers.toString())
                return headers
            }
        }

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

    fun getAccessTokenDiscoveryEngine(): String {
        val inputStream: InputStream = resources.openRawResource(R.raw.discovery_engine)
        val credentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

        credentials.refreshIfExpired()

        val token = credentials.accessToken.tokenValue
        Log.d("DiscoveryEngineAccessToken", token)
        return token
    }
    ////End Update Data Store////



}