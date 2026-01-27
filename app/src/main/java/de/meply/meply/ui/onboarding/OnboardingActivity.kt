package de.meply.meply.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import de.meply.meply.HomeActivity
import de.meply.meply.R
import de.meply.meply.network.ApiClient
import de.meply.meply.data.onboarding.CompleteOnboardingResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var btnBack: Button
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView

    private val fragments = listOf(
        OnboardingWelcomeFragment(),
        OnboardingGamesFragment(),
        OnboardingFeaturesFragment(),
        OnboardingNotificationsFragment(),
        OnboardingCompleteFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.onboarding_viewpager)
        dotsLayout = findViewById(R.id.onboarding_dots)
        btnBack = findViewById(R.id.btn_onboarding_back)
        btnNext = findViewById(R.id.btn_onboarding_next)
        btnSkip = findViewById(R.id.btn_onboarding_skip)

        setupViewPager()
        setupDots()
        setupButtons()
    }

    private fun setupViewPager() {
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButtons(position)
            }
        })

        // Disable swipe on games fragment (need min 10 games)
        viewPager.isUserInputEnabled = true
    }

    private fun setupDots() {
        for (i in fragments.indices) {
            val dot = ImageView(this)
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_unselected))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            dot.layoutParams = params
            dotsLayout.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(position: Int) {
        for (i in 0 until dotsLayout.childCount) {
            val dot = dotsLayout.getChildAt(i) as ImageView
            dot.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (i == position) R.drawable.dot_selected else R.drawable.dot_unselected
                )
            )
        }
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem = viewPager.currentItem - 1
            }
        }

        btnNext.setOnClickListener {
            val currentFragment = fragments[viewPager.currentItem]

            // Validate current step before proceeding
            if (currentFragment is OnboardingStepValidator && !currentFragment.canProceed()) {
                currentFragment.showValidationError()
                return@setOnClickListener
            }

            if (viewPager.currentItem < fragments.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                completeOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            completeOnboarding()
        }

        updateButtons(0)
    }

    private fun updateButtons(position: Int) {
        // Back button
        btnBack.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE

        // Next button text
        btnNext.text = if (position == fragments.size - 1) "Los geht's!" else "Weiter"

        // Skip button (hide on last step)
        btnSkip.visibility = if (position < fragments.size - 1) View.VISIBLE else View.GONE
    }

    private fun completeOnboarding() {
        btnNext.isEnabled = false
        btnNext.text = "Wird geladen..."

        ApiClient.retrofit.completeOnboarding()
            .enqueue(object : Callback<CompleteOnboardingResponse> {
                override fun onResponse(
                    call: Call<CompleteOnboardingResponse>,
                    response: Response<CompleteOnboardingResponse>
                ) {
                    if (response.isSuccessful) {
                        Log.d("Onboarding", "Onboarding completed: ${response.body()?.data?.isOnboarded}")
                        goToHome()
                    } else {
                        Log.e("Onboarding", "Error completing onboarding: ${response.code()}")
                        // Still go to home even if API fails
                        goToHome()
                    }
                }

                override fun onFailure(call: Call<CompleteOnboardingResponse>, t: Throwable) {
                    Log.e("Onboarding", "Network error completing onboarding", t)
                    // Still go to home even if API fails
                    goToHome()
                }
            })
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("skipOnboardingCheck", true)
        startActivity(intent)
        finish()
    }

    fun goToNextStep() {
        if (viewPager.currentItem < fragments.size - 1) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }
}

/**
 * Interface for fragments that need validation before proceeding
 */
interface OnboardingStepValidator {
    fun canProceed(): Boolean
    fun showValidationError()
}
