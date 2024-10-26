package com.example.tarswapper

import android.Manifest
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
                Toast.makeText(requireContext(), it.text, Toast.LENGTH_LONG).show()
                scannerView.visibility = View.GONE // Hide scanner after scanning
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



        //Back to the Navigation Page
        binding.btnBackNavigation.setOnClickListener() {
            val fragment = Navigation()

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




        return binding.root
    }

    private fun checkPermission(permission: String, reqCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), reqCode)
        }
    }

    override fun onResume() {
        super.onResume()
        scannerView.visibility = View.GONE // Hide it initially
    }

    override fun onPause() {
        codeScanner.releaseResources() // Release resources on pause
        super.onPause()
    }
}
