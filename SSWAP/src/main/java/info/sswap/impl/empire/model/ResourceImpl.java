/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import static info.sswap.impl.empire.Namespaces.SSWAP_NS;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.Namespaces;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.clarkparsia.utils.AbstractDataCommand;
import com.clarkparsia.utils.collections.CollectionUtil;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Vocabulary;

/**
 * Implementation of SSWAPResource. This abstract class contains several abstract methods whose implementations will be
 * provided at run-time by Empire.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
@Namespaces( { "sswap", SSWAP_NS })
@Entity
@RdfsClass("sswap:Resource")
public abstract class ResourceImpl extends EmpireGeneratedNodeImpl implements SSWAPResource {
	/**
	 * Set only for translated resources. Contains reference to the untranslated original
	 */
	private ResourceImpl originalResource;
	
	public ResourceImpl() {		
		addIgnoredType(Vocabulary.SSWAP_RESOURCE.getURI());
	}
	
	public ResourceImpl getOriginalResource() {
		return originalResource;
	}
	
	void setOriginalResource(ResourceImpl originalResource) {
		this.originalResource = originalResource;
	}

	/**
	 * Dereferences this SSWAPResource. In case, this resource was created by itself (e.g., by SSWAP.getResource(), and
	 * not by first retrieving a SSWAPProtocol and then the resource), dereferencing will create an RDG behind the
	 * scenes. That RDG will have the Jena model containing the actual RDF data.
	 */
	@Override
	public void dereference() throws DataAccessException {
		if (getSourceModel() == null) {
			setSourceModel(ImplFactory.get().createEmptySSWAPDataObject(getURI(), RDGImpl.class));
		}

		super.dereference();
	}

	/**
	 * @inheritDoc
	 */
	@RdfProperty("sswap:name")
	public abstract String getName();

	/**
	 * @inheritDoc
	 */
	public abstract void setName(String name);

	/**
	 * @inheritDoc
	 */
	@RdfProperty("sswap:oneLineDescription")
	public abstract String getOneLineDescription();

	/**
	 * @inheritDoc
	 */
	public abstract void setOneLineDescription(String oneLineDescription);

	/**
	 * @inheritDoc
	 */
	@RdfProperty(value="sswap:aboutURI",isXsdUri=true)
	public abstract URI getAboutURI();

	/**
	 * @inheritDoc
	 */
	public abstract void setAboutURI(URI aboutURI);

	/**
	 * @inheritDoc
	 */
	@RdfProperty(value="sswap:metadata",isXsdUri=true)
	public abstract URI getMetadata();

	/**
	 * @inheritDoc
	 */
	public abstract void setMetadata(URI metadata);

	/**
	 * @inheritDoc
	 */
	@RdfProperty(value="sswap:inputURI",isXsdUri=true)
	public abstract URI getInputURI();

	/**
	 * @inheritDoc
	 */
	public abstract void setInputURI(URI inputURI);

	/**
	 * @inheritDoc
	 */
	@RdfProperty(value="sswap:outputURI",isXsdUri=true)
	public abstract URI getOutputURI();

	/**
	 * @inheritDoc
	 */
	public abstract void setOutputURI(URI outputURI);

	/**
	 * @inheritDoc
	 */
	@RdfProperty(value="sswap:icon",isXsdUri=true)
	public abstract URI getIcon();
	
	/**
	 * @inheritDoc
	 */
	public abstract void setIcon(URI icon);
	
	/**
	 * @inheritDoc
	 */
	@RdfProperty("sswap:providedBy")
	public abstract ProviderImpl getProvidedBy();

	/**
	 * @inheritDoc
	 */
	public abstract void setProvidedBy(ProviderImpl provider);

	/**
	 * @inheritDoc
	 */
	public SSWAPProvider getProvider() {
		// retrieve the provider implementation, initialize its URI and return typed as an API object
		ProviderImpl provider = getProvidedBy();

		if (provider != null) {
			ImplFactory.initURI(provider);
			
			provider.setSourceModel(getSourceModel());		
		}

		return provider;
	}

	/**
	 * @inheritDoc
	 */
	public void setProvider(SSWAPProvider provider) {
		if (provider instanceof ProviderImpl || (provider == null)) {
			setProvidedBy((ProviderImpl) provider);
		}
		else {
			throw new IllegalArgumentException("The provider object has not been created by this API implementation.");
		}
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPGraph getGraph() {
		GraphImpl result = null;

		List<GraphImpl> graphList = getOperatesOnList();

		if ((graphList != null) && (!graphList.isEmpty())) {
			result = graphList.get(0);

			// properly set the source model
			if (result != null) {
				result.setSourceModel(getSourceModel());
				result.setResource(this);
			}
		}

		return result;
	}

	public void setGraph(SSWAPGraph graph) {		
		if (graph instanceof GraphImpl) {
			detachExistingGraphs();
			
			((GraphImpl) graph).setResource(this);
			List<GraphImpl> graphList = new LinkedList<GraphImpl>();
			graphList.add((GraphImpl) graph);			
			
			setOperatesOnEmpireList(graphList);
		}
		else {
			throw new IllegalArgumentException("The SSWAPGraph object has not been created by this API implementation");
		}
	}

	/**
	 * Gets a list of SSWAPGraph implementations that are all connected to this SSWAPResource by sswap:operatesOn
	 * predicate.
	 * 
	 * @return a list of SSWAPGraph implementations. The list may be either empty or null, if there are no such graphs.
	 */
	@RdfProperty("sswap:operatesOn")
	public abstract List<GraphImpl> getOperatesOnEmpireList();

	public List<GraphImpl> getOperatesOnList() {
		return ensureProperView(getOperatesOnEmpireList(), GraphImpl.class);
	}
	
	/**
	 * Sets a list of SSWAPGraph implementations that will all be connected to this SSWAPResource by sswap:operatesOn
	 * predicate.
	 * 
	 * @param graphImpls
	 *            a list of SSWAPGraph implementations.
	 */
	public abstract void setOperatesOnEmpireList(List<GraphImpl> graphImpls);

	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPGraph> getGraphs() {
		// type conversion from an implementation-typed collection to API-typed collection. Also it sets the source
		// model for the SSWAPGraph objects
		Collection<SSWAPGraph> result = listFromImpl(setSourceModel(getOperatesOnList(), getSourceModel()), SSWAPGraph.class, GraphImpl.class);
		
		CollectionUtil.each(result, new AbstractDataCommand<SSWAPGraph>() {
            public void execute() {
            	((GraphImpl) getData()).setResource(ResourceImpl.this);
            }
		});
		
		return result;
	}

	/**
	 * @inheritDoc
	 */
	public void setGraphs(Collection<SSWAPGraph> graphs) {
		detachExistingGraphs();
		
		CollectionUtil.each(graphs, new AbstractDataCommand<SSWAPGraph>() {
            public void execute() {
            	((GraphImpl) getData()).setResource(ResourceImpl.this);
            }
		});
		
		setOperatesOnEmpireList(toListImpl(graphs, SSWAPGraph.class, GraphImpl.class));
	}
	
	private void detachExistingGraphs() {
		List<GraphImpl> existingGraphs = getOperatesOnList();
		
		if (existingGraphs != null) {
			CollectionUtil.each(existingGraphs, new AbstractDataCommand<GraphImpl>() {
				public void execute() {
					getData().setResource(null);
				}
			});
		}
	}
	
	/**
	 * Checks whether this SSWAPResource is within an RQG (this affects the validation process).
	 * @return true if this resource is within an RQG, false otherwise
	 */
	private boolean isWithinRQG() {
		return (getSourceModel() instanceof RQG);
	}
	
	@Override
	public void validate() throws ValidationException {
		super.validate();
		
		if ((getName() == null) && !isWithinRQG()) {
			throw new ValidationException("There is no sswap:name predicate for sswap:Resource");
		}
		
		Collection<SSWAPGraph> graphs = getGraphs();
		
		if (graphs.isEmpty()) {
			throw new ValidationException("There are no sswap:Graphs defined in this resource");
		}
		
		for (SSWAPGraph graph : graphs) {
			graph.validate();
		}
				
		if ((getProvider() == null) && !isWithinRQG()) {
			throw new ValidationException("There is no sswap:Provider defined that provides this resource (via sswap:providedBy)");
		}		

		// TODO check whether the graphs for this resource are named (normally they are not), and if they are named,
		// the graph's URI should be "under" the resource's path
	}
	
	public void validateProvider() throws ValidationException {
		// validate the provider -- retrieve provider information
		ProviderImpl provider = (ProviderImpl) getProvider();

		if (provider == null) {
			throw new ValidationException("There is no sswap:Provider defined that provides this resource (via sswap:providedBy)");
		}
		
		try {
			provider.dereference();
		}
		catch (DataAccessException e) {
			throw new ValidationException(String.format("The resource %s claims to be provided by %s but it was not possible to access the provider's PDG to verify.", getURI(), provider.getURI()));
		}

		
		// if the resource's URI does not belong to the provider's security "domain"
		// the provider needs to explicitly list this resource in its PDG
		if ((provider != null) && !isWithinRQG() && !provider.belongsToProvidersDomain(getURI())) {		
			boolean vouchedFor = false;
			
			for (SSWAPResource resource : provider.getProvidesResources()) {
				if (resource.getURI().equals(getURI())) {
					vouchedFor = true;
					break;
				}
			}
			
			if (!vouchedFor) {
				throw new ValidationException(String.format("The resource %s claims to be provided by %s but that provider's PDG has no corresponding sswap:providesResource property", getURI(), provider.getURI()));
			}			
		}		
	}
			
	/**
	 * @inheritDoc
	 */
	public RDG getRDG() throws DataAccessException {
		// if this resource is a part of an RDG then we can just return that RDG directly. Otherwise,
		// we have to create another RDG object
		if ((getSourceModel() != null) && (getSourceModel() instanceof RDG)) {
			return (RDG) getSourceModel();
		}
		
		return SSWAP.getRDG(getURI());
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	void setDefaultParameterValues(IndividualImpl rdgIndividual) {
		super.setDefaultParameterValues(rdgIndividual);
		
		if (!(rdgIndividual instanceof SSWAPResource)) {
			throw new IllegalArgumentException("A sswap:Resource can only get default parameters from another sswap:Resource");
		}
		
		// in addition to SSWAPProperties, we also want to set default parameter values for "standard" parts of SSWAPResource (e.g., sswap:name etc.)
		// Since these properties are mapped using Empire, they cannot be changed using SSWAPProperties (handled in the super class)		
		
		SSWAPResource rdgResource = (SSWAPResource) rdgIndividual;
		
		if ((getName() == null) && (rdgResource.getName() != null)) {
			setName(rdgResource.getName());
		}
		
		if ((getAboutURI() == null) && (rdgResource.getAboutURI() != null)) {
			setAboutURI(rdgResource.getAboutURI());
		}
		
		if ((getInputURI() == null) && (rdgResource.getInputURI() != null)) {
			setInputURI(rdgResource.getInputURI());
		}

		if ((getOutputURI() == null) && (rdgResource.getOutputURI() != null)) {
			setOutputURI(rdgResource.getOutputURI());
		}
		
		if ((getIcon() == null) && (rdgResource.getIcon() != null)) {
			setIcon(rdgResource.getIcon());
		}
		
		if ((getMetadata() == null) && (rdgResource.getMetadata() != null)) {
			setMetadata(rdgResource.getMetadata());
		}

		if ((getOneLineDescription() == null) && (rdgResource.getOneLineDescription() != null)) {
			setOneLineDescription(rdgResource.getOneLineDescription());
		}
	}
}
