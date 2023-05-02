package com.minkiapps.biometricwrapper.biometric

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.minkiapps.biometricwrapper.R
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdContinueable
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdContinueable.ContinueState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.FaceIDUIState
import com.minkiapps.biometricwrapper.biometric.handler.HuaweiFaceIdUIStateable.TransitionState
import com.minkiapps.biometricwrapper.ui.theme.Black_olive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BiometricUI(biometricHandler: BiometricHandler?) {
    biometricHandler?.let { h ->
        when (h) {
            is HuaweiFaceIdUIStateable -> HuaweiFaceIdBiometricHandler(h)
            else -> {
                //system provides biometric UI
            }
        }
    }
}

@Composable
fun HuaweiFaceIdBiometricHandler(faceIDUI: HuaweiFaceIdUIStateable) {
    val faceUIState by faceIDUI.getShowUIDuringFaceIDFlow().collectAsStateWithLifecycle()

    when (faceUIState) {
        FaceIDUIState.None -> {}
        is FaceIDUIState.AskToRetry -> {
            val continuation = (faceUIState as FaceIDUIState.AskToRetry).faceIDContinuation
            FaceIDRetryDialog(continuation)
        }

        is FaceIDUIState.Recognising -> {
            val continuation = (faceUIState as FaceIDUIState.Recognising).faceIDContinuation
            FaceIDRecognisingDialog(continuation)
        }
    }
}

@Composable
fun FaceIDRetryDialog(continuable: HuaweiFaceIdContinueable) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .wrapContentHeight()
                .width(200.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Black_olive)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Face Not Recognised",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.LightGray)
                )
                Text(
                    text = "Try Face ID Again",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true)
                        ) {
                            continuable.continueWith(ContinueState.Retry)
                        }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.LightGray)
                )
                Text(
                    stringResource(id = android.R.string.cancel),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true)
                        ) {
                            continuable.continueWith(ContinueState.Cancel)
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun FaceIDRecognisingDialog(continuable: HuaweiFaceIdContinueable) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Black_olive)
        ) {
            Column(
                modifier = Modifier.padding(
                    bottom = 8.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val tState by continuable
                    .getUITransitionFlow()
                    .collectAsStateWithLifecycle()

                val imageSize = 100.dp
                when (tState) {
                    is TransitionState.Default -> {
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(
                                R.raw.face_id
                            )
                        )
                        val progress by animateLottieCompositionAsState(
                            composition = composition,
                            iterations = LottieConstants.IterateForever
                        )

                        LottieAnimation(
                            composition,
                            modifier = Modifier
                                .width(imageSize)
                                .height(imageSize),
                            progress = { progress }
                        )
                    }

                    is TransitionState.ToFailed -> {
                        continuable.continueWith(ContinueState.FailedTransitionEnd)
                    }

                    is TransitionState.ToSuccess -> {
                        continuable.continueWith(ContinueState.SuccessTransitionEnd)
                    }
                }

                Text(
                    text = "Face ID",
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewHuaweiFaceIDUI() {
    MaterialTheme {
        HuaweiFaceIdBiometricHandler(object : HuaweiFaceIdUIStateable {
            override fun getShowUIDuringFaceIDFlow(): StateFlow<FaceIDUIState> {
                return MutableStateFlow(
                    FaceIDUIState.AskToRetry(
                        object : HuaweiFaceIdContinueable {
                            override fun getUITransitionFlow(): StateFlow<TransitionState> {
                                return MutableStateFlow(TransitionState.Default)
                            }

                            override fun continueWith(state: ContinueState) {

                            }
                        }
                    )
                )
            }
        })
    }
}