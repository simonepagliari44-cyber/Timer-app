# Timer

App Android in Kotlin con Jetpack Compose e Material 3: timer multipli e cronometro.

## Funzionalità

- **Timer multipli**: aggiungi quanti timer vuoi (FAB "+"); ogni timer è indipendente con
  tre campi di input **Ore / Minuti / Secondi**, pulsanti Start / Stop / Reset e tempo
  rimanente in formato `HH:MM:SS`.
  Numerazione automatica sequenziale anche dopo la rimozione.
  Il countdown usa **deadline + alarm esatti** (`AlarmManager.setExactAndAllowWhileIdle`):
  sopravvive all'uccisione del processo e al reboot (ridefinito da `BOOT_COMPLETED`),
  quindi i timer durano anche molte ore in background.
  Ogni timer in esecuzione mostra una **notifica persistente nella barra delle notifiche**
  con il tempo rimanente e il pulsante **Stop**; a scadenza suona la
  **suoneria di allarme di sistema** con notifica e pulsante **Stop** per fermarla.
- **Cronometro**: Start / Stop / Reset, visualizzazione in formato `mm:ss.SSS`.
- **Material 3**: `TabRow` con le tab "Timer" e "Cronometro", `LazyColumn` per i timer,
  cronometro centrato.
- State + `StateFlow` gestiti tramite ViewModel, timing tramite coroutines.

## Requisiti

- JDK 17+
- Android SDK (compileSdk 34)
- Gradle 8.9 (wrapper incluso)

## Build

```bash
./gradlew assembleDebug    # APK debug: app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease  # APK release firmato: app/build/outputs/apk/release/app-release.apk
./gradlew lintDebug        # analisi statica
```

La versione release è firmata con un keystore di sviluppo incluso in
`app/keystore/timer-release.p12` (password: `timer123`, alias: `timer`).
**Per pubblicare su Play Store rigenera il keystore e non committarlo.**

## Struttura

```
app/src/main/java/com/simonecompany/timer/
├── MainActivity.kt              # Activity unica, UI con TabRow
├── data/TimerStore.kt           # Persistenza timer in esecuzione (deadline)
├── service/
│   ├── AlarmScheduler.kt        # Alarm esatti (sopravvivono a processo/reboot)
│   ├── TimerAlarmReceiver.kt    # Accensione alarm + ripristino dopo reboot
│   └── TimerRingService.kt      # Suoneria + notifica con pulsante Stop
├── ui/
│   ├── TimerSection.kt          # Lista timer multipli
│   └── StopwatchSection.kt      # Cronometro
├── ui/theme/                    # Tema Material 3
└── viewmodel/
    ├── TimerViewModel.kt        # StateFlow + coroutines + pianificazione alarm
    └── StopwatchViewModel.kt    # StateFlow + coroutines
```

## Notifiche

Su Android 13+ (API 33) l'app richiede il permesso `POST_NOTIFICATIONS` al primo timer
aggiunto. Il manifest dichiara anche `VIBRATE`, `FOREGROUND_SERVICE` e
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`.