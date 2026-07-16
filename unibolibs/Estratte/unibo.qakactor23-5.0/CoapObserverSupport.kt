package it.unibo.kactor

import it.unibo.kactor.MsgUtil.buildDispatch
import org.eclipse.californium.core.CoapObserveRelation
import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.CoapHandler
import org.eclipse.californium.core.CoapResponse
import unibo.basicomm23.tcp.TcpConnection
import unibo.basicomm23.utils.CommUtils


class CoapObserverSupport(
    private val owner: ActorBasic,
    host: String?,
    port: String,
    private val ctx: String,
    private val actorName: String,
    val msgid: String =  "coapUpdate",   //DEC23
    private val tt : String     = "               %%% "
) {
    private var relation: CoapObserveRelation? = null
    private var client: CoapClient? = null

    fun observe() {
        relation = client!!.observe( //
            object : CoapHandler {
                override fun onLoad(response: CoapResponse) {
                    val content = response.responseText
                    //CommUtils.outmagenta("CoapObserverSupport | content=$content msgid=$msgid" )
                    if (!content.contains("(createdtobediscared)")) {  //DEC23
                        val actorDispatch = buildDispatch(
                            actorName, "$msgid",
                            //"$msgid(RESOURCE, VALUE)".replace("RESOURCE", actorName).replace("VALUE", content),
                            //content,  //SEPT2024 after DagaBertoniD'Arsie'
                            "changed(" + content + ")",  //MAY2025
                            owner.name
                        )
                        //CommUtils.outyellow("CoapObserverSupport | sends $actorDispatch" )
                        owner.sendMsgToMyself(actorDispatch)
                    }
                }

                override fun onError() {
                    CommUtils.outred("CoapObserverSupport | OBSERVING FAILED")
                }
            })
    }//observe

    init {
        var addr = ""
        var res : Array<String>?  = null
        if (host!! == "discoverable") {
            //CommUtils.outred("$ctx discoverable CoapObserverSupport   ")
            var hostDicovered = sysUtil.ctxsDiscoveredMap.get(ctx)
             if (hostDicovered == null) {
                 res = sysUtil.discoverservciectx(ctx) //updates sysUtil.ctxsDiscoveredMap
                 if (res != null) {
                     addr = "coap://HOST:".replace("HOST", res[0]) + res[1] + "/CONTEXT/".replace(
                         "CONTEXT",
                         ctx
                     ) + actorName
                     CommUtils.outyellow("$tt CoapObserverSupport | init addr=$addr")
                     client = CoapClient(addr)
                     observe()
                 } else {
                     CommUtils.outred("FATAL discover")
                 }
             }
        }
    }//init
}