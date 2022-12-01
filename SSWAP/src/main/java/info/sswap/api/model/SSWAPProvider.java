/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;

/**
 * Describes a provider in SSWAP. The data about providers is available in a
 * {@link PDG}. Usually, a service provider such as an institution or a web site
 * will host a single provider URI and multiple services, each service
 * represented by its own {@link RDG}.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @see PDG
 */
public interface SSWAPProvider extends SSWAPNode {
	/**
	 * Gets the name of the provider. A valid description of the provider must include the information about the name.
	 * (The invalid graphs that do not provide information about the provider, will cause this method to return null.)
	 * 
	 * @return name of the provider (or null for invalid graphs)
	 */
	public String getName();

	/**
	 * Sets the name of the provider. A valid name of the provider must not be null.
	 * 
	 * @param name
	 *            the name of the provider (must not be null).
	 */
	public void setName(String name);

	/**
	 * Gets one line description of the provider.
	 * 
	 * @return one line description of the provider (or null if not available).
	 */
	public String getOneLineDescription();

	/**
	 * Sets one line description of the provider.
	 * 
	 * @param oneLineDescription
	 *            the new one line description of the provider, or null if the current description is to be removed.
	 */
	public void setOneLineDescription(String oneLineDescription);

	/**
	 * Gets the URI pointing to a human-readable description of this provider. This URI should be dereferenceable if not
	 * null.
	 * 
	 * @return URI pointing to a human-readable description, or null if there is no such description for the provider.
	 */
	public URI getAboutURI();

	/**
	 * Sets the URI pointing to a human-readable description of this provider. This URI should be dereferenceable or
	 * null.
	 * 
	 * @param aboutURI
	 *            URI pointing to a human-readable description of this provider, or null if there is no such description
	 *            for the provider.
	 */
	public void setAboutURI(URI aboutURI);

	/**
	 * Gets the URI containing the machine-readable metadata for this provider. This URI should be dereferenceable or
	 * null.
	 * 
	 * @return URI pointing to a machine-readable metadata for this provider, or null if there is no metadata for this
	 *         provider.
	 */
	public URI getMetadata();

	/**
	 * Sets the URI pointing to the machine-readable metadata for this provider. This URI should be dereferenceable or
	 * null.
	 * 
	 * @param metadata
	 *            URI pointing to a machine-readable metadata for this provider, or null if there is no metadata for
	 *            this provider.
	 */
	public void setMetadata(URI metadata);
	
	/**
	 * Adds a resource that is provided by this provider to the list.
	 * 
	 * @param resource
	 *            a SSWAPResource that is provided by this provider. (The object may not be dereferenced, since the only
	 *            information stored in a PDG about the provided resource is the resource's URI).
	 */
	public void addProvidesResource(SSWAPResource resource);

	/**
	 * Sets the information about all the resources provided by this provider.
	 * 
	 * @param resources
	 *            a set of SSWAPResource that are provided by this provider. (The objects may not be dereferenced, since
	 *            the only information stored in a PDG about the provided resource is the resource's URI).
	 */
	public void setProvidesResource(Collection<SSWAPResource> resources);

	/**
	 * Gets the information about all the resources provided by this provider.
	 * 
	 * @return a collection of SSWAPResource objects, which are not dereferenced (the information about them is stored
	 *         in different documents).
	 */
	public Collection<SSWAPResource> getProvidesResources();
	
	/**
	 * Gets the PDG that defines this provider
	 * 
	 * @return PDG the PDG to which this Provider belongs
	 */
	public PDG getPDG();
}
