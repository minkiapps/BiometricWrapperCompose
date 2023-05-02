package com.minkiapps.biometricwrapper.biometric.handler

import android.content.Context
import android.os.CancellationSignal
import com.huawei.hms.support.api.fido.bioauthn.BioAuthnCallback
import com.huawei.hms.support.api.fido.bioauthn.BioAuthnResult
import com.huawei.hms.support.api.fido.bioauthn.FaceManager
import com.minkiapps.biometricwrapper.biometric.BiometricHandler
import com.minkiapps.biometricwrapper.biometric.BiometricUIModel
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdContinueable.ContinueState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.FaceIDUIState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.TransitionState
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

interface HuaweiFaceIdUIStateable {
    sealed class FaceIDUIState {
        object None : FaceIDUIState()

        class Recognising(val faceIDContinuation: HuaweiFaceIdContinueable) : FaceIDUIState()

        class AskToRetry(val faceIDContinuation: HuaweiFaceIdContinueable) : FaceIDUIState()
    }

    sealed class TransitionState {
        object Default : TransitionState()
        object ToSuccess : TransitionState()
        object ToFailed : TransitionState()
    }

    fun getShowUIDuringFaceIDFlow() : StateFlow<FaceIDUIState>
}

interface HuaweiFaceIdContinueable {

    fun getUITransitionFlow() : StateFlow<TransitionState>

    fun continueWith(state : ContinueState)

    sealed class ContinueState {
        object Retry : ContinueState()

        object Cancel : ContinueState()

        object SuccessTransitionEnd : ContinueState()

        object FailedTransitionEnd : ContinueState()
    }
}

class HuaweiFaceIDHandler(context: Context) : BiometricHandler, HuaweiFaceIdUIStateable {

    companion object {
        //https://developer.huawei.com/consumer/en/doc/development/Security-References/facemanager_x-0000001050418949
        const val FACE_ERROR_HW_UNAVAILABLE = 1
        const val FACE_ERROR_TIMEOUT = 3
    }

    private val faceManager = FaceManager(context)

    private val uiDuringFaceIDFlow = MutableStateFlow<FaceIDUIState>(FaceIDUIState.None)

    override fun getShowUIDuringFaceIDFlow(): StateFlow<FaceIDUIState> = uiDuringFaceIDFlow

    override fun canBeUsed() : Boolean {
        return faceManager.canAuth() == FaceManager.FACE_SUCCESS
    }

    override suspend fun showBiometricPrompt(uiModel: BiometricUIModel): Boolean {
        return suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            val callbackWithContinuation = FaceIDContinuation(cont, cancellationSignal)

            uiDuringFaceIDFlow.update {
                FaceIDUIState.Recognising(callbackWithContinuation)
            }
            faceManager.auth(null, cancellationSignal, 0, callbackWithContinuation, null)

            cont.invokeOnCancellation {
                cancellationSignal.cancel()
            }
        }
    }

    inner class FaceIDContinuation(
        private val continuation: CancellableContinuation<Boolean>,
        private val cancellationSignal: CancellationSignal
    ) : BioAuthnCallback(), HuaweiFaceIdContinueable {

        private val transitionFlow = MutableStateFlow<TransitionState>(TransitionState.Default)

        override fun continueWith(state: ContinueState) {
            when(state) {
                is ContinueState.Cancel -> {
                    uiDuringFaceIDFlow.update { FaceIDUIState.None }
                    continuation.resume(false)
                }
                is ContinueState.Retry -> {
                    uiDuringFaceIDFlow.update {
                        FaceIDUIState.Recognising(this)
                    }
                    faceManager.auth(null, cancellationSignal, 0, this, null)
                }
                is ContinueState.SuccessTransitionEnd -> {
                    uiDuringFaceIDFlow.update { FaceIDUIState.None }
                    continuation.resume(true)
                }

                ContinueState.FailedTransitionEnd -> {
                    uiDuringFaceIDFlow.update { FaceIDUIState.None }
                    continuation.resume(false)
                }
            }
        }

        override fun getUITransitionFlow(): StateFlow<TransitionState> {
            return transitionFlow
        }

        override fun onAuthError(errMsgId: Int, errString: CharSequence) {
            Timber.e("Face Authentication error errorCode=$errMsgId,errorMessage=$errString")

            when(errMsgId) {
                FACE_ERROR_TIMEOUT -> {
                    uiDuringFaceIDFlow.update { FaceIDUIState.AskToRetry(this) }
                }
                FACE_ERROR_HW_UNAVAILABLE -> {
                    //ignore, since FACE_ERROR_TIMEOUT will follow
                }
                else -> {
                    transitionFlow.update { TransitionState.ToFailed }
                }
            }
        }

        override fun onAuthHelp(helpMsgId: Int, helpString: CharSequence) {
            Timber.d("Face Authentication help helpMsgId=$helpMsgId, helpString=$helpString")
        }

        override fun onAuthSucceeded(result: BioAuthnResult) {
            Timber.d("Face Authentication succeeded!")
            transitionFlow.update { TransitionState.ToSuccess }
        }

        override fun onAuthFailed() {
            Timber.e("Face Authentication failed.")
            transitionFlow.update { TransitionState.ToFailed }
        }
    }
}