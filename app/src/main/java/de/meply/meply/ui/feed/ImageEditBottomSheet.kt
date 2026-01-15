package de.meply.meply.ui.feed

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R

class ImageEditBottomSheet : BottomSheetDialogFragment() {

    private var imageUri: Uri? = null
    private var currentAltText: String = ""
    private var onSave: ((String) -> Unit)? = null
    private var onDelete: (() -> Unit)? = null
    private var onCrop: ((Uri) -> Unit)? = null

    companion object {
        private const val ARG_IMAGE_URI = "imageUri"
        private const val ARG_ALT_TEXT = "altText"

        fun newInstance(imageUri: Uri, altText: String): ImageEditBottomSheet {
            return ImageEditBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URI, imageUri.toString())
                    putString(ARG_ALT_TEXT, altText)
                }
            }
        }
    }

    fun setOnSaveListener(listener: (String) -> Unit) {
        onSave = listener
    }

    fun setOnDeleteListener(listener: () -> Unit) {
        onDelete = listener
    }

    fun setOnCropListener(listener: (Uri) -> Unit) {
        onCrop = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUri = Uri.parse(it.getString(ARG_IMAGE_URI))
            currentAltText = it.getString(ARG_ALT_TEXT, "")
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
                behavior.isDraggable = false
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_image_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePreview = view.findViewById<ImageView>(R.id.editImagePreview)
        val altTextInput = view.findViewById<TextInputEditText>(R.id.editAltTextInput)
        val deleteButton = view.findViewById<Button>(R.id.editDeleteButton)
        val saveButton = view.findViewById<Button>(R.id.editSaveButton)
        val cropButton = view.findViewById<Button>(R.id.editCropButton)
        val charCounter = view.findViewById<TextView>(R.id.charCounter)

        // Setze Bild
        imageUri?.let { imagePreview.setImageURI(it) }

        // Setze aktuellen Alt-Text
        altTextInput.setText(currentAltText)
        updateCharCounter(charCounter, currentAltText.length)

        // Zeichenzähler aktualisieren
        altTextInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateCharCounter(charCounter, s?.length ?: 0)
            }
        })

        // Zuschneiden
        cropButton.setOnClickListener {
            imageUri?.let { uri ->
                onCrop?.invoke(uri)
                dismiss()
            }
        }

        // Löschen
        deleteButton.setOnClickListener {
            onDelete?.invoke()
            dismiss()
        }

        // Speichern
        saveButton.setOnClickListener {
            onSave?.invoke(altTextInput.text?.toString() ?: "")
            dismiss()
        }
    }

    private fun updateCharCounter(counter: TextView, length: Int) {
        counter.text = "$length / 200"
    }
}
