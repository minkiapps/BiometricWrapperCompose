package com.minkiapps.biometricwrapper.biometric

import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionResult

data class AnimState(
    val compositionResult: LottieCompositionResult,
    val clipSpec: LottieClipSpec? = null,
    val iterations: Int = 1,
)