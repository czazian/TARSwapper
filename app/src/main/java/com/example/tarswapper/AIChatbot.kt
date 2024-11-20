package com.example.tarswapper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tarswapper.data.Message
import com.example.tarswapper.data.Product
import com.example.tarswapper.dataAdapter.AIChatbotAdapter
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
import org.json.JSONArray
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


        //When the text box is not empty, hide scroll faq
        binding.faqText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.scrollFAQ.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            binding.scrollFAQ.visibility = View.GONE
                        }
                        .start()
                } else {
                    binding.scrollFAQ.visibility = View.VISIBLE
                    binding.scrollFAQ.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })



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
                    put("text", inputText)  // User input text
                })
                put("languageCode", "en")  // Language code
            })

            // Adding the queryParams with timeZone
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

                            val queryResult = response.optJSONObject("queryResult")
                            if (queryResult != null) {
                                val responseMessages = queryResult.optJSONArray("responseMessages")

                                // Check if responseMessages is not null and contains data
                                if (responseMessages != null && responseMessages.length() > 0) {
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
                                        Log.e("Dialogflow Error", "No text response found.")
                                    }
                                } else {
                                    Log.e("Dialogflow Error", "Response messages are empty or null.")
                                }
                            } else {
                                Log.e("Dialogflow Error", "queryResult is null.")
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

                        override fun getRetryPolicy(): RetryPolicy {
                            //Retry policy example with exponential backoff
                            return DefaultRetryPolicy(
                                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                                3,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                            )
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
        val token = credentials.accessToken.tokenValue
        Log.d("AccessToken", token)
        return token
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

                val csvData = createPDFWithIndividualTables(products)
                storeToFirebaseStorage(csvData)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error fetching data: ${databaseError.message}")
            }
        })
    }






    private fun createPDFWithIndividualTables(products: List<Product>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = PdfDocument()

        // Page settings - A4 size in landscape orientation
        val pageInfo = PdfDocument.PageInfo.Builder(2480, 1754, 1).create()
        var currentPage = document.startPage(pageInfo)
        var canvas = currentPage.canvas

        // Enhanced styling
        val titlePaint = Paint().apply {
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            color = Color.rgb(33, 33, 33)
        }

        val headerPaint = Paint().apply {
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            color = Color.rgb(255, 255, 255)
        }

        // New: Separate paint for field names (bold)
        val fieldPaint = Paint().apply {
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            color = Color.rgb(66, 66, 66)
        }

        // Regular paint for values
        val contentPaint = Paint().apply {
            textSize = 30f
            color = Color.rgb(66, 66, 66)
        }

        val tablePaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        val headerBackgroundPaint = Paint().apply {
            color = Color.rgb(63, 81, 181) // Material Design primary color
            style = Paint.Style.FILL
        }

        // Table dimensions
        val tableMargin = 50f
        val cellPadding = 20f
        val columnWidths = listOf(600f, 1000f)
        val rowHeight = 100f
        val startX = (pageInfo.pageWidth - columnWidths.sum() - tableMargin * 2) / 2
        var currentY = 150f

        products.forEachIndexed { index, product ->
            // Check if we need a new page
            if (currentY + rowHeight * 10 > pageInfo.pageHeight - tableMargin) {
                document.finishPage(currentPage)
                currentPage = document.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = 150f
            }

            // Draw product title
            canvas.drawText("Product #${index + 1}", startX, currentY - 30f, titlePaint)

            // Draw table header background
            val headerRect = RectF(startX, currentY, startX + columnWidths.sum(), currentY + rowHeight)
            canvas.drawRect(headerRect, headerBackgroundPaint)

            // Draw header texts
            val headers = listOf("Field", "Value")
            drawTableRow(
                canvas = canvas,
                cells = headers,
                startX = startX,
                startY = currentY,
                fieldPaint = headerPaint, // Use header paint for both columns in header
                valuePaint = headerPaint,
                columnWidths = columnWidths,
                rowHeight = rowHeight,
                padding = cellPadding,
                isHeader = true
            )

            currentY += rowHeight

            // Product details
            val productDetails = listOf(
                "Product ID" to (product.productID ?: ""),
                "Product Name" to (product.name ?: ""),
                "Product Description" to (product.description ?: ""),
                "Category" to (product.category ?: ""),
                "Condition" to (product.condition ?: ""),
                "Status" to (product.status ?: ""),
                "Trade Type" to (product.tradeType ?: ""),
                "Created At" to formatDateTime(product.created_at ?: "")
            )

            productDetails.forEach { (key, value) ->
                val wrappedValue = wrapTextToWidth(value, contentPaint, columnWidths[1] - cellPadding * 2)

                // Calculate required height for wrapped text
                val requiredHeight = maxOf(rowHeight, wrappedValue.size * (contentPaint.textSize + cellPadding))

                // Check if we need a new page
                if (currentY + requiredHeight > pageInfo.pageHeight - tableMargin) {
                    document.finishPage(currentPage)
                    currentPage = document.startPage(pageInfo)
                    canvas = currentPage.canvas
                    currentY = 150f
                }

                // Draw the row with wrapped text
                drawTableRow(
                    canvas = canvas,
                    cells = listOf(key, wrappedValue.joinToString("\n")),
                    startX = startX,
                    startY = currentY,
                    fieldPaint = fieldPaint,  // Use bold paint for field names
                    valuePaint = contentPaint, // Use regular paint for values
                    columnWidths = columnWidths,
                    rowHeight = requiredHeight,
                    padding = cellPadding,
                    isHeader = false
                )

                // Draw cell borders
                canvas.drawRect(
                    startX,
                    currentY,
                    startX + columnWidths.sum(),
                    currentY + requiredHeight,
                    tablePaint
                )

                currentY += requiredHeight
            }

            currentY += 100f // Space between products
        }

        document.finishPage(currentPage)
        document.writeTo(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    private fun drawTableRow(
        canvas: Canvas,
        cells: List<String>,
        startX: Float,
        startY: Float,
        fieldPaint: Paint,  // Paint for field names (left column)
        valuePaint: Paint,  // Paint for values (right column)
        columnWidths: List<Float>,
        rowHeight: Float,
        padding: Float,
        isHeader: Boolean
    ) {
        var currentX = startX

        cells.forEachIndexed { index, cellText ->
            val cellRect = RectF(
                currentX,
                startY,
                currentX + columnWidths[index],
                startY + rowHeight
            )

            // Use appropriate paint based on column
            val paint = if (index == 0) fieldPaint else valuePaint

            // Draw text with wrapping
            val lines = cellText.split("\n")
            val textX = currentX + padding
            var textY = startY + padding + paint.textSize

            lines.forEach { line ->
                canvas.drawText(line, textX, textY, paint)
                textY += paint.textSize + padding/2
            }

            // Draw vertical line
            canvas.drawLine(
                currentX,
                startY,
                currentX,
                startY + rowHeight,
                paint
            )

            currentX += columnWidths[index]
        }

        // Draw final vertical line
        canvas.drawLine(
            currentX,
            startY,
            currentX,
            startY + rowHeight,
            fieldPaint
        )
    }

    private fun wrapTextToWidth(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder(words[0])

        for (i in 1 until words.size) {
            val word = words[i]
            val testLine = "${currentLine} $word"

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(" $word")
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }

        lines.add(currentLine.toString())
        return lines
    }

    private fun formatDateTime(input: String): String {
        return try {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val date = originalFormat.parse(input)
            val newFormat = SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
            newFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
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
            Log.d("Upload PDF", "PDF file with individual tables successfully uploaded.")

        }.addOnFailureListener { exception ->
            Log.e("Upload PDF Error", "Upload failed: ${exception.message}")
        }
    }
}
