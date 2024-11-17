package com.example.tarswapper.dataAdapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tarswapper.databinding.MultiImagesListBinding

class ProductImageListAdapter(private val imageUris: List<Uri>) : RecyclerView.Adapter<ProductImageListAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: MultiImagesListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = MultiImagesListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = imageUris[position]
        //originally using this no problem
        //holder.binding.imageView.setImageURI(imageUri) // Set image to ImageView

        Glide.with(holder.itemView.context)
            .load(imageUri)
            .into(holder.binding.imageView)
    }

    override fun getItemCount(): Int = imageUris.size
}
