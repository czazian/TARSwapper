package com.example.tarswapper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.tarswapper.Chat
import com.example.tarswapper.R
import com.example.tarswapper.Setting
import com.example.tarswapper.databinding.ActivityMainBinding
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
            val transaction = fragmentManager.beginTransaction()
            val initialFragment = Chat()
            transaction.replace(binding.frameLayout.id, initialFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.video -> {
                    val transaction = fragmentManager.beginTransaction()
                    val fragment = Chat()
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
                    val fragment = Setting()
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