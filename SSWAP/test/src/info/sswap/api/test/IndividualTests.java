/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPList;
import info.sswap.api.model.SSWAPLiteral;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.model.Literal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.XSD;


/**
 * Tests for SSWAPIndividuals.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class IndividualTests {
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:IndividualTest#";
	
	/**
	 * Tests for types of individuals
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testIndividualTypes() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "TestModel"));
		
		assertNotNull(document);
		
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		assertNotNull(individual);
		
		assertEquals(URI.create(NS + "TestIndividual"), individual.getURI());
		
		assertTrue(individual.getDeclaredTypes().isEmpty());
				
		SSWAPType type1 = document.getType(URI.create(NS + "type1"));
		
		individual.addType(type1);
		
		assertEquals(1, individual.getDeclaredTypes().size());
		assertTrue(individual.getDeclaredTypes().contains(type1));
		assertEquals(2, individual.getTypes().size());
		assertTrue(individual.getTypes().contains(type1));
		assertTrue(individual.getTypes().contains(document.getType(URI.create(OWL.Thing.getURI()))));
		
		assertEquals(type1, individual.getDeclaredType());
		
		SSWAPType type2 = document.getType(URI.create(NS + "type2"));
		individual.addType(type2);
		
		assertEquals(2, individual.getDeclaredTypes().size());
		assertTrue(individual.getDeclaredTypes().contains(type1));
		assertTrue(individual.getDeclaredTypes().contains(type2));		
		assertEquals(3, individual.getTypes().size());
		assertTrue(individual.getTypes().contains(type1));
		assertTrue(individual.getTypes().contains(type2));
		assertTrue(individual.getTypes().contains(document.getType(URI.create(OWL.Thing.getURI()))));
		
		individual.removeType(type1);
				
		assertEquals(1, individual.getDeclaredTypes().size());
		assertFalse(individual.getDeclaredTypes().contains(type1));
		assertTrue(individual.getDeclaredTypes().contains(type2));
	}
	
	/**
	 * Tests for individual properties
	 * @throws URISyntaxException
	 */
	@Test 
	public void testIndividualProperties() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "TestModel"));
		
		assertNotNull(document);
		
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		assertNotNull(individual);
		
		assertEquals(URI.create(NS + "TestIndividual"), individual.getURI());

		assertNotNull(individual.getProperties());
		assertTrue(individual.getProperties().isEmpty());
		
		SSWAPPredicate predicate = document.getPredicate(URI.create(NS + "property1"));;
		SSWAPProperty property = individual.addProperty(predicate, "test");
		
		assertEquals("test", property.getValue().asString());
		assertTrue(property.getValue().isLiteral());
		assertFalse(property.getValue().isIndividual());
		assertFalse(property.getValue().isList());
		assertNull(property.getValue().asIndividual());
		assertNull(property.getValue().asList());
				
		assertEquals(1, individual.getProperties().size());
		
		predicate = document.getPredicate(URI.create(NS + "property2"));
		property = individual.addProperty(predicate, "2.0", URI.create("http://www.w3.org/2001/XMLSchema#decimal"));
		
		assertEquals("2.0", property.getValue().asString());
		assertEquals(2.0, property.getValue().asDouble().doubleValue(), 0.0001);
		assertTrue(property.getValue().isLiteral());
		assertFalse(property.getValue().isIndividual());
		assertFalse(property.getValue().isList());
		assertNull(property.getValue().asIndividual());
		assertNull(property.getValue().asList());
		
		// test multiple values for the same property
		predicate = document.getPredicate(URI.create(NS + "property3"));
		individual.addProperty(predicate, "test2");
		individual.addProperty(predicate, "test3");
		
		assertEquals(4, individual.getProperties().size());
		assertEquals(1, individual.getProperties(document.getPredicate(URI.create(NS + "property1"))).size());
		assertEquals(1, individual.getProperties(document.getPredicate(URI.create(NS + "property2"))).size());
		assertEquals(2, individual.getProperties(document.getPredicate(URI.create(NS + "property3"))).size());
		
		assertNotNull(individual.getProperties(document.getPredicate(URI.create(NS + "non-existing-property-uri"))));
		assertTrue(individual.getProperties(document.getPredicate(URI.create(NS + "non-existing-property-uri"))).isEmpty());
		
		individual.serialize(System.out, RDFRepresentation.RDF_XML, true);
	}
	
	@Test
	public void testSetProperty() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "TestModel"));
		
		assertNotNull(document);
		
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		assertNotNull(individual);
		
		SSWAPPredicate predicate = document.getPredicate(URI.create(NS + "property1"));		
		SSWAPProperty property = null;
		
		// test the set when there was 0 instances of that property for the individual
		property = individual.setProperty(predicate, "test");
		
		Collection<SSWAPProperty> properties = individual.getProperties(property.getPredicate());		
		assertNotNull(properties);
		assertEquals(1, properties.size());
		assertEquals("test", properties.iterator().next().getValue().asString());

		// test the set when there was 1 instance of that property for the individual
		individual.setProperty(predicate, "test2");

		properties = individual.getProperties(property.getPredicate());
		assertNotNull(properties);
		assertEquals(1, properties.size());		
		assertEquals("test2", properties.iterator().next().getValue().asString());
		
		// prepare the individual with 2 properties		
		individual.addProperty(predicate, "test3");
		
		properties = individual.getProperties(property.getPredicate());
		assertNotNull(properties);
		assertEquals(2, properties.size());		
		
		// test the set when there was more than 1 instance of that property for the individual
		individual.setProperty(predicate, "test4");
		
		properties = individual.getProperties(property.getPredicate());
		assertNotNull(properties);
		assertEquals(1, properties.size());		
		assertEquals("test4", properties.iterator().next().getValue().asString());
		
		// set up a fresh individual
		individual = document.createIndividual(URI.create(NS + "freshIndividual"));
		
		// test clearing/adding a collection of properties
		SSWAPType owlDatatypeProperty = document.getType(URI.create("http://www.w3.org/2002/07/owl#DatatypeProperty"));
		
		// define an owl:DatatypeProperty and SSWAPProperty to remain after the test
		URI uri = URI.create(NS + "propertyToRemain");
		SSWAPPredicate predicateToRemain = document.getPredicate(uri);
		predicateToRemain.addType(owlDatatypeProperty);
		
		individual.setProperty(predicateToRemain, "ToRemain Value");
		
		// define an owl:DatatypeProperty and two SSWAPProperties to be replaced during the test
		uri = URI.create(NS + "propertyToBeReplaced");
		SSWAPPredicate predicateToBeReplaced = document.getPredicate(uri);
		predicateToBeReplaced.addType(owlDatatypeProperty);
		
		individual.setProperty(predicateToBeReplaced, "ToBeReplaced Value");
		
		// make another; same URI, different value
		
		// define an owl:DatatypeProperty and SSWAPProperty to be added during the test
		uri = URI.create(NS + "propertyToBeAdded");
		SSWAPPredicate predicateToBeAdded = document.getPredicate(uri);
		predicateToBeAdded.addType(owlDatatypeProperty);
		
		// pre-test the setup
		property = individual.getProperty(predicateToRemain);		
		assertEquals(property.getValue().asString(),"ToRemain Value");
		
		property = individual.getProperty(predicateToBeReplaced);
		assertEquals(property.getValue().asString(),"ToBeReplaced Value");
		
		property = individual.getProperty(predicateToBeAdded);
		assertNull(property);
		
		// stage and do the test
		individual.setProperty(predicateToBeReplaced, "New ToBeReplaced Value");
		individual.addProperty(predicateToBeAdded, "ToBeAdded Value");
	
		// check the results
		property = individual.getProperty(predicateToRemain);
		assertEquals(property.getValue().asString(),"ToRemain Value");
		
		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;
		for ( SSWAPProperty p : individual.getProperties(predicateToBeReplaced) ) {
			
			String v = p.getValue().asString();
			
			if ( v.equals("New ToBeReplaced Value") ) {
				found1 = true;
			} else if ( v.equals("Different ToBeReplaced Value") ) {
				found2 = true;
			} else if ( v.equals("ToBeReplaced Value") ) {
				found3 = true;
			} else {
				break;
			}
		}
		assertTrue(found1 && !found2 && !found3);
		
		property = individual.getProperty(predicateToBeAdded);
		assertEquals(property.getValue().asString(),"ToBeAdded Value");

	}
	
	/**
	 * Basic check whether the validation of typed literals is enabled and works (i.e.,
	 * it tries creating one correct literal and one incorrect literal).
	 * 
	 * There are no extensive tests because the underlying implementation uses validation provided
	 * by Jena (RDFDatatype.isValid()).
	 * 
	 * @throws URISyntaxException if the URIs of the datatypes should be incorrect
	 */
	@Test
	public void testTypedLiteralValidation() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "TestModel"));
		
		document.createTypedLiteral("2.0", URI.create("http://www.w3.org/2001/XMLSchema#decimal"));
		
		try {
			document.createTypedLiteral("illegalValue", URI.create("http://www.w3.org/2001/XMLSchema#decimal"));
			fail("The validation of typed literals allowed to create an invalid literal");
		}
		catch (IllegalArgumentException e) {
			// correct behavior
		}
	}
	
	@Test
	public void testLiteralMethods() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "TestModel"));
		
		SSWAPLiteral literal = document.createLiteral("xyzzy");
		
		assertNull(literal.asBoolean());
		assertNull(literal.asInteger());
		assertNull(literal.asDouble());
		assertNull(literal.asList());
		assertNull(literal.asIndividual());
		
		assertNull(literal.getDatatypeURI());
		assertNull(literal.getLanguage());
		
		assertEquals("xyzzy", literal.asString());		
		assertEquals("xyzzy", ((Literal) literal).getValue());
		
		try {
			literal.addComment("comment");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		

		try {
			literal.addLabel("label");
		}
		catch (IllegalArgumentException e) {
			// expected
		}		
	}
	
	@Test
	public void testLiteralLanguagesAndTypes() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "TestModel"));
		
		SSWAPLiteral literal = document.createTypedLiteral("1", URI.create(XSD.xstring.toString()));
		SSWAPLiteral literal2 = document.createTypedLiteral("1", URI.create(XSD.xint.toString()));
		SSWAPLiteral literal3 = document.createTypedLiteral("1", URI.create(XSD.xstring.toString()));
	
		assertFalse(literal.equals(literal2));
		assertTrue(literal.equals(literal3));
		
		SSWAPLiteral literal4 = new Literal("xyzzy", URI.create(XSD.xstring.toString()), "en");
		SSWAPLiteral literal5 = new Literal("xyzzy", URI.create(XSD.xstring.toString()), "de");
		SSWAPLiteral literal6 = new Literal("xyzzy", URI.create(XSD.xstring.toString()), null);
		
		assertFalse(literal4.equals(literal5));
		assertFalse(literal4.equals(literal6));
		assertFalse(literal6.equals(literal4));
	}
	
	@Test
	public void testCreateListValues() {
		SSWAPDocument document = SSWAP.createSSWAPDocument();
		
		SSWAPList list = document.createList();
		
		assertNotNull(list);
		
		assertTrue(list.isEmpty());
		assertEquals(0, list.size());
		assertTrue(list.isList());
		assertEquals(list, list.asList());
		
		SSWAPLiteral literal = document.createLiteral("abc");
		
		list.add(literal);
		assertFalse(list.isEmpty());
		assertEquals(1, list.size());
		
		assertEquals(literal, list.get(0));
		assertNotNull(list.iterator());
		assertEquals(literal, list.iterator().next());
		assertTrue(list.contains(literal));
		assertEquals(0, list.indexOf(literal));
		assertEquals(0, list.lastIndexOf(literal));
		
		assertNotNull(list.listIterator());
		
		SSWAPLiteral literal2 = document.createLiteral("xyz");
		list.add(literal2);
		assertEquals(2, list.size());
		assertTrue(list.listIterator(1).hasNext());
		list.remove(literal);
		assertEquals(1, list.size());
		
		assertEquals(1, list.toArray().length);
		assertEquals(literal2, list.toArray()[0]);
		
		list.clear();
		assertTrue(list.isEmpty());
		
		list.add(literal);
		
		SSWAPList list2 = document.createList();
		list2.add(document.createLiteral("1"));
		list2.add(document.createLiteral("2"));
		list2.add(1, document.createLiteral("3"));
		
		list.addAll(list2);
		assertTrue(list.containsAll(list2));
		list.removeAll(list2);
		
		list.addAll(0, list2);
		list.retainAll(list2);
		assertTrue(list.containsAll(list2));
		
		assertNotNull(list.toArray(new SSWAPElement[0]));
	}
	
	@Test
	public void testIsOfType() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPType a = document.getType(URI.create(NS + "type/A"));
		SSWAPType b = document.getType(URI.create(NS + "type/B"));
		SSWAPType c = document.getType(URI.create(NS + "type/C"));
		SSWAPType d = document.getType(URI.create(NS + "type/D"));
		
		b.addSubClassOf(a);
		c.addSubClassOf(b);
		
		SSWAPType nothing = document.getType(URI.create(OWL.Nothing.toString()));
		SSWAPType thing = document.getType(URI.create(OWL.Thing.toString()));
		
		SSWAPIndividual ind = document.createIndividual();
		ind.addType(b);
		
		assertTrue(ind.isOfType(thing));
		assertTrue(ind.isOfType(a));
		assertTrue(ind.isOfType(b));
		
		assertFalse(ind.isOfType(c));
		assertFalse(ind.isOfType(d));
		assertFalse(ind.isOfType(nothing));
	}
	
	@Test
	public void testIsCompatibleWith() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPType a = document.getType(URI.create(NS + "type/A"));
		SSWAPType b = document.getType(URI.create(NS + "type/B"));
		SSWAPType c = document.getType(URI.create(NS + "type/C"));
		SSWAPType d = document.getType(URI.create(NS + "type/D"));
		SSWAPType e = document.getType(URI.create(NS + "type/E"));

		a.addDisjointWith(b);
		c.addSubClassOf(a);
		d.addSubClassOf(b);
		
		SSWAPType nothing = document.getType(URI.create(OWL.Nothing.toString()));
		SSWAPType thing = document.getType(URI.create(OWL.Thing.toString()));
		
		SSWAPType intersectionType = a.intersectionOf(b);
		SSWAPType unionType = a.unionOf(b);
		
		SSWAPIndividual ind = document.createIndividual();
		ind.addType(b);
		
		assertTrue(ind.isCompatibleWith(thing));
		assertTrue(ind.isCompatibleWith(b));
		assertTrue(ind.isCompatibleWith(d));
		assertTrue(ind.isCompatibleWith(e));
		assertTrue(ind.isCompatibleWith(unionType));
		
		assertFalse(ind.isCompatibleWith(a));
		assertFalse(ind.isCompatibleWith(c));
		assertFalse(ind.isCompatibleWith(nothing));
		assertFalse(ind.isCompatibleWith(intersectionType));
	}
	
	@Test
	public void testIndividualPropertyDetachment() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		SSWAPProperty a1 = individual.addProperty(a, "1");
		
		assertEquals(individual, a1.getIndividual());
		assertTrue(a1.getValue().isLiteral());
		assertEquals("1", a1.getValue().asString());
		
		// force a refresh on the Jena model (internally, this involves rereading everything from Jena 
		// model and potentially adding/removing SSWAPProperties; however, existing properties must not
		// be touched (as references to them may be held by users of API)
		try {
	        document.validate();
        }
        catch (ValidationException e) {
	        e.printStackTrace();
        }
		
        // verify that a1 still is attached to individual
		assertEquals(individual, a1.getIndividual());
		
		// verify that individual has still exactly the same property (not just equals but also in terms of ==)
		assertTrue(individual.getProperty(a) == a1);
		assertEquals(individual.getProperty(a), a1);

		// add second property
		SSWAPProperty a2 = individual.addProperty(a, "2");
		
		assertEquals(individual, a2.getIndividual());
		assertTrue(a2.getValue().isLiteral());
		assertEquals("2", a2.getValue().asString());
		
		// verify that the first value is NOT detached
		assertEquals(individual, a1.getIndividual());
		
		// perform a detach on first two properties by setting (not adding) the value for the third property
		SSWAPProperty a3 = individual.setProperty(a, "3");
		
		assertEquals(individual, a3.getIndividual());
		assertTrue(a3.getValue().isLiteral());
		assertEquals("3", a3.getValue().asString());

		// now, both of the first values should have null as the individual
		assertNull(a1.getIndividual());
		assertNull(a2.getIndividual());		
		
		// and the only value should be a3
		assertTrue(individual.getProperty(a) == a3);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidDatatypeValue() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		SSWAPIndividual individual2 = document.createIndividual();

		// should fail -- attempt to set an individual as a value for a datatype property
		individual.setProperty(document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/symbol")), individual2);	
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidObjectValue() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		individual.setProperty(document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/hasQTL")), "1");	
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidObjectValue2() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		individual.setProperty(document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/hasQTL")), "1", URI.create(XSD.integer.toString()));	
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidObjectValue3() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		SSWAPLiteral literal = document.createLiteral("1");
		
		individual.setProperty(document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/hasQTL")), literal);	
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidObjectValue4() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));
		
		SSWAPLiteral literal = document.createTypedLiteral("1", URI.create(XSD.integer.toString()));
		
		individual.setProperty(document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/hasQTL")), literal);	
	}
	
	@Test
	public void testAddProperty1() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual
		SSWAPIndividual individual2 = document.createIndividual();		
		SSWAPProperty a1 = individual.addProperty(a, individual2);
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
		assertTrue(a1 == individual.getProperty(a));
		assertEquals(individual2, individual.getProperty(a).getValue());
		
		assertEquals(1, individual.getProperties(a).size());
		assertTrue(individual.getProperties(a).contains(a1));
		
		assertEquals(individual, a1.getIndividual());
		assertTrue(individual == a1.getIndividual());
	}
	
	@Test
	public void testAddProperty2() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual		
		SSWAPProperty a1 = individual.addProperty(a, "abc");
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
		assertTrue(a1 == individual.getProperty(a));
		assertEquals("abc", individual.getProperty(a).getValue().asString());
		
		assertEquals(1, individual.getProperties(a).size());
		assertTrue(individual.getProperties(a).contains(a1));
		
		assertEquals(individual, a1.getIndividual());
		assertTrue(individual == a1.getIndividual());
	}

	@Test
	public void testAddProperty3() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual		
		SSWAPProperty a1 = individual.addProperty(a, "1", URI.create(XSD.integer.toString()));
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
		assertTrue(a1 == individual.getProperty(a));
		assertEquals("1", individual.getProperty(a).getValue().asString());
		assertEquals(1, individual.getProperty(a).getValue().asInteger().intValue());
		assertEquals(URI.create(XSD.integer.toString()), individual.getProperty(a).getValue().asLiteral().getDatatypeURI());
		
		assertEquals(1, individual.getProperties(a).size());
		assertTrue(individual.getProperties(a).contains(a1));
		
		assertEquals(individual, a1.getIndividual());
		assertTrue(individual == a1.getIndividual());
	}

	@Test
	public void testAddProperty4() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual		
		SSWAPLiteral literal = document.createLiteral("abc");
		SSWAPProperty a1 = individual.addProperty(a, literal);
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
		assertTrue(a1 == individual.getProperty(a));
		assertEquals(literal, individual.getProperty(a).getValue());
		
		assertEquals(1, individual.getProperties(a).size());
		assertTrue(individual.getProperties(a).contains(a1));
		
		assertEquals(individual, a1.getIndividual());
		assertTrue(individual == a1.getIndividual());
	}
	
	@Test
	public void testSetProperty1() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual
		SSWAPIndividual individual2 = document.createIndividual();		
		SSWAPProperty a1 = individual.addProperty(a, individual2);
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
	
		SSWAPIndividual individual3 = document.createIndividual();
		SSWAPProperty a2 = individual.setProperty(a, individual3);
		
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a2, individual.getProperty(a));
	
		assertEquals(individual, a2.getIndividual());
		assertTrue(individual == a2.getIndividual());
		assertNull(a1.getIndividual());
	}
	
	@Test
	public void testSetProperty2() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual		
		SSWAPProperty a1 = individual.addProperty(a, "abc");
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
	
		SSWAPProperty a2 = individual.setProperty(a, "xyz");
		
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a2, individual.getProperty(a));
	
		assertEquals(individual, a2.getIndividual());
		assertTrue(individual == a2.getIndividual());
		assertNull(a1.getIndividual());
	}
	
	@Test
	public void testSetProperty3() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual		
		SSWAPProperty a1 = individual.addProperty(a, "1", URI.create(XSD.integer.toString()));
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
	
		SSWAPProperty a2 = individual.setProperty(a, "2", URI.create(XSD.integer.toString()));
		
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a2, individual.getProperty(a));
	
		assertEquals(individual, a2.getIndividual());
		assertTrue(individual == a2.getIndividual());
		assertNull(a1.getIndividual());
	}
	
	@Test
	public void testSetProperty4() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		
		// verify that there are no values for the predicate
		assertEquals(0, individual.getProperties().size());
		assertNull(individual.getProperty(a));
		assertNotNull(individual.getProperties(a));
		assertTrue(individual.getProperties(a).isEmpty());
		
		// add individual		
		SSWAPLiteral literal1 = document.createLiteral("abc");
		SSWAPProperty a1 = individual.addProperty(a, literal1);
		
		// check the state after adding
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a1, individual.getProperty(a));
	
		SSWAPLiteral literal2 = document.createLiteral("xyz");
		SSWAPProperty a2 = individual.setProperty(a, literal2);
		
		assertEquals(1, individual.getProperties().size());
		assertNotNull(individual.getProperty(a));
		assertEquals(a2, individual.getProperty(a));
	
		assertEquals(individual, a2.getIndividual());
		assertTrue(individual == a2.getIndividual());
		assertNull(a1.getIndividual());
	}
	
	@Test
	public void removeProperty1() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
	
		SSWAPProperty a1 = individual.addProperty(a, "1");
		SSWAPProperty a2 = individual.addProperty(a, "2");
		
		assertEquals(2, individual.getProperties(a).size());
		
		individual.removeProperty(a, a1.getValue());
		
		assertEquals(1, individual.getProperties(a).size());
		assertEquals(a2, individual.getProperty(a));	
	}
	
	@Test
	public void removeProperty1a() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
	
		SSWAPProperty a1 = individual.addProperty(a, "1");
		SSWAPProperty a2 = individual.addProperty(a, "2");
		
		assertEquals(2, individual.getProperties(a).size());
		
		a1.removeProperty();
		
		assertEquals(1, individual.getProperties(a).size());
		assertEquals(a2, individual.getProperty(a));	
	}
	
	@Test
	public void removeProperty2() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
	
		SSWAPProperty a1 = individual.addProperty(a, "1");
		SSWAPProperty a2 = individual.addProperty(a, "2");
		
		assertEquals(2, individual.getProperties(a).size());
		
		individual.removeProperty(a1);
		
		assertEquals(1, individual.getProperties(a).size());
		assertEquals(a2, individual.getProperty(a));	
	}
	
	@Test
	public void removeProperty3() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
	
		SSWAPIndividual individual1 = document.createIndividual();
		SSWAPIndividual individual2 = document.createIndividual();
		
		SSWAPProperty a1 = individual.addProperty(a, individual1);
		SSWAPProperty a2 = individual.addProperty(a, individual2);
		
		assertEquals(2, individual.getProperties(a).size());
		
		individual.removeProperty(a, a1.getValue());
		
		assertEquals(1, individual.getProperties(a).size());
		assertEquals(a2, individual.getProperty(a));	
	}
	
	@Test
	public void removeProperty3a() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
	
		SSWAPIndividual individual1 = document.createIndividual();
		SSWAPIndividual individual2 = document.createIndividual();
		
		SSWAPProperty a1 = individual.addProperty(a, individual1);
		SSWAPProperty a2 = individual.addProperty(a, individual2);
		
		assertEquals(2, individual.getProperties(a).size());
	
		a1.removeProperty();
		
		assertEquals(1, individual.getProperties(a).size());
		assertEquals(a2, individual.getProperty(a));	
	}

	
	@Test
	public void removeProperty4() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
	
		SSWAPIndividual individual1 = document.createIndividual();
		SSWAPIndividual individual2 = document.createIndividual();
		
		SSWAPProperty a1 = individual.addProperty(a, individual1);
		SSWAPProperty a2 = individual.addProperty(a, individual2);
		
		assertEquals(2, individual.getProperties(a).size());
		
		individual.removeProperty(a1);
		
		assertEquals(1, individual.getProperties(a).size());
		assertEquals(a2, individual.getProperty(a));	
	}
	
	@Test
	public void clearProperty1() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		SSWAPPredicate b = document.getPredicate(URI.create(NS + "property/b"));
	
		SSWAPProperty a1 = individual.addProperty(a, "1");
		SSWAPProperty a2 = individual.addProperty(a, "2");
		
		SSWAPProperty b1 = individual.addProperty(b, "1");
		SSWAPProperty b2 = individual.addProperty(b, "2");
		
		assertEquals(2, individual.getProperties(a).size());
		assertEquals(2, individual.getProperties(b).size());
		
		individual.clearProperty(a);
		
		assertEquals(0, individual.getProperties(a).size());
		assertEquals(2, individual.getProperties(b).size());
		
		assertNull(a1.getIndividual());
		assertNull(a2.getIndividual());
		
		assertEquals(individual, b1.getIndividual());
		assertEquals(individual, b2.getIndividual());
	}
	
	@Test
	public void testAutomaticTyping() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate symbol = document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/symbol"));
		
		SSWAPProperty symbol1 = individual.setProperty(symbol, "1");
		assertEquals(URI.create(XSD.xstring.toString()), symbol1.getValue().asLiteral().getDatatypeURI());
		
		SSWAPPredicate endPosition = document.getPredicate(URI.create("http://sswapmeet.sswap.info/map/endPosition"));
		
		SSWAPProperty endPosition1 = individual.setProperty(endPosition, "1");
		assertEquals(URI.create(XSD.decimal.toString()), endPosition1.getValue().asLiteral().getDatatypeURI());
		
		try {
			individual.setProperty(endPosition, "abc");
			fail("Was able to set \"abc\" as a value for property whose range is xsd:decimal");
		}
		catch (IllegalArgumentException e) {
			// correct behavior
		}
		
		SSWAPPredicate units = document.getPredicate(URI.create("http://sswapmeet.sswap.info/map/units"));
		SSWAPProperty unitsBand = individual.setProperty(units, "band");
		assertEquals(URI.create(XSD.xstring.toString()), unitsBand.getValue().asLiteral().getDatatypeURI());
		
		individual.setProperty(units, "incorrect");
		
		try {
	        document.validate();
	        fail("Document with a value that causes the ontology to be inconsistent passed validation");
        }
        catch (ValidationException e) {
	        // correct behavior
        }
	}
	
	@Test
	public void testHasValue1() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		SSWAPPredicate b = document.getPredicate(URI.create(NS + "property/b"));
				
		SSWAPProperty a1 = individual.addProperty(a, "abc");
		SSWAPProperty a2 = individual.addProperty(a, "def");
		SSWAPProperty b1 = individual.addProperty(b, "xyz");
		SSWAPProperty b2 = individual.addProperty(b, "pqr");
		
		assertTrue(individual.hasValue(a, a1.getValue()));
		assertTrue(individual.hasValue(a, a2.getValue()));
		assertFalse(individual.hasValue(a, b1.getValue()));
		assertFalse(individual.hasValue(a, b2.getValue()));
		
		assertFalse(individual.hasValue(b, a1.getValue()));
		assertFalse(individual.hasValue(b, a2.getValue()));
		assertTrue(individual.hasValue(b, b1.getValue()));
		assertTrue(individual.hasValue(b, b2.getValue()));

		// test for literals that are not identical but only equal()
		SSWAPLiteral abc = document.createLiteral("abc");
		assertTrue(individual.hasValue(a, abc));
		assertFalse(individual.hasValue(b, abc));
		
		Collection<SSWAPProperty> p = individual.hasValue(a1.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(a1));
		
		p = individual.hasValue(a2.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(a2));
		
		p = individual.hasValue(b1.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(b1));
		
		p = individual.hasValue(b2.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(b2));
		
		SSWAPProperty b3 = individual.addProperty(b, "abc");
		
		p = individual.hasValue(a1.getValue());
		assertNotNull(p);
		assertEquals(2, p.size());
		assertTrue(p.contains(a1));
		assertTrue(p.contains(b3));
		assertFalse(p.contains(a2));
		assertFalse(p.contains(b1));
		assertFalse(p.contains(b2));

		SSWAPLiteral nonExistent = document.createLiteral("nonExistingLiteral");
		
		assertFalse(individual.hasValue(a, nonExistent));
		assertFalse(individual.hasValue(b, nonExistent));
		p = individual.hasValue(nonExistent);
		assertNotNull(p);
		assertTrue(p.isEmpty());		
	}
	
	@Test
	public void testHasValue2() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPIndividual individual = document.createIndividual(URI.create(NS + "TestIndividual"));		
		
		SSWAPPredicate a = document.getPredicate(URI.create(NS + "property/a"));
		SSWAPPredicate b = document.getPredicate(URI.create(NS + "property/b"));
				
		SSWAPIndividual ia1 = document.createIndividual(URI.create(NS + "TestIndividual/abc"));
		SSWAPIndividual ia2 = document.createIndividual(URI.create(NS + "TestIndividual/def"));
		SSWAPIndividual ib1 = document.createIndividual(URI.create(NS + "TestIndividual/xyz"));
		SSWAPIndividual ib2 = document.createIndividual(URI.create(NS + "TestIndividual/pqr"));
		
		SSWAPProperty a1 = individual.addProperty(a, ia1);
		SSWAPProperty a2 = individual.addProperty(a, ia2);
		SSWAPProperty b1 = individual.addProperty(b, ib1);
		SSWAPProperty b2 = individual.addProperty(b, ib2);
		
		assertTrue(individual.hasValue(a, a1.getValue()));
		assertTrue(individual.hasValue(a, a2.getValue()));
		assertFalse(individual.hasValue(a, b1.getValue()));
		assertFalse(individual.hasValue(a, b2.getValue()));
		
		assertFalse(individual.hasValue(b, a1.getValue()));
		assertFalse(individual.hasValue(b, a2.getValue()));
		assertTrue(individual.hasValue(b, b1.getValue()));
		assertTrue(individual.hasValue(b, b2.getValue()));

		// test for individuals that are not identical but only equal() (same the same URI)
		SSWAPIndividual ia1Copy = document.createIndividual(URI.create(NS + "TestIndividual/abc"));
		assertTrue(individual.hasValue(a, ia1Copy));
		assertFalse(individual.hasValue(b, ia1Copy));
		
		Collection<SSWAPProperty> p = individual.hasValue(a1.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(a1));
		
		p = individual.hasValue(a2.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(a2));
		
		p = individual.hasValue(b1.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(b1));
		
		p = individual.hasValue(b2.getValue());
		assertNotNull(p);
		assertEquals(1, p.size());
		assertTrue(p.contains(b2));
		
		SSWAPProperty b3 = individual.addProperty(b, ia1);
		
		p = individual.hasValue(a1.getValue());
		assertNotNull(p);
		assertEquals(2, p.size());
		assertTrue(p.contains(a1));
		assertTrue(p.contains(b3));
		assertFalse(p.contains(a2));
		assertFalse(p.contains(b1));
		assertFalse(p.contains(b2));

		SSWAPIndividual nonExistent = document.createIndividual(URI.create(NS + "TestIndividual/nonExistingIndividual"));
		
		assertFalse(individual.hasValue(a, nonExistent));
		assertFalse(individual.hasValue(b, nonExistent));
		p = individual.hasValue(nonExistent);
		assertNotNull(p);
		assertTrue(p.isEmpty());
	}
	
	@Test
	public void testAccessNonExistingProperty() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		SSWAPSubject subject = rdg.getResource().getGraph().getSubject();
		
		SSWAPProperty accessionID = subject.getProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		
		assertNotNull(accessionID);
		
		Collection<SSWAPProperty> accessionIDs = subject.getProperties(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		assertNotNull(accessionIDs);
		assertFalse(accessionIDs.isEmpty());		
		
		SSWAPProperty nonExistingProperty = subject.getProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/nonExistingProperty")));
		assertNull(nonExistingProperty);
		
		Collection<SSWAPProperty> nonExistingProperties = subject.getProperties(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/nonExistingProperty")));
		assertNotNull(nonExistingProperties);
		assertTrue(nonExistingProperties.isEmpty());				
	}
	
	@Test
	public void testReservedProperties() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		SSWAPSubject subject = rdg.getResource().getGraph().getSubject();

		subject.setProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")), "abc");
		
		try {
			subject.setProperty(rdg.getPredicate(URI.create(Namespaces.RDF_NS + "type")), "abc");
			fail("Managed to set value for a restricted predicate");
		}
		catch (IllegalArgumentException e) {
			// expected behavior
		}
		
		try {
			subject.setProperty(rdg.getPredicate(URI.create(Namespaces.RDFS_NS + "subClassOf")), "abc");
			fail("Managed to set value for a restricted predicate");
		}
		catch (IllegalArgumentException e) {
			// expected behavior
		}
		
		try {
			subject.setProperty(rdg.getPredicate(URI.create(Namespaces.OWL_NS + "sameAs")), "abc");
			fail("Managed to set value for a restricted predicate");
		}
		catch (IllegalArgumentException e) {
			// expected behavior
		}
		
		try {
			subject.setProperty(rdg.getPredicate(URI.create(Namespaces.XSD_NS + "string")), "abc");
			fail("Managed to set value for a restricted predicate");
		}
		catch (IllegalArgumentException e) {
			// expected behavior
		}
		
		try {
			subject.setProperty(rdg.getPredicate(URI.create(Namespaces.SSWAP_NS + "mapsTo")), "abc");
			fail("Managed to set value for a restricted predicate");
		}
		catch (IllegalArgumentException e) {
			// expected behavior
		}
		
		// test exceptions (annotation properties)
		subject.setProperty(rdg.getPredicate(URI.create(Namespaces.RDFS_NS + "label")), "abc");
		subject.setProperty(rdg.getPredicate(URI.create(Namespaces.RDFS_NS + "comment")), "abc");
		subject.setProperty(rdg.getPredicate(URI.create(Namespaces.RDFS_NS + "seeAlso")), "abc");
		subject.setProperty(rdg.getPredicate(URI.create(Namespaces.RDFS_NS + "isDefinedBy")), "abc");
	}
	
	@Test
	public void testIndividualCommentsLabels() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		SSWAPSubject subject = rdg.getResource().getGraph().getSubject();

		subject.addComment("comment");
		subject.addLabel("label");
		
		assertEquals("comment", subject.getComment());
		assertEquals("label", subject.getLabel());
	}
	
	@Test
	public void testCreateLargeNumberOfIndividuals() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
				
		SSWAPSubject subject = rdg.getResource().getGraph().getSubject();
		
		SSWAPIndividual individual = rdg.createIndividual(URI.create(NS + "Individual1"));
		
		SSWAPPredicate predicate = rdg.getPredicate(URI.create(NS + "property1Datatype"));
		
		individual.addProperty(predicate, rdg.createLiteral("a"));
		
		List<SSWAPObject> objects = new LinkedList<SSWAPObject>(subject.getObjects());
		
		// let's create 1000 SSWAPObjects
		for (int i = 0; i < 1000; i++) {
			SSWAPObject object = rdg.createObject(URI.create(NS + "object" + i));
			
			SSWAPPredicate predicate2 = rdg.getPredicate(URI.create(NS + "property2Object"));
			
			object.addProperty(predicate2, individual);
			object.addProperty(predicate, rdg.createLiteral(String.valueOf(i)));
			
			objects.add(object);
		}
		
		subject.setObjects(objects);
	}
	
	@Test
	public void testPredicateRelativeURI() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		try {
			document.getPredicate(URI.create("qtlaccessionID"));
			fail("Allowed creation of a predicate with a relative URI");
		}
		catch (IllegalArgumentException e) {
			// expected behavior
		}
	}
	
	@Test
	public void testDatatypeRelativeURI() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		try {
			document.getDatatype(URI.create("qtlaccessionID"));
			fail("Allowed creation of a datatype with a relative URI");
		}
		catch (IllegalArgumentException e) {
			// expected behavior
		}
	}
	
	/**
	 * Check that if you set the value of a subProperty of a Datatype FunctionalProperty,
	 * that the operation succeeds and sets the value of the super-property also.
	 */
	@Ignore
	@Test
	public void testSetValueFunctionalProperty() {
		
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		// make predicate p a Datatype FunctionalProperty
		SSWAPPredicate p = document.getPredicate(URI.create(NS + "property/p"));
		p.addType(document.getType(URI.create(OWL.DatatypeProperty.getURI())));
		p.addType(document.getType(URI.create(OWL.FunctionalProperty.getURI())));

		// make predicate q a subproperty of p
		SSWAPPredicate q = document.getPredicate(URI.create(NS + "property/q"));
		q.addSubPredicateOf(p);
		
		// add properties p1 and q1 with (the same) value to an individual
		SSWAPIndividual individual = document.createIndividual();
		SSWAPProperty p1 = individual.addProperty(p, "abc");
		SSWAPProperty q1 = individual.addProperty(q, "abc");

		// check
		assertEquals("abc", individual.getProperty(q).getValue().asString());
		assertEquals("abc", individual.getProperty(p).getValue().asString());

		// (re)set the value
		individual.setProperty(q, "def");
		
		// check for consistency
		try {
			document.validate();
		} catch (ValidationException e) {
	        fail("Setting the value of a subproperty of a Functional property failed validation");
		}

		// check that reasoning has percolated the value up the subsumption chain
		// Note that in a "real" use case, one may not have knowledge of p;
		// it may have been retrieved by closure
		assertEquals("def", individual.getProperty(q).getValue().asString());
		assertEquals("def", individual.getProperty(p).getValue().asString());
		
	}
	
	@Test
	public void testNPEHandling() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim"));
		
		RIG rig = rdg.getRIG();
		
		SSWAPObject object = rig.getResource().getGraph().getSubject().getObject();
				
		try {
			object.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/sequence/accessionID")), (String) null);
			fail("No NPE when expected after passing a null value");
		}
		catch (NullPointerException e) {
			// expected behavior
		}
		
		try {
			object.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/sequence/accessionID")), (String) null, URI.create(XSD.xstring.toString()));
			fail("No NPE when expected after passing a null value");
		}
		catch (NullPointerException e) {
			// expected behavior
		}
				
		try {
			rig.serialize(System.out);
		}
		catch (NullPointerException e) {
			fail("Unable to serialize model (possible corruption of Jena model because of previous attempts to insert null values)");
		}
	}
}
