/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.net.URI;

import com.clarkparsia.empire.SupportsRdfId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPList;
import info.sswap.api.model.SSWAPLiteral;

/**
 * Implements a SSWAPElement (an element in SSWAP other than an RDF data source like a PDG or a canonical/protocol graph)
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public abstract class ElementImpl extends ModelImpl implements SSWAPElement {

	/**
	 * @inheritDoc
	 */
	public Boolean asBoolean() {
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Double asDouble() {
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPIndividual asIndividual() {
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Integer asInteger() {
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPList asList() {
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public String asString() {
		return null;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPLiteral asLiteral() {
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isIndividual() {
		return false;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isList() {
		return false;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isLiteral() {
		return false;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isAnonymous() {
		return ((getRdfId() != null) && ((getRdfId() instanceof SupportsRdfId.BNodeKey) || ModelUtils.isBNodeURI(getRdfId().toString())));
	}

	/**
	 * Retrieves a corresponding Jena resource for this element from a Jena model
	 * 
	 * @param model
	 *            the model that should contain information about this element
	 * @return the corresponding Jena resource or null, if there is no such a resource
	 */
	protected Resource getJenaResource(Model model) {
		// ensure proper handling of BNode elements
		/*
		if (isBlankNode()) {
			String bnodeId = this.getRdfId().value().toString();

			if (bnodeId != null) {
				return model.createResource(new AnonId(bnodeId));
			}
		}
		else {
		*/
			URI uri = getURI();

			if (uri != null) {
				return model.getResource(uri.toString());
			}
		//}

		return null;
	}
		 
    /**
     * @inheritDoc
     */
    public void addLabel(String label) {		
		Model model = assertModel();
		
		model.add(getJenaResource(model), RDFS.label, model.createTypedLiteral(label));
    }
    
    /**
     * @inheritDoc
     */
    public void addComment(String comment) {		
		Model model = assertModel();
		
		model.add(getJenaResource(model), RDFS.comment, model.createTypedLiteral(comment));
    }
    	  
    /**
     * @inheritDoc
     */
    public String getLabel() {		
		Model model = assertModel();
		
		Statement stmt = getJenaResource(model).getProperty(RDFS.label);
		return stmt == null ? null : stmt.getObject().isLiteral() ? stmt.getString() : stmt.getObject().toString();
    }
	  
    /**
     * @inheritDoc
     */
    public String getComment() {		
		Model model = assertModel();
		Statement stmt = getJenaResource(model).getProperty(RDFS.comment);
		return stmt == null ? null : stmt.getObject().isLiteral() ? stmt.getString() : stmt.getObject().toString();
    }
}
