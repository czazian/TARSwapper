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
import com.example.tarswapper.Chat
import com.example.tarswapper.R
import com.example.tarswapper.Setting
import com.example.tarswapper.databinding.ActivityMainBinding
import com.example.tarswapper.game.GameOver
import com.example.tarswapper.game.GameView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val fragmentManager = supportFragmentManager
    private lateinit var binding: ActivityMainBinding
    private var isBackPressPrevented = false

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

    fun restartActivity() {
        val intent = intent
        finish()
        startActivity(intent)
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
}