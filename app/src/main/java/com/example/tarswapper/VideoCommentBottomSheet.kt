import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tarswapper.R
import com.example.tarswapper.Video
import com.example.tarswapper.data.CommunityComment
import com.example.tarswapper.data.ShortVideoComment
import com.example.tarswapper.dataAdapter.CommunityCommentAdapter
import com.example.tarswapper.dataAdapter.VideoCommentAdapter
import com.example.tarswapper.databinding.VideoCommentBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class VideoCommentBottomSheet(private val videoId : String) : BottomSheetDialogFragment() {
    private lateinit var binding: VideoCommentBottomSheetBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = VideoCommentBottomSheetBinding.inflate(inflater, container, false)

        fetchComments(videoId){ commentList ->
            binding.commentRV.layoutManager = LinearLayoutManager(requireContext())
            binding.commentRV.adapter = VideoCommentAdapter(commentList, requireContext())
            binding.commentTV.text = "${commentList.size} comment"
        }

        val sharedPreferencesTARSwapper =
            requireActivity().getSharedPreferences("TARSwapperPreferences", Context.MODE_PRIVATE)
        val userID = sharedPreferencesTARSwapper.getString("userID", null)

        binding.sentCommentBtn.setOnClickListener {
            if(!binding.commentED.text.isNullOrBlank()){
                //push data
                val comment = ShortVideoComment(
                    comment = binding.commentED.text.toString(),
                    userID = userID,
                )

                val database = FirebaseDatabase.getInstance()
                val shortVideoRef = database.getReference("ShortVideo")
                val commentRef = shortVideoRef.child(videoId.toString()).child("Comment")
                commentRef.push().setValue(comment)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Comment added successfully.")
                        binding.commentED.setText("")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Failed to add Like: ${e.message}")
                    }

            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    fun fetchComments(videoID: String, onComplete: (List<com.example.tarswapper.data.ShortVideoComment>) -> Unit) {
        // Reference to the Community's Comment node
        val databaseRef = FirebaseDatabase.getInstance().getReference("ShortVideo/$videoID/Comment")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val commentList = mutableListOf<ShortVideoComment>()

                if(isAdded){
                    if (snapshot.exists()) {
                        // Iterate through all children (comments)
                        for (commentSnapshot in snapshot.children) {
                            val comment = commentSnapshot.getValue(ShortVideoComment::class.java)
                            comment?.let { commentList.add(it) }
                        }
                    } else {
                        Log.w("VideoCommentBottomSheet", "Fragment not attached!")
                    }
                }
                // Pass the list of comments to the callback
                onComplete(commentList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to fetch comments: ${error.message}")
            }
        })
    }
}
