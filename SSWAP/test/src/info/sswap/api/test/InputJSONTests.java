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

import java.net.URI;

import org.json.JSONObject;
import org.junit.Test;
import org.mindswap.pellet.utils.Namespaces;

/**
 * Tests for inputs.
 * 
 * @author Evren Sirin
 */
public class InputJSONTests extends AbstractInputTests {
	protected void testRoundTrip(Input expected) {
		JSONObject obj = Inputs.toJSON(expected);
		Input actual = Inputs.fromJSON(obj);

		assertEquals(expected, actual);
	}

	/**
	 * Tests named types
	 */
	@Test
	public void testNamed() {
		Input expected = createAtomicInput(A.getURI());

		testRoundTrip(expected);
	}

	/**
	 * Tests named label
	 */
	@Test
	public void testNamedLabel() {
		Input expected = createAtomicInput(A.getURI());
		expected.setLabel("label");
		
		testRoundTrip(expected);
	}

	/**
	 * Tests named description
	 */
	@Test
	public void testNamedDescription() {
		Input expected = createAtomicInput(A.getURI());
		expected.setDescription("descriptions are typically long");
		
		testRoundTrip(expected);
	}

	/**
	 * Tests union types
	 */
	@Test
	public void testIntersectionOf() {
		Input expected = createIntersectionInput(createAtomicInput(A.getURI()), createAtomicInput(B.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests union types
	 */
	@Test
	public void testUnionOf() {
		Input expected = createUnionInput(createAtomicInput(A.getURI()), createAtomicInput(B.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests union types
	 */
	@Test
	public void testUnionOfWithValueIndex() {
		UnionInput expected = createUnionInput(createAtomicInput(A.getURI()), createAtomicInput(B.getURI()));
		expected.setValueIndex(1);

		testRoundTrip(expected);
	}
	
	@Test
	public void testUnionOfWithValueIndexAndTypes() {
		Input inputA = createAtomicInput(A.getURI());
		Input inputB = createAtomicInput(B.getURI());
		UnionInput expected = createUnionInput(inputA, inputB, createIntersectionInput(inputA, inputB));
		expected.setValueType(0,  A.getURI());
		expected.setValueType(1,  B.getURI());
		expected.setValueIndex(1);

		testRoundTrip(expected);
	}

	/**
	 * Tests enumerated type
	 */
	@Test
	public void testOneOf() {
		Input expected = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests enumerated type with a value
	 */
	@Test
	public void testOneOfValue() {
		Input expected = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));
		expected.setValue(createURI(a.getURI()));
		
		testRoundTrip(expected);
	}

	/**
	 * Tests enumerated datatypes
	 */
	@Test
	public void testDataOneOf() {
		Input expected = createEnumeratedInput(createLiteral("1"), createLiteral("2", "en"),
		                                       createLiteral("3", URI.create(Namespaces.XSD + "int")));

		testRoundTrip(expected);
	}

	/**
	 * Tests intersection types
	 */
	@Test
	public void testSubClassOf() {
		Input expected = createAtomicInput(B.getURI());

		testRoundTrip(expected);
	}

	/**
	 * Tests some values restriction
	 */
	@Test
	public void testSomeValues() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests some values restriction with a value
	 */
	@Test
	public void testSomeValuesValue() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));
		expected.getRange().setValue(createLiteral("test"));

		testRoundTrip(expected);
	}

	/**
	 * Tests all values restriction
	 */
	@Test
	public void testAllValues() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(0);
		expected.setRange(createAtomicInput(B.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests min cardinality
	 */
	@Test
	public void testMinCardinality() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createUnrestricedInput());

		testRoundTrip(expected);
	}

	/**
	 * Tests max cardinality
	 */
	@Test
	public void testMaxCardinality() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMaxCardinality(1);
		expected.setRange(createUnrestricedInput());

		testRoundTrip(expected);
	}

	/**
	 * Tests max cardinality
	 */
	@Test
	public void testExactCardinality() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setMaxCardinality(1);
		expected.setRange(createUnrestricedInput());

		testRoundTrip(expected);
	}

	/**
	 * Tests intersection of some values restriction with max cardinality
	 */
	@Test
	public void testIntersectionOfSomeValuesMaxCardinality() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setMaxCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests intersection of all values restriction with min cardinality
	 */
	@Test
	public void testIntersectionOfAllValuesMinCardinality() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests combination of property range with min cardinality
	 */
	@Test
	public void testRangeMinCardinality() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.setRange(createAtomicInput(B.getURI()));

		testRoundTrip(expected);
	}

	/**
	 * Tests combination of property range with max cardinality
	 */
	@Test
	public void testRangeMaxCardinality() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMaxCardinality(1);
		expected.setRange(createAtomicInput(D.getURI()));

		testRoundTrip(expected);
	}
}
