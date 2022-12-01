/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.AtomicInput;
import info.sswap.api.input.BNodeValue;
import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.Input;
import info.sswap.api.input.InputFactory;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.IntersectionInput;
import info.sswap.api.input.LiteralValue;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.URIValue;
import info.sswap.api.input.UnionInput;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPType;
import info.sswap.impl.empire.Namespaces;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Creates {@link Input} objects from a given {@link SSWAPIndividual}.
 * 
 * @author Evren Sirin
 */
public class SSWAPIndividualDeserializer implements InputDeserializer<SSWAPIndividual> {
	protected static final Logger LOGGER = LogManager.getLogger(SSWAPIndividualDeserializer.class);

	public Input deserialize(SSWAPIndividual ind) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Create input from individual " + ind);

		SSWAPDeserializer typeDeserializer = new SSWAPDeserializer();
		
		List<URI> unionTypes = Lists.newArrayList();
		Map<URI,Input> typeInputs = Maps.newHashMap();
		for (SSWAPType type : ind.getDeclaredTypes()) {
			Input typeInput = typeDeserializer.deserialize(type);
			if (!(typeInput instanceof AtomicInput)) {
				typeInputs.put(type.getURI(), typeInput);
				processUnions(ind, typeInput, unionTypes);
			}
		}
		
		typeInputs.keySet().removeAll(unionTypes);
		
		List<Input> inputs = Lists.newArrayList(typeInputs.values());

		PropertyInputVisitor propertyInputVisitor = new PropertyInputVisitor();
		for (Input typeInput : inputs) {
			typeInput.accept(propertyInputVisitor);
		}
		
		Map<URI,PropertyInput> typeProperties = propertyInputVisitor.getResult();		
		
		for (SSWAPProperty prop : ind.getProperties()) {
			if (prop.getPredicate().getURI().toString().startsWith(Namespaces.SSWAP_NS) || 
				(prop.getPredicate().getURI().toString().startsWith(Namespaces.SSWAP_AGAVE_NS) && !prop.getPredicate().getURI().toString().startsWith(Namespaces.SSWAP_AGAVE_NS + "job/")) ||
				prop.getPredicate().isAnnotationPredicate()) {
				continue;
			}

			PropertyInput input = deserialize(prop);
			
			URI propURI = input.getProperty();
			PropertyInput propInput = typeProperties.get(propURI);
			if (propURI != null && propInput != null) {				
				propInput.getRange().setValue(input.getRange().getValue());
			}
			else {
				inputs.add(input);
			}
		}		

		int size = inputs.size();
		
		Input result = null;
		
		if (size == 0) {
			result = InputFactory.createUnrestricedInput();		
		}
		else if (size == 1) {
			result = inputs.get(0);
		}
		else {
			result = InputFactory.createIntersectionInput(inputs);
		}
		
		if (!ind.isAnonymous()) {
			result.setValue(InputFactory.createURI(ind.getURI()));
		}
		
		return result;
	}
	
	private void processUnions(SSWAPIndividual ind, Input input, List<URI> unionTypes) {
		if (input instanceof UnionInput) {
			UnionInput union = (UnionInput) input;
			for (int i = 0, n = union.getInputs().size(); i < n; i++) {
				URI typeURI = union.getValueType(i);				
				if (typeURI == null) {
					continue;
				}
				for (SSWAPType type : ind.getDeclaredTypes()) {
					if (typeURI.equals(type.getURI())) {
						unionTypes.add(typeURI);
						union.setValueIndex(i);
						return;
					}
				}
			}
		}
		else if (input instanceof IntersectionInput) {
			for (Input child : ((IntersectionInput) input).getInputs()) {
				processUnions(ind, child, unionTypes);
			}
		}
	}

	protected PropertyInput deserialize(SSWAPProperty prop) {
		Input range;
		SSWAPPredicate pred = prop.getPredicate();
		SSWAPElement value = prop.getValue();
		URI individualURI = null;

		if (value.isLiteral()) {
			if (value.asLiteral().getDatatypeURI() != null) {
				range = InputFactory.createAtomicInput(value.asLiteral().getDatatypeURI());
				range.setValue(InputFactory.createLiteral(value.asLiteral().asString(), value.asLiteral().getDatatypeURI()));
			}
			else if (value.asLiteral().getLanguage() != null) {
				range = InputFactory.createUnrestricedInput();
				range.setValue(InputFactory.createLiteral(value.asLiteral().asString(), value.asLiteral().getLanguage()));
			}
			else {
				range = InputFactory.createUnrestricedInput();
				range.setValue(InputFactory.createLiteral(value.asLiteral().asString()));
			}
		}
		else if (value.isIndividual()) {
			range = deserialize(value.asIndividual());
						
			if (!value.asIndividual().isAnonymous()) {
				individualURI = value.asIndividual().getURI();
			}
		}
		else {
			LOGGER.warn("RDF Lists not supported");
			range = InputFactory.createUnrestricedInput();
		}

		
		PropertyInput result = InputFactory.createPropertyInput(pred.getURI(), range);
		String label = pred.getLabel();

		if (label != null) {
			result.setLabel(label);
		}
		String desc = pred.getComment();
		if (desc != null) {
			result.setDescription(desc);
		}
		
		if (individualURI != null) {
			result.getRange().setValue(InputFactory.createURI(individualURI));
		}

		return result;
	}
	
	static class PropertyInputVisitor implements InputVisitor {

		private Map<URI,PropertyInput> result = new HashMap<URI,PropertyInput>();
		
		public Map<URI,PropertyInput> getResult() {
			return result;
		}
		
		@Override
        public void visit(AtomicInput input) {
        }

		@Override
        public void visit(IntersectionInput input) {
			for (Input child : input.getInputs()) {
				child.accept(this);
			}
        }

		@Override
        public void visit(UnionInput input) {
			for (Input child : input.getInputs()) {
				child.accept(this);
			}
		}

		@Override
        public void visit(EnumeratedInput input) {
        }

		@Override
        public void visit(PropertyInput input) {
			result.put(input.getProperty(), input);	        
        }

		@Override
        public void visit(URIValue value) {
        }

		@Override
        public void visit(BNodeValue value) {
        }

		@Override
        public void visit(LiteralValue value) {
        }		
	}
}
