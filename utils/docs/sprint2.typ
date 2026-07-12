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

= Problem analysis <model>

// =============================================================================
== Evoluzione rispetto al prototipo dello Sprint 1
// =============================================================================

Lo Sprint 1 ha validato il flusso principale di carico attraverso un prototipo eseguibile nel quale `ioportmock`, `sonarmock` e `ledmock` simulavano i dispositivi non ancora disponibili. Il *cargoservice* coordinava tali componenti mediante messaggi QAK, mentre il *cargorobot* inoltrava le richieste di movimento a *RobotSmart26*.

Nello Sprint 2 la logica applicativa già validata viene mantenuta, mentre vengono sostituiti progressivamente i componenti simulati con componenti accessibili attraverso tecnologie compatibili con la loro natura:

* l'IOPort viene realizzato come Web GUI;
* sonar e LED vengono integrati attraverso il PicoW;
* lo stato della hold e del servizio viene reso osservabile dall'esterno;
* viene completata la gestione dello stato *Out of service*.

L'obiettivo non è quindi modificare il ciclo di carico definito nello Sprint 1, ma estenderne i confini di interazione, permettendo al *cargoservice* di comunicare con una GUI web e con dispositivi fisici distribuiti.

// =============================================================================
== Osservabilità dello stato del sistema
// =============================================================================

La Web GUI deve mostrare lo stato corrente della hold e lo stato operativo del servizio. Tali informazioni sono però attualmente mantenute all'interno del *cargoservice* e del POJO `Hold`, e non sono direttamente accessibili ai componenti esterni.

È pertanto necessario definire una rappresentazione univoca e serializzabile dello stato osservabile del sistema. A tale scopo si sceglie di rappresentarlo mediante un documento JSON contenente almeno:

* lo stato logico del servizio (`engaged` oppure `disengaged`);
* lo stato operativo (`Service working` oppure `Out of service`);
* lo stato di occupazione dell'IOPort;
* l'identificativo dell'eventuale slot riservato;
* lo stato di occupazione degli slot1-4.

Il JSON rappresenta esclusivamente una vista dello stato mantenuto dal *cargoservice* e dalla `Hold`: non introduce una seconda sorgente di verità e non deve essere modificato direttamente dai componenti esterni.

Ogni volta che avviene una modifica significativa, il *cargoservice* aggiorna la risorsa osservabile. Gli aggiornamenti rilevanti comprendono, ad esempio:

* prenotazione o liberazione di uno slot;
* passaggio tra `engaged` e `disengaged`;
* rilevamento o rimozione del container dall'IOPort;
* ingresso nello stato *Out of service*;
* ripristino dello stato *Service working*;
* completamento del deposito nello slot riservato.

// =============================================================================
== Esposizione dello stato mediante CoAP
// =============================================================================

Il runtime QAK permette a un attore di esporre il proprio stato come risorsa CoAP osservabile. Questa possibilità risulta adatta al problema poiché consente a un componente esterno di:

* recuperare lo stato corrente del sistema;
* osservare la risorsa;
* ricevere una notifica quando il suo contenuto viene aggiornato.

Il *cargoservice* espone quindi il documento JSON mediante la propria risorsa CoAP, aggiornandola attraverso `updateResource(...)`.

La scelta di CoAP riguarda la comunicazione interna tra il sistema QAK e il componente che serve la Web GUI. Non è invece possibile utilizzare direttamente CoAP dal browser in modo portabile, poiché i browser non espongono normalmente API native per questo protocollo.

È quindi necessario introdurre un componente intermediario, denominato nel seguito *web server* o *IOPort server*, che svolga le seguenti funzioni:

* osservare la risorsa CoAP del *cargoservice*;
* tradurre gli aggiornamenti ricevuti in messaggi compatibili con il browser;
* ricevere dalla GUI le richieste dell'utente;
* inoltrare tali richieste al *cargoservice*.

// =============================================================================
== Realizzazione dell'IOPort come Web GUI
// =============================================================================

L'IOPort richiesto dalla committente è costituito logicamente da:

* un pushbutton, utilizzato dal cliente per inviare una `load_request`;
* un display, utilizzato per mostrare la risposta alla richiesta e lo stato corrente della hold.

La sua implementazione viene suddivisa in due parti:

* una pagina web eseguita nel browser;
* un server intermediario che collega il browser al sistema QAK.

La GUI non contiene la logica del ciclo di carico e non mantiene autonomamente lo stato della hold. Essa si limita a:

* inviare il comando associato alla pressione del pushbutton;
* visualizzare la risposta ricevuta;
* aggiornare il display quando cambia lo stato pubblicato dal *cargoservice*.

=== Invio della load_request

Quando il cliente preme il pulsante, la GUI invia una richiesta HTTP `POST` al server intermediario.

Il server traduce tale richiesta in una `load_request` indirizzata al *cargoservice* e attende una delle risposte già definite nello Sprint 1:

* `load_accepted`;
* `load_retrylater`;
* `load_refused`.

La risposta viene quindi restituita alla GUI e mostrata sul display.

Questa soluzione mantiene invariato il protocollo applicativo QAK già validato nello Sprint 1: HTTP viene utilizzato soltanto nel tratto browser-server, mentre l'interazione con il *cargoservice* continua a essere espressa mediante i messaggi del modello.

=== Aggiornamento del display

L'aggiornamento dello stato non dovrebbe dipendere da interrogazioni periodiche effettuate dal browser, poiché il polling introdurrebbe richieste ripetute anche in assenza di cambiamenti.

Si sceglie pertanto una comunicazione push mediante WebSocket:

1. il server osserva la risorsa CoAP del *cargoservice*;
2. quando la risorsa cambia, il server riceve il nuovo JSON;
3. il server inoltra l'aggiornamento ai browser connessi tramite WebSocket;
4. la GUI aggiorna il display.

Il server intermediario svolge quindi una funzione di adattamento tra tre differenti modalità di comunicazione:

* HTTP per i comandi provenienti dalla GUI;
* CoAP Observe per osservare lo stato del *cargoservice*;
* WebSocket per inviare aggiornamenti asincroni al browser.

// =============================================================================
== Integrazione del sonar reale
// =============================================================================

Nello Sprint 1 il sonar era rappresentato da `sonarmock`, che emetteva eventi `sonardata`. La logica del *cargoservice* dipende quindi dal contenuto delle misurazioni, ma non dalla concreta implementazione del dispositivo che le produce.

Nello Sprint 2 il sonar fisico è collegato al PicoW. Il software eseguito sul dispositivo deve:

* leggere periodicamente la distanza;
* rendere disponibili le misurazioni al sistema distribuito;
* rimanere indipendente dall'implementazione interna del *cargoservice*.

Il PicoW e il sistema QAK operano su piattaforme differenti. È pertanto necessario utilizzare un protocollo interoperabile e sufficientemente leggero per un dispositivo IoT.

Si sceglie MQTT, basato sul modello publish/subscribe. Il PicoW pubblica le misurazioni su un topic dedicato, mentre il sistema software si sottoscrive al medesimo topic e le traduce nel messaggio `sonardata` utilizzato dal *cargoservice*.

La corrispondenza tra topic MQTT e messaggio QAK deve essere configurata esplicitamente. In questo modo la logica applicativa del *cargoservice* può continuare a elaborare `sonardata` senza dipendere dal linguaggio o dalla piattaforma utilizzati dal dispositivo fisico.

Il sonar conserva quindi una responsabilità limitata:

* misurare la distanza;
* pubblicare la misura.

L'interpretazione applicativa della distanza rimane invece responsabilità del *cargoservice*.

// =============================================================================
== Validazione temporale delle misure sonar
// =============================================================================

I requisiti non associano un cambiamento di stato a una singola misura istantanea. La presenza del container e il possibile guasto del sonar devono essere riconosciuti soltanto quando la relativa condizione permane per almeno tre secondi.

Occorre quindi distinguere:

* `D < D_FREE/2`: possibile presenza del container;
* `D > D_FREE`: possibile condizione di guasto;
* valori intermedi: assenza di una delle due condizioni precedenti.

Una singola misura non è sufficiente a produrre una transizione. La condizione deve essere confermata attraverso misure consecutive per l'intervallo temporale richiesto.

La responsabilità della validazione temporale può essere collocata:

* sul PicoW, che pubblica soltanto condizioni già validate;
* nel sistema QAK, che riceve tutte le misurazioni e gestisce il tempo di permanenza.

Per mantenere il dispositivo focalizzato sull'acquisizione dei dati e centralizzare nel *cargoservice* le decisioni applicative, si sceglie di trasmettere le misurazioni grezze e di effettuare la validazione temporale lato sistema software.

Il *cargoservice* deve quindi mantenere separatamente:

* l'istante di inizio della condizione `D < D_FREE/2`;
* l'istante di inizio della condizione `D > D_FREE`;
* l'eventuale annullamento del conteggio quando la condizione non è più verificata.

Solo dopo il superamento dell'intervallo stabilito viene aggiornato lo stato del sistema.

// =============================================================================
== Integrazione del LED reale
// =============================================================================

Nello Sprint 1 il *cargoservice* inviava a `ledmock` il dispatch:

```qak
Dispatch led_ctrl : ledCmd(CMD)
```

Il comando rappresentava le operazioni logiche `on`, `off` e `blink`.

Nello Sprint 2 il LED è fisicamente collegato al PicoW. Anche in questo caso è necessario evitare che il *cargoservice* dipenda dai dettagli hardware del dispositivo.

Il messaggio `led_ctrl` viene pertanto mantenuto come interfaccia logica. Un componente di integrazione riceve il comando e lo pubblica su un topic MQTT dedicato al LED. Il software sul PicoW si sottoscrive al topic e traduce il valore ricevuto nell'operazione hardware corrispondente.

La catena logica diventa quindi:

```text
cargoservice -> led_ctrl -> adattatore MQTT -> PicoW -> LED
```

In questo modo:

* il *cargoservice* continua a utilizzare lo stesso comando definito nello Sprint 1;
* la gestione elettrica del LED rimane confinata sul PicoW;
* il componente fisico può essere sostituito senza modificare la logica applicativa.

// =============================================================================
== Gestione dello stato Out of service
// =============================================================================

Nello Sprint 1 il *cargoservice* aggiornava la variabile `ServiceWorking` sulla base degli eventi sonar, ma non interrompeva un movimento già in corso.

Nello Sprint 2 il comportamento viene completato in accordo con il chiarimento fornito dalla committente:

* quando la condizione `D > D_FREE` persiste per almeno tre secondi, il sistema entra nello stato *Out of service*;
* durante tale stato non vengono accettate nuove richieste;
* se il robot è in movimento, il piano deve essere interrotto;
* quando il sonar torna a misurare `D <= D_FREE`, il sistema ritorna nello stato *Service working*;
* se un movimento era stato interrotto, deve essere ripreso.

La gestione richiede di distinguere almeno due situazioni:

1. il sistema entra in *Out of service* mentre non è in corso alcun movimento;
2. il sistema entra in *Out of service* durante l'esecuzione di una procedura di movimentazione.

Nel primo caso è sufficiente aggiornare lo stato operativo e impedire l'accettazione di nuove richieste.

Nel secondo caso il *cargoservice* deve inoltre richiedere l'interruzione del movimento e ricordare che esiste una procedura sospesa. Al ripristino del servizio, il movimento viene ripreso secondo le operazioni offerte da *RobotSmart26*.

L'ingresso nello stato *Out of service* non deve:

* liberare automaticamente lo slot riservato;
* riportare il sistema nello stato `disengaged`;
* annullare la procedura di carico.

Si tratta infatti di una sospensione temporanea e non del fallimento definitivo dell'operazione.

// =============================================================================
== Gestione coerente dello stato della hold
// =============================================================================

La prenotazione di uno slot e la sua effettiva occupazione rappresentano due momenti distinti.

Quando una `load_request` viene accettata, lo slot viene riservato affinché non possa essere assegnato a un'altra richiesta. Esso può però essere considerato fisicamente occupato soltanto dopo che il robot ha completato il deposito.

È quindi opportuno distinguere logicamente almeno i seguenti stati:

* libero;
* riservato;
* occupato.

Questa distinzione permette alla Web GUI di mostrare uno stato più fedele della hold e rende esplicito cosa debba accadere nei casi di timeout, sospensione o fallimento del movimento.

In particolare:

* in caso di timeout prima del deposito, lo slot riservato torna libero;
* durante una sospensione *Out of service*, lo slot rimane riservato;
* dopo il deposito completato, lo slot diventa occupato;
* in caso di fallimento del robot, lo slot non viene liberato automaticamente.

// =============================================================================
== Evoluzione dell'architettura
// =============================================================================

Rispetto allo Sprint 1 vengono introdotti i seguenti elementi:

* una Web GUI che realizza l'IOCome posso evolvere lo sprint1 naturalmente nello sprint2?Port;
* un server intermediario per HTTP, WebSocket e osservazione CoAP;
* un broker MQTT per la comunicazione con il PicoW;
* il software sul PicoW per il sonar e il LED;
* un componente di adattamento tra MQTT e i messaggi QAK;
* la pubblicazione dello stato del *cargoservice* come risorsa CoAP osservabile.

Il *cargoservice* rimane l'orchestratore del ciclo di carico e la `Hold` rimane la sorgente dello stato degli slot. I nuovi componenti non trasferiscono altrove la logica applicativa, ma rendono possibile l'interazione con browser e dispositivi fisici.

L'evoluzione preserva quindi le interfacce logiche introdotte nello Sprint 1:

* `load_request` e relative reply per l'IOPort;
* `sonardata` per le misurazioni del sonar;
* `led_ctrl` per il controllo del LED;
* `moverobot` per la movimentazione;
* `mark_container` per la marcatura.

Le principali modifiche riguardano il trasporto dei messaggi e l'osservabilità dello stato, non il significato delle interazioni applicative già validate.

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
