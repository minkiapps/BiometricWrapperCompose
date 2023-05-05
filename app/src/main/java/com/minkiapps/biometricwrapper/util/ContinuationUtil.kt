package com.minkiapps.biometricwrapper.util

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun <T>CancellableContinuation<T>.resumeIfPossible(value : T) {
    if(!isCancelled && isActive)
        resume(value)
}

fun <T>CancellableContinuation<T>.resumeIfPossible(value : Throwable) {
    if(!isCancelled && isActive)
        resumeWithException(value)
}