/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import static info.sswap.api.input.PropertyInput.DEFAULT_MAX;
import static info.sswap.api.input.PropertyInput.DEFAULT_MIN;
import info.sswap.api.input.AtomicInput;
import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.Input;
import info.sswap.api.input.InputFactory;
import info.sswap.api.input.InputValue;
import info.sswap.api.input.Inputs;
import info.sswap.api.input.IntersectionInput;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.UnionInput;
import info.sswap.api.model.SSWAPType;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.model.ModelUtils;
import info.sswap.impl.empire.model.ReasoningServiceImpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.URIUtils;
import org.mindswap.pellet.utils.iterator.MultiListIterator;

import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

import com.clarkparsia.pellet.utils.MultiMapUtils;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Creates {@link Input} objects from a given {@link SSWAPType} using several heuristics. The heuristics used include
 * flattening nested intersection and union elements, combining multiple restrictions on the same property under one
 * {@link PropertyInput}, and so on. Recursive class expressions are detected and an {@link AtomicInput} is created when
 * in such cases.
 * 
 * @author Evren Sirin
 */
public class SSWAPDeserializer implements InputDeserializer<SSWAPType> {
	protected static final Logger LOGGER = LogManager.getLogger(SSWAPDeserializer.class);
	
	private static final Predicate<Object> IS_ATOMIC = Predicates.instanceOf(AtomicInput.class);
	private static final Predicate<Object> IS_PROPERTY = Predicates.instanceOf(PropertyInput.class);
	private static final Predicate<Object> IS_ENUMERATED = Predicates.instanceOf(EnumeratedInput.class);
	private static final Predicate<Input> IS_UNRESTRICTED = new Predicate<Input>() {
		@Override
		public boolean apply(Input input) {
			return input.isUnrestricted();
		}
	};
	private static final Predicate<Input> HAS_LABEL = new Predicate<Input>() {
		@Override
		public boolean apply(Input input) {
			return !Strings.isNullOrEmpty(input.getLabel());
		}
	};

	private ReasoningServiceImpl reasoningService;
	private KnowledgeBase kb;
	private SSWAPDeserializer.VisitedTerms visitedTerms;
	private Map<ATermAppl, Set<ATermAppl>> cachedDefinitions;

	public Input deserialize(SSWAPType type) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Create input from type " + type);
		
		reasoningService = ((ReasoningServiceImpl) type.getReasoningService());
		reasoningService.resetKB();
		kb = reasoningService.getPelletKB();
		visitedTerms = new VisitedTerms();
		
		if (LOGGER.isDebugEnabled()) {
			for (ATermAppl axiom : kb.getTBox().getAxioms()) {
				LOGGER.debug(ATermUtils.toString(axiom));
			}
		}
		
		cacheDefinitions();
		
//		reasoningService.getOntModel().getRawModel().write(System.out, "TTL", "TTL");

		ATermAppl cls = ATermUtils.makeTermAppl(type.getURI().toString());
		Input result = visit(cls);
		
		return result;
	}
	
	private void cacheDefinitions() {
		cachedDefinitions = new HashMap<ATermAppl, Set<ATermAppl>>();
		for (ATermAppl axiom : kb.getTBox().getAxioms()) {
			boolean isEquivalent = axiom.getAFun().equals(ATermUtils.EQCLASSFUN);
			ATermAppl c1 = (ATermAppl) axiom.getArgument(0);
			ATermAppl c2 = (ATermAppl) axiom.getArgument(1);
			
			if (ATermUtils.isPrimitive(c1)) {
				MultiMapUtils.add(cachedDefinitions, c1, c2);
			}
			
			if (isEquivalent && ATermUtils.isPrimitive(c2)) {
				MultiMapUtils.add(cachedDefinitions, c2, c1);
			}
		}		
	}

	private enum VisitDepth {
		NONE, CURRENT, PREVIOUS
	}

	private static class VisitedTerms {
		private List<Set<ATermAppl>> visitedByDepth = Lists.newArrayList();

		private VisitedTerms() {
		}

		int depth() {
			return visitedByDepth.size();
		}

		void increaseDepth() {
			visitedByDepth.add(Sets.<ATermAppl> newHashSet());
		}

		void decreaseDepth() {
			visitedByDepth.remove(depth() - 1);
		}

		SSWAPDeserializer.VisitDepth add(final ATermAppl term) {
			final int curr = depth() - 1;
			
			if (!visitedByDepth.get(curr).add(term)) {
				return VisitDepth.CURRENT;
			}
			
			for (int i = 0; i < curr; i++) {
				if (visitedByDepth.get(i).contains(term)) {
					return VisitDepth.PREVIOUS;
				}
			}

			return VisitDepth.NONE;
		}
	}
	
	private void setLabelDescription(Input input, ATermAppl term) {
		try {
			if (ATermUtils.isTop(term)) {
				return;
			}
			
	        String uri = term.getName();
	        Resource element = reasoningService.getOntModel().getResource(uri);

	        Statement label = element.getProperty(RDFS.label);
	        if (label != null) {
	        	input.setLabel(label.getString());
	        }
	        else if (!ModelUtils.isBNodeURI(uri)) {
	        	input.setLabel(URIUtils.getLocalName(uri));
	        }
	        else {
	        	input.setLabel("");
	        }
	        
	        Statement description = element.getProperty(RDFS.comment);
	        if (description != null) {
	        	input.setDescription(description.getString());
	        }
        }
        catch (Exception e) {
	        LOGGER.warn("Cannot get the label or description for  term " + term, e);
        }
	}	
	
	private boolean isRestriction(ATermAppl cls) {
		return ATermUtils.isSomeValues(cls)
		       || ATermUtils.isAllValues(cls)
		       || ATermUtils.isMin(cls)
		       || ATermUtils.isMax(cls)
		       || ATermUtils.CARDFUN.equals(cls.getAFun());
	}

	private Input visit(ATermAppl cls) {
		List<Input> inputs = Lists.newArrayList();
		visitedTerms.increaseDepth();
		visit(cls, inputs, false);
		visitedTerms.decreaseDepth();

		Input result = createInputFromList(inputs);

		if (ATermUtils.isPrimitive(cls) && !ModelUtils.isBNodeURI(cls.getName()) && !(result instanceof PropertyInput)) {
			setLabelDescription(result, cls);
		}

		if (isRestriction(cls)) {
			ATermAppl p = (ATermAppl) cls.getArgument(0);
			setLabelDescription(result, p);		
		}
		
		return result;
	}

	private Input createInputFromList(List<Input> inputs) {
		// if there is an enumeration of values in the input list, it overrides other options
		Input input = Iterables.find(inputs, IS_ENUMERATED, null);
		if (input != null) {
			return input;
		}

		// remove all unrestricted inputs as they are redundant
		Iterables.removeIf(inputs, IS_UNRESTRICTED);
		
		// filter the inputs in the list with labels but don't include property inputs
		Iterable<Input> labelInputs = Iterables.filter(inputs, Predicates.and(HAS_LABEL, Predicates.not(IS_PROPERTY)));
		
		// if there are non-atomic inputs in the list we want to remove non-atomic inputs
		// if all the inputs are atomic, leave them all
		boolean containsNonAtomicInput = Iterables.any(inputs, Predicates.not(IS_ATOMIC));
		if (containsNonAtomicInput) {
			Iterables.removeIf(inputs, IS_ATOMIC);
		}

		// create an intersection or singleton base don how many inputs are left
		int size = inputs.size();
		if (size == 0) {
			input = InputFactory.createUnrestricedInput();
		}
		else if (size == 1) {
			input = inputs.get(0);
		}
		else {
			input = InputFactory.createIntersectionInput(inputs);
		}
		
		// if the resulting input does not have a label only one of the elements in the
		// intersection had label, inherit that label and description
		if (!HAS_LABEL.apply(input) && Iterables.size(labelInputs) == 1) {
			copyLabelAndDescription(Iterables.getOnlyElement(labelInputs), input);
		}
		
		return input;
	}
	
	private void copyLabelAndDescription(Input source, Input target) {
		target.setLabel(source.getLabel());
		target.setDescription(source.getDescription());
	}

	private void visit(ATermAppl cls, List<Input> inputs, boolean isSuperClass) {
		SSWAPDeserializer.VisitDepth visitDepth = visitedTerms.add(cls);
		if (visitDepth != VisitDepth.NONE) {
			if (visitDepth == VisitDepth.PREVIOUS && ATermUtils.isPrimitive(cls)) {
				visitNamedClass(cls, ImmutableList.<ATermAppl> of(), inputs, false);
			}
			return;
		}

		if (ATermUtils.isPrimitive(cls)) {
			visitNamedClass(cls, inputs, isSuperClass);
		}
		else if (ATermUtils.isOneOf(cls)) {
			visitOneOf(argIterator(cls), inputs);
		}
		else if (ATermUtils.isNominal(cls)) {
			visitOneOf(Iterators.singletonIterator(cls), inputs);
		}
		else if (ATermUtils.isAnd(cls)) {
			visitIntersectionOf(argIterator(cls), inputs);
		}
		else if (ATermUtils.isOr(cls)) {
			visitUnionOf(argIterator(cls), inputs);
		}
		else if (ATermUtils.isSomeValues(cls)) {
			visitSomeValues(cls, inputs);
		}
		else if (ATermUtils.isAllValues(cls)) {
			visitAllValues(cls, inputs);
		}
		else if (ATermUtils.isMin(cls)) {
			visitMin(cls, inputs);
		}
		else if (ATermUtils.isMax(cls)) {
			visitMax(cls, inputs);
		}
		else if (ATermUtils.CARDFUN.equals(cls.getAFun())) {
			visitCard(cls, inputs);
		}
		else if (ATermUtils.isSelf(cls)) {
			// do nothing
		}
		else if (ATermUtils.isNot(cls)) {
			// do nothing
		}
		else {
			// mostly nominals which we do not validate/visit (should we?)
		}
	}

	private Iterator<ATermAppl> argIterator(ATermAppl term) {
		return new MultiListIterator((ATermList) term.getArgument(0));
	}

	private InputValue createValue(ATermAppl term) {
		if (ATermUtils.isPrimitive(term)) {
			return InputFactory.createURI(URI.create(term.getName()));
		}
		else if (ATermUtils.isBnode(term)) {
			return InputFactory.createBNode(((ATermAppl) term.getArgument(0)).getName());
		}
		else if (ATermUtils.isLiteral(term)) {
			String lexicalValue = ((ATermAppl) term.getArgument(0)).getName();
			ATermAppl lang = (ATermAppl) term.getArgument(1);
			ATermAppl datatype = (ATermAppl) term.getArgument(2);

			if (datatype.equals(ATermUtils.PLAIN_LITERAL_DATATYPE)) {
				if (lang.equals(ATermUtils.EMPTY))
					return InputFactory.createLiteral(lexicalValue);
				else
					return InputFactory.createLiteral(lexicalValue, lang.getName());
			}
			else {
				return InputFactory.createLiteral(lexicalValue, URI.create(datatype.getName()));
			}
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@SuppressWarnings("unchecked")
	private UnmodifiableIterator<PropertyInput> propertyInputs(List<Input> inputs, final URI prop) {
		return (UnmodifiableIterator) Iterators.filter(inputs.iterator(), new Predicate<Input>() {
			@Override
			public boolean apply(Input input) {
				return input instanceof PropertyInput && ((PropertyInput) input).getProperty().equals(prop);
			}
		});
	}

	private void visitNamedClass(ATermAppl namedClass, List<Input> inputs, boolean isSuperClass) {
		if (namedClass.toString().startsWith(Namespaces.SSWAP_NS)) {
			return;
		}
		
//		List<ATermAppl> supers = new ArrayList<ATermAppl>();
//
//		for (ATermAppl tboxAxiom : kb.getTBox().getAxioms(namedClass)) {
//			ATermAppl superClass = (ATermAppl) tboxAxiom.getArgument(1);
//			supers.add(superClass);
//		}

		Collection<ATermAppl> supers = MultiMapUtils.get(cachedDefinitions, namedClass);
				
		int inputSize = inputs.size();
		visitNamedClass(namedClass, supers, inputs, isSuperClass);
		if (inputSize + 1 == inputs.size()) {
			Input result = inputs.get(inputSize);
			if(!(result instanceof PropertyInput)) {
				setLabelDescription(result, namedClass);
			}
		}
	}

	private void visitNamedClass(ATermAppl namedClass, Collection<ATermAppl> supers, List<Input> inputs, boolean isSuperClass) {
		if (namedClass.toString().startsWith(Namespaces.SSWAP_NS) || namedClass.toString().startsWith(Namespaces.SSWAP_AGAVE_NS)) {
			return;
		}
		
		if (ATermUtils.TOP.equals(namedClass)) {
			inputs.add(InputFactory.createUnrestricedInput());
			return;
		}

		if (supers.isEmpty()) {
			Input input = InputFactory.createAtomicInput(URI.create(namedClass.getName()));
			if (isSuperClass) {
				inputs.remove(input);
			}
			else if (!inputs.contains(input)) {
				inputs.add(input);
			}
		}
		else {
			visitIntersectionOf(supers.iterator(), inputs, true);
		}
	}

	private void visitIntersectionOf(Iterator<ATermAppl> intersection, List<Input> inputs) {
		visitIntersectionOf(intersection, inputs, false);
	}
	
	private void visitIntersectionOf(Iterator<ATermAppl> intersection, List<Input> inputs, boolean isSuperClass) {	
		while (intersection.hasNext()) {
			ATermAppl cls = intersection.next();
			visit(cls, inputs, isSuperClass);
		}
	}

	private void visitUnionOf(Iterator<ATermAppl> union, List<Input> inputs) {
		List<Input> result = Lists.newArrayList();
		List<URI> valueTypes = Lists.newArrayList();
		while (union.hasNext()) {
			ATermAppl cls = union.next();
			Input singleInput = visit(cls);

			boolean namedClass = ATermUtils.isPrimitive(cls);			

			// flatten unions
			if (singleInput instanceof UnionInput) {
				UnionInput nestedUnion = (UnionInput) singleInput;
				List<Input> nestedInputs = nestedUnion.getInputs();
				for (int i = 0; i < nestedInputs.size(); i++) {
					Input nestedInput = nestedInputs.get(i);
					result.add(nestedInput);
					valueTypes.add(nestedUnion.getValueType(i));
				}
			}
			else if (singleInput != null) {
				if (!singleInput.isUnrestricted()) {
					result.add(singleInput);
					valueTypes.add(namedClass ? URI.create(cls.toString()) : null);
				}
			}
		}
		
		if (!result.isEmpty()) {
			UnionInput unionInput = InputFactory.createUnionInput(result);
			for (int i = 0; i < valueTypes.size(); i++) {
				URI valueType = valueTypes.get(i);
				unionInput.setValueType(i, valueType);
			}
			inputs.add(unionInput);
		}
	}

	private void visitOneOf(Iterator<ATermAppl> enumeration, List<Input> inputs) {
		List<InputValue> inputValues = Lists.newArrayList();
		while (enumeration.hasNext()) {
			ATermAppl val = (ATermAppl) enumeration.next().getArgument(0);
			inputValues.add(createValue(val));
		}

		EnumeratedInput input = InputFactory.createEnumeratedInput(inputValues);
		inputs.add(input);
	}

	private void addDomainRestrictions(ATermAppl p, List<Input> inputs) {
		Set<ATermAppl> domains = kb.getDomains(p);
		visitIntersectionOf(domains.iterator(), inputs);
	}

	private Input createRangeInput(ATermAppl p, Input additionalRange) {
		Role role = kb.getRole(p);

		Set<ATermAppl> ranges = new HashSet<ATermAppl>();

		if (role != null) {
			for (ATermAppl normalizedRange : role.getRanges()) {
				ATermAppl assertedRange = ATermUtils.nnf(normalizedRange);
				ranges.add(assertedRange);
			}
		}

		if (ranges.isEmpty()) {
			return additionalRange;
		}

		List<Input> inputs = Lists.newArrayList();
		if (!additionalRange.isUnrestricted()) {
			if (additionalRange instanceof IntersectionInput) {
				inputs.addAll(((IntersectionInput) additionalRange).getInputs());
			}
			else {
				inputs.add(additionalRange);
			}
		}

		visitedTerms.increaseDepth();
		visitIntersectionOf(ranges.iterator(), inputs);
		visitedTerms.decreaseDepth();

		return createInputFromList(inputs);
	}

	private void visitAllValues(ATermAppl allValues, List<Input> inputs) {
		ATermAppl p = (ATermAppl) allValues.getArgument(0);
		URI prop = URI.create(p.getName());
		Input range = visit((ATermAppl) allValues.getArgument(1));

		Iterator<PropertyInput> existingInputs = propertyInputs(inputs, prop);
		while (existingInputs.hasNext()) {
			PropertyInput existingInput = existingInputs.next();
			if (existingInput.getRange().equals(range)) {
				return;
			}
			if (existingInput.getRange().isUnrestricted()) {
				existingInput.setRange(range);
				return;
			}
		}

		Input input = InputFactory.createPropertyInput(prop, createRangeInput(p, range), DEFAULT_MIN, DEFAULT_MAX);
		setLabelDescription(input, p);	
		inputs.add(input);
	}

	private void visitSomeValues(ATermAppl someValues, List<Input> inputs) {
		ATermAppl p = (ATermAppl) someValues.getArgument(0);
		ATermAppl type = (ATermAppl) someValues.getArgument(1);
		
		visitMin(p, 1, type, inputs);
	}

	private void visitMin(ATermAppl min, List<Input> inputs) {
		ATermAppl p = (ATermAppl) min.getArgument(0);
		ATermAppl type = (ATermAppl) min.getArgument(2);
		int cardinality = ((ATermInt) min.getArgument(1)).getInt();
		
		visitMin(p, cardinality, type, inputs);
	}
	
	private void visitMin(ATermAppl p, int cardinality, ATermAppl type, List<Input> inputs) {		
		URI prop = URI.create(p.getName());
		Input range = visit(type);
		
		addDomainRestrictions(p, inputs);

		Iterator<PropertyInput> existingInputs = propertyInputs(inputs, prop);
		while (existingInputs.hasNext()) {
			PropertyInput existingInput = existingInputs.next();
			if (existingInput.getRange().isUnrestricted()
			    || range.isUnrestricted()
			    || existingInput.getRange().equals(range)) {
				if (existingInput.getRange().isUnrestricted()) {
					existingInput.setRange(range);
				}
				if (existingInput.getMinCardinality() < cardinality) {
					existingInput.setMinCardinality(cardinality);
				}				
				return;
			}
		}

		Input input = InputFactory.createPropertyInput(prop, createRangeInput(p, range), cardinality, DEFAULT_MAX);
		setLabelDescription(input, p);	
		inputs.add(input);
	}

	private void visitMax(ATermAppl max, List<Input> inputs) {
		ATermAppl p = (ATermAppl) max.getArgument(0);
		URI prop = URI.create(p.getName());
		Input range = visit((ATermAppl) max.getArgument(2));
		int cardinality = ((ATermInt) max.getArgument(1)).getInt();

		Iterator<PropertyInput> existingInputs = propertyInputs(inputs, prop);
		while (existingInputs.hasNext()) {
			PropertyInput existingInput = existingInputs.next();
			if (existingInput.getRange().isUnrestricted()
			    || range.isUnrestricted()
			    || existingInput.getRange().equals(range)) {
				if (existingInput.getRange().isUnrestricted()) {
					existingInput.setRange(range);
				}
				if (existingInput.getMaxCardinality() > cardinality) {
					existingInput.setMaxCardinality(cardinality);
				}				
				return;
			}
		}

		Input input = InputFactory.createPropertyInput(prop, createRangeInput(p, range), DEFAULT_MIN, cardinality);
		setLabelDescription(input, p);	
		inputs.add(input);
	}

	private void visitCard(ATermAppl card, List<Input> inputs) {
		ATermAppl p = (ATermAppl) card.getArgument(0);
		URI prop = URI.create(p.getName());
		Input range = visit((ATermAppl) card.getArgument(2));
		int cardinality = ((ATermInt) card.getArgument(1)).getInt();

		addDomainRestrictions(p, inputs);

		Iterator<PropertyInput> existingInputs = propertyInputs(inputs, prop);
		while (existingInputs.hasNext()) {
			PropertyInput existingInput = existingInputs.next();
			if (existingInput.getRange().isUnrestricted()
			    || range.isUnrestricted()
			    || existingInput.getRange().equals(range)) {
				if (existingInput.getRange().isUnrestricted()) {
					existingInput.setRange(range);
				}
				if (existingInput.getMinCardinality() < cardinality) {
					existingInput.setMinCardinality(cardinality);
				}	
				if (existingInput.getMaxCardinality() > cardinality) {
					existingInput.setMaxCardinality(cardinality);
				}
				return;
			}
		}

		Input input = InputFactory.createPropertyInput(prop, createRangeInput(p, range), cardinality, cardinality);
		setLabelDescription(input, p);	
		inputs.add(input);
	}
}
