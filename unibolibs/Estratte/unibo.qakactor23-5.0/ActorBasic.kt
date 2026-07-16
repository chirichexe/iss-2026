package it.unibo.kactor

import alice.tuprolog.Prolog
import alice.tuprolog.SolveInfo
import alice.tuprolog.Struct
import alice.tuprolog.Term
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.CoapResource
import org.eclipse.californium.core.coap.CoAP.ResponseCode.CHANGED
import org.eclipse.californium.core.coap.CoAP.ResponseCode.DELETED
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import unibo.basicomm23.coap.CoapConnection
import unibo.basicomm23.interfaces.IApplMessage
import unibo.basicomm23.interfaces.Interaction
import unibo.basicomm23.msg.ApplMessage
import unibo.basicomm23.tcp.TcpConnection
import unibo.basicomm23.utils.CommUtils
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer

/*
    Implements an abstract actor able to receive an ApplMessage and
    to delegate its processing to the abstract method actorBody
 */

abstract class  ActorBasic(name:         String,
                           val argscope: CoroutineScope = GlobalScope,
                           var discardMessages : Boolean = false,
                           var confined :    Boolean = false,
                           var dynamically:    Boolean = false,
                           val ioBound :     Boolean = false,
                           val channelSize : Int = 200
                        ) : CoapResource(name), MqttCallback {

    //val cpus = Runtime.getRuntime().availableProcessors();

    val tt      = "               %%% "
    var context : QakContext? = null  //to be injected
    var resVar  : String ="fail"      // see solve
    val pengine     = Prolog()      //USED FOR LOCAL KB
    val NoMsg       = MsgUtil.buildEvent(name, "local_noMsg", "noMsg")
				
    val mqtt        = MqttUtils(name)
    val logger      = org.slf4j.LoggerFactory.getLogger(name)
    var MyName      = name  //OCT23 var:JAV25

    protected val subscribers = mutableListOf<ActorBasic>()
    public var observers: Vector<ActorBasic> = Vector<ActorBasic>()
    //protected var observersMsgids: MutableMap<String, String> = mutableMapOf<String, String>()
    var mqttConnected         = false
    protected var count       = 1;

    protected lateinit var currentSolution : SolveInfo
    protected lateinit var currentProcess  : Process
 
//    private var timeAtStart: Long = 0
    //internal
    protected val requestMap : MutableMap<String, MutableList<IApplMessage>> = mutableMapOf<String, MutableList<IApplMessage>>()  //Sept2023
    protected val delegated : MutableMap<String, ActorBasic>   = mutableMapOf<String, ActorBasic>()  //April2023
    private   var logo             : String 	        //Coap Jan2020
    protected var ActorResourceRep : String 			//Coap Jan2020
	protected var actorLogfileName  : String = ""
	protected var msgLogNoCtxDir   = "logs/noctx"
	protected var msgLogDir        = msgLogNoCtxDir

    //Sept 2023
    //protected var dynamicOnly         = false
    protected var Creator             = ""
    protected var executorName        = ""
    protected var executorNameCounter = 0

    protected lateinit var myscope : CoroutineScope

    protected var isTerminated = false

    private var _currentLocalRequestAnswer: String = "none"

    //public  lateinit var currentLocalRequestAnswer : String
    // Proprietà pubblica con backing field privato

    @JvmField
    var currentLocalRequestAnswer: String = "none"
        /*
        get() = _currentLocalRequestAnswer
        set(value) {
            _currentLocalRequestAnswer = value
        }
*/

    /*
    protected val dispatcher =
        if( confined ) sysUtil.singleThreadContext
        else  if( ioBound ) sysUtil.ioBoundThreadContext
        else sysUtil.cpusThreadContext
*/
    protected lateinit var dispatcher : ExecutorCoroutineDispatcher

    init{                           //Coap Jan2020
 		//createMsglogFile()		//APR2020 : an Actor could have no context REMOVED OCT2023
        isObservable     = true
        logo             = "ActorBasic(Resource) $name "
        ActorResourceRep = "$name(createdtobediscared)"  //DEC23 non lo trasmetto: see CoapObserverSupport
        //sysUtil.aboutThreads("CREATED actor $name - dispatcher with ${sysUtil.cpus} threads"
    /*
        dispatcher =
            if( confined ) sysUtil.singleThreadContext
            else  if( ioBound ) sysUtil.ioBoundThreadContext
            else sysUtil.cpusThreadContext
*/
        //CommUtils.outmagenta("$name | ActorBasic init confined=$confined")

        if( confined )
            myscope = sysUtil.confinedscope
        else
            myscope = sysUtil.cpuRelatedscope
        //CommUtils.outred("$tt $name | init argscope=$argscope confined=$confined ioBound=$ioBound")
    }


    public fun getCLRAnswer() : String {
        return currentLocalRequestAnswer
    }
    public fun setCLRAnswer(v:String) {
        currentLocalRequestAnswer = v
        CommUtils.outred("setCLRAnswer $currentLocalRequestAnswer")
    }
    public fun waitCLRAnswer(v:String) {
        while( currentLocalRequestAnswer != v ){
            sysUtil.traceprintln("waitCLRAnswer $currentLocalRequestAnswer/$v")
            CommUtils.delay(100)
        }
     }
/*
 ===================================================
Message-driven kactor
 ===================================================
*/ 	
	
	open fun createMsglogFile(){
		actorLogfileName  = "${name}_MsLog.txt"
        sysUtil.createFile(actorLogfileName, dir = msgLogNoCtxDir)
	}
	fun createMsglogFileInContext(){	//called by QAkContext.addInternalActor when a context is injected
		if( context !== null ){
            sysUtil.deleteFile(actorLogfileName, dir = msgLogNoCtxDir)
			msgLogDir = "logs/${context!!.name}"
            sysUtil.createFile(actorLogfileName, dir = msgLogDir)
		}else
		    println("ActorBasic $name | WARNING: createMsglogFileInContext you should never be here")
	}
	
 	open fun writeMsgLog( msg: IApplMessage){ //APR2020
          if( context !== null ){ 
        	  //Update the log of the context
              sysUtil.updateLogfile(context!!.ctxLogfileName, "item($name,nostate,$msg).", dir = msgLogNoCtxDir)
		  }	
          //Update the log of the actor
        sysUtil.updateLogfile(actorLogfileName, "item($name,nostate,$msg).", dir = msgLogDir)
	}


    //val actor = argscope.actor<IApplMessage>(   capacity=channelSize ) {
    val actor = myscope.actor<IApplMessage>(   capacity=channelSize ) {
            //sysUtil.aboutThreads("ActorBasic $name|Created ${sysUtil.cpus}-cpus confined=$confined  "  )
            for( msg in channel ) {
                sysUtil.traceprintln("$tt ActorBasic  $name |  msg= $msg ")
                //writeMsgLog( msg )
                if( msg.msgContent() == "stopTheActor") {  channel.close() }
                else{
                    actorBody( msg )
                }
            }
        }


        //To be overridden by the application designer
        abstract suspend fun actorBody(msg : IApplMessage)



        fun setDiscard( v: Boolean){
            discardMessages = v
        }
/*
SEPT 2024
 */
    fun clearlog(fname: String){
       sysUtil.clearlog(fname)
    }

/*
TERMINATION 
*/
        fun currentNumOfActors() : Int {
            return  context!!.actorMap.size
        }

        fun terminate(arg: Int=0){
            if( context !== null ) {
                context!!.actorMap.remove(  name )
                sysUtil.removeqactorfact( name )  //Rimuove dalla base di conoscenza
            }
            if( ! name.contains("route") )
                sysUtil.traceprintln("$tt ActorBasic $name terminates. Num of actors in ${context!!.name}=${context!!.actorMap.size} ")
            actor.close()
            isTerminated = true
            mqtt.disconnect();
        }
        fun terminateCtx(arg: Int=0){
            println("$tt ActorBasic $name | terminateCtx $arg TODO ")
            //context!!.terminateTheContext()
        }


        suspend fun waitTermination(){
            (actor as Job).join()
        }
/*
--------------------------------------------
Messaging
--------------------------------------------
 */

        suspend open fun autoMsg(  msg : IApplMessage) {
            sysUtil.traceprintln("$tt ActorBasic $name | autoMsg $msg actor=${actor}")
            //CommUtils.("$tt ActorBasic $name | autoMsg $msg actor=${actor}")
            actor.send( msg )
        }

        //ADDED April 2022 to avoid loop
        fun sendMsgToMyself( msg : IApplMessage){
            //argscope.launch { autoMsg(msg) }
            myscope.launch { autoMsg(msg) }  //OCT2023
        }
/*
        fun sendMsgToMyself( dispatcher : CoroutineDispatcher, msg : IApplMessage){
            //CommUtils.outred("$name | ActorBasic sendMsgToMyself $dispatcher")
            val newScope = CoroutineScope( dispatcher )
            newScope.launch { autoMsg(msg) }
        }
*/
        suspend fun autoMsg( msgId : String, msg : String) {
            actor.send(MsgUtil.buildDispatch(name, msgId, msg, this.name))
        }



        //Oct2019

        fun sendAnswerToLocalCaller( msg: String ){
            CommUtils.outred("ANSWER NOT SENT (BEFORE): $msg   " +
                    "CLRAnswer=$currentLocalRequestAnswer myself=" + MyName )
            setCLRAnswer( msg.toString() )

            CommUtils.outred("ANSWER NOT SENT (AFTERa): v=$currentLocalRequestAnswer"  )
            CommUtils.outred("ANSWER NOT SENT (AFTERb): v=${getCLRAnswer()} myslef=$MyName"  )
        }

        suspend fun sendMessageToActor(msg : IApplMessage, destName: String, reqMsg : IApplMessage?=null, conn : Interaction? = null ) {
            //CommUtils.outmagenta("$tt ActorBasic sendMessageToActor | destName=$destName  conn=$conn")
            if( context == null ){  //Defensive programming
                sysUtil.traceprintln("$tt ActorBasic $name sendMessageToActor |  no QakContext for the current actor")
                return
            }

            if( destName == "directlocal"){//trick dec2024
                  sendAnswerToLocalCaller(msg.toString());
                return
            }


            val destactor = context!!.hasActor(destName)
            //CommUtils.outcyan("$tt ActorBasic $name sendMessageToActor | $msg ${destactor} $destName")
            if( destactor is ActorBasic) { //DESTINATION LOCAL
                //println("$tt ActorBasic sendMessageToActor | ${msg.msgId()}  dest=$destName LOCAL IN ${context!!.name}")
                destactor.actor.send( msg )
                return
            }
            /*
            if( ! (destactor is ActorBasic) ) { //defensive
                CommUtils.outred("$tt ActorBasic $name | $msg ??? $destName")
            }
            */
            val ctxNameCheck = sysUtil.getActorContextName(destName) //Nov23
            //CommUtils.outcyan("$tt ActorBasic sendMessageToActor | ${destName} ctxNameCheck = ${ctxNameCheck}  ")
            if(  ctxNameCheck != null ){ //conosco il nome del contesto (not dinamicamente) Nov23

                val host: String? =  sysUtil.solve("context( $ctxNameCheck,HOST,_,_ )", "HOST")
                //CommUtils.outyellow("$tt ActorBasic $name  | sendMessageToActor $destName on host=${host!!} of ${ctxNameCheck}  ")
                if( host!! == "discoverable"){ //OCT24
                    discoverAndSend(msg, ctxNameCheck, reqMsg)
                    return; //TODO
                }


                val proxy = context!!.proxyMap.get(ctxNameCheck)
                if( proxy != null  ) {
                    //CommUtils.outyellow("$tt ActorBasic sendMessageToActor |  ${msg.msgId()} to $destName via PROXY $proxy ${proxy.MyName}")
                    proxy.actor.send( msg )
                }
                else {

                    CommUtils.outmagenta("$tt WARNING 1. ActorBasic $name  | sendMessageToActor ${msg.msgId()} proxy of $ctxNameCheck is null ")
                }
                return
            }//conosco il nome del contesto
            val ctx = sysUtil.getActorContext(destName)
            if( ctx == null ) { //DESTINATION REMOTE but no context known
                //CommUtils.outyellow("$tt ActorBasic sendMessageToActor | ${msg.msgId()} dest=$destName REMOTE no context known ")
                if( conn != null ){ //we are sending an answer via TCP to an 'alien' (no coap ... ?? )
                    //CommUtils.outmagenta("$tt ActorBasic sendMessageToActor | alien dest=$destName sending answer  ${msg.msgId()} using $conn ")
                    conn.forward( msg )
                    return
                }else{ //attempt to send the reply via mqtt hoping that the destName is mqtt-connected
                    if( attemptToSendViaMqtt(context!!, msg, reqMsg, destName) ) return
                    else {
                        CommUtils.outmagenta("$tt ActorBasic sendMessageToActor |  ${msg.msgId()} WARNING dest=$destName NON REACHABLE ") //for $msg
                        return
                    }
                }
            }//ctx of destination is unknwkn
            //HERE: DESTINATION remote, context of dest known
            if( attemptToSendViaMqtt(ctx, msg, reqMsg, destName) ) return
/* Feb23  INVIO SEMPRE SU TCP per rispettare la sequenza
		if( ! msg.isRequest() && ! msg.isReply() ){
	        val uri = "coap://${ctx.hostAddr}:${ctx.portNum}/${ctx.name}/$destName"
	        //println("$tt ActorBasic sendMessageToActor qak | ${uri} msg=$msg" )
	        //if( attemptToSendViaMqtt(ctx, msg,destName) ) return  //APR2020
	        sendCoapMsg( uri, msg.toString() )
		}
 //DESTINATION remote, context of destName known and NO MQTT  => using proxy
        // REMOVED: Coap 2020 but not for request
        else{	//request
*/
            val proxy = context!!.proxyMap.get(ctx.name)
            //sysUtil.traceprintln("$tt ActorBasic sendMessageToActor |  ${msg.msgId()} $destName REMOTE with PROXY  ")
            //println("$tt ActorBasic | sendMessageToActor |  ${msg.msgId()} $destName via PROXY $proxy")
            //WARNING: destName must be the original and not the proxy
            if( proxy is ActorBasic) { proxy.actor.send( msg ) }
            else println("$tt WARNING FINAL. ActorBasic  sendMessageToActor | msgId=${msg.msgId()} proxy of $ctx is null")
            //}
        }
        //destName is the name of a context APRIL25
        fun discoverAndSend(msg : IApplMessage, ctxRegisteredName: String, reqMsg : IApplMessage?=null){ //OCT24
            //CommUtils.outcyan("$tt ActorBasic | discoverAndSend")
            var discoveredconn : TcpConnection? = sysUtil.ctxsDiscoveredMap.get(ctxRegisteredName)
            if( discoveredconn == null ) {
                CommUtils.outred("$tt ActorBasic | discover  $ctxRegisteredName  ")
                val res = sysUtil.discoverservciectx(ctxRegisteredName)
                if (res != null) {
                    CommUtils.outyellow("$tt ActorBasic | discovered $ctxRegisteredName host=${res[0]} port=${res[1]}")
                  } else {
                    CommUtils.outred("FATAL dicover")
                 }
            /*
             //CommUtils.outyellow("$tt ActorBasic | discovering $ctxRegisteredName")
             val hostPort = CommUtils.discoverService(sysUtil.eurekaClient, ctxRegisteredName) //String[]
             if (hostPort == null) {
                 CommUtils.outred("WARNING: $ctxRegisteredName NOT DISCOVERED")
                 return;
             }
             val host = hostPort[0]
             val port = hostPort[1]
             CommUtils.outyellow("$tt ActorBasic | discovered $ctxRegisteredName host=${host} port=${port}")

             discoveredconn = TcpConnection(host, port.toInt())
             sysUtil.ctxsDiscoveredMap.put(ctxRegisteredName, discoveredconn)

              */
            }
            //INVIO IL MESSAGGIO DOPO AVERE SCOPERTO Il ctx
            discoveredconn  = sysUtil.ctxsDiscoveredMap.get(ctxRegisteredName)
            if( msg.isDispatch() || msg.isEvent() ) discoveredconn!!.forward(msg)
            else if ( msg.isRequest() ){
                try {
                    val answer = discoveredconn!!.request(msg, 15000)  //request NON BLOCKING with tout JUNE25
                    //CommUtils.outcyan("$tt ActorBasic | discoverAndSend answer=$answer ")
                    sendMsgToMyself(answer)
                }catch( e: Exception){
                    CommUtils.outred("$tt ActorBasic | discoverAndSend $ctxRegisteredName ERROR: $e ")
                }
            }

        }


        fun attemptToSendViaMqtt(ctx : QakContext, msg : IApplMessage, reqMsg : IApplMessage?, destName : String) : Boolean{
            //CommUtils.outmagenta("$tt ActorBasic $name attemptToSendViaMqtt | $msg reqMsg=$reqMsg" )
            if( ctx.mqttAddr.length > 0  ) {
                if( ! mqttConnected ){
                    var addr = "" //JUNE2025
                    CommUtils.outcyan("connectToMqttBroker attemptToSendViaMqtt ${ctx.mqttAddr} $addr");
                    if( CommUtils.getEnvvarValue("MQTTBROKER") != null ){
                        addr = "tcp://"+CommUtils.getEnvvarValue("MQTTBROKER")+":1883";
                    }else addr = ctx.mqttAddr
                      mqtt.connect(name, addr)
                    mqttConnected = true
                }
                sysUtil.traceprintln("$tt ActorBasic sendViaMqtt | topic=unibo/qak/$destName : $msg")
                //mqtt.sendMsg(msg, "unibo/qak/$destName")
                mqtt.publish( "unibo/qak/$destName", msg.toString() )  //JAN25 imvio a chiamante qak
                if( reqMsg != null ) {  //invio ANCHE a chiamante alieno ???
                    val topicout = "answ_${reqMsg.msgId()}_${msg.msgReceiver()}"
                    //CommUtils.outmagenta("$tt ActorBasic attemptToSendViaMqtt topicout=$topicout")
                    mqtt.publish(topicout, msg.toString()) //JAN24
                    return true
                }else return false
            }
            CommUtils.outmagenta("$tt ActorBasic attemptToSendViaMqtt FAILED" )
            return false
        }


        suspend fun forward( msgId : String, msg: String, destName: String) {
            val m = MsgUtil.buildDispatch(name, msgId, msg, destName)
            sendMessageToActor( m, destName, null)
        }//forward


        suspend fun request( msgId : String, msg: String, destActor: ActorBasic) {
            //println("       ActorBasic $name | forward $msgId:$msg to ${destActor.name} in ${sysUtil.curThread() }")
            destActor.actor.send(MsgUtil.buildRequest(name, msgId, msg, destActor.name))
        }


        suspend fun request( msgId : String, msg: String, destName: String) {
            val m = MsgUtil.buildRequest(name, msgId, msg, destName)
            sendMessageToActor( m, destName, null)
        }//request


        // 2023-Sept
        fun findStoredRequest(reqId : String, sender : String?=null ) : IApplMessage?{
            //CommUtils.outblue("$tt ActorBasic | $name  findStoredRequest reqId=$reqId sender=$sender mapsize=${requestMap.size} ") ;
            requestMap.forEach {
                //val req = it.value  //è il msg
                val key = it.key
                //CommUtils.outyellow("$tt ActorBasic | $name findStoredRequest $reqId key=$key  sender=$sender")
                if (sender == null && key.contains(reqId)) {  //the program has not stored the request
                    //requestMap.remove( key )
                    val req = getValueFromRequestMap(key)
                    //CommUtils.outblue("$tt ActorBasic | $name  answer NO sender map remove,  mapsize=${requestMap.size}")
                    return req
                }
                if ( key == reqId + sender) { //&& req.msgSender() == sender
                    //requestMap.remove( key )
                    val req = getValueFromRequestMap(key)
                    //CommUtils.outblue("$tt ActorBasic | $name  findStoredRequest answer WITH sender map remove, mapsize=${requestMap.size}")
                    return req
                }
            }//foreach
            return null;
        }

    // 2023-Oct (from replyreq
    fun findRemoveStoredRequest(reqId : String, sender : String?=null ) : IApplMessage?{
        //CommUtils.outblue("$tt ActorBasic | $name  findRemoveStoredRequest reqId=$reqId sender=$sender mapsize=${requestMap.size} ") ;
        requestMap.forEach {
            //val req = it.value  //è il msg
            val key = it.key
            //CommUtils.outyellow("$tt ActorBasic | $name findRemoveStoredRequest $reqId key=$key  sender=$sender")
            if (sender == null && key.contains(reqId)) {  //the program has not stored the request
                //requestMap.remove( key )
                val req = removeValueFromRequestMap(key)
                //CommUtils.outblue("$tt ActorBasic | $name  answer NO sender map remove,  mapsize=${requestMap.size}")
                return req
            }
            if ( key == reqId + sender) { //&& req.msgSender() == sender
                //requestMap.remove( key )
                val req = removeValueFromRequestMap(key)
                //CommUtils.outblue("$tt ActorBasic | $name  findRemoveStoredRequest answer WITH sender map remove, mapsize=${requestMap.size}")
                return req
            }
        }//foreach
        return null;
    }

        fun addValueToRequestMap(//multiMap: MutableMap<String, MutableList<IApplMessage>>,
            key: String, value: IApplMessage) {
            // Verifica se la chiave esiste già nella mappa
            if (!requestMap.containsKey(key)) {
                // Se la chiave non esiste, crea una nuova lista
                requestMap[key] = mutableListOf()
            }
            // Aggiunge il valore alla lista associata alla chiave
            requestMap[key]?.add(value)
            //CommUtils.outyellow("$name | addValueToRequestMap $key, list=$requestMap  mapsize=${requestMap.size}")
        }

        fun removeValueFromRequestMap(//multiMap: MutableMap<String, MutableList<IApplMessage>>,
            key: String ) : IApplMessage{
            //CommUtils.outblue("$name | BEFORE removeValueFromRequestMap $key, mapsize=${requestMap.size}")
            val valuesForKey1 = requestMap[key] //multiMap[key]  //una lista
            //if( valuesForKey1 != null ) CommUtils.outblue("$name | BEFORE removeValueFromRequestMap $key, listSize=${valuesForKey1.size}")
            var selectedRequest : IApplMessage
            if( valuesForKey1 != null && valuesForKey1.size>0){ //Barbieri2023
                //CommUtils.outgray("$tt  $name | BEFORE removeValueFromRequestMap $key, listSize=${valuesForKey1.size} mapsize=${requestMap.size}")
                selectedRequest = valuesForKey1.removeFirst()

                if(valuesForKey1.size==0){ //Baribieri2023 List empty
                    requestMap.remove(key)
                }
                  //Cambia il contenuto della lista referenziata da key
                //CommUtils.outblue("$name | AFTER  key=$key, selectedRequest=$selectedRequest, listSize= ${valuesForKey1.size} mapsize=${requestMap.size}")
                return selectedRequest
            }
            else {
                //return null
                throw Exception("$tt ActorBasicFsm $name | removeValueFromRequestMap $key")
            }
        }

    fun getValueFromRequestMap( //Oct2023 for replyreq
        key: String ) : IApplMessage{
        //CommUtils.outblue("$name | BEFORE getValueFromRequestMap $key, mapsize=${requestMap.size}")
        val valuesForKey1 = requestMap[key] //multiMap[key]  //una lista
        //if( valuesForKey1 != null ) CommUtils.outblue("$name | BEFORE getValueFromRequestMap $key, listSize=${valuesForKey1.size}")
        var selectedRequest : IApplMessage
        if( valuesForKey1 != null && valuesForKey1.size>0){ //Barbieri2023
            //CommUtils.outgray("$tt  $name | BEFORE getValueFromRequestMap $key, listSize=${valuesForKey1.size} mapsize=${requestMap.size}")
            selectedRequest = valuesForKey1.first()

 //           if(valuesForKey1.size==0){ //Baribieri2023 List empty
 //               requestMap.remove(key)
 //           }
            //Cambia il contenuto della lista referenziata da key
            //CommUtils.outblue("$name | AFTER  key=$key, selectedRequest=$selectedRequest, listSize= ${valuesForKey1.size} mapsize=${requestMap.size}")
            return selectedRequest
        }
        else {
            //return null
            throw Exception("$tt ActorBasicFsm $name | getValueFromRequestMap $key")
        }
    }
        suspend fun answer( reqId: String, msgId : String, msg: String,  sender: String?=null, caller : String?=null ) {
            //sysUtil.traceprintln("$tt ActorBasic $name | answer $msgId:$msg  ")
            //CommUtils.outblue("$tt ActorBasic | $name  answer $msgId:$msg  to $sender mapsize=${requestMap.size}")
            //if( sender != null ) {
            val reqMsg = findRemoveStoredRequest(reqId, sender)
            if (reqMsg == null) {
                CommUtils.outred("$tt ActorBasic $name | WARNING_1: answer for $reqId no request found ")
                return
            }
            /*
            }//sender null or reqMsg != null
            val reqMsg = findStoredRequest(reqId, sender)
            //val reqMsg = requestMap.remove( reqId ) //one request, one reply
            //CommUtils.outblue("$tt  $name | answer done remove $msg map,  mapsize=${requestMap.size}")
            if( reqMsg == null ){
                    CommUtils.outred("$tt ActorBasic $name | WARNING_2: answer $msgId no request found ")
                    return
                }*/
            else {
                if( caller != null && ! caller.equals(reqMsg.msgSender())){
                    CommUtils.outred("$tt ActorBasic $name | WARNING_3: answer for $reqId WRONG caller ")
                    //java.awt.Toolkit.getDefaultToolkit().beep()
                    return
                }
                //Caller = null
                val destName = reqMsg.msgSender()
                val m = MsgUtil.buildReply(name, msgId, msg, destName)
                //CommUtils.outmagenta("$tt ActorBasic $name | answer destName=$destName msg=$m  ${(reqMsg as IApplMessage).conn } }")
                sendMessageToActor( m, destName, reqMsg, (reqMsg as IApplMessage).conn )
            }

            /*
            val reqMsg = requestMap.remove(reqId) //one request, one reply
            if( reqMsg == null ){
                CommUtils.outred("$tt ActorBasic $name | WARNING: answer $msgId no request found ")
                return
            }
            if( caller != null && ! caller.equals(reqMsg.msgSender())){
                // 2023-Sept devo cercare in requestMap la richiesta con quel callerId e
                // fare beep se non esiste
                CommUtils.outred("$tt ActorBasic $name | WARNING: answer $msgId WRONG caller ")
                java.awt.Toolkit.getDefaultToolkit().beep();
                //(Windows) The sound used is determined from the setting found in
                //Control Panel/Sounds and Devices/Sounds/Sound Scheme/"Default Beep".
                 return
            }
            val destName = reqMsg.msgSender()
            //println("$tt ActorBasic $name | answer destName=$destName  }")
            val m = MsgUtil.buildReply(name, msgId, msg, destName)
            sendMessageToActor( m, destName, (reqMsg as ApplMessage).conn )
             */
        }//answer

    suspend fun replyreq( reqId: String, reqestmsgId : String, msg: String) {  //OBSOLETE?
        CommUtils.outmagenta(" $tt ActorBasic $name | replyreq $reqId related to:$reqestmsgId content=$msg  ")
        val reqMsg = findStoredRequest(reqestmsgId)  //rimuove
        if( reqMsg == null ){
            println("$tt ActorBasic $name | WARNING: replyreq to $reqestmsgId INCONSISTENT: no request found ")
            return
        }
        if( reqMsg !== null ) {
            val destName = reqMsg.msgSender()
            val m = MsgUtil.buildReplyReq(name, reqId, msg, destName)
            sendMessageToActor(m, destName, reqMsg, reqMsg.conn)
        }
    }




        suspend fun emit(ctx: QakContext, event : IApplMessage) {  //used by NodeProxy
            sysUtil.traceprintln("$tt ActorBasic $name | emit from proxy ctx= ${ctx.name} ")
            ctx.actorMap.forEach {
                val ctxName  = it.key
                if(  ctx.name != ctxName ){
                    val destActor = it.value
                    sysUtil.traceprintln("$tt ActorBasic $name | PROPAGATE ${event.msgId()} locally to ${destActor.name} ")
                    destActor.actor.send(event)
                }
            }
        }

//ADDED MAY2020	


        suspend fun emitWithDelay(  evId: String, evContent: String, dt : Long = 0L ) {
            //argScope
            GlobalScope.launch{
                delay( dt )
                val event = MsgUtil.buildEvent(name, evId, evContent)
                CommUtils.outmagenta("            emitted $event after $dt")
                emit( event, avatar=true )
            }
        }



        suspend fun emit(event : IApplMessage, avatar : Boolean = false ) {
            //avatar=true means that the emitter is able to sense the event that emits
            if( context == null ){
                println("$tt ActorBasic $name | WARNING emit: actor has no QakContext. ")
                this.actor.send(event)  //AUTOMSG
                return
            }
            //PROPAGATE TO LOCAL ACTORS
            if( context!!.mqttAddr.length == 0  //There is NO MQTT for this context
                ||  //the event is local
                event.msgId().startsWith("local")  ) {
                context!!.actorMap.forEach {
                    val destActor = it.value
                    //do not propagate the event to the emitter!!!!
                    if( destActor.name != this.name ||  avatar) {
                        //sysUtil.traceprintln(" $tt ActorBasic $name | PROPAGATE ${event.msgId()} locally to ${destActor.name} " )
                        destActor.actor.send(event)
                    }
                }
            }
            //PROPAGATE TO REMOTE ACTORS
            if( event.msgId().startsWith("local")) return       //local_ => no propagation
            //EMIT VIA MQTT IF there is
            if( context!!.mqttAddr.length != 0 ) {
                //CommUtils.outyellow("$tt ActorBasic $name | emittttttttttttttttt MQTT ${event.msgId()}  on ${sysUtil.getMqttEventTopic()}")
                //mqtt.sendMsg(event, sysUtil.getMqttEventTopic())
                mqtt.publish(sysUtil.getMqttEventTopic(), event.toString() )  //Are perceived also by the emitter
            }
            //sysUtil.traceprintln(" $tt ActorBasic $name | ctxsMap SIZE = ${sysUtil.ctxsMap.size}")

            //APR2020: we do not look anymore at the ctxsMap since we have introduced sysUtil.connActive for TCP connections

            //Propago ai contesti noti e discovered
            sysUtil.ctxsMap.forEach{
                val ctxName  = it.key

                //sysUtil.traceprintln("$tt ActorBasic $name | ${context!!.name} try to propagate event ${event.msgId()} to ${ctxName}  ")
                //CommUtils.outred("$tt ActorBasic $name | ${it.value.name} try to propagate event ${event.msgId()}  ")
                val proxy  = context!!.proxyMap.get(ctxName)
                //CommUtils.outred("$tt ActorBasic $name | emit ${event.msgId()} has found proxy $proxy for ${it.value.name}")
                if( proxy == null ){
                    //deve andare a tutti gli altri attori nel nodo corrente
                    //CommUtils.outred("$tt ActorBasic $name emit $event in ${context!!.name}"  );
                    //e a tutti i contesti discoverale
                    sysUtil.ctxsDiscoveredMap.forEach{
                        val ctxDiscoveredName  = it.key
                        discoverAndSend( event, ctxDiscoveredName, null)
                    }
                }
                if( proxy is ActorBasic && proxy.name != this.context!!.name ){ //Tocheck NOV22
                    //CommUtils.outyellow("$tt ActorBasic $name | emit ${event}  towards $ctxName via proxy")
                    proxy.actor.send( event )    //Propagate via proxy THAT MUST exist if we know the context
                }

                //sysUtil.traceprintln(" $tt ActorBasic $name | emit ${event.msgId()}  ENDS")
            }

            // DEC2019 propagate on TCP connections
            //APR2020 : no more events to aliens on TCP - only via MQTT
//        sysUtil.connActive.forEach {
//            println(" $tt ActorBasic $name | emit ${event.msgId()} on active conn: $it")
//            it.sendALine(event.toString() )
//        }
        }



        suspend fun emit( msgId : String, msg : String) {
            val event = MsgUtil.buildEvent(name, msgId, msg)
            emit( event )
        }
    suspend fun emitlocal(msgId : String, msg : String){
        val event = MsgUtil.buildEvent(name, msgId, msg)
        emitLocalEvent(event)
    }
        //Dec2023
        suspend fun emitstreammqtt(topic: String, msgId : String, msg : String){
            val event    = MsgUtil.buildEvent(name, msgId, msg)
            /*
            val ctxnames = sysUtil.getContextNames()

            ctxnames.forEach{
                if( it != context!!.name){
                    CommUtils.outgray("$ctxnames" )
                    mqtt.publish(it,event.toString() )
                }
                emitLocalStreamEvent(msgId,msg)
                         }
            */
            val mqttMessage = MqttMessage(event.toString().toByteArray())
            //CommUtils.outblack("ActorBasic | $name emitstreamalsomqtt $event on $topic" )
            mqtt.publish(topic, mqttMessage.toString() )
        }

        /*
         --------------------------------------------
         OBSERVABLE
         --------------------------------------------
        */
        fun subscribe( a : ActorBasic) : ActorBasic {
            subscribers.add(a)

            sysUtil.traceprintln(" $tt ActorBasic $name | subscribe ${a.name} ")
            return a
        }
        fun subscribeToLocalActor( actorName : String) : ActorBasic {
            val a = sysUtil.getActor(actorName)
            if( a != null  ) {
                if ( ! a.subscribers.contains(this) ) {
                    a.subscribers.add(this);
                    //CommUtils.outyellow("$tt ActorBasic $name subscribeTo:$actorName (nsubscribed=${a.subscribers.size})");
                }
                return a
            }else{ println("WARNING: subscribeToLocalActor  $actorName not found" );
                throw Exception("actor $actorName not found in the current context ")
            }
        }



        fun subscribeTo ( actorName : String, sourceActorName: String ){
            val a      = sysUtil.getActor(actorName)
            val source = sysUtil.getActor(sourceActorName)
            if( a != null && source != null ){
                source.subscribers.add(a);
                //sysUtil.traceprintln("  $tt ActorBasic  $actorName subscribeTo: $sourceActorName ( nsubscribed=${a.subscribers.size} )" );
            } else{ println("WARNING: subscribeTo - actor not found" );
                throw Exception("subscribeTo | actor not found in the current context ")
            }
        }

        fun unsubscribe( a : ActorBasic) {
            subscribers.remove(a)
        }


        suspend fun emitLocalStreamEvent(ev: String, evc: String ){
            emitLocalStreamEvent(MsgUtil.buildEvent(name, ev, evc))
        }


            suspend fun emitLocalStreamEvent(v: IApplMessage){
            //println("emitLocalStreamEvent .... ${name} subs=${subscribers.size}")
            subscribers.forEach {
                sysUtil.traceprintln(" $tt ActorBasic $name | emitLocalStreamEvent $it $v ");
                it.actor.send(v)
            }
        }


        suspend fun emitLocalEvent(event: IApplMessage){
            //sysUtil.traceprintln("$tt ActorBasic $name | emit emitLocalEvent ctx= ${context!!.name} ")
            context!!.actorMap.forEach {
                val destActor = it.value
                //do not propagate the event to the emitter!!!!
                if( destActor.name != this.name  ) {
                    //CommUtils.outyellow(" $tt ActorBasic $name | PROPAGATE ${event.msgId()} locally to ${destActor.name} " )
                    if( ! destActor.actor.isClosedForSend ) destActor.actor.send(event)
                    //delay( 10 )
                }
            }
        }


        /*
        --------------------------------------------
        DELEGATION
        --------------------------------------------
        */
        fun delegate(msgid: String, actorName : String){
            val actor = context!!.hasActor(actorName)
            if( actor != null ){
                delegated.put(msgid, actor)
                CommUtils.outyellow(" $tt ActorBasic $name | Delegated $msgid to $actorName ")
            }
            else CommUtils.outred(" $tt ActorBasic $name | WARNING: delegation to non local actor $actorName not allowed ")
        }

        fun delegate(msgid: String, actor : ActorBasic){
            val found = context!!.hasActor(actor.name)
            if( found != null ) delegated.put(msgid, actor)
            else CommUtils.outred(" $tt ActorBasic $name | WARNING: delegation to non local actor ${actor.name} not allowed ")
        }
        /*
        --------------------------------------------
        MQTT
        --------------------------------------------
         */



        fun checkMqtt() : Boolean{
            //CommUtils.outyellow("$tt ActorBasic $name | checkMqtt mqttConnected=$mqttConnected contxt=$context")
            if( mqttConnected ) return true;

            if( context == null ){
                //wait ...
                CommUtils.outgreen( "$tt ActorBasic $name | waiting for context ...")
                CommUtils.delay( 500 )   //FORSE IMPEDISCE ....
            }
            if( ! mqttConnected && context != null && context!!.mqttAddr.length > 0  ){  //Viene injected in QAkContext addActor

                var addr = "" //JUNE2025
                //CommUtils.outcyan("connectToMqttBroker checkMqtt ${context!!.mqttAddr} $addr");
                if( CommUtils.getEnvvarValue("MQTTBROKER") != null ){
                    addr = "tcp://"+CommUtils.getEnvvarValue("MQTTBROKER")+":1883";
                }else addr = context!!.mqttAddr
                 mqttConnected = mqtt.connect(name,addr)
                //mqttConnected = true
                mqtt.subscribe(this, "unibo/qak/$name")      //Anche a questa?
                mqtt.subscribe( this, sysUtil.getMqttEventTopic())
                CommUtils.outyellow( "$tt ActorBasic $name | subscribed to unibo/qak/$name and to ${sysUtil.getMqttEventTopic()}")
            }

            //CommUtils.outgreen( "$tt ActorBasic $name | autoStartSysMsg ............ ")
            val autoStartMsg = MsgUtil.buildDispatch(name, "autoStartSysMsg", "start", name)
            sendMsgToMyself(autoStartMsg)

            return mqttConnected
        }


        fun removeFromMqtt(){
            if( context!!.mqttAddr.length > 0  ){
                mqtt.disconnect()
                mqttConnected = false
            }
        }



    override fun messageArrived(topic: String, msg: MqttMessage) {
        //Il messaggio non dovrebbe arrivare  agli attori terminati
        //CommUtils.outyellow("$tt ActorBasic $name|AS MqttCallback => messageArrived $msg on $topic" );
        if( isTerminated ) return   //defensive
        try {
            val m = ApplMessage(msg.toString())
            //sysUtil.traceprintln("$tt ActorBasic $name |  MQTT ARRIVED on $topic  m=$m ${actor}")
             //INVIO un non-event AL DESTINATARIO JAN24
            if( m.isEvent() ){
                //CommUtils.outyellow("ActorBasic $name |  MQTT ARRIVED event from $topic  m=$m ${MyName}")
                GlobalScope.launch{ actor.send( m ) }
            }
            if( m.isDispatch()
                || (m.isRequest() && m.msgReceiver() == name)
                || (m.isReply()   && m.msgReceiver() == name)
            ){
                CommUtils.outyellow("ActorBasic $name |  MQTT ARRIVED request/dispatch from $topic  m=$m ${MyName}")
                GlobalScope.launch{ actor.send( m ) }
            }
        }catch(e: Exception){
            //CommUtils.outred("$tt ActorBasic $name|AS MqttCallback => messageArrived $msg STRING" );
            val m: IApplMessage = MsgUtil.buildEvent("alien", "kernel_rawmsg", "kernel_rawmsg('$msg')" )
            //Evento mirato all'actor che riceve, al fine di elaborare una stringa
            GlobalScope.launch{ actor.send( m ) }
        }
    }
         override fun connectionLost(cause: Throwable?) {
            println("$tt ActorBasic $name |  MQTT connectionLost $cause " )
        }
        override fun deliveryComplete(token: IMqttDeliveryToken?) {
//		println("       ActorBasic $name |  deliveryComplete token= "+ token );
        }
        /*
        For direct usage without qak
         */
        fun connectToMqttBroker( mqttAddr : String){
            var addr = ""  //JUNE2025
            if( CommUtils.getEnvvarValue("MQTTBROKER") != null ){
                addr = "tcp://"+CommUtils.getEnvvarValue("MQTTBROKER")+":1883";
            }else addr = mqttAddr
            CommUtils.outcyan("connectToMqttBroker1 $mqttAddr $addr");
            mqtt.connect(name, addr )
        }
        fun connectToMqttBroker( mqttAddr : String, clientid:String ) {
            var addr = "" //JUNE2025
            if( CommUtils.getEnvvarValue("MQTTBROKER") != null ){
                addr = "tcp://"+CommUtils.getEnvvarValue("MQTTBROKER")+":1883";
            }else addr = mqttAddr
            CommUtils.outcyan("connectToMqttBroker2 $mqttAddr $addr");
            mqtt.connect(clientid, addr)
        }
        fun publish( msg: String, topic: String, qos: Int = 2, retain: Boolean = false ){
            //CommUtils.outyellow("ActorBasic | $name publish $mqtt ${mqtt.mqttConn} qos=$qos retain=$retain")
            mqtt.publish( topic, msg, qos, retain);
        }
    //Dec2023
        suspend fun cleartopic(topic: String){
            //mqtt.cleartopic(topic)
            emitstreammqtt(topic,"sys", ByteArray(0).toString())
        }



        /*
        --------------------------------------------
        machineExec
        --------------------------------------------
         */
        fun machineExec(cmd: String) : Process {
            try {
                //println("       ActorBasic $name | machineExec  $cmd ")
                return sysUtil.runtimeEnvironment.exec(cmd)
            } catch (e: Exception) {
                println("       ActorBasic $name | machineExec ERROR $e ")
                throw e
            }
        }
        fun getCurrentTime():Long {
            return System.currentTimeMillis()
        }

        fun getDuration(start: Long) : Long{
            val duration = (System.currentTimeMillis() - start)
            //println("DURATION = $duration start=$start")
            return duration
        }

        fun createActorDynamically(actorName:String,  instanceSuffix:String,
                                   isconfined:Boolean ):String{
/*
           //CommUtils.outred("createActorDynamically   actorName=${actorName}")
            //Create a new instance of the actor. The name is different for each actor and is never referred in the program.
            val actorClass   = sysUtil.solve("qactor($actorName, ${context!!.name},CLASS)","CLASS")
            // CommUtils.outred("createActorDynamically   actorClass= ${actorClass}")
            val className    = actorClass!!.replace("'","")
            executorNameCounter++
            // CommUtils.outred("CreateQActorAsExecutor $className   ${executorNameCounter}")
            if(instanceSuffix.contains("_")) executorName  = actorName+instanceSuffix
            else executorName     = actorName+executorNameCounter
            sysUtil.createActorFsm(context!!, executorName, className, argscope, isconfined )
            return executorName
  */
            val createdActor = createActorFsmDynamic(actorName,instanceSuffix,isconfined)
            return createdActor.name
        }
    fun createActorFsmDynamic(actorName:String,  instanceSuffix:String, isconfined:Boolean ):ActorBasic{
        //CommUtils.outred("createActorDynamically   actorName=${actorName} ") //in  ${context!!.name}
        //Create a new instance of the actor. The name is different for each actor and is never referred in the program.
        val actorClass   = sysUtil.solve("qactor($actorName, ${context!!.name},CLASS)","CLASS")
        // CommUtils.outred("createActorDynamically   actorClass= ${actorClass}")
        val className    = actorClass!!.replace("'","")
        executorNameCounter++
        // CommUtils.outred("CreateQActorAsExecutor $className   ${executorNameCounter}")
        if(instanceSuffix.contains("_")) executorName  = actorName+instanceSuffix
        else executorName = actorName+executorNameCounter
        val createdActor  = sysUtil.createActorFsm(context!!, executorName, className, argscope, isconfined, true )
        return createdActor as ActorBasic
    }

/*
KNOWLEDGE BASE
*/

        fun registerActor( ) {
            //	println("QActorUtils Regsitering in TuProlog ... " + this.getName()  );
            val lib = pengine.getLibrary("alice.tuprolog.lib.OOLibrary")
            //	println("QActorUtils Registering in TuProlog18 ... " + lib );
            val internalName = Struct("" + this.name)
            (lib as alice.tuprolog.lib.OOLibrary).register(internalName, this)
            //	println("QActorUtils Registered in TuProlog18 " + internalName );
        }

        fun solve( goal: String, rVar: String ="" ) {
            //println("       ActorBasic $name | solveGoal ${goal} rVar=$rVar" );
            val sol = pengine.solve( "$goal.")
            currentSolution = sol
            if(  sol.isSuccess  ) {
                if( (rVar != "") ) {
                    val resStr = sol.getVarValue(rVar).toString()
                    resVar = sysUtil.strCleaned(resStr)

                }else resVar = "success"
            } else resVar = "fail"
        }

    fun solvegoal( goal: String, rVar: String ) : String{ //DEC23
        CommUtils.outgreen("       ActorBasic $name | solveGoal ${goal} rVar=$rVar" );
        val sol = pengine.solve( "$goal.")
        if(  sol.isSuccess  ) {
            val resStr = sol.getVarValue(rVar).toString()
            resVar = sysUtil.strCleaned(resStr)
            return  resVar
        } else return "fail"
    }


    fun solveOk() : Boolean{
            return resVar != "fail"
    }

        fun getCurSol(v : String) : Term {
            if(currentSolution.isSuccess )
                return currentSolution.getVarValue( v )
            else return Term.createTerm("no(more)solution")
        }

/*
=======================================================================
     About CoAP: Jan 2020
=======================================================================
*/
/*

open fun observeResource( actor : String, msgId: String ){
    //Se actor è locale, mi registro
    val observable = QakContext.getActor(actor)
    if( observable != null ){
        CommUtils.outmagenta("observeResource adds " + this.name + " to " + observable.name)
        observable.observers.add(this)
        sysUtil.observersMsgids.put(this.name,msgId)
    }else{
        //Se no, apro un CoapObserverSupport
        val port = (context!!.portNum).toString()
        val ctxname = (context!!.name)
        CommUtils.outmagenta("ActorBasic Lancio CoapObserverSupport " + port + " " + actor + " " + msgId)
        //CoapObserverSupport(myself, "localhost",port,ctxname,actor,msgId)
    }
}
*/
/*
--------------------------------------------
OBSERVER March/2024
--------------------------------------------
*/
    fun observeResource( host : String, port: String, ctxName : String, actorName: String, msgId : String ){
        if(  context!!.name == ctxName ){ //osservo un attore locale
            val observable = QakContext.getActor(actorName)
            if( observable!!.observers.contains(this) ){
                CommUtils.outred(this.name + " already observing: " + observable!!.name)
                return
            }
            sysUtil.traceprintln("$tt ActorBasic observeResource adds " + this.name + " to " + observable!!.name)
            observable.observers.add(this)
            sysUtil.observersMsgids.put(this.name,msgId)
        }else{ //osservo un attore non locale
            CommUtils.outcyan("$tt ActorBasic $name osservo attore remoto $actorName - Lancio CoapObserverSupport su " + port + " msgId=" + msgId)
            CoapObserverSupport(this, host,port,ctxName,actorName,msgId)
        }
    }
        fun  updateResourceRep( msg : String){
            var mout : IApplMessage
            observers.forEach( //March 2024
            Consumer { obs: ActorBasic ->
                //CommUtils.outblack("$name updateResourceRep obs=${obs.name}")
                val infoMsg = sysUtil.observersMsgids.get(obs.name)
                //CommUtils.outblack("$name updateResourceRep infoMsg=${infoMsg}")
                val cmsg    = "changed( $msg )"  //MAY2025
                mout = CommUtils.buildDispatch(name, infoMsg, cmsg, obs.name)
                //sysUtil.traceprintln("$name updateResourceRep $m")
                //CommUtils.outblue("$$tt ActorBasic  $name updateResourceRep $mout")
                GlobalScope.launch{ obs.actor.send( mout ) }
            }
        )

            //preparo string (evento) visibile con Coap
        //val updateEvent = CommUtils.buildEvent(name, "changed", "changed("+msg+")" )
        ActorResourceRep = msg  //updateEvent .toString() //
        //CommUtils.outcyan("$tt ActorBasic  $name updateResourceRep  $msg "  )
        changed()             //DO NOT FORGET!!!
    }
    /*
    fun updateResourceRep( v : String, msgid: String){  //DEC23
        ActorResourceRep = v
        //CommUtils.outred("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  updateResourceRep $name $v"  )
        changed()             //DO NOT FORGET!!!
    }
*/
    fun geResourceRep() : String{
            return ActorResourceRep
    }

        override fun handleGET(exchange: CoapExchange) {
            //sysUtil.traceprintln("$logo | handleGET from: ${exchange.sourceAddress} arg: ${exchange.requestText}")
            //CommUtils.outgreen("$logo | handleGET from: ${exchange.sourceAddress} arg: ${exchange.requestText} answer:$ActorResourceRep")
            if( ! ActorResourceRep.contains("createdtobediscared"))  //FEB2023
                exchange.respond( "$ActorResourceRep")
            else exchange.respond( "nonews")
        }
        /*
         * POST is NOT idempotent.	Use POST when you want to add a child resource
         */
        override fun handlePOST(exchange: CoapExchange) {
            sysUtil.traceprintln("$logo | handlePOST from: ${exchange.sourceAddress} arg: ${exchange.requestText}")
            exchange.respond( "POST not implemented")
        }

/*
 * PUT method is idempotent. Use PUT when you want to modify
 */


        override fun handlePUT(exchange: CoapExchange) {
            var arg = exchange.requestText
            //CommUtils.outmagenta("$name | handlePUT | $arg  exchange=$exchange")
            val argdecoded = java.net.URLDecoder.decode(arg, StandardCharsets.UTF_8.toString());
            //CommUtils.outmagenta("$name | handlePUT decodde=| $argdecoded  ")
            //sysUtil.traceprintln("$logo | handlePUT arg=$arg")
            //println("$logo | handlePUT arg=$arg")
            try{
                val msg = ApplMessage( argdecoded )
                fromPutToMsg( arg, msg, exchange )
            }catch( e : Exception){
                CommUtils.outred("$logo | handlePUT ERROR on msg $e")
            }
        }

        override fun handleDELETE(exchange: CoapExchange) {
            delete()
            exchange.respond(DELETED)
        }



        fun fromPutToMsg(originallmsg: String, msg : IApplMessage, exchange: CoapExchange ) {
            //sysUtil.traceprintln("$logo | fromPutToMsggg  $msg ${msg.isEvent()}")
            //println("$logo | fromPutToMsggg msg=$msg")
            if( msg.isDispatch() || msg.isEvent() ) {
                //argscope.launch { autoMsg(msg) } //
                GlobalScope.launch { autoMsg(msg)  }   //JAN24
                exchange.respond( CHANGED )
                return
            }
            if( msg.isRequest() ) {
                //sysUtil.traceprintln("$logo | REQUEST in CoAP| create a temporary actor to manage $msg ")
                //CommUtils.outgreen("$logo | REQUEST in CoAP| create a temporary actor to manage $msg ")
                CoapToActor("caoproute${count++}", exchange, this, msg, originallmsg)
            }
        }

        fun sendCoapRequest(host : String , path: String, msg : String ) : String{
            //url=coap://127.0.0.1:8020/ctxbasicrobot/basicrobot
            val conn = CoapConnection.create(host,path)
            val answer = conn.request(msg)
            conn.close()
            return answer
        }

        fun sendCoapMsg(  url : String, msg : String   ){
            //url=coap://127.0.0.1:8020/ctxbasicrobot/basicrobot
            sysUtil.traceprintln("$logo |   sendCoapMsg $msg url=${url}")
            val client = CoapClient(url)
            val resp   = client.put(msg, MediaTypeRegistry.TEXT_PLAIN) //: CoapResponse
            sysUtil.traceprintln("$logo |   sendCoapMsg resp =${resp.getCode()}")
        }

    //ADDED JAn25
    //@JvmStatic (only in companion)
    open public fun fromRawDataToApplMessage(message: String) {
        CommUtils.outred("$logo | SORRY, fromRawDataToApplMessage has to be witten by the application")
    }

    }



