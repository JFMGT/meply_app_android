package de.meply.meply.ui.markt

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R

class MarktFilterBottomSheet : BottomSheetDialogFragment() {

    private var onFilterApplied: ((String?) -> Unit)? = null
    private var currentFilter: String? = null

    fun setOnFilterAppliedListener(listener: (String?) -> Unit) {
        onFilterApplied = listener
    }

    fun setCurrentFilter(filter: String?) {
        currentFilter = filter
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_markt_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<TextInputEditText>(R.id.searchInput)
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val clearButton = view.findViewById<Button>(R.id.clearButton)
        val searchButton = view.findViewById<Button>(R.id.searchButton)

        // Set current filter if any
        currentFilter?.let {
            searchInput.setText(it)
        }

        // Close button
        closeButton.setOnClickListener {
            dismiss()
        }

        // Clear button - reset filter
        clearButton.setOnClickListener {
            searchInput.setText("")
            onFilterApplied?.invoke(null)
            dismiss()
        }

        // Search button
        searchButton.setOnClickListener {
            val query = searchInput.text?.toString()?.trim()
            onFilterApplied?.invoke(if (query.isNullOrEmpty()) null else query)
            dismiss()
        }

        // Handle keyboard search action
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchInput.text?.toString()?.trim()
                onFilterApplied?.invoke(if (query.isNullOrEmpty()) null else query)
                dismiss()
                true
            } else {
                false
            }
        }

        // Focus on input
        searchInput.requestFocus()
    }

    companion object {
        fun newInstance(currentFilter: String? = null): MarktFilterBottomSheet {
            return MarktFilterBottomSheet().apply {
                setCurrentFilter(currentFilter)
            }
        }
    }
}
