// -----------------------------------------------------------------------------
//  Sprint 1 esame Natali
// ----------------------------------------------------------------------------- 
// 

#import "../../shared/template.typ": iss-template, iss-table, nota, domanda 

#show: iss-template.with(
  title:         "Maritime CargoService",
  subtitle:      "Sprint 1",
  course:        "Ingegneria dei Sistemi Software",
  university:    "Alma Mater Studiorum . Università di Bologna",
  academic-year: "2025/2026",
  authors:       ("Davide Chirichella", "Gabriele Doti", "Daniele Maccagnan"),
) 

= Introduction

Il punto di partenza di questo sprint è l'architettura logica (recuperabile al seguente #link("https://github.com/chirichexe/iss-2026/blob/main/sprint0/docs/sprint0_v3.pdf")[link]) e la formalizzazione dei requisiti (recuperabile al seguente #link("https://github.com/chirichexe/iss-2026/blob/main/sprint0/prototype/cargosystem/src/cargosystem.qak")[link]).


#figure(
    image("../../sprint0/prototype/cargosystem/cargosystemarch.png"),
    caption: [Architettura definita nello Sprint0.]
)

Si riporta di seguito il goal dello Sprint 1.

L'obiettivo dello Sprint 1 è realizzare un prototipo eseguibile del *cargoservice* che implementi il ciclo principale di carico di un container, dalla ricezione della `load_request` fino al deposito del container nello slot riservato. Si prevede di realizzare i componenti non ancora pronti in forma simulata (come mock) e di realizzare il movimento del cargorobot, delegando questo compito ad un componente esterno fornito dalla nostra casa di produzione. 

// =============================================================================
= Requirements
// =============================================================================

I requisiti del progetto sono riportati nel documento disponibile al seguente #link("https://anatali.github.io/issLab2026/_static/docs/Protobook.pdf#page=331")[link]

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
  - lo stato attuale della hold
  - il messaggio *"Service working"* quando tutto sta procedendo correttamente
  - il messaggio *"Out of service"* se il sensore sonar misura la distanza (del container dal sonar stesso) D > D#sub[FREE] per almeno 3 secondi (possibile guasto del sonar)

// =============================================================================
= Requirement analysis
// =============================================================================

Una analisi approfondita dei requisiti è stata svolta nello #link("https://github.com/chirichexe/iss-2026/blob/main/sprint0/docs/sprint0_v3.pdf")[Sprint0]. Tali risultati vengono assunti come validi anche per lo Sprint 1.

In questo sprint si intende soddisfare il sottoinsieme di requisiti relativo al ciclo principale di carico di un container. In particolare, il sistema dovrà essere in grado di ricevere una richiesta di carico, verificare le condizioni per la sua accettazione, prenotare uno slot libero, gestire gli stati engaged e disengaged, attendere il deposito del container entro il tempo previsto, coordinare la movimentazione del robot verso slot5, richiedere la marcatura del container, completare il deposito nello slot riservato e riportare il robot nella posizione Home.

Rispetto allo Sprint 0 non sono emerse ulteriori ambiguità sui requisiti funzionali.

= Problem analysis <model>

L'implementazione completa del sistema presuppone dell'esistenza di altri componenti del sistema fisici o in forma simulata (*sonar*, *LED*, *markerdevice*, *IOPort* come web GUI) non ancora sviluppati. 
La loro realizzazione concreta è pianificata per gli sprint successivi. I componenti "mock" che replicheranno, per test, il comportamento di quelli mancanti verranno evoluti rispetto alla formalizzazione QAK già avvenuta in fase di Sprint0 (recuperabile al seguente #link("https://github.com/chirichexe/iss-2026/blob/main/sprint0/prototype/cargosystem/src/cargosystem.qak")[link]). 

Come emerso dall'analisi dei requisiti, questo componente funge da *orchestratore*: coordina le operazioni degli altri componenti del sistema al 
fine di eseguire le procedure di carico. 
Inoltre, le entità sono distribuite su quattro nodi separati ma, per non rallentare la prototipazione, tutti i componenti //(ad eccezione del cargorobot) 
verranno rappresentati nello stesso nodo (rappresentato da un Context), tenendo in considerazione che gli attori non condividono memoria, e comunicano
tra loro tramite scambio di messaggi.

== Rappresentazione dello stato interno della hold

In questa fase la gestione della hold viene rappresentata tramite un *POJO*.

La scelta è motivata dal fatto che la hold non rappresenta un'entità attiva del sistema, ma una struttura dati (già definita in fase di sprint0) che descrive lo stato interno dell'ambiente: posizione degli slot, ostacoli, IOPort, home del robot e stato di occupazione degli slot.

L'interfaccia `IHold` definisce le operazioni necessarie per la gestione della hold, indipendentemente dalla loro implementazione. La classe `Hold` ne costituisce l'implementazione concreta, occupandosi della rappresentazione del suo stato e della logica di assegnazione degli slot.

Inoltre, lo stato iniziale della hold viene caricato da un file di configurazione JSON, così da evitare valori hard-coded nel codice e rendere più semplice modificare la disposizione dell'ambiente senza cambiare la logica applicativa.

Il file di configurazione è disponibile al seguente link:
#link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/prototype/src/hold_config.json")[configurazione hold]

Il codice della classe `Hold` è disponibile al seguente link:
#link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/prototype/src/Hold.java")[codice Hold]

Il codice dell'interfaccia `IHold` è disponibile al seguente link:
#link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/prototype/src/IHold.java")[codice Hold]

```java
public interface IHold {
    int doReserveSlot();
    void doFreeSlot(int slotId);
    int doGetSlotX(int slotId);
    int doGetSlotY(int slotId);
    int doGetHomeX();
    int doGetHomeY();
    CellType getCell(int x, int y);
    boolean isOccupied(int slotId);
}
```

== cargorobot

Come concordato con il committente, si è scelto di utilizzare il simulatore di ambiente virtuale *VirtualRobot26* e di riutilizzare il componente *robotsmart26* per la gestione della navigazione del robot. Tale scelta consente di sfruttare un software già disponibile e collaudato, evitando di implementare da zero le funzionalità di movimento e pianificazione del percorso e concentrando lo sviluppo sulla logica applicativa del sistema.

La documentazione di riferimento per *robotsmart26* è disponibile al seguente link:
#link("https://anatali.github.io/issLab2026/_static/docs/Protobook.pdf#chapter.30")[robotsmart26]

La specifica Qak del componente *robotsmart26* è disponibile al seguente link:
#link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/robotsmart26/src/robotsmart26.qak")[robotsmart26]



== Analisi delle Interazioni

Di seguito si riporta una formalizzazione delle interazioni tra gli attori modellate interpretando e applicando opportune scelte progettuali sui requisiti del sistema. Il codice è disponibile al seguente #link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/prototype/src/Prototype_Sprint1.qak")[link].

- Da requisiti, sappiamo che il sonar deve misurare continuamente la distanza del container dal sonar stesso. Nasce quindi la necessità di dover trasmettere queste misurazioni al cargoservice. Per isolare la responsabilità del sonar a semplice "misuratore e trasmettitore" di informazione, viene naturale formalizzare tale comunicazione come un Event, ovvero un messaggio broadcast che verrà ascoltato da chi interessato (in questo caso *cargoservice*)

```
Event    sonardata          : distance(D)
```

- L'invio di comandi al led è delegato al cargoservice. Non è necessario attendere una risposta, quindi utilizziamo una Dispatch.

```
Dispatch led_ctrl           : ledCmd(CMD) 
// CMD: "on", "off", "blink"
```

- Il cargoservice deve interrogare il markerdevice per sapere quando l'operazione di etichettatura è terminata, non potendo procedere finché il markerdevice non ha risposto. Usiamo quindi la Request/Reply.

```
//  CargoService <-> MarkerDevice 
Request mark_container : markContainer(none)
Reply   marking_done   : markingDone(none) for mark_container
```

- Per consentire lo spostamento del robot da una posizione a un'altra, cargoservice invia all'attore `robotsmart` una *request*, attraverso la quale è possibile specificare la destinazione (tramite coordinate) e il tempo di esecuzione. Alla ricezione della richiesta, `robotsmart` calcola il percorso verso la destinazione ed esegue il movimento del robot.

```qak
Request moverobot : moverobot(TARGETX, TARGETY, STEPTIME)
```

Al termine dell'operazione può rispondere con due possibili messaggi, uno per confermare l'avvenuto completamento del percorso e l'altro per segnalare eventuali problemi durante l'esecuzione del movimento. In quest'ultimo caso, il messaggio di risposta conterrà due parametri che indicano la parte di percorso già eseguita (`PLANDONE`) e quella ancora da percorrere (`PLANTODO`).

```qak
Reply moverobotdone : moverobotok(ARG) for moverobot
Reply moverobotfailed : moverobotfailed(PLANDONE, PLANTODO) for moverobot
```

== Rappresentazione dell'attore cargoservice come Macchina a Stati Finiti

L'attore cargoservice è, già da Sprint 0, inteso come una Macchina a Stati Finiti che utilizza variabili interne per:
- memorizzare se la porta di ingresso risulta occupata (IOPortOccupied)
- se il servizio è attualmente operativo (ServiceWorking)
- lo stato logico del servizio (CargoState) 
- l'identificativo dello slot eventualmente riservato (ReservedSlotId).
- durata del passo (in ms) di avanzamento di una singola cella da parte del robot (StepTime)

Per quanto riguarda le transizioni della FSM, si estende il comportamento già realizzato in Sprint0.

```qak
Context ctxprototype ip [host="localhost" port=8050] // Contesto unico del prototipo per lo Sprint 1
Context ctxrobotsmart      ip [host="127.0.0.1" port=8020]

ExternalQActor robotsmart context ctxrobotsmart

QActor cargoservice context ctxprototype {
    [#
        var IOPortOccupied = false
        var ServiceWorking = true
        var CargoState     = "disengaged"
        var ReservedSlotId = -1
        val StepTime       = 345
    #]
```

- Quando viene ricevuta una richiesta *load_request*, il cargoservice  prima di prenotare uno slot verifica che:
 1. il servizio non sia già impegnato nella gestione di un altro container
 2. il sistema risulti funzionante
 3. che la porta di ingresso sia libera.

Se tutte le condizioni risultino soddisfatte viene interrogata la Hold per richiedere la prenotazione di uno slot libero, il cargoservice memorizza l'identificativo dello slot riservato, passa allo stato engaged e informa il client dell'avvenuta accettazione della richiesta. 

Viene inoltre attivato il LED in modalità *blink* e viene comunicato l'ID dello slot (da mostrare in futuro sul display).
Nel caso in cui il magazzino risulti completamente occupato cargoservice comunicherà il rifiuto della richiesta, rimanendo disponibile per eventuali.

Il timer di 30 secondi viene avviato quando il sistema entra nello stato *engaged*. Se il container non viene posizionato nell'area del sensore entro tale intervallo, il sistema passa allo stato *disengaged*, libera lo slot precedentemente riservato e spegne il LED.


```qak
State s0 initial {
        println("cargoservice | STARTED") color magenta
    }
    Goto disengaged
    
    State disengaged {
        println("cargoservice | DISENGAGED: waiting for requests...") color blue
    }
    Transition t0
        whenRequest load_request -> handle_load_request
        whenEvent   sonardata    -> handle_sonar

    State engaged {
        println("cargoservice | ENGAGED: waiting for container deposit...") color blue
    }
    Transition t0
        whenTime    30000        -> handle_deposit_timeout
        whenRequest load_request -> handle_load_request
        whenEvent   sonardata    -> handle_sonar


    //GESTIONE RICHIESTE
    
    State handle_load_request {
        printCurrentMessage color yellow
        
        // Verifica precondizioni per accettare la richiesta
        if [# CargoState == "engaged" || !ServiceWorking || IOPortOccupied #] {
            replyTo load_request with load_retrylater : loadRetryLater(none)
        } else {
            // Interroga il POJO Hold per uno slot libero
            [# val SlotId = Hold.reserveSlot() #]
            if [# SlotId > 0 #] {
                [# 
                    ReservedSlotId = SlotId
                    CargoState     = "engaged" 
                #]
                forward ledmock -m led_ctrl : ledCmd(blink)
                [# val SlotName = "slot$ReservedSlotId" #]
                replyTo load_request with load_accepted : loadAccepted($SlotName)
            } else {
                replyTo load_request with load_refused : loadRefused(none)
            }
        }
    }
    Goto engaged if [# CargoState == "engaged" #] else disengaged

```

- Il cargoservic interpreta gli eventi emessi dal sonar in tre modi principali:

1. Viene rilevata la presenza di un container entro una certa distanza dal sensore (per almeno 3s) e viene impostata a true la variabile interna *IOPortOccupied* per indicare che l'IOPort è occupata.

2. Viene rilevata una distanza superiore a DFREE (per almeno 3s). Il sistema viene in questo caso messo nello stato *Out of service*. Viene quindi aggiornata la variabile interna *ServiceWorking*.

3. La distanza misurata non implica nessun cambiamento di stato. In questo caso il sistema continua a funzionare normalmente.


Al termine dell'elaborazione dell'evento il cargoserivce valuta se siano contemporaneamente soddisfatte tutte le condizioni necessarie per iniziare la movimentazione del container. Il robot viene attivato solamente quando il servizio si trova nello stato engaged, il container è stato effettivamente depositato e il sistema risulta operativo. 

Per semplicità, nello Sprint 1 non viene implementata la verifica della persistenza della misura per almeno 3 secondi richiesta dalla traccia. Il prototipo assume che ogni evento sonardata rappresenti una misura già validata dal sonar. La gestione completa del vincolo temporale verrà introdotta negli sprint successivi.

```qak
    State handle_sonar {
        onMsg(sonardata : distance(D)) {
            [# val Dist = payloadArg(0).toInt() #]
            
            // D < 50 indica presenza del container (IOPort occupata)
            if [# Dist < 50 #] { 
                [# IOPortOccupied = true #]
            } else {
                [# IOPortOccupied = false #]
            }

            // D > DFREE (es. > 150) per un intervallo prolungato indica condizione OUT OF SERVICE
            if [# Dist > 150 #] {
                [# ServiceWorking = false #]
                println("cargoservice | Sonar D > DFREE ($Dist) -> System OUT OF SERVICE!") color red
            } else {
                if [# !ServiceWorking #] {
                    println("cargoservice | Sonar D <= DFREE ($Dist) -> System WORKING again!") color green
                }
                [# ServiceWorking = true #]
            }
        }
    }
    // Se il container viene posato durante lo stato engaged e il sistema è operativo, il robot può iniziare
    Goto do_robot_job if [# IOPortOccupied && CargoState == "engaged" && ServiceWorking #] else returnToState

    State returnToState {}
    Goto engaged if [# CargoState == "engaged" #] else disengaged

```

La procedura di deposito è centralizzata nel *cargoservice*, mentre le operazioni di movimentazione del robot e di marcatura sono modellate tramite comunicazioni Request/Reply. In questo modo è garantita una sincronizzazione esplicita tra le fasi della procedura, impedendo che il deposito nello slot definitivo avvenga prima del completamento della marcatura.

Le coordinate delle destinazioni, invece, vengono recuperate dalla Hold.

Affinchè ogni nuova operazione inizi da una configurazione nota, al termine della procedura il robot viene riportato automaticamente nella posizione *Home*. 

Per consentire una successiva gestione del guasto, in caso di fallimento di uno spostamento, il cargoservice mantiene il sistema nello stato engaged e conserva la prenotazione dello slot. Si è scelto di non annullare automaticamente la procedura.


```qak
    // GESTIONE ROBOT E MARKER

    State do_robot_job {
        println("cargoservice | Container deposited! Moving robot to slot5 (2,5) [Row,Col] for marking via robotsmart26...") color magenta
        request robotsmart -m moverobot : moverobot(2, 5, $StepTime)
    }
    Transition t0
        whenReply moverobotdone   -> mark_container
        whenReply moverobotfailed -> handle_robot_fail
        
    State mark_container {
        println("cargoservice | At slot5. Asking markerdevice to mark...") color magenta
        request markerdevice -m mark_container : markContainer(none)
    }
    Transition t0
        whenReply marking_done -> move_to_reserved_slot
        
    State move_to_reserved_slot {
        [# 
            val DestX = Hold.getSlotX(ReservedSlotId)
            val DestY = Hold.getSlotY(ReservedSlotId)
        #]
        println("cargoservice | Marked! Moving container to slot$ReservedSlotId ($DestY, $DestX) [Row,Col] via robotsmart26...") color magenta
    	request robotsmart -m moverobot : moverobot($DestY, $DestX, $StepTime)
    }
    Transition t0
        whenReply moverobotdone   -> return_home
        whenReply moverobotfailed -> handle_robot_fail

    State return_home {
        [# 
            val HomeX = Hold.getHomeX()
            val HomeY = Hold.getHomeY()
        #]
        println("cargoservice | Container stored! Returning robot to HOME ($HomeY,$HomeX) [Row,Col] via robotsmart26...") color magenta
        request robotsmart -m moverobot : moverobot($HomeY, $HomeX, $StepTime)
    }
    Transition t0
        whenReply moverobotdone   -> finish_job
        whenReply moverobotfailed -> handle_robot_fail
        
    State finish_job {
        println("cargoservice | Job completed!") color green
        [# CargoState = "disengaged" #]
        forward ledmock -m led_ctrl : ledCmd(off)
    }
    Goto disengaged

    State handle_robot_fail {
        println("cargoservice | Robot movement failed!") color red
    }
    Goto engaged

    State handle_deposit_timeout {
        println("cargoservice | Deposit timeout! Freeing slot.") color red
        
        [# 
            CargoState = "disengaged" 
            Hold.freeSlot(ReservedSlotId)
        #]
        forward ledmock -m led_ctrl : ledCmd(off)
    }
    Goto disengaged
}
```

== Attori QAK di supporto

Gli attori QAK che rappresentano i dispositivi simulati sono riportati separando il comportamento di supporto al prototipo dalle parti usate esclusivamente come testplan.

```qak
QActor markerdevice context ctxprototype {
    State s0 initial {
        println("markerdevice | STARTED") color green
    }
    Goto work
    
    State work {}
    Transition t0
        whenRequest mark_container -> handle_mark
        
    State handle_mark {
        println("markerdevice | Marking container...") color cyan
        delay 1500
        println("markerdevice | Container marked!") color cyan
        replyTo mark_container with marking_done : markingDone(none)
    }
    Goto work
}

QActor ledmock context ctxprototype {
    State s0 initial { }
    Goto work
    
    State work {}
    Transition t0 
        whenMsg led_ctrl -> handle_cmd
        
    State handle_cmd {
        onMsg(led_ctrl : ledCmd(CMD)) {
            println("ledmock | LED is now ${payloadArg(0)}") color green
        }
    }
    Goto work
}
```

= Test plans <testplan>

Il testplan dello Sprint1 viene realizzato come scenario eseguibile interno al modello QAK (recuperabile al seguente #link("https://github.com/chirichexe/iss-2026/blob/main/sprint1/prototype/src/Prototype_Sprint1.qak")[link]).
Gli attori mock avviano automaticamente le interazioni necessarie a verificare il comportamento essenziale del `cargoservice`.

I test previsti sono:

- *TEST 1: richiesta accettata* \
  Eseguito da `ioportmock`: invia una prima `load_request` quando il servizio è libero. \
  Risultato atteso: `cargoservice` risponde con `load_accepted(slotN)` e invia `ledCmd(blink)` a `ledmock`.

- *TEST 2: nessun accodamento* \
  Eseguito da `ioportmock`: invia subito una seconda `load_request` mentre il servizio è `engaged`. \
  Risultato atteso: `cargoservice` risponde con `load_retrylater`, quindi la richiesta non viene accodata.

```qak
QActor ioportmock context ctxprototype {
    State s0 initial {
        // TEST 1: 
        delay 1000
        println("ioportmock | TEST 1 - Sending 1st load_request (expected load_accepted)") color cyan
        request cargoservice -m load_request : loadRequest(none)
    }
    Transition t0
        whenReply load_accepted   -> handle_accept
        whenReply load_retrylater -> handle_retry
        whenReply load_refused    -> handle_refuse

    State handle_accept {
        printCurrentMessage color green

        // TEST 2:
        delay 500
        println("ioportmock | TEST 2 - Sending 2nd load_request while engaged (expected load_retrylater)") color cyan
        request cargoservice -m load_request : loadRequest(none)
    }
    Transition t0
        whenReply load_retrylater -> handle_no_queue_ok
        whenReply load_accepted   -> handle_unexpected_accept
        whenReply load_refused    -> handle_refuse
}
```

- *TEST 3: deposito del container* \
  Eseguito da `sonarmock`: emette `sonardata: distance(30)`. \
  Risultato atteso: `cargoservice` rileva l'IOPort occupato e avvia la procedura di movimentazione del robot.

- *TEST 4: sonar out of service* \
  Eseguito da `sonarmock`: emette `sonardata: distance(200)`. \
  Risultato atteso: `cargoservice` imposta il sistema in stato *Out of service*.

- *TEST 5: ripristino sonar* \
  Eseguito da `sonarmock`: emette `sonardata: distance(100)`. \
  Risultato atteso: `cargoservice` riporta il sistema in stato *Working*.

```
QActor sonarmock context ctxprototype {
    State s0 initial {
        // TEST 3:
        delay 4000
        emit sonardata : distance(30)

        // TEST 4:
        delay 8000
        emit sonardata : distance(200)

        // TEST 5:
        delay 5000
        emit sonardata : distance(100)
    }
}
```

- *TEST 6: marcatura container* \
  Eseguito da `markerdevice`: riceve `mark_container` e risponde `marking_done`. \
  Risultato atteso: `cargoservice` prosegue verso il trasferimento allo slot riservato.

```
QActor markerdevice context ctxprototype {
    State handle_mark {
        // TEST 6:

        delay 1500
        replyTo mark_container with marking_done : markingDone(none)
    }
}
```

L'esecuzione del testplan avviene avviando il prototipo:

```bash
cd sprint1/prototype
./gradlew run
```

Le evidenze dei test sono visibili direttamente nel log prodotto dagli attori mock e dal `cargoservice`.

= Deployment <deployment>

Per procedere al deployment del prototipo è necessario eseguire i seguenti passaggi:

1. Clonare il repository del progetto da GitHub

2. Nella directory robotsmart26/yamls eseguire il comando 'docker compose -f unibobasic26.yaml up'

3. Aprire un broswer e andare su http://localhost:8090/

4. Eseguire all'interno della directory del progetto "robotsmart26" il comando `./gradlew run`

5. Eseguire all'interno della directory del progetto "prototype" il comando `./gradlew run`


// = Maintenance

= Pagina di sintesi
== Architettura finale del prototipo

L'architettura finale del prototipo sviluppato durante questo Sprint è riportata nella figura seguente, che costituirà il punto di partenza per lo Sprint 2.

#figure(
    image("../prototype/prototype_sprint1arch.png"),
    caption: [Architettura definita nello Sprint1.]
)


== Ripartizione del lavoro $$

Durante lo Sprint 1 il gruppo ha lavorato in modo collaborativo, condividendo le principali decisioni progettuali e distribuendo in maniera uniforme le attività di sviluppo.

Le principali aree di lavoro sono state:

- analisi dei requisiti e progettazione dell'architettura;
- implementazione del `CargoService`, della Hold e degli attori mock;
- integrazione con `RobotSmart` per la movimentazione del robot;
- definizione del testplan e verifica del corretto funzionamento del prototipo;
- redazione della documentazione tecnica.


== Goal dello Sprint 2

L'obiettivo dello Sprint 2 è evolvere il prototipo sviluppato sostituendo progressivamente i componenti simulati con le rispettive implementazioni reali.

Le attività previste sono:

- sostituire i mock con i componenti reali (IOPort come Web GUI, sonar e LED collegati al PicoW);
- completare l'integrazione con `VirtualRobot26` e `RobotSmart`;

Si stima una durata di circa *30 ore* complessive di lavoro ( \u{2248} ore per ciascun componente del gruppo).


== Goal dello Sprint 2

L'obiettivo principale dello Sprint 2 sarà incrementare il livello di realismo del prototipo sostituendo i componenti simulati con le rispettive implementazioni reali.

In particolare, le attività previste sono:

- sostituire il *cargorobotmock* con l'attore reale interfacciato a *VirtualRobot26*;
- realizzare l'*IOPort* come Web GUI, consentendo l'interazione dell'utente tramite browser;
- integrare i nuovi componenti mantenendo invariata l'architettura ad attori del sistema;
- verificare il corretto funzionamento del sistema mediante un aggiornamento dei test funzionali.

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
