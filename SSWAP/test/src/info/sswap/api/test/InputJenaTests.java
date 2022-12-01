/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static info.sswap.api.input.InputFactory.createAtomicInput;
import static info.sswap.api.input.InputFactory.createEnumeratedInput;
import static info.sswap.api.input.InputFactory.createIntersectionInput;
import static info.sswap.api.input.InputFactory.createLiteral;
import static info.sswap.api.input.InputFactory.createPropertyInput;
import static info.sswap.api.input.InputFactory.createURI;
import static info.sswap.api.input.InputFactory.createUnionInput;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.Input;
import info.sswap.api.input.IntersectionInput;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.UnionInput;
import info.sswap.api.input.io.JenaSerializer;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Test;
import org.mindswap.pellet.utils.Namespaces;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Tests for Jena serialization of input values.
 * 
 * @author Evren Sirin
 */
public class InputJenaTests extends AbstractInputTests {
	protected static final Resource X = ResourceFactory.createResource();
	protected static final Resource Y = ResourceFactory.createResource();
	
	protected JenaSerializer serializer = new JenaSerializer();

	protected void testSerialization(Input input, RDFNode node, Statement... statements) {
		Model expected = ModelFactory.createDefaultModel();
		for (Statement statement : statements) {
	        expected.add(statement);
        }
		
		testSerialization(input, node, expected);
	}
	
	protected void testSerialization(Input input, RDFNode expectedNode, Model expectedModel) {
		RDFNode actualNode = serializer.serialize(input);
		
		if (expectedNode == null) {
			assertNull("Serialization result is not null " + actualNode, actualNode);
		}
		else {
			assertNotNull("Serialization result is null", actualNode);
			assertTrue("Nodes differ: " + expectedNode + " " + actualNode, actualNode.isAnon()
			                                                               && expectedNode.isAnon()
			                                                               || actualNode.equals(expectedNode));
			Model actualModel = actualNode.getModel();
			if (!expectedModel.isIsomorphicWith(actualModel)) {
				System.err.println("Expected:");
				expectedModel.write(System.err, "N-TRIPLES");
				System.err.println("Actual:");
				actualModel.write(System.err, "N-TRIPLES");
				fail("Models differ: " + expectedModel + "\n" + actualModel);
			}
		}
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

		testSerialization(expected, null);
	}

	/**
	 * Tests named types
	 */
	@Test
	public void testAtomicValue() {
		Input expected = createAtomicInput(A.getURI());
		expected.setValue(createLiteral("test"));
		
		testSerialization(expected, lit("test"));
	}
	
	@Test
	public void testAtomicURI() {
		Input expected = createAtomicInput(A.getURI());
		expected.setValue(createURI(a.getURI()));
		
		Resource res = uri(a.getURI());
		testSerialization(expected, res, stmt(res, RDF.type, uri(A.getURI())));
	}

	@Test
	public void testOneOf() {
		Input expected = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));
		expected.setValue(createURI(a.getURI()));
		
		testSerialization(expected, uri(a.getURI()));
	}

	@Test
	public void testDataOneOf() {
		Input expected = createEnumeratedInput(createLiteral("1"), createLiteral("2", "en"),
		                                       createLiteral("3", URI.create(Namespaces.XSD + "int")));
		expected.setValue(createLiteral("3", URI.create(Namespaces.XSD + "int")));
		
		testSerialization(expected, createTypedLiteral(3));
	}

	@Test
	public void testPropertyValue() {
		PropertyInput expected = createPropertyInput(P.getURI());
		expected.getRange().setValue(createLiteral("test"));

		testSerialization(expected, X, stmt(X, prop(P.getURI()), lit("test")));
	}

	@Test
	public void testIntersectionOfPropertyValues() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.getRange().setValue(createLiteral("val1"));
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.getRange().setValue(createLiteral("val2"));
		
		IntersectionInput input = createIntersectionInput(p1, p2);

		testSerialization(input, X, stmt(X, prop(P.getURI()), lit("val1")), stmt(X, prop(R.getURI()), lit("val2")));
	}

	@Test
	public void testIntersectionOfPropertyValueAndEnumeration() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.getRange().setValue(createLiteral("val1"));
		
		EnumeratedInput enumerated = createEnumeratedInput(createURI(a.getURI()), createURI(b.getURI()));
		enumerated.setValue(createURI(a.getURI()));
		
		PropertyInput p2 = createPropertyInput(R.getURI(), enumerated);
		
		IntersectionInput input = createIntersectionInput(p1, p2);

		testSerialization(input, X, stmt(X, prop(P.getURI()), lit("val1")), stmt(X, prop(R.getURI()), uri(a.getURI())));
	}

	@Test
	public void testNestedPropertyValues() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.getRange().setValue(createLiteral("val1"));
		
		PropertyInput p2 = createPropertyInput(R.getURI(), p1);

		testSerialization(p2, X, stmt(X, prop(R.getURI()), Y), stmt(Y, prop(P.getURI()), lit("val1")));
	}

	@Ignore("This test is incorrect, if there is no value set in th einput we should not generate any output")
	@Test
	public void testNestedPropertyEmptyValues() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		
		PropertyInput p2 = createPropertyInput(R.getURI(), p1);

		testSerialization(p2, X, stmt(X, prop(R.getURI()), Y));
	}

	@Test
	public void testPropertyValueWithIntersection() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		p1.getRange().setValue(createLiteral("val1")); 
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.getRange().setValue(createLiteral("val2")); 

		IntersectionInput intersection = createIntersectionInput(p1, p2);
		
		PropertyInput p3 = createPropertyInput(P.getURI(), intersection);
		
		testSerialization(p3, X, stmt(X, prop(P.getURI()), Y), stmt(Y, prop(P.getURI()), lit("val1")),
		                  stmt(Y, prop(R.getURI()), lit("val2")));
	}

	@Test
	public void testUnionOfPropertyValues() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.getRange().setValue(createLiteral("val")); 

		UnionInput union = createUnionInput(p1, p2);
		union.setValueIndex(1);
		
		testSerialization(union, X, stmt(X, prop(R.getURI()), lit("val")));
	}

	@Test
	public void testUnionOfPropertyValuesNoIndex() {
		PropertyInput p1 = createPropertyInput(P.getURI());
		
		PropertyInput p2 = createPropertyInput(R.getURI());
		p2.getRange().setValue(createLiteral("val")); 

		UnionInput union = createUnionInput(p1, p2);
		
		testSerialization(union, null);
	}

	@Test
	public void testUnionOfNestedPropertyValues() {
		PropertyInput p1a = createPropertyInput(P.getURI());		
		p1a.getRange().setValue(createLiteral("val")); 
		PropertyInput p2a = createPropertyInput(R.getURI(), p1a);

		PropertyInput p1b = createPropertyInput(R.getURI());
		PropertyInput p2b = createPropertyInput(P.getURI(), p1b);

		UnionInput union = createUnionInput(p2a, p2b);
		union.setValueIndex(0);
		
		testSerialization(union, X, stmt(X, prop(R.getURI()), Y), stmt(Y, prop(P.getURI()), lit("val")));
	}

	@Test
	public void testUnionWithoutPropertyValues() {
		Input atomic1 = createAtomicInput(A.getURI());
		Input atomic2 = createAtomicInput(B.getURI());
		UnionInput union = createUnionInput(atomic1, atomic2);
		union.setValueType(0, A.getURI());
		union.setValueType(1, B.getURI());
		
		union.setValueIndex(0);		
		testSerialization(union, X, stmt(X, RDF.type, uri(A.getURI())));
		union.setValueIndex(1);		
		testSerialization(union, X, stmt(X, RDF.type, uri(B.getURI())));
	}
}
