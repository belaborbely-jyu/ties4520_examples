/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static info.sswap.api.input.InputFactory.createAtomicInput;
import static info.sswap.api.input.InputFactory.createEnumeratedInput;
import static info.sswap.api.input.InputFactory.createIntersectionInput;
import static info.sswap.api.input.InputFactory.createLiteral;
import static info.sswap.api.input.InputFactory.createPropertyInput;
import static info.sswap.api.input.InputFactory.createURI;
import static info.sswap.api.input.InputFactory.createUnionInput;
import static info.sswap.api.input.InputFactory.createUnrestricedInput;
import static org.junit.Assert.assertEquals;
import info.sswap.api.input.Input;
import info.sswap.api.input.Inputs;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.UnionInput;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.model.ModelUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Tests for inputs.
 * 
 * @author Evren Sirin
 */
public class InputSSWAPTests extends AbstractInputTests {
	protected void assertEqualsWithoutLabels(Input expected, Input actual) {
		assertEquals(expected, withoutLabels(actual));
	}
	
	protected Input withoutLabels(Input input) {
		if (ModelUtils.isBNodeURI(input.getLabel())) {
			input.setLabel(null);
		}
		return input;
	}
	
	/**
	 * Tests named types
	 */
	@Test
	public void testNamed() {
		Input actual = Inputs.fromSSWAP(A);
		Input expected = createAtomicInput(A.getURI());

		assertEquals(expected, actual);
	}

	/**
	 * Tests label
	 */
	@Test
	public void testLabel() {
		A.addLabel("label");
		
		Input actual = Inputs.fromSSWAP(A);
		Input expected = createAtomicInput(A.getURI());
		expected.setLabel("label");

		assertEquals(expected, actual);
	}

	/**
	 * Tests description
	 */
	@Test
	public void testDescription() {
		A.addComment("description");
		
		Input actual = Inputs.fromSSWAP(A);
		Input expected = createAtomicInput(A.getURI());
		expected.setDescription("description");

		assertEquals(expected, actual);
	}
	
	/**
	 * Tests intersection types
	 */
	@Test
	public void testIntersectionOf() {
		SSWAPType intersection = A.intersectionOf(B);

		Input actual = Inputs.fromSSWAP(intersection);
		Input expected = createIntersectionInput(createAtomicInput(A.getURI()), createAtomicInput(B.getURI()));
		System.out.println(Inputs.toJSONString(actual));
		System.out.println(Inputs.toJSONString(expected));
		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests union types
	 */
	@Test
	public void testUnionOf() {
		SSWAPType union = A.unionOf(B);

		Input actual = Inputs.fromSSWAP(union);
		UnionInput expected = createUnionInput(createAtomicInput(A.getURI()), createAtomicInput(B.getURI()));
		expected.setValueType(0, A.getURI());
		expected.setValueType(1, B.getURI());
		
		assertEquals(expected, withoutLabels(actual));
	}
	
	@Test
	public void testUnionOfValueTypes() {
		UnionInput union = (UnionInput) Inputs.fromSSWAP(A.unionOf(B));
		Set<URI> actual = Sets.newHashSet(union.getValueType(0), union.getValueType(1));
		Set<URI> expected = Sets.newHashSet(A.getURI(), B.getURI());
		assertEquals(expected, actual);
	}

	/**
	 * Tests enumerated types
	 */
	@Test
	public void testOneOf() {
		SSWAPType enumeration = DOC.createAnonymousType();
		enumeration.addOneOf(Arrays.asList(a.getURI(), b.getURI()));

		Input actual = Inputs.fromSSWAP(enumeration);
		Input expected = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests enumerated types
	 */
	@Test
	public void testIntersectionOneOf() {
		SSWAPType enumeration = DOC.createAnonymousType();
		enumeration.addOneOf(Arrays.asList(a.getURI(), b.getURI()));
		
		SSWAPType someValuesRestriction = DOC.createAnonymousType();
		someValuesRestriction.addRestrictionSomeValuesFrom(P, B);
		
		SSWAPType intersection = someValuesRestriction.intersectionOf(enumeration);

		Input actual = Inputs.fromSSWAP(intersection);
		Input expected = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests intersection types
	 */
	@Test
	public void testSubClassOf() {
		A.addSubClassOf(B);

		Input actual = Inputs.fromSSWAP(A);
		Input expected = createUnrestricedInput();
		expected.setLabel("A");

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests some values restrictions
	 */
	@Test
	public void testSomeValues() {
		SSWAPType someValuesRestriction = DOC.createAnonymousType();
		someValuesRestriction.addRestrictionSomeValuesFrom(P, B);

		A.addSubClassOf(someValuesRestriction);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests a recursive some values restriction
	 */
	@Test
	public void testSomeValuesRecursive() {
		SSWAPType someValuesRestriction = DOC.createAnonymousType();
		someValuesRestriction.addRestrictionSomeValuesFrom(P, A);

		A.addSubClassOf(someValuesRestriction);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(A.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests a recursive some values restriction
	 */
	@Test
	public void testSomeValuesIndirectRecursive() {
		SSWAPType someValuesRestriction = DOC.createAnonymousType();
		someValuesRestriction.addRestrictionSomeValuesFrom(P, B);

		A.addSubClassOf(someValuesRestriction);
		B.addSubClassOf(A);

		Input actual = Inputs.fromSSWAP(A);
		Input range = createAtomicInput(A.getURI());
		range.setLabel("B");
		PropertyInput expected = createPropertyInput(P.getURI(), range);
		expected.setMinCardinality(1);

		assertEquals(expected, withoutLabels(actual));
	}
	
	/**
	 * Tests all values restriction
	 */
	@Test
	public void testAllValues() {
		SSWAPType allValuesRestriction = DOC.createAnonymousType();
		allValuesRestriction.addRestrictionAllValuesFrom(P, B);
		A.addSubClassOf(allValuesRestriction);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(0);
		expected.setRange(createAtomicInput(B.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests min cardinality
	 */
	@Test
	public void testMinCardinality() {
		SSWAPType minCardinalityRestriction = DOC.createAnonymousType();
		minCardinalityRestriction.addRestrictionMinCardinality(P, 1);

		A.addSubClassOf(minCardinalityRestriction);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createUnrestricedInput());

		assertEquals(expected, withoutLabels(actual));
	}
	
	/**
	 * Tests max cardinality
	 */
	@Test
	public void testMaxCardinality() {
		SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
		maxCardinalityRestriction.addRestrictionMaxCardinality(P, 1);

		A.addSubClassOf(maxCardinalityRestriction);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMaxCardinality(1);
		expected.setRange(createUnrestricedInput());

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests max cardinality
	 */
	@Test
	public void testExactCardinality() {
		SSWAPType exactCardinalityRestriction = DOC.createAnonymousType();
		exactCardinalityRestriction.addRestrictionCardinality(P, 1);

		A.addSubClassOf(exactCardinalityRestriction);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setMaxCardinality(1);
		expected.setRange(createUnrestricedInput());

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests intersection of some values restriction with max cardinality
	 */
	@Test
	public void testSomeValuesDataOneOf() {
		// workaround to create someValuesFrom restriction on datatype property
		Model model = ExtensionAPI.asJenaModel(DOC);
		Resource resourceA = model.getResource(A.getURI().toString());
		Resource resourceP = model.getResource(P.getURI().toString());
		Resource resourceD = model.createResource();
		Resource someValuesRestriction = model.createResource();
		model.add(resourceA, RDFS.subClassOf, someValuesRestriction);
		model.add(someValuesRestriction, RDF.type, OWL.Restriction);
		model.add(someValuesRestriction, OWL.onProperty, resourceP);
		model.add(someValuesRestriction, OWL.someValuesFrom, resourceD);
		model.add(resourceD, RDF.type, RDFS.Datatype);
		model.add(resourceD,
		          OWL.oneOf,
		          model.createList(new RDFNode[] { model.createLiteral(l1.asString()),
		                          model.createTypedLiteral(l2.asInteger()) }));

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createEnumeratedInput(createLiteral(l1.asString()),
		                                        createLiteral(l2.asString(), XSD_INT.getURI())));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests intersection of some values restriction with max cardinality
	 */
	@Test
	public void testIntersectionOfSomeValuesMaxCardinality() {
		SSWAPType someValuesRestriction = DOC.createAnonymousType();
		someValuesRestriction.addRestrictionSomeValuesFrom(P, B);

		SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
		maxCardinalityRestriction.addRestrictionMaxCardinality(P, 1);

		A.addIntersectionOf(Arrays.asList(someValuesRestriction, maxCardinalityRestriction));

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setMaxCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests intersection of all values restriction with min cardinality
	 */
	@Test
	public void testIntersectionOfAllValuesMinCardinality() {
		SSWAPType allValuesRestriction = DOC.createAnonymousType();
		allValuesRestriction.addRestrictionAllValuesFrom(P, B);

		SSWAPType minCardinalityRestriction = DOC.createAnonymousType();
		minCardinalityRestriction.addRestrictionMinCardinality(P, 1);

		A.addIntersectionOf(Arrays.asList(allValuesRestriction, minCardinalityRestriction));

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests combination of property range with min cardinality
	 */
	@Test
	public void testRangeMinCardinality() {
		SSWAPType minCardinalityRestriction = DOC.createAnonymousType();
		minCardinalityRestriction.addRestrictionMinCardinality(P, 1);

		A.addSubClassOf(minCardinalityRestriction);
		P.addRange(B);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests combination of property range with max cardinality
	 */
	@Test
	public void testRangeMaxCardinality() {
		SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
		maxCardinalityRestriction.addRestrictionMaxCardinality(P, 1);

		A.addSubClassOf(maxCardinalityRestriction);
		P.addRange(D);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMaxCardinality(1);
		expected.setRange(createAtomicInput(D.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests combination of property range with min and max cardinality
	 */
	@Test
	public void testRangeMinMaxCardinality() {
		SSWAPType minCardinalityRestriction = DOC.createAnonymousType();
		minCardinalityRestriction.addRestrictionMinCardinality(P, 1);

		SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
		maxCardinalityRestriction.addRestrictionMaxCardinality(P, 1);

		A.addSubClassOf(minCardinalityRestriction);
		A.addSubClassOf(maxCardinalityRestriction);
		P.addRange(B);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setMaxCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}
	/**
	 * Tests using a min cardinality restriction on a  property whose range is a data enumeration
	 */
	@Test
	public void testMinCardinalityRangeDataOneOf() {
		Model model = ExtensionAPI.asJenaModel(DOC);
		Resource resourceA = model.getResource(A.getURI().toString());
		Resource resourceP = model.getResource(P.getURI().toString());
		Resource resourceD = model.createResource();
		Resource someValuesRestriction = model.createResource();
		model.add(resourceA, RDFS.subClassOf, someValuesRestriction);
		model.add(someValuesRestriction, RDF.type, OWL.Restriction);
		model.add(someValuesRestriction, OWL.onProperty, resourceP);
		model.add(someValuesRestriction, OWL.minCardinality, model.createTypedLiteral(1));
		model.add(resourceP, RDFS.range, resourceD);
		model.add(resourceD, RDF.type, RDFS.Datatype);
		model.add(resourceD,
		          OWL.oneOf,
		          model.createList(new RDFNode[] { model.createLiteral(l1.asString()),
		                          model.createTypedLiteral(l2.asInteger()) }));

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createEnumeratedInput(createLiteral(l1.asString()),
		                                        createLiteral(l2.asString(), XSD_INT.getURI())));

		assertEquals(expected, withoutLabels(actual));
	}

	/**
	 * Tests combination of property range with max cardinality
	 */
	@Test
	public void testCyclicDomainSomeValuesFrom() {
		SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
		maxCardinalityRestriction.addRestrictionMaxCardinality(P, 1);

		A.addSubClassOf(maxCardinalityRestriction);
		P.addRange(D);

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMaxCardinality(1);
		expected.setRange(createAtomicInput(D.getURI()));

		assertEquals(expected, withoutLabels(actual));
	}
	
	@Test
	public void testRestrictionLabels() {
		SSWAPType someValuesRestriction = DOC.createAnonymousType();
		someValuesRestriction.addRestrictionSomeValuesFrom(P, A);

		B.addSubClassOf(someValuesRestriction);
		A.addLabel("Type A");
		P.addLabel("Property P");

		Input actual = Inputs.fromSSWAP(B);
		Input range = createAtomicInput(A.getURI());
		range.setLabel("Type A");
		PropertyInput expected = createPropertyInput(P.getURI(), range);
		expected.setMinCardinality(1);
		expected.setLabel("Property P");

		assertEquals(expected, actual);
	}
	
	@Test
	public void testEnumerationLabels() {
		A.addOneOf(Arrays.asList(a.getURI(), b.getURI()));		
		A.addLabel("Type A");
		
		SSWAPType someValuesRestriction = DOC.createAnonymousType();
		someValuesRestriction.addRestrictionSomeValuesFrom(P, A);
		B.addSubClassOf(someValuesRestriction);

		Input actual = Inputs.fromSSWAP(B);
		Input range = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));
		range.setLabel("Type A");
		PropertyInput expected = createPropertyInput(P.getURI(), range);
		expected.setMinCardinality(1);

		assertEquals(expected, actual);
	}

	@Test
	public void testRedundantMinCardinality() {
		int cardinality = 10;
		for (int i = 0; i <= cardinality; i++) {
			SSWAPType minCardinalityRestriction = DOC.createAnonymousType();
			minCardinalityRestriction.addRestrictionMinCardinality(P, i);

			A.addSubClassOf(minCardinalityRestriction);	        
        }

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(cardinality);

		assertEquals(expected, withoutLabels(actual));
	}

	@Test
	public void testRedundantMaxCardinality() {
		int cardinality = 10;
		for (int i = 1; i <= cardinality; i++) {
			SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
			maxCardinalityRestriction.addRestrictionMaxCardinality(P, i);

			A.addSubClassOf(maxCardinalityRestriction);	        
        }

		Input actual = Inputs.fromSSWAP(A);
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMaxCardinality(1);

		assertEquals(expected, withoutLabels(actual));
	}
	

	
	@Test
	public void testLabelInheritance() {
		SSWAPType C = DOC.getType(URI.create(NS + "C"));
		SSWAPType D = DOC.getType(URI.create(NS + "D"));
		SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
		maxCardinalityRestriction.addRestrictionMaxCardinality(P, 1);
		
		A.addLabel("Label A");
		A.addComment("Comment A");
		A.addSubClassOf(C.unionOf(D));

		SSWAPType intersection = A.intersectionOf(maxCardinalityRestriction);	

		PropertyInput propInput = createPropertyInput(P.getURI());
		propInput.setMaxCardinality(1);
		UnionInput union = createUnionInput(createAtomicInput(C.getURI()), createAtomicInput(D.getURI()));
		union.setLabel(A.getLabel());
		union.setDescription(A.getComment());
		union.setValueType(0, C.getURI());
		union.setValueType(1, D.getURI());
		Input expected = createIntersectionInput(union, propInput);
		expected.setLabel(A.getLabel());
		expected.setDescription(A.getComment());
		
		Input actual = Inputs.fromSSWAP(intersection);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testUnionValueIndex() {
		SSWAPIndividual ind = DOC.createIndividual();		
		ind.addType(A);
		ind.addType(B);
		A.addUnionOf(Arrays.asList(B, C));
		
		Input actual = Inputs.fromSSWAP(ind);
		UnionInput expected = createUnionInput(createAtomicInput(B.getURI()), createAtomicInput(C.getURI()));
		expected.setLabel("A");
		expected.setValueType(0, B.getURI());
		expected.setValueType(1, C.getURI());
		expected.setValueIndex(0);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testUnionValueSubTypes() {
		SSWAPIndividual ind = DOC.createIndividual();		
		ind.addType(A);
		ind.addType(B);
		A.addUnionOf(Arrays.asList(B, C));

		SSWAPType maxCardinalityRestriction = DOC.createAnonymousType();
		maxCardinalityRestriction.addRestrictionMaxCardinality(P, 1);
		B.addSubClassOf(maxCardinalityRestriction);
		DOC.serialize(System.out);
		Input actual = Inputs.fromSSWAP(ind);
		Input inputB = createPropertyInput(P.getURI(), createUnrestricedInput(), 0, 1);
		UnionInput expected = createUnionInput(inputB, createAtomicInput(C.getURI()));
		expected.setValueType(0, B.getURI());
		expected.setValueType(1, C.getURI());
		expected.setValueIndex(0);
		expected.setLabel("A");

		assertEquals(expected, withoutLabels(actual));
	}
}
