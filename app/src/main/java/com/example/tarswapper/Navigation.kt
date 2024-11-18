package com.example.tarswapper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tarswapper.data.MeetUp
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.SwapRequest
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentNavigationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.SphericalUtil
import java.io.IOException

class Navigation : Fragment() {
    private lateinit var binding: FragmentNavigationBinding
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: LatLng? = null
    private var destinationLocation: String? = null
    private var currentPolyline: Polyline? = null
    private var currentLocationMarker: Marker? = null
    private var hasReachedDestination = false

    private var mapMode: String = "driving"
    private var swap: SwapRequest? = null


    //Check if location services are enabled
    private fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //Prompt user to enable location services
    private fun promptEnableLocationServices() {
        Toast.makeText(
            requireContext(),
            "Please enable location service before proceeding.",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)  // Opens the location settings
    }

    //Check if both location services and permission are enabled, else request
    private fun checkLocationAndPermission() {
        if (!isLocationServicesEnabled(requireContext())) {
            //If location services are not enabled, prompt the user to enable them
            promptEnableLocationServices()
        } else {
            //Location services and permission are both enabled, proceed
            startLocationUpdates()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNavigationBinding.inflate(layoutInflater, container, false)

        //Hide Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.GONE


        //Go to Profile Page
        binding.backProfile.setOnClickListener() {
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
            transaction?.addToBackStack(null)
            transaction?.commit()
        }


        //Bottom Sheet
        BottomSheetBehavior.from(binding.standardBottomSheet).apply {
            peekHeight = 350
            this.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        //Processing
        checkLocationAndPermission()

        //Get Current User ID
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        //Get Value from Bundle
        val transaction = arguments?.getSerializable("transaction") as? SwapRequest
        transaction?.let {
            //Store swap
            swap = transaction

            //Transaction ID
            binding.fillTransactionID.text = "Transaction ID: ${transaction.swapRequestID}"

            transaction.meetUpID?.let { it1 ->
                getMeetUpObject(it1){ meetUp ->
                    if(meetUp != null){
                        //Location
                        binding.fillDestination.text = meetUp!!.location.toString()
                        destinationLocation = meetUp!!.location.toString()

                        //Scheduled Date & Time
                        //Original Date & Time
                        val originalDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                        val dateToFormat = "${meetUp.date} ${meetUp.time}"

                        //Parse the date and time
                        val date: Date? = try {
                            originalDateFormat.parse(dateToFormat)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }

                        //Desired output format
                        val desiredDateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        val dateTime = date?.let { desiredDateFormat.format(it) } ?: ""

                        //Set the formatted date and time to the TextView
                        binding.fillDateTime.text = dateTime
                    }
                }
            }

            //Recipient
            getSenderProduct(transaction.senderProductID!!){ product ->
                if(product != null){
                    getSenderUser(product.created_by_UserID!!) { user ->
                        if(user != null){
                            if (userID == user.userID) {
                                //Item to Send
                                binding.fillItemToSend.text = product.name
                            } else {
                                binding.fillName.text = user.name

                                //Item to Receive
                                binding.fillItemToReceive.text = product.name
                            }
                        }
                    }

                    //Trade Type
                    if(product.tradeType != null){
                        binding.fillTradeType.text = product.tradeType
                    }
                }
            }

            getReceiverProduct(transaction.receiverProductID!!){ product ->
                if(product != null){
                    getReceiverUser(product.created_by_UserID!!) { user ->
                        if(user != null){
                            if (userID == user.userID) {
                                //Item to Send
                                binding.fillItemToSend.text = product.name
                            } else {
                                binding.fillName.text = user.name

                                //Item to Receive
                                binding.fillItemToReceive.text = product.name
                            }
                        }
                    }

                    //Item to Receive
                    binding.fillItemToReceive.text = product.name
                }
            }
        }


        //Working with Maps (Initialization)
        //Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())


        mapView = binding.mapViewElement
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map
            onMapReady(map)

            startLocationUpdates()
        }


        //Set Driving as Default
        binding.toggleButton.check(R.id.btnDrive)

        //Control walking or driving
        binding.toggleButton.addOnButtonCheckedListener { group, checkedId, isChecked ->
            when (checkedId) {
                R.id.btnWalk -> {
                    if (isChecked) {
                        //"Walking" button is selected
                        mapMode = "walking"

                        startLocationUpdates()
                    }
                }

                R.id.btnDrive -> {
                    if (isChecked) {
                        //"Driving" button is selected
                        mapMode = "driving"

                        startLocationUpdates()
                    }
                }
            }
        }


        return binding.root
    }

    private fun getSenderUser(createdByUserid: String, onResult: (User?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User").child(createdByUserid)

        userRef.get().addOnSuccessListener { dataSnapshot ->
            val user = dataSnapshot.getValue(User::class.java)
            onResult(user)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }

    private fun getReceiverUser(createdByUserid: String, onResult: (User?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("User").child(createdByUserid)

        userRef.get().addOnSuccessListener { dataSnapshot ->
            val user = dataSnapshot.getValue(User::class.java)
            onResult(user)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }

    private fun getReceiverProduct(receiverProductID: String, onResult: (Product?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val productRef = database.getReference("Product").child(receiverProductID)

        productRef.get().addOnSuccessListener { dataSnapshot ->
            val product = dataSnapshot.getValue(Product::class.java)
            onResult(product)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }

    private fun getSenderProduct(senderProductID: String, onResult: (Product?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val productRef = database.getReference("Product").child(senderProductID)

        productRef.get().addOnSuccessListener { dataSnapshot ->
            val product = dataSnapshot.getValue(Product::class.java)
            onResult(product)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }

    private fun getMeetUpObject(meetUpID: String, onResult: (MeetUp?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val meetUpRef = database.getReference("MeetUp").child(meetUpID)

        meetUpRef.get().addOnSuccessListener { dataSnapshot ->
            val meetUp = dataSnapshot.getValue(MeetUp::class.java)
            onResult(meetUp)
        }.addOnFailureListener { e ->
            println("Error fetching MeetUp: ${e.message}")
            onResult(null)
        }
    }

    private fun startLocationUpdates() {

        if (!this::googleMap.isInitialized) {
            Log.e("Navigation", "googleMap is not yet initialized. Skipping startLocationUpdates.")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            //Define the LocationRequest
            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000) //5 seconds update interval
                .setMaxUpdateDelayMillis(10000)   //Max 10 seconds delay
                .build()

            //Define the LocationCallback
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult?.let {
                        if (isAdded) {
                            for (location in it.locations) {
                                currentLocation = LatLng(location.latitude, location.longitude)
                                Log.d("Navigation", "Current Location: $currentLocation")

                                //Check if currentLocationMarker exists
                                if (currentLocationMarker == null) {
                                    //If it doesn't exist, create it
                                    currentLocationMarker = googleMap.addMarker(
                                        MarkerOptions().position(currentLocation!!)
                                            .title("Current Location")
                                    )
                                } else {
                                    //If it exists, just update its position
                                    currentLocationMarker?.position = currentLocation!!
                                }

                                updateRoute(currentLocation!!)  //Use the updated current location
                            }
                        }
                    }
                }
            }

            //Start receiving location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Log.e("Navigation", "Location permission is not granted.")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun getLatLngFromAddress(context: Context, addressString: String): LatLng? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addressList: MutableList<Address>? = geocoder.getFromLocationName(addressString, 1)
            if (addressList!!.isNotEmpty()) {
                val address: Address = addressList[0]
                LatLng(address.latitude, address.longitude)
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e("GeocoderError", "Geocoding failed: ${e.message}")
            null
        }
    }

    private fun getBitmapFromVectorDrawable(drawable: VectorDrawable): Bitmap {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        //Request current location and update the map (initial camera setup)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                //Move camera to the current location
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                //Add a marker for the current location (only once)
                if (currentLocationMarker == null) {
                    val vectorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.round_person_pin_circle_24) as VectorDrawable
                    val bitmap = getBitmapFromVectorDrawable(vectorDrawable)
                    currentLocationMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Current Location")
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .snippet("You are here")
                    )
                }

                //Update the route using the current location
                updateRoute(currentLatLng)
            } else {
                //If no current location is found, use the default location
                val defaultLocation = LatLng(3.2158649, 101.7304889)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

                //Add a marker for the default location
                if (currentLocationMarker == null) {
                    currentLocationMarker = googleMap.addMarker(
                        MarkerOptions().position(defaultLocation).title("Default Location")
                    )
                }

                //Update the route based on the default location
                updateRoute(defaultLocation)
            }
        }

        //Obtain Destination Marker
        var latLng: LatLng? = null
        if (destinationLocation != null) {
            latLng = getLatLngFromAddress(requireContext(), destinationLocation!!)
        }

        if (latLng != null) {
            googleMap.addMarker(MarkerOptions().position(latLng).title("Destination"))
        } else {
            Log.e("LocationError", "Address not found!")
        }
    }


    private fun updateRoute(origin: LatLng) {
        var latLng: LatLng? = null
        if (destinationLocation != null) {
            latLng = getLatLngFromAddress(requireContext(), destinationLocation!!)
        }

        val destination = latLng

        if (destination != null) {
            val requestQueue: RequestQueue = Volley.newRequestQueue(requireContext())
            val url: String = Uri.parse("https://maps.googleapis.com/maps/api/directions/json")
                .buildUpon()
                .appendQueryParameter("origin", "${origin.latitude},${origin.longitude}")
                .appendQueryParameter(
                    "destination",
                    "${destination.latitude},${destination.longitude}"
                )
                .appendQueryParameter("mode", mapMode)
                .appendQueryParameter("key", "AIzaSyBma5DDvejfQXrM8VWrZtf-EmQUjgzp9eM")
                .toString()

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                { response ->
                    val route = parseDirectionsResponse(response.toString())

                    //Clear the previous polyline if it exists
                    currentPolyline?.remove()

                    //Draw the new route and save a reference to it
                    currentPolyline = drawRoute(route)


                    ////Check Distance every 5 seconds////
                    //Check the distance between current location and destination
                    val distance = SphericalUtil.computeDistanceBetween(origin, destination)
                    Log.d("Navigation", "Distance to destination: $distance meters")

                    //Check if the distance is less than a threshold (like 50 meters)
                    if (distance < 50 && !hasReachedDestination) {
                        Log.d("Navigation", "You are close to the destination!")
                        Toast.makeText(
                            requireContext(),
                            "You reached the destination!",
                            Toast.LENGTH_SHORT
                        ).show()


                        hasReachedDestination = true


                        //If the threshold is reached, redirect to identity verification page
                        val bundle = Bundle().apply {
                            if (swap != null) {
                                putSerializable("swap", swap)
                                Log.e("Navigation", "Swap object is not null")
                            } else {
                                Log.e("Navigation", "Swap object is null")
                            }
                        }
                        val fragment = IdentityVerification().apply {
                            arguments = bundle
                        }
                        activity?.supportFragmentManager?.beginTransaction()?.apply {
                            replace(R.id.frameLayout, fragment)
                            setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                            commit()
                        }
                    }
                },
                { error ->
                    Log.e("GoogleDirections", "Request failed: ${error.message}")
                }
            )
            requestQueue.add(jsonObjectRequest)
        }
    }

    private fun parseDirectionsResponse(response: String?): List<LatLng> {
        val points = mutableListOf<LatLng>()
        response?.let {
            val jsonObject = JSONObject(it)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                if (legs.length() > 0) {
                    val steps = legs.getJSONObject(0).getJSONArray("steps")
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val polyline = step.getJSONObject("polyline").getString("points")
                        val path = decodePoly(polyline)
                        points.addAll(path)
                    }
                }
            }
        }
        return points
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    private fun drawRoute(route: List<LatLng>): Polyline? {
        if (this::googleMap.isInitialized) {
            val polyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(route)
                    .width(12f)
                    .color(android.graphics.Color.RED)
            )

            val boundsBuilder = LatLngBounds.builder()
            route.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            val padding = 100
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.animateCamera(cameraUpdate)

            return polyline
        }
        return null
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()

        //Only start location updates if googleMap is already initialized
        if (this::googleMap.isInitialized && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()

        if (::locationCallback.isInitialized) {
            stopLocationUpdates()
        }
        mapView.onPause()
    }

}