package com.tms.os.launcher.data

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/**
 * "Touch ID" for the launcher: the system's own fingerprint sheet, with the
 * device PIN, pattern or password as the alternative. Nothing is stored or
 * checked by the launcher itself – the system answers yes or no.
 */
object DeviceAuth {

    /** False when the phone has no screen lock at all: then there is nothing to secure with. */
    fun available(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true

    fun authenticate(activity: Activity, title: String, subtitle: String?, onResult: (Boolean) -> Unit) {
        if (!available(activity)) { onResult(false); return }
        val builder = BiometricPrompt.Builder(activity).setTitle(title)
        if (!subtitle.isNullOrBlank()) builder.setSubtitle(subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        var answered = false
        fun answer(ok: Boolean) { if (!answered) { answered = true; onResult(ok) } }
        runCatching {
            builder.build().authenticate(CancellationSignal(), activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = answer(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = answer(false)
                // A single unread finger is not the end: the sheet stays open for another try.
                override fun onAuthenticationFailed() = Unit
            })
        }.onFailure { answer(false) }
    }
}
