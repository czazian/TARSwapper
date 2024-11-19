package com.example.tarswapper

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.example.tarswapper.data.MeetUp
import com.example.tarswapper.data.Order
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.databinding.FragmentIdentityVerificationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class IdentityVerification : Fragment() {
    private lateinit var binding: FragmentIdentityVerificationBinding
    private lateinit var codeScanner: CodeScanner
    private lateinit var scannerView: CodeScannerView

    private var verificationCode: String? = ""
    private var meetUpIDGlobal: String? = ""
    private var successDialog: AlertDialog? = null
    private var hasShownSuccessDialog = false
    private var previousVerifiedStatus: Boolean? = null
    private var transfer: Order? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentIdentityVerificationBinding.inflate(inflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Get Current User ID
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)




        //Get received data from previous fragment
        val transaction = arguments?.getSerializable("order") as? Order

        if(transaction != null){
            listenForMeetUpUpdates(transaction.meetUpID.toString())
        }

        //Get sender product user id and receive product user id
        lifecycleScope.launch {
            transaction?.let {

                transfer = transaction

                when(transaction.tradeType){
                    "Sale" -> {
                        if (transaction.sellerID == userID || transaction.buyerID == userID) {
                            transaction!!.meetUpID?.let { meetUpID ->
                                getMeetUpObject(meetUpID) { meetUp ->
                                    if (meetUp != null) {
                                        //Store meetup id
                                        meetUpIDGlobal = transaction!!.meetUpID

                                        //Proceed only if the meetup is valid
                                        verificationCode = meetUp.verificationCode.toString()

                                        Log.d("TEST IV", "Verification Code: $verificationCode")

                                        val qrBitmap = generateQRCode(verificationCode!!)
                                        qrBitmap?.let {
                                            binding.qrcode.setImageBitmap(qrBitmap)
                                        }
                                    } else {
                                        Log.e("MeetUp", "MeetUp does not exist or is not authorized.")
                                    }
                                }
                            }
                        } else {
                            Log.e("Authorization", "Transaction does not belong to this user.")
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Unauthorized access!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                    "Swap" -> {
                        //Use the SwapRequestID in the Order to fetch sender and receiver product IDs
                        val swapRequestID = transaction.swapRequestID
                        val swapRequestDetails = swapRequestID?.let { id -> getSwapRequestDetails(id) }
                        val senderProductID = swapRequestDetails?.senderProductID
                        val receiverProductID = swapRequestDetails?.receiverProductID

                        Log.d("TEST IV", "Current User ID: $userID")
                        Log.d("TEST IV", "SwapRequest ID: $swapRequestID")
                        Log.d("TEST IV", "Sender Product ID: $senderProductID")
                        Log.d("TEST IV", "Receiver Product ID: $receiverProductID")

                        //Fetch user IDs for the sender and receiver products
                        val senderUserID = senderProductID?.let { id -> getUserIDForProduct(id) }
                        val receiverUserID = receiverProductID?.let { id -> getUserIDForProduct(id) }

                        //Get Verification Code - Ensure the correct parties are involves in the same transaction
                        if (senderUserID == userID || receiverUserID == userID) {
                            transaction!!.meetUpID?.let { meetUpID ->
                                getMeetUpObject(meetUpID) { meetUp ->
                                    if (meetUp != null) {
                                        //Store meetup id
                                        meetUpIDGlobal = transaction!!.meetUpID

                                        //Proceed only if the meetup is valid
                                        verificationCode = meetUp.verificationCode.toString()

                                        Log.d("TEST IV", "Verification Code: $verificationCode")

                                        val qrBitmap = generateQRCode(verificationCode!!)
                                        qrBitmap?.let {
                                            binding.qrcode.setImageBitmap(qrBitmap)
                                        }
                                    } else {
                                        Log.e("MeetUp", "MeetUp does not exist or is not authorized.")
                                    }
                                }
                            }
                        } else {
                            Log.e("Authorization", "Transaction does not belong to this user.")
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Unauthorized access!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                    else -> {}
                }
            }
        }



        //////QR CODE SCANNER/////
        //Initialize the CodeScannerView
        scannerView = LayoutInflater.from(context)
            .inflate(R.layout.qrcode_scanner, null) as CodeScannerView
        //Add the scannerView to the fragment's layout
        (binding.identityVerificationContainer as ConstraintLayout).addView(scannerView)
        //Initialize CodeScanner
        codeScanner = CodeScanner(requireActivity(), scannerView)
        //Setup CodeScanner parameters
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = com.budiyev.android.codescanner.ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false
        //Set decode and error callbacks
        codeScanner.decodeCallback = DecodeCallback {
            if (isAdded) {
                requireActivity().runOnUiThread {
                    scannerView.visibility = View.GONE

                    //If verification code is the same
                    if (verificationCode != "") {
                        if (it.text == verificationCode.toString()) {

                            if (meetUpIDGlobal != null) {
                                updateMeetUpStatus(
                                    meetUpIDGlobal!!,
                                    verified = true,
                                    completed = false
                                )
                            }

                        } else {

                            if (meetUpIDGlobal != null) {
                                updateMeetUpStatus(
                                    meetUpIDGlobal!!,
                                    verified = false,
                                    completed = false
                                )
                            }

                            //If verification code is not the same
                            val dialogView =
                                layoutInflater.inflate(R.layout.unsuccessful_verification, null)
                            val dialog = AlertDialog.Builder(requireContext())
                                .setView(dialogView)
                                .create()

                            val button = dialogView.findViewById<Button>(R.id.btnOkUnsuccess)

                            button.setOnClickListener() {
                                dialog.dismiss()
                            }

                            val window = dialog.window
                            window?.setBackgroundDrawableResource(android.R.color.transparent)

                            dialog.show()

                            binding.afterSuccessful.visibility = View.GONE
                            binding.identityMainContent.visibility = View.VISIBLE

                        }
                    }
                }
            }
        }
        codeScanner.errorCallback = ErrorCallback {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        //Request camera permission
        checkPermission(Manifest.permission.CAMERA, 200)


        //Set up button click listener to start the scanner
        binding.btnScanQR.setOnClickListener {
            scannerView.visibility = View.VISIBLE // Show scanner when button clicked
            codeScanner.startPreview() // Start the scanning process
        }


        //Back to the Notification Page
        binding.btnBackNavigation.setOnClickListener() {
            val fragment = Notification()

            //Back to previous page with animation
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.frameLayout, fragment)
            transaction?.setCustomAnimations(
                R.anim.fade_out,  // Enter animation
                R.anim.fade_in  // Exit animation
            )
            transaction?.commit()
        }


        //This button will only shown after the verification process is successful
        binding.btnComplete.setOnClickListener() {

            //Show Confirmation and Redirect to Receipt
            AlertDialog
                .Builder(requireContext())
                .setTitle("Transaction Confirmation")
                .setMessage("Are you sure you want to complete this transaction?")
                .setPositiveButton("Yes") { dialog, _ ->

                    ////When the User chose "Yes", do the things////

                    //Update another user view
                    meetUpIDGlobal?.let { meetUpID ->
                        updateMeetUpStatus(meetUpID, verified = true, completed = true)
                    }

                    //Update Status & Store to Database
                    if (transaction != null) {
                        updateStatus(transaction.meetUpID){ result ->
                            if(result != null && result){
                                //Redirect to Receipt
                                dialog.dismiss()
                                val bundle = Bundle().apply {
                                    putSerializable("order", transaction)
                                }
                                val fragment = Receipt().apply {
                                    arguments = bundle
                                }
                                activity?.supportFragmentManager?.beginTransaction()?.apply {
                                    replace(R.id.frameLayout, fragment)
                                    setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                                    commit()
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        return binding.root
    }


    private fun updateStatus(meetUpID: String?, onResult: (Boolean?) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Order")

        Log.e("MEET UP ID", meetUpID.toString())
        databaseReference.orderByChild("meetUpID").equalTo(meetUpID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (orderSnapshot in snapshot.children) {
                            //Update the 'status' field to "Past"
                            orderSnapshot.ref.child("status").setValue("Past")
                                .addOnSuccessListener {
                                    onResult(true)
                                }
                                .addOnFailureListener { error ->
                                    Log.e("Failed to update order status", "Failed to update order status: ${error.message}")
                                    onResult(false)
                                }
                        }
                    } else {
                        Log.e("Not Exist Meetup id", "Failed to update order status")
                        onResult(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Failed to update order status", "Error querying database: ${error.message}")
                    onResult(false)
                }
            })
    }

    //Function to fetch SwapRequest details by SwapRequestID
    private suspend fun getSwapRequestDetails(swapRequestID: String): SwapRequest? {
        return suspendCancellableCoroutine { continuation ->
            val swapRequestsRef = FirebaseDatabase.getInstance().getReference("SwapRequest")
            swapRequestsRef.child(swapRequestID).get().addOnSuccessListener { snapshot ->
                val swapRequest = snapshot.getValue(SwapRequest::class.java)
                continuation.resume(swapRequest)
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }

    //Function to fetch user ID for a given product ID
    private suspend fun getUserIDForProduct(productID: String): String? {
        return suspendCancellableCoroutine { continuation ->
            val productsRef = FirebaseDatabase.getInstance().getReference("Product")
            productsRef.child(productID).get().addOnSuccessListener { snapshot ->
                val product = snapshot.getValue(Product::class.java)
                continuation.resume(product?.created_by_UserID)
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }

    private fun listenForMeetUpUpdates(meetUpID: String) {
        val database = FirebaseDatabase.getInstance()

        //Listen to a specific meet up => Meet up is separated by ID, and it listen to the branch of with the given meet up id
        val meetUpRef = database.getReference("MeetUp").child(meetUpID)

        meetUpRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val meetUp = snapshot.getValue(MeetUp::class.java)
                if (meetUp != null) {
                    //Check if `verifiedStatus` has changed
                    if (previousVerifiedStatus != null && previousVerifiedStatus == true && !meetUp.verifiedStatus) {
                        //Only show the Toast when `verifiedStatus` changes from true to false
                        resetUIForVerification()
                    }

                    //Update `previousVerifiedStatus` to the current state
                    previousVerifiedStatus = meetUp.verifiedStatus

                    if (meetUp.verifiedStatus) {
                        if (!hasShownSuccessDialog) {
                            showSuccessUI()
                        }
                    }

                    //Handle the completion status
                    if (meetUp.completeStatus) {
                        showCompletionUI()
                    }
                }
            }


            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to listen for MeetUp updates: ${error.message}")
            }
        })
    }

    private fun resetUIForVerification() {
        //Check if the fragment is still attached to the activity
        if (!isAdded || context == null) {
            Log.e("IdentityVerification", "Fragment is not attached. Skipping UI reset.")
            return
        }

        //Reset verification UI to initial state
        binding.afterSuccessful.visibility = View.GONE
        binding.identityMainContent.visibility = View.VISIBLE

        if (isAdded && context != null) {
            Toast.makeText(
                requireContext(),
                "Verification status reset. Please scan the QR code again.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.e("IdentityVerification", "Fragment not attached. Skipping Toast display.")
        }

        hasShownSuccessDialog = false
    }


    private fun showSuccessUI() {
        //Check if the fragment is still attached
        if (!isAdded || activity == null) {
            Log.e("IdentityVerification", "Fragment is not attached. Skipping UI update.")
            return
        }

        //Only show the dialog if it hasn't been shown before
        if (!hasShownSuccessDialog) {
            successDialog?.dismiss()
            successDialog = null

            //Create and show the new dialog
            val dialogView = layoutInflater.inflate(R.layout.successful_verification, null)
            successDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            //Set up the button listener
            val button = dialogView.findViewById<Button>(R.id.btnOk)
            button.setOnClickListener {
                hasShownSuccessDialog = true // Mark as shown
                successDialog?.dismiss()
                successDialog = null
            }

            //Set background transparency
            val window = successDialog?.window
            window?.setBackgroundDrawableResource(android.R.color.transparent)

            //Show the dialog
            successDialog?.show()
            binding.afterSuccessful.visibility = View.VISIBLE
            binding.identityMainContent.visibility = View.GONE
        }
    }


    //User chose to complete order
    private fun showCompletionUI() {
        //Redirect to Receipt
        val bundle = Bundle().apply {
            putSerializable("order", transfer)
        }
        val fragment = Receipt().apply {
            arguments = bundle
        }
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.frameLayout, fragment)
            setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
            commit()
        }
    }

    private fun updateMeetUpStatus(meetUpID: String, verified: Boolean, completed: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val meetUpRef = database.getReference("MeetUp").child(meetUpID)

        //Update verification and completion statuses
        meetUpRef.child("verifiedStatus").setValue(verified)
        meetUpRef.child("completeStatus").setValue(completed)
    }


    private suspend fun getSenderUserID(senderProductID: String): String? {
        return try {
            val database = FirebaseDatabase.getInstance()
            val productRef = database.getReference("Product").child(senderProductID)
            val dataSnapshot = productRef.get().await()
            dataSnapshot.child("created_by_UserID").value as? String
        } catch (e: Exception) {
            Log.e("Error", "Failed to fetch sender user ID: ${e.message}")
            null
        }
    }

    private suspend fun getReceiverUserID(receiverProductID: String): String? {
        return try {
            val database = FirebaseDatabase.getInstance()
            val productRef = database.getReference("Product").child(receiverProductID)
            val dataSnapshot = productRef.get().await()
            dataSnapshot.child("created_by_UserID").value as? String
        } catch (e: Exception) {
            Log.e("Error", "Failed to fetch receiver user ID: ${e.message}")
            null
        }
    }

    private fun generateQRCode(content: String): Bitmap? {
        try {
            val writer = QRCodeWriter()
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 1

            //Generate QR Code matrix
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 180, 180, hints)

            //Convert bitMatrix to Bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(
                                x,
                                y
                            )
                        ) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }

            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }


    private fun getMeetUpObject(meetUpID: String, onResult: (MeetUp?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val meetUpRef = database.getReference("MeetUp").child(meetUpID)

        meetUpRef.get().addOnSuccessListener { dataSnapshot ->
            val meetUp = dataSnapshot.getValue(MeetUp::class.java)
            Log.e("MeetUp Object", meetUp.toString())
            onResult(meetUp)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }


    private fun checkPermission(permission: String, reqCode: Int) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), reqCode)
        }
    }

    override fun onResume() {
        super.onResume()
        scannerView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()

        codeScanner.releaseResources()

        successDialog?.dismiss()
        successDialog = null

        //Check if a MeetUp is currently being verified and reset the verification status
        if (meetUpIDGlobal != null) {
            resetVerificationStatus(meetUpIDGlobal!!)
        }
    }

    override fun onStop() {
        super.onStop()

        successDialog?.dismiss()
        successDialog = null

        //Optionally handle resetting the status here as well, depending on your needs
        if (meetUpIDGlobal != null) {
            resetVerificationStatus(meetUpIDGlobal!!)
        }
    }

    private fun resetVerificationStatus(meetUpID: String) {
        val database = FirebaseDatabase.getInstance()
        val meetUpRef = database.getReference("MeetUp").child(meetUpID)

        meetUpRef.child("verifiedStatus").setValue(false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firebase", "Verification status reset successfully.")
            } else {
                Log.e("Firebase", "Failed to reset verification status.")
            }
        }
    }

}
