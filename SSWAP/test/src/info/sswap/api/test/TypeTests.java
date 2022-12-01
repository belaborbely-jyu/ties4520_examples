/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPType;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.model.ModelImpl;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Tests for SSWAPTypes
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class TypeTests {
	// constants used in the tests
	
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:TypeTest#";
	
	private static final URI TYPE_A = URI.create(NS + "Type/A");
	private static final URI TYPE_B = URI.create(NS + "Type/B");
	private static final URI PROPERTY_P = URI.create(NS + "Property/P");
	private static final URI PROPERTY_Q = URI.create(NS + "Property/Q");
	private static final URI PROPERTY_R = URI.create(NS + "Property/R");
	private static final URI INDIVIDUAL_I = URI.create(NS + "Individual/I");
	private static final URI INDIVIDUAL_J = URI.create(NS + "Individual/J");
	
	/**
	 * The internal Jena model that holds all the constant resources used in these tests.
	 */
	private static final Model INTERNAL_MODEL = ModelFactory.createDefaultModel();
	
	private static final Resource A = INTERNAL_MODEL.getResource(TYPE_A.toString());
	private static final Resource B = INTERNAL_MODEL.getResource(TYPE_B.toString());
	private static final Property P = INTERNAL_MODEL.getProperty(PROPERTY_P.toString());
	
	private static final Statement A_SUBCLASS_OF_B = INTERNAL_MODEL.createStatement(A, RDFS.subClassOf, B);
	private static final Statement A_TYPE_RESTRICTION = INTERNAL_MODEL.createStatement(A, RDF.type, OWL.Restriction);
	private static final Statement A_ON_PROPERTY_P = INTERNAL_MODEL.createStatement(A, OWL.onProperty, P);
	private static final Statement A_ALL_VALUES_FROM_B = INTERNAL_MODEL.createStatement(A, OWL.allValuesFrom, B);
	private static final Statement A_SOME_VALUES_FROM_B = INTERNAL_MODEL.createStatement(A, OWL.someValuesFrom, B);
	private static final Statement A_EQUIVALENT_CLASS_B = INTERNAL_MODEL.createStatement(A, OWL.equivalentClass, B);
	private static final Statement A_DISJOINT_WITH_B = INTERNAL_MODEL.createStatement(A, OWL.disjointWith, B);
	private static final Statement A_LABEL_TEST_LABEL = INTERNAL_MODEL.createStatement(A, RDFS.label, 
					INTERNAL_MODEL.createTypedLiteral("Test Label"));
	
	private static final Statement A_COMMENT_TEST_COMMENT = INTERNAL_MODEL.createStatement(A, RDFS.comment, 
					INTERNAL_MODEL.createTypedLiteral("Test Comment"));
	
	
	private static SSWAPType type;
	
	/**
	 * Initializes the reasoning service and type field.
	 * 
	 * @throws Exception
	 */
	@Before
	public void init() throws Exception {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "Model"));
		type = document.getType(TYPE_A);
	}
	
	/**
	 * Tests reasoning when a type is declared to be a subclass of itself 
	 */
	@Test
	public void testSelfSubClassOf() {
		assertTrue( type.isSubTypeOf( type ) );
		assertFalse( type.isStrictSubTypeOf( type ) ) ;
	}
	
	/**
	 * Tests reasoning that combines unions, complements and subclass relationships
	 */
	@Test
	public void testUnionOfComplementOf() {
		SSWAPType complement = type.complementOf();
		
		assertNotNull( complement );
		
		SSWAPType union = complement.unionOf( type );
		
		assertNotNull( union );
		
		assertTrue( type.isSubTypeOf( union ) );
		assertTrue( complement.isSubTypeOf( union ) );
	}
	
	/**
	 * Tests reasoning that combines intersections, complements and subclass relationships
	 */
	@Test
	public void testIntersectionOfComplementOf() {
		SSWAPType complement = type.complementOf();
		
		assertNotNull( complement );
		
		SSWAPType intersection = complement.intersectionOf( type );
		
		assertNotNull( intersection );
				
		assertTrue( intersection.isSubTypeOf( type ) );
		assertTrue( intersection.isSubTypeOf( complement) );
	}
	
	@Test
	public void testSelfSubPropertyOf() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		p.addType(document.getType(URI.create(OWL.DatatypeProperty.toString())));
		
		assertTrue(p.isSubPredicateOf(p));
		assertFalse(p.isStrictSubPredicateOf(p));
	}
	
	/**
	 * Tests adding rdfs:subClassOf statement and reasoning about this relationship
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testSubClassOf() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);
		
		SSWAPType b = document.getType(TYPE_B);

		SSWAPType equivalentA = document.getType(URI.create(TYPE_A.toString() + "equivalent"));
		equivalentA.addEquivalentClass(a);
		
		a.addSubClassOf(b);
				
		assertTrue(a.isSubTypeOf(b));
		assertTrue(a.isStrictSubTypeOf(b));
		assertFalse(b.isSubTypeOf(a));
		assertFalse(b.isStrictSubTypeOf(a));		
		assertTrue(modelContainsAxiom(document, A_SUBCLASS_OF_B));
		
		// test equivalent class relations
		assertTrue(equivalentA.isSubTypeOf(b));
		assertTrue(equivalentA.isStrictSubTypeOf(b));
		assertFalse(b.isSubTypeOf(equivalentA));
		assertFalse(b.isStrictSubTypeOf(equivalentA));		
		
		assertTrue(equivalentA.isSubTypeOf(a));
		assertTrue(a.isSubTypeOf(equivalentA));
		assertFalse(a.isStrictSubTypeOf(equivalentA));
		assertFalse(equivalentA.isStrictSubTypeOf(a));
	}
	
	/**
	 * Test sub-property inferences
	 */
	@Test
	public void testSubPropertyOf() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		SSWAPPredicate q = document.getPredicate(PROPERTY_Q);
		SSWAPPredicate r = document.getPredicate(PROPERTY_R);
		
		SSWAPPredicate equivalentP = document.getPredicate(URI.create(PROPERTY_P.toString() + "equivalent"));
		p.addEquivalentPredicate(equivalentP);
		
		p.addSubPredicateOf(q);
		q.addSubPredicateOf(r);
			
		assertTrue(p.isSubPredicateOf(q));
		assertTrue(equivalentP.isSubPredicateOf(q));
		assertTrue(p.isStrictSubPredicateOf(q));
		assertTrue(equivalentP.isStrictSubPredicateOf(q));
		assertFalse(p.isStrictSubPredicateOf(p));
		assertFalse(equivalentP.isStrictSubPredicateOf(p));
		assertFalse(p.isStrictSubPredicateOf(equivalentP));
		
		assertTrue(q.isSubPredicateOf(r));
		assertTrue(q.isStrictSubPredicateOf(r));
		assertFalse(q.isStrictSubPredicateOf(q));
		
		assertTrue(p.isSubPredicateOf(r));
		assertTrue(equivalentP.isSubPredicateOf(r));
		assertTrue(p.isStrictSubPredicateOf(r));
		assertTrue(equivalentP.isStrictSubPredicateOf(r));
		assertFalse(r.isStrictSubPredicateOf(r));
	}
	
	/**
	 * Tests adding of owl:allValuesFrom restriction
	 * @throws URISyntaxException
	 */
	@Test
	public void testAllValuesFromRestriction() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);
		
		SSWAPType b = document.getType(TYPE_B);
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);		
		
		a.addRestrictionAllValuesFrom(p, b);
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));
		assertTrue(modelContainsAxiom(document, A_ALL_VALUES_FROM_B));
	}
	
	/**
	 * Tests adding owl:someValuesFromRestriction
	 * @throws URISyntaxException
	 */
	@Test
	public void testSomeValuesFromRestriction() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);
		
		SSWAPType b = document.getType(TYPE_B);
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		a.addRestrictionSomeValuesFrom(p, b);
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));
		assertTrue(modelContainsAxiom(document, A_SOME_VALUES_FROM_B));
	}
	
	/**
	 * Tests adding owl:hasValue restriction, when the value is not a literal
	 * @throws URISyntaxException
	 */
	@Test
	public void testHasValueRestrictionResource() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		SSWAPType a = document.getType(TYPE_A);
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		SSWAPElement i = document.createIndividual(INDIVIDUAL_I);
		
		a.addRestrictionHasValue(p, i);
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));
	}
	
	/**
	 * Tests adding owl:hasValue restriction, when the value is a literal
	 * @throws URISyntaxException
	 */
	@Test
	public void testHasValueRestrictionLiteral() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		a.addRestrictionHasValue(p, document.createTypedLiteral("42", URI.create(XSD.xint.toString())));
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));		
	}
	
	/**
	 * Tests adding owl:hasSelf restriction
	 * @throws URISyntaxException
	 */
	@Test
	public void testHasSelfRestriction() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		a.addRestrictionHasSelf(p, true);
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));		
	}

	/**
	 * Tests adding minCardinality restriction
	 * @throws URISyntaxException
	 */
	@Test
	public void testMinCardinalityRestriction() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		a.addRestrictionMinCardinality(p, 1);
		
		document.serialize(System.out);
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));
		assertTrue(modelContainsProperLiteralType(document, A, OWL.minCardinality, INTERNAL_MODEL.createTypedLiteral(1, XSDDatatype.XSDnonNegativeInteger)));
	}

	/**
	 * Tests adding maxCardinality restriction
	 * @throws URISyntaxException
	 */
	@Test
	public void testMaxCardinalityRestriction() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		a.addRestrictionMaxCardinality(p, 1);
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));
		assertTrue(modelContainsProperLiteralType(document, A, OWL.maxCardinality, INTERNAL_MODEL.createTypedLiteral(1, XSDDatatype.XSDnonNegativeInteger)));
	}
	
	/**
	 * Tests adding owl:cardinality restriction
	 * @throws URISyntaxException
	 */
	@Test
	public void testCardinalityRestriction() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		a.addRestrictionCardinality(p, 1);
		
		assertTrue(modelContainsAxiom(document, A_TYPE_RESTRICTION));
		assertTrue(modelContainsAxiom(document, A_ON_PROPERTY_P));
		assertTrue(modelContainsProperLiteralType(document, A, OWL.cardinality, INTERNAL_MODEL.createTypedLiteral(1, XSDDatatype.XSDnonNegativeInteger)));
	}
	
	/**
	 * Tests adding owl:equivalentClass
	 * @throws URISyntaxException
	 */
	@Test
	public void testEquivalentClass() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		
		SSWAPType b = document.getType(TYPE_B);		
		
		a.addEquivalentClass(b);
				
		assertTrue(a.isSubTypeOf(b));
		assertTrue(b.isSubTypeOf(a));
		assertTrue(modelContainsAxiom(document, A_EQUIVALENT_CLASS_B));		
	}
	
	/**
	 * Tests adding owl:disjointWith
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testDisjointWith() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		
		SSWAPType b = document.getType(TYPE_B);		
		
		a.addDisjointWith(b);
		
		assertTrue(modelContainsAxiom(document, A_DISJOINT_WITH_B));
	}
	
	@Test
	public void testAddLabel() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);
		
		a.addLabel("Test Label");
		
		assertTrue(modelContainsAxiom(document, A_LABEL_TEST_LABEL));
	}
	
	@Test
	public void testAddComment() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);
		
		a.addComment("Test Comment");
		
		assertTrue(modelContainsAxiom(document, A_COMMENT_TEST_COMMENT));
	}
	
	@Test
	public void testAddAnnotationProperty() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);
		SSWAPPredicate rdfsSeeAlso = document.getPredicate(URI.create(RDFS.seeAlso.getURI()));
		
		a.addAnnotationPredicate(rdfsSeeAlso, document.createIndividual(URI.create(NS + "testIndividual")));
		
		document.serialize(System.out);
	}
	
	
	/**
	 * Tests adding owl:oneOf
	 * @throws URISyntaxException
	 */
	@Test
	public void testOneOf() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);		
		Collection<URI> individuals = Arrays.asList(INDIVIDUAL_I, INDIVIDUAL_J);
		
		a.addOneOf(individuals);
	}
	
	/**
	 * Tests creating a complex restriction (that consists of intersections of simpler restrictions)
	 * @throws URISyntaxException
	 */
	@Test
	public void testCreateComplexRestrictions() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType a = document.getType(TYPE_A);
		SSWAPPredicate p = document.getPredicate(PROPERTY_P);
		
		SSWAPType restrictionType1 = document.createAnonymousType();
		restrictionType1.addRestrictionMinCardinality(p, 1);
		
		SSWAPType restrictionType2 = document.createAnonymousType();
		restrictionType2.addRestrictionMaxCardinality(p, 1);
		
		SSWAPType restrictionType3 = document.createAnonymousType();
		restrictionType3.addRestrictionHasSelf(p, false);
		
		SSWAPType intersectionType = document.createIntersectionOf(Arrays.asList(restrictionType1, restrictionType2, restrictionType3));
		
		a.addSubClassOf(intersectionType);
	}

	@Test
	public void testIsNothing() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPType nothing = document.getType(URI.create(OWL.Nothing.toString()));
		SSWAPType thing = document.getType(URI.create(OWL.Thing.toString()));
		
		assertTrue(nothing.isNothing());
		assertFalse(thing.isNothing());
		
		SSWAPType a = document.getType(TYPE_A);
		SSWAPType b = document.getType(TYPE_B);
		
		a.addDisjointWith(b);
		
		SSWAPType intersectionType = a.intersectionOf(b);
		SSWAPType unionType = a.unionOf(b);
		
		assertTrue(intersectionType.isNothing());
		assertFalse(unionType.isNothing());
		assertFalse(a.isNothing());
		assertFalse(b.isNothing());
	}
	
	/**
	 * Verifies whether the underlying model contains the specified statement.
	 * (Used to test the correctness of methods that add statements to the underlying Jena model.)
	 * 
	 * NOTE: It seems that Jena's Model.contains(Statement) does not check types of literals in the object position (so it will match
	 * "1"^^xsd:integer with "1"^^xsd:nonNegativeInteger)
	 * 
	 * @param model the SSWAP model which should contain the statement
	 * @param statement the statement that should be in the model
	 * @return true if the model contains the statement
	 */
	private boolean modelContainsAxiom(SSWAPModel model, Statement statement) {
		Model jenaModel = ((ModelImpl) model).getSourceModel().getModel();
		
		return jenaModel.contains(statement);
	}
	
	/**
	 * Verifies whether the underlying model contains the specified statement that has a typed literal in the object position (modelContainsAxiom method
	 * seems not to be handling typed literals properly)
	 * 
	 * @param model SSWAP model which should contain the statement
	 * @param subject subject resource
	 * @param predicate predicate
	 * @param literal the typed literal
	 * @return
	 */
	private boolean modelContainsProperLiteralType(SSWAPModel model, Resource subject, Property predicate, Literal literal) {
		Model jenaModel = ((ModelImpl) model).getSourceModel().getModel();
		
		StmtIterator it = jenaModel.listStatements(subject, predicate, literal);
		
		try {
			while (it.hasNext()) {
				Statement s = it.next();
				
				Literal sLiteral = s.getObject().asLiteral();
				
				if (sLiteral.getDatatype().equals(literal.getDatatype())) {
					return true;
				}
			}
		}
		finally {
			it.close();
		}
		
		return false;
	}
	
	@Test
	public void testReservedTypes() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		assertFalse(document.getType(TYPE_A).isReserved());
		
		assertTrue(document.getType(URI.create(Namespaces.RDF_NS + "Resource")).isReserved());
		assertTrue(document.getType(URI.create(Namespaces.RDFS_NS + "Class")).isReserved());
		assertTrue(document.getType(URI.create(Namespaces.OWL_NS + "Class")).isReserved());
		assertTrue(document.getType(URI.create(Namespaces.XSD_NS + "string")).isReserved());
		assertTrue(document.getType(URI.create(Namespaces.SSWAP_NS + "Subject")).isReserved());
	}
	
	@Test
	public void testTypeRelativeURI() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		try {
			document.getType(URI.create("QtlByAccession"));
			fail("Allowed creation of a predicate with a relative URI");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}
}
