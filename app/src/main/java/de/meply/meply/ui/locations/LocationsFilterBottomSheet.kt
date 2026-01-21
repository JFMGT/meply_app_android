package de.meply.meply.ui.locations

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R

class LocationsFilterBottomSheet : BottomSheetDialogFragment() {

    data class FilterValues(
        val plz: String?,
        val radius: Int,
        val type: String?
    )

    private var currentValues: FilterValues? = null
    private var onFilterApplied: ((FilterValues) -> Unit)? = null
    private var onFilterReset: (() -> Unit)? = null

    companion object {
        private const val ARG_PLZ = "plz"
        private const val ARG_RADIUS = "radius"
        private const val ARG_TYPE = "type"

        private val RADIUS_OPTIONS = listOf(
            "10 km" to 10,
            "25 km" to 25,
            "50 km" to 50,
            "100 km" to 100,
            "200 km" to 200
        )

        fun newInstance(plz: String?, radius: Int, type: String?): LocationsFilterBottomSheet {
            return LocationsFilterBottomSheet().apply {
                arguments = Bundle().apply {
                    plz?.let { putString(ARG_PLZ, it) }
                    putInt(ARG_RADIUS, radius)
                    type?.let { putString(ARG_TYPE, it) }
                }
            }
        }
    }

    fun setOnFilterAppliedListener(listener: (FilterValues) -> Unit) {
        onFilterApplied = listener
    }

    fun setOnFilterResetListener(listener: () -> Unit) {
        onFilterReset = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentValues = FilterValues(
                plz = it.getString(ARG_PLZ),
                radius = it.getInt(ARG_RADIUS, 50),
                type = it.getString(ARG_TYPE)
            )
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
        return inflater.inflate(R.layout.bottom_sheet_locations_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val plzInput = view.findViewById<TextInputEditText>(R.id.plzInput)
        val radiusSpinner = view.findViewById<AutoCompleteTextView>(R.id.radiusSpinner)
        val typeChipGroup = view.findViewById<ChipGroup>(R.id.typeChipGroup)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val btnReset = view.findViewById<TextView>(R.id.btnReset)

        // Setup radius spinner
        val radiusLabels = RADIUS_OPTIONS.map { it.first }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, radiusLabels)
        radiusSpinner.setAdapter(adapter)

        // Pre-fill current values
        currentValues?.let { values ->
            values.plz?.let { plzInput.setText(it) }

            // Set radius spinner
            val radiusLabel = RADIUS_OPTIONS.find { it.second == values.radius }?.first ?: "50 km"
            radiusSpinner.setText(radiusLabel, false)

            // Set type chip
            when (values.type) {
                "Geschäft" -> typeChipGroup.check(R.id.chipGeschaeft)
                "Cafe" -> typeChipGroup.check(R.id.chipCafe)
                "Club" -> typeChipGroup.check(R.id.chipClub)
                "Location" -> typeChipGroup.check(R.id.chipLocation)
                else -> typeChipGroup.check(R.id.chipAll)
            }
        } ?: run {
            radiusSpinner.setText("50 km", false)
            typeChipGroup.check(R.id.chipAll)
        }

        // Close button
        closeButton.setOnClickListener {
            hideKeyboard()
            dismiss()
        }

        // Apply button
        btnApply.setOnClickListener {
            val plz = plzInput.text?.toString()?.trim()
            val radiusText = radiusSpinner.text.toString()
            val radius = RADIUS_OPTIONS.find { it.first == radiusText }?.second ?: 50

            val selectedType = when {
                typeChipGroup.checkedChipId == R.id.chipGeschaeft -> "Geschäft"
                typeChipGroup.checkedChipId == R.id.chipCafe -> "Cafe"
                typeChipGroup.checkedChipId == R.id.chipClub -> "Club"
                typeChipGroup.checkedChipId == R.id.chipLocation -> "Location"
                else -> null
            }

            hideKeyboard()
            onFilterApplied?.invoke(FilterValues(plz, radius, selectedType))
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
