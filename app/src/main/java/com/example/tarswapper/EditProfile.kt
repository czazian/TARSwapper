package com.example.tarswapper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.tarswapper.data.Items
import com.example.tarswapper.data.PurchasedItem
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentEditProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//For Spinners (Store two or more values into one selection)
data class SpinnerItem(val itemID: Int, val itemName: String) {
    override fun toString(): String {
        return itemName
    }
}

class EditProfile : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var userObj: User
    private lateinit var hiddenImageURL: String
    private val IMAGE_PICK_CODE = 1000
    private val CAMERA_CAPTURE_CODE = 1002
    private var imageUri: Uri? = null

    //Spinner Selection (-1 means not yet assigned)
    var selectedUserTitleId: Int = -1
    var selectedProfileBackgroundId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEditProfileBinding.inflate(layoutInflater, container, false)


        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE


        //Disable email edit text
        binding.txtEditEmail.isEnabled = false;


        //Go to User Profile Page
        binding.btnBackProfile.setOnClickListener() {
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

        //When user spinner select change
        binding.spnUserTitle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view == null) return

                val selectedItem = parent.getItemAtPosition(position) as SpinnerItem
                selectedUserTitleId = selectedItem.itemID
                Log.e("Selected User Title ID = ", selectedUserTitleId.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
        binding.spnProfileBackground.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (view == null) return

                    val selectedItem = parent.getItemAtPosition(position) as SpinnerItem
                    selectedProfileBackgroundId = selectedItem.itemID
                    Log.e(
                        "Selected Profile Background ID = ",
                        selectedProfileBackgroundId.toString()
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }

        //Get User ID first leh
        val userID: String = getUserID()

        //Get User Record as returned by obj
        getUserRecord(userID) {
            if (it != null) {
                userObj = it
                hiddenImageURL = it.profileImage.toString()
                //Meaning to say the user has record, and store as "it"
                //Display user data
                binding.txtEditUsername.setText(it.name.toString())
                binding.txtEditEmail.setText(it.email.toString())
                Glide.with(requireContext()).load(it.profileImage) // User Icon URL string
                    .into(binding.userLoggedIcon)

                //Initially get all the UserTitle & ProfileBackground that the logged-in user owned
                getOwnedUserTitleAndProfileBackground(userID)
            }
        }

        binding.userLoggedIcon.setOnClickListener() {
            openImageChooser()
        }
        binding.updateImgIcon.setOnClickListener() {
            openImageChooser()
        }

        //Processing
        binding.btnSaveEdit.setOnClickListener() {
            val username = binding.txtEditUsername.text.toString()
            val email = binding.txtEditEmail.text.toString()

            updateAccountData(
                userID,
                username,
                email,
                hiddenImageURL,
            ) {
                if (it) {
                    //Show Toast
                    Toast.makeText(
                        requireContext(), "Update Successfully!", Toast.LENGTH_LONG
                    ).show()

                    //After Update Successful, Back to User Profile
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
            }
        }




        return binding.root
    }

    //Function to update spinners when no items are available
    private fun updateSpinnersForNoItems() {
        //Update the adapter of the spinner to show "No items available" message
        getAllUserTitle(emptyList(), null)
        getAllProfileBackground(emptyList(), null)
    }


    //Nested Firebase Process
    private fun getOwnedUserTitleAndProfileBackground(userID: String) {
        val database = FirebaseDatabase.getInstance()
        val purchasedItemRef = database.getReference("PurchasedItem")
        val itemsRef = database.getReference("Items")

        purchasedItemRef.orderByChild("userID").equalTo(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val itemIDs = mutableListOf<Int>()
                    var equippedTitleId: Int? = null
                    var equippedBackgroundId: Int? = null
                    var defaultTitleId: Int? = null
                    var defaultBackgroundId: Int? = null


                    if (dataSnapshot.childrenCount == 0L) {
                        //Handle case where no purchased items are found
                        Log.i("getOwnedUserTitle", "No purchased items found for userID: $userID")
                        //Update spinners to show "No items available" message
                        updateSpinnersForNoItems()
                        return
                    }


                    for (itemSnapshot in dataSnapshot.children) {
                        val purchasedItem = itemSnapshot.getValue(PurchasedItem::class.java)
                        if (purchasedItem != null) {
                            purchasedItem.itemID?.let { itemID ->
                                itemIDs.add(itemID)
                                //Check if the item is equipped
                                if (purchasedItem.isEquipped == true) {
                                    itemsRef.child(itemID.toString())
                                        .addListenerForSingleValueEvent(object :
                                            ValueEventListener {
                                            override fun onDataChange(itemSnapshot: DataSnapshot) {
                                                when (itemSnapshot.child("itemType")
                                                    .getValue(String::class.java)) {
                                                    "title" -> {
                                                        equippedTitleId = itemID
                                                        selectedUserTitleId = itemID
                                                    }

                                                    "background" -> {
                                                        equippedBackgroundId = itemID
                                                        selectedProfileBackgroundId = itemID
                                                    }
                                                }
                                                //Load items into the spinners once the types are identified
                                                getAllUserTitle(itemIDs, equippedTitleId)
                                                getAllProfileBackground(
                                                    itemIDs,
                                                    equippedBackgroundId
                                                )
                                            }

                                            override fun onCancelled(databaseError: DatabaseError) {
                                                Log.e(
                                                    "getOwnedProfile Error",
                                                    "Error: ${databaseError.message}"
                                                )
                                            }
                                        })
                                } else {
                                    //Store default item if they are not equipped (default item is the first un-equipped item)
                                    itemsRef.child(itemID.toString())
                                        .addListenerForSingleValueEvent(object :
                                            ValueEventListener {
                                            override fun onDataChange(itemSnapshot: DataSnapshot) {
                                                when (itemSnapshot.child("itemType")
                                                    .getValue(String::class.java)) {
                                                    "title" -> {
                                                        if (defaultTitleId == null) {
                                                            //Set first un-equipped title as default
                                                            defaultTitleId = itemID
                                                        }
                                                    }

                                                    "background" -> {
                                                        if (defaultBackgroundId == null) {
                                                            //Set first un-equipped background as default
                                                            defaultBackgroundId = itemID
                                                        }
                                                    }
                                                }

                                                //If none are equipped, set the default items
                                                if (equippedTitleId == null && defaultTitleId != null) {
                                                    equippedTitleId = defaultTitleId
                                                    selectedUserTitleId = defaultTitleId as Int

                                                }
                                                if (equippedBackgroundId == null && defaultBackgroundId != null) {
                                                    equippedBackgroundId = defaultBackgroundId
                                                    selectedProfileBackgroundId =
                                                        defaultBackgroundId as Int
                                                }

                                                //Load items into the spinners
                                                getAllUserTitle(itemIDs, equippedTitleId)
                                                getAllProfileBackground(
                                                    itemIDs,
                                                    equippedBackgroundId
                                                )
                                            }

                                            override fun onCancelled(databaseError: DatabaseError) {
                                                Log.e(
                                                    "getOwnedProfile Error",
                                                    "Error: ${databaseError.message}"
                                                )
                                            }
                                        })
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("getOwnedProfileBackground Error", "Error: ${databaseError.message}")
                }
            })
    }


    //Updated function for setting User Titles in Spinner with default selection
    private fun getAllUserTitle(itemIDs: List<Int>, equippedTitleId: Int?) {
        val database = FirebaseDatabase.getInstance()
        val itemsRef = database.getReference("Items")

        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userTitleList = mutableListOf<SpinnerItem>()

                for (itemSnapshot in dataSnapshot.children) {
                    val item = itemSnapshot.getValue(Items::class.java)
                    if (item != null && itemIDs.contains(item.itemID) && item.itemType == "title") {
                        userTitleList.add(SpinnerItem(item.itemID!!, item.itemName!!))
                    }
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    userTitleList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spnUserTitle.adapter = adapter

                //Set equipped item as selected by default, if available
                equippedTitleId?.let {

                    val position =
                        userTitleList.indexOfFirst { spinnerItem -> spinnerItem.itemID == it }
                    if (position >= 0) {
                        binding.spnUserTitle.setSelection(position)
                    }

                }

                //Show message if no items available
                if (userTitleList.isEmpty()) {
                    adapter.clear()
                    adapter.add(SpinnerItem(0, "No user titles available..."))
                    binding.spnUserTitle.isEnabled = false
                } else {
                    binding.spnUserTitle.isEnabled = true
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("getAllUserTitle Error", "Error: ${databaseError.message}")
            }
        })
    }

    private fun getAllProfileBackground(itemIDs: List<Int>, equippedBackgroundId: Int?) {
        val database = FirebaseDatabase.getInstance()
        val itemsRef = database.getReference("Items")

        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val profileBackgroundList = mutableListOf<SpinnerItem>()

                for (itemSnapshot in dataSnapshot.children) {
                    val item = itemSnapshot.getValue(Items::class.java)
                    if (item != null && itemIDs.contains(item.itemID) && item.itemType == "background") {
                        profileBackgroundList.add(SpinnerItem(item.itemID!!, item.itemName!!))

                    }
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    profileBackgroundList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spnProfileBackground.adapter = adapter

                //Set equipped item as selected by default, if available
                equippedBackgroundId?.let {

                    val position =
                        profileBackgroundList.indexOfFirst { spinnerItem -> spinnerItem.itemID == it }
                    if (position >= 0) {
                        binding.spnProfileBackground.setSelection(position)
                    }

                }

                //Show message if no items available
                if (profileBackgroundList.isEmpty()) {
                    adapter.clear()
                    adapter.add(SpinnerItem(0, "No backgrounds available..."))
                    binding.spnProfileBackground.isEnabled = false
                } else {
                    binding.spnProfileBackground.isEnabled = true
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("getAllProfileBackground Error", "Error: ${databaseError.message}")
            }
        })
    }


    //Update Account Data Processing
    private fun updateAccountData(
        userID: String,
        username: String,
        email: String,
        profileImageUrl: String,
        onResult: (Boolean) -> Unit
    ) {
        val dbRef = FirebaseDatabase.getInstance().getReference("User")

        val updatedUser = User(
            userID,
            username,
            email,
            userObj.password,
            profileImageUrl,
            userObj.joinedDate,
            userObj.coinAmount,
            true,
            userObj.gameChance,
            userObj.lastPlayDate
        )

        updateUserTitleAndProfileBackground(userID)

        dbRef.child(userID).setValue(updatedUser).addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }


    private fun updateUserTitleAndProfileBackground(userID: String) {
        val database = FirebaseDatabase.getInstance()
        val purchasedItemRef = database.getReference("PurchasedItem")

        //Update the title and background items based on selected IDs
        purchasedItemRef.orderByChild("userID").equalTo(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //Prepare to collect update operations
                    val updates = mutableMapOf<DatabaseReference, Map<String, Any>>()

                    for (itemSnapshot in dataSnapshot.children) {
                        val purchasedItem = itemSnapshot.getValue(PurchasedItem::class.java)
                        if (purchasedItem != null) {
                            //Determine if the item should be equipped (selected id as true, all other as false)
                            val isEquipped = when (purchasedItem.itemID) {
                                selectedUserTitleId -> true
                                selectedProfileBackgroundId -> true
                                else -> false
                            }
                            //Prepare the update for this item
                            updates[itemSnapshot.ref] = mapOf("equipped" to isEquipped)
                        }
                    }

                    //Execute all updates
                    for ((ref, update) in updates) {
                        ref.updateChildren(update)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("updateUserTitleAndBackground Error", "Error: ${databaseError.message}")
                }
            })
    }


    //Select Image/Take Photo Processing
    private fun openImageChooser() {
        //Create a Dialog lets user to choose weather to choose photo from gallery or take a photo with camera
        val options = arrayOf("Choose from Gallery", "Take a Photo")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image")

        builder.setItems(options) { _, which ->
            when (which) {
                0 -> chooseFromGallery()   //Choose to pick a photo from gallery - Option = 0
                1 -> takePhoto()           //Choose to take a photo from camera  - Option = 1
            }
        }
        builder.show()
    }

    private fun chooseFromGallery() {
        //Open Image Chooser
        val intent = Intent(Intent.ACTION_PICK)

        //Only displays image files
        intent.type = "image/*"

        //After chosen an image
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun takePhoto() {
        val imageFile = File(requireContext().externalCacheDir, "${System.currentTimeMillis()}.jpg")
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )

        //Open camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, CAMERA_CAPTURE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            //1000 = Pick from Gallery, 1002 = Camera
            //Determine the received code, to execute the crop image processing
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    val selectedImageUri = data?.data
                    //Force the image become square
                    selectedImageUri?.let { cropImageToSquare(it) }
                }

                CAMERA_CAPTURE_CODE -> {
                    //Force the image become square
                    imageUri?.let { cropImageToSquare(it) }
                }
            }
        }
    }

    //Force crop image to square
    private fun cropImageToSquare(imageUri: Uri) {
        //Load the image from the URI, keep it as bitmap
        val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)

        //Calculate the size for cropping
        val size = Math.min(bitmap.width, bitmap.height)

        //Create a square bitmap
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            size,
            size
        )

        //Save the cropped bitmap to a file
        val croppedFile =
            File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(croppedFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        //Upload the cropped image to Firebase
        uploadImageToFirebase(Uri.fromFile(croppedFile))
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        //Get the user ID, then use the user id as the image name to be stored into the Firebase Storage as .jpg
        val userID = getUserID()
        val storageRef = FirebaseStorage.getInstance().getReference("UserProfile/$userID.jpg")

        //Show progress bar
        binding.progressBar.visibility = View.VISIBLE
        //Hide Main Content
        binding.scrollView2.visibility = View.GONE

        //Upload the image to Firebase Storage
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                //Get the download URL after upload, then replace it into the hiddenImageURL variable
                //At the same time, temporary replace the user icon to the new Image URL
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    hiddenImageURL = imageUrl

                    //Load the image with Glide and handle progress visibility
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                //Hide progress bar in case of failure
                                binding.progressBar.visibility = View.GONE
                                binding.scrollView2.visibility = View.VISIBLE
                                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                //Hide progress bar when the image is fully loaded
                                binding.progressBar.visibility = View.GONE
                                binding.scrollView2.visibility = View.VISIBLE
                                return false
                            }
                        })
                        .into(binding.userLoggedIcon)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Failed to upload image: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    //Get Today Date
    private fun getCurrentDateString(): String {
        val currentDate = Date() // Get the current date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(currentDate)
    }


    //Get User ID from SharePreference as String
    private fun getUserID(): String {
        val sharedPreferences =
            requireContext().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString(
            "userID", null
        ) // Retrieve user ID, default is null if not found

        return userId.toString()
    }

    //Retrieve user record processing
    private fun getUserRecord(userID: String, onResult: (User?) -> Unit) {
        //Get a reference to the database
        val databaseRef = FirebaseDatabase.getInstance().getReference("User").child(userID)

        //Add a listener to retrieve the user data
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //Convert the snapshot to a User object
                    val user = snapshot.getValue(User::class.java)
                    onResult(user) //Return the user record
                } else {
                    onResult(null) //User not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Database error: ${error.message}")
                onResult(null) //In case of error, return null
            }
        })
    }

}