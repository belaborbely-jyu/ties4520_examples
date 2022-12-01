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
import static org.junit.Assert.assertEquals;
import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.Input;
import info.sswap.api.input.InputValidator;
import info.sswap.api.input.IntersectionInput;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.UnionInput;

import java.net.URI;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Tests for Jena serialization of input values.
 * 
 * @author Evren Sirin
 */
public class InputValidationTests extends AbstractInputTests {
	protected void testValidation(Input input, URI... properties) {
		InputValidator validator = new InputValidator();
		Set<URI> expected = Sets.newHashSet(properties);
		Set<URI> actual = validator.getMissingProperties(input);
		
		assertEquals(expected, actual);
	}

	protected Statement stmt(Resource s, Property p, RDFNode o) {
		return ResourceFactory.createStatement(s, p, o);
	}

	protected Property prop(URI uri) {
		return ResourceFactory.createProperty(uri.toString());
	}

	protected Resource uri(URI uri) {
		return ResourceFactory.createResource(uri.toString());
	}

	protected Literal lit(String label) {
		return ResourceFactory.createPlainLiteral(label);
	}
	
	/**
	 * Tests atomic type
	 */
	@Test
	public void testAtomic() {
		Input expected = createAtomicInput(A.getURI());

		testValidation(expected);
	}

	/**
	 * Tests named types
	 */
	@Test
	public void testAtomicValue() {
		Input expected = createAtomicInput(A.getURI());
		expected.setValue(createLiteral("test"));
		
		testValidation(expected);
	}

	@Test
	public void testOneOf() {
		Input expected = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));
		expected.setValue(createURI(a.getURI()));
		
		testValidation(expected);
	}

	@Test
	public void testPropertyValueNotRequired() {
		PropertyInput expected = createPropertyInput(P.getURI());

		testValidation(expected);
	}

	@Test
	public void testPropertyValueRequired() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		expected.getRange().setValue(createLiteral("test"));

		testValidation(expected);
	}

	@Test
	public void testPropertyValueRequiredMissing() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.setMinCardinality(1);
		
		testValidation(expected, P.getURI());
	}

	@Test
	public void testIntersectionOfPropertyValues() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.setMinCardinality(1);
		p1.getRange().setValue(createLiteral("val1"));
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.setMinCardinality(1);
		
		IntersectionInput input = createIntersectionInput(p1, p2);

		testValidation(input, R.getURI());
	}

	@Test
	public void testIntersectionOfPropertyValuesBothMissing() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.setMinCardinality(1);
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.setMinCardinality(1);
		
		IntersectionInput input = createIntersectionInput(p1, p2);

		testValidation(input, P.getURI(), R.getURI());
	}

	@Test
	public void testIntersectionOfPropertyValueAndEnumeration() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		
		EnumeratedInput enumerated = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));		
		PropertyInput p2 = createPropertyInput(R.getURI(), enumerated);
		p2.setMinCardinality(1);
		
		IntersectionInput input = createIntersectionInput(p1, p2);

		testValidation(input, R.getURI());
	}

	@Test
	public void testNestedPropertyValues1() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.setMinCardinality(1);
		
		PropertyInput p2 = createPropertyInput(R.getURI(), p1);

		testValidation(p2, P.getURI());
	}

	@Test
	public void testNestedPropertyValues2() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		
		PropertyInput p2 = createPropertyInput(R.getURI(), p1);
		p2.setMinCardinality(1);

		testValidation(p2);
	}

	@Test
	public void testNestedPropertyValues3() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.setMinCardinality(1);
		
		PropertyInput p2 = createPropertyInput(R.getURI(), p1);
		p2.setMinCardinality(1);

		testValidation(p2, P.getURI());
	}

	@Test
	public void testPropertyValueWithIntersection() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.setMinCardinality(1);
		p1.getRange().setValue(createLiteral("val1")); 
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.setMinCardinality(1);

		IntersectionInput intersection = createIntersectionInput(p1, p2);
		
		PropertyInput p3 = createPropertyInput(P.getURI(), intersection);
		
		testValidation(p3, R.getURI());
	}

	@Test
	public void testUnionOfPropertyValues1() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.setMinCardinality(1);
		p1.getRange().setValue(createLiteral("val1")); 
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.setMinCardinality(1);

		UnionInput union = createUnionInput(p1, p2);
		
		testValidation(union);
	}

	@Test
	public void testUnionOfPropertyValues2() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.setMinCardinality(1);
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.setMinCardinality(1);

		UnionInput union = createUnionInput(p1, p2);
		
		union.setValueIndex(0);		
		testValidation(union, P.getURI());
		
		union.setValueIndex(1);		
		testValidation(union, R.getURI());
	}

	@Test
	public void testUnionOfPropertyValues3() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		
		PropertyInput p2 = createPropertyInput(R.getURI());

		UnionInput union = createUnionInput(p1, p2);
		
		testValidation(union);
	}

	@Test
	public void testUnionOfNestedPropertyValues1() {
		PropertyInput p1a = createPropertyInput(P.getURI());		
		p1a.getRange().setValue(createLiteral("val")); 
		PropertyInput p2a = createPropertyInput(R.getURI(), p1a);

		PropertyInput p1b = createPropertyInput(R.getURI());
		PropertyInput p2b = createPropertyInput(P.getURI(), p1b);

		UnionInput union = createUnionInput(p2a, p2b);
		
		testValidation(union);
	}

	@Test
	public void testUnionOfNestedPropertyValues2() {
		PropertyInput p1a = createPropertyInput(P.getURI());
		p1a.setMinCardinality(1);
		p1a.getRange().setValue(createLiteral("val")); 
		PropertyInput p2a = createPropertyInput(R.getURI(), p1a);

		PropertyInput p1b = createPropertyInput(R.getURI());
		PropertyInput p2b = createPropertyInput(P.getURI(), p1b);

		UnionInput union = createUnionInput(p2a, p2b);
		
		testValidation(union);
	}

	@Test
	public void testUnionOfNestedPropertyValues3() {
		PropertyInput p1a = createPropertyInput(P.getURI());
		p1a.setMinCardinality(1);
		PropertyInput p2a = createPropertyInput(R.getURI(), p1a);

		PropertyInput p1b = createPropertyInput(R.getURI());
		PropertyInput p2b = createPropertyInput(P.getURI(), p1b);

		UnionInput union = createUnionInput(p2a, p2b);
		union.setValueIndex(0);
		
		testValidation(union, P.getURI());
	}

	@Test
	public void testUnionOfNestedPropertyValues4() {
		PropertyInput p1a = createPropertyInput(P.getURI());
		PropertyInput p2a = createPropertyInput(R.getURI(), p1a);

		PropertyInput p1b = createPropertyInput(R.getURI());
		p1b.getRange().setValue(createLiteral("val")); 
		PropertyInput p2b = createPropertyInput(P.getURI(), p1b);

		UnionInput union = createUnionInput(p2a, p2b);
		union.setValueIndex(1);
		
		testValidation(union);
	}
}
