/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.util.Collection;

/**
 * A SSWAP Graph of a SSWAP Resource. A Graph is a data structure that allows
 * multiple alternative mappings to be associated with a single {@link SSWAPResource}.
 * Because each SSWAP Graph can itself anchor one:one, one:many, many:one, or many:many
 * subject->object mappings, most cases use only a single SSWAP Graph.
 * <p>
 * To create a new <code>SSWAPGraph</code> use {@link SSWAPProtocol#createGraph}.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @see SSWAPProtocol
 * @see SSWAPResource
 * 
 */
public interface SSWAPGraph extends SSWAPNode {

	/**
	 * Gets the subject of the graph. (If there are more than one subject, the first one is returned).
	 * 
	 * @return a SSWAPSubject for the graph (a dereferenced object, since the information about subjects is always
	 *         within the same document as the graph).
	 */
	public SSWAPSubject getSubject();

	/**
	 * Gets all the subjects of the graph.
	 * 
	 * @return a set of SSWAPSubjects (all of them are dereferenced).
	 */
	public Collection<SSWAPSubject> getSubjects();

	/**
	 * Sets the subject of the graph. If there are any subjects in the graph, they will be overwritten (i.e., the graph
	 * will only have a one subject -- the one passed to this method).
	 * 
	 * @param subject
	 *            the subject
	 */
	public void setSubject(SSWAPSubject subject);

	/**
	 * Sets the subjects of the graph. If there are any subjects in the graph, they will be overwritten.
	 * 
	 * @param subjects
	 *            a collection of subjects for this graph
	 */
	public void setSubjects(Collection<SSWAPSubject> subjects);
	
	/**
	 * Gets the resource to which this graph belongs
	 * @return the SSWAP Resource
	 */
	public SSWAPResource getResource();
}
