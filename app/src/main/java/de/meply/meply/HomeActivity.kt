package de.meply.meply

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import de.meply.meply.ui.feed.CreatePostActivity
import de.meply.meply.ui.feed.FeedFragment
import de.meply.meply.ui.events.EventsFragment
import de.meply.meply.ui.pm.PmFragment
import de.meply.meply.ui.profile.ProfileFragment
import de.meply.meply.ui.followers.FollowersFragment
import de.meply.meply.ui.collection.MyCollectionFragment
import de.meply.meply.auth.AuthManager
import de.meply.meply.network.ApiClient
import de.meply.meply.data.profile.ProfileMeData
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.utils.AvatarUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private val feed by lazy { FeedFragment() }
    private val events by lazy { EventsFragment() }
    private val pm by lazy { PmFragment() }
    private val profile by lazy { ProfileFragment() }
    private val followers by lazy { FollowersFragment() }
    private val gesuche by lazy { GesucheFragment() }
    private val collection by lazy { MyCollectionFragment() }

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
        loadUserAvatar()
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
                R.id.menu_gesuche -> {
                    openGesuche()
                    true
                }
                R.id.menu_collection -> {
                    openCollection()
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
        switchTo(followers, "followers")
    }

    private fun openGesuche() {
        switchTo(gesuche, "gesuche")
    }

    private fun openCollection() {
        switchTo(collection, "collection")
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

    fun refreshUserAvatar() {
        loadUserAvatar()
    }

    private fun loadUserAvatar() {
        val userIcon = findViewById<ImageView>(R.id.toolbarUserIcon)

        ApiClient.retrofit.getMyProfile()
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    if (response.isSuccessful) {
                        val profileData = response.body()?.data
                        val userId = profileData?.documentId ?: "default"
                        val avatarUrl = profileData?.avatar?.firstOrNull()?.url

                        if (!avatarUrl.isNullOrEmpty()) {
                            // Uploaded avatar exists
                            val fullUrl = "${ApiClient.STRAPI_IMAGE_BASE}$avatarUrl"
                            Glide.with(this@HomeActivity)
                                .load(fullUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .into(userIcon)
                        } else {
                            // Use generated avatar
                            val defaultAvatarUrl = AvatarUtils.getDefaultAvatarUrl(userId)
                            Glide.with(this@HomeActivity)
                                .load(defaultAvatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .into(userIcon)
                        }
                    } else {
                        Log.e("HomeActivity", "Failed to load profile: ${response.code()}")
                        // Show placeholder on error
                        Glide.with(this@HomeActivity)
                            .load(R.drawable.ic_launcher_foreground)
                            .circleCrop()
                            .into(userIcon)
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    Log.e("HomeActivity", "Error loading profile avatar", t)
                    // Show placeholder on error
                    Glide.with(this@HomeActivity)
                        .load(R.drawable.ic_launcher_foreground)
                        .circleCrop()
                        .into(userIcon)
                }
            })
    }
}
