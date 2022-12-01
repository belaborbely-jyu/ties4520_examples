/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.ReasoningService;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.Namespaces;

/**
 * Implementation of SSWAPType.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class TypeImpl extends ElementImpl implements SSWAPType {

	/**
	 * The RDF identifier of this type. (The types can also be anonymous classes, and therefore they may not have an
	 * URI.)
	 */
	@SuppressWarnings("unchecked")
	private RdfKey rdfKey;

	/**
	 * The Jena resource that defines this type (important for fast access to type information).
	 * 
	 * This field is lazily initialized by getResource(). All accesses to this field should be made via the
	 * getResource() method.
	 */
	private Resource resource;
	
	public TypeImpl(SourceModel parent, URI uri) {
		this(parent, uri, false /* preventOWLClass */);
	}
	
	public TypeImpl(SourceModel parent, URI uri, boolean preventOWLClass) {
		if (uri != null) {
			setURI(uri);			
		}
		else {
			String anonId = ModelUtils.generateBNodeId();
			setURI(URI.create(anonId));
		}
		
		setSourceModel(parent);
		
		if (!preventOWLClass) {
			if (getURI() != null && !getURI().toString().startsWith(OWL.NS)) {
				Model model = parent.getModel();

				if (!(ModelUtils.isBNodeURI(getURI().toString()) && model.contains(model.createResource(getURI().toString()), RDF.type, OWL.Restriction))) {
					parent.getModel().add(createRdfTypeStatement(URI.create(OWL.Class.toString())));	
				}			
			}
		}
	}

	/**
	 * Gets the corresponding Jena resource for this type. (Since types can be anonymous classes, the Jena resource
	 * effectively provides a handle to such resources.) This method lazily initializes the "resource" field of this
	 * class.
	 * 
	 * @return Jena Resource representation
	 */
	Resource getResource() {
		if (resource == null) {
			if (rdfKey instanceof BNodeKey) {
				resource = assertModel().createResource(new AnonId(rdfKey.value().toString()));
			}
			else {
				resource = assertModel().createResource(getURI().toString());
			}
		}

		return resource;
	}

	/**
	 * Creates a Jena statement that states that this type is an owl:Class. The statement is of form:
	 * 
	 * this.rdfKey rdf:type owl:Class .
	 * 
	 * @return the statement created for the source model of this type
	 */
	private Statement createRdfTypeStatement(URI typeURI) {
		Model model = assertModel();

		Resource typeResource = model.createResource(typeURI.toString());

		return model.createStatement(getResource(), RDF.type, typeResource);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPType complementOf() {
		// create an anonymous type
		TypeImpl result = new TypeImpl(getSourceModel(), /* uri */ null);
		Model model = assertModel();

		// add information to the source model that this is a complement of this type
		model.add(model.createStatement(result.getResource(), OWL.complementOf, getResource()));

		return result;
	}	

	/**
	 * Creates a complex type that consists of multiple other types connected by a property (e.g., owl:intersectionOf or owl:unionOf). 
	 * 
	 * @param model the model where the type should be stored
	 * @param resultType the type to which the resultant type will be added
	 * @param connectingProperty the property (currently only owl:intersectionOf and owl:unionOf make sense)
	 * @param types the types to be connected -- must be a list of at least two types
	 * @throws IllegalArgumentException if the list of types does not contain at least two types or the objects were not created 
	 * by this API implementation
	 */
	private static void createComplexType(SSWAPModel model, TypeImpl resultType, Property connectingProperty, Collection<SSWAPType> types) throws IllegalArgumentException {
		if (types.size() < 2) {
			throw new IllegalArgumentException("The complex type has to be combined of at least two types");
		}
		
		ModelImpl modelImpl = ImplFactory.get().assertImplementation(model, ModelImpl.class);
		Model jenaModel = modelImpl.assertModel();
			
		// prepare the list of resources
		List<Resource> intersectionResources = new LinkedList<Resource>();
		for (SSWAPType type : types) {
			if (!(type instanceof TypeImpl)) {
				throw new IllegalArgumentException("This type has not been created by this API implementation");
			}
			
			intersectionResources.add(((TypeImpl) type).getResource());			
		}
		
		RDFList rdfList = jenaModel.createList(intersectionResources.iterator());
		jenaModel.add(jenaModel.createStatement(resultType.getResource(), connectingProperty, rdfList));
	}
	
	private static SSWAPType createComplexType(SSWAPModel model, Property connectingProperty, Collection<SSWAPType> types) {
		ModelImpl modelImpl = ImplFactory.get().assertImplementation(model, ModelImpl.class);
		
		// create an anonymous type for the result
		TypeImpl result = new TypeImpl(modelImpl.assertSourceModel(), /* uri */ null);
		
		createComplexType(model, result, connectingProperty, types);
		
		return result;
	}
	
	/**
	 * Creates a complex type that is an intersection of the specified types.
	 * 
	 * @param model the model that will store the types
	 * @param types the types to be included in the intersection
	 * @return a complex type: the intersection of multiple types
	 */
	public static SSWAPType intersectionOf(SSWAPModel model, Collection<SSWAPType> types) {
		return createComplexType(model, OWL.intersectionOf, types);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPType intersectionOf(SSWAPType a) {		
		return intersectionOf(this, Arrays.asList(this, a) );
	}

	/**
	 * Creates a complex type that is a union of the specified types.
	 * 
	 * @param model the model that will store the types
	 * @param types the types to be included in the union
	 * @return a complex type: the intersection of multiple types
	 */
	public static SSWAPType unionOf(SSWAPModel model, Collection<SSWAPType> types) {
		return createComplexType(model, OWL.unionOf, types);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPType unionOf(SSWAPType a) {
		return unionOf(this, Arrays.asList(this, a));
	}

	/**
	 * Gets the RDF identifier of this type.
	 * 
	 * @return the RDF identifier of this type (URL or BNode identifier)
	 */
	@SuppressWarnings("unchecked")
	public RdfKey getRdfId() {
		return rdfKey;
	}

	/**
	 * Sets the RDF identifier of this type.
	 * 
	 * @param rdfKey
	 *            the RDF identifier of this type (URL or BNode identifier).
	 */
	@SuppressWarnings("unchecked")
	public void setRdfId(RdfKey rdfKey) {
		this.rdfKey = rdfKey;
	}

	/**
	 * Returns the string representation of the RDF identifier of this type.
	 */
	public String toString() {
		return rdfKey.value().toString();
	}
	
	
	public void addUnionOf(Collection<SSWAPType> types) {
		createComplexType(this, this, OWL.unionOf, types);
	}
	
	public void addIntersectionOf(Collection<SSWAPType> types) {
		createComplexType(this, this, OWL.intersectionOf, types);
	}

    public void addDisjointUnionOf(Collection<SSWAPType> disjointClasses) {
    	createComplexType(this, this, OWL2.disjointUnionOf, disjointClasses);	    
    }

	/**
	 * @inheritDoc
	 */
    public void addDisjointWith(SSWAPType type) {
		TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), OWL.disjointWith,  typeImpl.getResource());    
	}

	/**
	 * @inheritDoc
	 */
    public void addEquivalentClass(SSWAPType type) {
		TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), OWL.equivalentClass, typeImpl.getResource());
    }

	/**
	 * @inheritDoc
	 */
    public void addOneOf(Collection<URI> oneOf) {
		Model model = assertModel();
		List<Resource> oneOfResources = new LinkedList<Resource>();
		
		for (URI uri : oneOf) {
			oneOfResources.add(model.getResource(uri.toString()));
		}
		
		RDFList rdfList = model.createList(oneOfResources.iterator());
		
		model.add(getResource(), OWL.oneOf, rdfList);
    }

	/**
	 * @inheritDoc
	 */
    public void addRestrictionAllValuesFrom(SSWAPPredicate predicate, SSWAPType type) {
    	TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);
    	Model model = assertModel();
    	
    	addRestriction(predicate, model.createStatement(getResource(), OWL.allValuesFrom, typeImpl.getResource()));    	
    }

	/**
	 * @inheritDoc
	 */
    public void addRestrictionHasSelf(SSWAPPredicate predicate, boolean value) {
    	Model model = assertModel();
    	
    	addRestriction(predicate, model.createStatement(getResource(), OWL2.hasSelf, model.createTypedLiteral(value)));
    }

	/**
	 * @inheritDoc
	 */
    public void addRestrictionHasValue(SSWAPPredicate predicate, SSWAPElement element) {
    	Model model = assertModel();
    	SourceModel sourceModel = assertSourceModel();
    	RDFNode value = ImplFactory.get().createRDFNode(sourceModel, element);
    	
	    addRestriction(predicate, model.createStatement(getResource(), OWL.hasValue, value));
    }

	/**
	 * @inheritDoc
	 */
    public void addRestrictionMaxCardinality(SSWAPPredicate predicate, int maxCardinality) {
    	Model model = assertModel();
    	
    	addRestriction(predicate, model.createStatement(getResource(), OWL.maxCardinality, model.createTypedLiteral(maxCardinality, XSDDatatype.XSDnonNegativeInteger)));
    }

	/**
	 * @inheritDoc
	 */
    public void addRestrictionMinCardinality(SSWAPPredicate predicate, int minCardinality) {
    	Model model = assertModel();
    	
    	addRestriction(predicate, model.createStatement(getResource(), OWL.minCardinality, model.createTypedLiteral(minCardinality, XSDDatatype.XSDnonNegativeInteger)));
    }

	/**
	 * @inheritDoc
	 */
    public void addRestrictionCardinality(SSWAPPredicate predicate, int cardinality) {
    	Model model = assertModel();
    	
    	addRestriction(predicate, model.createStatement(getResource(), OWL.cardinality, model.createTypedLiteral(cardinality, XSDDatatype.XSDnonNegativeInteger)));
    }

	/**
	 * @inheritDoc
	 */
    public void addRestrictionSomeValuesFrom(SSWAPPredicate predicate, SSWAPType type) {
    	TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);
    	Model model = assertModel();
    	
    	addRestriction(predicate, model.createStatement(getResource(), OWL.someValuesFrom, typeImpl.getResource()));
    }
    
    private void addRestriction(SSWAPPredicate predicate, Statement restrictionStatement) {
    	Model model = assertModel();
    	
    	boolean objectPredicate = predicate.isObjectPredicate();
    	boolean datatypePredicate = predicate.isDatatypePredicate();

    	addRestriction(model, getResource(), predicate.getURI().toString(), restrictionStatement, objectPredicate, datatypePredicate);
    }
    
    static void addRestriction(Model model, Resource resource, String propertyURI, Statement restrictionStatement, boolean objectPredicate, boolean datatypePredicate) {    	
    	model.add(model.createStatement(resource, RDF.type, OWL.Restriction));
    	model.add(model.createStatement(resource, OWL.onProperty, model.getResource(propertyURI)));

    	if (datatypePredicate) {    	
    		model.add(model.createStatement(model.getResource(propertyURI), RDF.type, OWL.DatatypeProperty));	
    	}
    	else if (objectPredicate) {
    		model.add(model.createStatement(model.getResource(propertyURI), RDF.type, OWL.ObjectProperty));
    	}
    	
    	model.add(restrictionStatement);    	
    }

	/**
	 * @inheritDoc
	 */
    public void addSubClassOf(SSWAPType type) {
		TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), RDFS.subClassOf, typeImpl.getResource());	    
    }
    
    /**
     * @inheritDoc
     */
    public void addAnnotationPredicate(SSWAPPredicate predicate, SSWAPElement value) {		
		Model model = assertModel();
		
		model.add(getResource(), model.getProperty(predicate.getURI().toString()), ImplFactory.get().createRDFNode(getSourceModel(), value));
    }

    private ReasoningServiceImpl getReasoningServiceImpl() {
		ReasoningService reasoningService = getReasoningService();
		
		if (reasoningService instanceof ReasoningServiceImpl) {
			return (ReasoningServiceImpl) reasoningService;
		}
		
		throw new IllegalArgumentException("The reasoning service associated with this property has not been created by this API implementation");
	}
    
    /**
     * @inheritDoc
     */
    public boolean isSubTypeOf(SSWAPType superType) {
    	return getReasoningServiceImpl().isSubTypeOf(this, superType);
    }
    
    /**
     * @inheritDoc
     */
    public boolean isStrictSubTypeOf(SSWAPType superType) {
    	return getReasoningServiceImpl().isStrictSubTypeOf(this, superType);
    }
    
    /**
     * @inheritDoc
     */
    public boolean isNothing() {
    	SSWAPType nothing = getDocument().getType(URI.create(OWL.Nothing.toString()));
    	
    	return isSubTypeOf(nothing);
    }
    
	@Override
	public boolean equals(Object o) {
		// the following is for performance reasons
		if (this == o) {
			return true;
		}

		// the only equivalent objects are ModelImpls or their subclasses
		// (NOTE: instanceof returns false for nulls)
		if (o instanceof TypeImpl) {
			return rdfIdEquals((TypeImpl) o);			
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
	@Override
	public boolean isReserved() {
		String uri = getURI().toString();
		
		if (uri != null) {
			return (uri.startsWith(Namespaces.RDF_NS)
			    || uri.startsWith(Namespaces.RDFS_NS)
			    || uri.startsWith(Namespaces.OWL_NS)
			    || uri.startsWith(Namespaces.XSD_NS)
			    || uri.startsWith(Namespaces.SSWAP_NS));			    				
		}
		
		return false;
	}
	
	public boolean isIntersection() {
		return getReasoningServiceImpl().isIntersection(getURI().toString());
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void serialize(OutputStream os, RDFRepresentation representation, boolean commentedOutput) {
		if (hasSourceModel()) {
			((SourceModelImpl) getSourceModel()).persist();

			Model closureModel = ((SourceModel) ExtensionAPI.getClosureDocument(getSourceModel())).getModel();
			Model partitionedModel = ModelUtils.partitionModel(closureModel, getURI().toString(), false);			
			
			ModelUtils.serializeModel(partitionedModel, os, representation, /* commentedOutput */ commentedOutput);			
		}
	}
}
