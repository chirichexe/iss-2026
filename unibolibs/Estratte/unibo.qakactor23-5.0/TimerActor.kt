package it.unibo.kactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import unibo.basicomm23.utils.CommUtils
import java.io.BufferedReader
import java.io.InputStreamReader

class TimerActor(name: String, scope: CoroutineScope, val ctx : QakContext,
                 val ev: String, val tout : Long) : Thread() { //: ActorBasic(name,scope)
    var terminated = false;
    var starttime = 0L
    var Duration  = 0L

    lateinit var myactor : ActorBasic
    init{
        //Refactored July 2021, in order to avoid the usage of delay(tout)
        var ownerName = ev.replace("local_tout_","")
        ownerName     =  ownerName.substringBeforeLast("_") //OCT 2023
        getTheActor(ownerName)  //APRIL2025
       /*
        CommUtils.delay(1000) //Give the time to start
        println("TimerActor inittttttttttttttttttttt time=$tout  ")
        try{
            myactor = QakContext.getActor(ownerName)!!
            start()
        }catch(e:Exception){
            println("WARNINGGGGGGGGGGGGGGGGGG: TimerActor time=$tout does not find its owner:${ownerName}")
        }
        */
    }


    fun getTheActor(ownerName:String){
 //       try{
            //println("   TimerActor |  TimerActor time=$tout check owner:${ownerName}")
            var test = QakContext.getActor(ownerName) //Potrebbe non essere stato registrato
            while( test == null ) {
                CommUtils.delay(500)
                CommUtils.outred("      TimerActor |  TimerActor TOO FAST ERROR  ")  //defensive NO MORE
                test = QakContext.getActor(ownerName)
            }
            myactor = test!!  //Potrebbe non essere stato registrato
                //println("   TimerActor |  TimerActor time=$tout check owner:${ownerName} myactor=$myactor")
            start()

 /*
        }catch(e:Exception){
            CommUtils.outred("   TimerActor |  TimerActor TOO FAST ERROR ${e.message}")
            CommUtils.delay(1000) //Give the time MQTT to conncet ....
            getTheActor( ownerName )
        }
 */
    }

    override fun run(){
        //println("TimerActor $name SLEEP tout=$tout ct=${System.currentTimeMillis()} ")
        starttime     = System.currentTimeMillis()
        Thread.sleep(tout)
        val dd = System.currentTimeMillis() - starttime
        //Runtime.getRuntime().exec("sudo ./sleepcrono")
        //println("TimerThread  RESUMES after:${dd} ")
        if( ! terminated ){
            val msgEv = MsgUtil.buildEvent("timer",ev,ev)
            runBlocking {
                myactor.actor.send(msgEv)
            }
              //CommUtils.outred("TimerActor $name has EMITTED:${msgEv} ")
        }else{
            //println("TimerActor $name ENDS without emitting since terminated after: $Duration")
        }
    }

    /*


   override suspend fun actorBody(msg : ApplMessage){
        //sysUtil.trace
    println("TimerActor using delay RECEIVES  ${msg} tout=$tout")
        if( msg.msgId() == "start") {
            /*
            The thread is returned to the pool while the coroutine is waiting,
            and when the waiting is done, the coroutine resumes on a free thread in the pool.
             */
            delay(tout)
                // Thread.sleep(tout)
            //delayUsingLinux()
            if( ! terminated ){
                emit(ev, ev)
                println("TimerActor terminated=$terminated EMITTED :${ev} ")
            }
            this.actor.close()
            //println("TimerActor $tout ENDS ")
        }
    }
*/
    fun delayUsingLinux(){
        val p      = Runtime.getRuntime().exec("sudo ./cronoLinux")
        val reader = BufferedReader(  InputStreamReader(p.getInputStream() ))
        println(" startReaddddddddddddddddddddddddddddddd ")
        //GlobalScope.launch{
        while( true ){
            var data = reader.readLine()
            println("crono data = $data"   )
            if( data == null ) break
        }
        //}

    }
    fun endTimer(){
        Duration = System.currentTimeMillis() - starttime
        //println("TimerActor $name TERMINATED duration=$Duration")
        terminated = true;
    }


}