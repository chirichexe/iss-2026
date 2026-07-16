package unibo.basicomm23.utils;

// Importing required classes for different protocol connections
import unibo.basicomm23.coap.CoapConnection;
import unibo.basicomm23.http.HttpConnection;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.mqtt.MqttInteraction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.tcp.TcpConnection;
import unibo.basicomm23.udp.UdpClientSupport;
import unibo.basicomm23.udp.UdpConnection;
import unibo.basicomm23.ws.WsConnection;

// Factory class to create protocol-specific client connections
public class ConnectionFactory {
    // Creates a client support for the given protocol, host, and entry
    public static Interaction createClientSupport23(
            ProtocolType protocol, String host, String entry){
        return (Interaction) createClientSupport(protocol,host,entry);
    }
    // Main factory method to create client support based on protocol
    public static Interaction createClientSupport(
            ProtocolType protocol, String host, String entry ){
        // Try to create the connection, handling exceptions
        try {
            switch( protocol ){
                case http  : {
                    // Create HTTP connection
                    Interaction  conn  = HttpConnection.create(  host ); //+":"+entry
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create htpp conn host=" + host );
                    return conn;
                }
                case ws   : {
                    // Create WebSocket connection
                    Interaction  conn  =  WsConnection.create( host, entry );
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create ws conn:" + host+" entry="+entry);
                    return conn;
                }
                case tcp   : {
                    // Create TCP connection (entry is port number)
                    int port              = Integer.valueOf(entry);
                    Interaction conn  = TcpConnection.create( host, port );
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create tcp port:" + port);
                    return conn;
                }
                case udp   : {
                    // Create UDP connection (entry is port number)
                    int port          = Integer.valueOf( entry );
                    Interaction conn  = UdpConnection.create(host, port);
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create udp port:" + port);
                    return conn;
                }
                case coap   : {
                    // Create CoAP connection
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create coap conn " + host + " entry=" + entry );
                    Interaction  conn  = CoapConnection.create(host, entry);
                    return conn;
                }
                case mqtt   : {
                    // Create MQTT connection (entry is topic info)
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create mqtt conn " + host + " entry=" + entry );
                    Interaction  conn  =  MqttInteraction.create(host,entry); //entry=name-topicIn-topicOut
                    return conn;
                }
                case bluetooth   : {
                    // Bluetooth connection not implemented
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create bluetooth conn TODO"  );
                    return null;
                }
                case serial   : {
                    // Serial connection not implemented
                    CommUtilsOrig.outyellow("    --- ConnectionFactory | create serial conn TODO"  );
                    return null;
                }
                default: return null;
            }
        }catch (Exception e) {  e.printStackTrace(); return null; }
    }//createClientSupport
}