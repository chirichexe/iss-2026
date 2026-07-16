package unibo.basicomm23.utils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;

import unibo.basicomm23.eureka.EurekaUniboUtils;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.msg.ApplMessageType;
import unibo.basicomm23.msg.ProtocolType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.PrintWriter;

/**
 * Classe di utilità per la gestione delle comunicazioni nel framework BasicComm23.
 * Fornisce metodi statici per:
 * - Gestione dei protocolli di comunicazione (CoAP, MQTT, TCP)
 * - Creazione e manipolazione di messaggi applicativi
 * - Interazione con servizi Eureka per service discovery
 * - Utilità di rete e gestione IP
 * - Output colorato per debugging
 * - Sincronizzazione e gestione thread
 * 
 * @author Unibo BasicComm23
 * @version 2024-2025
 */
public class CommUtils {
	
	/** Parser JSON condiviso per ottimizzare le performance di parsing */
	private static JSONParser simpleparser = new JSONParser();
	
	/** Contatore globale per la numerazione sequenziale dei messaggi */
	private static int msgNum = 0;
 	
	// ================== METODI PER PROTOCOLLI DI COMUNICAZIONE ==================
	
	/**
	 * Verifica se il protocollo configurato è CoAP (Constrained Application Protocol).
	 * @return true se il protocollo corrente è CoAP, false altrimenti
	 */
	public static boolean isCoap() {
		return CommSystemConfig.protcolType == ProtocolType.coap;
	}
	
	/**
	 * Verifica se il protocollo configurato è MQTT (Message Queuing Telemetry Transport).
	 * @return true se il protocollo corrente è MQTT, false altrimenti
	 */
	public static boolean isMqtt() {
		return CommSystemConfig.protcolType == ProtocolType.mqtt;
	}
	
	/**
	 * Verifica se il protocollo configurato è TCP (Transmission Control Protocol).
	 * @return true se il protocollo corrente è TCP, false altrimenti
	 */
	public static boolean isTcp() {
		return CommSystemConfig.protcolType == ProtocolType.tcp;
	}
	
	// ================== GESTIONE E PARSING DEI MESSAGGI ==================
	
	/**
	 * Estrae il contenuto da un messaggio, tentando prima di parsarlo come ApplMessage.
	 * Se il parsing fallisce, restituisce il messaggio originale.
	 * 
	 * @param msg il messaggio da cui estrarre il contenuto
	 * @return il contenuto del messaggio o il messaggio stesso se non parsabile
	 */
	public static String getContent(String msg) {
		String result = "";
		try {
			ApplMessage m = new ApplMessage(msg);
			result = m.msgContent();
		} catch (Exception e) {
			// Se non è un ApplMessage valido, restituisce il messaggio originale
			result = msg;
		}
		return result;	
	}
	
	/**
	 * Converte una stringa in oggetto JSON utilizzando il parser condiviso.
	 * 
	 * @param message la stringa da convertire in JSON
	 * @return l'oggetto JSONObject parsato, o null in caso di errore
	 */
	public static JSONObject parseForJson(String message) {
		try {
			JSONObject jsonObj = (JSONObject) simpleparser.parse(message);
			return jsonObj;
		} catch (Exception e) {
			//outred("ApplMessage | parseForJson error " + e.getMessage() );
			return null; 
		}
	}

	// ================== COSTRUTTORI DI MESSAGGI APPLICATIVI ==================

	/**
	 * Costruisce un messaggio di tipo dispatch (invio asincrono senza risposta attesa).
	 * 
	 * @param sender mittente del messaggio
	 * @param msgId identificatore univoco del messaggio
	 * @param payload contenuto del messaggio
	 * @param dest destinatario del messaggio
	 * @return l'oggetto IApplMessage creato, o null in caso di errore
	 */
	public static IApplMessage buildDispatch(String sender, String msgId, String payload, String dest) {
		try {
			return new ApplMessage(msgId, ApplMessageType.dispatch.toString(), sender, dest, payload, "" + (msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildDispatch ERROR:" + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Costruisce un messaggio di tipo request (richiesta con risposta attesa).
	 * 
	 * @param sender mittente della richiesta
	 * @param msgId identificatore univoco del messaggio
	 * @param payload contenuto della richiesta
	 * @param dest destinatario della richiesta
	 * @return l'oggetto IApplMessage creato, o null in caso di errore
	 */
	public static IApplMessage buildRequest(String sender, String msgId, String payload, String dest) {
		try {
			return new ApplMessage(msgId, ApplMessageType.request.toString(), sender, dest, payload, "" + (msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildRequest ERROR:" + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Costruisce un messaggio di tipo reply (risposta a una richiesta).
	 * 
	 * @param sender mittente della risposta
	 * @param msgId identificatore del messaggio (deve corrispondere alla richiesta)
	 * @param payload contenuto della risposta
	 * @param dest destinatario della risposta (mittente originale della richiesta)
	 * @return l'oggetto IApplMessage creato, o null in caso di errore
	 */
	public static IApplMessage buildReply(String sender, String msgId, String payload, String dest) {
		try {
			return new ApplMessage(msgId, ApplMessageType.reply.toString(), sender, dest, payload, "" + (msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildReply ERROR:" + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Prepara automaticamente una risposta a partire da un messaggio di richiesta.
	 * Inverte mittente e destinatario e mantiene l'ID del messaggio originale.
	 * 
	 * @param requestMsg il messaggio di richiesta a cui rispondere
	 * @param answer il contenuto della risposta
	 * @return il messaggio di risposta preparato, o null se il messaggio non è una richiesta
	 */
	public static IApplMessage prepareReply(IApplMessage requestMsg, String answer) {
		String sender = requestMsg.msgSender();
		String receiver = requestMsg.msgReceiver();
		String reqId = requestMsg.msgId();
		IApplMessage reply = null;
		
		if (requestMsg.isRequest()) { // Controllo difensivo
			// L'ID della risposta deve essere lo stesso della richiesta!
			reply = buildReply(receiver, reqId, answer, sender);
		} else { 
			ColorsOut.outerr("Utils | prepareReply ERROR: message not a request");
		}
		return reply;
	}

	/**
	 * Costruisce un messaggio di tipo event (evento broadcast).
	 * 
	 * @param emitter sorgente dell'evento
	 * @param msgId identificatore univoco dell'evento
	 * @param payload contenuto dell'evento
	 * @return l'oggetto IApplMessage creato, o null in caso di errore
	 */
	public static IApplMessage buildEvent(String emitter, String msgId, String payload) {
		try {
			return new ApplMessage(msgId, ApplMessageType.event.toString(), emitter, "ANY", payload, "" + (msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildEvent ERROR:" + e.getMessage());
			return null;
		}
	}

	// ================== METODI PER OUTPUT COLORATO ==================
	
	/** Stampa un messaggio in giallo */
	public static void outyellow(String msg) { ColorsOut.outappl(msg, ColorsOut.YELLOW); }
	
	/** Stampa un messaggio in verde */
	public static void outgreen(String msg) { ColorsOut.outappl(msg, ColorsOut.GREEN); }
	
	/** Stampa un messaggio in blu */
	public static void outblue(String msg) { ColorsOut.outappl(msg, ColorsOut.BLUE); }
	
	/** Stampa un messaggio in rosso */
	public static void outred(String msg) { ColorsOut.outappl(msg, ColorsOut.RED); }
	
	/** Stampa un messaggio in nero */
	public static void outblack(String msg) { ColorsOut.outappl(msg, ColorsOut.BLACK); }
	
	/** Stampa un messaggio in ciano */
	public static void outcyan(String msg) { ColorsOut.outappl(msg, ColorsOut.CYAN); }
	
	/** Stampa un messaggio in magenta */
	public static void outmagenta(String msg) { ColorsOut.outappl(msg, ColorsOut.MAGENTA); }
	
	/** Stampa un messaggio in grigio */
	public static void outgray(String msg) { ColorsOut.outappl(msg, ColorsOut.GRAY); }

	// ================== UTILITÀ GENERALI ==================

	/**
	 * Introduce un ritardo nell'esecuzione del thread corrente.
	 * 
	 * @param dt durata del ritardo in millisecondi
	 */
	public static void delay(int dt) {
		try {
			Thread.sleep(dt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	} 
	
	/**
	 * Stampa informazioni sui thread attivi per debugging.
	 * Mostra il nome del thread corrente e il numero totale di thread attivi.
	 * 
	 * @param msg messaggio descrittivo da stampare insieme alle info sui thread
	 */
	public static void aboutThreads(String msg) { 
		String tname = Thread.currentThread().getName();
		String nThreads = "" + Thread.activeCount();
		outcyan(msg + " curthread=" + tname + " n=" + nThreads);
	}

	// ================== GESTIONE INTERAZIONI DI RETE ==================

	/**
	 * Inoltra un comando attraverso una connessione di rete esistente.
	 * 
	 * @param conn la connessione su cui inoltrare il comando
	 * @param cmd il comando da inoltrare
	 */
	public static void forwardOnInterconn(Interaction conn, String cmd) {
		try {
			conn.forward(cmd);
		} catch (Exception e) {
			CommUtils.outred("CommUtils | forwardOnInterconn ERROR=" + e.getMessage());
		}
	}
	
	/**
	 * Invia una risposta attraverso una connessione di rete esistente.
	 * 
	 * @param conn la connessione su cui inviare la risposta
	 * @param cmd il comando/risposta da inviare
	 */
	public static void replyOnInterconn(Interaction conn, String cmd) {
		try {
			conn.reply(cmd);
		} catch (Exception e) {
			CommUtils.outred("CommUtils | replyOnInterconn ERROR=" + e.getMessage());
		}
	}
	
	/**
	 * Esegue una richiesta sincrona attraverso una connessione di rete.
	 * 
	 * @param conn la connessione su cui effettuare la richiesta
	 * @param request il contenuto della richiesta
	 * @return la risposta ricevuta, o null in caso di errore
	 */
	public static String requestSynchOnInterconn(Interaction conn, String request) {
		try {
			String answer = conn.request(request);
			return answer;
		} catch (Exception e) {
			CommUtils.outred("CommUtils | requestOnInterconn ERROR=" + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Attende input dall'utente prima di continuare l'esecuzione.
	 * Utile per debugging e controllo manuale del flusso del programma.
	 * 
	 * @param msg messaggio da mostrare all'utente
	 */
	public static void waitTheUser(String msg) {
		try {
			int v = -1;
			while (v == -1) {
			    outblue(msg);
				v = System.in.read();
				delay(2500);
			}
			System.in.read(); // scarta il carattere CR
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Emette un suono di sistema (beep) per notificare eventi.
	 */
	public static void beep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
	}

	// ================== UTILITÀ PER CONVERSIONE STRINGHE ==================

	/**
	 * Converte una stringa per l'invio sostituendo i caratteri newline.
	 * Racchiude la stringa tra apici e sostituisce \n con @!@.
	 * 
	 * @param s la stringa da convertire
	 * @return la stringa convertita pronta per l'invio
	 */
	public static String convertToSend(String s) {
		return "'" + s.replace("\n", "@!@") + "'";
	}
	
	/**
	 * Ripristina una stringa ricevuta dalla sua forma convertita.
	 * Operazione inversa di convertToSend.
	 * 
	 * @param s la stringa convertita da ripristinare
	 * @return la stringa originale con i newline ripristinati
	 */
	public static String restoreFromConvertToSend(String s) {
		return s.replace("@!@", "\n");
	}

	/**
	 * Svuota completamente un file di log.
	 * 
	 * @param fName il nome del file da svuotare
	 */
	public static void clearlog(String fName) {
		try {
			PrintWriter p = new PrintWriter(fName);
			p.write("");
			p.close();
		} catch (Exception e) {
			outred("CommUtils | clearlog ERROR:" + e.getMessage());
		}
	}

	// ================== UTILITÀ PER PROLOG ==================

	/**
	 * Converte una stringa nel formato appropriato per Prolog.
	 * Gestisce stringhe JSON, termini Prolog validi e stringhe generiche.
	 * 
	 * @param s la stringa da convertire
	 * @param on flag per abilitare/disabilitare la conversione
	 * @return la stringa formattata per Prolog
	 */
	public static String toPrologStr(String s, boolean on) {
		try {
			if (!on) {
				// Rimuove gli apici dalla stringa
				return "'" + s.replace("'", "");
			} else {
				// Verifica se è un oggetto JSON valido
				new org.json.simple.parser.JSONParser().parse(s);
				return "'" + s + "'";
			}
		} catch (Exception e) { // Non è JSON
			try {
				// Verifica se è un termine Prolog valido
				alice.tuprolog.Term.createTerm(s);
				return s;
			} catch (Exception e1) { 
				// Stringa generica (ad es. con spazi)
				return "'" + s + "'";
			}
		}					
	}
	
	// ================== UTILITÀ DI RETE ==================

	/**
	 * Ottiene l'indirizzo IP pubblico del sistema corrente.
	 * Utilizza il servizio AWS checkip per determinare l'IP pubblico.
	 * 
	 * @return l'indirizzo IP pubblico come stringa, o null in caso di errore
	 */
	public static String getMyPublicip() {
		try {
			// URL del servizio che restituisce l'indirizzo IP pubblico
			String serviceUrl = "https://checkip.amazonaws.com";

			// Creazione della connessione HTTP
			URL url = new URL(serviceUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			// Lettura della risposta
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();

			// Restituisce l'indirizzo IP pubblico pulito
			String myip = response.toString().trim();
			return myip;
		} catch (Exception e) {
			outred("Errore nell'ottenere l'indirizzo IP: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Determina l'indirizzo IP locale del server nella rete LAN.
	 * Cerca specificamente indirizzi nella sottorete 192.168.x.x.
	 * 
	 * @return l'indirizzo IP locale, o null se non trovato
	 */
	public static String getServerLocalIp() {		
        try {
            Enumeration<NetworkInterface> interfacce = NetworkInterface.getNetworkInterfaces();
            while (interfacce.hasMoreElements()) {
                NetworkInterface interfaccia = interfacce.nextElement();
                Enumeration<InetAddress> indirizzi = interfaccia.getInetAddresses();
                while (indirizzi.hasMoreElements()) {
                    InetAddress indirizzo = indirizzi.nextElement();
                    if (!indirizzo.isLoopbackAddress()) { // Esclude l'indirizzo loopback (127.0.0.1)
                        if (indirizzo.getHostAddress().startsWith("192.168")) {
                        	return indirizzo.getHostAddress();
                        }
                    }
                }
            }
            return null;
        } catch (SocketException e) {
            outred("Errore durante la ricerca degli indirizzi IP: " + e.getMessage());
            return null;
        }			
	}	

	// ================== INTEGRAZIONE CON EUREKA ==================

	/**
	 * Verifica se il server Eureka è disponibile e raggiungibile.
	 * Metodo legacy mantenuto per compatibilità.
	 * 
	 * @return true se Eureka è disponibile, false altrimenti
	 * @deprecated utilizzare checkEureka() invece
	 */
	public static boolean ckeckEureka() {  // LEGACY - mantenuto per compatibilità
		return EurekaUniboUtils.checkEureka();
	}
	
	/**
	 * Verifica se il server Eureka è disponibile e raggiungibile.
	 * 
	 * @return true se Eureka è disponibile, false altrimenti
	 */
	public static boolean checkEureka() {
		return EurekaUniboUtils.checkEureka();
	}

	/**
	 * Crea un client Eureka con configurazione di default.
	 * 
	 * @return il client DiscoveryClient configurato
	 */
	public static DiscoveryClient createEurekaClient() {
		return EurekaUniboUtils.createEurekaClient(); 
 	}
 	
	/**
	 * Crea un client Eureka con configurazione personalizzata.
	 * 
	 * @param config la configurazione dell'istanza Eureka
	 * @return il client DiscoveryClient configurato
	 */
	public static DiscoveryClient createEurekaClient(EurekaInstanceConfig config) {
		return EurekaUniboUtils.createEurekaClient(config);
 	}

	/**
	 * Registra un servizio sul server Eureka per il service discovery.
	 * 
	 * @param config la configurazione del servizio da registrare
	 * @return il client DiscoveryClient per il servizio registrato
	 */
	public static DiscoveryClient registerService(EurekaInstanceConfig config) {
		return EurekaUniboUtils.registerTheServiceOnEureka(config);
 	}
	
	/**
	 * Scopre un servizio registrato su Eureka utilizzando un client esistente.
	 * 
	 * @param eurekaClient il client Eureka da utilizzare per la ricerca
	 * @param serviceName il nome del servizio da cercare
	 * @return array di stringhe contenente le informazioni del servizio trovato
	 */
	public static String[] discoverService(EurekaClient eurekaClient, String serviceName) {
		return EurekaUniboUtils.discoverService(eurekaClient, serviceName);
	}

	/**
	 * Scopre un servizio registrato su Eureka creando automaticamente il client.
	 * 
	 * @param serviceName il nome del servizio da cercare
	 * @return array di stringhe contenente le informazioni del servizio trovato
	 */
	public static String[] discoverService(String serviceName) {
		return EurekaUniboUtils.discoverService(serviceName);
	}
	
	// ================== GESTIONE VARIABILI D'AMBIENTE ==================
	
	/**
	 * Recupera il valore di una variabile d'ambiente del sistema.
	 * 
	 * @param envvarName il nome della variabile d'ambiente
	 * @return il valore della variabile, o null se non esiste
	 */
	public static String getEnvvarValue(String envvarName) {
		return System.getenv(envvarName);
	}

	// ================== CONFIGURAZIONE LOGGING (COMMENTATO) ==================
	
	/*
	 * Metodo per configurare Logstash per l'invio centralizzato dei log.
	 * Attualmente commentato ma mantenuto per future implementazioni.
	 * 
	 * public static void loggerStashConfig(String logstashHost, int logstashPort) {
	 *     // Implementazione commentata per configurazione Logstash
	 * }
	 */
}
