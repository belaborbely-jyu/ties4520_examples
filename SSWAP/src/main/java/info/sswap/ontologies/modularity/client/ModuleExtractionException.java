/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.modularity.client;


/**
 * Thrown when something goes wrong when talking to the ME service
 * 
 * @author Pavel Klinov
 *
 */
public class ModuleExtractionException extends Exception {

	private static final long serialVersionUID = 1565267958243988981L;
	private final Throwable mSource;
	
	public ModuleExtractionException() {
		mSource = null;
	}
	
	public ModuleExtractionException(Throwable e) {
		mSource = e;
	}
}
