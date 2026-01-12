package de.meply.meply

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.meply.meply.auth.AuthManager

class MoreMenuBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.bottom_sheet_more, container, false)

        v.findViewById<TextView>(R.id.item_profile).setOnClickListener {
            // Navigate to Profile Fragment
            val activity = requireActivity() as? HomeActivity
            activity?.navigateToProfile()
            dismiss()
        }
        v.findViewById<TextView>(R.id.item_map).setOnClickListener {
            // TODO: Map öffnen
            dismiss()
        }
        v.findViewById<TextView>(R.id.item_users).setOnClickListener {
            // TODO: Benutzerliste öffnen
            dismiss()
        }
        v.findViewById<TextView>(R.id.item_fav).setOnClickListener {
            // TODO: Lieblingsspiele öffnen
            dismiss()
        }
        v.findViewById<TextView>(R.id.item_settings).setOnClickListener {
            // TODO: Einstellungen öffnen
            dismiss()
        }
        v.findViewById<TextView>(R.id.item_logout).setOnClickListener {
            // Logout: Token löschen und zurück zum Login
            AuthManager.clear(requireContext())
            dismiss()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        return v
    }
}
