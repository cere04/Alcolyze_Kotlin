package com.example.alcolyze.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UtenteEntity::class, DrinkEntity::class, SerataEntity::class, ConsumazioneEntity::class,
        PastoEntity::class
    ],
    // Se si cambia questo numero di versione, al primo avvio successivo il database locale viene
    // cancellato e ricreato vuoto (i drink si riscaricano da soli da internet, ma serate e
    // statistiche andrebbero perse).
    version = 4,
    exportSchema = false
)
/** Il database dell'app: un'unica copia condivisa, con le 5 tabelle (utenti, drink, serate, pasti, consumazioni). */
abstract class AlcolyzeDatabase : RoomDatabase() {

    abstract fun alcolyzeDao(): AlcolyzeDao

    companion object {
        @Volatile
        private var INSTANCE: AlcolyzeDatabase? = null

        fun getDatabase(context: Context): AlcolyzeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlcolyzeDatabase::class.java,
                    "alcolyze_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
