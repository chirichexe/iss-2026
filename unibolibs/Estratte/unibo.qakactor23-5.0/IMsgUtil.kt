package it.unibo.kactor

import unibo.basicomm23.interfaces.Interaction
import unibo.basicomm23.tcp.TcpConnection
import unibo.basicomm23.udp.UdpConnection
import unibo.basicomm23.interfaces.IApplMessage
import java.net.DatagramSocket
import java.net.Socket
import unibo.basicomm23.udp.UdpEndpoint

interface  IMsgUtil{
    fun buildDispatch(actor:String,msgId:String,content:String,dest:String):IApplMessage

    fun buildRequest( actor: String, msgId : String ,
                      content : String, dest: String ) : IApplMessage

    fun buildReply( actor: String, msgId : String ,
                    content : String, dest: String ) : IApplMessage

    fun buildEvent( actor: String, msgId : String , content : String  ) : IApplMessage

    //

    fun sendMsg( sender : String, msgId: String, msg: String, destActor: ActorBasic)
    fun sendRequest(sender : String, msgId: String, msg: String, destActor: ActorBasic)
    fun sendMsg(msg: IApplMessage, destActor: ActorBasic)
    fun sendMsg(msgId: String, msg: String, destActor: ActorBasic)
    fun sendRequest(msg: IApplMessage, destActor: ActorBasic)
    fun emitEvent(msg: IApplMessage)
    fun emitLocalEvent(msg: IApplMessage,  actor: ActorBasic)
    fun emitLocalStreamEvent( msg: IApplMessage,  actor: ActorBasic )


    fun sendMsg(  sender: String, msgId : String, payload: String, destName : String, mqtt: MqttUtils )
    fun getConnection(protocol: Protocol, hostName: String, portNum: Int, clientName:String) : Interaction?
    fun getConnectionSerial( portName: String, rate: Int) : Interaction


    fun strToProtocol( ps: String):Protocol
}