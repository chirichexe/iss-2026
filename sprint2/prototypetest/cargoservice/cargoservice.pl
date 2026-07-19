%====================================================================================
% cargoservice description   
%====================================================================================
mqttBroker("localhost", "1883", "cargosystem").
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
dispatch( incoming_sonar, distance(D) ).
dispatch( led_ctrl, ledCmd(CMD) ).
dispatch( deposit_timeout_msg, depositTimeout(none) ).
dispatch( stop_robot, stop(none) ).
dispatch( resume_robot, resume(none) ).
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ).
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
%====================================================================================
context(ctxcargoservice, "localhost",  "TCP", "8050").
context(ctxcargorobot, "127.0.0.1",  "TCP", "8053").
context(ctxmarkerdevice, "127.0.0.1",  "TCP", "8054").
context(ctxledadapter, "127.0.0.1",  "TCP", "8055").
 qactor( cargorobot, ctxcargorobot, "external").
  qactor( markerdevice, ctxmarkerdevice, "external").
  qactor( ledadapter, ctxledadapter, "external").
  qactor( cargoservice, ctxcargoservice, "it.unibo.cargoservice.Cargoservice").
 static(cargoservice).
