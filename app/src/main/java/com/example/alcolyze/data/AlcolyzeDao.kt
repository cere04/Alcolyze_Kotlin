package com.example.alcolyze.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

// Elenco di tutte le operazioni sul database. Ogni funzione è una lettura o una scrittura su
// una delle tabelle (utenti, drink, serate, pasti, consumazioni).
@Dao
interface AlcolyzeDao {

    // --- UTENTE ---
    // Solo per creare il primo utente. Non va usata per salvare le modifiche a un utente già
    // esistente: sovrascrivere una riga esistente qui la cancella e la ricrea, e con lei
    // sparirebbe tutto lo storico collegato (serate, consumazioni, pasti). Per le modifiche
    // c'è updateUtente.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtente(utente: UtenteEntity): Long

    // Salva le modifiche al profilo di un utente esistente senza cancellare la riga, quindi
    // senza rischio di perdere lo storico.
    @Update
    suspend fun updateUtente(utente: UtenteEntity)

    @Query("SELECT * FROM utenti LIMIT 1")
    suspend fun getUtente(): UtenteEntity?

    // --- DRINK ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrink(drink: DrinkEntity): Long

    // Tutti i drink del listino: sia quelli scaricati da internet, sia quelli creati dall'utente.
    @Query("SELECT * FROM drinks")
    suspend fun getAllDrinks(): List<DrinkEntity>

    // Le categorie mostrate nei filtri e nel form "drink personalizzato" non sono un elenco
    // fisso: sono quelle davvero presenti tra i drink salvati, sia scaricate sia inserite
    // dall'utente. Crescono da sole.
    @Query("SELECT DISTINCT categoria FROM drinks ORDER BY categoria ASC")
    suspend fun getDistinctCategories(): List<String>

    // Modifica un drink esistente senza cancellarne la riga. Se la si cancellasse (anche solo
    // per ricrearla subito uguale) sparirebbero tutte le sue registrazioni passate.
    @Update
    suspend fun updateDrink(drink: DrinkEntity)

    // Elimina un drink. Insieme al drink spariscono tutte le sue registrazioni, in ogni serata
    // passata e presente. La UI espone questa azione solo per i drink personalizzati.
    @Query("DELETE FROM drinks WHERE idDrink = :idDrink")
    suspend fun deleteDrink(idDrink: Long)

    // Toglie dal listino i drink standard il cui nome non è più tra [namesToKeep]: serve a far
    // sparire i drink rimossi dal listino online. Non tocca i drink creati dall'utente.
    @Query("DELETE FROM drinks WHERE isCustom = 0 AND nome NOT IN (:namesToKeep)")
    suspend fun deleteStandardDrinksNotIn(namesToKeep: List<String>)

    // --- SERATE & CONSUMAZIONI ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSerata(serata: SerataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumazione(consumazione: ConsumazioneEntity): Long

    @Query("SELECT * FROM serate ORDER BY dataInizio DESC")
    suspend fun getAllSerate(): List<SerataEntity>

    @Query("SELECT * FROM serate WHERE dataInizio >= :fromTimestamp ORDER BY dataInizio DESC")
    suspend fun getSerateFrom(fromTimestamp: Long): List<SerataEntity>

    // La serata ancora aperta dell'utente, se c'è: serve a riprenderla dopo che l'app è stata
    // chiusa, invece di ripartire da "Inizia la serata".
    @Query("SELECT * FROM serate WHERE idUtente = :idUtente AND dataFine IS NULL ORDER BY dataInizio DESC LIMIT 1")
    suspend fun getSerataAttiva(idUtente: Long): SerataEntity?

    @Query("UPDATE serate SET dataFine = :dataFine WHERE idSerata = :idSerata")
    suspend fun endSerata(idSerata: Long, dataFine: Long)

    @Query("SELECT * FROM consumazioni WHERE idSerata = :idSerata")
    suspend fun getConsumazioniForSerata(idSerata: Long): List<ConsumazioneEntity>

    // Ogni drink registrato in una serata, unito ai dati del drink (nome, volume, gradazione).
    // Serve a ricostruire la serata quando l'app viene riaperta.
    @Query(
        """
        SELECT c.idConsumazione AS idConsumazione, c.orarioAssunzione AS orarioAssunzione,
               c.statoStomaco AS statoStomaco, d.nome AS nome, d.volumeMl AS volumeMl,
               d.gradazioneAbv AS gradazioneAbv, d.categoria AS categoria, d.isCustom AS isCustom
        FROM consumazioni c
        INNER JOIN drinks d ON c.idDrink = d.idDrink
        WHERE c.idSerata = :idSerata
        ORDER BY c.orarioAssunzione ASC
        """
    )
    suspend fun getConsumazioniDettagliateForSerata(idSerata: Long): List<ConsumazioneDettagliataRow>

    @Query("DELETE FROM consumazioni WHERE idConsumazione = :idConsumazione")
    suspend fun deleteConsumazione(idConsumazione: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasto(pasto: PastoEntity): Long

    @Query("SELECT * FROM pasti WHERE idSerata = :idSerata ORDER BY orario ASC")
    suspend fun getPastiForSerata(idSerata: Long): List<PastoEntity>

    @Query("DELETE FROM pasti WHERE idPasto = :idPasto")
    suspend fun deletePasto(idPasto: Long)

    @Query("UPDATE serate SET bacMassimo = :bac WHERE idSerata = :idSerata")
    suspend fun updateBacMassimo(idSerata: Long, bac: Double)

    // Ogni drink registrato dopo una certa data, con i dati del drink: serve per i totali della
    // pagina Statistiche.
    @Query(
        """
        SELECT c.idSerata AS idSerata, d.nome AS nome, d.gradazioneAbv AS gradazioneAbv,
               d.volumeMl AS volumeMl, c.orarioAssunzione AS orarioAssunzione
        FROM consumazioni c
        INNER JOIN drinks d ON c.idDrink = d.idDrink
        WHERE c.orarioAssunzione >= :fromTimestamp
        """
    )
    suspend fun getConsumazioniConDrinkFrom(fromTimestamp: Long): List<ConsumazioneStatsRow>
}

/** Riga usata per i totali della pagina Statistiche. */
data class ConsumazioneStatsRow(
    val idSerata: Long,
    val nome: String,
    val gradazioneAbv: Double,
    val volumeMl: Double,
    val orarioAssunzione: Long
)

/** Riga usata per ricostruire i drink di una serata alla riapertura dell'app. */
data class ConsumazioneDettagliataRow(
    val idConsumazione: Long,
    val orarioAssunzione: Long,
    val statoStomaco: String,
    val nome: String,
    val volumeMl: Double,
    val gradazioneAbv: Double,
    val categoria: String,
    val isCustom: Boolean
)
