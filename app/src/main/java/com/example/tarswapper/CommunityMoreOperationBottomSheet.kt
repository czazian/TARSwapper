import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tarswapper.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CommunityMoreOperationBottomSheet(
    private val onEdit: () -> Unit,
    private val onDelete: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.community_more_operation_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editOption = view.findViewById<LinearLayout>(R.id.editOption)
        val deleteOption = view.findViewById<LinearLayout>(R.id.deleteOption)

        editOption.setOnClickListener {
            onEdit() // Execute the edit action
            dismiss() // Close the bottom sheet
        }

        deleteOption.setOnClickListener {
            onDelete() // Execute the delete action
            dismiss() // Close the bottom sheet
        }
    }
}
