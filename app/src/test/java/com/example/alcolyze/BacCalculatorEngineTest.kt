package com.example.alcolyze

import com.example.alcolyze.model.DriverCategory
import com.example.alcolyze.model.Drink
import com.example.alcolyze.model.Gender
import com.example.alcolyze.model.StomachState
import com.example.alcolyze.model.UserProfile
import com.example.alcolyze.utils.BacCalculator
import com.example.alcolyze.utils.SafetyZone
import com.example.alcolyze.utils.Trend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BacCalculatorEngineTest {

    private val referenceProfile = UserProfile(
        gender = Gender.MALE,
        weightKg = 80.0,
        heightCm = 180.0,
        age = 25,
        driverCategory = DriverCategory.STANDARD
    )

    private val neopatentatoProfile = referenceProfile.copy(driverCategory = DriverCategory.NEOPATENTATO_OR_PROFESSIONAL)

    private fun drinkOf(
        volumeMl: Double,
        abv: Double,
        ingestionTime: Instant,
        absorptionFactor: Double = 1.0,
        absorptionDelayHours: Double = 0.0
    ) = Drink(
        id = "test",
        name = "Test Drink",
        volumeMl = volumeMl,
        alcoholByVolume = abv,
        ingestionTime = ingestionTime,
        stomachAbsorptionFactor = absorptionFactor,
        absorptionDelayHours = absorptionDelayHours
    )

    @Test
    fun contribution_isZeroAtTheMomentOfIngestion() {
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now)

        assertEquals(0.0, BacCalculator.contributionAt(beer, referenceProfile, now), 0.0001)
    }

    @Test
    fun contribution_matchesAbsorptionAndEliminationFormula() {
        val ingestion = Instant.now().minusSeconds(30 * 60)
        // Una birra da 330 ml al 5% -> circa 13 grammi di alcol puro.
        val beer = drinkOf(330.0, 5.0, ingestion)
        val at = ingestion.plusSeconds(30 * 60)

        val peak = beer.pureEthanolGrams / (referenceProfile.weightKg * BacCalculator.calculateWidmarkFactor(referenceProfile))
        val hours = 0.5
        // Stima approssimata: nei primi minuti il fegato smaltisce meno, quindi il valore vero
        // è un po' più alto. Per questo si usa una tolleranza ampia invece di un confronto esatto.
        val expected = (peak * (1 - Math.exp(-beer.absorptionKaPerHour * hours)) - BacCalculator.METABOLIC_ELIMINATION_RATE * hours)
            .coerceAtLeast(0.0)

        val actual = BacCalculator.contributionAt(beer, referenceProfile, at)

        assertEquals(expected, actual, 0.01)
    }

    @Test
    fun contribution_risesThroughAbsorptionThenDecays() {
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now)

        val early = BacCalculator.contributionAt(beer, referenceProfile, now.plusSeconds(10 * 60))
        val nearPeak = BacCalculator.contributionAt(beer, referenceProfile, now.plusSeconds(25 * 60))
        val late = BacCalculator.contributionAt(beer, referenceProfile, now.plusSeconds(90 * 60))

        assertTrue(nearPeak > early)
        assertTrue(nearPeak > late)
    }

    @Test
    fun contribution_respectsAbsorptionDelay() {
        val now = Instant.now()
        // Con un ritardo lungo (stomaco pieno) dopo pochi minuti non deve essere ancora
        // assorbito niente.
        val beer = drinkOf(330.0, 5.0, now, absorptionDelayHours = 1.0)

        assertEquals(0.0, BacCalculator.contributionAt(beer, referenceProfile, now.plusSeconds(20 * 60)), 0.0001)
        assertTrue(BacCalculator.contributionAt(beer, referenceProfile, now.plusSeconds(90 * 60)) > 0.0)
    }

    @Test
    fun contribution_decaysLinearlyOnceFullyAbsorbed() {
        val now = Instant.now()
        // Bevuto abbastanza tempo fa da essere già tutto assorbito: da qui in poi la curva deve
        // scendere esattamente alla velocità con cui il fegato smaltisce.
        val spirit = drinkOf(500.0, 40.0, now.minusSeconds(3 * 3600))

        val atThreeHours = BacCalculator.contributionAt(spirit, referenceProfile, now)
        val atFourHours = BacCalculator.contributionAt(spirit, referenceProfile, now.plusSeconds(3600))

        assertEquals(atThreeHours - BacCalculator.METABOLIC_ELIMINATION_RATE, atFourHours, 0.005)
    }

    @Test
    fun currentBac_sharesOneEliminationPoolAcrossOverlappingDrinks() {
        // Due drink uguali insieme devono dare un picco quasi doppio di uno solo: il fegato è
        // uno, non raddoppia lo smaltimento per ogni drink in più.
        val now = Instant.now()
        val one = listOf(drinkOf(330.0, 5.0, now))
        val two = listOf(drinkOf(330.0, 5.0, now), drinkOf(330.0, 5.0, now))

        val peakOne = BacCalculator.peakBac(one, referenceProfile, now)
        val peakTwo = BacCalculator.peakBac(two, referenceProfile, now)

        assertTrue(peakTwo > peakOne * 1.8)
    }

    @Test
    fun trend_isUpTheInstantADrinkIsLogged() {
        // La freccetta deve mostrare "In salita" subito dopo aver registrato un drink, perché il
        // picco previsto è già noto in anticipo.
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now)

        val peak = BacCalculator.peakBacDetailed(listOf(beer), referenceProfile, now)
        val trend = BacCalculator.trendFor(now, peak.time, bac = 0.0)

        assertTrue(peak.time.isAfter(now))
        assertEquals(Trend.UP, trend)
    }

    @Test
    fun trend_flipsToDownOncePastTheProjectedPeak() {
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now)

        val peak = BacCalculator.peakBacDetailed(listOf(beer), referenceProfile, now)
        val afterPeak = peak.time.plusSeconds(600)
        val bacAfterPeak = BacCalculator.currentBac(listOf(beer), referenceProfile, afterPeak)

        val trend = BacCalculator.trendFor(afterPeak, peak.time, bacAfterPeak)

        assertEquals(Trend.DOWN, trend)
    }

    @Test
    fun twoDrinksAfterFullMeal_meaningfullyRaisesBac() {
        // Il fegato smaltisce come un unico processo, non drink per drink: due birre dopo un
        // pasto abbondante devono comunque far salire il tasso in modo apprezzabile.
        val now = Instant.now()
        val profile = UserProfile(
            gender = Gender.MALE, weightKg = 75.0, heightCm = 175.0, age = 25,
            driverCategory = DriverCategory.STANDARD
        )
        fun beerAfterFullMeal() = Drink(
            id = "beer", name = "Birra", volumeMl = 330.0, alcoholByVolume = 5.0,
            ingestionTime = now,
            stomachAbsorptionFactor = StomachState.FULL_MEAL.absorptionFactor,
            absorptionDelayHours = StomachState.FULL_MEAL.absorptionDelayMinutes / 60.0
        )

        val peak = BacCalculator.peakBac(listOf(beerAfterFullMeal(), beerAfterFullMeal()), profile, now)

        assertTrue("two beers after a full meal peaked at only $peak g/L", peak > 0.15)
    }

    @Test
    fun peakBac_findsTheMaximumOverTime() {
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now)

        val peak = BacCalculator.peakBac(listOf(beer), referenceProfile, now)
        val atStart = BacCalculator.currentBac(listOf(beer), referenceProfile, now)
        val atOneHour = BacCalculator.currentBac(listOf(beer), referenceProfile, now.plusSeconds(3600))

        assertTrue(peak > 0.0)
        assertTrue(peak >= atStart)
        assertTrue(peak >= atOneHour)
    }

    @Test
    fun peakBac_isZeroForNoDrinks() {
        assertEquals(0.0, BacCalculator.peakBac(emptyList(), referenceProfile), 0.0)
    }

    @Test
    fun peakBac_isConsistentWithOfficialItalianReferenceTable() {
        // Un uomo di 75 kg a stomaco vuoto con una birra normale dovrebbe arrivare intorno a
        // 0.26 g/L. L'app calcola in modo un po' diverso (dati personali e assorbimento
        // graduale), quindi qui si controlla solo che il valore sia nell'ordine di grandezza giusto.
        val profile = UserProfile(
            gender = Gender.MALE, weightKg = 75.0, heightCm = 175.0, age = 25,
            driverCategory = DriverCategory.STANDARD
        )
        val beer = Drink(
            id = "beer", name = "Birra normale", volumeMl = 330.0, alcoholByVolume = 5.0,
            ingestionTime = Instant.now(),
            stomachAbsorptionFactor = StomachState.EMPTY.absorptionFactor,
            absorptionDelayHours = StomachState.EMPTY.absorptionDelayMinutes / 60.0
        )

        val peak = BacCalculator.peakBac(listOf(beer), profile, Instant.now())

        assertTrue("expected peak in the ballpark of the official table's 0.26 g/L, was $peak", peak in 0.10..0.30)
    }

    @Test
    fun legalLimit_dependsOnDriverCategory() {
        val standard = referenceProfile.copy(driverCategory = DriverCategory.STANDARD)
        val neopatentato = referenceProfile.copy(driverCategory = DriverCategory.NEOPATENTATO_OR_PROFESSIONAL)

        assertEquals(BacCalculator.LEGAL_LIMIT_STANDARD, BacCalculator.legalLimit(standard), 0.0)
        assertEquals(BacCalculator.LEGAL_LIMIT_NEOPATENTATO, BacCalculator.legalLimit(neopatentato), 0.0)
    }

    @Test
    fun eliminationRate_isFasterForHabitualDrinkers() {
        val habitual = referenceProfile.copy(isHabitualDrinker = true)
        val occasional = referenceProfile.copy(isHabitualDrinker = false)

        assertEquals(BacCalculator.METABOLIC_ELIMINATION_RATE, BacCalculator.eliminationRate(occasional), 0.0001)
        assertEquals(BacCalculator.HABITUAL_ELIMINATION_RATE, BacCalculator.eliminationRate(habitual), 0.0001)
        assertTrue(BacCalculator.eliminationRate(habitual) > BacCalculator.eliminationRate(occasional))
    }

    @Test
    fun estimateSecondsUntil_matchesLinearDecayEstimate() {
        val now = Instant.now()
        // Drink forte, bevuto abbastanza tempo fa da essere tutto assorbito ma con ancora molto
        // tasso residuo. La soglia è tenuta abbastanza alta da avere una discesa costante fino
        // al valore obiettivo (il caso vicino a zero è testato più sotto).
        val spirit = drinkOf(500.0, 40.0, now.minusSeconds(3 * 3600))
        val currentBac = BacCalculator.currentBac(listOf(spirit), referenceProfile, now)
        val targetBac = 0.5

        val expectedSeconds = ((currentBac - targetBac) / BacCalculator.eliminationRate(referenceProfile)) * 3600.0

        val actual = BacCalculator.estimateSecondsUntil(
            drinks = listOf(spirit),
            profile = referenceProfile,
            targetBac = targetBac,
            now = now
        )

        assertTrue(actual != null)
        assertTrue(Math.abs(actual!! - expectedSeconds) < 90.0)
    }

    @Test
    fun estimateSecondsUntil_resolvesToPracticalSoberDespiteAsymptoticMmTail() {
        // Il tasso si avvicina a zero senza toccarlo mai del tutto, quindi anche con obiettivo
        // 0.0 il calcolo deve dare un tempo concreto e ragionevole. È importante perché il
        // limite dei neopatentati è proprio 0.0.
        val now = Instant.now()
        // Dopo 45 minuti: già oltre il picco e in discesa, ma non ancora smaltito.
        val beer = drinkOf(330.0, 5.0, now.minusSeconds(45 * 60))

        val actual = BacCalculator.estimateSecondsUntil(
            drinks = listOf(beer),
            profile = referenceProfile,
            targetBac = 0.0,
            now = now,
            maxHorizonHours = 24
        )

        assertTrue(actual != null)
        assertTrue("expected a concrete sobriety time well inside the 24h search horizon, was $actual", actual!! < 24 * 3600L)
    }

    @Test
    fun estimateSecondsUntil_isNullWhenAlreadyBelowTarget() {
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now)

        val actual = BacCalculator.estimateSecondsUntil(
            drinks = listOf(beer),
            profile = referenceProfile,
            targetBac = 5.0,
            now = now
        )

        assertNull(actual)
    }

    @Test
    fun estimateSecondsUntil_isNullWhenNoDrinks() {
        val actual = BacCalculator.estimateSecondsUntil(
            drinks = emptyList(),
            profile = referenceProfile,
            targetBac = 0.0
        )

        assertNull(actual)
    }

    // --- Ritorno alla sobrietà (chiusura automatica della serata quando il tasso torna a zero) ---

    @Test
    fun hasReturnedToSober_isFalseRightAfterIngestionBeforeAnyRise() {
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now)

        assertTrue(!BacCalculator.hasReturnedToSober(listOf(beer), referenceProfile, now))
    }

    @Test
    fun hasReturnedToSober_isFalseWhileStillRisingTowardItsPeak() {
        val now = Instant.now()
        val beer = drinkOf(330.0, 5.0, now.minusSeconds(10 * 60))

        assertTrue(!BacCalculator.hasReturnedToSober(listOf(beer), referenceProfile, now))
    }

    @Test
    fun hasReturnedToSober_isFalseWhileStillDescendingButNotYetPracticallyZero() {
        val now = Instant.now()
        // Oltre il picco ma ancora lontano dall'essere smaltito.
        val beer = drinkOf(330.0, 5.0, now.minusSeconds(45 * 60))

        assertTrue(!BacCalculator.hasReturnedToSober(listOf(beer), referenceProfile, now))
    }

    @Test
    fun hasReturnedToSober_isTrueOnceARealPeakHasFullyEliminated() {
        val now = Instant.now()
        // Un solo drink leggero, bevuto abbastanza tempo fa da essere ormai tutto smaltito.
        val beer = drinkOf(330.0, 5.0, now.minusSeconds(8 * 3600))

        assertTrue(BacCalculator.hasReturnedToSober(listOf(beer), referenceProfile, now))
    }

    @Test
    fun hasReturnedToSober_isFalseWhenNoDrinks() {
        assertTrue(!BacCalculator.hasReturnedToSober(emptyList(), referenceProfile, Instant.now()))
    }

    // --- Fasce e descrizioni della barra Sicurezza ------------------------------

    @Test
    fun safetyZoneFor_mergesTheFirstThreeTableBandsIntoASingleSobrioZone() {
        assertEquals(SafetyZone.SOBRIO, BacCalculator.safetyZoneFor(0.0))
        assertEquals(SafetyZone.SOBRIO, BacCalculator.safetyZoneFor(0.2))
        assertEquals(SafetyZone.SOBRIO, BacCalculator.safetyZoneFor(0.4))
    }

    @Test
    fun safetyZoneFor_matchesTheRemainingTableBands() {
        assertEquals(SafetyZone.FELICE, BacCalculator.safetyZoneFor(0.5))
        assertEquals(SafetyZone.FELICE, BacCalculator.safetyZoneFor(0.8))
        assertEquals(SafetyZone.UBRIACO, BacCalculator.safetyZoneFor(0.9))
        assertEquals(SafetyZone.UBRIACO, BacCalculator.safetyZoneFor(4.0))
        assertEquals(SafetyZone.PERICOLO, BacCalculator.safetyZoneFor(4.01))
    }

    @Test
    fun safetyZoneDescription_tellsAStandardDriverTheyCanDriveUnderTheLegalLimit() {
        val description = BacCalculator.safetyZoneDescription(SafetyZone.SOBRIO, 0.3, referenceProfile)
        assertTrue(description.contains("Puoi guidare"))
    }

    @Test
    fun safetyZoneDescription_warnsAStandardDriverOverTheLegalLimitEvenIfStillPhysiologicallySobrio() {
        val description = BacCalculator.safetyZoneDescription(SafetyZone.FELICE, 0.6, referenceProfile)
        assertTrue(description.contains("vivamente sconsigliata"))
    }

    @Test
    fun safetyZoneDescription_warnsANeopatentatoWellBeforeTheStandardLimitEvenInTheSobrioZone() {
        // 0.2 g/L rientra ancora nella fascia "Sobrio", ma per un neopatentato il limite è 0.
        val description = BacCalculator.safetyZoneDescription(SafetyZone.SOBRIO, 0.2, neopatentatoProfile)
        assertTrue(description.contains("vivamente sconsigliata"))
    }

    @Test
    fun safetyZoneDescription_allowsANeopatentatoToDriveOnlyAtTrueZero() {
        val description = BacCalculator.safetyZoneDescription(SafetyZone.SOBRIO, 0.0, neopatentatoProfile)
        assertTrue(description.contains("Puoi guidare"))
    }
}
