// -----------------------------------------------------------------------------
//  Sprint 2 esame Natali
// -----------------------------------------------------------------------------

#import "../../shared/template.typ": iss-template, iss-table, nota, domanda

#show: iss-template.with(
  title:         "Maritime CargoService",
  subtitle:      "Sprint 2",
  course:        "Ingegneria dei Sistemi Software",
  university:    "Alma Mater Studiorum . Università di Bologna",
  academic-year: "2025/2026",
  authors:       ("Davide Chirichella", "Gabriele Doti", "Daniele Maccagnan"),
)

// =============================================================================
= Introduction
// =============================================================================

Il punto di partenza di questo sprint è il prototipo realizzato nello Sprint 1, documentato al seguente #link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/docs/sprint1.pdf")[link].

Lo Sprint 1 ha prodotto un prototipo eseguibile del *cargoservice* in cui il ciclo principale di carico viene gestito tramite attori QAK, con componenti simulati per IOPort, sonar, LED e markerdevice, e con il *cargorobot* modellato come wrapper del servizio *robotsmart26*.

L'architettura finale dello Sprint 1, riportata di seguito, costituisce il riferimento di partenza per lo Sprint 2.

#figure(
  image("../../sprint1/prototype/prototype_sprint1arch.png"),
  caption: [Architettura finale definita nello Sprint 1.]
)

== Goal dello Sprint 2

Il goal dello Sprint 2 è quello definito nella pagina di sintesi dello Sprint 1.

L'obiettivo dello Sprint 2 è evolvere il prototipo sviluppato sostituendo progressivamente i componenti simulati con le rispettive implementazioni reali:

- IOPort come Web GUI;
- sonar e LED collegati al PicoW;
- completare la gestione dello stato *Out of service* integrando il sonar reale, implementando la verifica della misura e l'interruzione/ripresa del movimento del cargorobot.

Si stima una durata di circa *30 ore* complessive di lavoro, corrispondenti a circa 10 ore per ciascun componente del gruppo.

// =============================================================================
= Requirements
// =============================================================================

I requisiti del progetto sono riportati nel documento disponibile al seguente #link("https://anatali.github.io/issLab2026/_static/docs/Protobook.pdf#page=331")[link].

Per comodità si riporta anche l'allegato con i requisiti forniti dalla committente.

#figure(
  image("../../shared/requisiti_committente.png"),
  caption: [Requisiti forniti dalla committente.]
)

L'azienda richiede di realizzare un servizio denominato *cargoservice* con il seguente funzionamento:

- Gli slot1-4 rappresentano le aree della hold riservate per immagazzinare ciascuno un container.

- Lo slot5 rappresenta un'area in cui il cargorobot deve temporaneamente depositare un container, prima di posizionarlo in uno degli slot1-4. Durante la sosta temporanea, un dispositivo 'marker' etichetta il container con un codice a barre identificativo e segnala quando l'attività di marcatura è completata.

- L'IOPort è un dispositivo dotato di un pulsante e di un display. Il pulsante viene premuto dal cliente per inviare una richiesta di carico di un container sulla nave. Il display viene utilizzato per mostrare la risposta alla richiesta e lo stato attuale della hold.

- Il sensore associato all'IOPort è un dispositivo (un sonar) utilizzato per rilevare la presenza di un container, quando misura una distanza D, tale che D < D#sub[FREE]/2, per un tempo ragionevole (ad esempio 3 secondi).

- Il cargoservice è in grado di ricevere una richiesta di carico di un container inviata da un cliente tramite il pulsante dell'IOPort.

- Invia la risposta *retrylater* se l'IOPort è attualmente occupato da un container oppure se il sistema è *Out of service*.

- Rifiuta la richiesta quando la hold è già piena, ovvero gli slot1-4 sono già tutti occupati.

- Altrimenti, considera il sistema come *engaged*, rileva uno slot libero e restituisce come risposta il nome dello slot riservato. Mentre il sistema è engaged, il LED deve lampeggiare.

- Quando la richiesta di carico viene accettata, il cliente deve spostare il container nell'area del sensore entro un tempo prefissato (ad esempio 30 secondi), altrimenti il sistema diventa *disengaged*.

- Successivamente, il cargoservice utilizza il cargorobot per spostare il container dall'IOPort a slot5 (per l'etichettatura del container) e poi allo slot riservato.

- Il servizio deve inoltre mostrare sul display dell'IOPort:
  - lo stato attuale della hold;
  - il messaggio *"Service working"* quando tutto sta procedendo correttamente;
  - il messaggio *"Out of service"* se il sensore sonar misura la distanza del container dal sonar stesso D > D#sub[FREE] per almeno 3 secondi.

// =============================================================================
= Requirement analysis
// =============================================================================

L'analisi dei requisiti è stata svolta nello #link("https://github.com/chirichexe/iss-2026/blob/main/sprint0/docs/sprint0.pdf")[Sprint 0] e approfondita nello #link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/docs/sprint1.pdf")[Sprint 1]. Tali risultati vengono assunti come validi anche per lo Sprint 2.

In particolare, per lo Sprint 2 restano centrali le seguenti conclusioni:

- il *cargoservice* è l'orchestratore del ciclo di carico;
- la *hold* rappresenta lo stato interno degli slot e delle posizioni rilevanti;
- il *cargorobot* viene trattato come servizio reattivo, tramite wrapper verso *RobotSmart26*;
- sonar e LED devono essere integrati come dispositivi reali collegati al PicoW;
- l'IOPort deve essere realizzato come Web GUI;
- lo stato *Out of service* deve bloccare l'accettazione di nuove richieste e interrompere il movimento del robot, con ripresa al ritorno dello stato *Service working*.

== Architettura di partenza

Per lo Sprint 2 l'architettura di riferimento riprende la decomposizione logica individuata nello Sprint 0, separando il sistema nei seguenti contesti:

#iss-table(
  columns: (auto, 1fr),
  [*Contesto*], [*Responsabilità*],
  [*ctxcargoservice*], [Contiene il *cargoservice*, responsabile dell'orchestrazione del ciclo di carico e della gestione dello stato del servizio.],
  [*ctxcustomer*], [Contiene le componenti legate all'interazione con il customer e alla Web GUI dell'IOPort.],
  [*ctxdevices*], [Contiene le componenti fisiche o simulate relative a sonar, LED, hold e markerdevice.],
  [*ctxrobot*], [Contiene il wrapper del *cargorobot* e le interazioni con il servizio di movimentazione.],
)

Il codice di partenza dello Sprint 2 è organizzato nei seguenti moduli:

#iss-table(
  columns: (auto, 1fr),
  [*Modulo*], [*Descrizione*],
  [#link("https://github.com/chirichexe/iss-2026/tree/main/utils/prototype/cargoservice")[cargoservice]], [Modulo del servizio principale.],
  [#link("https://github.com/chirichexe/iss-2026/tree/main/utils/prototype/customer")[customer]], [Modulo dedicato al customer e all'IOPort.],
  [#link("https://github.com/chirichexe/iss-2026/tree/main/utils/prototype/devices")[devices]], [Modulo dedicato ai dispositivi della hold.],
  [#link("https://github.com/chirichexe/iss-2026/tree/main/utils/prototype/robot")[robot]], [Modulo dedicato al robot.],
)

#nota[
  Questa sezione dovrà essere completata durante lo Sprint 2 con le eventuali ulteriori decisioni architetturali introdotte dall'integrazione dei dispositivi reali.
]

// =============================================================================
= Problem analysis <model>
// =============================================================================

== Analisi dell'IOPort come Web GUI

#nota[Da completare con le scelte relative alla realizzazione della Web GUI dell'IOPort e al protocollo di comunicazione con il cargoservice.]

== Analisi dell'integrazione sonar e LED su PicoW

#nota[Da completare con l'analisi del collegamento dei dispositivi reali, del formato dei messaggi e della gestione delle misure del sonar.]

== Gestione dello stato Out of service

#nota[Da completare con la strategia di rilevazione della condizione di guasto, interruzione del movimento del robot e successiva ripresa.]

== Evoluzione dell'architettura QAK

#nota[Da completare con le modifiche ai messaggi, agli attori e ai contesti rispetto al prototipo dello Sprint 1.]

// =============================================================================
= Project
// =============================================================================

== Struttura del progetto

#nota[Da completare con la struttura finale dei moduli e con i riferimenti ai sorgenti implementati nello Sprint 2.]

== Implementazione dell'IOPort

#nota[Da completare.]

== Implementazione dei dispositivi reali

#nota[Da completare.]

== Implementazione della gestione Out of service

#nota[Da completare.]

// =============================================================================
= Test plans <testplan>
// =============================================================================

#nota[Da completare con i test funzionali e di integrazione previsti per lo Sprint 2.]

== Test IOPort

#nota[Da completare.]

== Test sonar e LED

#nota[Da completare.]

== Test Out of service e ripresa del robot

#nota[Da completare.]

// =============================================================================
= Deployment <deployment>
// =============================================================================

#nota[Da completare con le istruzioni di avvio dei moduli, della Web GUI, del PicoW e dei servizi necessari.]

// =============================================================================
= Maintenance
// =============================================================================

#nota[Da completare con eventuali note di manutenzione, limiti noti e attività rinviate agli sprint successivi.]

// =============================================================================
= Pagina di sintesi
// =============================================================================

== Architettura finale dello Sprint 2

#nota[Da completare con l'architettura finale prodotta nello Sprint 2.]

== Goal dello sprint successivo

#nota[Da completare al termine dello Sprint 2.]

// -----------------------------------------------------------------------------
// Allegati - Team di lavoro
// -----------------------------------------------------------------------------
#pagebreak()

= Team di lavoro

#iss-table(
  columns: (1fr, auto),
  [*Nome e Cognome*], [*Matricola*],
  [Davide Chirichella], [0001222371],
  [Gabriele Doti], [0001245897],
  [Daniele Maccagnan], [0001247340],
)
