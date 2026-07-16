package it.unibo.kactor

import it.unibo.kactor.sysUtil.ctxsMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newSingleThreadContext
import org.eclipse.californium.core.CoapServer
import unibo.basicomm23.interfaces.IApplMessage
import unibo.basicomm23.utils.CommUtils
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

open class QakContext(name: String, val hostAddr: String, val portNum: Int, var mqttAddr : String = "",
                      val external: Boolean=false, val gui : Boolean = false   ) : ActorBasic(name){

    internal val actorMap : ConcurrentHashMap<String, ActorBasic> = ConcurrentHashMap<String, ActorBasic>() //JUNE25 MutableMap<String, ActorBasic> = mutableMapOf<String, ActorBasic>()
    internal val proxyMap:  MutableMap<String, NodeProxy>  = mutableMapOf<String, NodeProxy>()  //cannot be static
    //lateinit private var serverCoap  :  CoapServer      //CoAP: Jan2020
    lateinit var resourceCtx : CoapResourceCtx
	lateinit var ctxserver  : QakContextServer
	lateinit var serverCoap : CoapServer
 	lateinit var ctxLogfileName : String

 	lateinit var commonObj : kotlin.Any

    companion object {
        val workTime = 1000L * 3600 * 10 //10 ore
        lateinit var scope22 : CoroutineScope
        enum class CtxMsg { attach, remove }

        fun getActor( actorName : String ) : ActorBasic? {
            return sysUtil.getActor(actorName)
        }


        fun createScope(): CoroutineScope{
            if( ! this::scope22.isInitialized ) {
                //println("               %%% QakContext | createScope  ++++++++++++++++++++++++++++++++++  ")
                val d = newSingleThreadContext("single");
                scope22 = CoroutineScope(d);
            }
            return scope22;
        }
/*
        fun createContexts(hostName: String, desrFilePath: String, rulesFilePath: String) {
        }
*/
        //Called by generated code main of ctx
        //ARGUMENT localContextName by Loris Giannatempo, August 2022
         fun createContexts(hostName: String, scope: CoroutineScope ,
                           desrFilePath: String, rulesFilePath: String,
                            localContextName: String? = null, ) {

            sysUtil.createContexts(hostName, desrFilePath, rulesFilePath, localContextName)
             if( sysUtil.ctxOnHost.size == 0 ){
                val ip = InetAddress.getLocalHost().getHostAddress()
  //              CommUtils.outred("               %%% QakContext | CREATING NO ACTORS on $hostName ip=${ip.toString()}")
                 CommUtils.outred("               %%% QakContext | no context found")
             }
            else{
                 //sysUtil.aboutThreads("QakContext scope=$scope BEFORE createContexts on $hostName " );
                 //println("               %%% QakContext | CREATING THE ACTORS on $hostName ")
            }
//			runBlocking {

            sysUtil.ctxOnHost.forEach {
                    ctx ->
                if( ctx.name == localContextName ){
                    //sysUtil.traceprintln("QakContext createTheActors for ${ctx.name} while $localContextName");
                    sysUtil.createTheActors(ctx, scope)
                }
            }

            //sysUtil.aboutThreads("QakContext AFTER createContexts on $hostName " );
 			//println("               %%% QakContext | createContexts on $hostName ENDS " )
        }
    }
    init{
        ctxLogfileName    = "${name}_MsLog.txt"	//APR2020
        //OCT2019 --> NOV2019 Create a QakContextServer also when we use MQTT
        resourceCtx = CoapResourceCtx( name, this )   //must be ininitialized here  ENABLE

        createCommonObj()

        if( ! external ){
            //sysUtil.aboutThreads("QakContext $hostAddr:$portNum AFTER CoapResourceCtx  " );
            //println("               %%% QakContext |  $hostAddr:$portNum INIT ")
            ctxserver = QakContextServer( this, createScope(), "server$name", Protocol.TCP ) //
            //CoAP: Jan2020

              try{
                  //sysUtil.waitUser("Starting CoapServer", 20000);
                  //sysUtil.aboutThreads("QakContext $hostAddr:$portNum BEFORE CoapServer " );
                  val coapPort    =  portNum
                  serverCoap      =  CoapServer(coapPort) //from org.eclipse.californium.core
                  serverCoap.add(  resourceCtx )
                  serverCoap.start()
                  CommUtils.outyellow("               %%% QakContext $name | serverCoap started on port $coapPort with added ${resourceCtx.name}")

                  //println("%%% serverCoap started "  )

                  //sysUtil.aboutThreads("QakContext $hostAddr:$portNum AFTER CoapServer on port: $coapPort " );
                                    //println( "               %%% QakContext $name |  serverCoap started on port: $coapPort" )
            }catch(e : Exception){
                println( "               %%% QakContext $name |  serverCoap error: ${e.message}" )
            }
          }
}

	fun terminateTheContext(){
        CommUtils.outmagenta("terminateTheContext " + this.name)
		serverCoap.stop()
		ctxserver.actor.close() //Un actor
	}

    override suspend fun actorBody(msg : IApplMessage){
        sysUtil.traceprintln( "               %%% QakContext $name |  receives $msg " )
    }


    fun addCtxProxy(ctxName: String, protocol: String, hostAddr: String, portNumStr: String) {
        val p       = MsgUtil.strToProtocol(protocol)
        val portNum = Integer.parseInt(portNumStr)
        //sysUtil.traceprintln("               %%% QakContext $name | addCtxProxy ${ctxName}, $hostAddr, $portNum")\
        val proxy = NodeProxy("proxy${ctxName}", this, p, hostAddr, portNum)
        CommUtils.outcyan("proxyMap.put ${ctxName}, $proxy" )
        proxyMap.put(ctxName, proxy)
    }
    /*
    Aug2022: gli attori vanno creati prima di lanciare il QakContextServer
     */
    fun addActor( actor: ActorBasic) {
        //CommUtils.outred("               %%% QakContext | addActor ${actor.name}  ")
        actor.context = this    //injects the context
 		//actor.createMsglogFileInContext()
        actorMap.put( actor.name, actor )
/*
        if( actor.checkMqtt()  ) { //fa una mqtt.conect che richiede tempo
            CommUtils.outred("               %%% QakContext | addActor ${actor.name} connecting to mqtt ...")
            //CommUtils.delay(1000)
        }*/
        //sysUtil.traceprintln("               %%% QakContext $name | addActor ${actor.name}")
            if (this::resourceCtx.isInitialized) {
                sysUtil.traceprintln("               %%% QakContext | addActor ${actor.name}")
                resourceCtx.addActorResource(actor)
            }   //CoAP: Jan2020
            else CommUtils.outred("               %%% QakContext | addActor resourceCtx not initialized")
    }

    fun addInternalActor( actor: ActorBasic) {
        actor.context = this    //injects the context
// 		actor.createMsglogFileInContext()	//internal actors have no context !
        actorMap.put( actor.name, actor )
        sysUtil.ctxActorMap.put(actor.name,actor  )  //Aug23
    }



    fun removeInternalActor( actor: ActorBasic, arg:Int=0){
        //actorMap.remove( actor.name )
 		actor.terminate(arg)
     }

    fun hasActor( actorName: String ) : ActorBasic? {
        return actorMap.get(actorName)
    }

    fun addCtxProxy( ctx : QakContext ){
        val pxy = proxyMap.get(ctx.name) //OCT23
        if( pxy != null ){
            CommUtils.outmagenta("addCtxProxy has already set proxy to ${ctx.name}")
            return;
        }
        //if( ctx.mqttAddr.length > 1 ) return  //WHY ???? MAY2024
        CommUtils.outmagenta("               %%% QakContext $name | addCtxProxy ip=${ctx.hostAddr}:${ctx.portNum}")
        val proxy = NodeProxy("proxy${ctx.name}", this, Protocol.TCP, ctx.hostAddr, ctx.portNum)
        proxyMap.put( ctx.name, proxy )
		//APR2020: we should remove the active connection from
    }

    fun addCtxProxy( ctxName :String, hostAddr: String, portNum : Int  ){
        sysUtil.traceprintln("               %%% QakContext $name | addCtxProxy host=$hostAddr portNum=${portNum}")
         val proxy = NodeProxy("proxy${ctxName}", this, Protocol.TCP, hostAddr, portNum)
        CommUtils.outcyan("               %%% QakContext $name | proxyMap.put ${ctxName}, $proxy")
        proxyMap.put( ctxName, proxy )
    }

    fun createCommonObj(){
        val commonObjClassName : String  = sysUtil.getCtxCommonobjClass(name)
        //CommUtils.outred("QakContext | createTheContext finds ${commonObjClassName}")
        if( commonObjClassName.length > 0 ) {
            val clazz = Class.forName(commonObjClassName)
            //clazz.getConstructor( String::class.java )  //Constructor<?>
            commonObj    = clazz.getDeclaredConstructor().newInstance()
            //CommUtils.outyellow("               %%% QakContext $name | createTheContext created ${commonObj}")
        }

    }

}