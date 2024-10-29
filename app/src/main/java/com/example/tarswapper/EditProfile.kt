package com.example.tarswapper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.User
import com.example.tarswapper.databinding.FragmentCoinShopBinding
import com.example.tarswapper.databinding.FragmentEditProfileBinding
import com.example.tarswapper.databinding.FragmentLoginBinding
import com.example.tarswapper.databinding.FragmentStartedBinding
import com.example.tarswapper.databinding.FragmentUserProfileBinding
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

class EditProfile : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var userObj: User
    private lateinit var hiddenImageURL: String
    private val IMAGE_PICK_CODE = 1000
    private val CAMERA_CAPTURE_CODE = 1002
    private var imageUri: Uri? = null

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

                //UserTitle & ProfileBackground (Pending)

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
            val userTitle = binding.spnUserTitle.id.toString()
            val profileBackground = binding.spnProfileBackground.id.toString()



            updateAccountData(
                userID,
                username,
                email,
                hiddenImageURL,
                userTitle,
                profileBackground,
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


    //Update Account Data Processing
    private fun updateAccountData(
        userID: String,
        username: String,
        email: String,
        profileImageUrl: String,
        userTitle: String,
        profileBackground: String,
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

        dbRef.child(userID).setValue(updatedUser).addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
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
        val croppedBitmap = Bitmap.createBitmap(bitmap,
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            size,
            size)

        //Save the cropped bitmap to a file
        val croppedFile = File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
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

        //Upload the image to Firebase Storage
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                //Get the download URL after upload, then replace it into the hiddenImageURL variable
                //At the same time, temporary replace the user icon to the new Image URL
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    hiddenImageURL = imageUrl
                    Glide.with(requireContext()).load(imageUrl)
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