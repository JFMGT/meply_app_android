package de.meply.meply

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import de.meply.meply.ui.profile.UserProfileActivity
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

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbarCreateButton: View
    private var currentUserSlug: String? = null

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

        drawerLayout = findViewById(R.id.drawerLayout)

        setupToolbar()
        setupBottomNavigation()
        setupDrawer()
        setupDrawerWidth()
        loadUserData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbarCreateButton = findViewById(R.id.toolbarCreateButton)

        // Burger menu click
        toolbar.setNavigationOnClickListener {
            showDrawerMenu(toolbar)
        }

        // Create button click - opens CreatePostActivity
        toolbarCreateButton.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            createPostLauncher.launch(intent)
        }

        // Show create button initially (Feed is default)
        toolbarCreateButton.visibility = View.VISIBLE
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

    private fun setupDrawer() {
        // User header click - open own profile in visitor view
        findViewById<View>(R.id.drawerUserHeader).setOnClickListener {
            currentUserSlug?.let { slug ->
                UserProfileActivity.start(this, slug)
            }
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // Menu item clicks
        findViewById<TextView>(R.id.menuProfile).setOnClickListener {
            navigateToProfile()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<TextView>(R.id.menuFollowers).setOnClickListener {
            openFollowers()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<TextView>(R.id.menuGesuche).setOnClickListener {
            openGesuche()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<TextView>(R.id.menuCollection).setOnClickListener {
            openCollection()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<TextView>(R.id.menuLogout).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            logout()
        }
    }

    private fun setupDrawerWidth() {
        // Set drawer width to 90% of screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val drawerWidth = (screenWidth * 0.9).toInt()

        val drawer = findViewById<View>(R.id.userDrawer)
        val params = drawer.layoutParams
        params.width = drawerWidth
        drawer.layoutParams = params
    }

    private fun openUserDrawer() {
        drawerLayout.openDrawer(GravityCompat.END)
    }

    private fun openFollowers() {
        toolbarCreateButton.visibility = View.GONE
        switchTo(followers, "followers")
    }

    private fun openGesuche() {
        toolbarCreateButton.visibility = View.GONE
        switchTo(gesuche, "gesuche")
    }

    private fun openCollection() {
        toolbarCreateButton.visibility = View.GONE
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
                R.id.nav_feed    -> {
                    toolbarCreateButton.visibility = View.VISIBLE
                    switchTo(feed, "feed")
                }
                R.id.nav_events  -> {
                    toolbarCreateButton.visibility = View.GONE
                    switchTo(events, "events")
                }
                R.id.nav_pm      -> {
                    toolbarCreateButton.visibility = View.GONE
                    switchTo(pm, "pm")
                }
                R.id.nav_user    -> {
                    openUserDrawer()
                    false // Don't select this item, just open drawer
                }
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
        toolbarCreateButton.visibility = View.GONE
        switchTo(profile, "profile")
    }

    fun refreshUserAvatar() {
        loadUserData()
    }

    private fun loadUserData() {
        val drawerAvatar = findViewById<ImageView>(R.id.drawerUserAvatar)
        val drawerUserName = findViewById<TextView>(R.id.drawerUserName)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

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
                        val username = profileData?.username ?: "Benutzer"
                        currentUserSlug = profileData?.userslug

                        // Update drawer username
                        drawerUserName.text = username

                        // Determine avatar URL
                        val fullAvatarUrl = if (!avatarUrl.isNullOrEmpty()) {
                            "${ApiClient.STRAPI_IMAGE_BASE}$avatarUrl"
                        } else {
                            AvatarUtils.getDefaultAvatarUrl(userId)
                        }

                        // Update drawer avatar
                        Glide.with(this@HomeActivity)
                            .load(fullAvatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(drawerAvatar)

                        // Update bottom navigation avatar
                        loadBottomNavAvatar(bottomNav, fullAvatarUrl)
                    } else {
                        Log.e("HomeActivity", "Failed to load profile: ${response.code()}")
                        drawerUserName.text = "Benutzer"
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    Log.e("HomeActivity", "Error loading profile", t)
                    drawerUserName.text = "Benutzer"
                }
            })
    }

    private fun loadBottomNavAvatar(bottomNav: BottomNavigationView, avatarUrl: String) {
        // Größeres Icon (40dp) um den Platz von Icon + Label zu füllen
        val size = (40 * resources.displayMetrics.density).toInt()

        Log.d("HomeActivity", "Loading bottom nav avatar: $avatarUrl, size: $size")

        Glide.with(this)
            .asBitmap()
            .load(avatarUrl)
            .circleCrop()
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>(size, size) {
                override fun onResourceReady(
                    resource: android.graphics.Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
                ) {
                    Log.d("HomeActivity", "Bottom nav avatar loaded successfully: ${resource.width}x${resource.height}")
                    val drawable = android.graphics.drawable.BitmapDrawable(resources, resource)
                    drawable.setBounds(0, 0, size, size)

                    val menuItem = bottomNav.menu.findItem(R.id.nav_user)
                    menuItem?.icon = drawable

                    // Deaktiviere Icon-Tinting für das Avatar
                    bottomNav.itemIconTintList = null
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // Keep current icon
                }

                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    Log.e("HomeActivity", "Failed to load bottom nav avatar")
                }
            })
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
