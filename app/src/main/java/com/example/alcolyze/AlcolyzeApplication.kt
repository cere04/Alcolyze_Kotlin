package com.example.alcolyze

import android.app.Application
import com.example.alcolyze.data.AlcolyzeDatabase

/** Punto di partenza dell'app: crea e tiene l'unico database, condiviso da tutto il resto. */
class AlcolyzeApplication : Application() {
    // Il database viene creato solo la prima volta che serve davvero.
    val database by lazy { AlcolyzeDatabase.getDatabase(this) }
}
