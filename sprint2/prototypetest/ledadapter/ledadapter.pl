%====================================================================================
% ledadapter description   
%====================================================================================
mqttBroker("localhost", "1883", "leddata").
dispatch( led_ctrl, ledCmd(CMD) ).
event( led_event, ledCmd(CMD) ).
%====================================================================================
context(ctxledadapter, "localhost",  "TCP", "8055").
 qactor( ledadapter, ctxledadapter, "it.unibo.ledadapter.Ledadapter").
 static(ledadapter).
