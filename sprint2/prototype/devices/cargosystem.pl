%====================================================================================
% cargosystem description   
%====================================================================================
request( load_request, loadRequest(none) ).
reply( load_accepted, loadAccepted(SLOTID) ).  %%for load_request
reply( load_retrylater, loadRetryLater(none) ).  %%for load_request
reply( load_refused, loadRefused(none) ).  %%for load_request
request( get_slot, getSlot(none) ).
reply( slot_reserved, slotReserved(SLOTID) ).  %%for get_slot
reply( hold_full, holdFull(none) ).  %%for get_slot
dispatch( free_slot, freeSlot(SLOTID) ).
event( sonardata, distance(D) ).
dispatch( set_service_status, setServiceStatus(STATUS) ).
dispatch( led_ctrl, ledCmd(CMD) ).
request( robot_move, robotMove(TARGET) ).
reply( robot_done, robotDone(none) ).  %%for robot_move
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
%====================================================================================
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
context(ctxcustomer, "127.0.0.1",  "TCP", "8051").
context(ctxdevices, "localhost",  "TCP", "8052").
context(ctxrobot, "127.0.0.1",  "TCP", "8053").
 qactor( hold, ctxdevices, "it.unibo.hold.Hold").
 static(hold).
  qactor( sonarmock, ctxdevices, "it.unibo.sonarmock.Sonarmock").
 static(sonarmock).
  qactor( markerdevice, ctxdevices, "it.unibo.markerdevice.Markerdevice").
 static(markerdevice).
  qactor( cargoservice, ctxcargoservice, "external").
