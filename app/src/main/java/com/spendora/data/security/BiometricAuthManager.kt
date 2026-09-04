package com.spendora.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.spendora.data.dao.AppSettingDao
import com.spendora.data.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    data class Unavailable(val reason: String) : BiometricAvailability()
}

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Failure(val message: String) : BiometricAuthResult()
    object UserCanceled : BiometricAuthResult()
}

/**
 * BiometricAuthManager
 *
 * Enforces authentic Android OS biometric authentication (BiometricPrompt).
 * Privacy Invariants:
 * - Never stores biometric templates, fingerprints, or PIN credentials.
 * - Authenticates strictly via Android BiometricPrompt with DEVICE_CREDENTIAL fallback.
 * - Completely decoupled from background SMS broadcast processing (SMS persistence continues natively).
 */
class BiometricAuthManager(
    private val appSettingDao: AppSettingDao
) {
    companion object {
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

    fun isAppLockEnabled(): Flow<Boolean> {
        return appSettingDao.observe(KEY_APP_LOCK_ENABLED).map { value ->
            value?.toBoolean() ?: false
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        appSettingDao.set(
            AppSettingEntity(
                key = KEY_APP_LOCK_ENABLED,
                value = enabled.toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.Unavailable("No biometric hardware detected")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.Unavailable("Biometric hardware currently unavailable")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.Unavailable("No biometric or screen lock enrolled")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.Unavailable("Security update required")
            else -> BiometricAvailability.Unavailable("Biometric authentication unavailable")
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock SPENDORA",
        subtitle: String = "Authenticate to access personal financial data",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onResult(BiometricAuthResult.UserCanceled)
                } else {
                    onResult(BiometricAuthResult.Failure(errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failure("Authentication failed"))
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        val prompt = BiometricPrompt(activity, executor, callback)
        prompt.authenticate(promptInfo)
    }
}
