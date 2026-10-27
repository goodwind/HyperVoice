package io.github.goodwind.hypervoice

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings as SystemSettings
import io.github.goodwind.hypervoice.core.AccessibilityList
import io.github.goodwind.hypervoice.core.SetupStatus

/** What the TV needs for the app, read and (given WRITE_SECURE_SETTINGS at setup) put right without a computer. */
object Repair {
    private const val RECOGNIZER = "voice_recognition_service"

    fun snapshot(context: Context, settings: Settings): SetupStatus.Snapshot {
        val resolver = context.contentResolver
        val enabled = SystemSettings.Secure.getString(resolver, SystemSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val recognizer = SystemSettings.Secure.getString(resolver, RECOGNIZER).orEmpty()
        return SetupStatus.Snapshot(
            audioPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
            accessibilityOn = AccessibilityList.has(enabled, context.packageName),
            recognizerIsOurs = recognizer.startsWith("${context.packageName}/"),
            hasKey = settings.hasKey,
            remoteMicFound = RemoteMic.present(context),
            canRepair = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED,
        )
    }

    /** Puts right the [items]; false if the TV refused. */
    fun apply(context: Context, items: List<SetupStatus.Item>): Boolean = try {
        val resolver = context.contentResolver
        if (SetupStatus.Item.Accessibility in items) {
            val service = ComponentName(context, HyperVoiceAccessibilityService::class.java).flattenToShortString()
            val current = SystemSettings.Secure.getString(resolver, SystemSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            SystemSettings.Secure.putString(resolver, SystemSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES, AccessibilityList.with(current, service))
            SystemSettings.Secure.putInt(resolver, SystemSettings.Secure.ACCESSIBILITY_ENABLED, 1)
        }
        if (SetupStatus.Item.Recognizer in items) {
            val service = ComponentName(context, HyperVoiceRecognitionService::class.java).flattenToShortString()
            SystemSettings.Secure.putString(resolver, RECOGNIZER, service)
        }
        true
    } catch (e: SecurityException) {
        false
    }
}
