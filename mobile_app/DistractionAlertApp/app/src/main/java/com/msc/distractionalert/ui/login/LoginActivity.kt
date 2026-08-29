package com.msc.distractionalert.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.msc.distractionalert.DistractionAlertApp
import com.msc.distractionalert.databinding.ActivityLoginBinding
import com.msc.distractionalert.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DistractionAlertApp

        // Already logged in from a previous session - skip straight past the login form.
        if (app.authRepository.isLoggedIn) {
            goToHistory()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, LoginViewModel.Factory(app.authRepository))[LoginViewModel::class.java]

        binding.loginButton.setOnClickListener {
            viewModel.login(
                binding.usernameInput.text.toString().trim(),
                binding.passwordInput.text.toString()
            )
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is LoginUiState.Idle -> renderIdle()
                is LoginUiState.Loading -> renderLoading()
                is LoginUiState.Success -> onLoginSuccess()
                is LoginUiState.Failed -> renderFailed(state.message)
            }
        }
    }

    private fun renderIdle() {
        binding.loginProgress.visibility = View.GONE
        binding.loginButton.isEnabled = true
    }

    private fun renderLoading() {
        binding.loginProgress.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false
        binding.errorText.visibility = View.GONE
    }

    private fun renderFailed(message: String) {
        binding.loginProgress.visibility = View.GONE
        binding.loginButton.isEnabled = true
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
    }

    private fun onLoginSuccess() {
        registerFcmTokenIfAvailable()
        Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()
        goToHistory()
    }

    /**
     * Registers whatever FCM token this device currently has now that we're
     * authenticated; DistractionFirebaseMessagingService.onNewToken handles
     * future refreshes on its own, once one has happened at least once.
     */
    private fun registerFcmTokenIfAvailable() {
        val app = application as DistractionAlertApp
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            lifecycleScope.launch { app.authRepository.registerFcmToken(token) }
        }
    }

    private fun goToHistory() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
