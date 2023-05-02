package com.minkiapps.biometricwrapper.biometric.handler

import androidx.biometric.BiometricManager
import androidx.biometric.auth.AuthPromptErrorException
import androidx.biometric.auth.AuthPromptFailureException
import androidx.biometric.auth.AuthPromptHost
import androidx.biometric.auth.Class2BiometricAuthPrompt
import androidx.biometric.auth.authenticate
import androidx.fragment.app.FragmentActivity
import com.minkiapps.biometricwrapper.biometric.BiometricHandler
import com.minkiapps.biometricwrapper.biometric.BiometricUIModel
import timber.log.Timber

class BiometricXHandler(private val activity: FragmentActivity) : BiometricHandler {

    private val biometricManager = BiometricManager.from(activity)

    override fun canBeUsed() : Boolean {
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun showBiometricPrompt(uiModel: BiometricUIModel): Boolean {
        val prompt = Class2BiometricAuthPrompt
            .Builder(uiModel.title, activity.getText(android.R.string.cancel))
            .setDescription(uiModel.description)
            .setConfirmationRequired(false)
            .build()

        return try {
            prompt.authenticate(AuthPromptHost(activity))
            Timber.d("Biometric authentication succeeded!")
            true
        } catch (e: AuthPromptErrorException) {
            // Handle irrecoverable error during authentication.
            // Possible values for AuthPromptErrorException.errorCode are listed in the @IntDef,
            // androidx.biometric.BiometricPrompt.AuthenticationError.
            Timber.e("Biometric auth error, code: ${e.errorCode} message: ${e.errorMessage}")
            false
        } catch (e: AuthPromptFailureException) {
            // Handle auth failure due biometric credentials being rejected.
            Timber.e("Biometric auth rejected.")
            false
        }
    }
}