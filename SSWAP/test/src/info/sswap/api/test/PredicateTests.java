/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPType;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.model.ModelImpl;
import info.sswap.impl.empire.model.ModelUtils;
import info.sswap.impl.empire.model.ReasoningServiceImpl;

import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Tests for SSWAPPredicate (property definition) objects
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public class PredicateTests {
	
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:PropertyTest#";
	// constants used in tests
	
	private static final URI PROPERTY_A = URI.create(NS + "Property/A");
	private static final URI PROPERTY_B = URI.create(NS + "Property/B");
	
	private static final URI TYPE_T = URI.create(NS + "Type/T");
	
	/**
	 * A model that holds all the constant resources used in the tests
	 */
	private static final Model INTERNAL_MODEL = ModelFactory.createDefaultModel();
	
	private static final Resource A = INTERNAL_MODEL.getResource(PROPERTY_A.toString());
	private static final Resource B = INTERNAL_MODEL.getResource(PROPERTY_B.toString());
	private static final Resource T = INTERNAL_MODEL.getResource(TYPE_T.toString());
	
	private static final Statement A_SUBPROPERTY_OF_B = INTERNAL_MODEL.createStatement(A, RDFS.subPropertyOf, B);
	private static final Statement A_DOMAIN_T = INTERNAL_MODEL.createStatement(A, RDFS.domain, T);
	private static final Statement A_RANGE_T = INTERNAL_MODEL.createStatement(A, RDFS.range, T);
	private static final Statement A_EQUIVALENT_PROPERTY_B = INTERNAL_MODEL.createStatement(A, OWL.equivalentProperty, B);
	private static final Statement A_INVERSE_OF_B = INTERNAL_MODEL.createStatement(A, OWL.inverseOf, B);
	private static final Statement A_LABEL_TEST_LABEL = INTERNAL_MODEL.createStatement(A, RDFS.label, 
					INTERNAL_MODEL.createTypedLiteral("Test Label"));
	
	private static final Statement A_COMMENT_TEST_COMMENT = INTERNAL_MODEL.createStatement(A, RDFS.comment, 
					INTERNAL_MODEL.createTypedLiteral("Test Comment"));
	
	/**
	 * Executes the code for adding rdfs:subPropertyOf
	 * @throws URISyntaxException
	 */
	@Test
	public void testSubPredicateOf() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		
		SSWAPPredicate b = document.getPredicate(PROPERTY_B);
		
		a.addSubPredicateOf(b);
		
		assertTrue(modelContainsAxiom(document, A_SUBPROPERTY_OF_B));
	}

	/**
	 * Executes the code for adding rdfs:domain information
	 * @throws URISyntaxException
	 */
	@Test
	public void testDomain() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		a.addType(document.getType(URI.create(OWL.ObjectProperty.getURI())));
				
		SSWAPType t = document.getType(TYPE_T);
		
		a.addDomain(t);
		
		assertTrue(modelContainsAxiom(document, A_DOMAIN_T));
		
		assertEquals(TYPE_T.toString(), a.getDomain().getURI().toString());
	}
	
	/**
	 * Executes the code for adding and checking rdfs:range information for object properties
	 * @throws URISyntaxException
	 */
	@Test
	public void testObjectPredicateRange() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		a.addType(document.getType(URI.create(OWL.ObjectProperty.getURI())));
		
		SSWAPType t = document.getType(TYPE_T);
		
		a.addRange(t);
		
		assertTrue(modelContainsAxiom(document, A_RANGE_T));
		
		assertEquals(TYPE_T.toString(), a.getObjectPredicateRange().toString());
	}
	
	/**
	 * Executes the code for adding and checking rdfs:range information for datatype properties
	 * @throws URISyntaxException
	 */
	@Test
	public void testObjectPredicateRanges() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));		
		
		SSWAPPredicate p = document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/hasQTL"));
		
		SSWAPType range = p.getObjectPredicateRange();
		
		assertEquals("http://sswapmeet.sswap.info/qtl/QTL", range.getURI().toString());
		
		Collection<SSWAPType> ranges = p.getObjectPredicateRanges();
		
		assertEquals(1, ranges.size());
		assertEquals("http://sswapmeet.sswap.info/qtl/QTL", ranges.iterator().next().getURI().toString());	
		
		try {
			p.getDatatypePredicateRange();
			fail("getDatatypePredicateRange() did not generate an IllegalArgumentException for an ObjectProperty");
		}
		catch (IllegalArgumentException e) {
			// expected -- correct behavior
		}
		
		try {
			p.getDatatypePredicateRanges();
			fail("getDatatypePredicateyRanges() did not generate an IllegalArgumentException for an ObjectProperty");
		}
		catch (IllegalArgumentException e) {
			// expected -- correct behavior
		}
	}
	
	/**
	 * Executes the code for adding and checking rdfs:range information for datatype properties
	 * @throws URISyntaxException
	 */
	@Test
	public void testDatatypePredicateRange() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		a.addType(document.getType(URI.create(OWL.DatatypeProperty.getURI())));
		
		SSWAPType t = document.getType(TYPE_T);
		
		a.addRange(t);
		
		assertTrue(modelContainsAxiom(document, A_RANGE_T));
		
		assertEquals(TYPE_T.toString(), a.getDatatypePredicateRange());
	}
	
	@Test
	public void testDatatypeEnumPredicateRange() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate p = document.getPredicate(URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/onlyDirectAnnotationsToTerm"));
		
		assertEquals(XSD.xboolean.getURI(), p.getDatatypePredicateRange());
	}
	
	/**
	 * Executes the code for adding and checking rdfs:range information for datatype properties
	 * @throws URISyntaxException
	 */
	@Test
	public void testDatatypePredicateRanges() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));		
		
		SSWAPPredicate p = document.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/symbol"));
		
		String range = p.getDatatypePredicateRange();
		
		assertEquals(XSD.xstring.getURI(), range);
		
		Collection<String> ranges = p.getDatatypePredicateRanges();
		
		assertEquals(1, ranges.size());
		assertTrue(ranges.contains(XSD.xstring.getURI()));	
		
		try {
			p.getObjectPredicateRange();
			fail("getObjectPredicateRange() did not generate an IllegalArgumentException for a DatatypeProperty");
		}
		catch (IllegalArgumentException e) {
			// expected -- correct behavior
		}
		
		try {
			p.getObjectPredicateRanges();
			fail("getObjectPredicateRanges() did not generate an IllegalArgumentException for a DatatypeProperty");
		}
		catch (IllegalArgumentException e) {
			// expected -- correct behavior
		}
	}


	
	/**
	 * Executes the code for adding owl:equivalentProperty
	 * @throws URISyntaxException
	 */
	@Test
	public void addEquivalentPredicate() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		a.addType(document.getType(URI.create(OWL.ObjectProperty.getURI())));
		
		SSWAPPredicate b = document.getPredicate(PROPERTY_B);
		b.addType(document.getType(URI.create(OWL.ObjectProperty.getURI())));
		
		a.addEquivalentPredicate(b);
		
		assertTrue(modelContainsAxiom(document, A_EQUIVALENT_PROPERTY_B));	
	}
	
	/**
	 * Executes the code for adding owl:inverseOf information
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testInverseOf() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		a.addType(document.getType(URI.create(OWL.ObjectProperty.getURI())));
		
		SSWAPPredicate b = document.getPredicate(PROPERTY_B);
		b.addType(document.getType(URI.create(OWL.ObjectProperty.getURI())));
		
		a.addInverseOf(b);
		
		assertTrue(modelContainsAxiom(document, A_INVERSE_OF_B));	
	}
	
	@Test
	public void testAddLabel() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		
		a.addLabel("Test Label");
		
		assertTrue(modelContainsAxiom(document, A_LABEL_TEST_LABEL));
	}
	
	@Test
	public void testAddComment() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		
		a.addComment("Test Comment");
		
		assertTrue(modelContainsAxiom(document, A_COMMENT_TEST_COMMENT));
	}
	

	@Test
	public void testAddAnnotationPredicate() throws URISyntaxException {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		SSWAPPredicate a = document.getPredicate(PROPERTY_A);
		a.addType(document.getType(URI.create(OWL.ObjectProperty.toString())));
		
		SSWAPPredicate rdfsSeeAlso = document.getPredicate(URI.create(RDFS.seeAlso.getURI()));
		
		a.addAnnotationPredicate(rdfsSeeAlso, document.createIndividual(URI.create(NS + "individual")));
		
		document.serialize(System.out);
	}
	

	@Test
	public void testRetrievedPredicateReasoning() {
		SSWAPDocument document = SSWAP.createSSWAPDocument(URI.create(NS + "emptyModel"));
		
		// there is no information about these properties in the document
		SSWAPPredicate predicate1 = document.getPredicate(URI.create("http://sswapmeet.sswap.info/exec/args"));
		SSWAPPredicate predicate2 = document.getPredicate(URI.create("http://sswapmeet.sswap.info/exec/datatypeProperty"));
		
		// these queries should trigger automatic fetching of property information
		assertTrue(predicate1.isSubPredicateOf(predicate2));
		assertFalse(predicate2.isSubPredicateOf(predicate1));
		
		assertFalse(predicate1.isObjectPredicate());
		assertFalse(predicate2.isObjectPredicate());
		
		assertTrue(predicate1.isDatatypePredicate());
		assertTrue(predicate2.isDatatypePredicate());
				
		String range = predicate1.getDatatypePredicateRange();
		
		assertNotNull(range);
		
		assertEquals(XSD.xstring.toString(), range);
		
		SSWAPPredicate nonExistentPredicate = document.getPredicate(URI.create("http://sswapmeet.sswap.info/sswap/doesNotExist"));
		assertFalse(nonExistentPredicate.isSubPredicateOf(predicate1));
		
		// technically, anonymous properties are not allowed in RDF/OWL but below I am trying to test code's behavior in such a situation
		SSWAPPredicate nonExistendPredicate2 = document.getPredicate(URI.create(ModelUtils.generateBNodeId()));
		assertFalse(nonExistendPredicate2.isSubPredicateOf(predicate1));
	}
	
	/**
	 * Verify that we can handle reasoning with properties whose range is an anonymous datatype (defined via rdfs:Datatype).
	 * This tests the issue reported by Shuly on April 7, 2011. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAnonymousDataRangeHandling() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/poAnnotation.owl");
		SSWAPDocument document = SSWAP.getResourceGraph(fis, SSWAPDocument.class);
		
		SSWAPPredicate predicate = document.getPredicate(URI.create("http://localhost:8080/paAnnotations/ontologies/poAnnotation/evidenceCode"));
		
		// it must NOT throw InonsistentOntologyException
		((ReasoningServiceImpl) document.getReasoningService()).getDomain(predicate);
	}
	
	@Test
	public void testDetectPropertyType() throws Exception {
		SSWAPDocument doc = SSWAP.createSSWAPDocument();
		SSWAPPredicate annotationPredicate = doc.getPredicate(URI.create("http://sswapmeet.sswap.info/oboInOwl/hasSubset"));
		
		assertTrue(annotationPredicate.isAnnotationPredicate());
		assertFalse(annotationPredicate.isDatatypePredicate());
		assertFalse(annotationPredicate.isObjectPredicate());
		
		SSWAPPredicate datatypePredicate = doc.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol"));
		
		assertFalse(datatypePredicate.isAnnotationPredicate());
		assertTrue(datatypePredicate.isDatatypePredicate());
		assertFalse(datatypePredicate.isObjectPredicate());
		
		SSWAPPredicate objectPredicate = doc.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/hasTrait"));
		
		assertFalse(objectPredicate.isAnnotationPredicate());
		assertFalse(objectPredicate.isDatatypePredicate());
		assertTrue(objectPredicate.isObjectPredicate());		
	}
	
	/**
	 * Checks whether the underlying model contains a specific Jena statement
	 * (used to verify the result of methods of SSWAPPredicate that add triples to the 
	 * underlying model.)
	 * 
	 * @param model the SSWAP model that should contain the axiom
	 * @param statement the statement that should be checked for presence in the model
	 * @return true if the model contains the specified axiom
	 */
	private boolean modelContainsAxiom(SSWAPModel model, Statement statement) {
		Model jenaModel = ((ModelImpl) model).getSourceModel().getModel();
		
		return jenaModel.contains(statement);
	}
	
	@Test
	public void testReservedPredicates() throws Exception {
		SSWAPDocument doc = SSWAP.createSSWAPDocument();
		SSWAPPredicate annotationPredicate = doc.getPredicate(URI.create("http://sswapmeet.sswap.info/oboInOwl/hasSubset"));

		assertFalse(annotationPredicate.isReserved());
		
		SSWAPPredicate rdfType = doc.getPredicate(URI.create(Namespaces.RDF_NS + "type"));
		assertTrue(rdfType.isReserved());
		
		SSWAPPredicate rdfsSubClassOf = doc.getPredicate(URI.create(Namespaces.RDFS_NS + "subClassOf"));		
		assertTrue(rdfsSubClassOf.isReserved());
		
		SSWAPPredicate xsdString = doc.getPredicate(URI.create(Namespaces.XSD_NS + "string"));
		assertTrue(xsdString.isReserved());
		
		SSWAPPredicate owlSameAs = doc.getPredicate(URI.create(Namespaces.OWL_NS + "sameAs"));
		assertTrue(owlSameAs.isReserved());
		
		SSWAPPredicate mapsTo = doc.getPredicate(URI.create(Namespaces.SSWAP_NS + "mapsTo"));
		assertTrue(mapsTo.isReserved());
	}
}
