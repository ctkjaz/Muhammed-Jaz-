package com.example

import com.example.data.local.ProfileAvatarStorage
import com.example.ui.viewmodel.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAvatarStorageTest {

    @Test
    fun `computeSampleSize downsamples high-res camera photos without over-shrinking`() {
        assertEquals(1, ProfileAvatarStorage.computeSampleSize(800, 600))
        assertEquals(2, ProfileAvatarStorage.computeSampleSize(1600, 1200))
        assertEquals(4, ProfileAvatarStorage.computeSampleSize(4000, 3000))
        assertEquals(1, ProfileAvatarStorage.computeSampleSize(0, 0))
    }

    @Test
    fun `computeSquareCropRect center-crops portrait and landscape images accurately`() {
        assertEquals(Triple(0, 200, 800), ProfileAvatarStorage.computeSquareCropRect(800, 1200))
        assertEquals(Triple(300, 0, 600), ProfileAvatarStorage.computeSquareCropRect(1200, 600))
        assertEquals(Triple(0, 0, 512), ProfileAvatarStorage.computeSquareCropRect(512, 512))
        assertEquals(Triple(0, 0, 0), ProfileAvatarStorage.computeSquareCropRect(0, 400))
    }

    @Test
    fun `bottom navigation tab labels are concise single-word strings`() {
        assertEquals("CBT", MainTab.CBT.label)
        MainTab.entries.forEach { tab ->
            assertFalse("Tab ${tab.name} label should not contain whitespace", tab.label.contains(" "))
            assertTrue("Tab ${tab.name} label should stay concise", tab.label.length <= 10)
        }
    }
}
