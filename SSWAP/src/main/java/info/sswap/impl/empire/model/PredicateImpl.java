/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import info.sswap.api.model.ReasoningService;
import info.sswap.api.model.SSWAPDatatype;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPType;
import info.sswap.impl.empire.Namespaces;

/**
 * Implementation of SSWAPPredicate. (A definition of a property.)
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public class PredicateImpl extends ElementImpl implements SSWAPPredicate {
	private static final Set<Property> BUILTIN_ANNOTATION_PROPS = ImmutableSet.of(RDFS.label, RDFS.comment,
	                RDFS.seeAlso, RDFS.isDefinedBy, OWL.versionInfo, OWL.backwardCompatibleWith, OWL.priorVersion,
	                OWL.incompatibleWith, OWL2.deprecated);
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

	/**
	 * Creates a property definition.
	 * 
	 * @param parent 
	 *            the source model to which this predicate belongs
	 * 
	 * @param uri
	 *            the URI of the property.
	 */
	public PredicateImpl(SourceModel parent, URI uri) {
		if (uri != null) {
			setURI(uri);
		}
		
		setSourceModel(parent);
	}

	
	/**
	 * Gets the corresponding Jena resource for this property. This method lazily initializes the "resource" field of this
	 * property
	 * 
	 * @return Jena Resource for the predicate
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
	 * @inheritDoc
	 */
	public void addDomain(SSWAPType type) {
		TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), RDFS.domain, typeImpl.getResource());
	}

	/**
	 * @inheritDoc
	 */
	public void addEquivalentPredicate(SSWAPPredicate propertyDef) {
		PredicateImpl propertyImpl =  ImplFactory.get().assertImplementation(propertyDef, PredicateImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), OWL.equivalentProperty, propertyImpl.getResource());
	}

	/**
	 * @inheritDoc
	 */
	public void addInverseOf(SSWAPPredicate propertyDef) {
		PredicateImpl propertyImpl =  ImplFactory.get().assertImplementation(propertyDef, PredicateImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), OWL.inverseOf, propertyImpl.getResource());	
	}

	/**
	 * @inheritDoc
	 */
    public void addRange(SSWAPType type) {
		TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), RDFS.range, typeImpl.getResource());
    }
    
    /**
	 * @inheritDoc
	 */
    public void addRange(SSWAPDatatype type) {
		DatatypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, DatatypeImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), RDFS.range, typeImpl.getResource());
    }

	/**
	 * @inheritDoc
	 */
    public void addSubPredicateOf(SSWAPPredicate propertyDef) {
		PredicateImpl propertyImpl =  ImplFactory.get().assertImplementation(propertyDef, PredicateImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), RDFS.subPropertyOf, propertyImpl.getResource());
    }
	
	/**
     * @inheritDoc
     */
    public void addType(SSWAPType type) {
    	TypeImpl typeImpl =  ImplFactory.get().assertImplementation(type, TypeImpl.class);		
		Model model = assertModel();
		
		model.add(getResource(), RDF.type, typeImpl.getResource());
    }
        
    /**
     * @inheritDoc
     */
    public void addAnnotationPredicate(SSWAPPredicate predicate, SSWAPElement value) {		
		Model model = assertModel();
		
		model.add(getResource(), model.getProperty(predicate.getURI().toString()), ImplFactory.get().createRDFNode(getSourceModel(), value));
    }
    
	
	/**
	 * @inheritDoc
	 */
	public boolean isObjectPredicate() {
		return getReasoningServiceImpl().isObjectPredicate(this);
	}

	/**
	 * @inheritDoc
	 */
	public boolean isDatatypePredicate() {
		return getReasoningServiceImpl().isDatatypePredicate(this);
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isAnnotationPredicate() {
		return BUILTIN_ANNOTATION_PROPS.contains(getResource())
		                || getReasoningServiceImpl().isAnnotationPredicate(this);
	}
	
	private ReasoningServiceImpl getReasoningServiceImpl() {
		ReasoningService reasoningService = getReasoningService();
		
		if (reasoningService instanceof ReasoningServiceImpl) {
			return (ReasoningServiceImpl) reasoningService;
		}
		
		throw new IllegalArgumentException("The reasoning service associated with this predicate has not been created by this API implementation");
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isSubPredicateOf(SSWAPPredicate sup) {
		return getReasoningServiceImpl().isSubPredicateOf(this, sup);
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isStrictSubPredicateOf(SSWAPPredicate sup) {
		return getReasoningServiceImpl().isStrictSubPredicateOf(this, sup);
	}
		
	/**
	 * @inheritDoc
	 */
	public SSWAPType getObjectPredicateRange() {
		Collection<SSWAPType> ranges = getObjectPredicateRanges();
		
		if ((ranges != null) && (ranges.size() == 1)) {
			return ranges.iterator().next();
		}
		else if ((ranges != null) && (ranges.size() > 1)) {
			return TypeImpl.intersectionOf(this, ranges);				
		} 
		else {
			return assertSourceModel().getType(URI.create(OWL.Thing.getURI()));
		}		
	}

	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPType> getObjectPredicateRanges() {
		List<SSWAPType> result = new LinkedList<SSWAPType>();
		
		if (isDatatypePredicate()) {
			throw new IllegalArgumentException("Unable to get a range via getObjectPredicateRanges() because this is a datatype predicate: " + getURI());
		}
		
		for (String range : getReasoningServiceImpl().getRanges(this)) {
			result.add(getDocument().getType(URI.create(range)));
		}
				
		return result;
	}

	
	/**
	 * @inheritDoc
	 */
	public String getDatatypePredicateRange() {
		if (isObjectPredicate()) {
			throw new IllegalArgumentException("Unable to get a range via getDatatypePredicateRange() because this is an object predicate: " + getURI());
		}
		
		String range = getReasoningServiceImpl().getRange(this); 
		
		// if we were unable to determine the range, it may be because the range is an anonymous datatype that is an enumeration of values
		// let's try to extract the type of literals from this enum
		if (range == null) {
			range = getReasoningServiceImpl().getTypeForEnumRange(this);
		}
		
		return range; 
	}

	/**
	 * @inheritDoc
	 */
	public Collection<String> getDatatypePredicateRanges() {		
		if (isObjectPredicate()) {
			throw new IllegalArgumentException("Unable to get a range via getDatatypePropertyRanges() because this is an object property: " + getURI());
		}
		
		return getReasoningServiceImpl().getRanges(this);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPType getDomain() {
		Collection<SSWAPType> domains = getReasoningServiceImpl().getDomains(this);
		
		if ((domains != null) && (domains.size() == 1)) {
			return domains.iterator().next();
		}
		else if ((domains != null) && (domains.size() > 1)) {
			return TypeImpl.intersectionOf(this, domains);				
		} 
		else {
			return assertSourceModel().getType(URI.create(OWL.Thing.getURI()));
		}
	}
	
	@Override
	public boolean equals(Object o) {
		// the following is for performance reasons
		if (this == o) {
			return true;
		}

		// the only equivalent objects are ModelImpls or their subclasses
		// (NOTE: instanceof returns false for nulls)
		if (o instanceof PredicateImpl) {
			return rdfIdEquals((PredicateImpl) o);			
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
}
