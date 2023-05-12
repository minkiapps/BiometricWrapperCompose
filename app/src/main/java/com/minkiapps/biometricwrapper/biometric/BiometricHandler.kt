package com.minkiapps.biometricwrapper.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.minkiapps.biometricwrapper.biometric.handler.BiometricHandlerImpl
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIDHandler
import com.minkiapps.biometricwrapper.util.isHMSAvailable

interface BiometricHandler {
    suspend fun showBiometricPrompt(uiModel : BiometricUIModel) : Boolean
}

data class BiometricUIModel(val title : String,
                            val description : String)

private fun getBiometricHandler(activity: FragmentActivity) : BiometricHandler? {
    val defaultHandler = BiometricHandlerImpl(activity)

    if(activity.isHMSAvailable()) {
        val handler = HuaweiFaceIDHandler(activity,
            if(defaultHandler.canBeUsed()) defaultHandler
            else null
        )
        if(handler.canBeUsed()) {
            return handler
        }
    }

    if(defaultHandler.canBeUsed()) {
        return defaultHandler
    }

    return null
}

@Composable
fun biometricHandlerLifeCycleAware(activity: FragmentActivity): BiometricHandler? {
    var handler : BiometricHandler? by remember {
        mutableStateOf(null)
    }
    val currentLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        currentLifecycleOwner
            .repeatOnLifecycle(Lifecycle.State.RESUMED) {
                handler = getBiometricHandler(activity)
            }
    }
    return handler
}
