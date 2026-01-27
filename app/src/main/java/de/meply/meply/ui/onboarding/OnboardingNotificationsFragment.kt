package de.meply.meply.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.meply.meply.R

class OnboardingNotificationsFragment : Fragment() {

    private lateinit var btnEnable: Button
    private lateinit var statusText: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateUI(isGranted)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnEnable = view.findViewById(R.id.btn_enable_notifications)
        statusText = view.findViewById(R.id.notification_status)

        btnEnable.setOnClickListener {
            requestNotificationPermission()
        }

        // Check current permission status
        checkPermissionStatus()
    }

    private fun checkPermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            updateUI(isGranted)
        } else {
            // Pre-Android 13, notifications are enabled by default
            updateUI(true)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun updateUI(isGranted: Boolean) {
        if (isGranted) {
            btnEnable.visibility = View.GONE
            statusText.text = "✓ Benachrichtigungen aktiviert"
            statusText.setTextColor(resources.getColor(R.color.success, null))
        } else {
            btnEnable.visibility = View.VISIBLE
            statusText.text = "Benachrichtigungen sind deaktiviert"
            statusText.setTextColor(resources.getColor(R.color.text_secondary, null))
        }
    }
}
