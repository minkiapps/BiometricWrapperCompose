package com.minkiapps.biometricwrapper.biometric.handler

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.AuthPromptErrorException
import androidx.biometric.auth.AuthPromptFailureException
import androidx.biometric.auth.AuthPromptHost
import androidx.biometric.auth.Class2BiometricAuthPrompt
import androidx.fragment.app.FragmentActivity
import com.minkiapps.biometricwrapper.biometric.BiometricHandler
import com.minkiapps.biometricwrapper.biometric.BiometricUIModel
import com.minkiapps.biometricwrapper.util.resumeIfPossible
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

class BiometricHandlerImpl(private val activity: FragmentActivity) :
    BiometricHandler {

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
            prompt.showBiometricPrompt(AuthPromptHost(activity))
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

private suspend fun Class2BiometricAuthPrompt.showBiometricPrompt(host: AuthPromptHost) : BiometricPrompt.AuthenticationResult{
    return suspendCancellableCoroutine { continuation ->
        val authPrompt = startAuthentication(
            host,
            Runnable::run,
            CoroutineAuthPromptCallback(continuation)
        )

        continuation.invokeOnCancellation {
            authPrompt.cancelAuthentication()
        }
    }
}

private class CoroutineAuthPromptCallback(
    private val continuation: CancellableContinuation<BiometricPrompt.AuthenticationResult>
) : AuthPromptCallback() {
    override fun onAuthenticationError(
        activity: FragmentActivity?,
        errorCode: Int,
        errString: CharSequence
    ) {
        continuation.resumeIfPossible(AuthPromptErrorException(errorCode, errString))
    }

    override fun onAuthenticationSucceeded(
        activity: FragmentActivity?,
        result: BiometricPrompt.AuthenticationResult
    ) {
        continuation.resumeIfPossible(result)
    }

    override fun onAuthenticationFailed(activity: FragmentActivity?) {
        Timber.e("Biometric onAuthFailed")
    }
}
