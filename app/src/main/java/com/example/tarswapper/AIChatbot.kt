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
import com.google.gson.Gson
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AIChatbot : Fragment() {
    private lateinit var binding: FragmentAIChatbotBinding
    private var listOfConversation = ArrayList<Message>()
    var adapter: AIChatbotAdapter? = null

    private var cachedToken: String? = null
    private var tokenExpiryTime: Long = 0
    private var isProcessing: Boolean = false
    private var typingMessage:Message? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAIChatbotBinding.inflate(layoutInflater, container, false)


        //Check weather the conversation is empty
        if (listOfConversation.isNotEmpty()) {
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
        binding.faqBtn.setOnClickListener() {
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




                        //Add Pending Text
                        typingMessage = Message(
                            "...",
                            "AI",
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time),
                            null,
                            null
                        )
                        listOfConversation.add(typingMessage!!)
                        adapter?.notifyItemInserted(listOfConversation.size - 1)
                        binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)

                        binding.faqBtn.isEnabled = false
                        binding.btnPreQues1.isEnabled = false
                        binding.btnPreQues2.isEnabled = false
                    }
                }
            }
        }


        //Quick Question 1
        binding.btnPreQues1.setOnClickListener() {
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




                        //Add Pending Text
                        typingMessage = Message(
                            "...",
                            "AI",
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time),
                            null,
                            null
                        )
                        listOfConversation.add(typingMessage!!)
                        adapter?.notifyItemInserted(listOfConversation.size - 1)
                        binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)

                        binding.faqBtn.isEnabled = false
                        binding.btnPreQues1.isEnabled = false
                        binding.btnPreQues2.isEnabled = false
                    }
                }
            }
        }


        //Quick Question 2
        binding.btnPreQues2.setOnClickListener() {
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



                        //Add Pending Text
                        typingMessage = Message(
                            "...",
                            "AI",
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time),
                            null,
                            null
                        )
                        listOfConversation.add(typingMessage!!)
                        adapter?.notifyItemInserted(listOfConversation.size - 1)
                        binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)

                        binding.faqBtn.isEnabled = false
                        binding.btnPreQues1.isEnabled = false
                        binding.btnPreQues2.isEnabled = false
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
        lifecycleScope.launch(Dispatchers.Main) {
            // Add typing message (UI operation)
            binding.faqBtn.isEnabled = false
            binding.btnPreQues1.isEnabled = false
            binding.btnPreQues2.isEnabled = false

            //Move network call to IO dispatcher
            val responseResult = withContext(Dispatchers.IO) {
                try {
                    makeNetworkRequest(inputText) // Call function for your network request
                } catch (e: Exception) {
                    Log.e("NetworkError", e.toString())
                    null
                }
            }

            //Process response (back on Main thread for UI updates)
            responseResult?.let { fulfillmentText ->
                //Remove typing message
                val position = listOfConversation.indexOf(typingMessage)
                if (position != -1) {
                    listOfConversation.removeAt(position)
                    adapter?.notifyItemRemoved(position)
                }

                // Add response message
                val message = Message(
                    fulfillmentText, "AI",
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time),
                    null, null
                )
                listOfConversation.add(message)
                adapter?.notifyItemInserted(listOfConversation.size - 1)
                binding.aiRecyclerView.scrollToPosition(listOfConversation.size - 1)
            } ?: run {
                //Remove typing message
                val position = listOfConversation.indexOf(typingMessage)
                if (position != -1) {
                    listOfConversation.removeAt(position)
                    adapter?.notifyItemRemoved(position)
                }
                Toast.makeText(requireContext(), "Failed to get a response.", Toast.LENGTH_LONG).show()
                Log.e("AI Response", "Failed to get a response.")
            }

            binding.faqBtn.isEnabled = true
            binding.btnPreQues1.isEnabled = true
            binding.btnPreQues2.isEnabled = true
        }
    }


    private suspend fun makeNetworkRequest(inputText: String): String? {
        val projectID = "tarswapper-d4b2a"
        val agentID = "488f5e39-9441-4b76-8d5b-5550b0757c7b"
        val location = "us-central1"
        val sessionID = UUID.randomUUID().toString()

        return try {
            val requestQueue: RequestQueue = Volley.newRequestQueue(requireContext())
            val url =
                "https://$location-dialogflow.googleapis.com/v3/projects/$projectID/locations/$location/agents/$agentID/sessions/$sessionID:detectIntent"

            val requestBody = JSONObject().apply {
                put("queryInput", JSONObject().apply {
                    put("text", JSONObject().apply { put("text", inputText) })
                    put("languageCode", "en")
                })
                put("queryParams", JSONObject().apply { put("timeZone", "America/Los_Angeles") })
            }

            // Perform the network request
            suspendCoroutine<String?> { continuation ->
                val jsonObjectRequest = object : JsonObjectRequest(
                    Method.POST, url, requestBody,
                    { response ->
                        val fulfillmentText = response.optJSONObject("queryResult")
                            ?.optJSONArray("responseMessages")
                            ?.optJSONObject(0)
                            ?.optJSONObject("text")
                            ?.optJSONArray("text")
                            ?.optString(0)

                        continuation.resume(fulfillmentText)

                        Log.d("AI Response", response.toString())
                    },
                    { error ->
                        Log.e("VolleyError", "Error: ${error.message}")
                        error.networkResponse?.let { response ->
                            Log.e("VolleyError", "Response: ${String(response.data)}")
                        }
                        continuation.resume(null)


                        binding.faqBtn.isEnabled = true
                        binding.btnPreQues1.isEnabled = true
                        binding.btnPreQues2.isEnabled = true
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = mutableMapOf<String, String>()
                        headers["Authorization"] = "Bearer ${getAccessToken()}"
                        headers["Content-Type"] = "application/json"
                        return headers
                    }

                    override fun getRetryPolicy(): RetryPolicy {
                        return DefaultRetryPolicy(
                            5000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                        )
                    }
                }

                requestQueue.add(jsonObjectRequest)
            }
        } catch (e: Exception) {
            Log.e("NetworkRequestError", e.toString())

            binding.faqBtn.isEnabled = true
            binding.btnPreQues1.isEnabled = true
            binding.btnPreQues2.isEnabled = true
            null
        }
    }

    fun getAccessToken(): String {
        val currentTime = System.currentTimeMillis()
        if (cachedToken != null && currentTime < tokenExpiryTime) {
            return cachedToken!!
        }

        val inputStream: InputStream = resources.openRawResource(R.raw.dialogflow_service)
        val credentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/dialogflow"))

        credentials.refreshIfExpired()
        cachedToken = credentials.accessToken.tokenValue
        tokenExpiryTime = credentials.accessToken.expirationTime.time
        Log.d("AccessToken", cachedToken!!)
        return cachedToken!!
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

        // Portrait orientation: Reduced page size to fit more content
        val pageInfo = PdfDocument.PageInfo.Builder(1240, 1748, 1).create()  // Portrait (Width: 1240px, Height: 1748px)
        var currentPage = document.startPage(pageInfo)
        var canvas = currentPage.canvas

        val titlePaint = Paint().apply {
            textSize = 24f  // Smaller font size for titles
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            color = Color.rgb(33, 33, 33)
        }

        val headerPaint = Paint().apply {
            textSize = 18f  // Smaller font size for headers
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            color = Color.rgb(255, 255, 255)
        }

        val fieldPaint = Paint().apply {
            textSize = 18f  // Smaller font size for fields
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            color = Color.rgb(66, 66, 66)
        }

        val contentPaint = Paint().apply {
            textSize = 18f  // Smaller font size for content
            color = Color.rgb(66, 66, 66)
        }

        val tablePaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        val headerBackgroundPaint = Paint().apply {
            color = Color.rgb(63, 81, 181)
            style = Paint.Style.FILL
        }

        // Further reduced margins and padding
        val tableMargin = 10f  // Smaller margin
        val cellPadding = 5f  // Smaller padding
        val columnWidths = listOf(400f, 600f)  // Adjusted column widths to fit more content
        val rowHeight = 50f  // Reduced row height
        val startX = (pageInfo.pageWidth - columnWidths.sum() - tableMargin * 2) / 2
        var currentY = 80f  // Starting Y position with reduced space

        products.forEachIndexed { index, product ->
            // Check if we need a new page
            if (currentY + rowHeight * 10 > pageInfo.pageHeight - tableMargin) {
                document.finishPage(currentPage)
                currentPage = document.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = 80f  // Reset Y position for new page
            }

            val headerRect = RectF(startX, currentY, startX + columnWidths.sum(), currentY + rowHeight)
            canvas.drawRect(headerRect, headerBackgroundPaint)

            val headers = listOf("Field", "Value")
            drawTableRow(
                canvas = canvas,
                cells = headers,
                startX = startX,
                startY = currentY,
                fieldPaint = headerPaint,
                valuePaint = headerPaint,
                columnWidths = columnWidths,
                rowHeight = rowHeight,
                padding = cellPadding,
                isHeader = true
            )

            currentY += rowHeight

            // Optimized product details
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

                val requiredHeight = maxOf(rowHeight, wrappedValue.size * (contentPaint.textSize + cellPadding))

                if (currentY + requiredHeight > pageInfo.pageHeight - tableMargin) {
                    document.finishPage(currentPage)
                    currentPage = document.startPage(pageInfo)
                    canvas = currentPage.canvas
                    currentY = 80f  // Reset Y position for new page
                }

                drawTableRow(
                    canvas = canvas,
                    cells = listOf(key, wrappedValue.joinToString("\n")),
                    startX = startX,
                    startY = currentY,
                    fieldPaint = fieldPaint,
                    valuePaint = contentPaint,
                    columnWidths = columnWidths,
                    rowHeight = requiredHeight,
                    padding = cellPadding,
                    isHeader = false
                )

                canvas.drawRect(startX, currentY, startX + columnWidths.sum(), currentY + requiredHeight, tablePaint)
                currentY += requiredHeight
            }

            currentY += 20f  // Smaller space between products to fit more on the page
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
                textY += paint.textSize + padding / 2
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
            val originalFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
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
