package it.unibo.kactor

import alice.tuprolog.*
import kotlinx.coroutines.*
import unibo.basicomm23.interfaces.IApplMessage
import unibo.basicomm23.utils.ColorsOut
import unibo.basicomm23.utils.CommUtils
import java.util.NoSuchElementException

/*
Versione con interrupt updated by Lenzi e gestion whenTime
inserendo il concetto di azione ripetibile di sistema (sysaction)
 */

/*
================================================================
 STATE
================================================================
 */
class State(val stateName : String, val scope: CoroutineScope ) {
    private var _edgeList          = mutableListOf<Transition>()
    private val stateEnterAction  = mutableListOf< suspend (State) -> Unit>()
    private val sysstateEnterAction  = mutableListOf< suspend (State) -> Unit>()
    private val myself : State    = this
     val interruptedStateTransitions = mutableListOf<Transition>()   //OCT2023 (no more generated)

     // Versione pubblica immutabile
    val edgeList : List<Transition>
        get() = _edgeList // non basta assegnare a edgeList _edgeList, visto che è var e può
                          // cambiare riferimento

    fun transition(edgeName: String, targetState: String, cond: Transition.() -> Unit ) {
        val trans = Transition(edgeName, targetState)
        //println("      trans $name $targetState")
        trans.cond() //set eventHandler (given by user) See fireIf
        _edgeList.add(trans)
    }
    //Aug2022
    //Called by a state as a transition
    fun interrupthandle(edgeName: String, targetState: String,
             cond: Transition.() -> Unit, storage: MutableList<Transition> ) {
        val trans = Transition(edgeName, targetState, _edgeList, isInterrupt = true)
        //println("      trans $name $targetState")
        trans.cond() //set eventHandler (given by user) See fireIf
        _edgeList.add(trans)
        //interruptedState = myself  //MEMO THE INTERRUPTED STATE, Aug22, after Lenzi
    }
    //Add a system action which will be called when the state is entered
    fun sysaction(  a:  suspend (State) -> Unit) {
        //CommUtils.outred("State $stateName    | sysstateEnterAction  $a")
        sysstateEnterAction.add( a )
    }
    //Add an action which will be called when the state is entered
    fun action(  a:  suspend (State) -> Unit) {
        //println("State $stateName    | add action  $a")
        stateEnterAction.add( a )
    }

    suspend fun enterStateForSystemActions() {
        val myself = this
        scope.launch {
            //println(" --- | State $stateName    | enterState ${myself.stateName} ")
            if( sysstateEnterAction.size > 0 )
                 sysstateEnterAction.get(0)( myself )
        }.join()
    }
    suspend fun enterState() {
        val myself = this
        scope.launch {
            //println(" --- | State $stateName    | enterState ${myself.stateName} ")
            if( stateEnterAction.size > 0 )
                stateEnterAction.get(0)( myself )
            if( sysstateEnterAction.size > 0 )
                sysstateEnterAction.get(0)( myself )
        }.join()
        //println(" --- | State $stateName    | enterState DONE ")
        //stateEnterAction.forEach {  it(this) }
    }
    //Get the appropriate Edge for the Message
    fun getTransitionForMessage(msg: IApplMessage): Transition? {
        //println("State $name       | getTransitionForMessage  $msg  list=${edgeList.size} ")
        val first = _edgeList.firstOrNull { it.canHandleMessage(msg) }
        return first
    }

    fun storeTransitionsOfStateInterrupted( t: List<Transition> ) {
        //println("&&&&  $stateName storeTransitionsOfStateInterrupted ${t}")
        _edgeList  = mutableListOf<Transition>()   //clear
        t.forEach { _edgeList.add(it) }   //Ricopio
        //t.clear()                      //NOOO: Reset the storage (sideeffect)
    }
}

/*
================================================================
 Transition
================================================================
 */
class Transition(val edgeName: String, val targetState: String,
                 private val _globalEdges: MutableList<Transition>? = null, //Aug2022
                 val isInterrupt: Boolean = false, // Lenzi 3.0.1
                 ) {

    lateinit var edgeEventHandler: ( IApplMessage ) -> Boolean  //MsgId previous: String
    private val actionList       = mutableListOf<(Transition) -> Unit>()
    //private val globalActionList = mutableListOf<Transition>()
    val globalEdges : List<Transition>? = _globalEdges

    fun action(action: (Transition) -> Unit) { //MEALY?
        //println("Transition  | add ACTION:  $action")
        actionList.add(action)
    }

    //Invoke when you go down the transition to another state
    fun enterTransition(retrieveState: (String) -> State): State {
        //println("Transition  | enterEdge  retrieveState: ${retrieveState} actionList=$actionList")
        actionList.forEach { it(this) }         //MEALY?
        return retrieveState(targetState)
    }

    fun canHandleMessage(msg: IApplMessage): Boolean {
        //println("Transition  | canHandleMessage: ${msg}  ${msg is Message.Event}" )
        return edgeEventHandler( msg  ) //msg.msgId()
    }
}

/*
================================================================
 ActorBasicFsm
================================================================
 */
abstract class ActorBasicFsm(qafsmname:  String,
                             fsmscope: CoroutineScope = GlobalScope,
                             discardMessages : Boolean = false,
                             confined :    Boolean = false,
                             dynamically:    Boolean = false,
                             ioBound :     Boolean = false,
                             channelSize : Int = 400
                    ): ActorBasic(  qafsmname, fsmscope, discardMessages, confined, dynamically, ioBound, channelSize ) {

    val autoStartMsg = MsgUtil.buildDispatch(name, "autoStartSysMsg", "start", name)

    var isStarted = false  //public ? since see createActorFsm Oct2023  NO!
    protected var myself : ActorBasicFsm
    protected lateinit var currentState: State	//inherited
    protected var currentMsg : IApplMessage      = NoMsg
    protected var msgToReply       = NoMsg
    lateinit protected var mybody: ActorBasicFsm.() -> Unit
    var stateTimer : TimerActor?   = null

    private val stateList = mutableListOf<State>()
    private val msgQueueStore = mutableListOf<IApplMessage>()

    private var interruptStateTransitions : List<Transition>? = null
    private var interruptedState : State? = null //Aug22, after Lenzi

    var perceiving = true //JUNE25

    //================================== STRUCTURAL =======================================
    fun state(stateName: String, build: State.() -> Unit) {
        //val state = State(stateName, argscope)
        val state = State(stateName, myscope)   //Oct2023
        state.build()
        stateList.add(state)
    }

    private fun getStateByName(name: String): State {
         return stateList.firstOrNull { it.stateName == name }
            ?: throw NoSuchElementException(name)
    }
    //===========================================================================================

    init {
        //CommUtils.outyellow("$tt  %%% ActorBasicFsm | INIT $qafsmname")
        myself  = this
        perceiving = true  //JUNE25
        setBody(getBody(), getInitialState())
        //perdotempo
        if( dynamically ) CommUtils.outyellow("$tt  %%% ActorBasicFsm | $name DYNAMIC INIT")

	 }

    abstract fun getBody(): (ActorBasicFsm.() -> Unit)
    abstract fun getInitialState(): String



    fun setBody( buildbody: ActorBasicFsm.() -> Unit, initialStateName: String ) {
        buildbody()            //Build the structural part
        currentState = getStateByName(initialStateName)
        //CommUtils.outblue("ActorBasicFsm $name |  setBody  autoStartMsg currentState=${currentState.stateName}")
        //scope.launch {

        //PRIMA DI PARTIRE controllo MQTT
        //IL tempo necessario a far partire il Thread basta per completare
        //l'azione di addActor di QakContext di inject the context
        Thread(){
            run(){
                checkMqtt()
            }
        }.start();


        //sendMsgToMyself(autoStartMsg)  //LO FA checkMqtt
               //if( !confined ) sendMsgToMyself(autoStartMsg)  //Oct 2023
               //else sendMsgToMyself(dispatcher, autoStartMsg)   //Uso il dispatecher di ActorBasic l68

        //if( confined ) sendMsgToMyself(sysUtil.singleThreadContext, autoStartMsg)
        //else sendMsgToMyself(dispatcher, autoStartMsg)
            //autoMsg(autoStartMsg)
        //}    //auto-start
    }

	//Now there is a state ....
 	override  fun writeMsgLog( msg: IApplMessage ){ //APR2020
		//Update the log of the actor
         sysUtil.updateLogfile(actorLogfileName,  "item($name,${currentState.stateName},$msg).", dir=msgLogDir )  
		//Update the log of the context
         if( context !== null )
			 sysUtil.updateLogfile(context!!.ctxLogfileName, "item($name,${currentState.stateName},$msg).", dir=msgLogNoCtxDir )		
	}
	


    override suspend fun actorBody(msg: IApplMessage) {
        //CommUtils.outred("ActorBasicFsm $name | actorBody $msg")
         if ( !isStarted && msg.msgId() == autoStartMsg.msgId() ) fsmStartWork( msg )
         else  fsmwork(msg)
    }


    suspend fun fsmStartWork( msg: IApplMessage ) {
        isStarted = true
        //CommUtils.outmagenta("$tt ActorBasicFsm $name | fsmStartWork in ${currentState.stateName} $msg")
        currentMsg = msg
        currentState.enterState()
        var nextState = checkTransition(NoMsg) //check FIRST EMPTY MOVE
        while (nextState is State) {
            currentMsg   = NoMsg
            currentState = nextState
            currentState.enterState()
			nextState = checkTransition(NoMsg) //check other EMPTY MOVE
         }
        //sysUtil.traceprintln("$tt ActorBasicFsm $name | fsmStartWork ENDS ")
     }

     suspend fun fsmwork(msg: IApplMessage) {
        //CommUtils.outmagenta("$tt ActorBasicFsm $name | fsmwork in ${currentState.stateName} $msg")
         //Guardo se l'attore ha delegato quati tipo di msg
         val delegatedactor = delegated.get(msg.msgId()) //April2023
         if( delegatedactor != null ){
             //CommUtils.outyellow("$tt ActorBasicFsm  $name | delegates ${msg.msgId()} to ${delegatedactor.name} ")
             //delegatedactor.autoMsg(msg) // May2023
             delegatedactor.actor.send(msg);
             //sendMsgToMyself(msg)
             return
        }
        var nextState = checkTransition(msg)
        var b         = handleCurrentMessage(msg, nextState)
        if (b)  elabMsgInState( )
        //sysUtil.traceprintln("$tt ActorBasicFsm $name | fsmwork ENDS for $msg")
   }

    //JAN24
    suspend fun delegateCurrentMsgTodynamic( actorName : String ){
        //val actor = context!!.hasActor(actorName)
        //if( actor != null ){
        val createdactor = createActorFsmDynamic(actorName, "", false )
        sysUtil.traceprintln("$tt  %%% ActorBasicFsm | delegateCurrentMsgTodynamic ${currentMsg} to ${createdactor.name} ")
        //CommUtils.outyellow("$tt  %%% ActorBasicFsm | delegateCurrentMsgTodynamicxyz ${currentMsg} to ${createdactor.name} ")
        delay(500)  //GIVE TIME TO CREATE
        createdactor.actor.send(currentMsg);  //REDIRECTION
        //}
        //else CommUtils.outred("%%% ActorBasic | WARNING: delegation to non local actor $actorName not allowed ")
    }

    //Aug2022
    //Lenzi 25/08: Deprecato, non necessario passare dall'esterno
    // mantenuto per retrocompatibilità con Qak <3.0
    @Deprecated("Passing list no longer needed since Qak 3.0.1", ReplaceWith("returnFromInterrupt()"))
    suspend fun returnFromInterrupt( t: MutableList<Transition> ) = returnFromInterrupt()

    suspend fun returnFromInterrupt() {
        //println("&&&&  ${currentState.stateName} returnFromInterrupt ${t.size}")
        // Copia immutabile per gestire multithreading ecc.
        val currentInterruptTransitions = interruptStateTransitions
        if (currentInterruptTransitions != null) {
            //runBlocking {
            interruptedState?.enterStateForSystemActions()  //for whentime
            //}
            currentState.storeTransitionsOfStateInterrupted(currentInterruptTransitions)
        } else {
            ColorsOut.outerr("$tt ActorBasicFsm $name | State: ${currentState.stateName}, tried to return from interrupt when not inside an interrupt!")
        }
        interruptStateTransitions = null
        interruptedState = null
    }

    suspend fun elabMsgInState( ) {
        sysUtil.traceprintln("$tt ActorBasicFsm $name | elabMsgInState in ${currentState.stateName} $currentMsg")
    	currentState.enterState() //execute local actions (Moore automaton)
	    checkEmptyMove() 
    	if( elabMsgQueueStore() ) elabMsgInState()
    }
	 


    suspend fun checkEmptyMove() {
		//sysUtil.traceprintln("$tt ActorBasicFsm $name | checkDoEmptyMoveInState msgQueueStoreSize=:${msgQueueStore.size}")
        var nextState = checkTransition(NoMsg) //EMPTY MOVE
        if (nextState is State) {
            currentMsg   = NoMsg
			currentState = nextState
			elabMsgInState( )
        }
    }



     fun handleCurrentMessage(msg: IApplMessage, nextState: State?, memo: Boolean = true): Boolean {
        //sysUtil.traceprintln("$tt ActorBasicFsmmmm $name | handleCurrentMessage in ${currentState.stateName} msg=${msg.msgId()} memo=$memo")
         //CommUtils.outblue("$tt ActorBasicFsmmmm $name | handleCurrentMessage $msg in ${currentState.stateName} msg=${msg.msgId()} mapsize=${requestMap.size}")
         if (nextState is State) {
            currentMsg   = msg
              if( currentMsg.isRequest() ){
				//Dec2021: currentMsg.msgId() + currentMsg.msgReceiver() MEGLIO !
				//Chi fa requestMap.remove? Il metodo answer in ActorBasic

				    //requestMap.put(currentMsg.msgId()+currentMsg.msgSender(), currentMsg)

                    addValueToRequestMap(currentMsg.msgId()+currentMsg.msgSender(), currentMsg)
            }  //Request
            var msgBody = currentMsg.msgContent()
            val endTheTimer = currentMsg.msgId() != "local_noMsg" &&
                            ( ! msgBody.startsWith("local_tout_")
                                    ||
                                //msgBody.startsWith("local_tout_") &&
                                    ( msgBody.contains(currentState.stateName) &&
                                      msgBody.contains(this.name) )
                            )
            currentState = nextState

            if( endTheTimer && (stateTimer !== null) ){
                stateTimer!!.endTimer() //terminate TimerActor
             }

             if( currentMsg.isEvent() ){
                 //CommUtils.outblack("$tt ActorBasicFsm $name | handle event $msg -> ${currentState.stateName}" )
                 if( ! msgBody.startsWith("local_tout_") )
                 return perceiving
                 else return true
             }

             return true
        } else { //No nextstate => EXCLUDE EVENTS FROM msgQueueStore
            if (!memo) return false   //
            if (!(msg.isEvent()) && ! discardMessages) {
                msgQueueStore.add(msg)
                println("$tt ActorBasicFsm $name|${currentState.stateName}:adds $msg in msgQueueStore")
                logger.info("     ActorBasicFsm |${currentState.stateName}:adds $msg in msgQueueStore")
            }
            else {
				//sysUtil.traceprintln("$tt ActorBasicFsm $name | DISCARDING : ${msg.msgId()}")
				sysUtil.updateLogfile(actorLogfileName,  "discard($name,${currentState.stateName},$msg).", dir=msgLogDir) 
			}
			return false
        }
    }

     protected fun msgQueueStore_size(  )  : Int {
        return msgQueueStore.size
    }

    /*
    Il blocco synchronized(msgQueueStore) garantisce che nessun altro thread modifichi la lista esternamente,
    ma non ti protegge dalle modifiche effettuate all'interno del ciclo stesso sullo stesso thread.
     */
    suspend protected fun msgQueueStore_clean(  )   {
        //synchronized(msgQueueStore) {
            val iterator = msgQueueStore.iterator()
            while (iterator.hasNext()) {
                val msg = iterator.next()
                logger.info("      ActorBasicFsm |${currentState.stateName}:removed $msg")
                iterator.remove() // Rimuove l'elemento corrente in modo sicuro
            }
        //}
        /*
        msgQueueStore.forEach {
            //logger.info("$tt ActorBasicFsm |${currentState.stateName}:removed $it")
            msgQueueStore.remove(it)
         }*/
        println("$tt ActorBasicFsm $name|${currentState.stateName}: msgQueueStore cleaned")
    }

	suspend private fun elabMsgQueueStore(  ) : Boolean {
        msgQueueStore.forEach {
           val state = checkTransition(it)
           if (state is State) {				
        	    sysUtil.traceprintln("$tt ActorBasicFsm $name | elabMsgQueueStore state=${state.stateName},curState=${currentState.stateName},it=${it}  ")
                currentMsg = msgQueueStore.get( msgQueueStore.indexOf(it) )
                msgQueueStore.remove(it)
				var b = handleCurrentMessage(currentMsg, state)	//sets currentState
				//sysUtil.traceprintln("$tt ActorBasicFsm $name | elabMsgQueueStore state=${state.stateName},curState=${currentState.stateName}, currentMsg=$currentMsg ")
				if( b ) return true		 
           }
		}
        return false
	}

    private fun checkTransition(msg: IApplMessage): State? {
        val trans = currentState.getTransitionForMessage(msg)
        //sysUtil.traceprintln("$tt ActorBasicFsm $name | checkTransition, $msg, curState=${currentState.stateName}, trans=$trans")
        return if (trans != null) {
             if (trans.isInterrupt) {
                if (interruptStateTransitions == null) {
                    interruptedState = currentState  //AterLenzi
                    interruptStateTransitions = trans.globalEdges!!
                } else {
                    throw IllegalStateException("$tt ActorBasicFsm $name | Cannot nest interruptions!")
                }
             }
             trans.enterTransition {
                  getStateByName(it)
             }
        } else {
            //sysUtil.traceprintln("$tt ActorBasicFsm $name | checkTransition in ${currentState.stateName} NO next State for $msg !!!")
            null
        }
    }

    fun doswitch(): Transition.() -> Unit {
        return { edgeEventHandler = { true } }
    }
    fun doswitchGuarded( guard:()->Boolean ): Transition.() -> Unit {
        return { edgeEventHandler = { guard() } }
    }


	
    fun whenEvent(evName: String): Transition.() -> Unit {
        return {
            edgeEventHandler = {
                //println("whenEvent $it - $evName");
                it.isEvent() && it.msgId() == evName }
                //it == evName } //it.isEvent() && it.msgId() == evName }
        }
    }
    fun whenEventGuarded(evName: String, guard:()->Boolean ): Transition.() -> Unit {
        return {
            edgeEventHandler = {
                //println("whenEventGuarded $it - $evName");
                it.isEvent() && it.msgId() == evName && guard() }
                //it == evName && guard()  } //it.isEvent() && it.msgId() == evName }
        }
    }

    fun whenDispatch(msgName: String ): Transition.() -> Unit {
			//println("$tt ActorBasicFsm $name | whenDispatch  planning $msgName  " )
            return {
                edgeEventHandler = {
                    //println("ActorBasicFsm $name | ${currentState.stateName} whenDispatch $it  $msgName  ")
                    it.isDispatch() && it.msgId() == msgName  }
                    //it == msgName }  //it.isDispatch() && it.msgId() == msgName }
            } 
    }
    fun whenDispatchGuarded(msgName: String, guard:()->Boolean ): Transition.() -> Unit {
        return {
            edgeEventHandler = {
                //if(it.isDispatch() && it.msgId() !== "local_noMsg") CommUtils.outcyan("whenDispatchGuarded $it - $msgName guard=${guard()}");
                it.isDispatch() && it.msgId() == msgName && guard()  }
                //it == msgName && guard() } //it.isDispatch() && it.msgId() == msgName }
        }
    }

    fun whenRequest(msgName: String): Transition.() -> Unit {
        //sysUtil.traceprintln("$tt ActorBasicFsm $name | whenRequest $currentMsg" )
        return {
            edgeEventHandler = {
                //sysUtil.traceprintln("$tt ActorBasicFsm $name | ${currentState.stateName} whenRequest $it  $msgName")
                it.isRequest() && it.msgId() == msgName }
                //it == msgName   }  //it.isRequest() && it.msgId() == msgName }
        }
    }
    fun whenRequestGuarded(msgName: String, guard:()->Boolean): Transition.() -> Unit {
        return {
            edgeEventHandler = {
                //sysUtil.traceprintln("$tt ActorBasicFsm $name | ${currentState.stateName} whenRequestGuarded $it, $msgName")
                it.isRequest() && it.msgId() == msgName && guard()  }
                //it == msgName   && guard() }  //it.isRequest() && it.msgId() == msgName }
        }
    }
    fun whenReply(msgName: String): Transition.() -> Unit {
        return {
            edgeEventHandler = {
                //sysUtil.traceprintln("$tt ActorBasicFsm $name | ${currentState.stateName} whenReply $it  $msgName")
                it.isReply() && it.msgId() == msgName }
                //it == msgName   }  //it.isReply() && it.msgId() == msgName }
        }
    }
    fun whenReplyGuarded(msgName: String, guard:()->Boolean): Transition.() -> Unit {
        return {
            edgeEventHandler = {
                //sysUtil.traceprintln("$tt ActorBasicFsm $name | ${currentState.stateName} whenReplyGuarded $it  $msgName")
                it.isReply() && it.msgId() == msgName && guard() }
                //it == msgName  && guard() }  //it.isReply() && it.msgId() == msgName }
        }
    }

    fun whenTimeout( timerEventName : String ): Transition.() -> Unit {
                return {
                    edgeEventHandler = {
                        //println("whenTimeoutt $it")
                        it.isEvent() && it.msgId() == timerEventName
                        //it == timerEventName
                    } //it.isEvent() && it.msgId() == timerEventName }
                }
    }



    fun storeCurrentMessageForReply() {
        msgToReply = currentMsg
        //println(getName() + " 			msgToReply:" +msgToReply );
    }




    suspend fun replyToCaller(msgId: String, msg: String) {
        //sysUtil.traceprintln("$tt ActorBasicFsm $name | replyToCaller msgToReply=" + msgToReply);
        val caller = msgToReply.msgSender()
        //println( " replyToCaller  $msgId : $msg  to $caller" );
        forward(msgId, msg, caller)
    }
	
/*
 -------------------------------------------------------------------
UTILITIES TO HANDLE MSG CONTENT
 -------------------------------------------------------------------
 */
    private var msgArgList = mutableListOf<String>()

	//called by onMsg translated
    fun checkMsgContent(template : Term, curT : Term,  content : String ) : Boolean{
        msgArgList = mutableListOf<String>()
        if( pengine.unify(curT, template ) && pengine.unify(curT, Term.createTerm(content) ) ){
            val tt   = Term.createTerm( curT.toString() )  as Struct
            val ttar = tt.arity
            for( i in 0..ttar-1 ) msgArgList.add( tt.getArg(i).toString().replace("'","") )
            return true
        }
        return false
    }
    fun  payloadArg( n : Int  ) : String{
         return msgArgList.elementAt(n)
    }

    fun subscribe(  topic: String ) {
        mqtt.subscribe(myself,topic)
    }

    fun connectFsmMqtt(mqttAddr: String, topic:String) { //JAN25
        CommUtils.delay(1000) //
        CommUtils.outcyan("$name  | connectFsmMqtt ${context!!.name} $mqttAddr")
           context!!.mqttAddr = mqttAddr
            mqtt.connect(name, mqttAddr)
            mqttConnected = true
            mqtt.subscribe(myself, "unibo/qak/$name")      //Anche a questa?
            mqtt.subscribe(myself, topic)
            sysUtil.mqttBrokerEventTopic = topic
            sysUtil.ctxActorMap.put(name,this  )
        CommUtils.outcyan("$name  | connectFsmMqtt with topic=unibo/qak/$name and $topic")
    }

    fun connectMqttSendOnly( mqttAddr: String, name: String ) { //FEB25
        //CommUtils.delay(1000) //
        CommUtils.outcyan("$name  | connectMqttSendOnly ${context!!.name} $mqttAddr")
        //context!!.mqttAddr = mqttAddr
        mqtt.connect(name, mqttAddr)
        /*
        mqttConnected = true
        mqtt.subscribe(myself, "unibo/qak/$name")      //Anche a questa?
        mqtt.subscribe(myself, topic)
        sysUtil.mqttBrokerEventTopic = topic
        sysUtil.ctxActorMap.put(name,this  )
        CommUtils.outcyan("$name  | connectFsmMqtt with topic=unibo/qak/$name and $topic")

         */
    }

    fun updateCellNameInContext(name: String){
        CommUtils.outmagenta("$name  | updateCellNameInContext $name")
        sysUtil.ctxActorMap.put(name,this  )
    }
/*
    fun storeCurrentRequest( ){
        requestMap.put(currentMsg.msgId(), currentMsg)
    }
*/

}