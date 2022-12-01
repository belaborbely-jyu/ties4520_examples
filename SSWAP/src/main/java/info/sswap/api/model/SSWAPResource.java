/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;

/**
 * Describes a resource (a service) in SSWAP. Services in SSWAP are described by
 * canonical/protocol graphs (<i>e.g.</i>, {@link RDG}).
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 * @see RDG
 */
public interface SSWAPResource extends SSWAPNode {
	/**
	 * Gets the name of the resource. A valid description of the resource must include the information about the name.
	 * (The invalid graphs that do not provide this information about the resource, will cause this method to return
	 * null.)
	 * 
	 * @return name of the resource (or null for invalid graphs)
	 */
	public String getName();

	/**
	 * Sets the name of the resource. A valid name of the resource must not be null.
	 * 
	 * @param name
	 *            the name of the resource (must not be null).
	 */
	public void setName(String name);

	/**
	 * Gets one line description of the resource.
	 * 
	 * @return one line description of the resource (or null if not available).
	 */
	public String getOneLineDescription();

	/**
	 * Sets one line description of the resource.
	 * 
	 * @param oneLineDescription
	 *            the new one line description of the resource, or null if the current description is to be removed.
	 */
	public void setOneLineDescription(String oneLineDescription);

	/**
	 * Gets the URI pointing to a human-readable description of this resource. This URI should be dereferenceable if not
	 * null.
	 * 
	 * @return URI pointing to a human-readable description, or null if there is no such description for the resource.
	 */
	public URI getAboutURI();

	/**
	 * Sets the URI pointing to a human-readable description of this resource. This URI should be dereferenceable or
	 * null.
	 * 
	 * @param aboutURI
	 *            URI pointing to a human-readable description of this resource, or null if there is no such description
	 *            for the resource.
	 */
	public void setAboutURI(URI aboutURI);

	/**
	 * Gets the URI containing the machine-readable metadata for this resource. This URI should be dereferenceable or
	 * null.
	 * 
	 * @return URI pointing to a machine-readable metadata for this resource, or null if there is no metadata for this
	 *         resource.
	 */
	public URI getMetadata();

	/**
	 * Sets the URI pointing to the machine-readable metadata for this resource. This URI should be dereferenceable or
	 * null.
	 * 
	 * @param metadata
	 *            URI pointing to a machine-readable metadata for this resource, or null if there is no metadata for
	 *            this resource.
	 */
	public void setMetadata(URI metadata);

	/**
	 * Gets the URI pointing to a human-readable user interface for this resource. This URI should be dereferenceable or
	 * null.
	 * 
	 * @return URI pointing to a human-readable user interface for this resource, or null if there is no such interface
	 *         for this resource.
	 */
	public URI getInputURI();

	/**
	 * Sets the URI pointing to a human-readable user interface for this resource. This URI should be dereferencable or
	 * null.
	 * 
	 * @param inputURI
	 *            URI pointing to a human-readable interface for this resource, or null if there is no such interface
	 *            for this resource.
	 */
	public void setInputURI(URI inputURI);

	public URI getOutputURI();

	public void setOutputURI(URI outputURI);

	/**
	 * Gets the object describing the provider of this resource. Since the information about the provider is typically
	 * stored in another graph (PDG), this object is not dereferenced.
	 * 
	 * @return a non-dereferenced SSWAPProvider for this resource.
	 */
	public SSWAPProvider getProvider();

	/**
	 * Sets the provider for this resource. Since the resource descriptions usually do not contain more information than
	 * the URI of the provider, this method can accept either dereferenced or non-dereferenced SSWAPProvider object.
	 * 
	 * @param provider
	 *            a SSWAPProvider object.
	 */
	public void setProvider(SSWAPProvider provider);

	/**
	 * Gets the SSWAP graph of the resource. If the resource has more than one graph, the first graph is
	 * returned.
	 * 
	 * @return the SSWAPGraph object (a dereferenced one, since this information is typically within the same
	 *         document as the resource description).
	 */
	public SSWAPGraph getGraph();

	/**
	 * Sets the SSWAPGraph for this resource. If the resource has more than one graph at the moment, they will be
	 * removed, and only the one set here will remain.
	 * 
	 * @param graph
	 *            the graph to be set for this resource
	 */
	public void setGraph(SSWAPGraph graph);

	/**
	 * Gets all the SSWAP graphs of the resource.
	 * 
	 * @return a collection of the SSWAPGraph objects. All objects are dereferenced one, since this
	 *         information is typically within the same document as the resource description.
	 */
	public Collection<SSWAPGraph> getGraphs();

	/**
	 * Sets the SSWAPGraphs for this resource. If the resource has any other graphs, they will be replaced with the ones
	 * provided here.
	 * 
	 * @param graphs
	 *            a collection of SSWAPGraphs
	 */
	public void setGraphs(Collection<SSWAPGraph> graphs);
		
	/**
	 * Gets the URI of the icon for this resource (if defined).
	 *  
	 * @return the URI of the icon or null
	 */
	public URI getIcon();
	
	/**
	 * Sets the URI of the icon for this resource
	 *  
	 * @param icon the URI of the icon or null
	 */
	public void setIcon(URI icon);
	
	/**
	 * Gets the RDG where this resource is defined.
	 * 
	 * @return the RDG
	 * @throws DataAccessException if it is necessary to retrieve the whole RDG and an error should occur while accessing it
	 */
	public RDG getRDG() throws DataAccessException;
	
	public void validateProvider() throws ValidationException;
}
