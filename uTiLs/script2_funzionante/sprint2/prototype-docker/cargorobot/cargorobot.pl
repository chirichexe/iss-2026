%====================================================================================
% cargorobot description   
%====================================================================================
mqttBroker("localhost", "1883", "cargosystem").
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ).
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
%====================================================================================
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
context(ctxioport, "127.0.0.1",  "TCP", "8051").
context(ctxdevices, "127.0.0.1",  "TCP", "8052").
context(ctxrobot, "localhost",  "TCP", "8053").
context(ctxrobotsmart, "127.0.0.1",  "TCP", "8020").
 qactor( cargoservice, ctxcargoservice, "external").
  qactor( robotsmart, ctxrobotsmart, "external").
  qactor( cargorobot, ctxrobot, "it.unibo.cargorobot.Cargorobot").
 static(cargorobot).
