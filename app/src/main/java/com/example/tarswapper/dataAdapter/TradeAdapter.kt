package com.example.tarswapper.dataAdapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.TradeProductDetail
import com.example.tarswapper.data.Product
import com.example.tarswapper.databinding.ProductListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class TradeAdapter(
    private val productList: List<Product>,
    private val context: Context,
) :
    RecyclerView.Adapter<TradeAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ProductListBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // Access views via binding instead of findViewById
        with(holder.binding) {
            productNameTV.text = product.name

            if (product.tradeType == "Sale"){
                tradeImg.setImageResource(R.drawable.baseline_attach_money_24)
                tradeDetailTV.text = "RM " + product.price
                tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                tradeDetailTV.backgroundTintList = null
            } else {
                tradeImg.setImageResource(R.drawable.baseline_wifi_protected_setup_24)
                tradeDetailTV.text = product.swapCategory
                tradeDetailTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_wifi_protected_setup_24, 0, 0, 0)
            }

            // Load image from Firebase Storage
            getFirebaseImageUrl(product) { url ->
                if (url != null) {
                    Glide.with(productImgV.context)
                        .load(url)
                        .into(productImgV)
                } else {
                    // Handle the case where the image URL is not retrieved
                    productImgV.setImageResource(R.drawable.ai) // Set a placeholder
                }
            }

            //redirect to product detail page
            holder.binding.productCV.setOnClickListener{
                val fragment = TradeProductDetail()

                // Create a Bundle to pass data
                val bundle = Bundle()
                bundle.putString("ProductID", product.productID) // Example data

                // Set the Bundle as arguments for the fragment
                fragment.arguments = bundle

                (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                    ?.apply {
                        replace(R.id.frameLayout, fragment)
                        setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                        addToBackStack(null)
                        commit()
                    }

            }


        }
    }

    override fun getItemCount(): Int = productList.size

    // Function to retrieve the download URL from Firebase Storage
    fun getFirebaseImageUrl(product: Product, onResult: (String?) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().getReference("ProductImages/" + product.productID + "/Thumbnail")

        // List all items in the folder
        storageReference.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    // Get the first file from the list of items
                    val firstImageRef = listResult.items[0]

                    // Get the download URL for the first image
                    firstImageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            onResult(uri.toString())  // Return the URL of the first image
                        }
                        .addOnFailureListener { e ->
                            onResult(null)  // In case of failure, return null
                        }
                } else {
                    // If no images exist in the folder
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                onResult(null)  // Handle any failure when listing items
            }
    }
}