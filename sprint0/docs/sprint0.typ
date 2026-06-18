// ─────────────────────────────────────────────────────────────────────────────
//  sprint0.typ  –  contenuto
// ─────────────────────────────────────────────────────────────────────────────

#import "../../shared/template.typ": iss-template, iss-table, nota, domanda

#show: iss-template.with(
  title:         "Maritime CargoService",
  subtitle:      "Sprint 0",
  course:        "Ingegneria dei Sistemi Software",
  university:    "Alma Mater Studiorum · Università di Bologna",
  academic-year: "2025/2026",
  authors:       ("Davide Chirichella", "Gabriele Doti", "Daniele Maccagnan"),
)

// ═════════════════════════════════════════════════════════════════════════════
= Introduction
// ═════════════════════════════════════════════════════════════════════════════

Una compagnia di trasporto marittimo di container (d'ora in poi _la committente_)
intende automatizzare le operazioni di carico dei container nella stiva della nave
(d'ora in poi *hold*). A tal fine prevede di impiegare un robot a guida differenziale
(Differential Drive Robot, d'ora in poi *cargorobot*).

L'obiettivo dello Sprint 0 è formalizzare i requisiti forniti dalla committente in modo
preciso e non ambiguo, costruire un primo modello logico dei macro-componenti del
sistema, evidenziare il _core business_, motivare la scelta del linguaggio di
modellazione e definire un primo insieme di piani di test funzionali.

Ogni scelta è strettamente ancorata ai requisiti: il modello qui presentato serve a
catturare il comportamento richiesto, senza anticipare decisioni progettuali o scelte
implementative che verranno affrontate negli sprint successivi.

== Glossario

#iss-table(
  columns: (auto, 1fr),
  [*Termine*], [*Definizione*],
  [*hold*],
    [Area rettangolare piatta che costituisce la stiva della nave. Contiene
     slot1--slot4 (immagazzinamento definitivo), slot5 (transito) e l'IOPort
     (punto di ingresso/uscita).],
  [*slot1--slot4*],
    [Quattro aree distinte della hold, ciascuna capace di contenere un container.
     Costituiscono la destinazione finale di ogni container caricato.],
  [*slot5*],
    [Area speciale in cui il cargorobot deposita temporaneamente un container
     prima della collocazione definitiva, in attesa dell'etichettatura.],
  [*IOPort*],
    [Dispositivo di ingresso/uscita dotato di: pushbutton (richieste di carico),
     display (stato e messaggi) e sonar (rilevazione presenza container).],
  [*customer*],
    [Attore esterno che interagisce fisicamente con l'IOPort per richiedere il
     carico e per depositare il container nell'area del sonar.],
  [*cargorobot*],
    [Robot a guida differenziale (DDR) previsto dal dominio applicativo,
     responsabile della movimentazione fisica dei container nella hold.],
  [*cargoservice*],
    [Servizio software principale da costruire. Orchestra il ciclo di carico:
     ricezione richieste, verifica precondizioni, guida del cargorobot,
     etichettatura, aggiornamento display, controllo LED.],
  [*marker device*],
    [Dispositivo fisico in slot5 che appone un codice a barre al container
     e segnala il completamento dell'etichettatura.],
  [*sonar*],
    [Sensore associato all'IOPort. Rileva la presenza di un container quando
     misura D < D#sub[FREE]/2 per almeno 3 secondi.],
  [*D#sub[FREE]*],
    [Distanza soglia del sonar. Se D > D#sub[FREE] per almeno 3 secondi si
     presuppone un malfunzionamento.],
  [*LED*],
    [Indicatore luminoso che lampeggia durante lo stato _engaged_.],
  [*engaged*],
    [Stato in cui una richiesta è stata accettata e l'operazione è in corso.],
  [*disengaged*],
    [Stato normale di attesa: nessuna operazione in corso.],
  [*Out of service*],
    [Stato anomalo (D > D#sub[FREE] per ≥ 3 s): cargoservice risponde
     _retrylater_ a ogni richiesta.],
)

// ═════════════════════════════════════════════════════════════════════════════
= Requirements
// ═════════════════════════════════════════════════════════════════════════════

== Requisiti funzionali

- Il servizio principale da costruire è *cargoservice*.
- Il customer invia a cargoservice una richiesta di carico tramite il pushbutton dell'IOPort.
- Se l'IOPort è occupata o il sistema è _Out of service_, cargoservice risponde *_retrylater_*.
- Se tutti gli slot1--slot4 sono occupati, cargoservice *rifiuta* la richiesta.
- Altrimenti, cargoservice entra in stato _engaged_, riserva uno slot libero e
  restituisce al customer il nome dello slot riservato.
- La scelta dello slot non introduce, nei requisiti, criteri di ottimizzazione:
  è sufficiente selezionare uno degli slot liberi.
- Fintanto che il sistema è _engaged_, un *LED lampeggia*.
- Dopo l'accettazione, il customer ha un tempo prefissato (es. 30 s) per depositare
  il container nell'area del sonar.
- Se il customer non deposita entro il timeout, il sistema torna _disengaged_.
- Quando la presenza del container è confermata dal sonar, cargoservice comanda al
  cargorobot di spostare il container *dall'IOPort a slot5*.
- Il marker device etichetta il container in slot5 e *segnala il completamento*.
- cargoservice comanda al cargorobot di spostare il container *da slot5 allo slot riservato*.
- Il display mostra in ogni momento lo *stato corrente della hold*.
- Il display mostra il messaggio *"Service working"* durante il normale funzionamento.
- Se il sonar misura D > D#sub[FREE] per ≥ 3 s, il sistema passa _Out of service_ e
  mostra *"Out of service"* sul display.
- Il sonar rileva la presenza di un container quando misura D < D#sub[FREE]/2 per ≥ 3 s.

== Requisiti non funzionali

- La hold è un'area rettangolare piatta con slot1--slot4, slot5 e IOPort.
- L'IOPort è dotata di pushbutton, display e sonar.
- Il cargorobot è l'entità software/robotica che dovrà governare un DDR; i requisiti
  non stabiliscono ancora quale parte sia già disponibile e quale debba essere
  realizzata.
- Il tempo massimo di attesa per il deposito è un parametro prefissato (es. 30 s).
- La soglia temporale per le rilevazioni del sonar è di almeno 3 secondi.

== Domande aperte alla committente

#domanda[
  *D1. Posizione dell'IOPort nella mappa.* I requisiti indicano che il cargorobot
  sposta il container _dall'IOPort a slot5_, lasciando intendere che l'IOPort sia
  una cella praticabile. Come si colloca nella griglia?
]

#domanda[
  *D2. Trasporto fisico dei container.* I requisiti non specificano come il
  cargorobot trasporti fisicamente i container. In questa fase interessa solo il
  fatto osservabile che il container venga spostato tra le aree richieste; il dettaglio
  meccanico va chiarito senza attribuire al robot responsabilità di business logic.
]

#domanda[
  *D3. Area di copertura del sonar.* La _sensor area_ coincide con l'area
  dell'IOPort o è una cella adiacente? Il chiarimento è essenziale per definire
  il comportamento di rilevazione e movimentazione.
]

#domanda[
  *D4. Interfaccia del pushbutton.* Il pushbutton è un dispositivo fisico
  dedicato oppure può essere realizzato tramite un'interfaccia software?
]

// ═════════════════════════════════════════════════════════════════════════════
= Requirement analysis
// ═════════════════════════════════════════════════════════════════════════════

== Core business

Il *core business* del sistema è la gestione del ciclo di carico di un container.
La responsabilità della sequenza applicativa resta in *cargoservice*: il cargorobot è
coinvolto per eseguire spostamenti richiesti dal servizio, non per decidere se una
richiesta debba essere accettata, rifiutata o sospesa.
La sequenza principale ricavata dai requisiti è:

+ Il customer preme il pushbutton dell'IOPort.
+ cargoservice verifica le precondizioni (stato sistema, IOPort, disponibilità slot).
+ Se soddisfatte: stato _engaged_, prenotazione slot, notifica al customer.
+ Il customer deposita il container nell'area del sonar entro il timeout.
+ cargoservice comanda al cargorobot di spostare il container da IOPort a slot5.
+ Il marker device etichetta il container e segnala il completamento.
+ cargoservice comanda al cargorobot di spostare il container da slot5 allo slot
  riservato; il sistema torna _disengaged_.

== Macro-componenti e natura software

#iss-table(
  columns: (auto, 1fr, auto),
  [*Componente*], [*Ruolo*], [*Stato*],
  [*cargoservice*],
    [Orchestratore principale. Natura reattiva (richieste) e proattiva (comandi).],
    [Da sviluppare],
  [*cargorobot*],
    [Entità che governa il DDR per la movimentazione fisica richiesta dal cargoservice.],
    [Da chiarire / da sviluppare],
  [*IOPort*],
    [Interfaccia con il customer (pushbutton + display). Fisico fornito; sw da sviluppare.],
    [Fisico fornito \ sw da sviluppare],
  [*sonar*],
    [Sensore di distanza. Fisico fornito; driver sw da sviluppare.],
    [Fisico fornito \ sw da sviluppare],
  [*marker device*],
    [Etichettatura in slot5. Fisico fornito; sw da sviluppare.],
    [Fisico fornito \ sw da sviluppare],
  [*LED*],
    [Indicatore stato _engaged_. Fisico fornito; sw da sviluppare.],
    [Fisico fornito \ sw da sviluppare],
  [*hold*],
    [Struttura dati per lo stato della stiva (occupazione slot). Passivo.],
    [Da sviluppare],
)

== Motivazione dell'uso del linguaggio QAK

Il linguaggio QAK non è assunto come vincolo a priori; viene usato perché consente di
produrre un primo modello eseguibile che cattura i requisiti in termini di stati,
messaggi e transizioni osservabili. La motivazione deriva da tre evidenze ricavate
direttamente dai requisiti:

- *Natura reattiva e proattiva di cargoservice*: il servizio risponde a stimoli
  esterni (richieste, segnalazioni sonar e marker device) e avvia autonomamente
  sequenze di azioni (comandi al cargorobot, aggiornamenti display). Un POJO
  (Plain Old Java Object), componente passivo attivato da chiamate sincrone,
  non cattura questo comportamento.

- *Sistema event-driven*: sonar, marker device, IOPort e cargorobot producono o
  consumano informazioni che non sono naturalmente descrivibili come una singola
  chiamata sincrona. È quindi utile esplicitare i messaggi del dominio.

- *Riduzione dell'abstraction gap*: il linguaggio QAK permette di rappresentare le
  entità rilevanti come attori autonomi e message-driven, mantenendo vicine le frasi
  dei requisiti e la loro formalizzazione. Il modello risultante può essere eseguito
  e usato come base per test funzionali già nello Sprint 0.

== Formalizzazione dei messaggi QAK

La seguente formalizzazione non introduce ancora scelte di progetto: elenca i messaggi
necessari per esprimere i requisiti. *Request/Reply* viene usato quando il requisito
prevede una risposta osservabile; *Dispatch* quando il requisito parla di una
segnalazione o di un aggiornamento senza risposta diretta.

=== customer / IOPort → cargoservice

```
Request  load_request    : loadRequest(none)
Reply    load_accepted   : loadAccepted(slotID)   for load_request
Reply    load_retrylater : loadRetryLater(none)    for load_request
Reply    load_refused    : loadRefused(none)       for load_request
```

=== sonar → cargoservice

```
Dispatch sonar_data         : sonarData(distance)
Dispatch container_detected : containerDetected(none)
Dispatch sonar_failure      : sonarFailure(none)
```

=== cargoservice → cargorobot

```
Request  move_to_slot5 : moveToSlot5(none)
Reply    move_done     : moveDone(none)     for move_to_slot5

Request  move_to_slot  : moveToSlot(slotID)
Reply    move_done     : moveDone(none)     for move_to_slot
```

=== marker device → cargoservice

```
Dispatch marking_done : markingDone(containerID)
```

=== cargoservice → display / LED

```
Dispatch display_hold_state : holdState(state)
Dispatch display_status     : displayStatus(message)
Dispatch led_on             : ledOn(none)
Dispatch led_off            : ledOff(none)
```

== Contesti logici

#iss-table(
  columns: (auto, 1fr),
  [*Contesto*], [*Componenti e responsabilità*],
  [*ctxCargoService*],
    [Contiene l'attore cargoservice. Nucleo del comportamento richiesto e punto di
     orchestrazione del ciclo di carico.],
  [*ctxIO*],
    [Raggruppa le entità legate all'IOPort e ai dispositivi citati dai requisiti:
     ioport, sonar, led, markerdevice.],
  [*ctxRobot*],
    [Raggruppa le entità legate al cargorobot e alla movimentazione richiesta.],
)

== Schema della hold

```
Legenda:  H = HOME   S = sonar/sensor area   1–4 = slot1–4
          5 = slot5  I = IOPort  X = ostacolo  . = libero

       col0  col1  col2  col3  col4  col5  col6
riga0 [  H ][  S ][  . ][  . ][  . ][  . ][  . ]
riga1 [  . ][  1 ][  X ][  X ][  2 ][  . ][  . ]
riga2 [  . ][  . ][  . ][  . ][  X ][  5 ][  . ]
riga3 [  . ][  3 ][  X ][  X ][  4 ][  . ][  . ]
riga4 [  . ][  . ][  . ][  . ][  . ][  . ][  . ]
riga5 [  I ][  X ][  X ][  X ][  X ][  X ][  X ]
```

#nota[
  La posizione esatta dell'IOPort e del sonar richiede conferma dalla committente
  (Domande D1 e D3). Lo schema è una prima interpretazione fedele alla figura
  allegata ai requisiti.
]

// ═════════════════════════════════════════════════════════════════════════════
= Test plan
// ═════════════════════════════════════════════════════════════════════════════

I test funzionali verificano il comportamento del sistema rispetto ai requisiti,
in modo indipendente dall'implementazione.

== Accettazione della richiesta

*Precondizioni:* _disengaged_, non _Out of service_, IOPort libera, almeno uno slot libero. \
*Azioni:* il customer preme il pushbutton. \
*Risultato:* risposta _load\_accepted_ con slot riservato; sistema _engaged_; LED lampeggiante.

== Rifiuto (hold piena)

*Precondizioni:* _disengaged_, non _Out of service_, IOPort libera, slot1--slot4 tutti occupati. \
*Azioni:* il customer preme il pushbutton. \
*Risultato:* risposta _load\_refused_; sistema _disengaged_; LED spento.

== Retrylater (Out of service)

*Precondizioni:* sistema _Out of service_. \
*Azioni:* il customer preme il pushbutton. \
*Risultato:* risposta _load\_retrylater_; stato immutato.

== Retrylater (IOPort occupata)

*Precondizioni:* _disengaged_, non _Out of service_, IOPort occupata. \
*Azioni:* il customer preme il pushbutton. \
*Risultato:* risposta _load\_retrylater_; sistema _disengaged_.

== Timeout deposito container

*Precondizioni:* sistema _engaged_. \
*Azioni:* il customer non deposita il container entro il tempo prefissato. \
*Risultato:* sistema _disengaged_; LED spento.

== Ciclo completo di carico

*Precondizioni:* _disengaged_, non _Out of service_, IOPort libera, slot disponibile. \
*Azioni:* (a) pushbutton → (b) _load\_accepted_ → (c) deposito container → (d) rilevazione sonar → (e) cargorobot: IOPort→slot5 → (f) etichettatura → (g) cargorobot: slot5→slot riservato. \
*Risultato:* container nello slot riservato; display aggiornato con _"Service working"_; sistema _disengaged_; LED spento.

== Malfunzionamento sonar

*Precondizioni:* sistema operativo. \
*Azioni:* sonar misura D > D#sub[FREE] per ≥ 3 s consecutivi. \
*Risultato:* sistema _Out of service_; display mostra _"Out of service"_.

== Rilevazione container (sonar)

*Precondizioni:* sistema _engaged_; sensor area vuota. \
*Azioni:* deposito container; sonar misura D < D#sub[FREE]/2 per ≥ 3 s. \
*Risultato:* cargoservice riceve _container\_detected_ e avvia la movimentazione.

// ═════════════════════════════════════════════════════════════════════════════
= Project
// ═════════════════════════════════════════════════════════════════════════════

Dall'analisi dei requisiti si propone, come ipotesi iniziale, una struttura a *tre
sprint* con integrazione progressiva dei componenti. La suddivisione serve a
organizzare il lavoro e i test, non a fissare decisioni architetturali definitive.

== Sprint 1: Core business

*Goal:* realizzare il primo nucleo eseguibile di cargoservice con collaboratori
simulati.

Funzionalità: ciclo di carico completo con collaboratori simulati, modello dello
stato della hold, LED simulato, timeout.

*Test:* TF01, TF02, TF05, TF06 (con mock).

== Sprint 2: IOPort, display e sonar

*Goal:* rendere esplicita l'interazione con IOPort, display e sonar a livello software.

Funzionalità: ioport come interfaccia web, sonar simulato (rilevazione container
e malfunzionamento), aggiornamento display con stato e messaggi.

*Test:* TF03, TF04, TF07, TF08.

== Sprint 3: Dispositivi fisici

*Goal:* integrare, dove disponibili, i dispositivi fisici o i servizi forniti e
verificare il comportamento end-to-end.

Funzionalità: cargorobot e servizio DDR disponibile, sonar fisico, marker device
fisico, LED fisico.

*Test:* TF06 (end-to-end su hardware), TF07, TF08.

// ─────────────────────────────────────────────────────────────────────────────
//  Allegati - Team di lavoro
// ─────────────────────────────────────────────────────────────────────────────
#pagebreak()

= Team di lavoro

#iss-table(
  columns: (1fr, auto),
  [*Nome e Cognome*], [*Ruolo*],
  [Davide Chirichella], [Membro del gruppo],
  [Gabriele Doti],     [Membro del gruppo],
  [Daniele Maccagnan], [Membro del gruppo],
)
