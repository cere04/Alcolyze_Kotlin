package com.example.alcolyze.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alcolyze.data.AlcolyzeRepository
import com.example.alcolyze.data.ConsumazioneEntity
import com.example.alcolyze.data.DrinkEntity
import com.example.alcolyze.data.PastoEntity
import com.example.alcolyze.data.SerataEntity
import com.example.alcolyze.data.ThemePreferences
import com.example.alcolyze.data.toDomain
import com.example.alcolyze.data.toEntity
import com.example.alcolyze.model.DriverCategory
import com.example.alcolyze.model.Drink
import com.example.alcolyze.model.Gender
import com.example.alcolyze.model.MealLog
import com.example.alcolyze.model.StomachState
import com.example.alcolyze.model.UserProfile
import com.example.alcolyze.utils.AlcoholUnitCalculator
import com.example.alcolyze.utils.BacCalculator
import com.example.alcolyze.utils.PeakBac
import com.example.alcolyze.utils.Trend
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.Calendar
import java.util.UUID

/** Da quanto tempo è stato bevuto o mangiato qualcosa, scelto nei fogli di registrazione. */
enum class TimeOffset(val minutes: Long, val label: String) {
    NOW(0, "Ora"),
    MIN_15(15, "15 min fa"),
    MIN_30(30, "30 min fa"),
    HOUR_1(60, "1h fa")
}

/** Periodo su cui calcolare i totali della pagina Statistiche. */
enum class StatsRange(val label: String) {
    WEEK("Ultima settimana"),
    MONTH("Ultimo mese"),
    THREE_MONTHS("Ultimi 3 mesi"),
    YEAR("Ultimo anno")
}

/** I numeri riepilogativi mostrati nella pagina Statistiche per il periodo scelto. */
data class StatsSummary(
    val topDrinks: List<Pair<String, Int>>,
    val seratesCount: Int,
    val totalDrinks: Int,
    val avgDrinksPerSerata: Double,
    val totalAlcoholGrams: Double,
    val totalCalories: Double,
    val peakBac: Double
)

/**
 * Il "cervello" dell'app: tiene tutti i dati che le schermate mostrano (tasso alcolemico,
 * euforia, drink e pasti della serata, statistiche...) e le azioni che l'utente può fare
 * (registrare un drink o un pasto, gestire i drink personalizzati, modificare il profilo).
 * Ce n'è uno solo, creato all'avvio e condiviso da tutte le schermate.
 *
 * Parole utili per orientarsi:
 * - Serata: il tempo che va dal primo drink o pasto fino a quando si torna sobri (o si preme
 *   "Torna sobrio", o passano 24 ore). Viene salvata, così se si chiude l'app una serata
 *   ancora in corso viene ripresa alla riapertura.
 * - Aggiornamento continuo: un ciclo che ricalcola tasso, euforia e conti alla rovescia ogni
 *   secondo, così gli indicatori e i timer restano aggiornati anche senza fare niente.
 * - Picco previsto: quanto arriveranno a salire tasso ed euforia nelle prossime ore, dato
 *   quanto già bevuto. Si ricalcola solo quando cambia la serata, non ogni secondo.
 * - Tutti i calcoli veri e propri stanno in BacCalculator; qui si decide solo quando farli.
 */
class AlcolyzeViewModel(
    private val repository: AlcolyzeRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    // ========================
    // DATI MOSTRATI DALLE SCHERMATE
    // ========================

    private val _isDarkTheme = MutableStateFlow(themePreferences.isDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _drinksDisponibili = MutableStateFlow<List<DrinkEntity>>(emptyList())
    val drinksDisponibili: StateFlow<List<DrinkEntity>> = _drinksDisponibili.asStateFlow()

    // Le categorie davvero presenti nel listino (scaricate da internet o create dall'utente),
    // non un elenco fisso: cresce da sola quando ne compaiono di nuove. Tenuta sempre allineata
    // al listino.
    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _userProfile = MutableStateFlow(DEFAULT_PROFILE)
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private var utenteId: Long = 0

    private val _stomachState = MutableStateFlow(StomachState.NORMAL_MEAL)
    val stomachState: StateFlow<StomachState> = _stomachState.asStateFlow()

    private val _selectedTimeOffset = MutableStateFlow(TimeOffset.NOW)
    val selectedTimeOffset: StateFlow<TimeOffset> = _selectedTimeOffset.asStateFlow()

    private val _sessionDrinks = MutableStateFlow<List<Drink>>(emptyList())
    val sessionDrinks: StateFlow<List<Drink>> = _sessionDrinks.asStateFlow()

    private val _sessionMeals = MutableStateFlow<List<MealLog>>(emptyList())
    val sessionMeals: StateFlow<List<MealLog>> = _sessionMeals.asStateFlow()

    private val _currentBac = MutableStateFlow(0.0)
    val currentBac: StateFlow<Double> = _currentBac.asStateFlow()

    private val _buzzScore = MutableStateFlow(0.0)
    val buzzScore: StateFlow<Double> = _buzzScore.asStateFlow()

    private val _bacTrend = MutableStateFlow(Trend.FLAT)
    val bacTrend: StateFlow<Trend> = _bacTrend.asStateFlow()

    // La direzione dell'euforia è calcolata sul picco dell'euforia stessa, non su quello del
    // tasso: le due curve non arrivano al massimo nello stesso momento (l'euforia un po' prima).
    // Usare la direzione del tasso qui avrebbe mostrato "in salita" anche quando l'euforia
    // stava già scendendo.
    private val _buzzTrend = MutableStateFlow(Trend.FLAT)
    val buzzTrend: StateFlow<Trend> = _buzzTrend.asStateFlow()

    private val _projectedPeakBac = MutableStateFlow(0.0)
    val projectedPeakBac: StateFlow<Double> = _projectedPeakBac.asStateFlow()

    private val _projectedPeakEuphoria = MutableStateFlow(0.0)
    val projectedPeakEuphoria: StateFlow<Double> = _projectedPeakEuphoria.asStateFlow()

    private val _projectedPeakBacTime = MutableStateFlow<Instant?>(null)
    val projectedPeakBacTime: StateFlow<Instant?> = _projectedPeakBacTime.asStateFlow()

    // A differenza di projectedPeakEuphoria (una stima teorica, per il segnalino sul quadrante),
    // questi due valori sono il massimo di euforia davvero raggiunto dalla curva simulata.
    // Servono al pallino di picco nel grafico della pagina Euforia, così coincide sempre col
    // massimo calcolato anche quando quel picco è già passato.
    private val _chartPeakEuphoria = MutableStateFlow(0.0)
    val chartPeakEuphoria: StateFlow<Double> = _chartPeakEuphoria.asStateFlow()

    private val _chartPeakEuphoriaTime = MutableStateFlow<Instant?>(null)
    val chartPeakEuphoriaTime: StateFlow<Instant?> = _chartPeakEuphoriaTime.asStateFlow()

    private val _driveInSeconds = MutableStateFlow<Long?>(null)
    val driveInSeconds: StateFlow<Long?> = _driveInSeconds.asStateFlow()

    private val _timeUntilOverLimitSeconds = MutableStateFlow<Long?>(null)
    val timeUntilOverLimitSeconds: StateFlow<Long?> = _timeUntilOverLimitSeconds.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

    private var currentSerataId: Long? = null
    private var currentSerataStart: Instant? = null
    private var sessionHasPeaked: Boolean = false

    private val _selectedStatsRange = MutableStateFlow(StatsRange.WEEK)
    val selectedStatsRange: StateFlow<StatsRange> = _selectedStatsRange.asStateFlow()

    private val _statsSummary = MutableStateFlow<StatsSummary?>(null)
    val statsSummary: StateFlow<StatsSummary?> = _statsSummary.asStateFlow()

    // Resta null finché non si sa se c'è una serata da riprendere; poi vero o falso. È ciò che
    // fa partire la schermata giusta all'avvio.
    private val _hasActiveSession = MutableStateFlow<Boolean?>(null)
    val hasActiveSession: StateFlow<Boolean?> = _hasActiveSession.asStateFlow()

    init {
        inizializzaDati()
        avviaTickerBac()
    }

    private fun inizializzaDati() {
        viewModelScope.launch {
            val utente = repository.getUtente()
            if (utente != null) {
                utenteId = utente.idUtente
                _userProfile.value = utente.toDomain()
            } else {
                utenteId = repository.insertUtente(DEFAULT_PROFILE.toEntity())
            }

            repository.syncStandardDrinks()
            refreshDrinksCatalog()

            ripristinaSessioneAttiva()
            loadStats()
        }
    }

    // Ricarica insieme il listino drink e le categorie, così restano sempre coerenti tra loro.
    // Va chiamata dopo ogni cambio che può toccarli (scarico da internet, creazione, modifica o
    // eliminazione di un drink personalizzato).
    private suspend fun refreshDrinksCatalog() {
        _drinksDisponibili.value = repository.getAllDrinks()
        _availableCategories.value = repository.getDistinctCategories()
    }

    /**
     * All'avvio dell'app cerca una serata ancora aperta e la ricostruisce in memoria (drink,
     * pasti, stato dello stomaco), così si riparte da dove si era lasciato invece che dalla
     * schermata di benvenuto. Chiude subito la serata, invece di riprenderla, se è vuota, se è
     * durata più di 24 ore, o se secondo il calcolo la persona sarebbe già tornata sobria.
     */
    private suspend fun ripristinaSessioneAttiva() {
        val serataAttiva = repository.getSerataAttiva(utenteId)
        if (serataAttiva == null) {
            _hasActiveSession.value = false
            return
        }

        val consumazioni = repository.getConsumazioniDettagliateForSerata(serataAttiva.idSerata)
        val pasti = repository.getPastiForSerata(serataAttiva.idSerata)
        val now = Instant.now()

        if (consumazioni.isEmpty() && pasti.isEmpty()) {
            repository.endSerata(serataAttiva.idSerata, now.toEpochMilli())
            _hasActiveSession.value = false
            return
        }

        val reconstructedDrinks = consumazioni.map { row ->
            val stomach = StomachState.valueOf(row.statoStomaco)
            Drink(
                id = UUID.randomUUID().toString(),
                name = row.nome,
                volumeMl = row.volumeMl,
                alcoholByVolume = row.gradazioneAbv,
                ingestionTime = Instant.ofEpochMilli(row.orarioAssunzione),
                stomachAbsorptionFactor = stomach.absorptionFactor,
                absorptionDelayHours = stomach.absorptionDelayMinutes / 60.0,
                absorptionKaPerHour = stomach.kaPerHour,
                isCustomLocalDrink = row.isCustom,
                category = row.categoria,
                dbId = row.idConsumazione
            )
        }

        val sessionStart = Instant.ofEpochMilli(serataAttiva.dataInizio)
        val expiredByDuration = Duration.between(sessionStart, now).toHours() >= SESSION_MAX_DURATION_HOURS
        val expiredBySobriety = BacCalculator.hasReturnedToSober(reconstructedDrinks, _userProfile.value, now)
        if (expiredByDuration || expiredBySobriety) {
            repository.endSerata(serataAttiva.idSerata, now.toEpochMilli())
            _hasActiveSession.value = false
            return
        }

        currentSerataId = serataAttiva.idSerata
        currentSerataStart = sessionStart

        (consumazioni.map { it.orarioAssunzione to it.statoStomaco } + pasti.map { it.orario to it.statoStomaco })
            .minByOrNull { it.first }
            ?.let { (_, statoStomaco) -> _stomachState.value = StomachState.valueOf(statoStomaco) }

        _sessionMeals.value = pasti.map { pasto ->
            MealLog(
                id = pasto.idPasto.toString(),
                stomachState = StomachState.valueOf(pasto.statoStomaco),
                time = Instant.ofEpochMilli(pasto.orario)
            )
        }
        _sessionDrinks.value = reconstructedDrinks
        updateProjectedPeaks(_sessionDrinks.value, now)

        recomputeBac()
        _hasActiveSession.value = true
    }

    /**
     * Ricalcola sia il picco previsto teorico di tasso ed euforia (per i segnalini sul
     * quadrante) sia il picco di euforia davvero raggiunto dalla curva (per il pallino nel
     * grafico della pagina Euforia). Va chiamata a ogni cambio della serata (drink o pasto
     * aggiunto o tolto, serata ripresa); poi i valori restano validi fino al cambio successivo,
     * senza bisogno di ricalcolarli ogni secondo.
     */
    private fun updateProjectedPeaks(drinks: List<Drink>, now: Instant): PeakBac {
        val profile = _userProfile.value
        val projectedPeak = BacCalculator.peakBacDetailed(drinks, profile, now)
        _projectedPeakBacTime.value = projectedPeak.time
        _projectedPeakBac.value = projectedPeak.value
        _projectedPeakEuphoria.value = BacCalculator.peakEuphoriaScore(projectedPeak.value)

        val chartPeak = BacCalculator.peakEuphoriaDetailed(drinks, profile, now)
        _chartPeakEuphoria.value = chartPeak.value
        _chartPeakEuphoriaTime.value = chartPeak.time

        return projectedPeak
    }

    /**
     * Avviato una sola volta all'avvio: ricalcola tasso, euforia e conti alla rovescia ogni
     * secondo per tutta la vita dell'app, così indicatori e timer restano aggiornati senza che
     * l'utente faccia niente. Il ciclo si ferma da solo quando l'app viene chiusa.
     */
    private fun avviaTickerBac() {
        viewModelScope.launch {
            while (isActive) {
                recomputeBac()
                delay(1000)
            }
        }
    }

    /**
     * Il cuore dell'aggiornamento continuo: ricalcola tasso, euforia, direzioni e conti alla
     * rovescia dai drink e pasti della serata, e chiude la serata da sola se la persona è
     * tornata sobria dopo un picco o se sono passate 24 ore. Chiamata ogni secondo e anche
     * subito dopo ogni cambio della serata o del profilo.
     */
    private fun recomputeBac() {
        val drinks = _sessionDrinks.value
        val profile = _userProfile.value
        val now = Instant.now()

        // Prende sia il tasso attuale sia la velocità con cui sta cambiando: quest'ultima serve
        // subito qui sotto per il punteggio euforia, che tiene conto di quanto in fretta si sta
        // assorbendo l'alcol.
        val (bac, rate) = BacCalculator.currentBacAndRate(drinks, profile, now)
        _currentBac.value = bac

        val trend = BacCalculator.trendFor(now, _projectedPeakBacTime.value, bac)

        val buzzScore = BacCalculator.euphoriaScore(bac, rate)
        _buzzScore.value = buzzScore
        _bacTrend.value = trend
        // Direzione dell'euforia calcolata sul suo picco, non su quello del tasso.
        _buzzTrend.value = BacCalculator.trendFor(now, _chartPeakEuphoriaTime.value, buzzScore)

        _driveInSeconds.value = BacCalculator.estimateSecondsUntil(
            drinks, profile, BacCalculator.legalLimit(profile), now
        )
        _timeUntilOverLimitSeconds.value = BacCalculator.estimateSecondsUntilExceeding(
            drinks, profile, BacCalculator.legalLimit(profile), now
        )

        _sessionDurationSeconds.value = currentSerataStart?.let { Duration.between(it, now).seconds } ?: 0L

        if (bac > BacCalculator.SOBER_BAC_THRESHOLD_G_PER_L) sessionHasPeaked = true

        val serataId = currentSerataId
        if (serataId != null) {
            val backToSober = sessionHasPeaked && trend != Trend.UP &&
                    bac <= BacCalculator.SOBER_BAC_THRESHOLD_G_PER_L
            val start = currentSerataStart
            val ranFullDuration = start != null &&
                    Duration.between(start, now).toHours() >= SESSION_MAX_DURATION_HOURS
            if (backToSober || ranFullDuration) {
                endActiveSession(serataId)
            }
        }
    }

    /**
     * Chiude la serata [serataId] sia in memoria (azzera drink, pasti e indicatori) sia nel
     * database (le mette l'orario di fine), e ricarica le statistiche perché ora la serata
     * appena finita ci rientra. Usata sia dal bottone "Torna sobrio" sia dalla chiusura
     * automatica.
     */
    private fun endActiveSession(serataId: Long) {
        _sessionDrinks.value = emptyList()
        _sessionMeals.value = emptyList()
        currentSerataId = null
        currentSerataStart = null
        sessionHasPeaked = false
        _projectedPeakBac.value = 0.0
        _projectedPeakEuphoria.value = 0.0
        _projectedPeakBacTime.value = null
        _chartPeakEuphoria.value = 0.0
        _chartPeakEuphoriaTime.value = null
        recomputeBac()

        viewModelScope.launch {
            repository.endSerata(serataId, Instant.now().toEpochMilli())
            loadStats()
        }
    }

    /** Salva quanto è pieno lo stomaco all'inizio, scelto nella schermata di onboarding. */
    fun updateStomachState(newState: StomachState) {
        _stomachState.value = newState
    }

    /** Sceglie "quando" verrà registrato il prossimo drink o pasto (Ora, 15 min fa, ...). */
    fun selectTimeOffset(offset: TimeOffset) {
        _selectedTimeOffset.value = offset
    }

    /**
     * Registra [drinkEntity] nella serata, all'orario scelto ("Ora", "15 min fa", ...). Se non
     * c'è ancora una serata aperta, ne apre una. Viene salvato subito e poi mostrato negli
     * indicatori e nel registro.
     */
    fun logDrink(drinkEntity: DrinkEntity) {
        viewModelScope.launch {
            val now = Instant.now()
            val offset = _selectedTimeOffset.value
            val ingestionTime = now.minusSeconds(offset.minutes * 60)
            val stomach = stomachStateAt(ingestionTime)

            var serataId = currentSerataId
            if (serataId == null) {
                serataId = repository.insertSerata(
                    SerataEntity(dataInizio = ingestionTime.toEpochMilli(), bacMassimo = 0.0, idUtente = utenteId)
                )
                currentSerataId = serataId
                currentSerataStart = ingestionTime
            }

            val consumazioneId = repository.insertConsumazione(
                ConsumazioneEntity(
                    orarioAssunzione = ingestionTime.toEpochMilli(),
                    statoStomaco = stomach.name,
                    idSerata = serataId,
                    idDrink = drinkEntity.idDrink
                )
            )

            val domainDrink = drinkEntity.toDomain(ingestionTime, stomach).copy(dbId = consumazioneId)
            _sessionDrinks.value = _sessionDrinks.value + domainDrink

            val projectedPeak = updateProjectedPeaks(_sessionDrinks.value, now)
            repository.updateBacMassimo(serataId, maxOf(projectedPeak.value, 0.0))

            recomputeBac()
            loadStats()
        }
    }

    /**
     * Toglie [drink] dal registro della serata (previa conferma dell'utente) e cancella la sua
     * riga salvata, poi ricalcola il picco previsto sui drink rimasti.
     */
    fun removeDrink(drink: Drink) {
        viewModelScope.launch {
            drink.dbId?.let { repository.deleteConsumazione(it) }
            _sessionDrinks.value = _sessionDrinks.value.filterNot { it.id == drink.id }

            val now = Instant.now()
            val remainingDrinks = _sessionDrinks.value
            val projectedPeak = updateProjectedPeaks(remainingDrinks, now)
            currentSerataId?.let { serataId ->
                repository.updateBacMassimo(serataId, maxOf(projectedPeak.value, 0.0))
            }

            recomputeBac()
            loadStats()
        }
    }

    /**
     * Toglie [meal] dal registro della serata (previa conferma dell'utente) e cancella la sua
     * riga salvata. A differenza di [removeDrink], cambia anche lo stato dello stomaco che
     * vale per i drink bevuti dopo quel pasto, quindi ne ricalcola l'assorbimento prima di
     * riproiettare il picco.
     */
    fun removeMeal(meal: MealLog) {
        viewModelScope.launch {
            meal.id.toLongOrNull()?.let { repository.deletePasto(it) }
            _sessionMeals.value = _sessionMeals.value.filterNot { it.id == meal.id }
            // Tolto un pasto, per i drink bevuti dopo vale un altro stato dello stomaco:
            // ricalcola l'assorbimento di tutti i drink della serata, altrimenti resterebbero
            // legati a un pasto che non c'è più.
            recomputeDrinkAbsorptionParams()

            val now = Instant.now()
            val projectedPeak = updateProjectedPeaks(_sessionDrinks.value, now)
            currentSerataId?.let { serataId ->
                repository.updateBacMassimo(serataId, maxOf(projectedPeak.value, 0.0))
            }

            recomputeBac()
            loadStats()
        }
    }

    /**
     * Quanto era pieno lo stomaco all'orario [time]: l'ultimo pasto registrato prima di
     * quell'istante, oppure lo stato scelto all'inizio se non c'era ancora nessun pasto.
     * Da questo dipende quanto in fretta un drink viene assorbito.
     */
    private fun stomachStateAt(time: Instant): StomachState =
        _sessionMeals.value
            .filter { !it.time.isAfter(time) }
            .maxByOrNull { it.time }
            ?.stomachState
            ?: _stomachState.value

    /**
     * Riassegna a ogni drink della serata l'assorbimento corrispondente allo stato dello
     * stomaco al suo orario. Va chiamata dopo ogni aggiunta o rimozione di un pasto, perché può
     * cambiare quale stato dello stomaco vale per i drink registrati vicino a quel momento.
     */
    private fun recomputeDrinkAbsorptionParams() {
        _sessionDrinks.value = _sessionDrinks.value.map { drink ->
            val state = stomachStateAt(drink.ingestionTime)
            drink.copy(
                stomachAbsorptionFactor = state.absorptionFactor,
                absorptionDelayHours = state.absorptionDelayMinutes / 60.0,
                absorptionKaPerHour = state.kaPerHour
            )
        }
    }

    /**
     * Registra un pasto con stato dello stomaco [stomachState] nella serata, all'orario scelto.
     * Come [logDrink], apre una serata se non ce n'è una. Cambia lo stato dello stomaco per i
     * drink bevuti dopo, quindi ne ricalcola l'assorbimento prima di riproiettare il picco.
     */
    fun logMeal(stomachState: StomachState) {
        viewModelScope.launch {
            val now = Instant.now()
            val mealTime = now.minusSeconds(_selectedTimeOffset.value.minutes * 60)

            var serataId = currentSerataId
            if (serataId == null) {
                serataId = repository.insertSerata(
                    SerataEntity(dataInizio = mealTime.toEpochMilli(), bacMassimo = 0.0, idUtente = utenteId)
                )
                currentSerataId = serataId
                currentSerataStart = mealTime
            }

            val pastoId = repository.insertPasto(
                PastoEntity(orario = mealTime.toEpochMilli(), statoStomaco = stomachState.name, idSerata = serataId)
            )

            _sessionMeals.value = _sessionMeals.value + MealLog(
                id = pastoId.toString(),
                stomachState = stomachState,
                time = mealTime
            )
            recomputeDrinkAbsorptionParams()

            val projectedPeak = updateProjectedPeaks(_sessionDrinks.value, now)
            repository.updateBacMassimo(serataId, maxOf(projectedPeak.value, 0.0))

            recomputeBac()
            loadStats()
        }
    }

    // Crea un drink personalizzato. La categoria è testo libero scelto tra quelle esistenti (o
    // una nuova), non un elenco fisso.
    fun createCustomDrink(nome: String, volumeMl: Double, gradazioneAbv: Double, categoria: String): Result<Unit> {
        if (nome.isBlank() || categoria.isBlank() || volumeMl <= 0.0 || gradazioneAbv <= 0.0 || gradazioneAbv > 100.0) {
            return Result.failure(IllegalArgumentException("Dati non validi"))
        }
        viewModelScope.launch {
            repository.insertCustomDrink(
                DrinkEntity(
                    nome = nome,
                    volumeMl = volumeMl,
                    gradazioneAbv = gradazioneAbv,
                    categoria = categoria,
                    isCustom = true,
                    idUtenteCreatore = utenteId,
                    unitaAlcoliche = AlcoholUnitCalculator.alcoholUnits(volumeMl, gradazioneAbv)
                )
            )
            refreshDrinksCatalog()
        }
        return Result.success(Unit)
    }

    // Modifica un drink creato dall'utente (non fa niente sui drink del listino condiviso).
    // I drink già registrati con questo drink prenderanno i nuovi valori solo alla prossima
    // lettura, non nella serata già in corso.
    fun updateCustomDrink(drink: DrinkEntity, nome: String, volumeMl: Double, gradazioneAbv: Double, categoria: String): Result<Unit> {
        if (!drink.isCustom) return Result.failure(IllegalStateException("Solo i drink personalizzati sono modificabili"))
        if (nome.isBlank() || categoria.isBlank() || volumeMl <= 0.0 || gradazioneAbv <= 0.0 || gradazioneAbv > 100.0) {
            return Result.failure(IllegalArgumentException("Dati non validi"))
        }
        viewModelScope.launch {
            repository.updateDrink(
                drink.copy(
                    nome = nome,
                    volumeMl = volumeMl,
                    gradazioneAbv = gradazioneAbv,
                    categoria = categoria,
                    unitaAlcoliche = AlcoholUnitCalculator.alcoholUnits(volumeMl, gradazioneAbv)
                )
            )
            refreshDrinksCatalog()
        }
        return Result.success(Unit)
    }

    // Elimina un drink creato dall'utente (non fa niente sui drink del listino condiviso).
    // Insieme al drink spariscono tutte le sue registrazioni, in ogni serata passata: per
    // questo si ricaricano le statistiche anche se la serata in corso non lo contiene.
    fun deleteCustomDrink(drink: DrinkEntity) {
        if (!drink.isCustom) return
        viewModelScope.launch {
            repository.deleteDrink(drink.idDrink)
            refreshDrinksCatalog()
            loadStats()
        }
    }

    /**
     * Bottone "Torna sobrio" (previa conferma): chiude subito la serata, qualunque sia il tasso
     * attuale. Se la serata era già salvata la chiude e aggiorna le statistiche; altrimenti
     * azzera solo i dati in memoria.
     */
    fun resetToSober() {
        val serataId = currentSerataId
        if (serataId != null) {
            endActiveSession(serataId)
        } else {
            _sessionDrinks.value = emptyList()
            _sessionMeals.value = emptyList()
            sessionHasPeaked = false
            _projectedPeakBac.value = 0.0
            _projectedPeakEuphoria.value = 0.0
            _projectedPeakBacTime.value = null
            _chartPeakEuphoria.value = 0.0
            _chartPeakEuphoriaTime.value = null
            recomputeBac()
        }
    }

    /** Passa da tema chiaro a scuro (e viceversa): aggiorna subito e ricorda la scelta. */
    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        themePreferences.setDarkTheme(isDark)
    }

    /**
     * Salva il profilo modificato: applica subito i nuovi dati (peso, età, sesso e categoria di
     * patente cambiano il calcolo) e ricalcola il tasso attuale, così una modifica a metà
     * serata si vede subito sugli indicatori.
     */
    fun saveProfile(profile: UserProfile) {
        _userProfile.value = profile
        viewModelScope.launch {
            repository.updateUtente(profile.toEntity(utenteId))
            recomputeBac()
        }
    }

    /** Cambia il periodo (settimana, mese, ...) della pagina Statistiche e ricarica i totali. */
    fun selectStatsRange(range: StatsRange) {
        _selectedStatsRange.value = range
        viewModelScope.launch { loadStats() }
    }

    /**
     * Ricalcola i totali della pagina Statistiche per il periodo scelto: mette insieme tutte le
     * consumazioni di ogni serata (non solo quella in corso) e ricava drink più bevuti, numero
     * di serate, medie, alcol e calorie totali, picco di tasso più alto. Va richiamata dopo
     * ogni evento che può cambiare questi numeri e quando si cambia periodo.
     */
    private suspend fun loadStats() {
        val fromTimestamp = fromTimestampFor(_selectedStatsRange.value)
        val serate = repository.getSerateFrom(fromTimestamp)
        val righe = repository.getConsumazioniConDrinkFrom(fromTimestamp)

        if (righe.isEmpty()) {
            _statsSummary.value = StatsSummary(
                topDrinks = emptyList(),
                seratesCount = 0,
                totalDrinks = 0,
                avgDrinksPerSerata = 0.0,
                totalAlcoholGrams = 0.0,
                totalCalories = 0.0,
                peakBac = 0.0
            )
            return
        }

        val topDrinks = righe.groupingBy { it.nome }.eachCount().entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }
        val serateCount = righe.map { it.idSerata }.distinct().size
        val totalDrinks = righe.size
        val totalAlcoholGrams = righe.sumOf { it.volumeMl * (it.gradazioneAbv / 100.0) * 0.789 }
        val totalCalories = BacCalculator.estimatedCalories(totalAlcoholGrams)
        val peakBac = serate.maxOfOrNull { it.bacMassimo ?: 0.0 } ?: 0.0

        _statsSummary.value = StatsSummary(
            topDrinks = topDrinks,
            seratesCount = serateCount,
            totalDrinks = totalDrinks,
            avgDrinksPerSerata = if (serateCount > 0) totalDrinks.toDouble() / serateCount else 0.0,
            totalAlcoholGrams = totalAlcoholGrams,
            totalCalories = totalCalories,
            peakBac = peakBac
        )
    }

    /** L'istante di inizio del periodo scelto (es. WEEK = 7 giorni fa da adesso). */
    private fun fromTimestampFor(range: StatsRange): Long {
        val calendar = Calendar.getInstance()
        when (range) {
            StatsRange.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            StatsRange.MONTH -> calendar.add(Calendar.MONTH, -1)
            StatsRange.THREE_MONTHS -> calendar.add(Calendar.MONTH, -3)
            StatsRange.YEAR -> calendar.add(Calendar.YEAR, -1)
        }
        return calendar.timeInMillis
    }

    companion object {
        // Oltre questa durata una serata viene chiusa da sola anche senza essere tornati sobri:
        // evita serate che resterebbero aperte all'infinito per un errore o per un tasso che
        // non scende mai del tutto a zero.
        const val SESSION_MAX_DURATION_HOURS = 24L

        // Profilo di partenza, usato finché non c'è quello vero e per il primo utente creato al
        // primissimo avvio.
        val DEFAULT_PROFILE = UserProfile(
            gender = Gender.MALE,
            weightKg = 75.0,
            heightCm = 175.0,
            age = 25,
            driverCategory = DriverCategory.STANDARD,
            isHabitualDrinker = false
        )
    }
}

/** Costruisce l'AlcolyzeViewModel passandogli le sue dipendenze (database e preferenze). */
class AlcolyzeViewModelFactory(
    private val repository: AlcolyzeRepository,
    private val themePreferences: ThemePreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlcolyzeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlcolyzeViewModel(repository, themePreferences) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}
