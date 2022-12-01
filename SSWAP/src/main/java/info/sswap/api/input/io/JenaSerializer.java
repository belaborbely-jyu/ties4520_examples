/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.AtomicInput;
import info.sswap.api.input.BNodeValue;
import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.Input;
import info.sswap.api.input.InputValue;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.IntersectionInput;
import info.sswap.api.input.LiteralValue;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.URIValue;
import info.sswap.api.input.UnionInput;

import java.net.URI;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Creates a Jena model from the values of an {@link Input} object.
 * 
 * @author Evren Sirin
 */
public class JenaSerializer implements InputVisitor, InputSerializer<RDFNode> {
	private Model model;
	private RDFNode out;

	public JenaSerializer() {
	}

	public RDFNode serialize(Input input) {
		return serialize(input, ModelFactory.createDefaultModel());
	}

	public RDFNode serialize(Input input, Model model) {
		Resource out = null;
		
		if ((input.getValue() != null) && (input.getValue() instanceof URIValue)) {
			out = model.createResource(((URIValue) input.getValue()).getURI().toString());		
		}
		else {
			out = model.createResource();
		}
		
		return serialize(input, out);
	}
	
	public RDFNode serialize(Input input, Resource resource) {
		this.model = resource.getModel();
		this.out = resource;
		
		input.accept(this);
		
		return out;
	}

	private void outputValue(Input input) {
		InputValue inputValue = input.getValue();
		if (inputValue == null) {
			out = null;
		}
		else {
			inputValue.accept(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(AtomicInput input) {
		outputValue(input);
		
		if (out instanceof Resource && !input.isUnrestricted()) {
			String typeURI = input.getType().toString();
			Resource type = model.createResource(typeURI);
			model.add((Resource) out, RDF.type, type);
		}					
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(IntersectionInput intersection) {
		boolean allNull = true;
		Resource subj = (Resource) out;
		for (Input input : intersection.getInputs()) {
			input.accept(this);
			// if one of the intersections don't provide a value, ignore it and restore the previous value
			if (out == null) {
				out = subj;
			}
			else {
				allNull = false;
			}
		}
		
		if (allNull) {
			out = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(UnionInput union) {
		Resource subj = (Resource) out;
		int valueIndex = union.getValueIndex();
		if (valueIndex != -1) {
			Input input = union.getInputs().get(valueIndex);
			input.accept(this);
			
			URI valueType = union.getValueType(valueIndex);
			if (valueType != null) {
				if (out == null) {
					out = subj;
				}
				
				if (out instanceof Resource) {
					Resource type = model.createResource(valueType.toString());
					model.add((Resource) out, RDF.type, type);
				}
			}
		}
		else {
			out = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(EnumeratedInput input) {
		outputValue(input);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(PropertyInput input) {
		Resource subj = (Resource) out;
		Property prop = model.createProperty(input.getProperty().toString());
		
		out = model.createResource();
		input.getRange().accept(this);
		if (out != null) {
			model.add(subj, prop, out);			
			out = subj;			
		}		
		else {
			out = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(URIValue value) {
		out = model.createResource(value.getURI().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(BNodeValue value) {
		out = model.asRDFNode(Node.createAnon(new AnonId(value.getID())));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(LiteralValue value) {
		if (value.getDatatype() != null) {
			out = model.createTypedLiteral(value.getLabel(), value.getDatatype().toString());
		}
		else if (value.getLanguage() != null) {
			out = model.createLiteral(value.getLabel(), value.getLanguage());
		}
		else {
			out = model.createLiteral(value.getLabel());
		}
	}

}
