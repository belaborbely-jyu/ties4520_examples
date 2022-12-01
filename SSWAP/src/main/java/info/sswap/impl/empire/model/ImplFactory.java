/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPList;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.clarkparsia.empire.codegen.InstanceGenerator;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.jena.JenaConfig;
import com.clarkparsia.empire.jena.JenaDataSource;
import com.clarkparsia.empire.jena.JenaEmpireModule;
import com.clarkparsia.empire.sesame.OpenRdfEmpireModule;
import com.clarkparsia.empire.util.EmpireUtil;
import com.complexible.common.util.PrefixMapping;
import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Factory class for creating and initializing concrete implementations of SSWAP interfaces.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ImplFactory {
	private static final String GET_TYPES_QUERY = "select distinct ?t where { ??ind <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t . }";
	
	/**
	 * The factory for creating Empire EntityManagers (one manager for each Jena model/data source).
	 */
	private EntityManagerFactory entityManagerFactory;

	/**
	 * Private constructor (following the Singleton design pattern) that initializes Empire and creates Empire
	 * EntityManagerFactory.
	 */
	private ImplFactory() {
		EmpireConfiguration empireConfiguration = new EmpireConfiguration();
		Empire.init(empireConfiguration, new JenaEmpireModule(), new OpenRdfEmpireModule());

		Map<String, Object> factoryMap = new HashMap<String, Object>();
		factoryMap.put(JenaConfig.FACTORY, "jena");

		entityManagerFactory = Empire.get().persistenceProvider().createEntityManagerFactory("", factoryMap);
	}

	/**
	 * The instance singleton instance of this class.
	 */
	private static ImplFactory instance = new ImplFactory();

	/**
	 * The getter method for the singleton instance.
	 * 
	 * @return the instance of the implementation
	 */
	public static ImplFactory get() {
		return instance;
	}

	/**
	 * Creates an empty Empire-generated object. The classes for Empire-generated objects (like ProviderImpl) are
	 * abstract, and therefore cannot be instantiated directly since Empire has to provide the implementations for the
	 * annotated methods. This method wraps the call to Empire to provide such implementation, and instantiates the
	 * object. The object is not initially populated with any data (there is no data source to populate the object). The
	 * object has the URI set.
	 * 
	 * For example, to create an empty object representing a SSWAPProvider, one would call
	 * createEmptySSWAPDataObject(uri, ProviderImpl.class)
	 * 
	 * @param <T>
	 *            the Java class with Empire annotations
	 * @param uri
	 *            the URI that will be assigned to this SSWAPModel
	 * @param clazz
	 *            the class for the implementation (it should contain Empire annotations)
	 * @return a new instance of the requested class or null if it was not possible to instantiate (most likely because
	 *         the class is not properly annotated by empire or has other abstract methods than the ones to be filled by
	 *         Empire)
	 */
	public <T extends ModelImpl> T createEmptySSWAPDataObject(URI uri, Class<T> clazz) throws DataAccessException {
		try {			
			Class<T> instanceClass = InstanceGenerator.generateInstanceClass(clazz);
			
			if (instanceClass == null) {
				throw new DataAccessException("Unable to find concrete implementation to instantiate " + clazz.toString());
			}
			
			T result = instanceClass.newInstance();
			
			if (result == null) {
				throw new DataAccessException("Unable to instantiate the concrete implementation for " + clazz.toString());
			}

			result.setURI(uri);
			
			return result;
		}
		catch (DataAccessException e) {
			throw e;
		}
		catch (Exception e) {
			throw new DataAccessException(e.getMessage());
		}
	}

	/**
	 * Creates a new entity manager for a Jena data source.
	 * 
	 * @param stream
	 *            the place to read the data from. Typically it is an InputStream, but it can be any Empire-supported
	 *            object (e.g., Reader, File, or URL).
	 * @param format
	 *            the string containing the format of the data (e.g., "RDF/XML")
	 * @throws DataAccessException if an error should occur when accessing the data source (e.g., I/O error or malformed data)
	 * @return the entity manager for the given data source
	 */
	public EntityManager createEntityManager(Object stream, String format) throws DataAccessException {
		Map<String, Object> entityManagerMap = new HashMap<String, Object>();
		
		try {
			EntityManager aManager = null;
			
			// if we were passed an input stream, we can try to process it directly with JenaModelFactory
			// (more control over error handling)
			if (stream instanceof InputStream) {
				// read the model (it already removes the bnodes/converts them into our naming scheme
				Model model = JenaModelFactory.get().getModel((InputStream) stream);
				
				// pass the read model through Empire
				entityManagerMap.put(JenaConfig.TYPE, JenaConfig.MODEL);
				entityManagerMap.put(JenaConfig.MODEL, model);
				aManager = entityManagerFactory.createEntityManager(entityManagerMap);
				
				ModelUtils.removeBNodes(model);		
			}
			else {
				// this means we were passed a Reader, File or URL (which are still passed as STREAM to Empire)
				// we let Empire to instantiate a model, read the data etc.
				entityManagerMap.put(JenaConfig.STREAM, stream);
				entityManagerMap.put(JenaConfig.FORMAT, format);
				
				aManager = entityManagerFactory.createEntityManager(entityManagerMap);
				
				// extract the model with the data
				Model model = ((JenaDataSource) aManager.getDelegate()).getModel();
				
				// remove the bnodes/convert them into our naming scheme
				ModelUtils.removeBNodes(model);				
			}

			return aManager;
		}
		catch (Throwable e) {
			// try to determine the root cause for the exception
			Throwable rootCause = getRootCause(e);
			
			// the generic message (will only be used if we cannot determine a more specific message)
			StringBuffer message = new StringBuffer("Problem while trying to create an entity manager for the underlying data source: ");
			
			if (rootCause instanceof SAXException) {
				// SAXExceptions are generated when trying to parse RDF/XML in Jena
				message = new StringBuffer("RDF/XML Syntax Error: ");
								
				// some SAXExceptions contain more specific information about the specific parsing problem
				// (they then belong to a subclass -- SAXParseException)
				if (rootCause instanceof SAXParseException) {
					SAXParseException saxParseException = (SAXParseException) rootCause;
					
					// include information about line number and column in the error message (if this information was provided)
					
					if (saxParseException.getLineNumber() != -1) {
						message.append("line ");
						message.append(saxParseException.getLineNumber());
						message.append(" : ");
					}
					
					if (saxParseException.getColumnNumber() != -1) {
						message.append("column ");
						message.append(saxParseException.getColumnNumber());
						message.append(" : ");
					}
				}
			}
			else if (rootCause instanceof IOException) {
				// the problem was caused by an I/O issue
				message = new StringBuffer("I/O error while reading RDF: ");
			}
			
			// if there was any root cause, try to include it in the top level message
			if (rootCause.getMessage() != null) {
				message.append(rootCause.getMessage());
			}
			
			throw new DataAccessException(message.toString(), e);
		}
	}
	
	/**
	 * Gets the root cause for the exception
	 * 
	 * @param e exception 
	 * @return the root cause
	 */
	private Throwable getRootCause(Throwable e) {
		if ((e.getCause() == null) || (e.getCause() == e)) {
			// this exception is the root cause (the second check prevents an infinite loop, if an exception
			// indicates itself as a cause (I have seen such exceptions)
			// TODO: we are not checking for cycles in general -- only for exceptions indicating themselves as the cause (maybe
			// we should but such exceptions should be rare ..)
			return e;
		} 
		else {
			// if there is an underlying cause, recursively determine the root exception of the lower-level exception
			return getRootCause(e.getCause());
		}		
	}
	
	
	/**
	 * Creates an EntityManager for a specified Jena model
	 * 
	 * @param model
	 *            the Jena model for which the EntityManager should be created
	 * @return the newly created EntityManager
	 */
	public EntityManager createEntityManager(Model model) {
		Map<String, Object> entityManagerMap = new HashMap<String, Object>();

		entityManagerMap.put(JenaConfig.MODEL, model);

		EntityManager aManager = entityManagerFactory.createEntityManager(entityManagerMap);

		return aManager;
	}

	/**
	 * Creates a SourceModel based on an empty Jena model.
	 * 
	 * @param <T>
	 *            the type parameter
	 * @param uri
	 *            the URI of the SourceModel
	 * @param clazz
	 *            the actual implementation class
	 * @return the newly created SourceModel with a properly created empty Jena model and associated EntityManager
	 */
	public <T extends SourceModelImpl> T createEmptySourceModel(URI uri, Class<T> clazz) {
		T result = createEmptySSWAPDataObject(uri, clazz);

		Model model = ModelFactory.createDefaultModel();
		EntityManager entityManager = createEntityManager(model);
		model = ((JenaDataSource) entityManager.getDelegate()).getModel();

		result.setEntityManager(entityManager);
		result.setModel(model);

		return result;
	}

	/**
	 * Creates a dependent object for a SourceModel (e.g., a SSWAPResource for an RDG).
	 * 
	 * @param <T>
	 *            the type parameter for the object to be created
	 * @param sourceModel
	 *            the SourceModel in which the dependent object should be created
	 * @param uri
	 *            the URI of the newly created model
	 * @param clazz
	 *            the actual implementation class
	 * @return the newly created object that is dependent for this SourceModel
	 */
	public <T extends ModelImpl> T createDependentObject(SourceModel sourceModel, URI uri, Class<T> clazz) {
		if (!uri.isAbsolute()) {
			throw new IllegalArgumentException("Not a valid (absolute) URI: " + uri.toString());
		}
		
		if ((sourceModel.getDependentModel(uri) != null)|| existsTyped(sourceModel, uri)) {
			return castDependentModel(sourceModel, uri, clazz);	
		}
		
		T result = ImplFactory.get().createEmptySSWAPDataObject(uri, clazz);

		sourceModel.addDependentModel(result);

		if (sourceModel.getEntityManager() != null) {
			try {
				sourceModel.getEntityManager().persist(result);
			}
			catch (RuntimeException e) {
				// if there is problem persisting, remove it from dependent models (otherwise, we may have a partially initialized
				// dependent model -- a model that does not yet know its source model
				sourceModel.removeDependentModel(result);
				
				throw e;				
			}
		}

		result.setSourceModel(sourceModel);
		
		sourceModel.getEntityManager().merge(result);
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private Collection<URI> getTypes(SourceModel sourceModel, URI individualURI) {
		List<URI> result = new LinkedList<URI>();
		EntityManager em = sourceModel.getEntityManager();
		Query q = em.createQuery(GET_TYPES_QUERY);
		
		q.setParameter("ind", new URIImpl(individualURI.toString()));
		
		List list = q.getResultList();
		
		for (Object listElement : list) {
			BindingSet bs = (BindingSet) listElement;
			URI resultURI = URI.create(((org.openrdf.model.URI) bs.getValue("t")).toString());
			
			result.add(resultURI);
		}
		
		return result;
	}
		
    private boolean isTypedAs(SourceModel sourceModel, URI individualURI, URI typeURI) {
		return getTypes(sourceModel, individualURI).contains(typeURI);		
	}
    
    private boolean existsTyped(SourceModel sourceModel, URI individualURI) {
    	return !getTypes(sourceModel, individualURI).isEmpty();
    }
	
	/**
	 * Checks whether the given individual can be viewed using the specified Empire class. This method
	 * checks @RdfsClass annotation on the class and compares it with with the asserted types in the underlying model.
	 * 
	 * @param <T> Empire class 
	 * @param sourceModel SSWAP Model to which the individual belongs
	 * @param uri URI of individual to check
	 * @param clazz	Class to which to check the annotation RdfsClass.class
	 * @return true if the individual does belong to the class; false otherwise
	 */
	public <T extends ModelImpl> boolean canAs(SourceModel sourceModel, URI uri, Class<T> clazz) {
		RdfsClass rdfsClass = clazz.getAnnotation(RdfsClass.class);
		
		URI typeURI = null; //URI.create(PrefixMapping.GLOBAL.uri(rdfsClass.value()));
		
		return isTypedAs(sourceModel, uri, typeURI);		
	}
	
	public <T extends ModelImpl> T castDependentModel(SourceModel sourceModel, URI uri, Class<T> clazz) {
		if (!uri.isAbsolute()) {
			throw new IllegalArgumentException("Not a valid (absolute) URI: " + uri.toString());
		}		
		
		for (SSWAPModel model : sourceModel.getDependentModels(uri)) {
			if (clazz.isAssignableFrom(model.getClass())) {
				return (T) model;
			}
		}
			
		T result = sourceModel.getEntityManager().find(clazz, uri);
		
		result.setURI(uri);				
		
		sourceModel.addDependentModel(result);
		result.setSourceModel(sourceModel);	
		
		result.refresh();
		
		// persist to make sure the new type triple gets propagated to the model (and other sibling models)
		result.persist();
		
		return result;
	}

	/**
	 * Reads a single Empire-annotated object from the data source managed by the specified entity manager. The returned
	 * object is populated with data. If the data source contains data for multiple objects, only the first object will
	 * be returned. (This method should generally be used for data sources that contain only information about one
	 * object; e.g., like a PDG contains information about only one SSWAPProvider).
	 * 
	 * @param <T>
	 *            Empire-annotated type of the object to be returned by the Empire (the annotation on the object will
	 *            allow Empire to identify which triples in the data source correspond to the object's fields).
	 * 
	 * @param aManager
	 *            the entity manager that manages that data source
	 * @param clazz
	 *            the Empire-annotated class
	 * @return a single Empire-annotated object populated with data, or null, if there is no data matching the class
	 *         annotations.
	 */
	public <T> T readSSWAPDataObject(EntityManager aManager, Class<T> clazz) {
		Collection<T> elements = EmpireUtil.all(aManager, clazz);

		if (!elements.isEmpty()) {
			T result = elements.iterator().next();

			return result;
		}

		return null;
	}
	
	public <T> T readSSWAPDataObject(EntityManager aManager, Class<T> clazz, URI uri) {
		return aManager.find(clazz, uri);
	}
	

	/**
	 * Reads a single Empire-annotated object from a data source (usually a stream). This method supports any
	 * Empire-supported data source: InputStream, Reader, File, or URL. The returned object is populated with data from
	 * the data source. If the data source contains data for multiple objects, only the first object will be returned.
	 * (This method should generally be used for data sources that contain only information about one object; e.g., like
	 * a PDG contains information about only one SSWAPProvider).
	 * 
	 * Since this method creates an underlying Jena model, it can only be used for creating SourceModels (i.e., PDGs and
	 * SSWAP protocol graphs)
	 * 
	 * @param <T>
	 *            Empire-annotated type of the object to be returned by the Empire (the annotation on the object will
	 *            allow Empire to identify which triples in the data source correspond to the object's fields). This
	 *            type has to implement SourceModel to pass the underlying Jena model to the object.
	 * 
	 * @param stream
	 *            the place to read the data from. Typically it is an InputStream, but it can be any Empire-supported
	 *            object (e.g., Reader, File, or URL).
	 * @param format
	 *            the string containing the format of the data (e.g., "RDF/XML")
	 * @param clazz
	 *            the Empire-annotated class
	 * @return a single Empire-annotated object populated with data, or null, if there is no data matching the class
	 *         annotations.
	 */
	public <T extends SourceModel> T readSSWAPDataObject(Object stream, String format, Class<T> clazz) {
		EntityManager aManager = createEntityManager(stream, format);

		Model model = ((JenaDataSource) aManager.getDelegate()).getModel();

		T result = readSSWAPDataObject(aManager, clazz);

		if (result != null) {
			result.setModel(model);
			result.setEntityManager(aManager);
		}

		return result;
	}

	/**
	 * Initializes the URI of a SSWAPModel based on its RDF identifier.
	 * 
	 * @param object
	 *            an object that does not correspond to a blank node in RDF
	 */
	public static void initURI(ModelImpl object) {
		if ((object.getRdfId() != null) && (object.getRdfId().value() instanceof URI)) {
			object.setURI((URI) object.getRdfId().value());
		}
	}

	/**
	 * Creates a SSWAPElement based on an RDF node. The actual subtype of the SSWAPElement will be determined based on
	 * the type/content of the rdf node (e.g., SSWAPIndividual, SSWAPList or just a literal SSWAPElement).
	 * 
	 * @param sourceModel
	 *            the source model for which the SSWAPElement should be created
	 * @param rdfNode
	 *            the RDF node based on which the SSWAPElement will be created
	 * @return the SSWAPElement appropriately cast to an SSWAPIndividual, SSWAPList, just a literal SSWAPElement
	 */
	public SSWAPElement createElement(SourceModel sourceModel, RDFNode rdfNode) {
		if (rdfNode.isLiteral()) {
			return createLiteral((com.hp.hpl.jena.rdf.model.Literal) rdfNode);
		}
		else if (rdfNode.isResource()) {
			Resource resource = (Resource) rdfNode;

			if (resource instanceof RDFList) {
				return createList(sourceModel, (RDFList) resource);
			}
			else {
				return createIndividual(sourceModel, resource);
			}
		}
		else {
			throw new IllegalArgumentException("The RDF Node is neither a literal nor a resource!");
		}
	}

	public Literal createLiteral(String value, SourceModel sourceModel) {
		return createLiteral(value, null /* datatypeURI */, null /* language */, sourceModel);
	}

	public Literal createLiteral(String value, URI datatypeURI, SourceModel sourceModel) throws IllegalArgumentException {
		return createLiteral(value, datatypeURI, null /* language */, sourceModel);
	}

	public Literal createLiteral(String value, String language, SourceModel sourceModel) {
		return createLiteral(value, null /* datatypeURI */, language, sourceModel);
	}

	/**
	 * Creates a literal with the specified value
	 * 
	 * @param value the lexical representation of the value
	 * @param datatypeURI the URI of the XSD datatype (may be null)
	 * @param language the declared language of the datatype (may be null)
	 * @param sourceModel the source the source model where the literal should be created
	 * @return the created literal
	 * @throws IllegalArgumentException if the lexical representation of the value is not valid according to the declared datatypeURI
	 */
	public Literal createLiteral(String value, URI datatypeURI, String language, SourceModel sourceModel) throws IllegalArgumentException {
		Literal result = new Literal(value, datatypeURI, language);

		sourceModel.addDependentModel(result);
		result.setSourceModel(sourceModel);

		return result;
	}

	/**
	 * Creates a SSWAP implementation of a literal based on a Jena literal.
	 * 
	 * @param literal
	 *            the Jena literal
	 * @return the created implementation of a SSWAP literal.
	 */
	public Literal createLiteral(com.hp.hpl.jena.rdf.model.Literal literal) {
		URI datatypeURI = null;

		if (literal.getDatatypeURI() != null) {
			try {
				datatypeURI = new URI(literal.getDatatypeURI());
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("Encountered an invalid datatype URI for a literal: " + e.getInput());
			}
		}

		if ((literal.getLanguage() == null) || ("".equals(literal.getLanguage()))) {
			return new Literal(literal.getString(), datatypeURI, null);	
		}
		
		return new Literal(literal.getString(), datatypeURI, literal.getLanguage());
	}

	/**
	 * Creates a SSWAPList implementation for an RDFList
	 * 
	 * @param sourceModel
	 *            the source model where the SSWAPList should be created
	 * @param list
	 *            the RDFList that should be represented as a SSWAPList
	 * @return the created implementation of the SSWAPList
	 */
	public ListImpl createList(SourceModel sourceModel, RDFList list) {
		List<SSWAPElement> elements = new LinkedList<SSWAPElement>();

		for (RDFNode listNode : list.asJavaList()) {
			elements.add(createElement(sourceModel, listNode));
		}

		return new ListImpl(elements);
	}

	/**
	 * Creates a SSWAPIndividual that for the Jena resource.
	 * 
	 * @param sourceModel
	 *            the source model where the entity should be created
	 * @param resource
	 *            the Jena resource for which the entity should be created
	 * @return the created SSWAPIndividual
	 */
	public IndividualImpl createIndividual(SourceModel sourceModel, Resource resource) {
		try {
			return createIndividual(sourceModel, new URI(resource.getURI()));
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Jena Resource has an invalid URI: " + e.getInput());
		}
	}

	/**
	 * Creates a generic (non-Empire managed) SSWAP Entity.
	 * 
	 * @param sourceModel
	 *            a source model where the entity should be created
	 * @param uri
	 *            the URI of the entity
	 * @return the SSWAPIndividual
	 */
	public IndividualImpl createIndividual(SourceModel sourceModel, URI uri) {
		IndividualImpl result = null;
		
		// check whether such an entity already exists in the source model
		// (this will cause the entity to be reused, and prevents
		// endless loops when there are cycles in entity references).
		SSWAPModel existingModel = sourceModel.getDependentModel(uri);
		
		if (existingModel != null && existingModel instanceof IndividualImpl) {			
			result = (IndividualImpl) sourceModel.getDependentModel(uri);
		}
			
		if (result == null) {		
			result = new NonEmpireIndividual(uri);

			sourceModel.addDependentModel(result);
			result.setSourceModel(sourceModel);
		}

		return result;
	}

	/**
	 * Creates an implementation of SSWAPProperty with the specified URI
	 * 
	 * @param sourceModel
	 *            the source model for which the SSWAPProperty should be created
	 * @param uri
	 *            the URI of the property
	 * @return the created SSWAPProperty implementation
	 */
	public PropertyImpl createProperty(SourceModel sourceModel, SSWAPIndividual individual, SSWAPElement value, URI uri) {
		Model model = sourceModel.getModel();
		Property property = model.getProperty(uri.toString());

		PropertyImpl result = new PropertyImpl(individual, property);
		result.setSourceModel(sourceModel);
		result.setValue(value);

		return result;
	}

	/**
	 * Creates an RDFNode for a SSWAPElement.
	 * 
	 * @param sourceModel
	 *            the source model where the RDFNode should be created
	 * @param element
	 *            the SSWAPElement which should be reflected as the RDFNode
	 * @return the created RDFNode
	 */
	public RDFNode createRDFNode(SourceModel sourceModel, SSWAPElement element) {
		if (element.isLiteral()) {
			return createLiteral(sourceModel, (Literal) element);
		}
		else if (element.isList()) {
			return createRDFList(sourceModel, element.asList());
		}
		else if (element.isIndividual()) {
			return createResource(sourceModel, element.asIndividual());
		}
		else {
			throw new IllegalArgumentException("Found a SSWAPElement that is neither a literal, a list, nor an entity");
		}
	}

	/**
	 * Creates a Jena literal for a SSWAPLiteral
	 * 
	 * @param sourceModel
	 *            the source model for which the Jena literal should be created
	 * @param literal
	 *            the SSWAP literal
	 * @return the created Jena literal
	 */
	public com.hp.hpl.jena.rdf.model.Literal createLiteral(SourceModel sourceModel, Literal literal) {
		Model model = sourceModel.getModel();

		if (literal.getDatatypeURI() != null) {
			RDFDatatype datatype = new BaseDatatype(literal.getDatatypeURI().toString());
			return model.createTypedLiteral(literal.asString(), datatype);
		}
		else if (literal.getLanguage() != null) {
			return model.createLiteral(literal.asString(), literal.getLanguage());
		}
		else {
			return model.createLiteral(literal.asString());
		}
	}

	/**
	 * Creates an RDFList for a SSWAPList.
	 * 
	 * @param sourceModel
	 *            the source model in which the RDFList should be created
	 * @param list
	 *            the list for which the RDFList should be created
	 * @return the created RDFList
	 */
	public RDFList createRDFList(SourceModel sourceModel, SSWAPList list) {
		Model model = sourceModel.getModel();

		RDFList result = model.createList();

		for (int i = 0; i < list.size(); i++) {
			result.with(createRDFNode(sourceModel, list.get(i)));
		}

		return result;
	}

	/**
	 * Creates a corresponding Jena Resource for the SSWAPIndividual
	 * 
	 * @param sourceModel
	 *            the source model in which the Jena resource should be created
	 * @param individual
	 *            the entity for which the resource should be created
	 * @return the created Jena resource
	 */
	public Resource createResource(SourceModel sourceModel, SSWAPIndividual individual) {
		Model model = sourceModel.getModel();
		return model.getResource(individual.getURI().toString());
	}
	
	/**
	 * Asserts that the parameter typed via an API interface has the expected implementation.
	 * 
	 * @param <I> the expected implementation type
	 * @param <A> the API interface
	 * @param apiObject the object to be casted
	 * @param implementationClass the expected implementation class
	 * @return apiObject cast to type (I)
	 */
	@SuppressWarnings("unchecked")
    public <I extends ModelImpl, A extends SSWAPModel> I assertImplementation(A apiObject, Class<I> implementationClass) {
		if (!implementationClass.isAssignableFrom(apiObject.getClass())) {
			throw new IllegalArgumentException("This object was not created by this API implementation: " + apiObject.getClass());
		}
		
		return (I) apiObject;
	}
	
	/**
	 * Gets the source model of a dereferenced ModelImpl
	 * 
	 * @param modelImpl
	 *            the implementation of a SSWAPModel that should be dereferenced
	 * @return a source model of the passed model (never null)
	 * @throws IllegalArgumentException
	 *             if the model has not been dereferenced, and therefore does not have a source model
	 */
	public SourceModel assertSourceModel(ModelImpl modelImpl) throws IllegalArgumentException {
		SourceModel result = modelImpl.getSourceModel();

		if (result == null) {
			throw new IllegalArgumentException("The model has not been dereferenced");
		}

		return result;
	}
	
	/**
	 * Perform a deep copy of a SSWAPList into another source model
	 * 
	 * @param newModel the destination model into which the list should be copied
	 * @param originalList the original list (in the source model) that should be copied
	 * @return the new copy of the SSWAPList
	 */
	private SSWAPList deepCopyList(SourceModel newModel, SSWAPList originalList, Set<URI> copiedIndividuals) {
		ListImpl result = new ListImpl();
		
		for (SSWAPElement originalListElement : originalList) {
			result.add(deepCopyElement(newModel, originalListElement, copiedIndividuals));
		}
		
		newModel.addDependentModel(result);
		
		return result;
	}
	
	/**
	 * Perform a deep copy of a SSWAPIndividual into another source model
	 * 
	 * @param newModel the destination model into which the individual should be copied
	 * @param originalIndividual the original individual (in the source model) that should be copied
	 * @return a deep copy of SSWAPIndividual into the newModel
	 */
	private SSWAPIndividual deepCopyIndividual(SourceModel newModel, SSWAPIndividual originalIndividual, Set<URI> copiedIndividuals) {
		IndividualImpl result = createIndividual(newModel, originalIndividual.getURI());
		
		deepCopyIndividual(newModel, originalIndividual, result, copiedIndividuals);
		
		newModel.addDependentModel(result);
		
		return result;
	}
	
	public void deepCopyIndividual(SourceModel newModel, SSWAPIndividual originalIndividual, IndividualImpl dstIndividual, Set<URI> copiedIndividuals) {
		if (copiedIndividuals.contains(originalIndividual.getURI())) {
			return;
		}
		
		copiedIndividuals.add(originalIndividual.getURI());
		
		// types
		for (SSWAPType originalType : originalIndividual.getDeclaredTypes()) {
			dstIndividual.addType(newModel.getType(originalType.getURI()));
		}
		
		// properties 
		for (SSWAPProperty originalProperty : originalIndividual.getProperties()) {
			SSWAPElement newValue = deepCopyElement(newModel, originalProperty.getValue(), copiedIndividuals);
			PropertyImpl newProperty = createProperty(newModel, dstIndividual, newValue, originalProperty.getURI());
			
			dstIndividual.addProperty(newProperty);
		}		
	}
	
	/**
	 * Performs a deep copy of a SSWAPElement from one source model to another
	 * 
	 * @param newModel the new source model to which the element should be copied
	 * @param originalElement the original element (in the source model) to be copied
	 * @return a new SSWAPElement in the newModel that is a copy of originalElement
	 */
	public SSWAPElement deepCopyElement(SourceModel newModel, SSWAPElement originalElement, Set<URI> copiedIndividuals) {
		ElementImpl newElement = null;
		
		// TODO: Maybe we should use visitor design pattern here?
		if (originalElement.isIndividual()) {
			newElement = (ElementImpl) deepCopyIndividual(newModel, (SSWAPIndividual) originalElement, copiedIndividuals);
		}
		else if (originalElement.isList()) {
			newElement = (ElementImpl) deepCopyList(newModel, originalElement.asList(), copiedIndividuals); 
		}
		else if (originalElement.isLiteral()) {
			newElement = new Literal((Literal) originalElement);
			newModel.addDependentModel(newElement);
		}
		else {
			throw new IllegalArgumentException("This element is not an individual, list or literal!");
		}		
		
		return newElement;
	}
	
	public void copy(SSWAPIndividual srcInd, SSWAPIndividual dstInd, SSWAPDocument dstDoc, Collection<String> excludedTypes) {
		Model srcModel = ((SourceModelImpl) srcInd.getDocument()).assertModel();
		Model dstModel = ((SourceModelImpl) dstDoc).assertModel();
		
		for (SSWAPType objectType : srcInd.getDeclaredTypes()) {
			if (!excludedTypes.contains(objectType.getURI().toString())) {
				if (ModelUtils.isBNodeURI(objectType.getURI().toString())) {
					// for anonymous types we need a deep copy ...					
					dstModel.add(ReasoningServiceImpl.extractTypeDefinition(srcModel, srcModel.getResource(objectType.getURI().toString())));
				}

				dstInd.addType(dstDoc.getType(objectType.getURI()));
			}
		}
		
		for (SSWAPProperty property : srcInd.getProperties()) {
			dstInd.clearProperty(property.getPredicate());
		}
		
		for (SSWAPProperty property : srcInd.getProperties()) {
			SSWAPPredicate predicate = dstDoc.getPredicate(property.getURI());
            SSWAPElement objectValue = property.getValue();
            
            SSWAPElement subjectValue = ImplFactory.get().deepCopyElement((SourceModel) dstDoc, objectValue, new HashSet<URI>());
            
            if (subjectValue.isIndividual()) {
            	dstInd.addProperty(predicate, subjectValue.asIndividual());
            }
            else if (subjectValue.isLiteral()) {
            	dstInd.addProperty(predicate, subjectValue.asLiteral());
            }							
		}		
	}
}
