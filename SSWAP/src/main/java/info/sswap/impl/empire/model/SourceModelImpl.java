/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.Config;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.Expressivity;
import info.sswap.api.model.ModelResolver;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.ReasoningService;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDatatype;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPList;
import info.sswap.api.model.SSWAPLiteral;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.io.ClosureModelResolver;
import info.sswap.ontologies.modularity.ModularityModelResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;

import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.clarkparsia.empire.jena.JenaDataSource;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * Implementation of SSWAP model that is directly backed by a Jena model.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
@MappedSuperclass
public abstract class SourceModelImpl extends ModelImpl implements SourceModel {
	/**
	 * The URI used for ontology imports
	 */
	public static final String SSWAP_ONTOLOGY_URI = "http://sswapmeet.sswap.info/sswap/owlOntology";

	/**
	 * The URI for "owl:Ontology" resource
	 */
	private static final String OWL_ONTOLOGY_URI = Namespaces.OWL_NS + "Ontology";

	/**
	 * The URI for "owl:imports" property
	 */
	private static final String OWL_IMPORTS_URI = Namespaces.OWL_NS + "imports";

	/**
	 * The URI for "rdf:type" property
	 */
	private static final String RDF_TYPE_URI = Namespaces.RDF_NS + "type";

	/**
	 * The entity manager that creates Empire objects based on the information from the Jena model.
	 */
	private EntityManager entityManager;

	/**
	 * The actual Jena model
	 */
	private Model model;
	
	/**
	 * The Jena model containing the closure.
	 */
	private Model closureModel;
	
	private SSWAPDocument closedWorldModel;

	/**
	 * A model-specific limit on the number of bytes during closure retrieval.
	 * Value of -1 means that a system-wide limit should be used for this model.
	 */
	private long maxClosureBytes = -1;
	
	/**
	 * A model-specific limit on time (in ms) during closure retrieval.
	 * Value of -1 means that a system-wide limit should be used for this model.
	 */
	private long maxClosureTime = -1;
	
	/**
	 * A model-specific limit on number of threads during closure retrieval.
	 * Value of -1 means that a system-wide limit should be used for this model.
	 */
	private int maxClosureThreads = 3;
	
	/**
	 * Flag whether closure computation is enabled for this source model
	 */
	private boolean closureEnabled;
	
	/**
	 * All other SSWAPModels that are based on the information from the Jena model. (They are considered dependent
	 * objects of this object). This list is mostly maintained for the purpose of refreshing the dependent models should
	 * the data in the underlying Jena model should change (e.g., during a dereference process).
	 */
	private List<ModelImpl> dependentModels;
	
	/**
	 * Mapping of URIs of dependent models to actual models.
	 */
	private Map<URI,List<ModelImpl>> dependentModelsMap;

	/**
	 * The reasoning service (wrapping a PelletReasoner) for this Jena model. This field may be null -- it is lazily
	 * initialized when a ReasoningService is requested for the first time. (The initialization may take some time since
	 * Pellet is performing classification during that time.)
	 */
	private ReasoningServiceImpl reasoningServiceImpl;
	
	private boolean owlDlRequired;

	/**
	 * A cache of all SSWAP type implementations defined in this Jena model.
	 */
	private Map<URI, TypeImpl> typeImpls;
	
	/**
	 * A cache for intersection types created via this source model (to avoid recreating them)
	 * The keys are set of the types in the intersection and the value is the corresponding intersection type
	 */
	private Map<Set<SSWAPType>, SSWAPType> cachedIntersectionTypes;
	
	private Map<URI, PredicateImpl> predicateImpls;
	
	private Map<URI, DatatypeImpl> datatypeImpls;

	private ExpressivityChecker expressivityChecker;

	/*
	 * Used to resolve externally defined terms
	 */
	private final ModelResolver mResolver = Boolean.valueOf(Config.get().getProperty(Config.MODULE_EXTRACTION_ENABLED_KEY)) 
											? new ModularityModelResolver()
											: new ClosureModelResolver();
	
	private boolean valueValidationEnabled;
	
	/**
	 * Initializes an empty, undereferenced SourceModel
	 */
	public SourceModelImpl() {
		setSourceModel(this);
		dependentModels = new LinkedList<ModelImpl>();
		typeImpls = new HashMap<URI, TypeImpl>();
		cachedIntersectionTypes = new HashMap<Set<SSWAPType>, SSWAPType>();
		predicateImpls = new HashMap<URI, PredicateImpl>();
		datatypeImpls = new HashMap<URI,DatatypeImpl>();
		dependentModelsMap = new HashMap<URI,List<ModelImpl>>();
		owlDlRequired = true;
		valueValidationEnabled = true;
		closureEnabled = true;
	}

	/**
	 * @inheritDoc
	 */
	public void addDependentModel(ModelImpl dependentModel) {
		if (dependentModel != this) {
			dependentModels.add(dependentModel);
			
			if (dependentModel.getURI() != null) {
				getDependentModelList(dependentModel.getURI()).add(dependentModel);
			}						
		}
	}
	
	private List<ModelImpl> getDependentModelList(URI uri) {
		List<ModelImpl> result = dependentModelsMap.get(uri);
		
		if (result == null) {
			result = new LinkedList<ModelImpl>();
			dependentModelsMap.put(uri, result);
		}
		
		return result;
	}

	/**
	 * @inheritDoc
	 */
	public void removeDependentModel(ModelImpl dependentModel) {
		dependentModels.remove(dependentModel);
		
		if (dependentModel.getURI() != null) {
			Iterator<ModelImpl> it = getDependentModelList(dependentModel.getURI()).iterator();
			
			while (it.hasNext()) {
				ModelImpl other = it.next();
					
				// intentionally using == (all elements in this list are equal() to each other)
				if (other == dependentModel) {
					it.remove();
					break;
				}
			}
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isValueValidationEnabled() {
		return valueValidationEnabled;
	}
	
	/**
	 * @inheritDoc
	 */
	public void setValueValidationEnabled(boolean validationEnabled) {
		this.valueValidationEnabled = validationEnabled;
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isClosureEnabled() {
		return closureEnabled;
	}
	
	public void setClosureEnabled(boolean closureEnabled) {
		this.closureEnabled = closureEnabled;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPModel getDependentModel(URI uri) {
		Collection<SSWAPModel> dependentModels = getDependentModels(uri);
		
		if (dependentModels.isEmpty()) {
			return null;
		}
		
		return dependentModels.iterator().next();
	}
	
	public Collection<SSWAPModel> getDependentModels(URI uri) {
		List<ModelImpl> implList = dependentModelsMap.get(uri);
		
		if (implList == null) {
			implList = Collections.emptyList();
		}
		
		Collection<SSWAPModel> list = new LinkedList<SSWAPModel>(implList);
		
		return list;
	}
	
	public <T extends SSWAPModel> T getDependentModel(URI uri, Class<T> clazz) {
		for (SSWAPModel model : getDependentModels(uri)) {
			if (clazz.isAssignableFrom(model.getClass())) {
				return (T) model;
			}
		}
		
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * @inheritDoc
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * @inheritDoc
	 */
	public Model getClosureModel() {
		if (closureModel == null) {
			doClosure();
		}
		
		return closureModel;
	}

	/**
	 * @inheritDoc
	 */
	public Model getClosedWorldModel() {
		if (closedWorldModel == null) {			
			closedWorldModel = SSWAP.createSSWAPDocument();						
			getReasoningService().addModel(closedWorldModel);
		}
		
		return ((SourceModel) closedWorldModel).getModel();
	}
	
	/**
	 * @inheritDoc
	 */
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * @inheritDoc
	 */
	public void setModel(Model model) {
		this.model = model;
	}

	private static int getConnectTimeout() {
		int invocationTimeout;
		
		try {
			invocationTimeout = Integer.parseInt(Config.get().getProperty(Config.CLOSURE_CONNECT_TIMEOUT_KEY, Config.CLOSURE_CONNECT_TIMEOUT_DEFAULT));
		} catch (NumberFormatException e) {
			invocationTimeout = Integer.parseInt(Config.CLOSURE_CONNECT_TIMEOUT_DEFAULT);
		}
		
		return invocationTimeout;
	}
	
	private static int getReadTimeout() {
		int invocationTimeout;
		
		try {
			invocationTimeout = Integer.parseInt(Config.get().getProperty(Config.CLOSURE_READ_TIMEOUT_KEY, Config.CLOSURE_READ_TIMEOUT_DEFAULT));
		} catch (NumberFormatException e) {
			invocationTimeout = Integer.parseInt(Config.CLOSURE_READ_TIMEOUT_DEFAULT);
		}
		
		return invocationTimeout;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void dereference() throws DataAccessException {
		super.dereference();

		URI uri = getURI();

		if (uri == null) {
			throw new IllegalArgumentException("This object does not have an URI, and therefore cannot be dereferenced");
		}

		try {
			URL url = uri.toURL();
			
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(getConnectTimeout());
			connection.setReadTimeout(getReadTimeout());
			InputStream is = connection.getInputStream();			
			
			doDereference(is);
			is.close();
		}
		catch (IOException e) {
			throw new DataAccessException("Unable to read data from: " + uri, e);
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public void dereference(InputStream is) throws DataAccessException {
		if (is == null) {
			throw new NullPointerException("Null InputStream is not allowed in dereference(InputStream)");
		}
		
 		super.dereference(is);
		
		doDereference(is);
	}
	
	/**
	 * Performs actual dereferencing 
	 * 
	 * @param source
	 * @throws DataAccessException if a problem should occur while accessing the underlying data source
	 */
	private void doDereference(Object source) throws DataAccessException {
		boolean valueValidationCopy = isValueValidationEnabled();

		setValueValidationEnabled(false);

		try {
			// if this model has not yet been dereferenced, create an Empire entity manager, populate it with data read
			// in
			// RDF/XML format, and retrieve the underlying Jena model
			if (getEntityManager() == null) {
				EntityManager entityManager = ImplFactory.get().createEntityManager(source, "RDF/XML");
				Model model = ((JenaDataSource) entityManager.getDelegate()).getModel();

				setEntityManager(entityManager);
				setModel(model);
			}

			// perform a refresh for this and all dependent objects (Empire-managed objects will update their
			// information, if the underlying RDF information changes)
			refresh();

			setDereferenced(true);
		}
		finally {
			setValueValidationEnabled(valueValidationCopy);
		}
	}
	
	public void dereference(Model model) {
		if (model == null) {
			throw new NullPointerException("Null model is not allowed in dereference(Model)");
		}
		
		boolean valueValidationCopy = isValueValidationEnabled();

		setValueValidationEnabled(false);

		try {
			super.dereference();

			if (getEntityManager() == null) {
				EntityManager entityManager = ImplFactory.get().createEntityManager(ModelFactory.createDefaultModel());		
				Model entityManagerModel = ((JenaDataSource) entityManager.getDelegate()).getModel();

				entityManagerModel.add(model);

				setEntityManager(entityManager);
				setModel(entityManagerModel);
			}

			// perform a refresh for this and all dependent objects (Empire-managed objects will update their
			// information, if the underlying RDF information changes)
			refresh();

			setDereferenced(true);
		} 
		finally {
			setValueValidationEnabled(valueValidationCopy);
		}
	}
		
	/**
	 * Computes closure for this model.
	 * 
	 * @return the degree of the closure computed.
	 */
	public int doClosure() {
		if (!closureEnabled) {
			closureModel = JenaModelFactory.get().createEmptyModel();
			return 0;
		}
		
		setClosureModel(mResolver.resolveSourceModel(this));
		
		return 0;
	}
	
	private ExpressivityChecker getExpressivityChecker() {
		if (expressivityChecker == null) {
			expressivityChecker = new ExpressivityChecker();
		}
		
		return expressivityChecker;
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean checkProfile(Expressivity expressivity) {
		if (expressivity == null) {
			throw new NullPointerException("Null Expressivity is not allowed in checkProfile(Expressivity)");
		}
		
		Model model = assertModel();
		Model closure = getClosureModel();
		
		try {
			OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
	        OWLOntology ontology = ModelUtils.createOWLOntology(ontologyManager, getURI().toString(), model, closure);
	       	        
	        return getExpressivityChecker().checkProfile(expressivity, ontology);
        }
        catch (OWLOntologyCreationException e) {
	        e.printStackTrace();
	        return false;
        }
	}
	

	/**
	 * Refreshes the information encoded in this SSWAP model (and all dependent models) with the information stored in
	 * the underlying Jena model.
	 */
	@Override
	public void refresh() {
		// a refresh is only possible for dereferenced models (i.e., they have an Entity manager at that time).
		if (getEntityManager() != null) {
			// if this source model contains any Empire-managed information, refresh it
			// getEntityManager().refresh(this);

			super.refresh();

			// perform refresh for all dependent models
			Set<ModelImpl> dependentModelsCopy = new HashSet<ModelImpl>(dependentModels);
			for (ModelImpl dependentModel : dependentModelsCopy) {
				dependentModel.refresh();
			}
		}
	}

	@Override
	public synchronized ReasoningService getReasoningService() {
		if (reasoningServiceImpl == null) {						
			reasoningServiceImpl = new ReasoningServiceImpl(this);
		}

		return reasoningServiceImpl;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPType getType(URI uri) {
		if (uri == null) {
			throw new NullPointerException("Null URI is not allowed in getType(URI)");
		}
		
		TypeImpl result = typeImpls.get(uri);

		if (!uri.isAbsolute()) {
			throw new IllegalArgumentException("Not a valid (absolute) URI: " + uri.toString());
		}
		
		if (result == null) {
			result = new TypeImpl(this, uri);
			typeImpls.put(uri, result);
		}

		return result;
	}
	
	public SSWAPType getIntersectionType(Set<SSWAPType> intersectionComponents) {
		if (intersectionComponents == null) {
			throw new NullPointerException("Null set is not allowed as an argument to getIntersectionType(Set<SSWAPType>)");
		}
		
		SSWAPType result = cachedIntersectionTypes.get(intersectionComponents);
		
		result = TypeImpl.intersectionOf(this, intersectionComponents);
		cachedIntersectionTypes.put(intersectionComponents, result);
		
		return result;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPPredicate getPredicate(URI uri) {
		if (uri == null) {
			throw new NullPointerException("Null URI is not allowed as an argument to getPredicate(URI)");
		}
		
		if (!uri.isAbsolute()) {
			throw new IllegalArgumentException("Not a valid (absolute) URI: " + uri.toString());
		}
		
		PredicateImpl result = predicateImpls.get(uri);

		if (result == null) {
			result = new PredicateImpl(this, uri);
			predicateImpls.put(uri, result);
		}

		return result;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPDatatype getDatatype(URI uri) {				
		if (uri == null) {
			return new DatatypeImpl(this, null);
		}
		
		if (!uri.isAbsolute()) {
			throw new IllegalArgumentException("Not a valid (absolute) URI: " + uri.toString());
		}
		
		DatatypeImpl result = datatypeImpls.get(uri);
		
		if (result == null) {
			result = new DatatypeImpl(this, uri);
			datatypeImpls.put(uri, result);
		}
		
		return result;
	}

	/**
	 * Writes back any changes to the entity manager and underlying model
	 */
	@Override
	public void persist() {
		if ((entityManager != null) && (model != null)) {
			super.persist();

			Collection<ModelImpl> dependentModels = new LinkedList<ModelImpl>(this.dependentModels);
			
			for (ModelImpl dependentModel : dependentModels) {
				dependentModel.persist();
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void serialize(OutputStream os) {
		if (os == null) {
			throw new NullPointerException("Null OutputStream is not allowed as an argument to serialize(OutputStream)");
		}
		
		serialize(os, RDFRepresentation.RDF_XML, false);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void serialize(OutputStream os, RDFRepresentation rdfRepresentation, boolean commentedOutput) {
		if (os == null) {
			throw new NullPointerException("Null OutputStream is not allowed as an argument to serialize(OutputStream, RDFRepresentation, boolean)");
		}
		
		if (rdfRepresentation == null) {
			throw new NullPointerException("Null RDFRepresentation is not allowed as an argument to serialize(OutputStream, RDFRepresentation, boolean)");
		}
		
		persist();

		ModelUtils.serializeModel(model, os, rdfRepresentation, commentedOutput);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void setNsPrefix(String prefix, URI uri) {
		if (uri == null) {
			throw new NullPointerException("Null URI is not allowed as an argument to setNsPrefix(String, URI)");
		}
		
		if (hasJenaModel()) {
			model.setNsPrefix(prefix, uri.toString());
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void removeNsPrefix(String prefix) {
		if (hasJenaModel()) {
			model.removeNsPrefix(prefix);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Map<String, String> getNsPrefixMap() {
		if (hasJenaModel()) {
			return model.getNsPrefixMap();
		}

		return null;
	}

	/**
	 * Gets the main owl:Ontology resource in the model, if there is one. The main owl:Ontology needs to have a specific
	 * URI (http://sswapmeet.sswap.info/sswap/owlImports) and be typed as owl:Ontology.
	 * 
	 * @return the resource for the main ontology or null, if there is no resource matching the criteria
	 */
	private Resource getOwlOntologyResource() {
		Resource result = null;

		if (hasJenaModel()) {
			Resource sswapOntologyResource = model.getResource(SSWAP_ONTOLOGY_URI);
			Property rdfType = model.getProperty(RDF_TYPE_URI);
			Resource owlOntology = model.getResource(OWL_ONTOLOGY_URI);

			StmtIterator it = model.listStatements(sswapOntologyResource, rdfType, owlOntology);

			if (it.hasNext()) {
				Statement statement = it.next();
				result = statement.getSubject();
			}

			it.close();
		}

		return result;
	}

	/**
	 * Creates a correct main owl:Ontology resource in the model (i.e., the one with URI of
	 * http://sswapmeet.sswap.info/sswap/owlImports) and typed as owl:Ontology.
	 * 
	 * @return the created main owl:Ontology resource
	 */
	private Resource createOwlOntologyResource() {
		Property rdfTypeProperty = model.getProperty(RDF_TYPE_URI);
		Resource owlOntology = model.getResource(OWL_ONTOLOGY_URI);

		Resource owlOntologyResource = model.getResource(SSWAP_ONTOLOGY_URI);
		model.add(model.createStatement(owlOntologyResource, rdfTypeProperty, owlOntology));

		return owlOntologyResource;
	}

	/**
	 * @inheritDoc
	 */
	public Collection<String> getImports() {
		List<String> result = new LinkedList<String>();

		if (hasJenaModel()) {
			Property owlImports = model.getProperty(OWL_IMPORTS_URI);

			for (StmtIterator it = model.listStatements(null, owlImports, (RDFNode) null); it.hasNext();) {
				Statement statement = it.next();

				if (statement.getObject().isURIResource()) {
					result.add(((Resource) statement.getObject()).getURI());
				}
			}
		}

		return result;
	}

	/**
	 * @inheritDoc
	 */
	public void addImport(URI uri) {
		if (uri == null) {
			throw new NullPointerException("Null URI is not allowed as an argument to addImport(URI)");
		}
		
		if (!uri.isAbsolute()) {
			throw new IllegalArgumentException("Not a valid (absolute) URI: " + uri.toString());
		}
		
		if (hasJenaModel()) {
			Resource ontologyResource = getOwlOntologyResource();

			if (ontologyResource == null) {
				ontologyResource = createOwlOntologyResource();
			}

			Property owlImports = model.getProperty(OWL_IMPORTS_URI);
			Resource importResource = model.getResource(uri.toString());

			Statement statement = model.createStatement(ontologyResource, owlImports, importResource);
			model.add(statement);

			Property rdfTypeProperty = model.getProperty(RDF_TYPE_URI);
			Resource owlOntology = model.getResource(OWL_ONTOLOGY_URI);

			model.add(model.createStatement(importResource, rdfTypeProperty, owlOntology));
		}
	}

	/**
	 * @inheritDoc
	 */
	public void removeImport(URI uri) {
		if (uri == null) {
			throw new NullPointerException("Null URI is not allowed as an argument to removeImport(URI)");
		}
		
		if (!uri.isAbsolute()) {
			throw new IllegalArgumentException("Not a valid (absolute) URI: " + uri.toString());
		}
		
		if (hasJenaModel()) {
			Resource ontologyResource = getOwlOntologyResource();

			if (ontologyResource == null) {
				ontologyResource = createOwlOntologyResource();
			}

			Property owlImports = model.getProperty(OWL_IMPORTS_URI);
			Resource importResource = model.getResource(uri.toString());

			model.remove(model.createStatement(ontologyResource, owlImports, importResource));
		}
	}

	protected boolean hasJenaModel() {
		return (model != null);
	}
	
	protected boolean isOwlDlRequired() {
		return owlDlRequired;
	}
	
	protected void setOwlDlRequired(boolean owlDlRequired) {
		this.owlDlRequired = owlDlRequired;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void validate() throws ValidationException {
		validate(false /* strict */);
	}
	
	
	public void validate(boolean strict) throws ValidationException {
		super.validate();
		
		try {
			persist();
			refresh();
		
			Model model = assertModel();
				
			((ReasoningServiceImpl) getReasoningService()).validateConsistency();
		}
		catch (InconsistentOntologyException e) {
			throw new ValidationException(e.getMessage());
		}
		
		try {
			OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
	        OWLOntology ontology = ModelUtils.createOWLOntology(ontologyManager, getURI().toString(), model);
	   
	        if (isOwlDlRequired()) {
	        	if (strict) {
	        		Collection<String> violations = getExpressivityChecker().getViolationExplanations(Expressivity.DL, ontology);
	        			        		
	        		if (!violations.isEmpty()) {
	        			throw new ValidationException(violations.iterator().next());
	        		}	        		
	        	}
	        	else {
	        		getExpressivityChecker().validateProfileIgnoringUndefinedEntities(Expressivity.DL, ontology);
	        	}
	        }
        }
        catch (OWLOntologyCreationException e) {
	        e.printStackTrace();
        }
        
        ModelUtils.validateSSWAPVocabulary(model);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPType createAnonymousType() {
		return new TypeImpl(this, /* uri */ null);
	}
	
	public SSWAPType createAnonymousRestrictionType() {
		return new TypeImpl(this, /* uri */ null, true /* preventOWLClass */);
	}


	/**
	 * @inheritDoc
	 */
	public SSWAPIndividual createIndividual() {
		return createIndividual(null);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPIndividual createIndividual(URI uri) {
		URI individualURI = (uri == null) ? URI.create(ModelUtils.generateBNodeId()) : uri;
		
		return ImplFactory.get().createIndividual(this, individualURI);
	}

	public SSWAPList createList() {
		return new ListImpl();
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPLiteral createLiteral(String value) {
		if (value == null) {
			throw new NullPointerException("Null String value is not allowed as an argument to createLiteral(String)");
		}
		
		return ImplFactory.get().createLiteral(value, this);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPLiteral createTypedLiteral(String value, URI datatypeURI) throws IllegalArgumentException {
		if (value == null) {
			throw new NullPointerException("Null String value is not allowed as an argument to createTypedLiteral(String, URI)");
		}
		
		return ImplFactory.get().createLiteral(value, datatypeURI, this);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPType createIntersectionOf(Collection<SSWAPType> types) {
		if (types == null) {
			throw new NullPointerException("Null set of types is not a valid argument to createIntersectionOf(Collection<SSWAPType>)");
		}
		
		return TypeImpl.intersectionOf(this, types);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPType createUnionOf(Collection<SSWAPType> types) {
		if (types == null) {
			throw new NullPointerException("Null set of types is not a valid argument to createUnionOf(Collection<SSWAPType>)");
		}
		
		return TypeImpl.unionOf(this, types);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPDatatype createAnonymousDatatype() {
		return getDatatype(null);
	}
	
	public SSWAPDocument getInferredTBox() {
		Model tboxModel = ((ReasoningServiceImpl) getReasoningService()).extractTBox();
		
		SourceModel result = ImplFactory.get().createEmptySSWAPDataObject(getURI(), SourceModelImpl.class);
		result.dereference(tboxModel);

		return result;
	}
	
	public SSWAPDocument getInferredABox() {
		Model aboxModel = ((ReasoningServiceImpl) getReasoningService()).extractABox();
		
		SourceModel result = ImplFactory.get().createEmptySSWAPDataObject(getURI(), SourceModelImpl.class);
		result.dereference(aboxModel);

		return result;		
	}
	
	public void setMaxClosureBytes(long maxClosureBytes) {
		this.maxClosureBytes = maxClosureBytes;
	}
	
	public long getMaxClosureBytes() {
		return maxClosureBytes;
	}
	
	/**
	 * @inheritDoc
	 */
	public void setMaxClosureTime(long maxClosureTime) {
		this.maxClosureTime = maxClosureTime;
	}
	
	public long getMaxClosureTime() {
		return maxClosureTime;
	}
	
	/**
	 * @inheritDoc
	 */
	public void setMaxClosureThreads(int maxClosureThreads) {
		this.maxClosureThreads = maxClosureThreads;
	}
	
	public long getMaxClosureThreads() {
		return maxClosureThreads;
	}
	
	@Override
	public boolean equals(Object o) {
		// the following is for performance reasons
		if (this == o) {
			return true;
		}

		// the only equivalent objects are ModelImpls or their subclasses
		// (NOTE: instanceof returns false for nulls)
		if (o instanceof SourceModelImpl) {
			return rdfIdEquals((SourceModelImpl) o);			
		}

		return false;
	}

	/**
	 * Overridden hash code method to make sure that the generated hashcodes are consistent with the overriden equals()
	 * method.
	 * 
	 * @return the hashcode.
	 */
	@Override
	public int hashCode() {
		return rdfIdHashCode();
	}
	
	protected void setClosureModel(Model clModel) {
		closureModel = clModel;
	}
	
	protected ModelResolver getModelResolver() {
		return mResolver;
	}
	
	/*
	 * TODO Figure out whether this is really needed
	 */
	protected void resetReasoningService() {
		reasoningServiceImpl = null;
	}	
	
	@Override
	public <T extends SSWAPIndividual> T newIndividual(T sourceIndividual) {
		return newIndividual(sourceIndividual, null /* copyURI */);
	}
	
	@Override
	public <T extends SSWAPIndividual> T newIndividual(T sourceIndividual, URI copyURI) {
		T copy = createCopyObject(sourceIndividual, copyURI);
		
		ImplFactory.get().copy(sourceIndividual, copy, this, Collections.<String>emptyList());
		
		return copy;
	}
	
	protected <T extends SSWAPIndividual> T createCopyObject(T sourceIndividual, URI copyURI) {
		SSWAPIndividual result = createIndividual(copyURI);
		
		if (!sourceIndividual.getClass().isAssignableFrom(result.getClass())) {
			throw new IllegalArgumentException("This document cannot create objects of this type: " + sourceIndividual.getClass().getName());
		}
		
		return (T) result;
	}
}
