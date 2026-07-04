%====================================================================================
% cargosystem description   
%====================================================================================
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
%====================================================================================
context(ctxcargoservice, "localhost",  "TCP", "8050").
context(ctxioport, "localhost",  "TCP", "8051").
context(ctxrobot, "localhost",  "TCP", "8052").
context(ctxdevices, "localhost",  "TCP", "8053").
 qactor( cargoservice, ctxcargoservice, "it.unibo.cargoservice.Cargoservice").
 static(cargoservice).
  qactor( ioport, ctxioport, "it.unibo.ioport.Ioport").
 static(ioport).
  qactor( cargorobot, ctxrobot, "it.unibo.cargorobot.Cargorobot").
 static(cargorobot).
  qactor( led, ctxdevices, "it.unibo.led.Led").
 static(led).
  qactor( markerdevice, ctxdevices, "it.unibo.markerdevice.Markerdevice").
 static(markerdevice).
  qactor( sonar, ctxdevices, "it.unibo.sonar.Sonar").
 static(sonar).
