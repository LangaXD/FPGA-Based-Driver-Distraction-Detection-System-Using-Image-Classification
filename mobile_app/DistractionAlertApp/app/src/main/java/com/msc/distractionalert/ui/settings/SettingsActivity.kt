package com.msc.distractionalert.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.msc.distractionalert.DistractionAlertApp
import com.msc.distractionalert.R
import com.msc.distractionalert.databinding.ActivitySettingsBinding
import com.msc.distractionalert.ui.login.LoginActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val app = application as DistractionAlertApp
        viewModel = ViewModelProvider(
            this,
            SettingsViewModel.Factory(app.prefsManager, app.authRepository)
        )[SettingsViewModel::class.java]

        binding.serverUrlInput.setText(viewModel.serverUrl)
        binding.notificationsSwitch.isChecked = viewModel.notificationsEnabled

        binding.saveButton.setOnClickListener {
            val url = binding.serverUrlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                viewModel.saveServerUrl(url)
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            }
        }

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotificationsEnabled(isChecked)
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
