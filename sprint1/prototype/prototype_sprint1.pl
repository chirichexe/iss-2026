%====================================================================================
% prototype_sprint1 description   
%====================================================================================
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
event( sonardata, distance(D) ).
dispatch( led_ctrl, ledCmd(CMD) ).
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ).
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
%====================================================================================
context(ctxprototype, "localhost",  "TCP", "8050").
context(ctxrobotsmart, "127.0.0.1",  "TCP", "8020").
 qactor( robotsmart, ctxrobotsmart, "external").
  qactor( cargoservice, ctxprototype, "it.unibo.cargoservice.Cargoservice").
 static(cargoservice).
  qactor( sonarmock, ctxprototype, "it.unibo.sonarmock.Sonarmock").
 static(sonarmock).
  qactor( markerdevice, ctxprototype, "it.unibo.markerdevice.Markerdevice").
 static(markerdevice).
  qactor( ledmock, ctxprototype, "it.unibo.ledmock.Ledmock").
 static(ledmock).
  qactor( ioportmock, ctxprototype, "it.unibo.ioportmock.Ioportmock").
 static(ioportmock).
