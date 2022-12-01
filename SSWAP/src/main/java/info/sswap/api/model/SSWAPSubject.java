/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.util.Collection;

/**
 * Subject of a SSWAP Graph. Subjects in SSWAP represent the input data of
 * the service, which are mapped to Objects (results) during the
 * execution of the service.
 * <p>
 * To create a new SSWAP Subject use {@link SSWAPProtocol#createSubject}.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @see SSWAPProtocol
 * @see SSWAPObject
 */
public interface SSWAPSubject extends SSWAPNode {

	/**
	 * Gets the object, onto which this subject is mapped. If the subject is mapped onto more than one object, the first
	 * object is returned.
	 * 
	 * @return the SSWAPObject for this subject (a dereferenced object).
	 */
	public SSWAPObject getObject();

	/**
	 * Gets all the objects, onto which this subject is mapped.
	 * 
	 * @return a collection of SSWAPObjects for this subject (all of them are dereferenced).
	 */
	public Collection<SSWAPObject> getObjects();

	/**
	 * Sets a new SSWAPObject for this SSWAPSubject. If the subject has currently any other mappings, they will be
	 * removed.
	 * 
	 * @param object
	 *            the new SSWAPObject
	 */
	public void setObject(SSWAPObject object);

	/**
	 * Adds a new object for this SSWAPSubject (i.e., existing objects are preserved, and the added object is appended).
	 *  
	 * @param object the object to be added
	 */
	public void addObject(SSWAPObject object);
	
	/**
	 * Maps this subject onto the provided collection
	 * 
	 * @param objects Collection of SSWAPObjects to be associated with (mapped from) this SSWAPSubject
	 */
	public void setObjects(Collection<SSWAPObject> objects);

	/**
	 * Gets the graph to which this subject belongs. (In case it belongs to more than one graph,
	 * it returns the first one.) 
	 * 
	 * @return the graph to which this subject belongs or null, if it does not belong to any graph
	 */
	public SSWAPGraph getGraph();
	
	/**
	 * Gets the graphs to which this subject belongs. 
	 * 
	 * @return a collection of graphs.
	 */
	public Collection<SSWAPGraph> getGraphs();	
}
