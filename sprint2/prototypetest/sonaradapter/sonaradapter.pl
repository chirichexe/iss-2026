%====================================================================================
% sonaradapter description   
%====================================================================================
mqttBroker("localhost", "1883", "cargosystem").
event( wall_sonardata, distance(D) ).
dispatch( incoming_sonar, distance(D) ).
%====================================================================================
context(ctxsonaradapter, "localhost",  "TCP", "8052").
context(ctxcargoservice, "127.0.0.1",  "TCP", "8050").
 qactor( cargoservice, ctxcargoservice, "external").
  qactor( sonaradapter, ctxsonaradapter, "it.unibo.sonaradapter.Sonaradapter").
 static(sonaradapter).
