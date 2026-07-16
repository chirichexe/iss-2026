package unibo.basicomm23.utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
 
import unibo.basicomm23.utils.CommUtils;

public class LogUtils {
//	private static String userDirectory = System.getProperty("user.dir");

	private org.slf4j.Logger logger  ;
	
	public LogUtils(String name) {
		logger = org.slf4j.LoggerFactory.getLogger(name);
	}
	public void append( String msg ) {
		logger.info(msg);
	}
	public void info( String msg ) {
		logger.info(msg);
	}
	
	public void clearlog(String fname) {
	    try (PrintWriter writer = new PrintWriter(fname)) {
	        writer.print("");
	        // Il writer.close() viene chiamato automaticamente alla fine del blocco try
	    } catch (FileNotFoundException e) {
	        CommUtils.outred("Errore: Impossibile trovare o creare il file " + fname);
 	    }
	}
	/*
	public void updateLogfile(String fname, String msg, String dir) {
             try {
                // Costruisce il percorso: userDirectory/dir/fname
                //Path path = Paths.get(userDirectory, dir, fname);
                Path path = Paths.get( userDirectory+"/"+fname );
                //CommUtils.outyellow("updateLogfile path: " + path);
                // Assicura che la directory esista prima di scrivere
                if (Files.notExists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }

                // Append del messaggio con newline
                String entry = msg + "\n";
                Files.write(
                    path, 
                    entry.getBytes(), 
                    StandardOpenOption.CREATE, // Crea il file se non esiste
                    StandardOpenOption.APPEND   // Aggiunge in coda
                );
            } catch (IOException e) {
                CommUtils.outred("Errore durante l'aggiornamento del log: " + e.getMessage());
            }
     }
     */
}
