/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import info.sswap.api.model.SSWAPDatatype;
import info.sswap.api.model.SSWAPLiteral;

/**
 * Implementation of SSWAPDatatype
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class DatatypeImpl extends ElementImpl implements SSWAPDatatype {

	/**
	 * The RDF identifier of this data type.
	 */ 
	@SuppressWarnings("unchecked")
	private RdfKey rdfKey;

	/**
	 * The Jena resource that defines this data type (important for fast access to type information).
	 * 
	 * This field is lazily initialized by getResource(). All accesses to this field should be made via the
	 * getResource() method.
	 */
	private Resource resource;

	/**
	 * Creates a datatype 
	 * @param parent the source model where the datatype should be created
	 * @param uri the URI of the datatype (may be null for an anonymous datatype)
	 */
	public DatatypeImpl(SourceModel parent, URI uri) {
		if (uri != null) {
			setURI(uri);
			
			setSourceModel(parent);
		}
		else {
			String anonId = ModelUtils.generateBNodeId();
			setURI(URI.create(anonId));
			setRdfId(new BNodeKey(anonId));
			setSourceModel(parent);
			
			// TODO: resolve when do we add an rdf:type rdfs:Datatype statement
			// (e.g., this may be unsuitable to add when the datatype is one of the XSD standard datatypes)
			parent.getModel().add(createRdfTypeStatement(URI.create(RDFS.Datatype.getURI())));
		}			
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
    public void addOneOf(Collection<SSWAPLiteral> oneOf) {
		Model model = assertModel();
		List<RDFNode> oneOfNodes = new LinkedList<RDFNode>();
		
		for (SSWAPLiteral literal : oneOf) {			
			oneOfNodes.add(ImplFactory.get().createRDFNode(getSourceModel(), literal));
		}
		
		RDFList rdfList = model.createList(oneOfNodes.iterator());
		
		model.add(getResource(), OWL.oneOf, rdfList);
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
	 * Gets the corresponding Jena resource for this datatype. This method lazily initializes the "resource" field of this
	 * datatype.
	 * 
	 * @return Jena Resource of this datatype
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
	
	@Override
	public boolean equals(Object o) {
		// the following is for performance reasons
		if (this == o) {
			return true;
		}

		// the only equivalent objects are ModelImpls or their subclasses
		// (NOTE: instanceof returns false for nulls)
		if (o instanceof DatatypeImpl) {
			return rdfIdEquals((DatatypeImpl) o);			
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
}
