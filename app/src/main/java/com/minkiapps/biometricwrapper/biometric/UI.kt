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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieAnimatable
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
import timber.log.Timber

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
                modifier = Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.face_id_failed)
                )
                val imageSize = 100.dp
                LottieAnimation(composition,
                    modifier = Modifier
                        .width(imageSize)
                        .height(imageSize)
                )

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
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.LightGray)
                )
                Text(
                    text = "Use Fingerprint",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true)
                        ) {
                            continuable.continueWith(ContinueState.UseDefaultBiometric)
                        }
                        .padding(12.dp)
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
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun FaceIDRecognisingDialog(continuable: HuaweiFaceIdContinueable) {
    Dialog(
        onDismissRequest = {
            continuable.continueWith(ContinueState.Cancel)
        },
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
                    bottom = 8.dp,
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val tState by continuable
                    .getUITransitionFlow()
                    .collectAsStateWithLifecycle()

                Timber.d("NEW tState: ${tState.javaClass.simpleName}")

                FaceIDAnimationWithChangingClipSpecImpl(tState) {
                    continuable.continueWith(ContinueState.SuccessTransitionEnd)
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

@Composable
fun FaceIDAnimationWithChangingClipSpecImpl(tState : TransitionState, animationEnd : () -> Unit) {
    Timber.d("Calling FaceIDAnimationWithChangingClipSpecImpl")
    val imageSize = 100.dp

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.face_id_success)
    )

    val endFrameForInfiniteAnimation = 120
    var clipSpec by remember {
        mutableStateOf<LottieClipSpec>(LottieClipSpec.Frame(0, endFrameForInfiniteAnimation))
    }
    var iteration by remember {
        mutableStateOf(LottieConstants.IterateForever)
    }
    val animState = animateLottieCompositionAsState(
        composition = composition,
        clipSpec = clipSpec,
        iterations = iteration
    )

    LottieAnimation(
        composition,
        progress = { animState.progress },
        modifier = Modifier
            .width(imageSize)
            .height(imageSize)
    )

    LaunchedEffect(tState) {
        if(tState is TransitionState.ToSuccess) {
            Timber.d("SUCCESS TRANSITION! Changing Clipspec")
            val frame = composition?.getFrameForProgress(animState.progress)?.toInt()
            clipSpec = LottieClipSpec.Frame(frame, null)
            iteration = 1
        }
    }

    if(tState == TransitionState.ToSuccess) {
        val end by remember {
            derivedStateOf {
                animState.progress >= 1f
            }
        }

        if(end) {
            animationEnd()
        }
    }
}

@Composable
fun FaceIDAnimationWithSwapAnimationImpl(tState : TransitionState, animationEnd : () -> Unit) {
    Timber.d("Calling FaceIDAnimationWithSwapAnimationImpl")
    val imageSize = 100.dp

    val recComp = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.face_id_recognising)
    )

    val successComp = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.face_id_success)
    )

    val animState = remember {
        mutableStateOf(
            AnimState(recComp, iterations = LottieConstants.IterateForever)
        )
    }

    val animatable = rememberLottieAnimatable()
    LaunchedEffect(animState.value.compositionResult.value) {
        Timber.d("LAUNCH ANIMATION due to compositionResult.value change.")
        animatable.animate(
            animState.value.compositionResult.value,
            iterations = animState.value.iterations,
            clipSpec = animState.value.clipSpec
        )
    }


    LottieAnimation(
        composition = animatable.composition,
        progress = { animatable.progress },
        modifier = Modifier
            .width(imageSize)
            .height(imageSize)
    )

    LaunchedEffect(tState) {
        if(tState == TransitionState.ToSuccess) {
            Timber.d("SUCCESS TRANSITION! Swap animation with intial frame.")
            val currentFrame =
                recComp.value?.getFrameForProgress(animatable.progress)?.toInt()
            Timber.d("CURRENT FRAME: $currentFrame")
            val clipSpec = LottieClipSpec.Frame(
                min = currentFrame
            )

            animState.value = AnimState(
                successComp,
                clipSpec,
                1
            )
        }
    }

    if(tState == TransitionState.ToSuccess) {
        val end by remember {
            derivedStateOf {
                animatable.progress >= 1f
            }
        }

        if(end) {
            animationEnd()
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
                                return MutableStateFlow(TransitionState.ToSuccess)
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