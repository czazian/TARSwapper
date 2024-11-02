package com.example.tarswapper

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import com.bumptech.glide.Glide
import com.example.tarswapper.databinding.FragmentEnlargedMediaBinding

class EnlargedMedia : Fragment() {
    private lateinit var binding: FragmentEnlargedMediaBinding
    private lateinit var mediaUrl: String
    private lateinit var mediaType: String
    private lateinit var oppositeUserID: String
    private lateinit var roomID: String
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEnlargedMediaBinding.inflate(layoutInflater, container, false)

        //Get bundle values
        arguments?.let {
            oppositeUserID = it.getString("oppositeUserID") ?: ""
            roomID = it.getString("roomID") ?: ""
            mediaUrl = it.getString("mediaUrl") ?: ""
            mediaType = it.getString("mediaType") ?: ""
        }

        Log.e("Enlarged Media", "Media URL: $mediaUrl, Type: $mediaType")

        //Show Image/Video
        setupMediaDisplay()

        //When back button is clicked
        binding.btnBackChat.setOnClickListener(){
            val bundle = Bundle().apply {
                putString("oppositeUserID", oppositeUserID)
                putString("roomID", roomID)
            }

            val fragment = Chat().apply {
                arguments = bundle
            }

            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.frameLayout, fragment)
                setCustomAnimations(R.anim.fade_out, R.anim.fade_in)
                commit()
            }
        }

        return binding.root
    }

    private fun setupMediaDisplay() {
        try {
            when (mediaType.lowercase()) {
                "image" -> {
                    binding.apply {
                        enlargedImg.visibility = View.VISIBLE
                        enlargedVideo.visibility = View.GONE
                        showImage()
                    }
                }
                "video" -> {
                    binding.apply {
                        enlargedImg.visibility = View.GONE
                        enlargedVideo.visibility = View.VISIBLE
                        showVideo()
                    }
                }
                else -> {
                    Log.e("EnlargedMedia", "Unsupported media type: $mediaType")
                }
            }
        } catch (e: Exception) {
            Log.e("EnlargedMedia", "Error setting up media display", e)
        }
    }

    private fun showImage() {
        try {
            Log.d("EnlargedMedia", "Loading image from: $mediaUrl")
            Glide.with(requireContext())
                .load(mediaUrl)
                .into(binding.enlargedImg)
        } catch (e: Exception) {
            Log.e("EnlargedMedia", "Error loading image", e)
        }
    }

    private fun showVideo() {
        try {
            Log.d("EnlargedMedia", "Loading video from: $mediaUrl")
            binding.enlargedVideo.apply {
                setVideoURI(Uri.parse(mediaUrl))

                //Add media controls
                val mediaController = MediaController(context)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)

                //Handle video loading and errors
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.setOnVideoSizeChangedListener { mp, width, height ->
                        mediaController.show()
                    }
                    start()
                }

                setOnErrorListener { mediaPlayer, what, extra ->
                    Log.e("EnlargedMedia", "Video error: what=$what extra=$extra")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("EnlargedMedia", "Error loading video", e)
        }
    }
}