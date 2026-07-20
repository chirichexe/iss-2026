# Executive Summary: Sprint 2 — Analisi & Checklist

---

## 🔴 1. Refusi Critici nel Testo (`sprint2.typ`)

| Riga | Tipo | Problema & Azione |
|------|------|-------------------|
| **401** | **Bozza inline** | Testo incollato per errore: `...realizza l'IO`**`Come posso evolvere lo sprint1 naturalmente nello sprint2?`**`Port;`. Rimuovere la frase. |
| **232-234**| **Bozza inline** | Blocco di note lasciate nel testo: `????????...PERCHE USIAMO EVENT...????????`. Rimuovere o integrare. |
| **230** | **Link mancante** | `Il link allo script può essere trovato: QUA` → inserire il link GitHub reale allo script ESP32. |
| **279** | **Link mancante** | `Il Codice può essere trovato QUI` → inserire link alla gestione del timer sonar in `cargoservice.qak`. |
| **293** | **Placeholder** | `QuALCHE ALTRO SNIPPET` → sostituire con lo snippet di codice o rimuovere l'intestazione. |
| **82-86** | **Commento di bozza**| Blocco `/* ... */` con vecchie note di Sprint 1. Rimuovere. |

---

## ❌ 2. Sezioni Mancanti (da compilare)

| Riga | Sezione | Contenuto Richiesto |
|------|---------|---------------------|
| **426-438** | **5. Project** | Descrivere la struttura a 5 moduli (`cargoservice`, `cargorobot`, `devices`, `ioport-backend`, `ioport-frontend`) e il ruolo degli attori adapter (`sonaradapter`, `ledadapter`, `markerdevice`). |
| **444-456** | **6. Test Plans** | Aggiungere la tabella dei test (come Sprint 1): `load_request` (accepted/retrylater/refused), timer 30s deposito, timer 3s sonar (Out of Service / Working), marcatura slot5, consegna slot. |
| **545** | **8. Maintenance** | Limitazioni note (es. gestione errori rete MQTT, recupero guasti fisici del robot). |
| **553-557** | **9. Sintesi** | Inserire l'immagine dell'architettura finale di Sprint 2 e definire i goal per lo Sprint 3. |

---

## ⚠️ 3. Inconsistenze Documento vs. Codice Reale

1. **Nomi Cartelle e Task Gradle nel Deployment (righe 481-529):**
   - Doc scrive `robot` → Il nome reale della cartella è `cargorobot`
   - Doc scrive `customer` → Il nome reale della cartella è `ioport` (contesto QAK)
   - Doc scrive `IOPortServer` su `customer` → Il backend reale è `ioport-backend` (Java/Javalin)
   - Doc indica `./gradlew runCustomer` e `runIOPortServer` → I comandi reali sono `./gradlew runIoport` (in `ioport`) e `./gradlew run` (in `ioport-backend`).

2. **Evoluzione Messaggi Sonar (riga 413):**
   - Il doc elenca `sonardata` tra le interfacce preservate.
   - **Realtà:** `sonardata` è stato rimosso e sostituito da `Event wall_sonardata` (MQTT) + `Dispatch incoming_sonar` (TCP). Aggiornare il testo.

3. **ESP32 vs PicoW (righe 94, 325, 327, 332):**
   - La nota a riga 94 spiega l'uso dell'ESP32, ma nelle righe 325-332 si parla ancora di "PicoW". Uniformare a ESP32.

4. **Typo nel QAK (`cargoservice.qak` riga 64):**
   - Stampa `println("cargojewish | ENGAGED...")` anziché `cargoservice`.

---

## 📐 4. Mappa Sintetica dell'Architettura Reale

```
ESP32 Sonar ──(MQTT: wall_sonardata)──► sonaradapter (8052) ──(TCP: incoming_sonar)──► cargoservice (8050)
Browser ◄──(WS)── ioport-backend (8086) ◄──(CoAP Observe)─────────────────────────────┤ (Hold.java)
Browser ───(HTTP POST)──────────────────► ioport-backend ──(TCP: load_request)──────────┤
cargoservice ──(TCP: moverobot)──► cargorobot (8053) ──(TCP / MQTT alarm)──► robotsmart (8020) ──► wenv (8090)
cargoservice ──(TCP: led_ctrl)───► ledadapter (8052) ──(MQTT: led_event)────► ESP32 LED
```

---

## ✅ 5. Checklist per la Consegna

1. [ ] Correggere i 6 refusi di testo a righe 401, 232-234, 230, 279, 293, 82-86.
2. [ ] Aggiornare la sezione Deployment con i nomi cartelle reali (`cargorobot`, `ioport-backend`, `ioport`).
3. [ ] Compilare la sezione **Project** con l'elenco dei 5 moduli.
4. [ ] Compilare la sezione **Test Plans** (formato tabella come Sprint 1).
5. [ ] Inserire il diagramma dell'architettura finale nella **Pagina di Sintesi**.
6. [ ] Rimuovere il typo `cargojewish` a riga 64 in `cargoservice.qak`.
