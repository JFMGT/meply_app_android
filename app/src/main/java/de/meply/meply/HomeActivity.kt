package de.meply.meply

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import de.meply.meply.ui.feed.CreatePostActivity
import de.meply.meply.ui.feed.FeedFragment
import de.meply.meply.ui.events.EventsFragment
import de.meply.meply.ui.pm.PmFragment
import de.meply.meply.ui.profile.ProfileFragment
import de.meply.meply.auth.AuthManager
import de.meply.meply.network.ApiClient

class HomeActivity : AppCompatActivity() {

    private val feed by lazy { FeedFragment() }
    private val events by lazy { EventsFragment() }
    private val pm by lazy { PmFragment() }
    private val profile by lazy { ProfileFragment() }

    private val createPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh feed when a new post is created
            (supportFragmentManager.findFragmentByTag("feed") as? FeedFragment)?.refreshFeed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Ensure JWT is loaded into ApiClient when HomeActivity starts
        // (in case the process was killed and restarted)
        AuthManager.getJwt(this)?.let { jwt ->
            ApiClient.setJwt(jwt)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host, feed, "feed")
                .commit()
        }

        setupToolbar()
        setupBottomNavigation()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val userIcon = findViewById<ImageView>(R.id.toolbarUserIcon)

        // Burger menu click
        toolbar.setNavigationOnClickListener {
            showDrawerMenu(toolbar)
        }

        // User icon click
        userIcon.setOnClickListener {
            showUserMenu(userIcon)
        }
    }

    private fun showDrawerMenu(anchor: MaterialToolbar) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.drawer_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_impressum -> {
                    openWebView("https://dev.meply.de/pages/impressum/", "Impressum")
                    true
                }
                R.id.menu_datenschutz -> {
                    openWebView("https://dev.meply.de/pages/datenschutzerklaerung/", "Datenschutzerklärung")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showUserMenu(anchor: ImageView) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.user_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_profile -> {
                    navigateToProfile()
                    true
                }
                R.id.menu_followers -> {
                    openFollowers()
                    true
                }
                R.id.menu_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openFollowers() {
        val intent = Intent(this, FollowersActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        AuthManager.clear(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun openWebView(url: String, title: String) {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("url", url)
        intent.putExtra("title", title)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_feed    -> switchTo(feed, "feed")
                R.id.nav_events  -> switchTo(events, "events")
                R.id.nav_create  -> {
                    val intent = Intent(this, CreatePostActivity::class.java)
                    createPostLauncher.launch(intent)
                    false // Don't select this item
                }
                R.id.nav_pm      -> switchTo(pm, "pm")
                else -> false
            }
        }
    }

    private fun switchTo(target: Fragment, tag: String): Boolean {
        if (supportFragmentManager.findFragmentById(R.id.nav_host) !== target) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host, target, tag)
                .commit()
        }
        return true
    }

    fun navigateToProfile() {
        switchTo(profile, "profile")
    }
}
