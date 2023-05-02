package com.minkiapps.biometricwrapper.biometric

import androidx.fragment.app.FragmentActivity
import com.minkiapps.biometricwrapper.biometric.handler.BiometricXHandler
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIDHandler
import com.minkiapps.biometricwrapper.util.isHMSAvailable

interface BiometricHandler {
    fun canBeUsed() : Boolean
    suspend fun showBiometricPrompt(uiModel : BiometricUIModel) : Boolean
}

data class BiometricUIModel(val title : String,
                            val description : String)

fun getBiometricHandler(activity: FragmentActivity) : BiometricHandler? {
    if(activity.isHMSAvailable()) {
        val handler = HuaweiFaceIDHandler(activity)
        if(handler.canBeUsed()) {
            return handler
        }
    }

    val handler = BiometricXHandler(activity)
    if(handler.canBeUsed()) {
        return handler
    }

    return null
}
