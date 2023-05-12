package com.minkiapps.biometricwrapper

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.minkiapps.biometricwrapper.biometric.AnimState
import com.minkiapps.biometricwrapper.biometric.BiometricHandler
import com.minkiapps.biometricwrapper.biometric.BiometricUI
import com.minkiapps.biometricwrapper.biometric.BiometricUIModel
import com.minkiapps.biometricwrapper.biometric.biometricHandlerLifeCycleAware
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdContinueable
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.FaceIDUIState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.TransitionState
import com.minkiapps.biometricwrapper.ui.theme.BiometricwrapperTheme
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
            val fireBiometric by vm.fireBiometricFlow.collectAsStateWithLifecycle()

            BiometricwrapperTheme {
                Screen(
                    isHMSAvailable,
                    fireBiometric,
                    biometricHandlerLifeCycleAware(this)
                ) { event -> vm.onViewEvent(event) }
            }
        }
    }
}

@Composable
fun Screen(
    isHMSAvailable: Boolean,
    fireBiometric: Boolean,
    biometricHandler: BiometricHandler?,
    onViewEvent: (ViewEvent) -> Unit
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

    BiometricUI(biometricHandler = biometricHandler)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { pV ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pV)
        ) {
            Text(
                text = "Biometric Wrapper",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .fillMaxWidth()
            )

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
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Has Biometric:",
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (biometricHandler != null) {
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

            if (biometricHandler != null) {
                Text(text = biometricHandler.toString())
            }

            if (biometricHandler != null) {
                val scope = rememberCoroutineScope()
                fun showBiometric() {
                    scope.launch {
                        val identified = biometricHandler.showBiometricPrompt(
                            BiometricUIModel(
                                "Biometric ID",
                                "Identify yourself using Biometric"
                            )
                        )

                        if (identified) {
                            setShowSnackBar(
                                ShowSnackBarState(
                                    show = true,
                                    text = "Biometric identification successful!"
                                )
                            )
                        } else {
                            setShowSnackBar(
                                ShowSnackBarState(
                                    show = true,
                                    text = "Biometric identification failed."
                                )
                            )
                        }

                        onViewEvent.invoke(ViewEvent.OnBiometricResult(identified))
                    }
                }

                LaunchedEffect(fireBiometric) {
                    if (fireBiometric) {
                        Timber.d("FIRE BIOMETRIC!")
                        showBiometric()
                    }
                }

                Button(onClick = {
                    onViewEvent.invoke(ViewEvent.OnLaunchBiometricButtonClicked)
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = "Biometric Auth")
                }
            }
        }
    }
}

@Composable
fun TestScreen() {
    val animatable = rememberLottieAnimatable()
    val recognising =
        rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.face_id_recognising))
    val success =
        rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.face_id_success))

    val default = AnimState(recognising, iterations = LottieConstants.IterateForever)
    val state = remember {
        mutableStateOf(default)
    }

    LaunchedEffect(
        state.value.compositionResult.value
    ) {
        animatable.animate(
            state.value.compositionResult.value,
            iterations = state.value.iterations,
            clipSpec = state.value.clipSpec
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                state.value = default
            }) {
                Text(text = "Reset")
            }
            Button(onClick = {
                val startFrame =
                    recognising.value?.getFrameForProgress(animatable.progress)?.toInt()
                state.value = AnimState(
                    success,
                    LottieClipSpec.Frame(min = startFrame),
                    1
                )
            }) {
                Text(text = "Success")
            }
        }

        LottieAnimation(
            modifier = Modifier.size(100.dp),
            composition = animatable.composition,
            progress = { animatable.progress })
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
            fireBiometric, object : BiometricHandler, HuaweiFaceIdUIStateable {
                private val flow = MutableStateFlow<FaceIDUIState>(FaceIDUIState.None)

                override fun toString(): String {
                    return "Test Huawei FaceID Handler"
                }

                override suspend fun showBiometricPrompt(uiModel: BiometricUIModel): Boolean {
                    toSuccessContinueable.tFlow.update { TransitionState.Default }
                    flow.update { FaceIDUIState.Recognising(toSuccessContinueable) }
                    delay(500 + Random.nextLong(2000))
                    toSuccessContinueable.tFlow.update { TransitionState.ToSuccess }
                    delay(1500)
                    flow.update { FaceIDUIState.None }
                    return true
                }

                override fun getShowUIDuringFaceIDFlow(): StateFlow<FaceIDUIState> {
                    return flow
                }
            }) { e ->
            fireBiometric = e == ViewEvent.OnLaunchBiometricButtonClicked
        }
    }
}

@Preview
@Composable
fun TestScreenPreview() {
    TestScreen()
}

data class ShowSnackBarState(
    val show: Boolean = false,
    val text: String = ""
)