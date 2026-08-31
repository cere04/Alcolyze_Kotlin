ALCOLYZE

App Android per stimare il tasso alcolemico (BAC) e un punteggio di
"euforia" a partire dai drink e dai pasti registrati durante una serata.
Il calcolo non è una stima "a occhio": un modello scientifico ricostruisce
minuto per minuto quanto alcol entra nel sangue e quanto ne smaltisce il
fegato.

Progetto realizzato per il corso di Programmazione Mobile.


----------------------------------------------------------------------
1. COSA FA L'APP
----------------------------------------------------------------------
L'utente inserisce i propri dati (sesso, età, peso, altezza, tipo di
patente) e poi, durante la serata, registra i drink che beve e i pasti
che fa. L'app mostra in tempo reale:
- il tasso alcolemico stimato e la sua evoluzione nelle ore successive;
- un punteggio di "euforia" da 0 a 10, che tiene conto anche di quanto
  in fretta si sta bevendo;
- quanto manca per poter guidare (o prima di superare il limite legale);
- la fascia di rischio in cui ci si trova, con una breve spiegazione;
- statistiche della serata e statistiche storiche.


----------------------------------------------------------------------
2. FUNZIONALITÀ PRINCIPALI
----------------------------------------------------------------------
- Modello farmacocinetico: assorbimento graduale dallo stomaco (che
  dipende da quanto si è mangiato) ed eliminazione epatica, risolti
  minuto per minuto.
- Due indicatori circolari live (Intossicazione ed Euforia) con
  segnalino del picco previsto.
- Barra "Sicurezza" a 4 fasce con cursore che segue il tasso.
- Conto alla rovescia per la guida, diverso per patente standard e
  neopatentati/professionisti.
- Registro della serata: ogni drink e pasto, con possibilità di
  rimuoverli (i valori vengono ricalcolati).
- Grafico dell'andamento con pallino del picco toccabile.
- Listino drink con filtri per categoria e ricerca; possibilità di
  creare, modificare ed eliminare drink personalizzati.
- Ripresa automatica della serata alla riapertura dell'app; chiusura
  automatica quando si torna sobri o dopo 24 ore.
- Statistiche per periodo (settimana / mese / 3 mesi / anno) con podio
  dei drink più bevuti.
- Tema chiaro/scuro con preferenza salvata.
- Pulsante SOS che apre il telefono precompilato sul 112.


----------------------------------------------------------------------
3. TECNOLOGIE E ARCHITETTURA
----------------------------------------------------------------------
- Linguaggio: Kotlin
- Interfaccia: Jetpack Compose (Material 3)
- Navigazione: Navigation Compose
- Stato: un solo ViewModel condiviso da tutte le schermate
- Database locale: Room (SQLite) con 5 tabelle
- Listino drink condiviso: Firebase Firestore (opzionale, vedi punto 6)
- Asincronia: coroutine di Kotlin
- Grafici e indicatori: disegnati a mano su Canvas, senza librerie esterne


----------------------------------------------------------------------
4. REQUISITI
----------------------------------------------------------------------
Per compilare:
- Android Studio recente (versione che supporti Android Gradle Plugin 9.x)
- JDK 17
- Gradle 9.3.1 (scaricato in automatico dal wrapper incluso)
- Connessione a internet al primo build (per scaricare le dipendenze)

Per eseguire:
- Un dispositivo Android o un emulatore con Android 8.0 (API 26) o superiore

Versioni principali usate:
- Kotlin 2.2.10
- Compose BOM 2024.09.00
- Room 2.8.0
- Navigation Compose 2.8.4
- Firebase BOM 34.16.0
- versionName "1.0", applicationId "com.example.alcolyze"


----------------------------------------------------------------------
5. COME COMPILARE ED ESEGUIRE
----------------------------------------------------------------------
1. Clonare il repository

2. Aprire la cartella del progetto con Android Studio e attendere la
   sincronizzazione di Gradle.

3. Selezionare un dispositivo o avviare un emulatore.

4. Premere "Run" (oppure da terminale, nella cartella del progetto):
   ./gradlew installDebug        (installa la app di debug)
   ./gradlew assembleDebug       (crea solo il file .apk)


----------------------------------------------------------------------
6. FIREBASE (LISTINO DRINK ONLINE)
----------------------------------------------------------------------
Firebase Firestore serve SOLO a scaricare il listino dei drink standard
("standard_drinks"). Non è indispensabile:
- al primo avvio con connessione, il listino viene scaricato e salvato
  in locale;
- senza connessione (o senza configurazione Firebase) l'app funziona
  comunque: restano disponibili i drink già scaricati in precedenza e
  quelli personalizzati creati dall'utente, e si può usare tutto il
  resto dell'app.

Il file di configurazione app/google-services.json è incluso nel
repository per comodità di valutazione del progetto. Se si vuole usare
un proprio progetto Firebase, sostituire quel file con il proprio.


----------------------------------------------------------------------
7. STRUTTURA DEL PROGETTO
----------------------------------------------------------------------
app/src/main/java/com/example/alcolyze/
  AlcolyzeApplication.kt   avvio dell'app, crea il database
  MainActivity.kt          schermata Android iniziale
  data/                    database, tabelle, query, repository
  model/                   i dati usati da schermate e calcoli
  utils/                   il motore di calcolo (BacCalculator)
  viewmodel/               stato dell'app e azioni dell'utente
  ui/                      interfaccia:
    AlcolyzeApp.kt         schermata principale + barra in basso
    screens/               le schermate intere
    components/            pezzi riutilizzabili
    theme/                 colori, tema, testi
    util/                  formattazione e scala dei grafici
app/src/test/              test sul computer
app/src/androidTest/       test su dispositivo
app/src/main/res/          risorse (icone, stringhe, tema di sistema)
app/sampledata/            materiale di contorno, non incluso nell'app
                           (relazione, mockup, studi, questo file)


----------------------------------------------------------------------
10. AVVERTENZE
----------------------------------------------------------------------
Le stime fornite dall'app sono calcolate con modelli clinici, ma
restano puramente indicative: la risposta all'alcol varia molto da
persona a persona. L'app NON sostituisce un etilometro.
Non mettersi mai alla guida dopo aver bevuto.


----------------------------------------------------------------------
11. AUTORE
----------------------------------------------------------------------
Autori: Cesaretti Nicola s1116009, Andrenacci Laura s1115933
