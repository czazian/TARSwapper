package com.example.tarswapper
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tarswapper.Chat
import com.example.tarswapper.R
import com.example.tarswapper.Setting
import com.example.tarswapper.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val fragmentManager = supportFragmentManager
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //Set initial fragment
        if (savedInstanceState == null) {
            //Clear any existing fragments from the back stack
            val fragmentManager = supportFragmentManager
            while (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            }

            //Check for SharePreference (Permanent Session) is Empty. If not Empty, allow user to directly access the app.
            //SharePreference is removed only when the user "Logout" from their account.
            val sharedPreferences = this.getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
            val userID = sharedPreferences.getString("userID", null) //Retrieve user ID, default is null if not found
            if(userID != null) {
                val transaction = fragmentManager.beginTransaction()
                val initialFragment: Fragment = UserProfile()
                transaction.replace(binding.frameLayout.id, initialFragment)
                transaction.commit()
            } else {
                //Otherwise, if SharePreference really really is empty, so redirect user for login.
                //After "Login" successful, store the userID as SharePreference, so it is not empty now.
                val transaction = fragmentManager.beginTransaction()
                val initialFragment: Fragment = Login()
                transaction.replace(binding.frameLayout.id, initialFragment)
                transaction.commit()
            }
        }


        //Bottom Navigation Redirection Processing
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.video -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = ChatSelection()
                    transaction.replace(binding.frameLayout.id, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                R.id.community -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = Chat()
                    transaction.replace(binding.frameLayout.id, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                R.id.tag -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = Chat()
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
}