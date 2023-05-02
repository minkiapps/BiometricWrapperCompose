package com.minkiapps.biometricwrapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _fireBiometricFlow = MutableStateFlow(false)

    val fireBiometricFlow : StateFlow<Boolean> = _fireBiometricFlow

    private var autoLaunchJob : Job = viewModelScope.launch {
        delay(3000)
        _fireBiometricFlow.update { true }
    }

    fun onViewEvent(viewEvent: ViewEvent) {
        when(viewEvent) {
            is ViewEvent.OnBiometricResult -> {
                _fireBiometricFlow.update { false }
            }
            ViewEvent.OnLaunchBiometricButtonClicked -> {
                autoLaunchJob.cancel()
                _fireBiometricFlow.update { true }
            }
        }
    }
}

sealed class ViewEvent {
    object OnLaunchBiometricButtonClicked : ViewEvent()
    class OnBiometricResult(val success : Boolean) : ViewEvent()
}