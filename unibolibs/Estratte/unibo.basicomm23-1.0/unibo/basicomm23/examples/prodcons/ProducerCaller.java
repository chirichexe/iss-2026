package unibo.basicomm23.examples.prodcons;

import unibo.basicomm23.examples.ActorNaiveCaller;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtilsOrig;


public class ProducerCaller extends ActorNaiveCaller {
    private ProducerLogic prodLogic  ;

    public ProducerCaller(String name, ProducerLogic prodLogic, ProtocolType protocol, String hostAddr, String entry){
        super(name, protocol,   hostAddr,   entry);
        //CommUtils.outmagenta("ProducerCaller " + hostAddr + " entry=" + entry);
        this.prodLogic = prodLogic;
    }

    @Override
    protected void body() throws Exception{

        for( int i=1; i<=3; i++ ) {
            String d         = prodLogic.getDistance(   );
            IApplMessage req = CommUtilsOrig.buildRequest(
                    name, "distance", d, "consumer");
            CommUtilsOrig.outblue(name + " | sends request " + i + " " + connSupport);
            IApplMessage answer = connSupport.request(req);  //raise exception
            CommUtilsOrig.outblue(name + " | answer=" + answer);
            CommUtilsOrig.delay(2000);
        }
    }


}
