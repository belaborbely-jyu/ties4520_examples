/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * Interface to reasoning services about data types.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface ReasoningService {	
	/**
	 * Adds another model (as a sub-model) whose contents are to be taken into account when reasoning.
	 * 
	 * @param model dereferenced model
	 */
	public void addModel(SSWAPModel model);
	
	/**
	 * Removes a previously added submodel (via addModel(Model)). The content of the removed model
	 * will no longer be taken into account when reasoning.
	 * 
	 * @param model dereferenced model
	 */
	public void removeModel(SSWAPModel model);
}
