// -----------------------------------------------------------------------------
//  Sprint 0 esame Natali
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

Una compagnia di trasporto marittimo di container (d'ora in poi _la committente_) intende automatizzare le operazioni di carico 
dei container nella hold della nave (d'ora in poi *hold*). A tal fine prevede di impiegare un robot a guida differenziale 
(Differential Drive Robot, d'ora in poi *cargorobot*). 

L'obiettivo dello Sprint 0 è formalizzare e disambiguare i requisiti forniti dalla committente in linguaggio naturale, 
costruire un primo modello del sistema, evidenziare il _core business_, 
motivare la scelta del linguaggio di modellazione e definire un primo insieme di piani di test funzionali e il goal dello Sprint 1. 

Ogni scelta è strettamente ancorata ai requisiti: il modello qui presentato serve a catturare il comportamento richiesto, senza anticipare decisioni progettuali o scelte implementative che verranno affrontate negli sprint successivi.  

I requisiti del progetto sono riportati nel documento disponibile al seguente #link("https://anatali.github.io/issLab2026/_static/docs/Protobook.pdf#page=331")[link] 

// =============================================================================
= Requirements   
// ============================================================================

L'azienda richiede di realizzare un servizio denominato *cargoservice* con il seguente funzionamento:

- Il cargoservice è in grado di ricevere una richiesta di carico di un container inviata da un cliente tramite il pulsante dell'IOPort.
- Invia la risposta *retrylater* se l'IOPort è attualmente occupato da un container oppure se il sistema è *Out of service*.
- Rifiuta la richiesta quando la hold è già piena, ovvero gli slot1-4 sono già tutti occupati.
- Altrimenti, considera il sistema come *engaged*, rileva uno slot libero e restituisce come risposta il nome dello slot riservato. Mentre il sistema è engaged, il LED deve lampeggiare.
- Quando la richiesta di carico viene accettata, il cliente deve spostare il container nell'area del sensore entro un tempo prefissato (ad esempio 30 secondi), altrimenti il sistema diventa *disengaged*.
- Successivamente, il cargoservice utilizza il cargorobot per spostare il container dall'IOPort a slot5 (per l'etichettatura del container) e poi allo slot riservato.
- Il servizio deve inoltre mostrare sul display dell'IOPort:
  - lo stato attuale della hold
  - il messaggio *"Service working"* quando tutto sta procedendo correttamente
  - il messaggio *"Out of service"* se il sensore sonar misura una distanza D > D#sub[FREE] per almeno 3 secondi (possibile guasto del sonar)

== Domande aperte alla committente 

#domanda[ 
  *Posizione dell'IOPort nella mappa.* I requisiti indicano che il cargorobot sposta il container dall'IOPort a slot5, lasciando intendere che l'IOPort sia una cella praticabile. Come si colloca nella griglia? 
] 
/ *Risposta della committente*: La collocazione dell'IOPort nella griglia è da intendersi informalmente come mostrato nella figura presente nella traccia.

#domanda[ 
  *Richieste concorrenti.* Il sistema deve bufferizzare più richieste di carico contemporanee, oppure una nuova richiesta ricevuta mentre il sistema è engaged viene semplicemente refusata / respinta con retrylater senza accodamento? Dai requisiti si interpreta che le richieste *non siano bufferizzate*: se il sistema è occupato, la risposta è immediata (retrylater o refused) senza code di attesa. 
] 
/ *Risposta della committente*: Si conferma che le richieste non devono essere bufferizzate. Il sistema accetta le richieste mediante l'IOPort e, se questa risulta occupata, non deve essere possibile inviare altre richieste.

#domanda[ 
  *Liberazione degli slot e aggiornamento della disponibilità.* I requisiti descrivono il processo di carico dei container negli slot1-4, ma non specificano come venga gestita la liberazione di uno slot già occupato. Possiamo assumere che lo svuotamento degli slot sia effettuato da sistemi o operatori esterni al nostro sistema? In tal caso, con quale meccanismo viene notificato a cargoservice che uno specifico slot è stato liberato e può tornare disponibile per future prenotazioni? 
] 
/ *Risposta della committente*: Non è previsto lo svuotamento degli slot. Sarà l'obiettivo di un progetto futuro.

#domanda[ 
  *Ripristino dello stato Service working.* I requisiti specificano che il sistema entra nello stato Out of service quando il sonar rileva una distanza D > D#sub[FREE] per almeno 3 secondi. Non è invece specificata la condizione per il ritorno allo stato Service working. Il sistema deve tornare operativo non appena il sonar rileva D <= D#sub[FREE] oppure è richiesto un ulteriore intervallo di stabilità prima del ripristino del servizio?
] 
/ *Risposta della committente*: Si conferma che l'interpretazione è corretta.

#domanda[ 
  *Indicazioni sulla realizzazione dell'IOPort.* Ci sono indicazioni sulla realizzazione dell'IOPort?
] 
/ *Risposta della committente*: Si conferma che bisogna svilupparla come una web GUI.


// =============================================================================
= Requirement analysis
// =============================================================================

== Motivazione dell’uso del linguaggio QAK
QAK è il linguaggio messo a disposizione dalla nostra software house
per modellare sistemi software distribuiti, particolarmente espressivo nel formalizzare
il concetto di *attore autonomo* e di *messaggio*, riducendo di molto l'“Abstraction
Gap” tra requisiti nel contesto di un sistema distribuito eterogeneo.

I requisiti descrivono un sistema composto da entità che ricevono richieste, inviano risposte, 
osservano eventi e coordinano dispositivi. Java, preso come linguaggio general purpose, 
esprime invece in modo naturale soprattutto interazioni tramite chiamate di procedura, spesso sincrone e bloccanti. 
Questo rischia di introdurre precocemente dettagli implementativi che distolgono dal problema principale:
formalizzare il comportamento osservabile richiesto dalla committente.

QAK introduce invece come concetti primitivi quelli di *attore*, *messaggio*,
*request*, *reply*, *dispatch* ed *event*, rendendo il modello più vicino al dominio
del problema. La natura *reattiva* e *proattiva* di servizi che devono rispondere
a stimoli esterni e avviare autonomamente sequenze di azioni è infatti catturata in
modo naturale da un attore QAK, cosa che un POJO, componente
passivo attivato da chiamate sincrone, non catturerebbe altrettanto bene.

Dal modello QAK viene inoltre generato automaticamente codice Kotlin eseguibile,
il che consente di disporre di un *primo prototipo osservabile già nello Sprint 0*,
prima ancora di scrivere una riga di logica applicativa.

== Vocabolario

#iss-table(
columns: (auto, 1fr),
[*Termine*], [*Significato*],

[*engaged*], [Stato nel quale il sistema sta gestendo una richiesta di carico.],

[*disengaged*], [Stato nel quale il sistema può accettare una nuova richiesta.],

[*Out of service*], [Stato nel quale il servizio non può accettare richieste a causa del malfunzionamento del sonar.],

[*hold*], [Modello logico della stiva e dell'occupazione degli slot.],

[*slot libero*], [Primo slot disponibile tra slot1-slot4.],
)

== Contesti logici

Dai requisiti emerge che il sistema non è naturalmente concentrato in un unico processo; le entità sono quindi state distribuite su context distinti, ciascuno dei quali rappresenta un nodo di elaborazione.
#iss-table(
columns: (auto, 1fr),
[*Contesto*], [*Componenti e responsabilità*],
[*ctxCargoService*],
[È il nucleo del comportamento richiesto e punto di orchestrazione del ciclo di carico. Contiene il cargoservice.],
[*ctxIOPort*],
[Raggruppa le entità dedicate all'interazione con l'utente. Contiene l'IOPort (con display e pushbutton)],
[*ctxDevices*],
[Raggruppa i dispositivi presenti nella hold: sonar, LED, hold e markerdevice.],
[*ctxRobot*],
[Raggruppa le entità legate al cargorobot e alla movimentazione richiesta.],
)

== Formalizzazione dei messaggi QAK 

Dai requisiti, l'unica richiesta che si evince è quella di carico, che viene modellata come request in qak.
La richiesta viene inviata dal customer al cargoservice tramite l'IOPort.

```qak
Request  load_request    : loadRequest(none)
Reply load_accepted : loadAccepted(slotID) for load_request // accettazione
Reply load_retrylater : loadRetryLater(none) for load_request // rinvio temporaneo
Reply load_refused : loadRefused(none) for load_request // rifiuto definitivo
```

== Macro-componenti e natura software

=== cargoservice

Il cargoservice è l'orchestratore principale del sistema. Gestisce le richieste di carico, verifica le condizioni della hold, controlla i timeout e coordina le altre componenti.

Il componente è da sviluppare.

Dal punto di vista della natura software, presenta un comportamento sia reattivo (gestione di richieste ed eventi) sia proattivo (invio di comandi e aggiornamenti). Per questo motivo si ritiene opportuno rappresentarlo come un QAK Actor.

```qak
QActor cargoservice context ctxCargoService {
    // DA SVILUPPARE: definizione degli stati e delle transizioni secondo i requisiti
}
```

=== cargorobot

Il *cargorobot* è il sottosistema responsabile della movimentazione fisica del container all'interno della hold.

Dopo una discussione con la committente, si conviene che la responsabilità di decidere
la sequenza applicativa resta in capo al *cargoservice*: il cargorobot non decide se
una richiesta debba essere accettata, rifiutata o sospesa, ma viene guidato dal
cargoservice per eseguire operazioni di movimentazione.

Per ora non assumiamo che il cargorobot coincida con un QActor sviluppato da noi.
È invece opportuno analizzare quali software già disponibili nella software house
o in rete siano adeguati al problema, ad esempio servizi già esistenti per il controllo
del DDR. In particolare, andrà valutato se riutilizzare software come *robosmart26* o
*robotservice26*.

Nel modello dei requisiti il cargorobot viene quindi considerato come collaboratore
del cargoservice, eventualmente realizzato come servizio autonomo. La decisione sulla
sua realizzazione concreta è rinviata all'analisi del problema.

=== IOPort

L'IOPort rappresenta l'interfaccia tra customer e sistema. 
Dopo una discussione con la committente, si comprende che esso viene interpretato come una Web GUI composta da un pushbutton e 
da un display. Dai requisiti si deduce che è IOPort ad emettere la richiesta *load_request* verso *cargoservice* e mostrare informazioni 
di stato.

Viene demandata all'analisi del problema la scelta se il server dell'IOPort coincida con quello del cargoservice oppure se sia modellato 
come servizio separato per ottenere maggiore disaccoppiamento.

Nel modello QAK può essere rappresentato come attore per modellarne il comportamento comunicativo,
non perché la sua implementazione finale debba necessariamente essere QAK.

```qak
QActor ioport context ctxCustomer {
  // DA SVILUPPARE
}
```

=== sonar

Il sonar rileva la presenza di un container nella sensor_area.

Il dispositivo fisico è considerato fornito ed è collegato al PicoW, mentre il software di integrazione è da sviluppare.

```qak
QActor sonar context ctxDevices {
  // DA SVILUPPARE
}
```

=== markerdevice

Il markerdevice etichetta i container depositati nello slot5 e notifica il completamento della marcatura.

Il dispositivo è considerato disponibile in forma simulata, mentre il software di controllo è da sviluppare.

```qak
QActor markerdevice context ctxDevices {
  // DA SVILUPPARE
}
```

=== LED

Il LED è un dispositivo fisico usato per rendere osservabile lo stato *engaged* del sistema.

La frase dei requisiti _"while engaged, the system blink a Led"_ viene formalizzata
nel seguente modo: quando il cargoservice entra nello stato *engaged*, il LED deve
lampeggiare; quando il sistema torna nello stato *disengaged*, il LED deve essere spento.

Dopo una consultazione con la committente, si è definito che il LED è considerato un dispositivo 
fisico integrato nel PicoW che gestisce anche il sonar. Il software di controllo del LED è quindi 
da sviluppare o integrare nel software eseguito sul PicoW.

=== hold

La hold è l'entità che rappresenta logicamente la stiva della nave e lo stato di occupazione degli slot.

Il componente è da sviluppare.

Dal punto di vista della natura software, può essere rappresentata come una struttura dati passiva composta
composta da Celle. Ogni Cella può indicare uno spazio libero, un ostacolo, una posizione speciale o uno slot.
Una possibile implementazione consiste in una matrice bidimensionale di celle, In particolare, la hold contiene gli slot
di destinazione *slot1*--*slot4* e lo *slot5* usato per la marcatura.

La seguente rappresentazione non vincola l'implementazione finale, ma permette di formalizzare il concetto di hold piena e di slot libero.

```java
num CellType {
    FREE, OBSTACLE, HOME, SENSOR,
    IOPORT, SLOT1, SLOT2, SLOT3, SLOT4, SLOT5
}

class Hold {
    private final CellType[][] cells;
    private final boolean[] occupiedSlots = new boolean[4];
  // TODO
}
```

== Core business 

Il *core business* del sistema è la gestione del ciclo di carico di un container. La responsabilità della sequenza applicativa resta in *cargoservice*: il cargorobot è coinvolto per eseguire spostamenti richiesti dal servizio, non per decideree se una richiesta debba essere accettata, rifiutata o sospesa. La sequenza principale ricavata dai requisiti è espressa dal metamodello ccome segue:

```
System cargosystem

// Vocabolario delle interazioni
Request load_request      : loadRequest(none)
Reply load_accepted       : loadAccepted(slotID) for load_request
Reply load_retrylater     : loadRetryLater(none) for load_request
Reply load_refused        : loadRefused(none) for load_request

Context ctxcargoservice ip [host="localhost" port=8050]

// Core Business
QActor cargoservice context ctxcargoservice {

    State s0 initial {
        println("cargoservice | started")
    }
    Goto disengaged

    State disengaged {
        println("cargoservice | DISENGAGED: waiting for load_request...")
    }
    Transition t0
        whenRequest load_request -> handle_load_request

    State handle_load_request {
        printCurrentMessage

        // simulazione di una richiesta accettata
        replyTo load_request with load_accepted : loadAccepted(slot1)
    }
    Goto engaged

    State engaged {
        println("cargoservice | ENGAGED: slot reserved")
    }
}
```

// =============================================================================

= Test plan

// =============================================================================

I test funzionali verificano il comportamento osservabile del sistema rispetto ai
requisiti, indipendentemente dall'implementazione concreta dei componenti.

#iss-table(
columns: (1.2fr, 2fr, 2fr, 2fr),
[*Test*], [*Precondizioni*], [*Azione*], [*Risultato atteso*],

[*T1 - Accettazione richiesta*],
[*disengaged*, sistema non *Out of service*, IOPort libera, almeno uno slot libero],
[Invio di *load_request* al cargoservice],
[Reply *load_accepted(slotID)*; lo slot indicato viene riservato; il sistema diventa *engaged*; il LED lampeggia],

[*T2 - Hold piena*],
[*disengaged*, sistema non *Out of service*, IOPort libera, slot1--slot4 occupati],
[Invio di *load_request* al cargoservice],
[Reply *load_refused*; il sistema resta *disengaged*; il LED resta spento],

[*T3 - Sistema Out of service*],
[Sistema in stato *Out of service*],
[Invio di *load_request* al cargoservice],
[Reply *load_retrylater*; lo stato del sistema non cambia],

[*T4 - IOPort occupata*],
[*disengaged*, sistema non *Out of service*, IOPort occupata da un container],
[Invio di *load_request* o pressione del pushbutton],
[Reply *load_retrylater* oppure richiesta non inoltrata; il sistema resta *disengaged*],

[*T5 - Timeout deposito*],
[Sistema *engaged* dopo una richiesta accettata],
[Il customer non deposita il container nella sensor area entro il tempo massimo],
[Il sistema torna *disengaged*; il LED viene spento]
)

```java
  /**
    * Scenario di test 1:
    * Verifica che una richiesta di carico venga accettata quando
    * il sistema è disponibile, la IOPort è libera e almeno uno slot
    * della hold è disponibile. Il cargoservice deve rispondere con
    * LOAD_ACCEPTED, riservare lo slot, passare allo stato ENGAGED
    * e attivare il lampeggio del LED.
    */
  @Test
  void T1_loadRequestAccepted() {
      CargoService service = new CargoService();

      service.setOutOfService(false);
      service.setIoPortFree(true);
      service.setSlotFree("slot1");

      LoadReply reply = service.loadRequest();

      assertEquals(LoadReplyType.LOAD_ACCEPTED, reply.getType());
      assertEquals("slot1", reply.getSlotId());
      assertTrue(service.isSlotReserved("slot1"));
      assertEquals(ServiceState.ENGAGED, service.getState());
      assertTrue(service.isLedBlinking());
  }

  /**
    * Scenario di test 2:
    * Verifica che una richiesta di carico venga rifiutata quando
    * tutti gli slot della hold risultano occupati. Il cargoservice
    * deve rispondere con LOAD_REFUSED e rimanere nello stato
    * DISENGAGED senza attivare il LED.
    */
  @Test
  void T2_loadRequestRefusedWhenHoldIsFull() {
      CargoService service = new CargoService();

      service.setOutOfService(false);
      service.setIoPortFree(true);
      service.occupyAllSlots();

      LoadReply reply = service.loadRequest();

      assertEquals(LoadReplyType.LOAD_REFUSED, reply.getType());
      assertEquals(ServiceState.DISENGAGED, service.getState());
      assertFalse(service.isLedOn());
  }

  /**
    * Scenario di test 3:
    * Verifica che una richiesta di carico venga rinviata quando
    * il sistema si trova nello stato Out of service. Il cargoservice
    * deve rispondere con LOAD_RETRYLATER senza modificare
    * il proprio stato interno.
    */
  @Test
  void T3_retryLaterWhenSystemIsOutOfService() {
      CargoService service = new CargoService();

      service.setOutOfService(true);
      ServiceState previousState = service.getState();

      LoadReply reply = service.loadRequest();

      assertEquals(LoadReplyType.LOAD_RETRYLATER, reply.getType());
      assertEquals(previousState, service.getState());
  }

  /**
    * Scenario di test 4:
    * Verifica che una richiesta di carico non venga accettata
    * quando la IOPort è già occupata. Il cargoservice deve
    * rispondere con LOAD_RETRYLATER e rimanere nello
    * stato DISENGAGED.
    */
  @Test
  void T4_retryLaterWhenIoPortIsOccupied() {
      CargoService service = new CargoService();

      service.setOutOfService(false);
      service.setIoPortFree(false);

      LoadReply reply = service.loadRequest();

      assertEquals(LoadReplyType.LOAD_RETRYLATER, reply.getType());
      assertEquals(ServiceState.DISENGAGED, service.getState());
  }

  /**
    * Scenario di test 5:
    * Verifica che, dopo l'accettazione di una richiesta di carico,
    * il mancato deposito del container entro il tempo massimo
    * provochi il timeout dell'operazione. Il cargoservice deve
    * tornare nello stato DISENGAGED e spegnere il LED.
    */
  @Test
  void T5_systemReturnsDisengagedAfterDepositTimeout() {
      CargoService service = new CargoService();

      service.setOutOfService(false);
      service.setIoPortFree(true);
      service.setSlotFree("slot1");

      service.loadRequest();

      service.depositTimeoutExpired();

      assertEquals(ServiceState.DISENGAGED, service.getState());
      assertFalse(service.isLedOn());
  }
```

// =============================================================================
= Project
// =============================================================================

Dall'analisi dei requisiti si propone, come ipotesi iniziale, una struttura a *tre sprint* con integrazione progressiva dei componenti. La suddivisione serve a organizzare il lavoro e i test, non a fissare decisioni architetturali definitive.

== Sprint 1: Core business

*Goal:* realizzare un primo prototipo eseguibile del *cargoservice* che implementi il
comportamento principale descritto dai requisiti mediante collaboratori simulati.

Al termine dello Sprint1 il sistema dovrà essere in grado di:

- ricevere una *load_request* proveniente dall'IOPort;
- verificare lo stato della hold, dell'IOPort e del servizio;
- produrre una delle tre risposte previste:
  - *load_accepted(slotID)*;
  - *load_retrylater*;
  - *load_refused*;
- mantenere il modello logico della hold e la prenotazione dello slot;
- gestire gli stati *engaged* e *disengaged*;
- pilotare il LED in funzione dello stato del sistema;
- superare i test funzionali definiti nello Sprint0.

In questa fase il cargorobot, il sonar, il markerdevice e l'IOPort saranno rappresentati
tramite collaboratori simulati.

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