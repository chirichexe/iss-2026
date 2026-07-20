# Indice del Test Plan (Aggiornato)

Ecco l'indice del test plan aggiornato con tutte le correzioni e integrazioni richieste, per renderlo più rigoroso e aderente ai requisiti dello sprint:

## 1. Gestione Richieste (IOPort)
* **Accettazione richiesta valida:** verificare che una richiesta di carico venga accettata se c'è spazio e il sistema è disponibile.
* **Richiesta rinviata per area di ingresso occupata:** verificare che una richiesta restituisca `retryLater` se la *Indoor Area* è già occupata da un altro container.
* **Richiesta rinviata per servizio non disponibile:** verificare che una richiesta restituisca `retryLater` se il servizio è fermo (es. per allarme/Out of Service).
* **Richiesta rifiutata per capacità esaurita:** verificare che una richiesta venga rifiutata se la stiva (*ColdRoom*) è completamente piena.
* **Richieste concorrenti durante un'operazione:** verificare che, mentre il sistema è già impegnato in un'operazione di carico, non vengano accettate nuove richieste.

## 2. Workflow di Carico e Movimentazione (Robot)
* **Avvio della procedura di movimentazione:** verificare che, subito dopo il deposito del container (inserimento del ticket), il sistema avvii correttamente la movimentazione prevista.
* **Completamento del workflow di carico:** verificare che il container venga movimentato fino allo spazio assegnato nella *ColdRoom* e che il sistema ritorni correttamente nello stato disponibile (Home).
* **Rilevamento dello spazio libero dopo il completamento di un carico:** verificare che, dopo aver occupato uno spazio, la richiesta successiva venga associata a un diverso spazio ancora disponibile.

## 3. Aggiornamento Stato e Osservabilità
* **Aggiornamento dello stato interno del sistema:** verificare che il sistema di backend mantenga costantemente traccia corretta dei pesi, dei container presenti e dello stato di ciascun singolo slot.
* **Aggiornamento delle informazioni mostrate all'utente:** verificare che le transizioni di stato (es. aggiornamento slot liberi/occupati, stato del robot) vengano notificate in tempo reale all'interfaccia web.

## 4. Eccezioni e Out of Service (Sonar / LED)
* **Gestione allarme Sonar (Out of Service):** verificare che il robot si fermi quando il sonar rileva un ostacolo sotto la distanza di sicurezza per un tempo prolungato.
* **Ripresa post-allarme:** verificare che il robot riprenda l'esecuzione dal punto in cui si era interrotto non appena le condizioni di sicurezza vengono ripristinate.
* **Segnalazione visiva (LED):** verificare che il LED rifletta correttamente lo stato del sistema (spento, lampeggiante, acceso fisso).
