%====================================================================================
% cargosystem description   
%====================================================================================
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
%====================================================================================
context(ctxcargoservice, "localhost",  "TCP", "8050").
 qactor( cargoservice, ctxcargoservice, "it.unibo.cargoservice.Cargoservice").
 static(cargoservice).
context(ctxioport, "localhost",  "TCP", "8051").
 qactor( ioport, ctxioport, "it.unibo.ioport.Ioport").
 static(ioport).
context(ctxrobot, "localhost",  "TCP", "8052").
 qactor( cargorobot, ctxrobot, "it.unibo.cargorobot.Cargorobot").
 static(cargorobot).
