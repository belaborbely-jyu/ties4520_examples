/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import info.sswap.api.model.Config;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPLiteral;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.Vocabulary;

/**
 * Implementation of a SSWAP individual (objects in SSWAP). The RDF properties of SSWAPIndividuals are generally converted into
 * SSWAPProperties. Yet, there is a group of properties which have special meaning, and their RDF details are hidden
 * from the users of this API (e.g., rdf:type). Usually, the data encoded in such hidden properties is available via a
 * special function in this API (e.g., getTypes()).
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public abstract class IndividualImpl extends ElementImpl implements SSWAPIndividual {
	/**
	 * Constant for rdf:type property
	 */
	private static final String RDF_TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	/**
	 * A set of rdf properties that is ignored (i.e., not visible to the users of this object)
	 */
	private Set<String> ignoredProperties = new HashSet<String>(Arrays.asList(new String[] { RDF_TYPE_URI }));

	/**
	 * The list of all visible SSWAPProperties of this individual. Initialized by readProperties() method.
	 */
	private HashMap<URI, List<SSWAPProperty>> properties;

	/**
	 * The list of all declared (told) types of this individual. (It does not contain any inferred types.) Initialized by
	 * initTypes() method.
	 */
	private Set<SSWAPType> types;
	
	private Set<String> ignoredTypes = new HashSet<String>();
	
	/**
	 * Contains a type that is intersection of all declared types. Initialized lazily by getDeclaredType() and cleared
	 * by addType() and removeType() methods.
	 */
	private SSWAPType declaredTypeIntersection;
	
	/**
	 * Contains a type that is intersection of all inferred types. Initialized lazily by getType() and cleared by addType(), and removeType().
	 * 
	 * TODO: maybe this field should also be cleared on any modification to this individual (?) -- after all the inferred type may also
	 * change based on the properties this individual has (e.g., if these properties have domain/range defined).
	 */
	private SSWAPType inferredTypeIntersection;
	
	/**
	 * Creates an empty individual.
	 */
	public IndividualImpl() {
		properties = new HashMap<URI, List<SSWAPProperty>>();
		types = new HashSet<SSWAPType>();
	}

	/**
	 * Gets a set of properties for this object. (If the object is not dereferenced, it will return an empty set.
	 * 
	 * @return non-null set of properties (may be empty)
	 */
	public Collection<SSWAPProperty> getProperties() {
		List<SSWAPProperty> result = new LinkedList<SSWAPProperty>();

		for (List<SSWAPProperty> propertyForURI : properties.values()) {
			result.addAll(propertyForURI);
		}

		return result;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPProperty getProperty(SSWAPPredicate predicate) {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to getProperty(SSWAPPredicate)");
		}
		
		Collection<SSWAPProperty> propertiesForURI = properties.get(predicate.getURI());
		
		if ((propertiesForURI != null) && (!propertiesForURI.isEmpty())) {
			return propertiesForURI.iterator().next();
		}

		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPProperty> getProperties(SSWAPPredicate predicate) {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to getProperties(SSWAPPredicate)");
		}
		
		Collection<SSWAPProperty> result = properties.get(predicate.getURI());
		
		if (result == null) {
			return new LinkedList<SSWAPProperty>();
		}
		
		return result; 
	}
	
	/**
	 * Gets a list of properties from the properties map that holds all the SSWAPProperties with the given URI. If there
	 * is no such a key in the map, one would be created, and an empty list will be put there.
	 * 
	 * @param name
	 *            the URI of the property
	 * @return a list for the properties with the specified name.
	 */
	private List<SSWAPProperty> getPropertyListForURI(URI name) {
		List<SSWAPProperty> result = properties.get(name);

		if (result == null) {
			result = new LinkedList<SSWAPProperty>();
			properties.put(name, result);
		}

		return result;
	}

	/**
	 * Gets all declared (told) types of this individual. (This method does not return any inferred types.)
	 * 
	 * @return a non-null set of types (may be empty if the current individual does not have any type information).
	 */
	public Collection<SSWAPType> getDeclaredTypes() {
		return Collections.unmodifiableCollection(types);
	}

	/**
	 * @inheritDoc
	 */
	public void addType(SSWAPType type) {
		if (type == null) {
			throw new NullPointerException("Null SSWAPType is not allowed as an argument to addType(SSWAPType)");
		}
		
		doAddType(type);
		persist();
	}

	private void doAddType(SSWAPType type) {
		types.add(type);

		// clear cached copy of intersection type
		declaredTypeIntersection = null;
		inferredTypeIntersection = null;
	}

	
	/**
	 * @inheritDoc
	 */
	public void removeType(SSWAPType type) {
		if (type == null) {
			throw new NullPointerException("Null SSWAPType is not allowed as an argument to removeType(SSWAPType)");
		}
		
		doRemoveType(type);
		
		persist();
	}
	
	private void doRemoveType(SSWAPType type) {
		types.remove(type);

		// clear cached copy of intersection type
		declaredTypeIntersection = null;		
		inferredTypeIntersection = null;	
	}


	/**
	 * @inheritDoc
	 */
	public SSWAPType getDeclaredType() {
		if (declaredTypeIntersection == null) {		
			if ((types != null) && (types.size() == 1)) {
				declaredTypeIntersection = types.iterator().next();
			}
			else if ((types != null) && (types.size() > 1)) {
				declaredTypeIntersection = TypeImpl.intersectionOf(this, types);				
			} 
			else {
				// when there is no type information the intersection is owl:Thing
				// TODO check whether this is still a valid result for *declared* type

				declaredTypeIntersection = assertSourceModel().getType(URI.create(OWL.Thing.getURI()));
			}
		}
		
		return declaredTypeIntersection;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPType getType() {				
		if (inferredTypeIntersection == null) {			
			Set<SSWAPType> types = new HashSet<SSWAPType>(getTypes());			
			
			if ((types != null) && (types.size() == 1)) {
				inferredTypeIntersection = types.iterator().next();
			}
			else if ((types != null) && (types.size() > 1)) {
				inferredTypeIntersection = ((SourceModelImpl) getDocument()).getIntersectionType(types);			
			} 
			else {
				// when there is no type information the intersection is owl:Thing				
				inferredTypeIntersection = assertSourceModel().getType(URI.create(OWL.Thing.getURI()));
			}						
		}

		return inferredTypeIntersection;
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPType> getTypes() {
		persist();
		
		List<SSWAPType> result = new LinkedList<SSWAPType>();
		
		for (String typeURI : ((ReasoningServiceImpl) getReasoningService()).getInferredTypes(this)) {
			TypeImpl type = (TypeImpl) getDocument().getType(URI.create(typeURI));
			
			if (!type.isIntersection()) {
				result.add(type);
			}							
		}
		
		return result;
	}
	
	void addProperty(SSWAPProperty property) {
		verifyReservedPredicate(property.getPredicate());
		
		doAddProperty(property);
		persist();
	}
	
	private void doAddProperty(SSWAPProperty property) {
		if (property instanceof PropertyImpl) {
			PropertyImpl propertyImpl = (PropertyImpl) property;
			getPropertyListForURI(propertyImpl.getURI()).add(propertyImpl);			
		}
		else {
			throw new IllegalArgumentException("The SSWAPProperty has not been created by this API implementation");
		}
	}
	
	void setProperty(SSWAPProperty property) {
		verifyReservedPredicate(property.getPredicate());
		
		detachProperties(getPropertyListForURI(property.getURI()));		
        getPropertyListForURI(property.getURI()).clear();
        
        doAddProperty(property);
        persist();
	}
	
	/**
	 * Removes a property from this individual.
	 * 
	 * @param property
	 *            the property to be removed
	 */
	public void removeProperty(SSWAPProperty property) {
		if (property == null) {
			throw new NullPointerException("Null SSWAPProperty is not allowed as an argument to removeProperty(SSWAPProperty)");
		}

		verifyReservedPredicate(property.getPredicate());
		
		doRemoveProperty(property);
		
		persist();
	}
	
	public void removeProperty(SSWAPPredicate predicate, SSWAPElement value) {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to removeProperty(SSWAPPredicate, SSWAPElement)");
		}

		if (value == null) {
			throw new NullPointerException("Null SSWAPElement is not allowed as an argument to removeProperty(SSWAPPredicate, SSWAPElement)");
		}
		
		verifyReservedPredicate(predicate);
		
		SSWAPProperty existingProperty = null;
		
		for (SSWAPProperty property : getPropertyListForURI(predicate.getURI())) {
			if (value != null) {
				if (value.equals(property.getValue())) {
					existingProperty = property;
					break;
				}
			} 
			else {
				if (property.getValue() == null) {
					existingProperty = property;
					break;
				}
			}
		}
		
		if (existingProperty != null) {
			removeProperty(existingProperty);
		}
	}
	
	private void doRemoveProperty(SSWAPProperty property) {
		if (property instanceof PropertyImpl) {
			PropertyImpl propertyImpl = (PropertyImpl) property;
			getPropertyListForURI(propertyImpl.getURI()).remove(propertyImpl);
		}
		else {
			throw new IllegalArgumentException("The SSWAPProperty has not been created by this API implementation");
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public void clearProperty(URI uri) {	
		if (uri == null) {
			throw new NullPointerException("Null URI is not allowed as an argument to clearProperty(URI)");
		}
		
		detachProperties(getPropertyListForURI(uri));
		getPropertyListForURI(uri).clear();
		
		persist();
	}
	
	/**
	 * @inheritDoc
	 */
	public void clearProperty(SSWAPPredicate predicate) {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to clearProperty(URI)");
		}
		
		clearProperty(predicate.getURI());
	}

	private Map<SSWAPProperty,SSWAPProperty> getEquivalentPropertiesMap() {
		Map<SSWAPProperty,SSWAPProperty> result = new HashMap<SSWAPProperty,SSWAPProperty>();
		
		for (URI propertyURI : properties.keySet()) {
			for (SSWAPProperty property : properties.get(propertyURI)) {
				result.put(property, property);
			}
		}
		
		return result;
	}
	
	/**
	 * Scans the source model to find all the properties of this object, and initializes properties map. This method
	 * also initializes the type information for this individual (by calling initTypes())
	 */
	private void readProperties() {
		// only initialize if there is a source model
		Model model = (getSourceModel() != null) ? getSourceModel().getModel() : null;

		if (model != null) {
			Resource resource = getJenaResource(model);

			// init types (since they are hidden from the user via "normal" property interface)
			readTypes(model, resource);

			// TODO: test this
			final Map<SSWAPProperty,SSWAPProperty> equivalentProperties = getEquivalentPropertiesMap();
			properties.clear();
			
			
			forEachProperty(new Function<Statement, Void>() {
				public Void apply(Statement statement) {
					Property property = statement.getPredicate();
					
					if (property.getURI().startsWith(Namespaces.OWL_NS)) {
						return null;
					}

					RDFNode object = statement.getObject();
					SSWAPElement value = ImplFactory.get().createElement(getSourceModel(), object);
					PropertyImpl propertyImpl = new PropertyImpl(IndividualImpl.this, property);
					propertyImpl.setSourceModel(getSourceModel());	
					propertyImpl.setValue(value);
					
					// check whether we had already such an object created -- if yes, reuse the existing one
					// (so that any references users can hold remain valid)
					if (equivalentProperties.containsKey(propertyImpl)) {						
						doAddProperty(equivalentProperties.get(propertyImpl));
						equivalentProperties.remove(propertyImpl);
						getSourceModel().removeDependentModel(propertyImpl);
					}
					else {
						
						doAddProperty(propertyImpl);
					}

					return null;
				}
			});
			
			detachProperties(equivalentProperties.values());
		}
	}

	/**
	 * Executes a function for every non-ignored Jena property for this individual.
	 * 
	 * @param function
	 *            the function to be executed
	 */
	private void forEachProperty(Function<Statement, Void> function) {
		Model model = (getSourceModel() != null) ? getSourceModel().getModel() : null;

		if (model != null) {
			Resource resource = getJenaResource(model);

			Set<String> ignoredProperties = getIgnoredProperties();

			// for every statement where this individual is the subject, extract a property (unless it is an ignored
			// property)
			for (StmtIterator it = model.listStatements(resource, (Property) null, (RDFNode) null); it.hasNext();) {
				Statement statement = it.next();
				Property property = statement.getPredicate();

				if (!ignoredProperties.contains(property.getURI())) {
					function.apply(statement);
				}
			}
		}
	}

	private void updateTypes() {
		removeTypes();
		storeTypes();
	}

	private void removeTypes() {
		final Model model = (getSourceModel() != null) ? getSourceModel().getModel() : null;
		Resource resource = getJenaResource(model);
		List<Statement> statementsToRemove = new LinkedList<Statement>();

		for (StmtIterator it = model.listStatements(resource, RDF.type, (RDFNode) null); it.hasNext(); ) {
			Statement statement = it.next();
			
			if (!statement.getObject().isURIResource() ||
				!ignoredTypes.contains(statement.getObject().asResource().getURI())) {
				statementsToRemove.add(statement);
			}
		}
		
		model.remove(statementsToRemove);
	}

	private void storeTypes() {
		final Model model = (getSourceModel() != null) ? getSourceModel().getModel() : null;
		Resource resource = getJenaResource(model);
		
		if (resource == null) {
			return;
		}
		
		Property typeProperty = model.createProperty(RDF_TYPE_URI);

		for (SSWAPType type : getDeclaredTypes()) {
			RDFNode typeResource = model.getResource(type.getURI().toString());
			
			Statement statement = model.createStatement(resource, typeProperty, typeResource);
			model.add(statement);
		}
	}

	/**
	 * Updates the SSWAPProperties in the underlying data source. This consists of removing all underlying statements
	 * relating to the properties first, and then recreating them from scratch.
	 */
	private void updateProperties() {
		updateTypes();

		removeProperties();
		storeProperties();
	}

	/**
	 * Removes all statements about this object that correspond to non-ignored properties. (That is, after this
	 * operation, only ignored properties will remain.)
	 */
	private void removeProperties() {
		final Model model = (getSourceModel() != null) ? getSourceModel().getModel() : null;

		final List<Statement> statementsToRemove = new LinkedList<Statement>();

		// find properties to be removed (it is not possible to remove them during the find process,
		// as this will trigger a ConcurrentModificationException (the modification of the data over
		// which we iterate)
		forEachProperty(new Function<Statement, Void>() {
			public Void apply(Statement statement) {
				statementsToRemove.add(statement);

				return null;
			}
		});

		model.remove(statementsToRemove);
	}

	/**
	 * Creates statements about all non-ignored properties. (It is assumed that none of such statements exist at this
	 * point.)
	 */
	private void storeProperties() {
		final Model model = (getSourceModel() != null) ? getSourceModel().getModel() : null;
		Resource resource = getJenaResource(model);
		
		if (resource == null) {
			return;
		}

		for (SSWAPProperty property : getProperties()) {
			PropertyImpl propertyImpl = (PropertyImpl) property;

			Property jenaProperty = model.getProperty(propertyImpl.getURI().toString());
			RDFNode object = model.createLiteral("");

			if (propertyImpl.getValue() != null) {
				object = ImplFactory.get().createRDFNode(getSourceModel(), propertyImpl.getValue());
			}

			Statement statement = model.createStatement(resource, jenaProperty, object);
			model.add(statement);
		}
	}

	/**
	 * Reads the declared type information about an individual
	 * 
	 * @param model
	 *            the model containing the information about the individual
	 * @param resource
	 *            the Jena resource corresponding to the individual
	 */
	private void readTypes(Model model, Resource resource) {
		Property typeProperty = model.createProperty(RDF_TYPE_URI);

		// first gather all the objects of rdf:type predicates relating to this individual
		Set<String> types = new HashSet<String>();
		types.addAll(ignoredTypes);

		for (StmtIterator it = model.listStatements(resource, typeProperty, (RDFNode) null); it.hasNext();) {
			Statement statement = it.next();

			RDFNode object = statement.getObject();

			if (object.isResource()) {
				Resource objectResource = (Resource) object;

				types.add(objectResource.getURI());
			}
		}

		this.types = new HashSet<SSWAPType>();

		// now try to resolve the gathered information into SSWAPType/TypeImpl objects from the source model
		for (String type : types) {
			try {
				SSWAPType sswapType = getSourceModel().getType(new URI(type));
				this.types.add(sswapType);
			}
			catch (URISyntaxException e) {
				// ignore invalid data
			}
		}
	}

	/**
	 * Sets the source model for this individual. Additionally forces a refresh when this happens to ensure proper
	 * initialization of properties and types.
	 * 
	 * @param sourceModel
	 *            the new source model
	 */
	@Override
	public void setSourceModel(SourceModel sourceModel) {
		super.setSourceModel(sourceModel);

		if (isDereferenced()) {
			refresh();
		}
	}

	/**
	 * Refreshes the information stored in this individual by rescanning the underlying data source.
	 */
	@Override
	public void refresh() {
		super.refresh();
		readProperties();
	}

	/**
	 * Synchronizes the information stored in this individual to the underlying data source.
	 */
	@Override
	public void persist() {
		super.persist();
		
		updateProperties();
		
		refreshSiblings();
	}

	/**
	 * Gets the information about the ignored properties (hidden) from the user. This method is intentionally protected
	 * to allow subtypes to define their own sets of special properties.
	 * 
	 * @return the set of URIs of ignored properties
	 */
	protected Set<String> getIgnoredProperties() {
		return ignoredProperties;
	}

	protected Set<String> getIgnoredTypes() {
		return ignoredTypes;
	}
	
	/**
	 * Adds a new property to the list of ignored properties. This method should be mostly called by the subclasses that
	 * wish to hide their SSWAP-specific properties (e.g., Subject's "sswap:mapsTo").
	 * 
	 * @param ignoredProperty
	 *            the URL of the property that should be ignored
	 */
	protected void addIgnoredProperty(String ignoredProperty) {
		ignoredProperties.add(ignoredProperty);
	}
	
	protected void addIgnoredType(String ignoredType) {
		ignoredTypes.add(ignoredType);
	}

	/**
	 * Allows to safely cast this individual to SSWAPIndividual. (Useful when holding a reference to this object as
	 * SSWAPElement.)
	 * 
	 * @return a reference to this individual cast to SSWAPIndividual
	 */
	@Override
	public SSWAPIndividual asIndividual() {
		return this;
	}

	/**
	 * Allows to check whether this is a SSWAP individual. (It overrides the SSWAPElement.isIndividual() method, which returns
	 * false.)
	 * 
	 * @return always true, since this is an individual
	 */
	@Override
	public boolean isIndividual() {
		return true;
	}
	
	protected void assertType(String typeURI) throws ValidationException {
		try {
			SSWAPType type = assertSourceModel().getType(new URI(typeURI));
		
			if (!getDeclaredTypes().contains(type)) {
				throw new ValidationException("The individual is not properly typed as " + typeURI);
			}
		}
		catch (URISyntaxException e) {
			throw new ValidationException("The type provided is not a valid URI: " + typeURI);
		}
	}
	
	/**
	 * Checks whether a property defined in the RDG is defined for this individual (or whether a subproperty of the
	 * RDG's property is defined for this individual)
	 * 
	 * @param rdgProperty the property defined in RDG
	 * @param properties the properties defined for this individual
	 * @return true if there exists a property for this individual (incl. a subproperty) for the specified RDG property
	 */
	private static boolean isRDGPropertyDefined(SSWAPProperty rdgProperty, Collection<SSWAPProperty> properties) {
		for (SSWAPProperty rigProperty : properties) {
			if (rigProperty.getPredicate().isSubPredicateOf(rdgProperty.getPredicate())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Sets the default parameter values for this individual based on the information from the individual in an RDG.
	 * 
	 * @param rdgIndividual
	 */
	void setDefaultParameterValues(IndividualImpl rdgIndividual) {		
		Collection<SSWAPProperty> properties = getProperties();
				
		for (SSWAPProperty rdgProperty : rdgIndividual.getProperties()) {
			if (!isRDGPropertyDefined(rdgProperty, properties)) {
				// RIG did not define a property that existed in RDG, this means that we will copy the value of the property from RDG
				// to RIG (properties from RDG are considered to be default values for RIG)
				SSWAPElement newValue = ImplFactory.get().deepCopyElement(getSourceModel(), rdgProperty.getValue(), new HashSet<URI>());
				PropertyImpl newProperty = createProperty(rdgProperty.getURI(), newValue); 
				doAddProperty(newProperty);
				persist();
			}
		}
	}		
	
	/**
	 * @inheritDoc
	 */
	public boolean isOfType(SSWAPType type) {
		if (type == null) {
			throw new NullPointerException("Null SSWAPType is not allowed as an argument to isOfType(SSWAPType)");
		}
		
		return getType().isSubTypeOf(type);
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isCompatibleWith(SSWAPType type) {
		if (type == null) {
			throw new NullPointerException("Null SSWAPType is not allowed as an argument to isCompatibleWith(SSWAPType)");
		}
		
		return !getType().intersectionOf(type).isNothing();
	}
		
	/**
	 * Creates a property instance for the specified predicate URI and value
	 * 
	 * @param uri the URI of the predicate
	 * @param value the value
	 * @throws IllegalArgumentException if the value is not legal for the specified predicate (e.g., literal for an object
	 * property)
	 * @return the created property
	 */
	private PropertyImpl createProperty(URI uri, SSWAPElement value) throws IllegalArgumentException{	
		SourceModel sourceModel = assertSourceModel();
		
		PropertyImpl result =  ImplFactory.get().createProperty(sourceModel, this, value, uri);
		
		return result;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, SSWAPIndividual individual) throws IllegalArgumentException {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to addProperty(SSWAPPredicate, SSWAPIndividual)");
		}
		
		if (individual == null) {
			throw new NullPointerException("Null SSWAPIndividual is not allowed as an argument to addProperty(SSWAPPredicate, SSWAPIndividual)");
		}
		
		verifyReservedPredicate(predicate);
		
		SSWAPProperty property = createProperty(predicate.getURI(), individual);
		
		addProperty(property);
		
		return property;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, String value) throws IllegalArgumentException {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to addProperty(SSWAPPredicate, String)");
		}
		
		if (value == null) {
			throw new NullPointerException("Null String value is not allowed as an argument to addProperty(SSWAPPredicate, String)");
		}
		
		verifyReservedPredicate(predicate);
		
		SourceModel sourceModel = assertSourceModel();
		
		SSWAPLiteral literal = null;
		
		if (predicate.isDatatypePredicate()) {
			String range = predicate.getDatatypePredicateRange();
			
			if (range != null) {
				try {
					URI datatypeURI = new URI(range);
					literal = sourceModel.createTypedLiteral(value, datatypeURI);
				}
				catch (URISyntaxException e) {
					// nothing -- literal will be created untyped below 
				}
			}
		}
		
		
		if (literal == null) {
			// we were unable to properly type the literal, so we will create an untyped one
			literal = sourceModel.createLiteral(value);	
		}
		
		SSWAPProperty property = createProperty(predicate.getURI(), literal);
		
		addProperty(property);
		
		return property;		
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, String value, URI datatype) throws IllegalArgumentException {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to addProperty(SSWAPPredicate, String, URI)");
		}
		
		if (value == null) {
			throw new NullPointerException("Null String value is not allowed as an argument to addProperty(SSWAPPredicate, String, URI)");
		}
		
		if (datatype == null) {
			throw new NullPointerException("Null URI is not allowed as the datatype argument to addProperty(SSWAPredicate, String, URI)");
		}
		
		verifyReservedPredicate(predicate);
		
		SourceModel sourceModel = assertSourceModel();
		SSWAPLiteral literal = sourceModel.createTypedLiteral(value, datatype);
		
		SSWAPProperty property = createProperty(predicate.getURI(), literal);
		
		addProperty(property);
		
		return property;				
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty addProperty(SSWAPPredicate predicate, SSWAPLiteral literal) throws IllegalArgumentException {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to addProperty(SSWAPPredicate, SSWAPLiteral)");
		}
		
		if (literal == null) {
			throw new NullPointerException("Null SSWAPLiteral is not allowed as an argument to addProperty(SSWAPPredicate, SSWAPLiteral)");
		}
		
		verifyReservedPredicate(predicate);
		
		SSWAPProperty property = createProperty(predicate.getURI(), literal);
		
		addProperty(property);
		
		return property;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, SSWAPIndividual individual) throws IllegalArgumentException {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to setProperty(SSWAPPredicate, SSWAPIndividual)");
		}
		
		if (individual == null) {
			throw new NullPointerException("Null SSWAPIndividual is not allowed as an argument to setProperty(SSWAPPredicate, SSWAPIndividual)");
		}

		verifyReservedPredicate(predicate);
		
		SSWAPProperty property = createProperty(predicate.getURI(), individual);
		
		setProperty(property);
		
		return property;		
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, String value) throws IllegalArgumentException {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to setProperty(SSWAPPredicate, String)");
		}
		
		if (value == null) {
			throw new NullPointerException("Null String value is not allowed as an argument to setProperty(SSWAPPredicate, String)");
		}
		
		verifyReservedPredicate(predicate);
		
		SourceModel sourceModel = assertSourceModel();
		
		SSWAPLiteral literal = null;
		
		if (predicate.isDatatypePredicate()) {
			String range = predicate.getDatatypePredicateRange();
			
			if (range != null) {
				try {
					URI datatypeURI = new URI(range);
					literal = sourceModel.createTypedLiteral(value, datatypeURI);
				}
				catch (URISyntaxException e) {
					// nothing -- literal will be created untyped below 
				}
			}
		}
		
		
		if (literal == null) {
			// we were unable to properly type the literal, so we will create an untyped one
			literal = sourceModel.createLiteral(value);	
		}
		
		SSWAPProperty property = createProperty(predicate.getURI(), literal);
				
		setProperty(property);
		
		return property;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, String value, URI datatype) throws IllegalArgumentException {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to setProperty(SSWAPPredicate, String, URI)");
		}
		
		if (value == null) {
			throw new NullPointerException("Null String value is not allowed as an argument to setProperty(SSWAPPredicate, String, URI)");
		}
		
		if (datatype == null) {
			throw new NullPointerException("Null URI is not allowed as the datatype argument to setProperty(SSWAPredicate, String, URI)");
		}
		
		verifyReservedPredicate(predicate);
		
		SourceModel sourceModel = assertSourceModel();
		SSWAPLiteral literal = sourceModel.createTypedLiteral(value, datatype);
		
		SSWAPProperty property = createProperty(predicate.getURI(), literal);
		
		setProperty(property);
		
		return property;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPProperty setProperty(SSWAPPredicate predicate, SSWAPLiteral literal) {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to setProperty(SSWAPPredicate, SSWAPLiteral)");
		}
		
		if (literal == null) {
			throw new NullPointerException("Null SSWAPLiteral is not allowed as an argument to setProperty(SSWAPPredicate, SSWAPLiteral)");
		}
		
		verifyReservedPredicate(predicate);
		
		SSWAPProperty property = createProperty(predicate.getURI(), literal);
		
		setProperty(property);
		
		return property;		
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean hasValue(SSWAPPredicate predicate, SSWAPElement element) {
		if (predicate == null) {
			throw new NullPointerException("Null SSWAPPredicate is not allowed as an argument to hasValue(SSWAPredicate, SSWAPElement)");
		}
		
		if (element == null) {
			throw new NullPointerException("Null SSWAPElement is not allowed as an argument to hasValue(SSWAPPredicate, SSWAPElement)");
		}
		
		for (SSWAPProperty property : getProperties(predicate)) {
			if (element.equals(property.getValue())) {
				return true;
			}			
		}
		
		return false;
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPProperty> hasValue(SSWAPElement element) {
		if (element == null) {
			throw new NullPointerException("Null SSWAPElement is not allowed as an argument to hasValue(SSWAPElement)");
		}	
		
		List<SSWAPProperty> result = new LinkedList<SSWAPProperty>();
		
		for (SSWAPProperty property : getProperties()) {
			if (element.equals(property.getValue())) {
				result.add(property);
			}
		}
		
		return result;		
	}
	
	/**
	 * Detaches SSWAPProperties from this individual and source model. (To be called if 
	 * the SSWAPProperties are no longer used in the current individual.)
	 * 
	 * @param properties
	 */
	private void detachProperties(Collection<SSWAPProperty> properties) {
		SourceModel sourceModel = assertSourceModel();
		
		if (properties == null) {
			return;
		}
		
		for (SSWAPProperty property : properties) {
			if (property instanceof PropertyImpl) {
				// set individual to null (no longer belongs to this individual)
				((PropertyImpl) property).setIndividual(null);
				
				// detach it from the model (to prevent any memory leak)
				sourceModel.removeDependentModel((PropertyImpl) property);
			}
		}
	}
	
	/**
	 * Overridden equals() method. For an individual, it is only equal to another individual
	 * with the same rdf identifier (URL or BNode).
	 * 
	 * @param o another object to be compared for equality with this one
	 * @return true if another object is equivalent to this one, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		// the following is for performance reasons
		if (this == o) {
			return true;
		}

		// the only equivalent objects are ModelImpls or their subclasses
		// (NOTE: instanceof returns false for nulls)
		if (o instanceof IndividualImpl) {
			return rdfIdEquals((IndividualImpl) o);			
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
	
	/**
     * @inheritDoc
     */
    public void addLabel(String label) {
    	if (label == null) {
    		throw new NullPointerException("Null label is not allowed in addLabel(String)");
    	}
    	
    	SourceModel sourceModel = assertSourceModel();
    	addProperty(sourceModel.getPredicate(URI.create(RDFS.label.toString())), label);	
    }
    
    /**
     * @inheritDoc
     */
    public void addComment(String comment) {
    	if (comment == null) {
    		throw new NullPointerException("Null comment is not allowed in addComment(String)");
    	}
    	
    	SourceModel sourceModel = assertSourceModel();
    	addProperty(sourceModel.getPredicate(URI.create(RDFS.comment.toString())), comment);	
    }
    
    /**
     * @inheritDoc
     */
    public SSWAPIndividual getInferredIndividual() {
    	ReasoningServiceImpl reasoningService = ((ReasoningServiceImpl) getReasoningService());
    	
    	Model infIndModel = reasoningService.extractInferredIndividualModel(getURI());
    	ModelUtils.removeBNodes(infIndModel);
    	
    	SourceModelImpl infIndSourceModel = ImplFactory.get().createEmptySSWAPDataObject(getURI(), SourceModelImpl.class);
    	infIndSourceModel.dereference(infIndModel);
    	
    	((ReasoningServiceImpl) infIndSourceModel.getReasoningService()).getPelletKB();
    	
    	IndividualImpl infInd = ImplFactory.get().createEmptySSWAPDataObject(getURI(), IndividualImpl.class);
    	
    	infInd.setSourceModel(infIndSourceModel);
    	
    	return infInd;
    }
    
    private void verifyReservedPredicate(SSWAPPredicate predicate) throws IllegalArgumentException {
    	if (predicate.isReserved() && !predicate.isAnnotationPredicate() && !predicate.getURI().toString().equals(Vocabulary.TOKEN.getURI())) {
    		throw new IllegalArgumentException("Attempt to set value for a reserved predicate: " + predicate.getURI());
    	}
    }
    
    private static boolean isUNADisabledWhenClosingWorld() {
    	return Boolean.valueOf(Config.get().getProperty(Config.DISABLE_UNA_WHEN_CLOSING_WORLD_KEY, 
    					                                Config.DISABLE_UNA_WHEN_CLOSING_WORLD));
    }
    
    /**
     * Closing the world for all *existing* properties of the individual by adding an owl:maxCardinality restriction
     * for each property (i.e., we are not closing the world for properties that are not known for this individual)
     */
    void closeWorld() {
    	Map<URI,List<SSWAPElement>> propertyValues = new HashMap<URI,List<SSWAPElement>>();
    	
    	for (SSWAPProperty property : getProperties()) {
    		List<SSWAPElement> values = propertyValues.get(property.getURI());
    		
    		if (values == null) {
    			values = new LinkedList<SSWAPElement>();
    			propertyValues.put(property.getURI(), values);
    		}    		
    		
    		values.add(property.getValue());
    		
    		// I believe that we need this line to make sure the type of the predicate (object vs. datatype)
    		// is known to ReasoningService/Pellet before addCloseWorldRestrictions will put a restriction on
    		// this predicate (AFAICT not having this line could force Pellet to make an non-deterministic decision whether the
    		// predicate is object/datatype property (and a wrong decision) when it sees the restriction
    		property.getPredicate().isDatatypePredicate();
    	}
    	
    	for (URI predicateURI :  propertyValues.keySet()) {
    		List<SSWAPElement> values = propertyValues.get(predicateURI);
    		
    		if (isUNADisabledWhenClosingWorld()) {
    			disableUNA(values);
    		}
    		    		
    		addCloseWorldRestrictions(predicateURI, values.size());
       	}    	    	    	
    }
    
    private void addCloseWorldRestrictions(URI predicateURI, int count) {
		Model closedWorldModel = getSourceModel().getClosedWorldModel();
		
		boolean objectPredicate = getDocument().getPredicate(predicateURI).isObjectPredicate();
    	boolean datatypePredicate = getDocument().getPredicate(predicateURI).isDatatypePredicate();
		
		Resource restrictionResource = closedWorldModel.getResource(ModelUtils.generateBNodeId());
		Statement restrictionStatement = closedWorldModel.createStatement(restrictionResource, OWL.maxCardinality, closedWorldModel.createTypedLiteral(count, XSDDatatype.XSDnonNegativeInteger));
		
		TypeImpl.addRestriction(closedWorldModel, restrictionResource, predicateURI.toString(), restrictionStatement, objectPredicate, datatypePredicate);
		
		Resource individualResource = closedWorldModel.getResource(getURI().toString());
		
		closedWorldModel.add(individualResource, RDF.type, restrictionResource);    	
    }
    
    private void disableUNA(Collection<SSWAPElement> values) {
    	Model closedWorldModel = getSourceModel().getClosedWorldModel();		
    	
    	ReasoningServiceImpl reasoningService = (ReasoningServiceImpl) getReasoningService();
    	
		for (SSWAPElement element1 : values) {
			for (SSWAPElement element2 : values) {
				if (element1.isIndividual() && element2.isIndividual() && !element1.getURI().equals(element2.getURI())) {										
					SSWAPIndividual ind1 = element1.asIndividual();
					SSWAPIndividual ind2 = element2.asIndividual();
					
					boolean sameAs = reasoningService.isSameAs(ind1, ind2);
					boolean differentFrom = reasoningService.isDifferentFrom(ind1, ind2);
										
					if (!sameAs && !differentFrom) {												
						Resource ind1Resource = closedWorldModel.getResource(ind1.getURI().toString());
						Resource ind2Resource = closedWorldModel.getResource(ind2.getURI().toString());
						
						closedWorldModel.add(ind1Resource, OWL.differentFrom, ind2Resource);
					}
				}
			}	
		}

    }
}
