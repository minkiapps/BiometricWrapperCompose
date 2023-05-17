package com.minkiapps.biometricwrapper

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minkiapps.biometricwrapper.biometric.BiometricResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel : ViewModel() {

    private val logs = mutableStateListOf<BiometricLog>()

    private val _fireBiometricFlow = MutableStateFlow(false)
    private val _biometricResultLog = MutableStateFlow(logs)

    val state = combine(_fireBiometricFlow, _biometricResultLog) { fireBiometric, logs ->
        ScreenState(fireBiometric, logs)
    }.stateIn(viewModelScope, SharingStarted.Lazily, ScreenState())

    private var autoLaunchJob : Job = viewModelScope.launch {
        delay(3000)

        logs.add(BiometricLog(message = "Auto fire biometric 3 seconds after activity launch"))
        _fireBiometricFlow.update { true }
    }

    fun onViewEvent(viewEvent: ViewEvent) {
        when(viewEvent) {
            is ViewEvent.OnBiometricResult -> {
                Timber.d("Received Biometric Result: ${viewEvent.biometricResult}")
                logs.add(BiometricLog(message = "On biometric result received: ${viewEvent.biometricResult}"))
                _fireBiometricFlow.update { false }
            }
            ViewEvent.OnLaunchBiometricButtonClicked -> {
                logs.add(BiometricLog(message = "On launch biometric button clicked"))
                if(autoLaunchJob.isActive) {
                    logs.add(BiometricLog(message = "Cancel auto fire biometric job"))
                    autoLaunchJob.cancel()
                }
                _fireBiometricFlow.update { true }
            }

            ViewEvent.OnClearLogsClicked -> {
                logs.clear()
            }

            ViewEvent.ResetFireBiometricState -> {
                _fireBiometricFlow.update { false }
            }
        }
    }
}

data class BiometricLog(val timeStamp : Long = System.currentTimeMillis(), val message : String)

sealed class ViewEvent {
    object OnLaunchBiometricButtonClicked : ViewEvent()
    object OnClearLogsClicked : ViewEvent()
    class OnBiometricResult(val biometricResult: BiometricResult) : ViewEvent()
    object ResetFireBiometricState : ViewEvent()
}

data class ScreenState(
    val fireBiometric : Boolean = false,
    val logs: List<BiometricLog> = listOf()
)
