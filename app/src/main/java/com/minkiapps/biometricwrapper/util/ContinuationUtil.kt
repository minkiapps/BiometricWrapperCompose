package com.minkiapps.biometricwrapper.util

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

fun <T>CancellableContinuation<T>.resumeIfPossible(value : T) {
    if(!isCancelled && isActive)
        resume(value)
}