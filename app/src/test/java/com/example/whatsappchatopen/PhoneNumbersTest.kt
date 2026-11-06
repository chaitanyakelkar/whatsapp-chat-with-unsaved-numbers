package com.example.whatsappchatopen

import com.example.whatsappchatopen.PhoneNumbers.Extracted
import com.example.whatsappchatopen.PhoneNumbers.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun pastedInternationalNumberSwitchesCountryAndDropsPrefix() {
        assertEquals(Extracted("IN", "9876543210"), PhoneNumbers.extract("+91 98765 43210", "US", strict = false))
        assertEquals(Extracted("GB", "7400123456"), PhoneNumbers.extract("+44 (0)7400 123456", "IN", strict = false))
        assertEquals(Extracted("DE", "15123456789"), PhoneNumbers.extract("0049 151 23456789", "IN", strict = false))
    }

    @Test
    fun sharedCallingCodeResolvesToExactCountry() {
        assertEquals("CA", PhoneNumbers.extract("+1 416 555 0123", "IN", strict = false)?.regionCode)
        assertEquals("US", PhoneNumbers.extract("+1 650 253 0000", "IN", strict = false)?.regionCode)
    }

    @Test
    fun countryCodeWithoutPlusIsRecognised() {
        assertEquals(Extracted("IN", "9876543210"), PhoneNumbers.extract("919876543210", "IN", strict = true))
    }

    @Test
    fun localNumberKeepsCurrentCountryAndStripsTrunkPrefix() {
        assertEquals(Extracted("IN", "9876543210"), PhoneNumbers.extract("098765 43210", "IN", strict = false))
        assertEquals(Extracted("GB", "7400123456"), PhoneNumbers.extract("07400 123456", "GB", strict = false))
    }

    @Test
    fun whatsappLinksAndSurroundingTextAreHandled() {
        assertEquals(Extracted("IN", "9876543210"), PhoneNumbers.extract("https://wa.me/919876543210", "US", strict = true))
        assertEquals(Extracted("US", "6502530000"), PhoneNumbers.extract("Call me at +1 650-253-0000 tomorrow", "IN", strict = true))
    }

    @Test
    fun strictModeIgnoresNonNumbers() {
        assertNull(PhoneNumbers.extract("Your OTP is 482913", "IN", strict = true))
        assertNull(PhoneNumbers.extract("hello world", "IN", strict = true))
    }

    @Test
    fun lengthValidationFollowsCountry() {
        assertEquals(Status.VALID, PhoneNumbers.check("IN", "9876543210").status)
        assertEquals(Status.TOO_SHORT, PhoneNumbers.check("IN", "98765").status)
        assertEquals(Status.TOO_LONG, PhoneNumbers.check("IN", "98765432101234").status)
        // Singapore numbers have 8 digits, not 10
        assertEquals(Status.VALID, PhoneNumbers.check("SG", "81234567").status)
        assertEquals(listOf(10), PhoneNumbers.possibleLengths("IN"))
        assertEquals(listOf(8), PhoneNumbers.possibleLengths("SG"))
    }

    @Test
    fun waMeNumberIsCountryCodePlusNationalNumber() {
        assertEquals("919876543210", PhoneNumbers.check("IN", "098765 43210").waMeNumber)
        assertEquals("447400123456", PhoneNumbers.check("GB", "07400 123456").waMeNumber)
        assertNull(PhoneNumbers.check("IN", "98765").waMeNumber)
    }
}
