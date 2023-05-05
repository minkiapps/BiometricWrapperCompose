package com.minkiapps.biometricwrapper.biometric

import androidx.fragment.app.FragmentActivity
import com.minkiapps.biometricwrapper.biometric.handler.BiometricHandlerImpl
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIDHandler
import com.minkiapps.biometricwrapper.util.isHMSAvailable

interface BiometricHandler {
    fun canBeUsed() : Boolean
    suspend fun showBiometricPrompt(uiModel : BiometricUIModel) : Boolean
}

data class BiometricUIModel(val title : String,
                            val description : String)

fun getBiometricHandler(activity: FragmentActivity) : com.minkiapps.biometricwrapper.biometric.BiometricHandler? {
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
