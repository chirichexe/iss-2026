// -----------------------------------------------------------------------------
//  sprint0.typ  -  contenuto
// -----------------------------------------------------------------------------

#import "../../shared/template.typ": iss-template, iss-table, nota, domanda

#show: iss-template.with(
  title:         "Maritime CargoService",
  subtitle:      "Sprint 0",
  course:        "Ingegneria dei Sistemi Software",
  university:    "Alma Mater Studiorum . Università di Bologna",
  academic-year: "2025/2026",
  authors:       ("Davide Chirichella", "Gabriele Doti", "Daniele Maccagnan"),
)

// =============================================================================
= Introduction
// =============================================================================

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

La traccia del progetto può essere scaricata dal seguente #link("https://anatali.github.io/issLab2026/_static/docs/Protobook.pdf")[link]

// =============================================================================
= Requirements
// =============================================================================

== Requisiti funzionali

- Il servizio da realizzare è *cargoservice*: il customer gli invia una richiesta di carico
  tramite il pushbutton dell'IOPort.
- L'IOPort è un dispositivo di ingresso/uscita dotato di: pushbutton (richieste di carico),
     display (stato e messaggi) e sonar (rilevazione presenza container).
- Se l'IOPort è occupata o il sistema è _Out of service_, cargoservice risponde *_retrylater_*.
- Se tutti gli slot1--slot4 (ciascuno capace di contenere un container) sono occupati, cargoservice *rifiuta* la richiesta.
- Altrimenti, cargoservice entra in stato _engaged_, riserva uno slot libero (senza criteri
  di ottimizzazione) e restituisce al customer il nome dello slot riservato.
- Fintanto che il sistema è _engaged_, un *LED lampeggia*.
- Dopo l'accettazione, il customer ha un tempo prefissato (es. 30 s) per depositare
  il container nell'area del sonar.
- Se il customer non deposita entro il timeout, il sistema torna _disengaged_.
- Quando la presenza del container è confermata dal sonar, cargoservice comanda al cargorobot di spostare il container *dall'IOPort* allo *slot5* (adibito al deposito temporaneo di un container prima della collocazione definitiva).
- Il marker device etichetta il container in slot5 e *segnala il completamento*.
- cargoservice comanda al cargorobot di spostare il container *da slot5 allo slot riservato*.
- Il display mostra in ogni momento lo *stato corrente della hold* e il messaggio
  *"Service working"* durante il normale funzionamento.
- Se il sonar misura D > D#sub[FREE] per >= 3 s, il sistema passa _Out of service_ e
  mostra *"Out of service"* sul display.
- Il sonar rileva la presenza di un container quando misura D < D#sub[FREE]/2 per >= 3 s.

== Requisiti non funzionali

- La hold è un'area rettangolare piatta con slot1--slot4, slot5 e IOPort.
- Il cargorobot è l'entità software/robotica che dovrà governare un DDR; i requisiti
  non stabiliscono ancora quale parte sia già disponibile e quale debba essere
  realizzata.
- Il tempo massimo di attesa per il deposito e la soglia di rilevazione sonar sono
  parametri prefissati (es. 30 s e 3 s rispettivamente).

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
  *D4. Interfaccia del pushbutton / IOPort.* Il pushbutton è un dispositivo fisico
  dedicato oppure può essere realizzato tramite un'interfaccia software (es. pagina web)?
  Se si opta per una web GUI, l'IOPort diventa un microservizio indipendente: il
  customer interagisce con essa tramite browser (pulsante virtuale + display), e l'IOPort
  comunica con cargoservice. Le responsabilità dei due servizi risultano così
  completamente disaccoppiate e sviluppabili in parallelo.
]

#domanda[
  *D5. Richieste concorrenti.* Il sistema deve bufferizzare più richieste di carico
  contemporanee, oppure una nuova richiesta ricevuta mentre il sistema è _engaged_
  viene semplicemente rifiutata / respinta con _retrylater_ senza accodamento?
  Dai requisiti si interpreta che le richieste *non siano bufferizzate*: se il sistema è
  occupato, la risposta è immediata (_retrylater_ o _refused_) senza code di attesa.
]

#domanda[
  *D6. LED: fisico o virtuale.* Il LED indicatore dello stato _engaged_ deve essere
  necessariamente un dispositivo fisico oppure è accettabile una rappresentazione
  virtuale (es. elemento grafico nel display dell'IOPort o nella web GUI)?
]

// =============================================================================
= Requirement analysis
// =============================================================================

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
    [Orchestratore principale. Natura reattiva (richieste in arrivo) _e_ proattiva
     (comandi al cargorobot, aggiornamenti display). Non può restare privo di
     controllo: da requisito deve rispondere _e_ agire autonomamente.],
    [Da sviluppare],
  [*cargorobot*],
    [Entità che governa il DDR per la movimentazione fisica richiesta dal cargoservice.
     La scelta POJO vs. microservizio è rimandata all'analisi: se fosse un POJO,
     il cargoservice gli cederebbe il controllo (trasferimento di controllo sincrono)
     e non potrebbe reagire ad altri eventi durante la movimentazione. Se invece
     fosse un microservizio, la comunicazione sarebbe asincrona e message-driven,
     con un disaccoppiamento molto maggiore. Forti motivazioni spingono verso la
     seconda opzione, ma la decisione è oggetto degli sprint successivi.],
    [Da chiarire / da sviluppare],
  [*IOPort*],
    [Interfaccia con il customer (pushbutton + display). Se realizzata come
     microservizio separato (es. web GUI), le responsabilità sono completamente
     disaccoppiate da cargoservice e i due possono essere sviluppati in parallelo;
     l'unica interfaccia condivisa è il messaggio _load\_request_ definito in
     questo sprint.],
    [Fisico fornito \ sw da sviluppare],
  [*sonar*],
    [Sensore di distanza. Fisico fornito; driver sw da sviluppare.],
    [Fisico fornito \ sw da sviluppare],
  [*marker device*],
    [Etichettatura in slot5. Fisico fornito; sw da sviluppare.],
    [Fisico fornito \ sw da sviluppare],
  [*LED*],
    [Indicatore stato _engaged_. Può essere fisico o virtuale (es. elemento grafico
     nella web GUI). La scelta dipende dall'infrastruttura disponibile (D6).],
    [Fisico o virtuale \ sw da sviluppare],
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

=== customer / IOPort -> cargoservice

```
Request  load_request    : loadRequest(none)
Reply    load_accepted   : loadAccepted(slotID)   for load_request
Reply    load_retrylater : loadRetryLater(none)    for load_request
Reply    load_refused    : loadRefused(none)       for load_request
```

=== sonar -> cargoservice

```
Dispatch sonar_data         : sonarData(distance)
Dispatch container_detected : containerDetected(none)
Dispatch sonar_failure      : sonarFailure(none)
```

#nota[
  La scelta _Dispatch_ (e non _Request_) per i messaggi del sonar è una
  *ipotesi di analisi*, non una scelta di progetto: il sonar segnala eventi
  al cargoservice senza aspettarsi una risposta diretta. Se l'analisi rivelasse
  la necessità di una conferma (es. handshake di rilevazione), il tipo del
  messaggio andrà rivisto nello sprint 1.
]

=== cargoservice -> cargorobot

```
Request  move_to_slot5 : moveToSlot5(none)
Reply    move_done     : moveDone(none)     for move_to_slot5

Request  move_to_slot  : moveToSlot(slotID)
Reply    move_done     : moveDone(none)     for move_to_slot
```

=== marker device -> cargoservice

```
Dispatch marking_done : markingDone(containerID)
```

=== cargoservice -> display / LED

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
Legenda:  H = HOME   S = sonar/sensor area   1-4 = slot1-4
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

// =============================================================================
= Test plan
// =============================================================================

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
*Azioni:* (a) pushbutton -> (b) _load\_accepted_ -> (c) deposito container -> (d) rilevazione sonar -> (e) cargorobot: IOPort->slot5 -> (f) etichettatura -> (g) cargorobot: slot5->slot riservato. \
*Risultato:* container nello slot riservato; display aggiornato con _"Service working"_; sistema _disengaged_; LED spento.

== Malfunzionamento sonar

*Precondizioni:* sistema operativo. \
*Azioni:* sonar misura D > D#sub[FREE] per >= 3 s consecutivi. \
*Risultato:* sistema _Out of service_; display mostra _"Out of service"_.

== Rilevazione container (sonar)

*Precondizioni:* sistema _engaged_; sensor area vuota. \
*Azioni:* deposito container; sonar misura D < D#sub[FREE]/2 per >= 3 s. \
*Risultato:* cargoservice riceve _container\_detected_ e avvia la movimentazione.

// =============================================================================
= Project
// =============================================================================

Dall'analisi dei requisiti si propone, come ipotesi iniziale, una struttura a *tre
sprint* con integrazione progressiva dei componenti. La suddivisione serve a
organizzare il lavoro e i test, non a fissare decisioni architetturali definitive.

== Sprint 1: Core business

*Goal:* realizzare il primo nucleo eseguibile di cargoservice con collaboratori
simulati.

Funzionalità: ciclo di carico completo con collaboratori simulati, modello dello
stato della hold, LED simulato, timeout.

== Sprint 2: IOPort, display e sonar

*Goal:* rendere esplicita l'interazione con IOPort, display e sonar a livello software.

Funzionalità: ioport come interfaccia web, sonar simulato (rilevazione container
e malfunzionamento), aggiornamento display con stato e messaggi.

== Sprint 3: Dispositivi fisici

*Goal:* integrare, dove disponibili, i dispositivi fisici o i servizi forniti e
verificare il comportamento end-to-end.

Funzionalità: cargorobot e servizio DDR disponibile, sonar fisico, marker device
fisico, LED fisico.

// -----------------------------------------------------------------------------
//  Allegati - Team di lavoro
// -----------------------------------------------------------------------------
#pagebreak()

= Team di lavoro

#iss-table(
  columns: (1fr, auto),
  [*Nome e Cognome*], [*Ruolo*],
  [Davide Chirichella], [Membro del gruppo],
  [Gabriele Doti],     [Membro del gruppo],
  [Daniele Maccagnan], [Membro del gruppo],
)
