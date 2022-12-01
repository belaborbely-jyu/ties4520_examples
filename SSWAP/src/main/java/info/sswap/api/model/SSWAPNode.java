/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;

/**
 * SSWAPNodes are special individuals in SSWAP protocol, and they are handled
 * specially. These individuals include {@link SSWAPProvider},
 * {@link SSWAPResource}, {@link SSWAPGraph}, {@link SSWAPSubject}, and
 * {@link SSWAPObject}.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 * @see SSWAPProvider
 * @see SSWAPResource
 * @see SSWAPGraph
 * @see SSWAPSubject
 * @see SSWAPObject
 */
public interface SSWAPNode extends SSWAPIndividual {
	/**
	 * Gets a unique identifier of this node. For nodes that are not blank nodes, this method returns a regular URI of
	 * this node (as returned by getURI() method). For blank nodes, this method returns a document-wide unique
	 * identifier of this blank node.
	 * 
	 * @return a document-wide unique identifier of this node.
	 */
	public URI getID();

	/**
	 * Checks whether this node is a SSWAPProvider.
	 * 
	 * @return true if this node is a SSWAPProvider
	 */
	public boolean isSSWAPProvider();

	/**
	 * Checks whether this node is a SSWAPResource.
	 * 
	 * @return true if this node is a SSWAPResource
	 */
	public boolean isSSWAPResource();

	/**
	 * Checks whether this node is a SSWAPGraph
	 * 
	 * @return true if this node is a SSWAPGraph
	 */
	public boolean isSSWAPGraph();

	/**
	 * Checks whether this node is a SSWAPSubject
	 * 
	 * @return true if this node is a SSWAPSubject
	 */
	public boolean isSSWAPSubject();

	/**
	 * Checks whether this node is a SSWAPObject
	 * 
	 * @return true if this node is a SSWAPObject
	 */
	public boolean isSSWAPObject();

	/**
	 * Type-safe cast to SSWAPProvider. This is only possible for nodes that are actually SSWAPProviders.
	 * 
	 * @return a SSWAPProvider, if this node is a SSWAPProvider, null otherwise.
	 */
	public SSWAPProvider asSSWAPProvider();

	/**
	 * Type-safe cast to SSWAPResource. This is only possible for nodes that are actually SSWAPResources.
	 * 
	 * @return a SSWAPResource, if this node is a SSWAPResource, null otherwise.
	 */
	public SSWAPResource asSSWAPResource();

	/**
	 * Type-safe cast to SSWAPGraph. This is only possible for nodes that are actually SSWAPGraphs.
	 * 
	 * @return a SSWAPGraph, if this node is a SSWAPGraph, null otherwise.
	 */
	public SSWAPGraph asSSWAPGraph();

	/**
	 * Type-safe cast to SSWAPSubject. This is only possible for nodes that are actually SSWAPSubjects.
	 * 
	 * @return a SSWAPSubject, if this node is a SSWAPSubject, null otherwise.
	 */
	public SSWAPSubject asSSWAPSubject();

	/**
	 * Type-safe cast to SSWAPObject. This is only possible for nodes that are actually SSWAPObjects.
	 * 
	 * @return a SSWAPObject, if this node is a SSWAPObject, null otherwise.
	 */
	public SSWAPObject asSSWAPObject();
}
