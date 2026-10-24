package com.ferbotz.momentic

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WelcomeFlagTest {

    @Test
    fun aNewInstallHasNotSeenTheWelcome() {
        assertFalse(AppPreferences(InMemorySettings()).hasSeenWelcome)
    }

    @Test
    fun onceSeenItStaysSeen() {
        val settings = InMemorySettings()
        AppPreferences(settings).markWelcomeSeen()
        assertTrue(AppPreferences(settings).hasSeenWelcome, "a later launch reads the same store")
    }

    @Test
    fun signingOutDoesNotShowTheWelcomeAgain() {
        val prefs = AppPreferences(InMemorySettings()).apply {
            saveAuthToken("jwt")
            markWelcomeSeen()
        }
        prefs.clearSession()
        assertFalse(prefs.isSignedIn)
        assertTrue(prefs.hasSeenWelcome)
    }
}
