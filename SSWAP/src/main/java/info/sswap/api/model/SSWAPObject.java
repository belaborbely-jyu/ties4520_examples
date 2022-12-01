/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import info.sswap.ontologies.exec.api.ExecServlet;

import java.util.Collection;

/**
 * Object of SSWAP Graph. Objects in SSWAP represent the results of a service's
 * mapping of the SSWAP Subject to (one or more) SSWAP Objects.
 * <p>
 * To create a new SSWAP Object use {@link SSWAPProtocol#createObject}.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @see SSWAPProtocol
 * @see SSWAPSubject
 * 
 */
public interface SSWAPObject extends SSWAPNode {
	/**
	 * Gets the subject that maps onto this object. In the situation when more than
	 * one subject maps onto this object, only the first subject is returned.
	 * 
	 * @return the subject that maps onto this object or null, if no subject
	 * maps currently onto this object.
	 */
	public SSWAPSubject getSubject();
	
	/**
	 * Gets all the subjects that map onto this object.
	 * 
	 * @return a set of SSWAPSubjects (all of them dereferenced objects).
	 */
	public Collection<SSWAPSubject> getSubjects();
}
