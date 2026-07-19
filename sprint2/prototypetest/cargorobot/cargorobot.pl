%====================================================================================
% cargorobot description   
%====================================================================================
mqttBroker("localhost", "1883", "robotsmart26in").
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ).
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
dispatch( stop_robot, stop(none) ).
dispatch( resume_robot, resume(none) ).
event( alarm, alarm(X) ).
%====================================================================================
context(ctxcargorobot, "localhost",  "TCP", "8053").
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
context(ctxrobotsmart, "127.0.0.1",  "TCP", "8020").
 qactor( cargoservice, ctxcargoservice, "external").
  qactor( robotsmart, ctxrobotsmart, "external").
  qactor( cargorobot, ctxcargorobot, "it.unibo.cargorobot.Cargorobot").
 static(cargorobot).
