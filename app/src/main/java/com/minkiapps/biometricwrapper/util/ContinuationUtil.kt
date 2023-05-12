package com.minkiapps.biometricwrapper.util

import kotlinx.coroutines.CancellableContinuation
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun <T>CancellableContinuation<T>.resumeIfPossible(value : T) {
    if(!isCancelled && isActive) {
        resume(value)
    } else {
        Timber.e("Tried to resume on continuation on a cancelled or not active state! Cancelled: $isCancelled IsActive: $isActive")
    }
}

fun <T>CancellableContinuation<T>.resumeIfPossible(value : Throwable) {
    if(!isCancelled && isActive)
        resumeWithException(value)
}