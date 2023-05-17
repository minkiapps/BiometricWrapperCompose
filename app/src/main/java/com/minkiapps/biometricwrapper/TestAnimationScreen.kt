package com.minkiapps.biometricwrapper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.minkiapps.biometricwrapper.biometric.AnimState

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

@Preview
@Composable
fun TestScreenPreview() {
    TestScreen()
}