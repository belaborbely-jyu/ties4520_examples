/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.io;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Stores the results of closure computation
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class Closure {
	/**
	 * The base model (the one before closure computation)
	 */
	private Model baseModel;
	
	private int degree;
	
	/**
	 * The closure model (the one after closure computation)
	 */
	private Model closureModel;
	
	public Closure(Model baseModel, Model closureModel, int degree) {
		this.baseModel = baseModel;
		this.closureModel = closureModel;
		this.degree = degree;
	}

	/**
	 * Gets the base model (the one before closure computation)
	 * 
     * @return the baseModel
     */
    public Model getBaseModel() {
    	return baseModel;
    }

	/**
	 * The closure model (the one after the closure computation)
	 * 
     * @return the closureModel
     */
    public Model getClosureModel() {
    	return closureModel;
    }
    
    public int getDegree() {
    	return degree;
    }
}
