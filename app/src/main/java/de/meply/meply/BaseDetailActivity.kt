package de.meply.meply

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupDetailToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val userIcon = findViewById<ImageView>(R.id.toolbarUserIcon)

        // Back button
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // User icon click
        userIcon?.setOnClickListener {
            showUserMenu(userIcon)
        }
    }

    private fun showUserMenu(anchor: ImageView) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.user_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_profile -> {
                    // Navigate to HomeActivity and switch to profile
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    // HomeActivity will need to handle switching to profile
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
