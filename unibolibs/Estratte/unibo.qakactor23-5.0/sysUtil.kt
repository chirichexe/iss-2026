package it.unibo.kactor

import alice.tuprolog.Prolog
import alice.tuprolog.Struct
import alice.tuprolog.Term
import alice.tuprolog.Theory
import com.netflix.discovery.DiscoveryClient
import junit.framework.Assert.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import org.json.simple.JSONObject
import unibo.basicomm23.interfaces.Interaction
import unibo.basicomm23.msg.ProtocolType
import unibo.basicomm23.utils.CommUtils
import unibo.basicomm23.utils.ConnectionFactory
import unibo.basicomm23.tcp.TcpConnection
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*


/*
ECLIPSE KOTLIN
https://dl.bintray.com/jetbrains/kotlin/eclipse-plugin/last/
*/

//A module in kotlin is a set of Kotlin files compiled together
object sysUtil{
	private val pengine = Prolog()
    internal val dispatchMap :  MutableMap<String,Struct>      = mutableMapOf<String,Struct>()
	internal val ctxsMap :      MutableMap<String, QakContext> = mutableMapOf<String, QakContext>()
	internal val ctxsDiscoveredMap :      MutableMap<String, TcpConnection> = mutableMapOf<String, TcpConnection>()
	internal val ctxActorMap :  MutableMap<String, ActorBasic> = mutableMapOf<String, ActorBasic>()
	val ctxOnHost =  mutableListOf<QakContext>()

	val observersMsgids: MutableMap<String, String> = mutableMapOf<String, String>()

	val runtimeEnvironment     = Runtime.getRuntime()
	val userDirectory          = System.getProperty("user.dir")
	val cpus                   = Runtime.getRuntime().availableProcessors()

	//By default, the maximum number of threads used by Default dispatcher is equal to the number of CPU cores, but is at least two.
	val singleThreadContext    = newSingleThreadContext("qaksingle")
	val ioBoundThreadContext   = newFixedThreadPoolContext(64, "qakiopool")
	val cpusThreadContext      = newFixedThreadPoolContext(cpus, "qakcpuspool")

	val confinedscope   = CoroutineScope( singleThreadContext )
	val cpuRelatedscope = CoroutineScope( cpusThreadContext )

	var mqttBrokerIP : String? = ""
	var mqttBrokerPort : String? = ""
	var mqttBrokerEventTopic : String? = ""
	var trace   : Boolean = false
	var logMsgs : Boolean = false
	val connActive : MutableSet<Interaction> = mutableSetOf<Interaction>()    //Oct2019

	lateinit var eurekaClient : DiscoveryClient;

@JvmStatic    	fun getMqttEventTopic() : String {
		if(mqttBrokerEventTopic !== null ) return mqttBrokerEventTopic!!
		else return "unibo/qak/events"  //NON SI APPLICA MAI
	}

@JvmStatic    	fun getPrologEngine() : Prolog = pengine
@JvmStatic    	fun curThread() : String = "thread=${Thread.currentThread().name}"

@JvmStatic    	fun getContext( ctxName : String ) : QakContext?  {
	return ctxsMap.get(ctxName.lowercase(Locale.getDefault()))}
@JvmStatic    	fun getActor( actorName : String ) : ActorBasic? {
	//CommUtils.outred("sysUtil.getActor $actorName")
	return ctxActorMap.get(actorName.lowercase(Locale.getDefault()))
}

@JvmStatic 	fun addContextInMap( ctx : QakContext ){  //Nove23
		ctxsMap.put(ctx.name, ctx )
	}

	//Dec 2023
	@JvmStatic 	fun getContextNames(): MutableSet<String>{
		return ctxsMap.keys;
	}


	@JvmStatic fun removeqactorfact( actorName : String){
		//CommUtils.outgreen("sysUtil  |  removeqactorfact $actorName")
		pengine.solve("retract( qactor($actorName,_,_) ).")
		//solve( "retract( qactor($actorName,_,_) )", ""  )
	}

	@JvmStatic fun getActorContextName( actorName : String): String?{
		val ctxName = solve( "qactor($actorName,CTX,_)", "CTX" )
		return ctxName
	}
@JvmStatic 	fun setActorContextName( actorName : String, ctxName : String ) {
		//if does not exists ...
		val res = solve( "qactor($actorName,$ctxName,_)", "" )
		if( res == "success") println("$actorName already set in $ctxName")
		else solve( "assertz( qactor($actorName,$ctxName,_) )", "" )
	}
	@JvmStatic    	fun getActorContext ( actorName : String): QakContext?{
		val ctxName = solve( "qactor($actorName,CTX,_)", "CTX" )
		//Chiamata in ActorBasic 225 (sendMessageToActor) per destinatario non locale
		//CommUtils.outyellow("               %%% sysUtil |  getActorContext ctxName=${ctxName} - ${ctxsMap.get( ctxName )}")
		return ctxsMap.get( ctxName )
	}


	@JvmStatic 	fun createContexts(  hostName : String,
					desrFilePath:String, rulesFilePath:String, localContextName: String? = null){
		//CommUtils.outmagenta("sysUtil createContexts  localContextName=$localContextName host=$hostName")

		loadTheory( desrFilePath )
		loadTheory( rulesFilePath )
		if( solve("tracing", "" ).equals("success") ) trace=true
		if( solve("msglogging", "" ).equals("success") ) logMsgs=true
		try {
			mqttBrokerIP         = solve("mqttBroker(IP,_,_)", "IP" )
			mqttBrokerPort       = solve("mqttBroker(_,PORT,_)", "PORT")
			mqttBrokerEventTopic = solve("mqttBroker(_,_,EVTOPIC)", "EVTOPIC")
		}catch(e: Exception){
			println("               %%% sysUtil | NO MQTT borker FOUND")
		}
        //Create the messages ?????
        try {
            val dispatcNames      = solve("getDispatchIds(D)", "D")
            val dispatchNamesList = strRepToList(dispatcNames!!)
            dispatchNamesList.forEach { d -> createDispatch(d) }
        }catch( e : Exception){ //println("               %%% sysUtil | NO DISPATCH FOUND")
		}

		//Create the contexts
		//println("               %%% sysUtil | getCtxNames( X )" )
			val ctxs: String? = solve("getCtxNames( X )", "X")
			//context( CTX, HOST, PROTOCOL, PORT )
			val ctxsList = strRepToList(ctxs!!)
			//waits for all the other context before activating the actors
		//sysUtil.aboutThreads("sysUtil createContexts  ctxsList=$ctxsList  ")
		if(localContextName==null) { //before Giannatempo
			CommUtils.outmagenta("               %%% sysUtil +++ createContexts localContextName is null");
			ctxsList.forEach { ctx -> createTheContext(ctx, hostName = hostName) }//foreach ctx
			addProxyToOtherCtxs(ctxsList, hostName = hostName)  //here could wait in polling ...
		}else {
			if( ctxsList.size == 0 ){
				CommUtils.outred("               %%% sysUtil +++ createContexts NO CONTEXT FOUND");
				return;
			}
			CommUtils.outmagenta("               %%% sysUtil +++ createContexts $localContextName")
			ctxsList.forEach { ctx ->  //NOV22
				val ctxhostName = solve("context( CTX, HOST, PROTOCOL, PORT )".replace("CTX",ctx), "HOST")
				traceprintln("               %%% sysUtil +++ ctx=$ctx ctxhostName=$ctxhostName")
				if( ctxhostName=="discoverable" ) {

					if( ! this::eurekaClient.isInitialized ) eurekaClient = CommUtils.createEurekaClient();
					//else CommUtils.outred(" +++++++++++++++++++++ sysUtil eurekaClient  already set")

				}else
				createTheContext(ctx, hostName = ctxhostName!!, localContextName)
			}//foreach ctx
			addProxyToOtherCtxs(ctxsList, hostName = hostName, localContextName)  //here could wait in polling ...
		}
		//APR2020: removed, since we use CoAP e no more TCP
 			//addProxyToOtherCtxs(ctxsList, hostName = hostName)  //xxx here could wait in polling ...
	}//createContexts

    fun createDispatch(  d : String )  {
         val dd  = solve("dispatch($d,C)", "C")
         val dt = ( Term.createTerm("dispatch($d,$dd)") )
         //println("sysUtil | dispatch $dt  ")
         dispatchMap.put( d, dt as Struct )
     }


	@JvmStatic fun createTheContext(  ctx : String, hostName : String  ) : QakContext?{
		CommUtils.outgreen("               %%% sysUtil +++  createTheContext  ctx=$ctx host=$hostName")
		val ctxHost : String?  = solve("getCtxHost($ctx,H)","H")
		//println("               %%% sysUtil | createTheContext $ctx ctxHost=$ctxHost  ")
		//val ctxProtocol : String? = solve("getCtxProtocol($ctx,P)","P")
		val ctxPort     : String? = solve("getCtxPort($ctx,P)","P")
		//println("               %%% sysUtil | $ctx host=$ctxHost port = $ctxPort protocol=$ctxProtocol")
		val portNum = Integer.parseInt(ctxPort)

//		val useMqtt = ctxProtocol!!.toLowerCase() == "mqtt"	//CoAP 2020
		var mqttAddr = ""
//		if( useMqtt ){	//CoAP 2020
			if( mqttBrokerIP != null ){
				mqttAddr = "tcp://$mqttBrokerIP:$mqttBrokerPort"
				println("               %%% sysUtil | context $ctx WORKS ALSO WITH MQTT mqttAddr=$mqttAddr")
			}
			//else{ throw Exception("no MQTT broker declared")  }	//CoAP 2020
//		}
		//CREATE AND MEMO THE CONTEXT
		var newctx : QakContext?
		if( ! ctxHost.equals(hostName) ){
              println("               %%% sysUtil | createTheContext $ctx for DIFFERENT ctxHost=$ctxHost host=$hostName} ")
			  newctx = QakContext( ctx, "$ctxHost", portNum, "", true) //isa ActorBasic
		}else{
			  println("               %%% sysUtil | createTheContext $ctx FOR host=$hostName  ")
			  newctx = QakContext( ctx, "$ctxHost", portNum, "") //isa ActorBasic
		}
		//val newctx = QakContext( ctx, "$ctxHost", portNum, "") //isa ActorBasic
		newctx.mqttAddr = mqttAddr //!!!!!! INJECTION !!!!!!
		ctxsMap.put(ctx, newctx)
		if( ! ctxHost.equals(hostName) ){
			return null
		}
		ctxOnHost.add(newctx)
		return newctx
 	}//createTheContext

	@JvmStatic fun createTheContext(ctx: String, hostName: String, localContextName: String) : QakContext?{
		//sysUtil.aboutThreads("sysUtil createTheContext $ctx localContextName=$localContextName host=$hostName")
		//CommUtils.outmagenta("sysUtil createTheContext $ctx localContextName=$localContextName host=$hostName")
		val ctxHost : String?     = solve("getCtxHost($ctx,H)","H")
		val ctxPort     : String? = solve("getCtxPort($ctx,P)","P")
		//println("               %%% sysUtil | $ctx host=$ctxHost port = $ctxPort protocol=$ctxProtocol")
		val portNum = Integer.parseInt(ctxPort)
		var mqttAddr = ""
		if( mqttBrokerIP != null ){
			mqttAddr = "tcp://$mqttBrokerIP:$mqttBrokerPort"
			sysUtil.aboutThreads("sysUtil | context $ctx WORKS ALSO WITH MQTT mqttAddr=$mqttAddr")
		}
		//CREATE AND MEMO THE CONTEXT
		var newctx : QakContext? = null
		if( ctx != localContextName && hostName != "localhost" ){ //NOV22  //
			//sysUtil.aboutThreads("sysUtil | createTheContext $ctx for DIFFERENT node (localContextName=$localContextName host=$ctxHost) ")
			//OCT24
            //CommUtils.outmagenta("               %%% sysUtil +++ createTheContext $ctx for DIFFERENT node (localContextName=$localContextName host=$ctxHost) ");
		    newctx = QakContext( ctx, "$ctxHost", portNum, "", true) //isa ActorBasic
		}else if( ctx == localContextName && hostName == "localhost"){
			//sysUtil.aboutThreads("sysUtil | createTheContext $ctx for LOCAL node (host=$hostName)  ")
			newctx = QakContext( ctx, "$ctxHost", portNum, "") //isa ActorBasic
			//CommUtils.outmagenta("sysUtil createTheContext DONE $newctx   host=$ctxHost")
		}else{
			//CommUtils.outred("sysUtil createTheContext STRANGE host " + hostName + " for ctx=" + localContextName);
			return null
		}
		//val newctx = QakContext( ctx, "$ctxHost", portNum, "") //isa ActorBasic
		if( newctx != null ){
			CommUtils.outcyan("               %%% sysUtil | INJECTION ctxsMap.put ${newctx.name} mqttAddr=$mqttAddr")
			newctx.mqttAddr = mqttAddr //!!!!!! INJECTION !!!!!!
		    ctxsMap.put(ctx, newctx)
			ctxOnHost.add(newctx)
			return newctx
		}else return null;
		//if( ctx!=localContextName ){ return null }
	}//createTheContext

	fun addProxyToOtherCtxs( ctxsList : List<String>, hostName : String, localContextName: String? = null){
		ctxsList.forEach { ctx ->
			//aboutThreads("sysUtil addProxyToOtherCtxs $ctx in $ctxsList host=$hostName localContextName=$localContextName")
			val curCtx = ctxsMap.get("$ctx")
			//val a : Boolean
			val isLocalContext = if(localContextName==null){
				curCtx!!.hostAddr == hostName
			}else{
				ctx == localContextName
			}
			if( curCtx is QakContext && !isLocalContext ) {
				//CommUtils.outmagenta("sysUtil addProxyToOtherCtxs $localContextName $ctx")
				val others = solve("getOtherContextNames(OTHERS,$ctx)","OTHERS")
				val ctxs   = strRepToList(others!!)
				ctxs.forEach {
					if( it.length==0  ) return
					val ctxOther = ctxsMap.get("$it")
					if (ctxOther is QakContext ) { //&& ctxOther != curCtx Aug2022
						//CommUtils.outmagenta("sysUtil addCtxProxy ${ctxOther.name} , ${curCtx.name}")
 						ctxOther.addCtxProxy(curCtx)
 					}else{  //NEVER??
 						//CommUtils.outred("               %%% sysUtil | addProxyToOtherCtxs $ctxOther")
						if( ctxOther==null ) return;
						if( ctxOther!!.mqttAddr.length > 1 )  return //NO PROXY for MQTT ctx
						println("               %%% sysUtil | WARNING: CONTEXT ${it} NOT ACTIVATED: " +
								"WE SHOULD WAIT FOR IT, TO SET THE PROXY in ${curCtx.name}")
						val ctxHost : String?     = solve("getCtxHost($it,H)","H")
						val ctxProtocol : String? = solve("getCtxProtocol($it,P)","P")
						val ctxPort : String?     = solve("getCtxPort($it,P)","P")
						curCtx.addCtxProxy( it, ctxProtocol!!, ctxHost!!, ctxPort!! )
					}
				}
			} //else{ println("sysUtil | WARNING: $ctx NOT ACTIVATED ") }
		}
	}//addProxyToOtherCtxs

	@JvmStatic fun createProxy( ctx: String, curCtx: QakContext ){
		aboutThreads("createProxy to $ctx  for ${curCtx.name}")
		/*
		val others = solve("getOtherContextNames(OTHERS,$ctx)","OTHERS")
		val ctxs   = strRepToList(others!!)
		CommUtils.outred("createProxy has the list: $ctxs   ")
		ctxs.forEach {*/
			//if( it.length==0  ) return
			val ctxOther = ctxsMap.get(ctx)
			if (ctxOther != null && ctxOther is QakContext ) { //&& ctxOther != curCtx Aug2022
				aboutThreads("               sysUtil addCtxProxy ${ctx} in ${curCtx.name}")
				curCtx.addCtxProxy(ctxOther)
			}else{
				CommUtils.outred("               sysUtil createProxy does not find the context ${ctx}")
			}
			/* else{  //NEVER??
				CommUtils.outred("addProxyToOtherCtxs $ctxOther ???")
				if( ctxOther==null ) return;
				if( ctxOther!!.mqttAddr.length > 1 )  return //NO PROXY for MQTT ctx
				CommUtils.outred("%%% sysUtil | WARNING: CONTEXT ${it} NOT ACTIVATED: " +
						"WE SHOULD WAIT FOR IT, TO SET THE PROXY in ${curCtx.name}")
				val ctxHost : String?     = solve("getCtxHost($it,H)","H")
				val ctxProtocol : String? = solve("getCtxProtocol($it,P)","P")
				val ctxPort : String?     = solve("getCtxPort($it,P)","P")
				curCtx.addCtxProxy( it, ctxProtocol!!, ctxHost!!, ctxPort!! )
			}*/
		//}
	}

	@JvmStatic fun getActorNames(ctxName: String) : List<String>{
		val actorNames : String? = solve("getActorNames(A,$ctxName)","A" )
		val actorList = strRepToList(actorNames!!)
		return actorList
	}
	@JvmStatic fun getAllActorNames(ctxName: String) : List<String>{
		val actorNames : String? = solve("getAllActorNames(A,$ctxName)","A" )
		val actorList = strRepToList(actorNames!!)
		return actorList
	}
	@JvmStatic fun getAllActorNames( ) : List<String>{
		val actorNames : String? = solve("getAllActorNames(A)","A" )
		val actorList = strRepToList(actorNames!!)
		return actorList
	}
	@JvmStatic fun getCtxCommonobjClass(ctxName: String) :  String{
		val objclassName : String? = solve("commonobjinctx($ctxName, H)", "H" )
		if( objclassName != null )
			return objclassName
		else return ""
	}
	@JvmStatic fun getNonlocalActorNames( ctx : String ) : List<String>{
		val actorNames : String? = solve("getNonlocalActorNames($ctx,A)","A" )
		val actorList = strRepToList(actorNames!!)
		return actorList
	}

	@JvmStatic  fun strRepToList( liststrRep: String ) : List<String>{
		//if( liststrRep.contains(","))
		return liststrRep.replace("[","")
			.replace("]","").split(",")
		//else return List<String>({""})
	}
	@JvmStatic  fun createTheActors( ctx: QakContext, scope : CoroutineScope ){
		val actorList = getAllActorNames(ctx.name)
		//aboutThreads("sysUtil|createTheActors in ${ctx.name}:$actorList "   )
		actorList.forEach{
			if( it.length > 0 ){
				val actorClass = solve("qactor($it,${ctx.name},CLASS)","CLASS")
				//println("sysUtil | CREATE actor=$it in context:${ctx.name}  class=$actorClass"   )
				val className = actorClass!!.replace("'","")
				createActor( ctx, it, className, scope, false)
			}
		}
	}//createTheActors

	//NOV22 Creazione di attori in nuovo contesto su localhost dopo che un contesto su localhost è stato attivato
	//DEPRECATED
	/*
	@JvmStatic  fun createOtherLocalActors( ctx: String, curCtx: String  ){
		CommUtils.outgreen("sysUtil | createOtherLocalActors ctx=$ctx curCtx:${curCtx}  "   )
		//createTheContext(ctx, "localhost", ctx)
		val actorList = getAllActorNames( ctx )
		actorList.forEach{
			if( it.length > 0 ){
				val actorClass = solve("qactor($it,${ctx},CLASS)","CLASS")
				CommUtils.outgreen("sysUtil | CREATE actor=$it in context:${ctx}  class=$actorClass"   )
				val className = actorClass!!.replace("'","")
				val actorCtx  = getContext(ctx);
				if( actorCtx != null) {
					val a = createActor( actorCtx, it, className, QakContext.scope22)
					if( a != null ) getActorContext(curCtx)!!.actorMap.putIfAbsent(it, a)
				}else CommUtils.outred("sysUtil | createOtherLocalActors - no context found for " + ctx);
			}
		}
	}*/

	@JvmStatic fun createActor( ctx: QakContext, actorName: String,
					 className : String, scope : CoroutineScope = GlobalScope,
								//discardMessages : Boolean = false,
	                 confined : Boolean = false) : ActorBasic?{

		//aboutThreads("%%% sysUtil | CREATE actor=$actorName in context:${ctx.name}  class=$className"   )
		val clazz = Class.forName(className)	//Class<?>
		CommUtils.outyellow("               sysUtil  | createActor class for $className   ${clazz}" )
        var actor  : ActorBasic
		var ctor   : java.lang.reflect.Constructor<out Any>
        try {
            ctor = clazz.getConstructor(
				String::class.java, CoroutineScope::class.java, Boolean::class.java, Boolean::class.java )  //Constructor<?>
			actor = ctor.newInstance(actorName, scope, confined,false  ) as ActorBasic

			//QUI PARTE SUBITO ....
        //val ctor = clazz.getConstructor(String::class.java, CoroutineScope::class.java,
			//	Boolean::class.java, Boolean::class.java, Boolean::class.java, Int::class.java )  //Constructor<?>
            //actor = ctor.newInstance(actorName, scope, discardMessages, confined, false, 200  ) as ActorBasic
        } catch( e : Exception ){
        	CommUtils.outred("               sysUtil  |  createActor ERROR: ${e}")
			//aboutThreads("sysUtil  | WARNING: createActor $actorName  ${e}" )
            //Oct 2023 val ctor = clazz.getConstructor( String::class.java )  //Constructor<?>
            //Oct 2023 actor    = ctor.newInstance( actorName  ) as ActorBasic
			//aboutThreads("sysUtil createActor ${actor.name} in ${ctx.name}" );
			ctor = clazz.getConstructor( String::class.java  )  //Constructor<?>
			actor = ctor.newInstance(actorName  ) as ActorBasic
 			//throw e
		}

		//CommUtils.outgreen("sysUtillllllllll  | createActor addActor ${actor.name}in ${ctx.name}")
		//L'actor è PARTITO ed esegue lo stato iniziale mentre la connessione MQTT non c'è ancora

		ctx.addActor(actor)
		//NON faccio partire in modo automatico ActorBasic. Lo farà ActorBasicFsm
		//MEMO THE ACTOR
		ctxActorMap.put(actorName,actor  )
		return actor
	}

	@JvmStatic fun createActorFsm( ctx: QakContext, actorName: String,
								className : String, scope : CoroutineScope = GlobalScope ,
								confined : Boolean, dinamically : Boolean ) : ActorBasicFsm?{

		//sysUtil.aboutThreads("%%% sysUtil | CREATE actor=$actorName in context:${ctx.name}  class=$className"   )
		val clazz = Class.forName(className)	//Class<?>
		//CommUtils.outred("               sysUtil  | createActorFsm class for $className   ${clazz}" )
		var actor  : ActorBasicFsm
		try {
			val ctor = clazz.getConstructor(
				String::class.java, CoroutineScope::class.java, Boolean::class.java, Boolean::class.java)
				//,Boolean::class.java, Boolean::class.java, Boolean::class.java, Int::class.java )  //Constructor<?>
			actor = ctor.newInstance( actorName, scope , confined, dinamically ) as ActorBasicFsm

			//CommUtils.outgreen("               sysUtillllllllll  | createActorFsm addActor ${actor.name}in ${ctx.name}")
			ctx.addActor(actor)
			actor.context = ctx
			//actor.isStarted = true;
			ctxActorMap.put(actorName,actor  )

			//Aggiungo l'info sul nuovo qactir al contesto corente.
			//NON NECESSARIO perchè sysUtil.createActorFsm aggiorna ctxActorMap
			//MA UTILE nel caso di introspezione
			val fact = "qactor($actorName,${ctx.name},\"$className\" )"
			val sol  = pengine.solve("assert("+ fact +").")
			if( ! sol.isSuccess ) CommUtils.outred("sysUtil.createActorFsm FAILS to add $fact")
		} catch( e : Exception ){
			CommUtils.outred("               sysUtil  | createActorFsm ERROR ${e}" )
			//Oct 2023 val ctor = clazz.getConstructor( String::class.java )  //Constructor<?>
			//Oct 2023 actor    = ctor.newInstance( actorName  ) as ActorBasicFsm
			//aboutThreads("sysUtil createActorFsm ${actor.name} in ${ctx.name}" );
			throw e
		}

		return actor
	}


	@JvmStatic fun solve( goal: String, resVar: String  ) : String? {
		//println("sysUtil  | solveGoal ${goal} resVar=$resVar" );
		//val sol = pengine.solve( "context(CTX, HOST,  PROTOCOL, PORT)."); //, "CTX"
		val sol = pengine.solve( goal+".");
		if( sol.isSuccess ) {
			if( resVar.length == 0 ) return "success"
			val result = sol.getVarValue(resVar)  //Term
			var resStr = result.toString()
			return  strCleaned( resStr )
		}
		else return null
	}

	@JvmStatic fun loadTheory( path: String ) {
		try {
			//user.dir is typically the directory in which the Java virtual machine was invoked.
			//val executionPath = System.getProperty("user.dir")
			//println("               %%% sysUtil | loadheory executionPath: ${executionPath}" )
			//val resource = classLoader.getResource("/") //URL
			//val cl =  javaClass<ActorBasic> //javaClass does not work
			//println("               %%% sysUtil | loadheory classloader: ${cl}" )
			CommUtils.outyellow("               %%% sysUtil | loadheory  $path" )
			val worldTh = Theory( FileInputStream(path) )
			pengine.addTheory(worldTh)
		} catch (e: Exception) {
			CommUtils.outgray("               %%% sysUtil | loadheory WARNING: ${e}" )
			loadTheoryFromDistribution("../"+path)  //JAN24
			//throw e
		}
	}
	@JvmStatic fun loadTheoryFromDistribution( path: String ) { //JAN24
		try {
			CommUtils.outyellow("               %%% sysUtil | loadTheoryFromDistribution  $path" )
			val worldTh = Theory( FileInputStream(path) )
			pengine.addTheory(worldTh)
		} catch (e: Exception) {
			CommUtils.outred("               %%% sysUtil | loadTheoryFromDistribution WARNING: ${e}" )
 			//throw e
		}

	}



	@JvmStatic fun strCleaned( s : String) : String{
		if( s.startsWith("'")) return s.replace("'","")
		else return s

	}


	fun traceprintln( msg : String ){
		if( sysUtil.trace ) CommUtils.outyellow(
			msg + " | " + Thread.currentThread().getName() + " n=" + Thread.activeCount())
	}

/*
 	MSG LOGS
*/
@JvmStatic fun createFile( fname : String, dir : String = "logs" ){
 		val logDirectory = File("$userDirectory/$dir")
		logDirectory.mkdirs()	//have the object build the directory structure, if needed
		var file = File(logDirectory, fname)
//		println("               %%% sysUtil | createFile file $file in $dir")
		file.writeText("")	//file is created and nothing is written to it
	}

	@JvmStatic fun deleteFile( fname : String, dir  : String ){
		File("$userDirectory/$dir/$fname").delete()
	}
	@JvmStatic fun updateLogfile( fname: String, msg : String, dir : String = "logs" ){
		if( logMsgs ) File("$userDirectory/$dir/$fname").appendText("${msg}\n")
	}
	@JvmStatic  fun aboutThreads(info: String){
		val tname    = Thread.currentThread().getName();
		val nThreads = ""+Thread.activeCount() ;
		CommUtils.outyellow("               %%% $info thread=$tname nThrds=$nThreads"  )
	}

	@JvmStatic  fun waitUser(prompt: String, tout: Long = 2000   ) {
			try {
				print(">>>  $prompt (tout=$tout) >>>  ")
				val input     = BufferedReader(InputStreamReader(System.`in`))
				val startTime = System.currentTimeMillis()
				while (System.currentTimeMillis() - startTime < tout && !input.ready() ) { }
				println("")
  			} catch (e: java.lang.Exception) {
				e.printStackTrace()
			}
	}

	//JAN24

	@JvmStatic fun showOutput(proc: Process) {
		object : Thread() {
			override fun run() {
				try {
					val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
					val stdError = BufferedReader(InputStreamReader(proc.errorStream))
					// Read the output from the command
					CommUtils.outcyan("Here is the standard output of the command:\n")
					var s: String? = null
					while (stdInput.readLine().also { s = it } != null) {
						println(s)
					}
					// Read any errors from the attempted command
					//CommUtils.outcyan("Here is the standard error of the command (if any):\n")
					while (stdError.readLine().also { s = it } != null) {
						CommUtils.outcyan("Here is the standard error of the command (if any):\n")
						println(s)
					}
				} catch (e: java.lang.Exception) {
					fail("startTheService " + e.message)
				}
			}
		}.start()
		CommUtils.outblue("started")
	}

	//OCT24
	@JvmStatic  fun  toPrologStr(s: String, on: Boolean = true) : String{
		try{
			if( !on ) return s.replace("'","")  //tolgo gli apici
			else {
				org.json.simple.parser.JSONParser().parse(s)
				return "'$s'"
			}
		}catch(e: Exception){
			try{
				val t = Term.createTerm(s)
                //UN termione prolog è ok
				return s
			}catch(e1: Exception) {  //Stringa qualsiasi, ad ese. con bianchi
				//CommUtils.outred("toPrologStr not a term")
				return "'$s'"
			}
		}
	}

	@JvmStatic  fun  logStr(source: String, message:String,category:String) : String{
		val jsonObj = JSONObject()
		jsonObj.put("category", category)
		jsonObj.put("source", source)
		jsonObj.put("message", message)
		return jsonObj.toJSONString()
	}

	@JvmStatic fun clearlog(fname: String){
		CommUtils.outmagenta("clear log $fname ---- ")
		val writer = java.io.PrintWriter("$fname");
		writer.print("");
		writer.close();
	}

	//APRIL2025
	@JvmStatic fun changeActorName( actor : ActorBasic , newname : String ){
		//ctxActorMap.remove()
		actor.MyName = newname;
		actor.name   = newname;
		CommUtils.outred("		sysUtilllllllllllllllllllllllll | changeActorName $newname")
		/*
		//ctxActorMap.put( newname,actor  )
		val fact = "qactor($newname,${actor.context!!.name},\"dontcare\" )"
		val sol  = pengine.solve("assert("+ fact +").")
		if( ! sol.isSuccess ) CommUtils.outred("sysUtil.changeActorName FAILS to add $fact")
		*/
		actor.context!!.actorMap.put(newname,actor);
	}

     //JUNE2025
	 @JvmStatic fun discoverservciectx(  ctx : String ) : Array<String>{
 		 	val hostPort = CommUtils.discoverService( eurekaClient, ctx )
			 if (hostPort == null) {
				 CommUtils.outred("WARNING: $ctx  NOT DISCOVERED")
			 } else {
				 val host = hostPort[0]
				 val port = hostPort[1]
				 CommUtils.outyellow("sysUtill | discovered $ctx at host=${host} port=${port}")
				 val discoveredconn = TcpConnection(host, port.toInt())
				 ctxsDiscoveredMap.put(ctx, discoveredconn)
			 }
			return hostPort
	 }

}//sysUtil
