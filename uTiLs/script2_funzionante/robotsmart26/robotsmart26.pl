%====================================================================================
% robotsmart26 description   
%====================================================================================
mqttBroker("localhost", "1883", "robotsmart26in").
request( buildPlan, buildPlan(PX,PY,TX,TY) ). %create plan from (PX,PY) to (TX,TY)
reply( buildPlanDone, buildPlanDone(PLAN) ).  %%for buildPlan
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ). %move from current pos to (TARGETX,TARGETY)
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
dispatch( noplan, noplan(X) ).
dispatch( setplanbuildelay, value(V) ). %parameter = V >= 0
dispatch( partnerstarted, partnerstarted(NAME) ).
request( doplan, doplan(PLAN,STEPTIME) ). %execute PLAN with STEPTIME
reply( doplandone, doplandone(ARG) ).  %%for doplan
reply( doplanfailed, doplanfailed(PLANTODO) ).  %%for doplan
dispatch( nextmove, nextmove(M) ). %autodispatch
dispatch( nomoremove, nomoremove(M) ). %autodispatch
event( alarm, alarm(X) ). %event at application level
request( step, step(TIME) ). %step command for TIME duration
reply( stepdone, stepdone(V) ).  %%for step
reply( stepfailed, stepfailed(DURATION,CAUSE) ).  %%for step
dispatch( move, move(M) ). %MOVE = l|r|a|d|h   mosse aril sincrone ok
dispatch( setrobotstate, setpos(X,Y,D) ). %set robot position to (X,Y) direction D=up|down|left|right
request( setdirection, dir(D) ). %set robot direction to D=up|down|left|right
reply( setdirectiondone, pos(PX,PY) ).  %%for setdirection
dispatch( changedir, changedir(X) ).
request( getrobotstate, getrobotstate(ARG) ). %request robot state ARG unused
reply( robotstate, robotstate(POS,DIR) ). %%for getrobotstate | POS->pos(X,Y) DIR->up|down|left|right
request( getenvmap, getenvmap(X) ). %request environment map as string
reply( envmap, envmap(MAP) ). %%for getenvmap | MAP->string 
request( tuneAtHome, tuneAtHome(X) ). %reposition in home X don't care
reply( tuneDone, tuneDone(X) ).  %%for tuneAtHome
event( vrinfo, vrinfo(SOURCE,INFO) ). %streamed: emesso da WEnv
event( sonardata, sonar(DISTANCE) ). %streamed: emesso dal SONAR di WEnv
event( sonaralarm, distance(DISTANCE) ). %emesso ad uso di enti esterni al sistema
%====================================================================================
context(ctxrobotsmart, "localhost",  "TCP", "8020").
 qactor( robotsmart, ctxrobotsmart, "it.unibo.robotsmart.Robotsmart").
 static(robotsmart).
  qactor( robotmnemo, ctxrobotsmart, "it.unibo.robotmnemo.Robotmnemo").
 static(robotmnemo).
  qactor( planexec, ctxrobotsmart, "it.unibo.planexec.Planexec").
 static(planexec).
