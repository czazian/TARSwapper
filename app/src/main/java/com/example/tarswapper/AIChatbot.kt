package com.example.tarswapper

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.Product
import com.example.tarswapper.dataAdapter.AIChatbotAdapter
import com.example.tarswapper.dataAdapter.ChatAdapter
import com.example.tarswapper.databinding.FragmentAIChatbotBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import java.util.UUID


class AIChatbot : Fragment() {
    private lateinit var binding: FragmentAIChatbotBinding
    private var listOfConversation = ArrayList<Message>()
    var adapter: AIChatbotAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAIChatbotBinding.inflate(layoutInflater, container, false)


        //Check weather the conversation is empty
        if(listOfConversation.isNotEmpty()) {
            binding.imageView.visibility = View.GONE
            binding.previewTextAI.visibility = View.GONE
        } else {
            binding.imageView.visibility = View.VISIBLE
            binding.previewTextAI.visibility = View.VISIBLE
        }


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
        //When the user enters AI Chat bot, it will get data from Firebase and export to Google Drive as JSONL
        exportToGoogleStore()

        //Assign Adapter and Update it
        ////Bind the messages to the RecyclerView////
        adapter = AIChatbotAdapter(
            requireContext(),
            listOfConversation,
        )
        binding.aiRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.aiRecyclerView.adapter = adapter

        //Send Message to AI
        binding.faqBtn.setOnClickListener(){
            binding.imageView.visibility = View.GONE
            binding.previewTextAI.visibility = View.GONE
            val question = binding.faqText.text.toString()

            if (question.isNotEmpty()) {

                lifecycleScope.launch(Dispatchers.IO) {

                    //Send Request
                    sendRequestToAgent(question)

                    withContext(Dispatchers.Main) {
                        //Add Question to the List
                        val message = Message(
                            question,
                            "User",
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Calendar.getInstance().time),
                            null,
                            null,
                        )
                        listOfConversation.add(message)


                        //Notify the adapter that the new item has been added at the last position
                        adapter?.notifyItemInserted(listOfConversation.size - 1)

                        //Optionally, scroll to the bottom of the RecyclerView to show the latest message
                        binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)

                        //Clear Text
                        binding.faqText.setText("")

                    }
                }
            }
        }


        //Quick Question 1
        binding.btnPreQues1.setOnClickListener(){
            binding.imageView.visibility = View.GONE
            binding.previewTextAI.visibility = View.GONE
            val question = "What is TARSwapper?"

            if (question.isNotEmpty()) {

                lifecycleScope.launch(Dispatchers.IO) {

                    //Send Request
                    sendRequestToAgent(question)

                    withContext(Dispatchers.Main) {
                        //Add Question to the List
                        val message = Message(
                            question,
                            "User",
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Calendar.getInstance().time),
                            null,
                            null,
                        )
                        listOfConversation.add(message)


                        //Notify the adapter that the new item has been added at the last position
                        adapter?.notifyItemInserted(listOfConversation.size - 1)

                        //Optionally, scroll to the bottom of the RecyclerView to show the latest message
                        binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)

                    }
                }
            }
        }


        //Quick Question 2
        binding.btnPreQues2.setOnClickListener(){
            binding.imageView.visibility = View.GONE
            binding.previewTextAI.visibility = View.GONE

            val question = "What can you do?"

            if (question.isNotEmpty()) {

                lifecycleScope.launch(Dispatchers.IO) {

                    //Send Request
                    sendRequestToAgent(question)

                    withContext(Dispatchers.Main) {
                        //Add Question to the List
                        val message = Message(
                            question,
                            "User",
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Calendar.getInstance().time),
                            null,
                            null,
                        )
                        listOfConversation.add(message)


                        //Notify the adapter that the new item has been added at the last position
                        adapter?.notifyItemInserted(listOfConversation.size - 1)

                        //Optionally, scroll to the bottom of the RecyclerView to show the latest message
                        binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)

                    }
                }
            }
        }


        return binding.root
    }







    ////START OF AI CHAT BOT////
    private fun sendRequestToAgent(inputText: String) {
        val projectID = "tarswapper-d4b2a"
        val agentID = "488f5e39-9441-4b76-8d5b-5550b0757c7b"
        val location = "us-central1"
        val sessionID = UUID.randomUUID().toString()

        val url = "https://$location-dialogflow.googleapis.com/v3/projects/$projectID/locations/$location/agents/$agentID/sessions/$sessionID:detectIntent"

        val requestBody = JSONObject().apply {
            put("queryInput", JSONObject().apply {
                put("text", JSONObject().apply {
                    put("text", inputText)  //User input text
                })
                put("languageCode", "en")  //Language code
            })

            //Adding the queryParams with timeZone
            put("queryParams", JSONObject().apply {
                put("timeZone", "America/Los_Angeles")
            })
        }

        Log.d("RequestBody", requestBody.toString())

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = withContext(Dispatchers.IO) {
                    val requestQueue: RequestQueue = Volley.newRequestQueue(requireContext())

                    val jsonObjectRequest = object : JsonObjectRequest(
                        Method.POST,
                        url,
                        requestBody,
                        { response ->
                            val responseText = response.toString()
                            Log.d("API Response", responseText)

                            val responseMessages = response.getJSONObject("queryResult")
                                .getJSONArray("responseMessages")

                            //Check if the responseMessages array contains a text response
                            if (responseMessages.length() > 0) {
                                val fulfillmentText = responseMessages.getJSONObject(0)
                                    .optJSONObject("text")?.optJSONArray("text")?.optString(0)

                                if (!fulfillmentText.isNullOrEmpty()) {
                                    // Add the answer from AI to the conversation
                                    val message = Message(
                                        fulfillmentText,
                                        "AI",
                                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time),
                                        null,
                                        null
                                    )
                                    listOfConversation.add(message)

                                    // Notify the adapter and scroll to the new message
                                    adapter?.notifyItemInserted(listOfConversation.size - 1)
                                    binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)
                                } else {
                                    // Handle case where no text response was found
                                    Log.e("Dialogflow Error", "No text response found.")
                                }
                            } else {
                                Log.e("Dialogflow Error", "Response messages are empty.")
                            }


                        },
                        { error ->
                            Log.e("VolleyError", error.toString())
                            error.networkResponse?.let {
                                Log.e("VolleyErrorResponse", String(it.data))
                            }
                            Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_LONG).show()
                        }) {


                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = mutableMapOf<String, String>()
                            headers["Authorization"] = "Bearer ${getAccessToken()}"
                            headers["Content-Type"] = "application/json"
                            Log.d("RequestHeaders", headers.toString())
                            return headers
                        }
                    }
                    requestQueue.add(jsonObjectRequest)
                }
            } catch (e: Exception) {
                Log.e("NetworkError", e.toString())
            }
        }
    }

    fun getAccessToken(): String {
        val inputStream: InputStream = resources.openRawResource(R.raw.dialogflow_service)
        val credentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/dialogflow"))

        credentials.refreshIfExpired()

        Log.d("AccessToken", credentials.accessToken.tokenValue)
        return credentials.accessToken.tokenValue
    }
    ////END OF AI CHAT BOT////














    //Updating the Product List of PDF for the Manual Update of AI Learning Process
    private fun exportToGoogleStore() {
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

            //Calculate the maximum row height from all wrapped text
            rowHeight = wrappedTextLines.maxOf { it.size } * (paint.textSize + 4f)

            //Draw each cell's text in this row
            wrappedTextLines.forEachIndexed { i, lines ->
                val cellX = startX + columnWidths.take(i).sum()
                var cellY = currentY

                lines.forEach { line ->
                    canvas.drawText(line, cellX, cellY, paint)
                    cellY += paint.textSize + 4f
                }
            }
            currentY += rowHeight + 10f
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
