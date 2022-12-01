/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import static info.sswap.impl.empire.Namespaces.SSWAP_NS;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.Namespaces;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.PDG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Vocabulary;

/**
 * Record describing a Provider in SSWAP. This object is created based on the data read from the underlying semantic web
 * data. In general, if the information is missing from that graph, the corresponding method will return null reference.
 * 
 * @author Blazej Bulka
 */
@Namespaces( { "sswap", SSWAP_NS })
@Entity
@RdfsClass("sswap:Provider")
public abstract class ProviderImpl extends EmpireGeneratedNodeImpl implements SSWAPProvider {
	public ProviderImpl() {		
		addIgnoredType(Vocabulary.SSWAP_PROVIDER.getURI());
	}
	
	@Override
	public void dereference() throws DataAccessException {
		if (getSourceModel() == null) {
			setSourceModel(ImplFactory.get().createEmptySSWAPDataObject(getURI(), PDGImpl.class));
		}

		super.dereference();
	}

	/**
	 * Gets the name of the provider. A valid description of the provider must include the information about the name.
	 * (The invalid graphs that do not provide information about the provider, will cause this method to return null.)
	 * 
	 * @return name of the provider (or null for invalid graphs)
	 */
	@RdfProperty("sswap:name")
	public abstract String getName();

	/**
	 * Sets the name of the provider. A valid name of the provider must not be null.
	 * 
	 * @param name
	 *            the name of the provider (must not be null).
	 */
	public abstract void setName(String name);

	/**
	 * Gets one line description of the provider.
	 * 
	 * @return one line description of the provider (or null if not available).
	 */
	@RdfProperty("sswap:oneLineDescription")
	public abstract String getOneLineDescription();

	/**
	 * Sets one line description of the provider.
	 * 
	 * @param oneLineDescription
	 *            the new one line description of the provider, or null if the current description is to be removed.
	 */
	public abstract void setOneLineDescription(String oneLineDescription);

	/**
	 * Gets the URI pointing to a human-readable description of this provider. This URI should be dereferenceable if not
	 * null.
	 * 
	 * @return URI pointing to a human-readable description, or null if there is no such description for the provider.
	 */
	@RdfProperty(value="sswap:aboutURI",isXsdUri=true)
	public abstract URI getAboutURI();

	/**
	 * Sets the URI pointing to a human-readable description of this provider. This URI should be dereferenceable or
	 * null.
	 * 
	 * @param aboutURI
	 *            URI pointing to a human-readable description of this provider, or null if there is no such description
	 *            for the provider.
	 */
	public abstract void setAboutURI(URI aboutURI);

	/**
	 * Gets the URI containing the machine-readable metadata for this provider. This URI should be dereferenceable or
	 * null.
	 * 
	 * @return URI pointing to a machine-readable metadata for this provider, or null if there is no metadata for this
	 *         provider.
	 */
	@RdfProperty(value="sswap:metadata",isXsdUri=true)
	public abstract URI getMetadata();

	/**
	 * Sets the URI pointing to the machine-readable metadata for this provider. This URI should be dereferenceable or
	 * null.
	 * 
	 * @param metadata
	 *            URI pointing to a machine-readable metadata for this provider, or null if there is no metadata for
	 *            this provider.
	 */
	public abstract void setMetadata(URI metadata);
	
	/**
	 * Adds a resource that is provided by this provider to the list.
	 * 
	 * @param resource
	 *            a SSWAPResource that is provided by this provider. (The object may not be dereferenced, since the only
	 *            information stored in a PDG about the provided resource is the resource's URI).
	 */
	public void addProvidesResource(SSWAPResource resource) {
		if (!(resource instanceof ResourceImpl)) {
			throw new IllegalArgumentException("The resource is created by a different implementation of this API");
		}

		List<ResourceImpl> providesResourceData = getProvidesResourceList();

		// handle properly the case if Empire returns a null list
		if (providesResourceData == null) {
			providesResourceData = new LinkedList<ResourceImpl>();
		}

		// add the new SSWAP resource only if it is not already there
		if (!providesResourceData.contains(resource)) {
			providesResourceData.add((ResourceImpl) resource);
			setProvidesResourceList(providesResourceData);
		}
	}

	/**
	 * Sets the information about all the resources provided by this provider.
	 * 
	 * @param resources
	 *            a set of SSWAPResource that are provided by this provider. (The objects may not be dereferenced, since
	 *            the only information stored in a PDG about the provided resource is the resource's URI).
	 */
	public void setProvidesResource(Collection<SSWAPResource> resources) {
		setProvidesResourceList(toListImpl(resources, SSWAPResource.class, ResourceImpl.class));
	}

	/**
	 * Gets the information about all the resources provided by this provider.
	 * 
	 * @return a collection of SSWAPResource objects, which are not dereferenced (the information about them is stored
	 *         in different documents).
	 */
	public Collection<SSWAPResource> getProvidesResources() {
		List<SSWAPResource> result = new LinkedList<SSWAPResource>();
		
		Collection<ResourceImpl> providesResourceList = getProvidesResourceList();
		
		if (providesResourceList != null) {
			for (ResourceImpl resource : providesResourceList) {
				ImplFactory.initURI(resource);
				
				result.add(resource);
			}
		}
		
		return result;
	}
	

	/**
	 * Empire-generated method that returns a list of implementations for SSWAPResource connected to this PDG by
	 * sswap:providesResource predicate.
	 * 
	 * @return a list of ResourceImpl objects (may be empty or null, if there are no sswap:providesResource predicates
	 *         in this PDG).
	 */
	@RdfProperty("sswap:providesResource")
	public abstract List<ResourceImpl> getProvidesResourceList();

	/**
	 * Empire-generated method for setting a list of implementations of SSWAPResource that will be connected to this PDG
	 * by sswap:providesResource predicate.
	 * 
	 * @param providesResourceData
	 *            a list of ResourceImpl objects
	 */
	public abstract void setProvidesResourceList(List<ResourceImpl> providesResourceData);
	
	/**
	 * Checks whether the following URI (usually a Resource URI) belongs to 
	 * this provider's "security domain"; that is, whether the resource is assumed to be
	 * provided by this provider just because of its URI and not because it is listed explicitly
	 * in the PDG.
	 * 
	 * In order for a URI to belong to the provider's security domain, the URI has to have the same
	 * scheme (protocol) and host as the provider. Moreover, the resource's path on the host must either
	 * have the same directory as the provider, or be located in one of the subdirectories of the provider
	 * (directly or indirectly)
	 * 
	 * @param uri the URI to be checked whether it belongs to the provider's domain
	 * @return true if the Provider belongs to the "security domain" as described above; false otherwise
	 */
	boolean belongsToProvidersDomain(URI uri) {
		URI providerURI = getURI().normalize();
		uri = uri.normalize();

		if (!providerURI.getScheme().equalsIgnoreCase(uri.getScheme())) return false;
		if (!providerURI.getHost().equalsIgnoreCase(uri.getHost())) return false;

		String dir = (new File(uri.getPath())).getParent();
		String providerDir = (new File(providerURI.getPath())).getParent();

		return dir.startsWith(providerDir);
	}
	
	@Override
	public void validate() throws ValidationException {
		super.validate();
		
		if (getName() == null) {
			throw new ValidationException("The provider does not have a defined name (via sswap:name)");
		}
	}
	
	public PDG getPDG() {
		// if this provider object is a part of its PDG then just return that PDG, otherwise, create a separate PDG object
		if ((getSourceModel() != null) && (getSourceModel() instanceof PDG)) {
			return (PDG) getSourceModel();
		}
		
		return SSWAP.getPDG(getURI());		
	}
}
