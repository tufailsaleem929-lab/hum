package com.example.security

import android.content.Context
import android.content.SharedPreferences

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("noteflow_security_prefs", Context.MODE_PRIVATE)

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()

    var appLockPin: String?
        get() = prefs.getString("app_lock_pin", null)
        set(value) = prefs.edit().putString("app_lock_pin", value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var isAutoLockImmediate: Boolean
        get() = prefs.getBoolean("auto_lock_immediate", true)
        set(value) = prefs.edit().putBoolean("auto_lock_immediate", value).apply()

    fun verifyPin(enteredPin: String): Boolean {
        val stored = appLockPin ?: return true
        return stored == enteredPin
    }

    fun setAppPin(pin: String) {
        appLockPin = pin
        isAppLockEnabled = true
    }

    fun disableAppPin() {
        appLockPin = null
        isAppLockEnabled = false
    }

    // Per-note unlock cache for active session
    private val unlockedNoteIds = mutableSetOf<Long>()

    fun isNoteUnlocked(noteId: Long): Boolean {
        return unlockedNoteIds.contains(noteId)
    }

    fun unlockNoteForSession(noteId: Long) {
        unlockedNoteIds.add(noteId)
    }

    fun lockNoteForSession(noteId: Long) {
        unlockedNoteIds.remove(noteId)
    }
}
