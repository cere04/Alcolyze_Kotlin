package com.example.alcolyze.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Le tabelle del database e come sono collegate tra loro:
// - un utente ha molti drink personalizzati (se si cancella l'utente, il drink resta ma senza
//   creatore)
// - un utente ha molte serate (se si cancella l'utente, spariscono anche le sue serate)
// - una serata ha molti pasti e molte consumazioni (se si cancella la serata, spariscono anche
//   quelli)
// - ogni consumazione punta a un drink del listino (se si cancella il drink, spariscono le sue
//   consumazioni)
// La conversione tra queste tabelle e i dati usati dalle schermate è in Mappers.kt.

/** Il profilo dell'unico utente dell'app (non c'è la gestione di più utenti). */
@Entity(tableName = "utenti")
data class UtenteEntity(
    @PrimaryKey(autoGenerate = true) val idUtente: Long = 0,
    val sesso: String,
    val eta: Int,
    val peso: Double,
    val altezza: Double,
    val categoriaGuidatore: String,
    val abituale: Boolean
)

@Entity(
    tableName = "drinks",
    foreignKeys = [
        ForeignKey(
            entity = UtenteEntity::class,
            parentColumns = ["idUtente"],
            childColumns = ["idUtenteCreatore"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    // Rende più veloci le ricerche per creatore del drink.
    indices = [Index(value = ["idUtenteCreatore"])]
)
/** Una voce del listino drink: standard (scaricata da internet) oppure creata dall'utente. */
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true) val idDrink: Long = 0,
    val nome: String,
    val volumeMl: Double,
    val gradazioneAbv: Double,
    val categoria: String,
    val isCustom: Boolean,
    val idUtenteCreatore: Long?,
    // Calcolate automaticamente ogni volta che il drink viene salvato, mai scritte a mano.
    val unitaAlcoliche: Double
)

@Entity(
    tableName = "serate",
    foreignKeys = [
        ForeignKey(
            entity = UtenteEntity::class,
            parentColumns = ["idUtente"],
            childColumns = ["idUtente"],
            onDelete = ForeignKey.CASCADE // Cancellando l'utente si cancella tutto il suo storico
        )
    ]
)
/** Una serata: dal primo drink o pasto registrato fino a quando si torna sobri. */
data class SerataEntity(
    @PrimaryKey(autoGenerate = true) val idSerata: Long = 0,
    val dataInizio: Long, // Le date sono salvate come numero (millisecondi)
    val bacMassimo: Double?,
    val idUtente: Long,
    // null = serata ancora in corso (viene ripresa all'avvio dell'app); ha un valore solo
    // quando la serata viene chiusa.
    val dataFine: Long? = null
)

@Entity(
    tableName = "pasti",
    foreignKeys = [
        ForeignKey(
            entity = SerataEntity::class,
            parentColumns = ["idSerata"],
            childColumns = ["idSerata"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idSerata"])]
)
/** Un pasto registrato durante una serata: cambia lo stato dello stomaco per i drink bevuti dopo. */
data class PastoEntity(
    @PrimaryKey(autoGenerate = true) val idPasto: Long = 0,
    val orario: Long,
    val statoStomaco: String,
    val idSerata: Long
)

@Entity(
    tableName = "consumazioni",
    foreignKeys = [
        ForeignKey(
            entity = SerataEntity::class,
            parentColumns = ["idSerata"],
            childColumns = ["idSerata"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DrinkEntity::class,
            parentColumns = ["idDrink"],
            childColumns = ["idDrink"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Rendono più veloci le ricerche delle consumazioni per serata o per drink.
    indices = [Index(value = ["idSerata"]), Index(value = ["idDrink"])]
)
/** Un singolo drink davvero registrato in una serata, con l'orario e lo stato dello stomaco di quel momento. */
data class ConsumazioneEntity(
    @PrimaryKey(autoGenerate = true) val idConsumazione: Long = 0,
    val orarioAssunzione: Long,
    val statoStomaco: String,
    val idSerata: Long,
    val idDrink: Long
)
