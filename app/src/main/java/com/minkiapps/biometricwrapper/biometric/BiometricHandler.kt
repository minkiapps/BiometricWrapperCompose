package com.minkiapps.biometricwrapper.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.minkiapps.biometricwrapper.biometric.BiometricHandlerResult.NoHandler.Reason
import com.minkiapps.biometricwrapper.biometric.handler.BiometricHandlerImpl
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIDHandler
import com.minkiapps.biometricwrapper.util.isHMSAvailable

const val HUAWEI_FACE_ID_CANCELLED = 10001
sealed class BiometricHandlerResult {
    object Initialising : BiometricHandlerResult()
    class HasHandler(val biometricHandler: BiometricHandler) : BiometricHandlerResult()
    class NoHandler(val reason : Reason) : BiometricHandlerResult() {
        enum class Reason {
            NONE_ENROLLED, //no biometric enrolled in settings
            CANNOT_USED //either no hardware, or OS not sufficient
        }
    }
}

sealed class BiometricResult {
    object Success : BiometricResult() {
        override fun toString(): String {
            return "Success"
        }
    }

    class Cancelled(val errorCode : Int, val errorMessage : CharSequence = "") : BiometricResult() {
        override fun toString(): String {
            return "Cancelled, ErrorCode: $errorCode, ErrorMessage: $errorMessage"
        }
    }
    class Failed(val errorCode : Int,
                 val humanReadableErrorMessage : CharSequence) : BiometricResult() {
        override fun toString(): String {
            return "Failed, ErrorCode: $errorCode, ErrorMessage: $humanReadableErrorMessage"
        }
    }
}


interface BiometricHandler {
    suspend fun showBiometricPrompt(uiModel : BiometricUIModel) : BiometricResult
}

data class BiometricUIModel(val title : String,
                            val description : String)

private fun getBiometricHandlerResult(activity: FragmentActivity) : BiometricHandlerResult {
    return if(activity.isHMSAvailable()) {
        getHuaweiBiometricHandlerResult(activity)
    } else {
        getDefaultBiometricHandlerResult(activity)
    }
}

private fun getDefaultBiometricHandlerResult(activity: FragmentActivity): BiometricHandlerResult {
    val defaultHandler = BiometricHandlerImpl(activity)
    return if (defaultHandler.canBeUsed()) {
        BiometricHandlerResult.HasHandler(defaultHandler)
    } else {
        BiometricHandlerResult.NoHandler(
            if (defaultHandler.nonEnrolled()) Reason.NONE_ENROLLED
            else Reason.CANNOT_USED
        )
    }
}

private fun getHuaweiBiometricHandlerResult(activity: FragmentActivity) : BiometricHandlerResult {
    val defaultHandler = BiometricHandlerImpl(activity)

    val faceHandler = HuaweiFaceIDHandler(activity,
        if(defaultHandler.canBeUsed()) defaultHandler
        else null
    )

    return if(faceHandler.canBeUsed()) {
        BiometricHandlerResult.HasHandler(faceHandler)
    } else if(defaultHandler.canBeUsed()){
        BiometricHandlerResult.HasHandler(defaultHandler)
    } else {
        BiometricHandlerResult.NoHandler(
            if (faceHandler.nonEnrolled() || defaultHandler.nonEnrolled()) Reason.NONE_ENROLLED
            else Reason.CANNOT_USED
        )
    }
}

@Composable
fun biometricHandlerLifeCycleAware(activity: FragmentActivity): BiometricHandlerResult {
    var handlerResult : BiometricHandlerResult by remember {
        mutableStateOf(BiometricHandlerResult.Initialising)
    }
    val currentLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        currentLifecycleOwner
            .repeatOnLifecycle(Lifecycle.State.RESUMED) {
                handlerResult = getBiometricHandlerResult(activity)
            }
    }
    return handlerResult
}
