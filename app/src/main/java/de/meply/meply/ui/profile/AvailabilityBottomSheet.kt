package de.meply.meply.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.availability.SetAvailabilityRequest
import de.meply.meply.data.availability.UserAvailability
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * BottomSheet for setting/editing user availability status
 */
class AvailabilityBottomSheet : BottomSheetDialogFragment() {

    private lateinit var durationGroup: RadioGroup
    private lateinit var hostingHome: CheckBox
    private lateinit var hostingAway: CheckBox
    private lateinit var hostingNeutral: CheckBox
    private lateinit var noteInput: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var existingAvailability: UserAvailability? = null
    private var onSaveListener: ((SetAvailabilityRequest) -> Unit)? = null

    companion object {
        private const val ARG_EXISTING_EXPIRES_AT = "existing_expires_at"
        private const val ARG_EXISTING_HOSTING = "existing_hosting"
        private const val ARG_EXISTING_NOTE = "existing_note"

        fun newInstance(
            existingAvailability: UserAvailability? = null,
            onSave: (SetAvailabilityRequest) -> Unit
        ): AvailabilityBottomSheet {
            return AvailabilityBottomSheet().apply {
                arguments = Bundle().apply {
                    existingAvailability?.let {
                        putString(ARG_EXISTING_EXPIRES_AT, it.expiresAt)
                        putStringArrayList(ARG_EXISTING_HOSTING, ArrayList(it.hostingPreference ?: emptyList()))
                        putString(ARG_EXISTING_NOTE, it.note)
                    }
                }
                this.onSaveListener = onSave
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_availability, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        durationGroup = view.findViewById(R.id.duration_group)
        hostingHome = view.findViewById(R.id.hosting_home)
        hostingAway = view.findViewById(R.id.hosting_away)
        hostingNeutral = view.findViewById(R.id.hosting_neutral)
        noteInput = view.findViewById(R.id.note_input)
        btnSave = view.findViewById(R.id.btn_save_availability)
        btnCancel = view.findViewById(R.id.btn_cancel)

        // Set defaults
        durationGroup.check(R.id.duration_today)
        hostingHome.isChecked = true

        // Restore existing values if editing
        arguments?.let { args ->
            args.getStringArrayList(ARG_EXISTING_HOSTING)?.let { hosting ->
                hostingHome.isChecked = hosting.contains("home")
                hostingAway.isChecked = hosting.contains("away")
                hostingNeutral.isChecked = hosting.contains("neutral")
            }
            args.getString(ARG_EXISTING_NOTE)?.let { note ->
                noteInput.setText(note)
            }
        }

        // Button listeners
        btnSave.setOnClickListener { onSaveClicked() }
        btnCancel.setOnClickListener { dismiss() }
    }

    private fun onSaveClicked() {
        // Validate hosting preference
        if (!hostingHome.isChecked && !hostingAway.isChecked && !hostingNeutral.isChecked) {
            Toast.makeText(context, "Bitte wähle mindestens eine Spielort-Option", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate expiry date
        val expiresAt = calculateExpiryDate()

        // Build hosting preference list
        val hostingPreference = mutableListOf<String>()
        if (hostingHome.isChecked) hostingPreference.add("home")
        if (hostingAway.isChecked) hostingPreference.add("away")
        if (hostingNeutral.isChecked) hostingPreference.add("neutral")

        // Get note (optional)
        val note = noteInput.text?.toString()?.takeIf { it.isNotBlank() }

        // Create request
        val request = SetAvailabilityRequest(
            expiresAt = expiresAt,
            hostingPreference = hostingPreference,
            note = note
        )

        // Callback
        onSaveListener?.invoke(request)
        dismiss()
    }

    private fun calculateExpiryDate(): String {
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()

        val expiryDateTime = when (durationGroup.checkedRadioButtonId) {
            R.id.duration_today -> {
                // End of today (23:59:59)
                now.toLocalDate().atTime(23, 59, 59)
            }
            R.id.duration_tomorrow -> {
                // End of tomorrow
                now.toLocalDate().plusDays(1).atTime(23, 59, 59)
            }
            R.id.duration_weekend -> {
                // End of Sunday
                val nextSunday = if (now.dayOfWeek == DayOfWeek.SUNDAY) {
                    now.toLocalDate()
                } else {
                    now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                }
                nextSunday.atTime(23, 59, 59)
            }
            R.id.duration_week -> {
                // 7 days from now
                now.plusDays(7).toLocalDate().atTime(23, 59, 59)
            }
            else -> {
                // Default: end of today
                now.toLocalDate().atTime(23, 59, 59)
            }
        }

        // Convert to ISO 8601 UTC format
        val instant = expiryDateTime.atZone(zone).toInstant()
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }
}
