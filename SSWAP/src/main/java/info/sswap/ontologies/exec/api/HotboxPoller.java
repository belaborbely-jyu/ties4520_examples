/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.api;

/**
 * A hotbox poller polls (scans) a directory on a recurring interval
 * (<i>e.g.</i> 5 seconds). If it finds a file in the hotbox directory that is
 * younger (more recent) than an associated file in another directory, the
 * poller initiates a process on the hotbox file.
 * <p>
 * In the context of the Exec package, a hotbox directory is polled for JSON
 * files. New files are sent to the HTTP API <a
 * href="http://sswap.info/api/makeRDG">/makeRDG</a> for conversion into
 * <code>RDG</code>s. This allows one to wrap a legcay program as a semantic web
 * service by simply dropping an appropriately parameterized JSON
 * <code>RDG</code> description into the hotbox directory.
 * 
 * @author Damian Gessler
 * 
 */
public interface HotboxPoller {
	
	/**
	 * Start the hotbox directory polling.
	 */
	public void start();
	
	/**
	 * Set the delay interval between directory scans.
	 * 
	 * @param sleepSeconds number of seconds to wait between polling
	 */
	public void setInterval(int sleepSeconds);
	
	/**
	 * Stop the hotbox directory polling.
	 */
	public void stop();
	
	/**
	 * Check if the hotbox poller is running.
	 * 
	 * @return true if running; false if stopped; <i>e.g.</i>, because it was
	 *         never started or because it was stopped (interrupted).
	 */
	public boolean isRunning();
}
