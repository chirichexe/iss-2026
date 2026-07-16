package unibo.basicomm23.utils;

// Import required classes for file and JSON handling
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unibo.basicomm23.msg.ProtocolType;

// Configuration class for communication system settings
public class CommSystemConfig {
    // MQTT broker address (default: localhost)
    public static  String mqttBrokerAddr = "tcp://localhost:1883"; //: 1883  OPTIONAL  tcp://broker.hivemq.com
    // Server timeout in milliseconds (default: 100 minutes)
    public static int serverTimeOut        =  6000000;  //100 minuti
    // Protocol type used for communication (default: TCP)
    public static ProtocolType protcolType = ProtocolType.tcp;
    // Enable or disable tracing of communication
    public static boolean tracing          = false;

    // JSON parser instance
    private static JSONParser simpleparser        = new JSONParser();;
    
    // Set configuration using default JSON file path
    public static void setTheConfiguration(  ) {
        setTheConfiguration("../CommSystemConfig.json");
    }
    
    // Set configuration using specified JSON file path
    public static void setTheConfiguration( String resourceName ) {
        // In distribution, resourceName is in a directory that includes the binary
        try {
            ColorsOut.out("%%% setTheConfiguration from file:" + resourceName);
            // Read lines from the configuration file
            Stream<String> stream = Files.lines(Paths.get(resourceName), StandardCharsets.UTF_8);
            // Parse each line as a JSON object and update configuration fields
            stream.forEach(s -> {
                JSONObject object;
                try {
                    object = (JSONObject) simpleparser.parse(s);
                    mqttBrokerAddr   = object.get("mqttBrokerAddr").toString();
                    tracing          = object.get("tracing").equals("true");
                    // Set protocol type based on JSON value
                    switch( object.get("protocolType").toString() ) {
                        case "tcp"  : protcolType = ProtocolType.tcp; break;
                        case "coap" : protcolType = ProtocolType.coap; break;
                        case "mqtt" : protcolType = ProtocolType.mqtt; break;
                    }
                } catch (ParseException e) {
                    ColorsOut.outerr("setTheConfiguration read ERROR " + e.getMessage() );
                }
            }  );
        }   catch ( Exception e) {
            ColorsOut.outerr("setTheConfiguration ERROR " + e.getMessage() );
        } 
    }    
    
}