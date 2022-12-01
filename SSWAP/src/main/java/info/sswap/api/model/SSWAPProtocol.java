/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Common interface for all protocol graphs ({@link RDG}, {@link RIG},
 * {@link RRG}, etc.).
 * <p>
 * For more on the protocol, see <a href="http://sswap.info/protocol">SSWAP
 * Protocol</a>.
 * 
 * @see PDG
 * @see RDG
 * @see RIG
 * @see RRG
 * @see RQG
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface SSWAPProtocol extends SSWAPDocument {

	/**
	 * Gets the SSWAP provider for this canonical graph (or more exactly the SSWAPProvider of the SSWAPResource in the
	 * file).
	 * 
	 * @return the SSWAPProvider object (may not be dereferenced)
	 */
	public SSWAPProvider getProvider();

	/**
	 * Gets the SSWAPResource in the file
	 * 
	 * @return gets the SSWAPResource defined in this file
	 */
	public SSWAPResource getResource();

	/**
	 * Gets a map from SSWAPGraphs to SSWAPSubjects
	 * 
	 * @return the map of SSWAPGraphs to a collection of SSWAPSubjects
	 */
	public Map<SSWAPGraph, Collection<SSWAPSubject>> getMappings();

	/**
	 * Checks whether this graph contains the specific mapping of subjects to objects.
	 * 
	 * @param pattern
	 *            the pattern to be checked
	 * @return true, if the graph contains that pattern
	 */
	public boolean isPattern(MappingPattern pattern);

	/**
	 * Checks whether this graph contains multiple SSWAPGraphs
	 * 
	 * @return true if it contains more than one SSWAPGraph
	 */
	public boolean isMultiGraphs();	

	/**
	 * Creates a graph that will be associated with this SSWAPModel.
	 * <p>
	 * Note: creating a graph does not assign it to the
	 * <code>SSWAPResource</code>; see
	 * {@link SSWAPResource#setGraph(SSWAPGraph)} variant methods to assign a
	 * graph to the resource.
	 * 
	 * @return the new SSWAPGraph
	 */
	public SSWAPGraph createGraph();

	/**
	 * Creates an anonymous object (blank node) subject that will be associated
	 * with this SSWAPModel.
	 * <p>
	 * Note: creating a subject does not assign it to a <code>SSWAPGraph</code>;
	 * see {@link SSWAPGraph#setSubject(SSWAPSubject)} and variant methods to assign a subject to a
	 * graph.
	 * 
	 * @return the new SSWAPSubject
	 */
	public SSWAPSubject createSubject();

	/**
	 * Creates a subject of the resource URI that will be associated with this
	 * SSWAPModel. If <code>uri</code> is <code>null</code> it creates an
	 * anonymous subject (blank node).
	 * <p>
	 * Note: creating a subject does not assign it to a <code>SSWAPGraph</code>;
	 * see {@link SSWAPGraph#setSubject(SSWAPSubject)} and variant methods to assign a subject to a
	 * graph.
	 * 
	 * @param uri
	 *            URI of the resource
	 * 
	 * @return the new SSWAPSubject
	 */
	public SSWAPSubject createSubject(URI uri);

	/**
	 * Creates an anonymous object (blank node) that will be associated with
	 * this SSWAPModel.
	 * <p>
	 * Note: creating an object does not assign it to a
	 * <code>SSWAPSubject</code>; see
	 * {@link SSWAPSubject#addObject(SSWAPObject)} variant methods to assign an
	 * object to a subject.
	 * 
	 * @return the new SSWAPObject
	 */
	public SSWAPObject createObject();

	/**
	 * Creates an object of the resource URI that will be associated with this
	 * SSWAPModel. If <code>uri</code> is <code>null</code> it creates an
	 * anonymous object (blank node).
	 * <p>
	 * Note: creating an object does not assign it to a
	 * <code>SSWAPSubject</code>; see
	 * {@link SSWAPSubject#addObject(SSWAPObject)} variant methods to assign an
	 * object to a subject.
	 * 
	 * @param uri
	 *            the URI of the object
	 * @return the new SSWAPObject
	 */
	public SSWAPObject createObject(URI uri);

}
