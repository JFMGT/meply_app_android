package de.meply.meply.ui.events

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R

class EventFilterBottomSheet : BottomSheetDialogFragment() {

    private var currentZip: String? = null
    private var currentRadius: Int? = null
    private var onFilterApplied: ((zip: String, radius: Double) -> Unit)? = null
    private var onFilterReset: (() -> Unit)? = null

    companion object {
        private const val ARG_ZIP = "zip"
        private const val ARG_RADIUS = "radius"

        fun newInstance(zip: String?, radius: Int?): EventFilterBottomSheet {
            return EventFilterBottomSheet().apply {
                arguments = Bundle().apply {
                    zip?.let { putString(ARG_ZIP, it) }
                    radius?.let { putInt(ARG_RADIUS, it) }
                }
            }
        }
    }

    fun setOnFilterAppliedListener(listener: (zip: String, radius: Double) -> Unit) {
        onFilterApplied = listener
    }

    fun setOnFilterResetListener(listener: () -> Unit) {
        onFilterReset = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentZip = it.getString(ARG_ZIP)
            currentRadius = if (it.containsKey(ARG_RADIUS)) it.getInt(ARG_RADIUS) else null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_event_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val editZip = view.findViewById<TextInputEditText>(R.id.editZip)
        val editRadius = view.findViewById<TextInputEditText>(R.id.editRadius)
        val btnSearch = view.findViewById<Button>(R.id.btnSearch)
        val btnReset = view.findViewById<TextView>(R.id.btnReset)

        // Pre-fill current values
        currentZip?.let { editZip.setText(it) }
        currentRadius?.let { editRadius.setText(it.toString()) }

        // Close button
        closeButton.setOnClickListener {
            hideKeyboard()
            dismiss()
        }

        // Search button
        btnSearch.setOnClickListener {
            val zip = editZip.text.toString().trim()
            val radiusStr = editRadius.text.toString().trim()

            if (zip.isEmpty() || zip.length != 5) {
                Toast.makeText(requireContext(), "Bitte gib eine 5-stellige PLZ ein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val radius = radiusStr.toDoubleOrNull()
            if (radius == null || radius <= 0) {
                Toast.makeText(requireContext(), "Bitte gib einen gueltigen Radius ein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()
            onFilterApplied?.invoke(zip, radius)
            dismiss()
        }

        // Reset button
        btnReset.setOnClickListener {
            hideKeyboard()
            onFilterReset?.invoke()
            dismiss()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
