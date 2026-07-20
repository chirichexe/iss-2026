%====================================================================================
% devices description   
%====================================================================================
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
%====================================================================================
context(ctxdevices, "localhost",  "TCP", "8052").
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
 qactor( cargoservice, ctxcargoservice, "external").
  qactor( markerdevice, ctxdevices, "it.unibo.markerdevice.Markerdevice").
 static(markerdevice).
