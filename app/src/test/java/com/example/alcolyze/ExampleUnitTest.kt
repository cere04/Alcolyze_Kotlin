package com.example.alcolyze

import com.example.alcolyze.model.DriverCategory
import com.example.alcolyze.model.Gender
import com.example.alcolyze.model.UserProfile
import com.example.alcolyze.utils.BacCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class BacScientificUnitTest {

    @Test
    fun testWatsonTotalBodyWater_isCorrect() {
        // Persona di prova: uomo, 80 kg, 180 cm, 25 anni.
        // Litri d'acqua nel corpo attesi: 46.386.

        val testUser = UserProfile(
            gender = Gender.MALE,
            weightKg = 80.0,
            heightCm = 180.0,
            age = 25,
            driverCategory = DriverCategory.STANDARD
        )

        val expectedLiters = 46.386
        val calculatedLiters = BacCalculator.calculateTBW(testUser)

        // Verifica che il calcolo combaci, con una tolleranza di 0.01 litri.
        assertEquals(expectedLiters, calculatedLiters, 0.01)
    }

    @Test
    fun testWidmarkFactor_isCorrect() {
        val testUser = UserProfile(
            gender = Gender.MALE,
            weightKg = 80.0,
            heightCm = 180.0,
            age = 25,
            driverCategory = DriverCategory.STANDARD
        )

        // Quanto si diluisce l'alcol nel corpo: acqua / (0.801 * peso) -> valore atteso 0.7239.
        val expectedR = 0.7239
        val calculatedR = BacCalculator.calculateWidmarkFactor(testUser)

        assertEquals(expectedR, calculatedR, 0.001)
    }
}