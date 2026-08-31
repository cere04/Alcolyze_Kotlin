package com.example.alcolyze

import com.example.alcolyze.ui.util.formatIntoxicationGauge
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun formatIntoxicationGauge_showsRawGPerLUnderTheCap() {
        assertEquals("0.00", formatIntoxicationGauge(0.0))
        assertEquals("0.25", formatIntoxicationGauge(0.25))
        assertEquals("4.00", formatIntoxicationGauge(4.0))
    }

    @Test
    fun formatIntoxicationGauge_showsPlusPastTheCapInsteadOfAnUnboundedNumber() {
        assertEquals("4.0+", formatIntoxicationGauge(4.01))
        assertEquals("4.0+", formatIntoxicationGauge(10.0))
    }
}