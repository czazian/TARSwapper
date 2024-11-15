package com.example.tarswapper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.intl.Locale
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tarswapper.data.ChatRoom
import com.example.tarswapper.data.Product
import com.example.tarswapper.data.User
import com.example.tarswapper.dataAdapter.ChatSelectionAdapter
import com.example.tarswapper.dataAdapter.ProductImageListAdapter
import com.example.tarswapper.databinding.FragmentAddProductBinding
import com.example.tarswapper.interfaces.OnUserContactClick
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

class TradeAddProduct : Fragment() {
    //fragment name
    private lateinit var binding: FragmentAddProductBinding
    private lateinit var userObj: User

    private val selectedThumbnailImages: MutableList<Uri> = mutableListOf()  // List to store thumbnail images
    private val selectedImages: MutableList<Uri>          = mutableListOf()  // List to store selected images
    private lateinit var multi_image_adapter: ProductImageListAdapter
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAddProductBinding.inflate(layoutInflater, container, false)

        // Set up RecyclerView with GridLayoutManager
        val layoutManager = GridLayoutManager(requireContext(), 4) // 4 columns in the grid
        binding.multiImagesRecyclerView.layoutManager = layoutManager
        multi_image_adapter = ProductImageListAdapter(selectedImages)
        binding.multiImagesRecyclerView.adapter = multi_image_adapter

        //Show Bottom Navigation Bar
        val bottomNavigation =
            (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigation.visibility = View.VISIBLE

        ////User IDs////
        //Get User ID - From SharedPreference
        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        getUserRecord(userID.toString()) {
            if (it != null) {
                userObj = it
                //Meaning to say the user has record, and store as "it"


            }
        }

        //set spinner value
        val productCategoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.product_category))
        productCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.productCategorySpinner.adapter = productCategoryAdapter

        val productConditionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.product_condition))
        productConditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.productConditionSpinner.adapter = productConditionAdapter

        val productTypeTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.trade_type))
        productTypeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.productTradeTypeSpinner.adapter = productTypeTypeAdapter

        // Set up an item selected listener for the Spinner
        binding.productTradeTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Check if the selected item is "Swap"
                val selectedItem = resources.getStringArray(R.array.trade_type)[position]
                if (selectedItem == "Sale") {
                    // Make the ConstraintLayout visible
                    binding.saleDetailContainer.visibility = View.VISIBLE
                    binding.swapDetailContainer.visibility = View.GONE
                } else if (selectedItem == "Swap") {
                    // Hide the ConstraintLayout for other selections
                    binding.saleDetailContainer.visibility = View.GONE
                    binding.swapDetailContainer.visibility = View.VISIBLE

                    val productSwapTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.product_category))
                    productSwapTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.productSwapCategorySpinner.adapter = productSwapTypeAdapter
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        //onclick and submit
        binding.btnBackAddProduct.setOnClickListener(){
            val fragment = TradeMyShop()

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

        //select image from phone gallery for thumbnail
        binding.addThumbnailImgBtn.setOnClickListener(){
            // Open the gallery to pick an image
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Set up the  to open gallery for selecting multiple images
        binding.addImgBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)  // Allow multiple image selection
            startActivityForResult(intent, PICK_MULTIPLE_IMAGE_REQUEST)
        }

        binding.submitBtn.setOnClickListener(){

            var isValid = true
            // Check if name field is empty
            if (binding.nameED.text.isNullOrBlank()) {
                binding.nameED.error = "Product Name is required"
                isValid = false
            }

            if (binding.descriptionED.text.isNullOrBlank()) {
                binding.descriptionED.error = "Product Description is required"
                isValid = false
            }

            if (isValid) {
                //push data into firebase
                var newProduct = Product(
                    name = binding.nameED.text.toString(),
                    description = binding.descriptionED.text.toString(),
                    category = binding.productCategorySpinner.selectedItem.toString(),
                    condition = binding.productConditionSpinner.selectedItem.toString(),
                    tradeType = binding.productTradeTypeSpinner.selectedItem.toString(),
                    //status is available by default
                    status = resources.getStringArray(R.array.product_status)[0],
                    created_at = LocalDateTime.now().toString(),
                    created_by_UserID = userObj.userID,
                )

                if (newProduct.tradeType == "Sale") {
                    newProduct.price = binding.priceED.text.toString()
                } else if (newProduct.tradeType == "Swap") {
                    newProduct.swapCategory = binding.productSwapCategorySpinner.selectedItem.toString()
                    newProduct.swapRemark = binding.swapRemarkED.text.toString()
                }

                addProductToFirebase(newProduct)
                Toast.makeText(context, "Product created successfully", Toast.LENGTH_SHORT).show()

                //back to previous UI
                val fragment = TradeMyShop()
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
            } else {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }

        }

        return binding.root
    }

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

    fun addProductToFirebase(product: Product) {
        // Get a reference to the Firebase database
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("Product")

        // Generate a unique key for the new product
        val newProductRef = productsRef.push()
        // Set the autogenerated product ID
        product.productID = newProductRef.key

        // Push data to Firebase
        newProductRef.setValue(product)
            .addOnSuccessListener {
                // Data successfully written
                println("Product added successfully")
            }
            .addOnFailureListener { e ->
                // Handle failure
                println("Failed to add product: ${e.message}")
            }

        //push product image to Storage
        uploadProductImages(product)
    }

    fun uploadProductImages(product: Product) {
        // Get a reference to Firebase Storage
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReference("ProductImages")
        val thumbnailImageFolder = storageRef.child(product.productID + "/Thumbnail")  // Use productID to organize images in folders
        val imagesFolder         = storageRef.child(product.productID + "/Images")     // Use productID to organize images in folders

        selectedThumbnailImages.forEach { imageUri ->
            val imageRef = thumbnailImageFolder.child(UUID.randomUUID().toString())  // Use a unique name for each image

            // Upload the image
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    // Get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Update the product with the image URL
                        //updateProductWithImageUrl(product.productID, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    // Handle image upload failure
                    println("Image upload failed: ${e.message}")
                }
        }

        // Iterate over selected images and upload each one
        selectedImages.forEach { imageUri ->
            val imageRef = imagesFolder.child(UUID.randomUUID().toString())  // Use a unique name for each image

            // Upload the image
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    // Get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Update the product with the image URL
                        //updateProductWithImageUrl(product.productID, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    // Handle image upload failure
                    println("Image upload failed: ${e.message}")
                }
        }
    }

    //not nessasary
    fun updateProductWithImageUrl(product: Product, imageUrl: String) {
        // Get a reference to the product in the Firebase database
        val productRef = FirebaseDatabase.getInstance().getReference("Product").child(product.productID.toString())

        // Update the product with the image URL (assuming there's an "images" field in the product model)
        productRef.child("images").push().setValue(imageUrl)
            .addOnSuccessListener {
                println("Image URL added to product successfully")
            }
            .addOnFailureListener { e ->
                println("Failed to update product with image URL: ${e.message}")
            }
    }

    // Handle the result after image selection
// Inside onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                // Single image selection
                PICK_IMAGE_REQUEST -> {
                    val selectedImageUri = data?.data
                    binding.addThumbnailImgBtn.setImageURI(selectedImageUri)
                    if (selectedImageUri != null) {
                        selectedThumbnailImages.add(selectedImageUri)
                    }
                }
                // Multiple image selection
                PICK_MULTIPLE_IMAGE_REQUEST -> {
                    data?.clipData?.let { clipData ->
                        // If multiple images are selected
                        for (i in 0 until clipData.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            selectedImages.add(imageUri)
                        }
                    } ?: run {
                        // If only one image is selected (not in a ClipData)
                        data?.data?.let { imageUri ->
                            selectedImages.add(imageUri)
                        }
                    }

                    // Notify adapter that the data has changed
                    multi_image_adapter.notifyDataSetChanged()
                }
            }
        }
    }


    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PICK_MULTIPLE_IMAGE_REQUEST = 2
    }




}
