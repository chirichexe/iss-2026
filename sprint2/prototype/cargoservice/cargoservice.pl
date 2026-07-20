%====================================================================================
% cargoservice description   
%====================================================================================
mqttBroker("localhost", "1883", "leddata").
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
event( sonar_event, distance(D) ).
event( led_event, ledCmd(CMD) ).
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ).
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
dispatch( stop_robot, stop(none) ).
dispatch( resume_robot, resume(none) ).
dispatch( deposit_timeout_msg, depositTimeout(none) ).
%====================================================================================
context(ctxcargoservice, "localhost",  "TCP", "8050").
context(ctxdevices, "127.0.0.1",  "TCP", "8052").
context(ctxrobot, "127.0.0.1",  "TCP", "8053").
 qactor( markerdevice, ctxdevices, "external").
  qactor( cargorobot, ctxrobot, "external").
  qactor( cargoservice, ctxcargoservice, "it.unibo.cargoservice.Cargoservice").
 static(cargoservice).
