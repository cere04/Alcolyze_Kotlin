package com.example.alcolyze

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/** Test strumentato, eseguito su un device Android reale (o emulatore). */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contesto dell'app sotto test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.alcolyze", appContext.packageName)
    }
}