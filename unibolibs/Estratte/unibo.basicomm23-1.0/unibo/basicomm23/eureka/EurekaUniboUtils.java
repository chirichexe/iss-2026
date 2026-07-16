package unibo.basicomm23.eureka;
import com.netflix.discovery.EurekaClient;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

 
import unibo.basicomm23.utils.CommUtilsOrig;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.MyDataCenterInfo;

public class EurekaUniboUtils {


	
	private static boolean checkEurekaConnection(String addr) {
        String url = addr.replace("/eureka/", "");
        CommUtilsOrig.outmagenta( "		EurekaUniboUtils | checkEurekaConnection url="+ url  );
       
        for( int i=1;i<=2;i++) {
        try {
            // Creiamo una connessione HTTP all'URL
            URL serverUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) serverUrl.openConnection();

            // Impostiamo il metodo della richiesta (GET in questo caso)
            connection.setRequestMethod("GET");

            // Definiamo un timeout per la connessione e la lettura
            connection.setConnectTimeout(5000);  // Timeout di connessione in millisecondi (5 secondi)
            connection.setReadTimeout(5000);     // Timeout di lettura in millisecondi (5 secondi)

            // Facciamo la richiesta al server e otteniamo il codice di risposta
            int statusCode = connection.getResponseCode();
            //System.out.println("Serverr statusCode: " + statusCode);
            // Verifica se il codice di risposta indica successo (codici 2xx)
            if (statusCode >= 200 && statusCode < 300) {
                System.out.println("Server EUREKA raggiungibile. Risposta: " + statusCode);
                return true;
            } else {
                CommUtilsOrig.outred("Errore nel contattare il server. Codice di risposta: " + statusCode);
                return false;
            }

        }catch (Exception e) {
            // Gestiamo gli errori che possono verificarsi (es. il server non è raggiungibile)
            CommUtilsOrig.outred("EurekaUniboUtils | checkEureka Errore: Impossibile raggiungere il server. i=" + i);
        }
        }
        return false;
	}
	
	
    public static boolean checkEureka( ) {
            //CommUtils.outmagenta( "EurekaUniboUtils | checkEureka"  );
 
            String zoneSpec = System.getenv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE");
            CommUtilsOrig.outmagenta( "		EurekaUniboUtils | checkEurekaConnection  zoneSpec=" + zoneSpec);            
            if( zoneSpec != null )  if ( checkEurekaConnection(zoneSpec) ) return true;

            //PROVO QUANTO DEFINITO IN EurekaServiceConfig
        	com.netflix.discovery.EurekaClientConfig clientConfig = new DefaultEurekaClientConfig();
        	List<String> eurekaUrlDefaulZone = clientConfig.getEurekaServerServiceUrls("defaultZone");
        	CommUtilsOrig.outmagenta( "		EurekaUniboUtils | checkEureka eurekaUrlDefaulZone num="+ eurekaUrlDefaulZone.size() );
        	if( eurekaUrlDefaulZone.size() == 0 ) return false;
        	
        	int eurekaNum =  eurekaUrlDefaulZone.size();
        	for( int i=0; i<eurekaNum; i++) {
        		String addr_i =  clientConfig.getEurekaServerServiceUrls("defaultZone").get(i);
        	    CommUtilsOrig.outblue( "		EurekaUniboUtils | "+addr_i);
        	    if( checkEurekaConnection(addr_i) ) return true;
           	}
        	return false;

    }
    
    
	public static DiscoveryClient createEurekaClient( ) {
		//if( checkEureka() ) {
			CommUtilsOrig.outyellow("		CommUtils | createEurekaClient wirh DefaultEurekaClientConfig ");
	       EurekaInstanceConfig instanceConfig   =  new MyDataCenterInstanceConfig(); //OK Usa il file eureka-client.properties 
	//         EurekaInstanceConfig instanceConfig           =  new DataCenterInstanceConfig();   //OK
	        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(instanceConfig); //DEPRECATED MA OK
	        return new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig() ); // setEurekaZone()

	}    

	public static DiscoveryClient createEurekaClient( EurekaInstanceConfig instanceConfig ) {
        	CommUtilsOrig.outyellow("		CommUtils | createEurekaClient with config");
	        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(instanceConfig); //DEPRECATED MA OK
	        return new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig() ); // setEurekaZone()
	}

	public static DefaultEurekaClientConfig setEurekaZone() {
		 //CommUtils.outmagenta("setEurekaZone .......... " );
		 
        return new DefaultEurekaClientConfig() {
           @Override
           public List<String> getEurekaServerServiceUrls(String myZone) {
           	ArrayList<String> a = new ArrayList<String>();
           	a.add("http://localhost:8761/eureka/");
           	a.add("http://eureka:8761/eureka/");                
           	return a;
           }
       };		
		 
	}
	
	public static InstanceInfo discoverServiceIstance(EurekaClient eurekaClient, String appName) {
		Applications applications = eurekaClient.getApplications();
//		CommUtils.outyellow("		CommUtils | eurekaClient=" + eurekaClient 
//				+ " appName=" + appName
//				+ " Nappls="  + applications.getRegisteredApplications().size()
//				);
        for (Application application : applications.getRegisteredApplications()) {
            CommUtilsOrig.outgreen("		CommUtils | discoverServiceIstance Application Name: " + application.getName() + " N=" + application.getInstances().size());
            if( application.getName().equals(appName )){
            	return application.getInstances().get(0);  //SOLO UNO? JUNE2025
            }   	 
        }
        return null;
	}

	public static String[] discoverService( String appName ) {
		CommUtilsOrig.outyellow("		CommUtils | discoverService no client for " + appName);
		return discoverService( createEurekaClient(), appName );
	}
	
	public static String[] discoverService(EurekaClient eurekaClient, String serviceName) {
		CommUtilsOrig.outyellow("		CommUtils | discoverService with client for " + serviceName);
		InstanceInfo serviceInfo = discoverServiceIstance(eurekaClient, serviceName.toUpperCase());
		CommUtilsOrig.outyellow("		CommUtils | discoverService with client serviceInfo " + serviceInfo);
		if (serviceInfo != null && serviceInfo.getStatus().name().equals("UP") ) { 
			return new String[] {serviceInfo.getHostName(), ""+serviceInfo.getPort()};
		} else {
//			serviceInfo = discoverServiceIstance(eurekaClient, serviceName );
//			CommUtils.outyellow("		CommUtils | discoverService with client serviceInfoooooooooooooooo " + serviceInfo);
//			return  new String[] {serviceInfo.getHostName(), ""+serviceInfo.getPort()};
			return null;
		}	
	}	
	
	public static DiscoveryClient registerTheServiceOnEureka( EurekaInstanceConfig instanceConfig ) {
		//if( checkEureka() ) {
	        CommUtilsOrig.outyellow("		CommUtils | register with EurekaInstanceConfig port=" 
	        		+ instanceConfig.getNonSecurePort()
					+ " host=" + instanceConfig.getHostName(false)
					+ " IP=" + instanceConfig.getIpAddress()
					//+ " INSTANCEID=" + instanceConfig.getInstanceId()
	        		+ " appName=" + instanceConfig.getAppname() );
	        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(instanceConfig);
	        //DefaultEurekaClientConfig contiene le informazioni di configurazione per il client Eureka, 
	        //come eureka.serviceUrl.defaultZone, e altri dettagli per connettersi al server Eureka
	        DefaultEurekaClientConfig clientConfig = new  DefaultEurekaClientConfig();
//	        CommUtils.outyellow("		CommUtils | registerTheServiceOnEureka  | region="+clientConfig.getRegion());
	        DiscoveryClient client = new DiscoveryClient(applicationInfoManager, clientConfig);
	        
	        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
	        CommUtilsOrig.outyellow("		CommUtils | registerTheServiceOnEureka done " 
	        		+  instanceConfig.getAppname() );
	        
	        return client;
	}	

}
