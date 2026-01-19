package de.meply.meply.ui.collection

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R

data class CollectionFilter(
    val title: String? = null,
    val state: String? = null,
    val minRating: Float? = null,
    val forSale: String? = null,
    val sortBy: String = "title"
)

class CollectionFilterBottomSheet : BottomSheetDialogFragment() {

    private var onFilterApplied: ((CollectionFilter) -> Unit)? = null
    private var currentFilter: CollectionFilter = CollectionFilter()

    private val stateOptions = listOf(
        "Alle" to null,
        "Will spielen" to "wishlist",
        "Habe gespielt" to "played",
        "Besitze" to "owned"
    )

    private val ratingOptions = listOf(
        "Alle" to null,
        "★ 1+" to 1f,
        "★ 2+" to 2f,
        "★ 3+" to 3f,
        "★ 4+" to 4f,
        "★ 5" to 5f
    )

    private val forSaleOptions = listOf(
        "Alle" to null,
        "Nicht angeboten" to "false",
        "Angeboten" to "true"
    )

    private val sortOptions = listOf(
        "Alphabetisch (A-Z)" to "title",
        "Bewertung (5★ → 1★)" to "rating"
    )

    fun setOnFilterAppliedListener(listener: (CollectionFilter) -> Unit) {
        onFilterApplied = listener
    }

    fun setCurrentFilter(filter: CollectionFilter) {
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
        return inflater.inflate(R.layout.bottom_sheet_collection_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleInput = view.findViewById<TextInputEditText>(R.id.titleInput)
        val stateSpinner = view.findViewById<Spinner>(R.id.stateSpinner)
        val ratingSpinner = view.findViewById<Spinner>(R.id.ratingSpinner)
        val forSaleSpinner = view.findViewById<Spinner>(R.id.forSaleSpinner)
        val sortSpinner = view.findViewById<Spinner>(R.id.sortSpinner)
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val clearButton = view.findViewById<Button>(R.id.clearButton)
        val applyButton = view.findViewById<Button>(R.id.applyButton)

        // Setup spinners
        setupSpinner(stateSpinner, stateOptions.map { it.first })
        setupSpinner(ratingSpinner, ratingOptions.map { it.first })
        setupSpinner(forSaleSpinner, forSaleOptions.map { it.first })
        setupSpinner(sortSpinner, sortOptions.map { it.first })

        // Set current filter values
        currentFilter.title?.let { titleInput.setText(it) }
        currentFilter.state?.let { state ->
            val index = stateOptions.indexOfFirst { it.second == state }
            if (index >= 0) stateSpinner.setSelection(index)
        }
        currentFilter.minRating?.let { rating ->
            val index = ratingOptions.indexOfFirst { it.second == rating }
            if (index >= 0) ratingSpinner.setSelection(index)
        }
        currentFilter.forSale?.let { forSale ->
            val index = forSaleOptions.indexOfFirst { it.second == forSale }
            if (index >= 0) forSaleSpinner.setSelection(index)
        }
        // Set current sort value
        val sortIndex = sortOptions.indexOfFirst { it.second == currentFilter.sortBy }
        if (sortIndex >= 0) sortSpinner.setSelection(sortIndex)

        // Close button
        closeButton.setOnClickListener {
            dismiss()
        }

        // Clear button - reset all filters
        clearButton.setOnClickListener {
            onFilterApplied?.invoke(CollectionFilter())
            dismiss()
        }

        // Apply button
        applyButton.setOnClickListener {
            val title = titleInput.text?.toString()?.trim()?.ifEmpty { null }
            val state = stateOptions[stateSpinner.selectedItemPosition].second
            val minRating = ratingOptions[ratingSpinner.selectedItemPosition].second
            val forSale = forSaleOptions[forSaleSpinner.selectedItemPosition].second
            val sortBy = sortOptions[sortSpinner.selectedItemPosition].second

            onFilterApplied?.invoke(CollectionFilter(title, state, minRating, forSale, sortBy))
            dismiss()
        }
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.setTextColor(resources.getColor(R.color.text_on_light, null))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setPadding(16, 16, 16, 16)
                }
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    companion object {
        fun newInstance(currentFilter: CollectionFilter = CollectionFilter()): CollectionFilterBottomSheet {
            return CollectionFilterBottomSheet().apply {
                setCurrentFilter(currentFilter)
            }
        }
    }
}
