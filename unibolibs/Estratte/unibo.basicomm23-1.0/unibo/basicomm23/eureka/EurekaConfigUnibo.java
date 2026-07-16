package unibo.basicomm23.eureka;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Applications;

import unibo.basicomm23.utils.CommUtilsOrig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class EurekaConfigUnibo {
	
	public static void config() {
//		Map<String, String> serviceUrls = new HashMap<>();
//        serviceUrls.put("defaultZone", "http://eureka-server-ip:8761/eureka/");
        
        List<String> myserviceUrls = new ArrayList<String>();
        myserviceUrls.add("http://localhost:8761/eureka/");

        // Imposta le configurazioni di Eureka
        DefaultEurekaClientConfig clientConfig = new DefaultEurekaClientConfig() {
             
//            public Map<String, String> getServiceUrl() {
//                return serviceUrls;
//            }
            @Override
            public List<String>	getEurekaServerServiceUrls(String myZone){
				return myserviceUrls;  
            	
            }
        };

        // Avvia il client Eureka
        DiscoveryManager.getInstance().initComponent(
                new MyDataCenterInstanceConfig(),
                clientConfig
        );

        // Ottieni un riferimento a EurekaClient
        EurekaClient eurekaClient = DiscoveryManager.getInstance().getEurekaClient();

        // Esempio: usa il client per ottenere informazioni
//        InstanceInfo instance = eurekaClient.getNextServerFromEureka("NOME_SERVIZIO", false);
//        System.out.println("Indirizzo servizio: " + instance.getHomePageUrl());
        
        Applications applications = eurekaClient.getApplications();
        /*
		CommUtils.outcyan("		EurekaConfigUnibo eurekaClient=" + eurekaClient + " applications: " 
        + applications.getRegisteredApplications().size());
   		*/
		
	}

//	public static void main(String[] args) {
//		EurekaConfigUnibo.config();
//	}
}
