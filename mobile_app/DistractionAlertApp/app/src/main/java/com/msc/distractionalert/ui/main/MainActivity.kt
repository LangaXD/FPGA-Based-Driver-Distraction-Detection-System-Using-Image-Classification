package com.msc.distractionalert.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.msc.distractionalert.DistractionAlertApp
import com.msc.distractionalert.R
import com.msc.distractionalert.databinding.ActivityMainBinding
import com.msc.distractionalert.ui.chart.ChartFragment
import com.msc.distractionalert.ui.history.HistoryFragment
import com.msc.distractionalert.ui.settings.SettingsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How often to re-poll /api/alerts while the app is in the foreground.
 * Stands in for FCM push, which is wired but inert until a real Firebase
 * project is configured (see FIREBASE_SETUP.md) - without this, a new board
 * alert never appears until the user force-closes/relaunches the app or
 * pulls to refresh manually. */
private const val ALERTS_POLL_INTERVAL_MS = 15_000L

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var alertsViewModel: AlertsViewModel
        private set

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val app = application as DistractionAlertApp
        alertsViewModel = ViewModelProvider(
            this,
            AlertsViewModel.Factory(app.alertRepository)
        )[AlertsViewModel::class.java]

        binding.viewPager.adapter = TabsAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_history)
                else -> getString(R.string.tab_stats)
            }
        }.attach()

        requestNotificationPermissionIfNeeded()
        startAlertsPolling()
    }

    /** Foreground-only polling fallback - see ALERTS_POLL_INTERVAL_MS above. */
    private fun startAlertsPolling() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(ALERTS_POLL_INTERVAL_MS)
                    alertsViewModel.refresh()
                }
            }
        }
    }

    /** POST_NOTIFICATIONS is a runtime permission from API 33 onward; without it, alerts arrive but never show. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private class TabsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment =
            if (position == 0) HistoryFragment() else ChartFragment()
    }
}
