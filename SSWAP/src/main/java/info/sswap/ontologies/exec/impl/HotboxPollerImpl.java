/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

import info.sswap.api.http.HTTPProvider;
import info.sswap.api.model.RDG;
import info.sswap.api.spi.HTTPAPI;
import info.sswap.ontologies.exec.api.HotboxPoller;

/**
 * Implementation of a hotbox poller: the class "polls"--repeatedly checks after
 * a pre-set delay--the contents of a "hotbox" directory. If files, subject to
 * filename filtering, are newer than their associated file in another directory
 * (the "public" or target directory), then a method is run on the files.
 * <p>
 * This poller checks for JSON formated Resource Description Graphs (
 * <code>RDG</code>s); if one is found that is newer than its RDF/XML
 * <code>RDG</code>, it is sent to the HTTP API <code>/makeRDG</code>. On
 * success, this returns a valid <code>RDG</code> which is placed in the target
 * directory.
 * <p>
 * Running the poller with the Exec package allows one to simply drop JSON pre-
 * <code>RDG</code>s into the hotbox directory to stand-up semantic web
 * services.
 * 
 * @author Damian Gessler
 * 
 */
public class HotboxPollerImpl implements HotboxPoller {
	
	private static final Logger LOGGER = LogManager.getLogger(HotboxPollerImpl.class);

	private static File hotboxDir = null;
	private static String hotboxPath = null;
	private static String publicPath = null;
	
	private String fileNameExtensionFilter = ".json";
	private String fileNameExtensionProperty = "info.sswap.exec.api.fileNameExtensionFilter";
	private JSONFilenameFilter filenameFilter;
	
	long sleepMilliSeconds;
	long defaultSleepMilliSeconds = 5 * 1000;
	String sleepSecondsSystemProperty = "info.sswap.exec.api.sleepSeconds";

	DaemonThread daemonThread = null;

	// singleton constructor
	private HotboxPollerImpl() {
		
		try {
			String sleepSecondsStr = System.getProperty(sleepSecondsSystemProperty);
			if ( sleepSecondsStr != null ) {
				Integer sleepSecondsInt = Integer.getInteger(sleepSecondsStr);
				if ( sleepSecondsInt * 1000L > 0 ) {
					defaultSleepMilliSeconds = sleepSecondsInt * 1000L;
				}
			}
		} catch ( Exception e ) {
			; // keep default value
		}
		
		sleepMilliSeconds = defaultSleepMilliSeconds;

		try {
			String fileNameExtensionStr = System.getProperty(fileNameExtensionProperty);
			if ( fileNameExtensionStr != null ) {
				fileNameExtensionFilter = fileNameExtensionStr;
			}
		} catch ( Exception e ) {
			; // keep default value
		}
		
		filenameFilter = new JSONFilenameFilter();
		daemonThread = new DaemonThread();
		start();
		
	}
	
	private static class SingletonHolder {	// pattern re Bill Pugh
	     public static final HotboxPoller INSTANCE = new HotboxPollerImpl();
	   }
	
	public static HotboxPoller getInstance(String hotboxPathStr, String publicPathStr) throws IOException {
		
		if ( hotboxPath == null && hotboxPathStr != null ) {
			
			hotboxDir = new File(hotboxPathStr);
			
			if ( ! hotboxDir.isDirectory() ) {
				LOGGER.error("Hotbox path is not a directory: " + hotboxPathStr);
				hotboxDir = null;
			} else if ( ! hotboxDir.canRead() ) {
				LOGGER.error("Cannot read hotbox directory : " + hotboxPathStr);
				hotboxDir = null;
			} else if ( ! hotboxDir.canWrite() ) {
				LOGGER.error("Cannot write to hotbox directory : " + hotboxPathStr);
				hotboxDir = null;
			}

		} else {
			hotboxPath = null;
			hotboxDir = null;
		}
				
		File publicDir = null;
		
		if ( publicPath == null && publicPathStr != null ) {

			if ( ! publicPathStr.endsWith(File.separator) ) {
				publicPathStr += File.separator;
			}
			
			publicDir = new File(publicPathStr);
			
			if ( ! publicDir.isDirectory() ) {
				LOGGER.error("Exec path (\"RDGPath\" in web.xml) is not a directory: " + publicPathStr);
				publicDir = null;
			} else if ( ! publicDir.canRead() ) {
				LOGGER.error("Cannot read Exec RDG directory (\"RDGPath\" in web.xml): " + publicPathStr);
				publicDir = null;
			} else if ( ! publicDir.canWrite() ) {
				LOGGER.error("Cannot write to Exec RDG directory (\"RDGPath\" in web.xml): " + publicPathStr);
				publicDir = null;
			}
		} else {
			publicPath = null;
		}
		
		if ( publicDir == null ) {
			throw new IOException("Exec requires \"RDGPath\" in web.xml to be defined");
		}
			
		publicPath = publicPathStr;
		
		return SingletonHolder.INSTANCE;
	}
	
	@Override
	public void setInterval(int sleepSeconds) {
		sleepMilliSeconds = sleepSeconds * 1000L > 0 ? sleepSeconds * 1000L : defaultSleepMilliSeconds;
	}
	
	@Override
	public void start() {
					
		if ( isRunning() ) {
			stop();
		}
		
		daemonThread.start();
	}
	
	@Override
	public void stop() {
		
		if ( isRunning() ) {
			daemonThread.interrupt();
		}
	}
	
	@Override
	public boolean isRunning() {
		return daemonThread.isAlive() && ! daemonThread.isInterrupted();
	}
	
	private void updatePublic(File[] jsonFiles) {
		
		for ( File jsonFile : jsonFiles ) {
			
			// get the name of the JSON file
			String jsonName = jsonFile.getName();
			int ndx = jsonName.toLowerCase().lastIndexOf(fileNameExtensionFilter);
			String owlName = jsonName.substring(0, ndx);
			if ( owlName.isEmpty() ) {
				continue;
			}
			
			// construct the associated RDF/XML OWL file; loop if newer
			File owlFile = new File(publicPath + owlName);
			if ( jsonFile.lastModified() < owlFile.lastModified() ) {
				continue;
			}
			
			FileInputStream jsonFileInputStream = null;
			FileOutputStream owlFileOutputStream = null;

			try {				
		
				// make a RDG based on the JSON input
				InputSupplier<FileInputStream> inputSupplier = Files.newInputStreamSupplier(jsonFile);
				jsonFileInputStream = inputSupplier.getInput();
				HTTPProvider httpProvider = HTTPAPI.getProvider();
				HTTPProvider.RDGResponse rdgResponse = httpProvider.makeRDG(jsonFileInputStream);
				
				RDG rdg = rdgResponse.getRDG();
				if ( rdg != null ) { // write the RDF/XML RDG
					
					OutputSupplier<FileOutputStream> outputSupplier = Files.newOutputStreamSupplier(owlFile);
					owlFileOutputStream = outputSupplier.getOutput();
					rdg.serialize(owlFileOutputStream);
				}
			
			} catch ( Exception e ) {
				LOGGER.error("Error transforming JSON to RDF/XML for : " + jsonFile.getAbsolutePath(),e);
			} finally {
				
				if ( jsonFileInputStream != null ) {
					try {
						jsonFileInputStream.close();
					} catch ( IOException e ) {
						;
					}
				}
				
				if ( owlFileOutputStream != null ) {
					try {
						owlFileOutputStream.close();
					} catch ( IOException e ) {
						;
					}
				}
			}
			
		}
	}
	
	private class JSONFilenameFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String fileName) {
			
			boolean acceptFile = false;

			try {
				File file = new File(dir.getCanonicalPath() + File.separator + fileName);
				if ( file.canRead() && file.isFile() ) {
					acceptFile = fileName.toLowerCase().endsWith(fileNameExtensionFilter);
				}
			} catch (IOException e) {
				;
			}
			
			return acceptFile;
		}
		
	}
	
	private class DaemonThread extends Thread {
		
		// constructor
		DaemonThread() {
			this.setDaemon(true);
		}
		
		public synchronized void run() {
			
			if ( hotboxDir == null ) {
				LOGGER.info("Hotbox directory not set; polling not initiated");
			}

			while ( true ) {
				try {
					while ( ! interrupted() ) {
						updatePublic(hotboxDir.listFiles(filenameFilter));
						Thread.sleep(sleepMilliSeconds);
					}
				} catch ( InterruptedException ie ) {
					break;
				}
			}
		}
		
	}

}
