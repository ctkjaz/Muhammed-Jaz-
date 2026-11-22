package com.example

import com.example.data.model.AccountSessionLogic
import com.example.data.model.SessionCheckResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSessionLogicTest {

    @Test
    fun `evaluateRemoteSession handles claim valid and revoked states`() {
        assertEquals(
            SessionCheckResult.ClaimNeeded,
            AccountSessionLogic.evaluateRemoteSession("sess-A", "", "Pixel 8")
        )
        assertEquals(
            SessionCheckResult.ClaimNeeded,
            AccountSessionLogic.evaluateRemoteSession("sess-A", null, null)
        )
        assertEquals(
            SessionCheckResult.Valid,
            AccountSessionLogic.evaluateRemoteSession("sess-A", "sess-A", "Pixel 8")
        )
        val revoked = AccountSessionLogic.evaluateRemoteSession("sess-A", "sess-B", "OnePlus CPH2447")
        assertTrue(revoked is SessionCheckResult.Revoked)
        assertEquals("OnePlus CPH2447", (revoked as SessionCheckResult.Revoked).activeDeviceName)
        assertTrue(
            AccountSessionLogic.buildForcedSignOutMessage(revoked.activeDeviceName)
                .contains("OnePlus CPH2447")
        )
        assertTrue(
            AccountSessionLogic.buildForcedSignOutMessage(null)
                .contains("another phone")
        )
    }

    @Test
    fun `buildDeviceLabel formats manufacturer and model cleanly`() {
        assertEquals("Samsung SM-S918B", AccountSessionLogic.buildDeviceLabel("samsung", "SM-S918B"))
        assertEquals("Google Pixel 8", AccountSessionLogic.buildDeviceLabel("google", "Pixel 8"))
        assertEquals("Samsung Galaxy S24", AccountSessionLogic.buildDeviceLabel("samsung", "Samsung Galaxy S24"))
        assertEquals("Android Phone", AccountSessionLogic.buildDeviceLabel("", ""))
    }

    @Test
    fun `phone number sanitization and optional validation handle edge cases`() {
        assertTrue(AccountSessionLogic.isPhoneNumberValidOrEmpty(""))
        assertTrue(AccountSessionLogic.isPhoneNumberValidOrEmpty("   "))
        assertTrue(AccountSessionLogic.isPhoneNumberValidOrEmpty("+91 98765 43210"))
        assertTrue(AccountSessionLogic.isPhoneNumberValidOrEmpty("9876543210"))
        assertFalse(AccountSessionLogic.isPhoneNumberValidOrEmpty("12345"))
        assertEquals("+91 98765-43210", AccountSessionLogic.sanitizePhoneNumber("  +91 98765-43210abc  "))
    }
}
