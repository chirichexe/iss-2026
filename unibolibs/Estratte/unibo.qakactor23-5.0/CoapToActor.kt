package it.unibo.kactor

import kotlinx.coroutines.launch
import org.eclipse.californium.core.coap.CoAP
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.server.resources.CoapExchange
import unibo.basicomm23.interfaces.IApplMessage
import unibo.basicomm23.msg.ApplMessage
import unibo.basicomm23.utils.CommUtils
import java.nio.charset.StandardCharsets

/*
 * ----------------------------------------------------------------------------------------------
 * Temporary actor that makes a qak request and waits for a reply to a qak request
 * ----------------------------------------------------------------------------------------------
 */
//dlegated?? FEB2023
class CoapToActor(name : String, val exchange: CoapExchange,
                  val owner: ActorBasic, val extmsg : IApplMessage, val originalMsg : String
) : ActorBasic( name ){
var answer = "noanswer" 	
 	init{
        this.context = owner.context
        context!!.addInternalActor( this )
		sysUtil.traceprintln("$tt $name| CREATED in ctx=${context!!.name} exchange=${exchange.getSourceAddress()}")
		//scope.launch{ autoMsg("start", "start") }
        //CommUtils.outmagenta("CoapToActor $name init with  owner= ${owner.name} extmsg=$extmsg exchange=$exchange")
        argscope.launch{ request( extmsg.msgId(), extmsg.msgContent(), owner) }
     }



     override suspend fun actorBody(msg : IApplMessage){
         //sysUtil.traceprintln("$tt $name | PUT response: $msg exchange=${exchange.getSourceAddress()}"  )
         //CommUtils.outyellow("$tt $name | PUT response: $msg exchange=${exchange}"  )
         if( msg.isReply() ){
  			//answer = msg.toString().replace(name,owner.name)
  			 val answerMsg = msg.toString().replace(name,extmsg.msgSender()) //FEB2024
             val answer    = ApplMessage(answerMsg)
             //sysUtil.traceprintln("$tt $name | PUT answer: $answer" )
             //CommUtils.outyellow("$tt $name | PUT answer: $answer to ${answer.msgReceiver()}" )
             val answerEncoded = java.net.URLEncoder.encode( answer.toString(), StandardCharsets.UTF_8.toString() )
             //CommUtils.outcyan("$tt $name | PUT answerEncoded: $answerEncoded" )
             try {
                 exchange.respond(CoAP.ResponseCode.CONTENT, answerEncoded, MediaTypeRegistry.TEXT_PLAIN)
              }catch( e: Exception){
                 CommUtils.outred("exchange.respond ERROR " + e)
             }
             //la operazione 'respond' di org.eclipse.californium.core.server.resources.CoapExchange non invia dati

             //sendMessageToActor(answer,answer.msgReceiver())   //TODO Explain WHY


             context!!.removeInternalActor( this )
             //CommUtils.outcyan("$tt $name | removed")
         }
	}
}