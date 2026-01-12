package de.meply.meply

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import de.meply.meply.ui.feed.CreatePostActivity
import de.meply.meply.ui.feed.FeedFragment
import de.meply.meply.ui.events.EventsFragment
import de.meply.meply.ui.pm.PmFragment
import de.meply.meply.ui.profile.ProfileFragment

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

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host, feed, "feed")
                .commit()
        }

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
                R.id.nav_more    -> { MoreMenuBottomSheet().show(supportFragmentManager, "more"); true }
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
