%====================================================================================
% devices description   
%====================================================================================
mqttBroker("localhost", "1883", "cargosystem").
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
event( wall_sonardata, distance(D) ).
dispatch( set_service_status, setServiceStatus(STATUS) ).
dispatch( led_ctrl, ledCmd(CMD) ).
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ).
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
%====================================================================================
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
context(ctxioport, "127.0.0.1",  "TCP", "8051").
context(ctxdevices, "localhost",  "TCP", "8052").
context(ctxrobot, "127.0.0.1",  "TCP", "8053").
 qactor( sonaradapter, ctxdevices, "it.unibo.sonaradapter.Sonaradapter").
 static(sonaradapter).
  qactor( markerdevice, ctxdevices, "it.unibo.markerdevice.Markerdevice").
 static(markerdevice).
  qactor( cargoservice, ctxcargoservice, "external").
