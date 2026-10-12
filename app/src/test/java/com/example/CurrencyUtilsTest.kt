package com.example

import com.example.core.util.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun `test rupees string to exact integer paise`() {
        assertEquals(149950L, CurrencyUtils.rupeesStringToPaise("1499.50"))
        assertEquals(149900L, CurrencyUtils.rupeesStringToPaise("1,499.00"))
        assertEquals(50000000L, CurrencyUtils.rupeesStringToPaise("5,00,000"))
        assertEquals(340L, CurrencyUtils.rupeesStringToPaise("3.40"))
        assertEquals(0L, CurrencyUtils.rupeesStringToPaise(""))
    }

    @Test
    fun `test format paise to rupees`() {
        assertEquals("₹1,499.50", CurrencyUtils.formatPaiseToRupees(149950L))
        assertEquals("₹1,499.00", CurrencyUtils.formatPaiseToRupees(149900L))
        assertEquals("₹1,499", CurrencyUtils.formatPaiseToRupees(149900L, showDecimals = false))
        assertEquals("₹5,00,000.00", CurrencyUtils.formatPaiseToRupees(50000000L))
    }

    @Test
    fun `test Indian number formatting`() {
        assertEquals("1,00,000", CurrencyUtils.formatIndianNumber(100000L))
        assertEquals("15,000", CurrencyUtils.formatIndianNumber(15000L))
        assertEquals("1,23,45,678", CurrencyUtils.formatIndianNumber(12345678L))
    }
}
