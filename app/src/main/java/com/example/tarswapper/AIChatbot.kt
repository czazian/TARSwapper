package com.example.tarswapper

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.tarswapper.data.Product
import com.example.tarswapper.databinding.FragmentAIChatbotBinding
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream

import okhttp3.OkHttpClient

import java.util.UUID


class AIChatbot : Fragment() {
    private lateinit var binding: FragmentAIChatbotBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAIChatbotBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Go to User Profile Page
        binding.backChatButton.setOnClickListener() {
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


        ////Processing////
        //When the user enters AI Chatbot, it will get data from Firebase and export to Google Drive as JSONL
        exportToGoolgleStore()

        //Send Message to AI
        binding.faqBtn.setOnClickListener(){
            val question = binding.faqText.text.toString()
            if (question.isNotEmpty()) {

            }
        }

        return binding.root
    }


    ////START OF AI CHAT BOT////
    class VertexAIClient(private val context: Context) {

        //OAuthID : 654492757407-q3j609ven15l6vdlguc0k3cum2unk62o.apps.googleusercontent.com
        private val client = OkHttpClient()
        private val projectID = "tarswapper-d4b2a"
        private val agentID = "488f5e39-9441-4b76-8d5b-5550b0757c7b"
        private val location = "us-central1"
        private val sessionID = UUID.randomUUID().toString()

        //Updated endpoint for Vertex AI
        private val endpoint =
            "https://$location-dialogflow.googleapis.com/v3/projects/$projectID/locations/$location/agents/$agentID/sessions/$sessionID:detectIntent"


    }
    ////END OF AI CHAT BOT////








    private fun exportToGoolgleStore() {
        val database: DatabaseReference = FirebaseDatabase.getInstance().reference
        val productsRef = database.child("Product")

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val products = mutableListOf<Product>()
                for (productSnapshot in dataSnapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { products.add(it) }
                }

                val csvData = createPDFWithTable(products)
                storeToFirebaseStorage(csvData)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error fetching data: ${databaseError.message}")
            }
        })
    }

    private fun createPDFWithTable(products: List<Product>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint().apply { textSize = 12f }

        val columnWidths = arrayOf(60f, 80f, 150f, 70f, 60f, 60f, 90f)
        val startX = 40f
        var currentY = 60f

        paint.isFakeBoldText = true
        val headers = listOf(
            "Product ID",
            "Product Name",
            "Description",
            "Category",
            "Status",
            "Trade Type",
            "Created At"
        )

        headers.forEachIndexed { i, header ->
            canvas.drawText(header, startX + columnWidths.take(i).sum(), currentY, paint)
        }
        currentY += 30f
        paint.isFakeBoldText = false

        products.forEach { product ->
            val rowData = listOf(
                product.productID ?: "",
                product.name ?: "",
                product.description ?: "",
                product.category ?: "",
                product.status ?: "",
                product.tradeType ?: "",
                product.created_at ?: ""
            )

            // Calculate the maximum height required for this row based on text wrapping
            var rowHeight = 0f
            val wrappedTextLines = rowData.mapIndexed { i, text ->
                wrapTextToWidth(text, paint, columnWidths[i])
            }

            // Calculate the maximum row height from all wrapped text
            rowHeight = wrappedTextLines.maxOf { it.size } * (paint.textSize + 4f)

            // Draw each cell's text in this row
            wrappedTextLines.forEachIndexed { i, lines ->
                val cellX = startX + columnWidths.take(i).sum()
                var cellY = currentY

                lines.forEach { line ->
                    canvas.drawText(line, cellX, cellY, paint)
                    cellY += paint.textSize + 4f
                }
            }
            currentY += rowHeight + 10f // Move Y position for the next row
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    private fun wrapTextToWidth(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line = ""

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) <= maxWidth) {
                line = testLine
            } else {
                lines.add(line)
                line = word
            }
        }
        if (line.isNotEmpty()) {
            lines.add(line)
        }
        return lines
    }


    private fun storeToFirebaseStorage(pdfData: ByteArray) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val pdfRef = storageRef.child("Dataset/products.pdf")

        val metadata = StorageMetadata.Builder()
            .setContentType("application/pdf")
            .build()

        val uploadTask = pdfRef.putBytes(pdfData, metadata)

        uploadTask.addOnSuccessListener {
            Log.d("Upload PDF", "PDF file with table successfully uploaded.")
        }.addOnFailureListener { exception ->
            Log.e("Upload PDF Error", "Upload failed: ${exception.message}")
        }
    }
}
