%====================================================================================
% markerdevice description   
%====================================================================================
mqttBroker("localhost", "1883", "cargosystem").
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
%====================================================================================
context(ctxmarkerdevice, "localhost",  "TCP", "8054").
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
 qactor( cargoservice, ctxcargoservice, "external").
  qactor( markerdevice, ctxmarkerdevice, "it.unibo.markerdevice.Markerdevice").
 static(markerdevice).
