%====================================================================================
% devices description   
%====================================================================================
mqttBroker("localhost", "1883", "leddata").
event( wall_sonardata, distance(D) ).
dispatch( incoming_sonar, distance(D) ).
request( mark_container, markContainer(none) ).
reply( marking_done, markingDone(none) ).  %%for mark_container
dispatch( led_ctrl, ledCmd(CMD) ).
event( led_event, ledCmd(CMD) ).
%====================================================================================
context(ctxdevices, "localhost",  "TCP", "8052").
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
 qactor( cargoservice, ctxcargoservice, "external").
  qactor( sonaradapter, ctxdevices, "it.unibo.sonaradapter.Sonaradapter").
 static(sonaradapter).
  qactor( markerdevice, ctxdevices, "it.unibo.markerdevice.Markerdevice").
 static(markerdevice).
  qactor( ledadapter, ctxdevices, "it.unibo.ledadapter.Ledadapter").
 static(ledadapter).
