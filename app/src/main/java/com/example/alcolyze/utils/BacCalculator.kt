package com.example.alcolyze.utils

import com.example.alcolyze.model.Drink
import com.example.alcolyze.model.DriverCategory
import com.example.alcolyze.model.Gender
import com.example.alcolyze.model.UserProfile
import java.time.Duration
import java.time.Instant
import kotlin.math.exp

/** In quale delle 4 fasce si trova il tasso alcolemico attuale. Serve a colorare la barra Sicurezza e l'indicatore Intossicazione. */
enum class SafetyZone(val label: String) {
    SOBRIO("Sobrio"),
    FELICE("Felice"),
    UBRIACO("Ubriaco"),
    PERICOLO("In pericolo")
}

/** In che direzione sta andando un valore in questo momento: su, giù o fermo. Serve per la freccetta accanto agli indicatori. */
enum class Trend { UP, DOWN, FLAT }

/** Il valore più alto raggiunto dalla curva del tasso alcolemico e l'orario in cui succede. */
data class PeakBac(val value: Double, val time: Instant)

/** Il valore più alto raggiunto dalla curva dell'euforia e l'orario in cui succede. */
data class PeakEuphoria(val value: Double, val time: Instant)

/** Un punto della curva prevista: tasso alcolemico e punteggio euforia a uno stesso orario. */
data class BacTrajectoryPoint(val time: Instant, val bac: Double, val euphoria: Double)

/**
 * Il motore di calcolo dell'app: tutti i conti sul tasso alcolemico (grammi di alcol per litro
 * di sangue) e sul punteggio euforia si fanno qui. Non conserva niente tra una chiamata e
 * l'altra: ogni funzione riceve i drink e i dati della persona e restituisce un risultato.
 *
 * Il cuore è la funzione di simulazione, che ricostruisce l'intera curva minuto per minuto
 * tenendo conto dell'alcol che entra nel sangue e di quello smaltito dal fegato. Tutte le altre
 * funzioni (tasso attuale, picco previsto, quanto manca per guidare, euforia, curva per i
 * grafici) partono dalla stessa simulazione, così i numeri mostrati restano coerenti tra loro.
 */
object BacCalculator {

    /** Litri d'acqua contenuti nel corpo, stimati da sesso, età, altezza e peso. */
    fun calculateTBW(profile: UserProfile): Double {
        return when (profile.gender) {
            Gender.MALE -> 2.447 - (0.09156 * profile.age) + (0.1074 * profile.heightCm) + (0.3362 * profile.weightKg)
            Gender.FEMALE -> -2.097 + (0.1069 * profile.heightCm) + (0.2466 * profile.weightKg)
        }
    }

    /** Quanta parte del sangue è acqua (valore fisso). */
    const val BLOOD_WATER_CONTENT = 0.801

    /**
     * Quanto l'alcol bevuto si diluisce nel corpo: dipende da quanta acqua contiene il corpo
     * rispetto al peso. Un corpo più piccolo o più magro fa salire di più il tasso alcolemico a
     * parità di drink.
     */
    fun calculateWidmarkFactor(profile: UserProfile): Double {
        val tbw = calculateTBW(profile)
        return tbw / (BLOOD_WATER_CONTENT * profile.weightKg)
    }

    // Velocità con cui il fegato smaltisce l'alcol (grammi per litro all'ora): la seconda vale
    // per chi beve abitualmente.
    const val METABOLIC_ELIMINATION_RATE = 0.15
    const val HABITUAL_ELIMINATION_RATE = 0.25

    // Costanti della formula dello smaltimento del fegato: a tasso basso lo smaltimento rallenta,
    // sopra una certa soglia diventa costante.
    const val MM_KM_G_PER_L = 0.027
    const val MM_SWITCH_MULTIPLIER = 3.0
    private const val MM_VMAX_CONTINUITY_FRACTION = 0.75

    // Limiti legali per la guida (grammi per litro).
    const val LEGAL_LIMIT_STANDARD = 0.5
    const val LEGAL_LIMIT_NEOPATENTATO = 0.0

    // Soglie che separano le 4 fasce della barra Sicurezza (grammi per litro).
    const val SAFETY_ZONE_FELICE_THRESHOLD_G_PER_L = 0.5
    const val SAFETY_ZONE_UBRIACO_THRESHOLD_G_PER_L = 0.9
    const val SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L = 4.0

    // Calorie per grammo di alcol puro.
    const val KCAL_PER_GRAM_ETHANOL = 7.0

    // Costanti del punteggio euforia (0-10): non dipende solo dal tasso alcolemico, ma anche da
    // quanto in fretta si sta bevendo.
    const val EUPHORIA_SCORE_MAX = 10.0
    const val EUPHORIA_PEAK_BAC_G_PER_L = 0.55
    private const val EUPHORIA_OVERSHOOT_DECAY = 2.0
    private const val EUPHORIA_DESCENDING_RETENTION = 0.35
    private const val EUPHORIA_ASCEND_REFERENCE_RATE = 0.5
    private const val EUPHORIA_DESCEND_REFERENCE_RATE = 0.3
    private const val EUPHORIA_MODULATION_FLOOR = 0.6

    // Sotto questa variazione il valore si considera "fermo".
    const val TREND_FLAT_EPSILON = 0.01

    /** Limite legale per guidare (grammi per litro) in base alla categoria di patente. */
    fun legalLimit(profile: UserProfile): Double = when (profile.driverCategory) {
        DriverCategory.STANDARD -> LEGAL_LIMIT_STANDARD
        DriverCategory.NEOPATENTATO_OR_PROFESSIONAL -> LEGAL_LIMIT_NEOPATENTATO
    }

    /** Velocità con cui il fegato smaltisce l'alcol (grammi per litro all'ora); più alta per chi beve spesso. */
    fun eliminationRate(profile: UserProfile): Double =
        if (profile.isHabitualDrinker) HABITUAL_ELIMINATION_RATE else METABOLIC_ELIMINATION_RATE

    /** In quale delle 4 fasce di sicurezza si trova un dato tasso alcolemico. */
    fun safetyZoneFor(bac: Double): SafetyZone = when {
        bac < SAFETY_ZONE_FELICE_THRESHOLD_G_PER_L -> SafetyZone.SOBRIO
        bac < SAFETY_ZONE_UBRIACO_THRESHOLD_G_PER_L -> SafetyZone.FELICE
        bac <= SAFETY_ZONE_PERICOLO_THRESHOLD_G_PER_L -> SafetyZone.UBRIACO
        else -> SafetyZone.PERICOLO
    }

    /** Testo mostrato sotto la barra Sicurezza: gli effetti della fascia più un avviso sulla guida in base al limite della persona. */
    fun safetyZoneDescription(zone: SafetyZone, bac: Double, profile: UserProfile): String {
        val physiological = when (zone) {
            SafetyZone.SOBRIO -> "Nessuna alterazione significativa."
            SafetyZone.FELICE -> "Primi cambiamenti dell'umore: attenzione e controllo già in calo."
            SafetyZone.UBRIACO -> "Coordinazione, riflessi e capacità di giudizio seriamente compromessi."
            SafetyZone.PERICOLO -> "Livello estremamente pericoloso: rischio concreto di perdita di coscienza."
        }
        val guida = if (bac > legalLimit(profile)) {
            "La guida è vivamente sconsigliata."
        } else {
            "Puoi guidare, ma sempre con attenzione."
        }
        return "$physiological $guida"
    }

    /** Calorie stimate dai grammi di alcol puro (7 kcal per grammo). */
    fun estimatedCalories(pureEthanolGrams: Double): Double = pureEthanolGrams * KCAL_PER_GRAM_ETHANOL

    /** Quanto un singolo drink alzerebbe il tasso alcolemico se tutto il suo alcol entrasse nel sangue in una volta sola; la simulazione poi lo distribuisce nel tempo. */
    private fun peakContribution(drink: Drink, profile: UserProfile): Double {
        val r = calculateWidmarkFactor(profile)
        if (r <= 0 || profile.weightKg <= 0) return 0.0
        return (drink.pureEthanolGrams * drink.stomachAbsorptionFactor) / (profile.weightKg * r)
    }

    /**
     * Ricostruisce la curva del tasso alcolemico dal primo drink fino all'orario [to], avanzando
     * a piccoli passi. Per ogni passo calcola quanto alcol entra nel sangue e quanto ne toglie il
     * fegato, e comunica il risultato tramite [onSample], che riporta anche la velocità con cui
     * il valore sta cambiando in quel momento.
     */
    private fun simulate(
        drinks: List<Drink>,
        profile: UserProfile,
        to: Instant,
        stepSeconds: Long = 60L,
        onSample: (at: Instant, bac: Double, rate: Double) -> Unit
    ) {
        if (drinks.isEmpty()) return
        val start = drinks.minOf { it.ingestionTime }
        val totalSeconds = Duration.between(start, to).seconds
        onSample(start, 0.0, 0.0)
        if (totalSeconds <= 0) return

        val count = drinks.size
        val peaks = DoubleArray(count)
        val delays = DoubleArray(count)
        val kas = DoubleArray(count)
        val offsets = DoubleArray(count)
        drinks.forEachIndexed { i, drink ->
            peaks[i] = peakContribution(drink, profile)
            delays[i] = drink.absorptionDelayHours
            kas[i] = drink.absorptionKaPerHour
            offsets[i] = Duration.between(start, drink.ingestionTime).toMillis() / 3_600_000.0
        }

        val beta = eliminationRate(profile)
        val vMax = beta / MM_VMAX_CONTINUITY_FRACTION
        val switchThreshold = MM_SWITCH_MULTIPLIER * MM_KM_G_PER_L

        // Quanto alcol sta entrando nel sangue all'ora [h], sommando tutti i drink che a quel
        // punto hanno già iniziato a essere assorbiti.
        fun absorptionRateAt(h: Double): Double {
            var rate = 0.0
            for (i in 0 until count) {
                val u = h - offsets[i] - delays[i]
                if (u >= 0.0) rate += peaks[i] * kas[i] * exp(-kas[i] * u)
            }
            return rate
        }

        // Quanto alcol sta togliendo il fegato al tasso attuale: rallenta quando il tasso è
        // basso, è costante quando è alto.
        fun eliminationRateAt(bac: Double): Double = when {
            bac <= 0.0 -> 0.0
            bac > switchThreshold -> beta
            else -> vMax * bac / (MM_KM_G_PER_L + bac)
        }

        // Variazione netta del tasso: quello che entra meno quello che esce.
        fun rate(h: Double, bac: Double): Double = absorptionRateAt(h) - eliminationRateAt(bac)

        var bac = 0.0
        var elapsedSeconds = 0L
        while (elapsedSeconds < totalSeconds) {
            val stepClamped = minOf(stepSeconds, totalSeconds - elapsedSeconds)
            val dt = stepClamped / 3600.0
            val h = elapsedSeconds / 3600.0
            val k1 = rate(h, bac)
            val k2 = rate(h + dt / 2, (bac + dt / 2 * k1).coerceAtLeast(0.0))
            val k3 = rate(h + dt / 2, (bac + dt / 2 * k2).coerceAtLeast(0.0))
            val k4 = rate(h + dt, (bac + dt * k3).coerceAtLeast(0.0))
            bac = (bac + (dt / 6.0) * (k1 + 2 * k2 + 2 * k3 + k4)).coerceAtLeast(0.0)
            elapsedSeconds += stepClamped

            // Velocità con cui il tasso sta cambiando in questo istante.
            val currentRate = rate(elapsedSeconds / 3600.0, bac)
            onSample(start.plusSeconds(elapsedSeconds), bac, currentRate)
        }
    }

    /** Il tasso alcolemico all'orario [at]. */
    fun currentBac(drinks: List<Drink>, profile: UserProfile, at: Instant = Instant.now()): Double {
        if (drinks.isEmpty()) return 0.0
        var result = 0.0
        simulate(drinks, profile, at) { _, bac, _ -> result = bac }
        return result
    }

    /** Il tasso alcolemico all'orario [at] e la velocità con cui sta cambiando: quest'ultima serve per il punteggio euforia. */
    fun currentBacAndRate(drinks: List<Drink>, profile: UserProfile, at: Instant = Instant.now()): Pair<Double, Double> {
        if (drinks.isEmpty()) return Pair(0.0, 0.0)
        var finalBac = 0.0
        var finalRate = 0.0
        simulate(drinks, profile, at) { _, bac, rate ->
            finalBac = bac
            finalRate = rate
        }
        return Pair(finalBac, finalRate)
    }

    /** Quanto un singolo drink pesa sul tasso alcolemico a un dato orario, considerato da solo. */
    fun contributionAt(drink: Drink, profile: UserProfile, at: Instant): Double =
        currentBac(listOf(drink), profile, at)

    /**
     * Il punteggio euforia (0-10) mostrato nell'indicatore. Sale fino a un massimo intorno a
     * 0.55 g/L e poi cala. Conta anche quanto in fretta si sta bevendo ([rateOfChangePerHour]):
     * a parità di tasso, bere in fretta dà un effetto più forte che bere piano. Il punto più
     * basso della modulazione cade esattamente quando il tasso smette di salire, così il
     * punteggio non "rimbalza" in modo strano.
     */
    fun euphoriaScore(bac: Double, rateOfChangePerHour: Double = EUPHORIA_ASCEND_REFERENCE_RATE): Double {
        if (bac <= 0.0) return 0.0
        val ratio = bac / EUPHORIA_PEAK_BAC_G_PER_L
        val ascendingPotential = if (ratio <= 1.0) {
            EUPHORIA_SCORE_MAX * (ratio * ratio * (3.0 - 2.0 * ratio))
        } else {
            EUPHORIA_SCORE_MAX * exp(-(ratio - 1.0) * EUPHORIA_OVERSHOOT_DECAY)
        }

        val multiplier = if (rateOfChangePerHour >= 0.0) {
            EUPHORIA_MODULATION_FLOOR + (1.0 - EUPHORIA_MODULATION_FLOOR) *
                (rateOfChangePerHour / EUPHORIA_ASCEND_REFERENCE_RATE).coerceIn(0.0, 1.0)
        } else {
            val descendEase = (-rateOfChangePerHour / EUPHORIA_DESCEND_REFERENCE_RATE).coerceIn(0.0, 1.0)
            EUPHORIA_MODULATION_FLOOR - (EUPHORIA_MODULATION_FLOOR - EUPHORIA_DESCENDING_RETENTION) * descendEase
        }

        return (ascendingPotential * multiplier).coerceIn(0.0, EUPHORIA_SCORE_MAX)
    }

    /** Euforia massima teorica dato il picco previsto del tasso: è il valore usato solo per il segnalino sul quadrante. */
    fun peakEuphoriaScore(peakBac: Double): Double =
        euphoriaScore(minOf(peakBac, EUPHORIA_PEAK_BAC_G_PER_L))

    /** Direzione di un valore: sale se non ha ancora raggiunto il suo picco previsto, scende se è ancora sopra zero, altrimenti è fermo. */
    fun trendFor(now: Instant, peakBacTime: Instant?, bac: Double): Trend = when {
        peakBacTime != null && now.isBefore(peakBacTime) -> Trend.UP
        bac > TREND_FLAT_EPSILON -> Trend.DOWN
        else -> Trend.FLAT
    }

    /**
     * Il picco previsto del tasso alcolemico: valore massimo e orario che la curva raggiungerà
     * nelle prossime [maxHorizonHours] ore, dato quanto già bevuto. È il segnalino fisso mostrato
     * sull'indicatore Intossicazione finché non si registra un altro drink.
     */
    fun peakBacDetailed(
        drinks: List<Drink>,
        profile: UserProfile,
        from: Instant = Instant.now(),
        maxHorizonHours: Int = 24
    ): PeakBac {
        if (drinks.isEmpty()) return PeakBac(0.0, from)

        var best = currentBac(drinks, profile, from)
        var bestAt = from
        simulate(drinks, profile, from.plusSeconds(maxHorizonHours * 3600L)) { at, bac, _ ->
            if (!at.isBefore(from) && bac > best) {
                best = bac
                bestAt = at
            }
        }
        return PeakBac(best, bestAt)
    }

    /**
     * Il punteggio euforia più alto che la curva raggiunge davvero simulandola tutta (diverso da
     * peakEuphoriaScore, che è solo una stima teorica). È il valore usato nel grafico della
     * pagina di dettaglio Euforia.
     */
    fun peakEuphoriaDetailed(
        drinks: List<Drink>,
        profile: UserProfile,
        from: Instant = Instant.now(),
        maxHorizonHours: Int = 24
    ): PeakEuphoria {
        if (drinks.isEmpty()) return PeakEuphoria(0.0, from)

        val (bacNow, rateNow) = currentBacAndRate(drinks, profile, from)
        var best = euphoriaScore(bacNow, rateNow)
        var bestAt = from
        simulate(drinks, profile, from.plusSeconds(maxHorizonHours * 3600L)) { at, bac, rate ->
            if (!at.isBefore(from)) {
                val score = euphoriaScore(bac, rate)
                if (score > best) {
                    best = score
                    bestAt = at
                }
            }
        }
        return PeakEuphoria(best, bestAt)
    }

    /** Solo il valore del picco previsto del tasso, senza l'orario. */
    fun peakBac(
        drinks: List<Drink>,
        profile: UserProfile,
        from: Instant = Instant.now(),
        maxHorizonHours: Int = 24
    ): Double = peakBacDetailed(drinks, profile, from, maxHorizonHours).value

    /** Quante ore in avanti proietta la curva il grafico "andamento". */
    const val TREND_CHART_HORIZON_HOURS = 5.0

    /**
     * La sequenza di punti (tasso e euforia) da [from] alle prossime [horizonHours] ore, per il
     * grafico "andamento". Usa la stessa simulazione dei valori attuali, così il grafico è
     * sempre coerente con gli indicatori della home.
     */
    fun projectTrajectory(
        drinks: List<Drink>,
        profile: UserProfile,
        from: Instant = Instant.now(),
        horizonHours: Double = TREND_CHART_HORIZON_HOURS,
        stepSeconds: Long = 60L
    ): List<BacTrajectoryPoint> {
        if (drinks.isEmpty()) return listOf(BacTrajectoryPoint(from, 0.0, 0.0))

        fun pointAt(at: Instant, bac: Double, rate: Double): BacTrajectoryPoint =
            BacTrajectoryPoint(at, bac, euphoriaScore(bac, rate))

        val (bacNow, rateNow) = currentBacAndRate(drinks, profile, from)
        val points = mutableListOf(pointAt(from, bacNow, rateNow))

        val to = from.plusSeconds((horizonHours * 3600.0).toLong())
        simulate(drinks, profile, to, stepSeconds) { at, bac, rate ->
            if (at.isAfter(from)) points += pointAt(at, bac, rate)
        }
        return points
    }

    /** Sotto questo valore il tasso alcolemico si considera praticamente zero. */
    const val SOBER_BAC_THRESHOLD_G_PER_L = 0.001

    /** Quanti secondi mancano da [now] prima che il tasso previsto scenda sotto [targetBac] (es. il limite legale); null se non succede entro [maxHorizonHours] ore. */
    fun estimateSecondsUntil(
        drinks: List<Drink>,
        profile: UserProfile,
        targetBac: Double,
        now: Instant = Instant.now(),
        maxHorizonHours: Int = 24
    ): Long? {
        if (drinks.isEmpty()) return null
        // Un piccolo margine serve solo per soglie vicine a zero (es. il limite 0.0 dei
        // neopatentati): il tasso non tocca mai lo zero esatto, quindi senza margine non si
        // troverebbe mai il momento in cui lo attraversa. Per soglie normali (es. 0.5) niente margine.
        val target = if (targetBac <= SOBER_BAC_THRESHOLD_G_PER_L) targetBac + SOBER_BAC_THRESHOLD_G_PER_L else targetBac
        if (currentBac(drinks, profile, now) <= target) return null

        val maxElapsed = maxHorizonHours * 3600L
        var lastAboveSeconds = 0L
        var crossingSeconds: Long? = null
        simulate(drinks, profile, now.plusSeconds(maxElapsed)) { at, bac, _ ->
            if (crossingSeconds == null && !at.isBefore(now)) {
                val secondsFromNow = Duration.between(now, at).seconds
                if (bac > target) lastAboveSeconds = secondsFromNow else crossingSeconds = secondsFromNow
            }
        }
        val windowEnd = crossingSeconds ?: return maxElapsed

        var low = lastAboveSeconds
        var high = windowEnd
        while (low < high) {
            val mid = low + (high - low) / 2
            if (currentBac(drinks, profile, now.plusSeconds(mid)) <= target) high = mid else low = mid + 1
        }
        return low
    }

    /**
     * L'opposto di estimateSecondsUntil: quanti secondi mancano da [now] prima che il tasso
     * previsto SUPERI [targetBac] per la prima volta. Restituisce null sia se è già oltre quel
     * valore adesso, sia se resterà sotto per tutte le prossime [maxHorizonHours] ore ("non lo
     * supererà mai" è un esito valido, non approssimato).
     */
    fun estimateSecondsUntilExceeding(
        drinks: List<Drink>,
        profile: UserProfile,
        targetBac: Double,
        now: Instant = Instant.now(),
        maxHorizonHours: Int = 24
    ): Long? {
        if (drinks.isEmpty()) return null
        if (currentBac(drinks, profile, now) >= targetBac) return null

        val maxElapsed = maxHorizonHours * 3600L
        var lastBelowSeconds = 0L
        var crossingSeconds: Long? = null
        simulate(drinks, profile, now.plusSeconds(maxElapsed)) { at, bac, _ ->
            if (crossingSeconds == null && !at.isBefore(now)) {
                val secondsFromNow = Duration.between(now, at).seconds
                if (bac < targetBac) lastBelowSeconds = secondsFromNow else crossingSeconds = secondsFromNow
            }
        }
        val windowEnd = crossingSeconds ?: return null

        var low = lastBelowSeconds
        var high = windowEnd
        while (low < high) {
            val mid = low + (high - low) / 2
            if (currentBac(drinks, profile, now.plusSeconds(mid)) < targetBac) low = mid + 1 else high = mid
        }
        return low
    }

    /**
     * Vero se la persona ha già avuto un picco di tasso alcolemico e, all'orario [at], è tornata
     * praticamente a zero. Serve alla riapertura dell'app per capire se una serata rimasta
     * aperta va chiusa invece che ripresa, perché nel frattempo la persona sarebbe tornata sobria.
     */
    fun hasReturnedToSober(drinks: List<Drink>, profile: UserProfile, at: Instant): Boolean {
        if (drinks.isEmpty()) return false
        val start = drinks.minOf { it.ingestionTime }
        if (!at.isAfter(start)) return false

        val hoursSoFar = Duration.between(start, at).toHours().toInt() + 1
        val peakSoFar = peakBacDetailed(drinks, profile, from = start, maxHorizonHours = hoursSoFar)
        if (peakSoFar.value <= SOBER_BAC_THRESHOLD_G_PER_L) return false
        if (at.isBefore(peakSoFar.time)) return false

        return currentBac(drinks, profile, at) <= SOBER_BAC_THRESHOLD_G_PER_L
    }
}
