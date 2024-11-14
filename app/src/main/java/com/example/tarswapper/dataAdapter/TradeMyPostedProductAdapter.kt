package com.example.tarswapper.dataAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.R
import com.example.tarswapper.data.Product
import com.example.tarswapper.databinding.ProductListHoriBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class TradeMyPostedProductAdapter(private val productList: List<Product>) :
    RecyclerView.Adapter<TradeMyPostedProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ProductListHoriBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductListHoriBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

            holder.binding.productMoreOperationImgBtn.setOnClickListener {
                // Toggle the visibility of the remove button
                if (holder.binding.removeBtn.visibility == View.VISIBLE) {
                    holder.binding.removeBtn.visibility = View.GONE
                } else {
                    holder.binding.removeBtn.visibility = View.VISIBLE
                }
            }

            holder.binding.removeBtn.setOnClickListener{
                //show dialog first
                MaterialAlertDialogBuilder(holder.itemView.context) // use context or activity context
                    .setTitle("Delete Product: ${product.name}")
                    .setMessage("Are you sure you want to remove this product?")
                    .setPositiveButton("Yes") { dialog, which ->
                        Toast.makeText(holder.itemView.context, "Product removed", Toast.LENGTH_SHORT).show()
                        //remove item
                        // Get a reference to the product in Firebase
                        val productRef = FirebaseDatabase.getInstance().getReference("Product").child(product.productID.toString())

                        // Remove the product from Firebase
                        productRef.removeValue()
                            .addOnSuccessListener {
                                // Successfully removed from Firebase
                                // Optionally, show a message or update the UI
                                Toast.makeText(holder.itemView.context, "Product removed successfully", Toast.LENGTH_SHORT).show()

                                // Optionally, remove the product from the list and notify the adapter
                                // You can update your productList and notify the adapter to refresh the view
                                val updatedList = productList.toMutableList()
                                updatedList.removeAt(position) // Remove item at the current position
                                notifyItemRemoved(position) // Update the RecyclerView
                            }
                            .addOnFailureListener {
                                // Handle any error that occurred while removing the product
                                Toast.makeText(holder.itemView.context, "Failed to remove product", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("No") { dialog, which ->
                        dialog.dismiss()
                        holder.binding.removeBtn.visibility = View.GONE
                    }
                    .show()

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