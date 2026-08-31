package com.example.alcolyze.data

import com.example.alcolyze.model.DriverCategory
import com.example.alcolyze.model.Drink
import com.example.alcolyze.model.Gender
import com.example.alcolyze.model.StomachState
import com.example.alcolyze.model.UserProfile
import java.time.Instant
import java.util.UUID

// Traduzioni tra le righe salvate nel database e i dati usati dalle schermate e dal motore di
// calcolo. Tenere i due lati separati fa sì che un cambio nel database non si ripercuota
// automaticamente sul resto dell'app.

/** Riga utente del database -> profilo usato dalle schermate e dai calcoli. */
fun UtenteEntity.toDomain(): UserProfile = UserProfile(
    gender = Gender.valueOf(sesso),
    weightKg = peso,
    heightCm = altezza,
    age = eta,
    driverCategory = DriverCategory.valueOf(categoriaGuidatore),
    isHabitualDrinker = abituale
)

/** Profilo -> riga utente da salvare nel database (id 0 = nuovo utente). */
fun UserProfile.toEntity(id: Long = 0): UtenteEntity = UtenteEntity(
    idUtente = id,
    sesso = gender.name,
    eta = age,
    peso = weightKg,
    altezza = heightCm,
    categoriaGuidatore = driverCategory.name,
    abituale = isHabitualDrinker
)

/** Voce di listino -> drink davvero bevuto a [ingestionTime], con lo stato dello stomaco di quel momento. */
fun DrinkEntity.toDomain(ingestionTime: Instant, stomachState: StomachState): Drink = Drink(
    id = UUID.randomUUID().toString(),
    name = nome,
    volumeMl = volumeMl,
    alcoholByVolume = gradazioneAbv,
    ingestionTime = ingestionTime,
    stomachAbsorptionFactor = stomachState.absorptionFactor,
    absorptionDelayHours = stomachState.absorptionDelayMinutes / 60.0,
    absorptionKaPerHour = stomachState.kaPerHour,
    isCustomLocalDrink = isCustom,
    category = categoria
)
