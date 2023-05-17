package com.minkiapps.biometricwrapper.biometric.handler

import android.os.CancellationSignal
import android.os.Looper
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.huawei.hms.support.api.fido.bioauthn.BioAuthnCallback
import com.huawei.hms.support.api.fido.bioauthn.BioAuthnResult
import com.huawei.hms.support.api.fido.bioauthn.FaceManager
import com.minkiapps.biometricwrapper.biometric.BiometricHandler
import com.minkiapps.biometricwrapper.biometric.BiometricResult
import com.minkiapps.biometricwrapper.biometric.BiometricUIModel
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdContinueable.FaceIDState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.FaceIDUIState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.TransitionState
import com.minkiapps.biometricwrapper.util.resumeIfPossible
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

interface HuaweiFaceIdUIStateable {
    sealed class FaceIDUIState {
        object None : FaceIDUIState()

        class Recognising(val faceIDContinuation: HuaweiFaceIdContinueable) : FaceIDUIState()

        class NotRecognised(
            val faceIDContinuation: HuaweiFaceIdContinueable,
            val isNonRecoverableError : Boolean,
            val hasFingerprint : Boolean) : FaceIDUIState()
    }

    sealed class TransitionState {
        object Default : TransitionState()
        object ToSuccess : TransitionState()
    }

    fun getShowUIDuringFaceIDFlow() : StateFlow<FaceIDUIState>
}

interface HuaweiFaceIdContinueable {

    fun getUITransitionFlow() : StateFlow<TransitionState>

    fun continueWith(state : FaceIDState)

    sealed class FaceIDState {
        object Recognising : FaceIDState()
        object Recognised : FaceIDState()
        object SuccessTransitionEnd : FaceIDState()
        object NotRecognised : FaceIDState()
        object UseDefaultBiometric : FaceIDState()
        object Cancel : FaceIDState()
        object Failed : FaceIDState()
        object Final : FaceIDState() //after this state all further state chnages are ignored
    }
}

class HuaweiFaceIDHandler(private val activity: FragmentActivity,
                          private val defaultBiometricHandler: BiometricHandler?)
    : BiometricHandler, HuaweiFaceIdUIStateable {

    companion object {
        //https://developer.huawei.com/consumer/en/doc/development/Security-References/facemanager_x-0000001050418949
        const val FACE_ERROR_HW_UNAVAILABLE = 1
        const val FACE_ERROR_TIMEOUT = 3
        const val FACE_ERROR_CANCELED = 5
    }

    enum class FaceIDHandlerResult {
        SUCCESS,
        CANCELLED,
        FALLBACK_TO_DEFAULT
    }

    private val faceManager = FaceManager(activity)

    private val uiDuringFaceIDFlow = MutableStateFlow<FaceIDUIState>(FaceIDUIState.None)

    override fun getShowUIDuringFaceIDFlow(): StateFlow<FaceIDUIState> = uiDuringFaceIDFlow

    fun canBeUsed() : Boolean {
        return faceManager.canAuth() == FaceManager.FACE_SUCCESS
    }

    fun nonEnrolled() : Boolean {
        return faceManager.canAuth() == FaceManager.FACE_ERROR_NOT_ENROLLED
    }

    @MainThread
    override suspend fun showBiometricPrompt(uiModel: BiometricUIModel): BiometricResult {
        val result : FaceIDHandlerResult = suspendCancellableCoroutine { cont ->
            val faceIDContinuation = FaceIDContinuation(cont, CancellationSignal(), activity.lifecycle)
            faceIDContinuation.continueWith(FaceIDState.Recognising)
        }

        return when(result) {
            FaceIDHandlerResult.SUCCESS -> BiometricResult.Success
            FaceIDHandlerResult.CANCELLED -> BiometricResult.Cancelled
            FaceIDHandlerResult.FALLBACK_TO_DEFAULT -> {
                defaultBiometricHandler?.showBiometricPrompt(uiModel) ?: BiometricResult.Cancelled
            }
        }
    }

    inner class FaceIDContinuation(
        private val continuation: CancellableContinuation<FaceIDHandlerResult>,
        private val cancellationSignal: CancellationSignal,
        lifecycle: Lifecycle
    ) : BioAuthnCallback(), HuaweiFaceIdContinueable, LifecycleEventObserver {

        private val transitionFlow = MutableStateFlow<TransitionState>(TransitionState.Default)
        private var internalFaceIDState : FaceIDState? = null

        init {
            lifecycle.addObserver(this)
            continuation.invokeOnCancellation { //cleanup face recognition and UI state
                uiDuringFaceIDFlow.update { FaceIDUIState.None }
                cancellationSignal.cancel()
                continueWith(FaceIDState.Final)
            }
        }
        override fun continueWith(state: FaceIDState) {
            Timber.d("Continue with state ${state.javaClass.simpleName} on Main Thread = ${Looper.getMainLooper().isCurrentThread}")
            if(internalFaceIDState == FaceIDState.Final) {
                return
            }

            internalFaceIDState = state
            when(state) {
                FaceIDState.Cancel -> {
                    uiDuringFaceIDFlow.update { FaceIDUIState.None }
                    continuation.resumeIfPossible(FaceIDHandlerResult.CANCELLED)
                    continueWith(FaceIDState.Final)
                }

                FaceIDState.Recognised -> {
                    transitionFlow.update { TransitionState.ToSuccess }
                }

                FaceIDState.Recognising -> {
                    uiDuringFaceIDFlow.update {
                        FaceIDUIState.Recognising(this)
                    }
                    faceManager.auth(null, cancellationSignal, 0, this, null)
                }

                FaceIDState.SuccessTransitionEnd -> {
                    uiDuringFaceIDFlow.update { FaceIDUIState.None }
                    continuation.resumeIfPossible(FaceIDHandlerResult.SUCCESS)
                    continueWith(FaceIDState.Final)
                }

                FaceIDState.UseDefaultBiometric -> {
                    uiDuringFaceIDFlow.update { FaceIDUIState.None }
                    continuation.resumeIfPossible(FaceIDHandlerResult.FALLBACK_TO_DEFAULT)
                    continueWith(FaceIDState.Final)
                }

                is FaceIDState.NotRecognised -> {
                    uiDuringFaceIDFlow.update {
                        FaceIDUIState.NotRecognised(this, false,
                            defaultBiometricHandler != null)
                    }
                }

                FaceIDState.Failed -> {
                    uiDuringFaceIDFlow.update {
                        FaceIDUIState.NotRecognised(this, true,
                            defaultBiometricHandler != null)
                    }
                }

                FaceIDState.Final -> {
                    //do nothing
                }
            }
        }

        override fun getUITransitionFlow(): StateFlow<TransitionState> {
            return transitionFlow
        }

        override fun onAuthError(errMsgId: Int, errString: CharSequence) {
            Timber.e("Face Authentication error errorCode=$errMsgId,errorMessage=$errString")

            when(errMsgId) {
                FACE_ERROR_CANCELED -> {
                    continueWith(FaceIDState.Cancel)
                }
                FACE_ERROR_TIMEOUT, FACE_ERROR_HW_UNAVAILABLE -> {
                    continueWith(FaceIDState.NotRecognised)
                }
                else -> {
                    continueWith(FaceIDState.Failed)
                }
            }
        }

        override fun onAuthHelp(helpMsgId: Int, helpString: CharSequence) {
            Timber.d("Face Authentication help helpMsgId=$helpMsgId, helpString=$helpString")
        }

        override fun onAuthSucceeded(result: BioAuthnResult) {
            Timber.d("Face Authentication succeeded!")
            continueWith(FaceIDState.Recognised)
        }

        override fun onAuthFailed() {
            Timber.e("Face Authentication failed.")
            continueWith(FaceIDState.Failed)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if(event == Lifecycle.Event.ON_PAUSE) {
                source.lifecycle.removeObserver(this) //whole flow dies after onPause
                when(internalFaceIDState) {
                    FaceIDState.Recognising -> {
                        Timber.d("On Pause detected while Recognising state, cancel whole flow.")
                        cancellationSignal.cancel()
                    }
                    FaceIDState.NotRecognised -> {
                        Timber.d("On Pause detected while NotRecognised state, cancel whole flow.")
                        continueWith(FaceIDState.Cancel)
                    }
                    else -> {

                    }
                }
            }
        }
    }

    override fun toString(): String {
        return "Huawei FaceID Handler, with default handler as fallback = ${defaultBiometricHandler != null}"
    }
}