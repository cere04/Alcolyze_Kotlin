package com.example.alcolyze.data

import android.util.Log
import com.example.alcolyze.utils.AlcoholUnitCalculator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Punto unico da cui il resto dell'app legge e scrive i dati: gira le richieste al database
// locale e, per il listino drink, anche al listino condiviso online.
class AlcolyzeRepository(private val alcolyzeDao: AlcolyzeDao) {

    // Collegamento al listino drink condiviso online.
    private val firestore = FirebaseFirestore.getInstance()

    // ========================
    // UTENTE
    // ========================
    suspend fun getUtente(): UtenteEntity? = alcolyzeDao.getUtente()

    suspend fun insertUtente(utente: UtenteEntity): Long = alcolyzeDao.insertUtente(utente)

    suspend fun updateUtente(utente: UtenteEntity) = alcolyzeDao.updateUtente(utente)

    // ========================
    // DRINK
    // ========================
    suspend fun getAllDrinks(): List<DrinkEntity> = alcolyzeDao.getAllDrinks()

    suspend fun insertCustomDrink(drink: DrinkEntity): Long = alcolyzeDao.insertDrink(drink)

    suspend fun updateDrink(drink: DrinkEntity) = alcolyzeDao.updateDrink(drink)

    suspend fun deleteDrink(idDrink: Long) = alcolyzeDao.deleteDrink(idDrink)

    suspend fun getDistinctCategories(): List<String> = alcolyzeDao.getDistinctCategories()

    /**
     * Scarica il listino drink standard da internet e aggiorna la copia locale: modifica quelli
     * già presenti (confrontando per nome), aggiunge i nuovi e toglie quelli spariti dal listino
     * online. Se non c'è connessione o la richiesta fallisce, non cambia niente e l'app continua
     * con il listino che ha già.
     */
    suspend fun syncStandardDrinks() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Chiede il listino online e aspetta la risposta.
                val snapshot = firestore.collection("standard_drinks").get().await()

                // 2. Trasforma ogni voce ricevuta in un drink da salvare.
                val standardDrinks = snapshot.documents.mapNotNull { doc ->
                    val nome = doc.getString("nome")
                    val volumeMl = doc.getDouble("volume_ml")
                    val gradazioneAbv = doc.getDouble("gradazione_abv")
                    val categoria = normalizeCategory(doc.getString("category"))

                    // Serve che ci siano nome, volume e gradazione. La categoria è testo libero,
                    // quindi non è mai un motivo per scartare la voce.
                    if (nome != null && volumeMl != null && gradazioneAbv != null) {
                        DrinkEntity(
                            nome = nome,
                            volumeMl = volumeMl,
                            gradazioneAbv = gradazioneAbv,
                            categoria = categoria,
                            isCustom = false, // È un drink del listino standard
                            idUtenteCreatore = null,
                            // Calcolate qui, non lette dal listino online.
                            unitaAlcoliche = AlcoholUnitCalculator.alcoholUnits(volumeMl, gradazioneAbv)
                        )
                    } else {
                        null // Salta le voci incomplete
                    }
                }

                // 3. Aggiorna la copia locale. I drink già presenti vengono modificati sul posto,
                // non cancellati e ricreati: se venissero cancellati sparirebbero anche tutte le
                // loro registrazioni passate, serata in corso compresa.
                if (standardDrinks.isNotEmpty()) {
                    val drinkStandardEsistentiPerNome = alcolyzeDao.getAllDrinks()
                        .filter { !it.isCustom }
                        .associateBy { it.nome }

                    standardDrinks.forEach { drink ->
                        val esistente = drinkStandardEsistentiPerNome[drink.nome]
                        if (esistente != null) {
                            alcolyzeDao.updateDrink(drink.copy(idDrink = esistente.idDrink))
                        } else {
                            alcolyzeDao.insertDrink(drink)
                        }
                    }
                    // I drink standard spariti dal listino online vengono tolti davvero: qui
                    // perdere anche le loro registrazioni è il comportamento voluto, non un errore.
                    alcolyzeDao.deleteStandardDrinksNotIn(standardDrinks.map { it.nome })
                    Log.d("AlcolyzeRepository", "Sincronizzazione completata: ${standardDrinks.size} drink sincronizzati.")
                }

            } catch (e: Exception) {
                // Se non c'è connessione o qualcosa va storto, lo scrive nei log e basta.
                // L'app non si blocca e continua con i dati che ha già in locale.
                Log.e("AlcolyzeRepository", "Errore di sincronizzazione offline: ${e.message}")
            }
        }
    }

    // Le categorie non sono un elenco fisso: sono quelle che arrivano dal listino online, e ne
    // possono comparire di nuove senza aggiornare l'app. Il listino online mescola maiuscole e
    // lingue diverse ("beer", "Cocktail", "aperitivi"): qui si riscrivono con l'iniziale
    // maiuscola, così i filtri non mostrano la stessa categoria due volte scritta in modo diverso.
    private fun normalizeCategory(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return "Altro"
        return trimmed.lowercase().split(" ").joinToString(" ") { word ->
            if (word.isEmpty()) word else word.replaceFirstChar { it.uppercase() }
        }
    }

    // ========================
    // SERATE E CONSUMAZIONI
    // ========================
    suspend fun insertSerata(serata: SerataEntity): Long = alcolyzeDao.insertSerata(serata)

    suspend fun insertConsumazione(consumazione: ConsumazioneEntity): Long = alcolyzeDao.insertConsumazione(consumazione)

    suspend fun getAllSerate(): List<SerataEntity> = alcolyzeDao.getAllSerate()

    suspend fun getSerateFrom(fromTimestamp: Long): List<SerataEntity> = alcolyzeDao.getSerateFrom(fromTimestamp)

    suspend fun getSerataAttiva(idUtente: Long): SerataEntity? = alcolyzeDao.getSerataAttiva(idUtente)

    suspend fun endSerata(idSerata: Long, dataFine: Long) = alcolyzeDao.endSerata(idSerata, dataFine)

    suspend fun updateBacMassimo(idSerata: Long, bac: Double) = alcolyzeDao.updateBacMassimo(idSerata, bac)

    suspend fun deleteConsumazione(idConsumazione: Long) = alcolyzeDao.deleteConsumazione(idConsumazione)

    suspend fun getConsumazioniDettagliateForSerata(idSerata: Long): List<ConsumazioneDettagliataRow> =
        alcolyzeDao.getConsumazioniDettagliateForSerata(idSerata)

    suspend fun insertPasto(pasto: PastoEntity): Long = alcolyzeDao.insertPasto(pasto)

    suspend fun getPastiForSerata(idSerata: Long): List<PastoEntity> = alcolyzeDao.getPastiForSerata(idSerata)

    suspend fun deletePasto(idPasto: Long) = alcolyzeDao.deletePasto(idPasto)

    suspend fun getConsumazioniConDrinkFrom(fromTimestamp: Long): List<ConsumazioneStatsRow> =
        alcolyzeDao.getConsumazioniConDrinkFrom(fromTimestamp)
}
