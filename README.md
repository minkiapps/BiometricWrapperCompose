# BiometricWrapperCompose
A wrapper of androidx.biometric and Huawei Face Manager API written in Compose and Coroutines

Since Huawei is not suppoting the Face Recognition in [androix.biometric API](https://developer.android.com/jetpack/androidx/releases/biometric), I wrote a custom UI in compose which uses [Huawei Fido Kit](https://developer.huawei.com/consumer/en/doc/development/Security-Guides/introduction-0000001051069988) for face recognition, 
when the device is not Huawei and has no face recognition, androix.biometric API is used for biometric authentication.

![ezgif com-resize](https://github.com/minkiapps/BiometricWrapperCompose/assets/52449229/fc86fbf1-d14a-41b6-b7b9-9a1f0b4a739b)
