package com.whatscheck.engines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsTest {

    @Test
    fun defaultDurationIs8Seconds() {
        assertEquals(8, Prefs.DEFAULT_OVERLAY_DURATION_SECONDS)
    }

    @Test
    fun allowedDurationsContainExpectedValues() {
        assertEquals(listOf(5, 8, 12), Prefs.ALLOWED_OVERLAY_DURATIONS)
    }

    @Test
    fun sanitizeOverlayDurationAcceptsAllowedValues() {
        assertEquals(5, Prefs.sanitizeOverlayDuration(5))
        assertEquals(8, Prefs.sanitizeOverlayDuration(8))
        assertEquals(12, Prefs.sanitizeOverlayDuration(12))
    }

    @Test
    fun sanitizeOverlayDurationFallsBackToDefaultOnInvalidValues() {
        assertEquals(8, Prefs.sanitizeOverlayDuration(0))
        assertEquals(8, Prefs.sanitizeOverlayDuration(-5))
        assertEquals(8, Prefs.sanitizeOverlayDuration(7))
        assertEquals(8, Prefs.sanitizeOverlayDuration(45))
        assertEquals(8, Prefs.sanitizeOverlayDuration(99))
    }
}
