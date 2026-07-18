%====================================================================================
% ioport description   
%====================================================================================
mqttBroker("localhost", "1883", "cargosystem").
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
dispatch( led_ctrl, ledCmd(CMD) ).
event( led_net_event, ledCmd(CMD) ).
%====================================================================================
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
context(ctxioport, "localhost",  "TCP", "8051").
context(ctxdevices, "127.0.0.1",  "TCP", "8052").
context(ctxrobot, "127.0.0.1",  "TCP", "8053").
 qactor( ledadapter, ctxioport, "it.unibo.ledadapter.Ledadapter").
 static(ledadapter).
  qactor( cargoservice, ctxcargoservice, "external").
