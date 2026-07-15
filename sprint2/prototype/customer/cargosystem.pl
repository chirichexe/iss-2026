%====================================================================================
% cargosystem description   
%====================================================================================
mqttBroker("localhost", "1883", "cargosystem").
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
event( sonardata, distance(D) ).
dispatch( set_service_status, setServiceStatus(STATUS) ).
dispatch( led_ctrl, ledCmd(CMD) ).
%====================================================================================
context(ctxcargoservice, "localhost",  "TCP", "8050").
context(ctxcustomer, "localhost",  "TCP", "8051").
context(ctxdevices, "localhost",  "TCP", "8052").
context(ctxrobot, "localhost",  "TCP", "8053").
 qactor( ledmock, ctxcustomer, "it.unibo.ledmock.Ledmock").
 static(ledmock).
  qactor( cargoservice, ctxcargoservice, "external").
