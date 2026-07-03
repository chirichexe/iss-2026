%====================================================================================
% cargosystem description   
%====================================================================================
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
%====================================================================================
% Un solo context: entrambi gli attori nella stessa JVM (sprint0 = prototipo monolitico)
context(ctxcargosystem, "localhost",  "TCP", "8050").
 qactor( cargoservice, ctxcargosystem, "it.unibo.cargoservice.Cargoservice").
 static(cargoservice).
 qactor( ioport, ctxcargosystem, "it.unibo.ioport.Ioport").
 static(ioport).
