package com.example.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    /**
     * Converts rupees (e.g. 1499.50) into exact integer paise (149950L) without floating point imprecision.
     */
    fun rupeesStringToPaise(rupeesStr: String): Long {
        val cleaned = rupeesStr.replace(",", "").trim()
        if (cleaned.isEmpty()) return 0L
        return try {
            val bd = BigDecimal(cleaned)
            bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Converts paise to formatted Indian Rupee display (e.g. ₹1,499.50 or ₹1,499).
     */
    fun formatPaiseToRupees(paise: Long, showDecimals: Boolean = true): String {
        val isNegative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val paisePart = absPaise % 100

        val formattedRupees = formatIndianNumber(rupees)
        val sign = if (isNegative) "-" else ""

        return if (showDecimals && paisePart > 0) {
            String.format(Locale.US, "%s₹%s.%02d", sign, formattedRupees, paisePart)
        } else if (showDecimals) {
            String.format(Locale.US, "%s₹%s.00", sign, formattedRupees)
        } else {
            String.format(Locale.US, "%s₹%s", sign, formattedRupees)
        }
    }

    /**
     * Formats positive integer numbers into the Indian numbering format (e.g. 1,00,000).
     */
    fun formatIndianNumber(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str

        val lastThree = str.substring(str.length - 3)
        var remaining = str.substring(0, str.length - 3)
        val parts = mutableListOf<String>()

        while (remaining.length > 2) {
            parts.add(0, remaining.substring(remaining.length - 2))
            remaining = remaining.substring(0, remaining.length - 2)
        }
        if (remaining.isNotEmpty()) {
            parts.add(0, remaining)
        }
        return parts.joinToString(",") + "," + lastThree
    }

    fun paiseToRupeesDouble(paise: Long): Double {
        return paise.toDouble() / 100.0
    }
}
