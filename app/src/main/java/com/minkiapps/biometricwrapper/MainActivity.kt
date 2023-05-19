package com.minkiapps.biometricwrapper

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minkiapps.biometricwrapper.biometric.BiometricHandler
import com.minkiapps.biometricwrapper.biometric.BiometricHandlerResult
import com.minkiapps.biometricwrapper.biometric.BiometricResult
import com.minkiapps.biometricwrapper.biometric.BiometricUI
import com.minkiapps.biometricwrapper.biometric.BiometricUIModel
import com.minkiapps.biometricwrapper.biometric.biometricHandlerLifeCycleAware
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdContinueable
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.FaceIDUIState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.TransitionState
import com.minkiapps.biometricwrapper.ui.theme.BiometricwrapperTheme
import com.minkiapps.biometricwrapper.util.formatLogTime
import com.minkiapps.biometricwrapper.util.isHMSAvailable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

class MainActivity : FragmentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isHMSAvailable = isHMSAvailable()

        setContent {
            val screenState by vm.state.collectAsStateWithLifecycle()

            BiometricwrapperTheme {
                Screen(
                    isHMSAvailable,
                    screenState,
                    biometricHandlerLifeCycleAware(this),
                    { event -> vm.onViewEvent(event) }, {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onViewEvent(ViewEvent.ResetFireBiometricState)
    }
}

@Composable
fun Screen(
    isHMSAvailable: Boolean,
    screenState: ScreenState,
    handlerResult: BiometricHandlerResult,
    onViewEvent: (ViewEvent) -> Unit,
    gotoSystemSettings: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    val (showSnackBar, setShowSnackBar) = remember {
        mutableStateOf(ShowSnackBarState())
    }

    LaunchedEffect(snackBarHostState, showSnackBar) {
        if (showSnackBar.show) {
            snackBarHostState.showSnackbar(
                message = showSnackBar.text,
            )
            setShowSnackBar(ShowSnackBarState())
        }
    }

    if(handlerResult is BiometricHandlerResult.HasHandler) {
        BiometricUI(handlerResult.biometricHandler)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { pV ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pV)
                .padding(start = 8.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "HMS available:",
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (isHMSAvailable) {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_check_24),
                        contentDescription = "Available",
                        colorFilter = ColorFilter.tint(Color.Green)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_close_24),
                        contentDescription = "Unavailable",
                        colorFilter = ColorFilter.tint(Color.Red)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Has Biometric:",
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (handlerResult is BiometricHandlerResult.HasHandler) {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_check_24),
                        contentDescription = "Available",
                        colorFilter = ColorFilter.tint(Color.Green)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_close_24),
                        contentDescription = "Unavailable",
                        colorFilter = ColorFilter.tint(Color.Red)
                    )
                }
            }

            when(handlerResult) {
                is BiometricHandlerResult.HasHandler -> {
                    Text(text = handlerResult.biometricHandler.toString())
                }
                BiometricHandlerResult.Initialising -> { }
                is BiometricHandlerResult.NoHandler -> {
                    Text(text = "Why no Biometric reason: ${handlerResult.reason}")
                }
            }

            Button(onClick = {
                gotoSystemSettings.invoke()
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "Settings")
            }

            if (handlerResult is BiometricHandlerResult.HasHandler) {
                val scope = rememberCoroutineScope()
                fun showBiometric() {
                    scope.launch {
                        val result = handlerResult.biometricHandler.showBiometricPrompt(
                            BiometricUIModel(
                                "Biometric ID",
                                "Identify yourself using Biometric"
                            )
                        )

                        val snackBarText = when(result) {
                            is BiometricResult.Cancelled -> "Biometric identification cancelled!"
                            is BiometricResult.Failed -> "Biometric identification failed!"
                            BiometricResult.Success -> "Biometric identification successful!"
                        }

                        setShowSnackBar(
                            ShowSnackBarState(show = true, text = snackBarText)
                        )

                        onViewEvent.invoke(ViewEvent.OnBiometricResult(result))
                    }
                }

                LaunchedEffect(screenState.fireBiometric) {
                    Timber
                    if (screenState.fireBiometric) {
                        Timber.d("FIRE BIOMETRIC!")
                        showBiometric()
                    }
                }

                Row {
                    Button(
                        onClick = {
                            onViewEvent.invoke(ViewEvent.OnLaunchBiometricButtonClicked)
                        }, modifier = Modifier.padding(top = 8.dp, end = 8.dp)
                    ) {
                        Text(text = "Biometric Auth")
                    }

                    Button(onClick = {
                        onViewEvent.invoke(ViewEvent.OnClearLogsClicked)
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(text = "Clear Log")
                    }
                }

                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 8.dp)) {
                        items(screenState.logs) { l ->
                            Text(
                            text = "${l.timeStamp.formatLogTime()}: ${l.message}",
                            style = MaterialTheme.typography.bodySmall
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DefaultPreview() {
    val toSuccessContinueable = object : HuaweiFaceIdContinueable {
        val tFlow = MutableStateFlow<TransitionState>(TransitionState.Default)
        override fun getUITransitionFlow(): StateFlow<TransitionState> {
            return tFlow
        }

        override fun continueWith(state: HuaweiFaceIdContinueable.FaceIDState) {

        }
    }
    BiometricwrapperTheme {
        var fireBiometric by remember {
            mutableStateOf(false)
        }
        Screen(isHMSAvailable = true,
            ScreenState(fireBiometric = false, logs = listOf(
                BiometricLog(System.currentTimeMillis(), "Fire Biometric"),
                BiometricLog(System.currentTimeMillis() + 200, "Biometric failed")
            )), BiometricHandlerResult.HasHandler(object : BiometricHandler, HuaweiFaceIdUIStateable {
                private val flow = MutableStateFlow<FaceIDUIState>(FaceIDUIState.None)

                override fun toString(): String {
                    return "Test Huawei FaceID Handler"
                }

                override suspend fun showBiometricPrompt(uiModel: BiometricUIModel): BiometricResult {
                    toSuccessContinueable.tFlow.update { TransitionState.Default }
                    flow.update { FaceIDUIState.Recognising(toSuccessContinueable) }
                    delay(500 + Random.nextLong(2000))
                    toSuccessContinueable.tFlow.update { TransitionState.ToSuccess }
                    delay(1500)
                    flow.update { FaceIDUIState.None }
                    return BiometricResult.Success
                }

                override fun getShowUIDuringFaceIDFlow(): StateFlow<FaceIDUIState> {
                    return flow
                }
            }),{ e ->
                fireBiometric = e == ViewEvent.OnLaunchBiometricButtonClicked
            }, {

            })
    }
}

data class ShowSnackBarState(
    val show: Boolean = false,
    val text: String = ""
)