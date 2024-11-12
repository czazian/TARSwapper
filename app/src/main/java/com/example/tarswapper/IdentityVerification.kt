package com.example.tarswapper

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.example.tarswapper.databinding.FragmentIdentityVerificationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class IdentityVerification : Fragment() {
    private lateinit var binding: FragmentIdentityVerificationBinding
    private lateinit var codeScanner: CodeScanner
    private lateinit var scannerView: CodeScannerView

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





        //Temporary Data - Replace with the real verification code
        val verificationCode = 4406





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
            requireActivity().runOnUiThread {
                scannerView.visibility = View.GONE // Hide scanner after scanning

                //If verification code is the same
                if(it.text == verificationCode.toString()){
                    val dialogView = layoutInflater.inflate(R.layout.successful_verification, null)
                    val dialog = AlertDialog.Builder(context)
                        .setView(dialogView)
                        .create()

                    val button = dialogView.findViewById<Button>(R.id.btnOk)

                    button.setOnClickListener(){
                        dialog.dismiss()
                    }

                    val window = dialog.window
                    window?.setBackgroundDrawableResource(android.R.color.transparent)

                    dialog.show()

                    binding.afterSuccessful.visibility = View.VISIBLE
                    binding.identityMainContent.visibility = View.GONE

                } else {
                    //If verification code is not the same
                    val dialogView = layoutInflater.inflate(R.layout.unsuccessful_verification, null)
                    val dialog = AlertDialog.Builder(context)
                        .setView(dialogView)
                        .create()

                    val button = dialogView.findViewById<Button>(R.id.btnOkUnsuccess)

                    button.setOnClickListener(){
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
        codeScanner.errorCallback = ErrorCallback {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Camera initialization error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
        // Request camera permission
        checkPermission(Manifest.permission.CAMERA, 200)





        //Set up button click listener to start the scanner
        binding.btnScanQR.setOnClickListener {
            scannerView.visibility = View.VISIBLE // Show scanner when button clicked
            codeScanner.startPreview() // Start the scanning process
        }



        //Back to the Notification Page
        binding.btnBackNavigation.setOnClickListener() {
            val fragment = Notification()

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
            transaction?.commit()
        }


        //This button will only shown after the verification process is successful
        binding.btnComplete.setOnClickListener() {

            AlertDialog
                .Builder(context)
                .setTitle("Transaction Confirmation")
                .setMessage("Are you sure you want to complete this transaction?")
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()

                    val bundle = Bundle().apply {

                    }
                    val fragment = Receipt().apply {

                    }
                    activity?.supportFragmentManager?.beginTransaction()?.apply {
                        replace(R.id.frameLayout, fragment)
                        setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                        commit()
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


    private fun checkPermission(permission: String, reqCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), reqCode)
        }
    }

    override fun onResume() {
        super.onResume()
        scannerView.visibility = View.GONE
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
}
