package com.talhastudio.callerid

import com.talhastudio.callerid.core.*
import org.junit.Assert.*
import org.junit.Test

class PakistaniNumberTest {
    @Test fun normalizesSupportedFormats() {
        assertEquals("+923001234567", PakistaniNumber.normalize("03001234567"))
        assertEquals("+923001234567", PakistaniNumber.normalize("+923001234567"))
        assertEquals("+923001234567", PakistaniNumber.normalize("923001234567"))
        assertEquals("+923001234567", PakistaniNumber.normalize("0300 123-4567"))
        assertEquals("+923001234567", PakistaniNumber.normalize("3001234567"))
    }
    @Test fun rejectsInvalidNumbers() {
        listOf("", "0300123456", "030012345678", "02101234567", "+14155550123", "abc", "+92300123456").forEach {
            assertNull(it, PakistaniNumber.normalize(it))
        }
    }
    @Test fun callerIdAcceptsPakistaniOrInternational() {
        assertEquals("+923451234567", PakistaniNumber.parseCallerId("03451234567"))
        assertEquals("+442071838750", PakistaniNumber.parseCallerId("+442071838750"))
        assertNull(PakistaniNumber.parseCallerId("12345"))
    }
    @Test fun detectsOperator() {
        assertEquals(PakistaniNumber.Operator.JAZZ, PakistaniNumber.operator("+923001234567"))
        assertEquals(PakistaniNumber.Operator.ZONG, PakistaniNumber.operator("+923111234567"))
        assertEquals(PakistaniNumber.Operator.UFONE, PakistaniNumber.operator("+923331234567"))
        assertEquals(PakistaniNumber.Operator.TELENOR, PakistaniNumber.operator("+923451234567"))
    }
    @Test fun callerNameRules() {
        assertTrue(PakistaniNumber.isValidCallerName("TALHA STUDIO"))
        assertFalse(PakistaniNumber.isValidCallerName("<script>"))
        assertFalse(PakistaniNumber.isValidCallerName("A"))
    }
}

class ModelsTest {
    @Test fun callStateMapping() {
        assertEquals(CallState.RINGING, CallState.fromWire("ringing"))
        assertEquals(CallState.CONNECTED, CallState.fromWire("CONNECTED"))
        assertEquals(CallState.FAILED, CallState.fromWire("something-new"))
        assertTrue(CallState.ENDED.terminal)
        assertFalse(CallState.CALLING.terminal)
    }
    @Test fun durationFormat() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("02:05", formatDuration(125))
    }
}
