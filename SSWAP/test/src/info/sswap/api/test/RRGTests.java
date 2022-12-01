/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPProtocol;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.model.ProtocolImpl.MappingValidator.MappingType;
import info.sswap.impl.empire.model.RDGImpl;
import info.sswap.impl.empire.model.ReasoningServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Tests for RRGs
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class RRGTests {
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:RRGTest#";
	
	/**
	 * The URI for an RDG (QtlByTraitAccession) used for tests
	 */
	private static final String QTL_BY_TRAIT_ACCESSION_URI = "http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession";
	
	private static final String QTL_BY_TRAIT_NAME_URI = "http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-name";

	/**
	 * The path to the local file containing the data for QtlByTraitAccession RDG (to avoid a network connection during
	 * the test)
	 */
	private static final String QTL_BY_TRAIT_ACCESSION_FILE = "test/data/qtl-by-trait-accession.owl";
	
	/**
	 * The RDG object for QtlByTraitAccesion
	 */
	private static RDG qtlByTraitAccessionRDG;
	
	/**
	 * The RIG object for QtlByTraitAccession
	 */
	private static RIG qtlByTraitAccessionRIG;
	
	/**
	 * Gets a dereferenced RDG object from a local file.
	 * 
	 * @param uri
	 *            the URI of the RDG (if it weren't read from a local file)
	 * @param path
	 *            the path to the local file
	 * @return the dereferenced RDG
	 * @throws URISyntaxException
	 *             if the URI passed is not a syntactically valid URI
	 * @throws IOException
	 *             if an I/O error occurs while reading the file
	 */
	private static RDG getDereferencedRDG(String uri, String path) throws URISyntaxException, IOException {
		FileInputStream fis = new FileInputStream(path);
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, new URI(uri));
		fis.close();

		return rdg;
	}
	
	/**
	 * Initializes the test data (RDGs and RIGs) before the actual tests are run.
	 * 
	 * @throws URISyntaxException
	 *             if the constants encode a URI that is not syntactically valid
	 * @throws IOException
	 *             if I/O error occurs while reading the test data
	 */
	@BeforeClass
	public static void initRDGs() throws URISyntaxException, IOException {
		qtlByTraitAccessionRDG = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, QTL_BY_TRAIT_ACCESSION_FILE);
		qtlByTraitAccessionRIG = qtlByTraitAccessionRDG.getRIG();
		
		// set the value for qtl:accessionID to "abc"
		SSWAPResource resource = qtlByTraitAccessionRIG.getResource();		
		SSWAPSubject subject = resource.getGraph().getSubject();		
		SSWAPPredicate predicate = qtlByTraitAccessionRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"));		
		subject.setProperty(predicate, "abc", URI.create(XSD.xstring.getURI()));
		
		SSWAPObject object = subject.getObject();
		
		object.setProperty(object.getDocument().getPredicate(URI.create("http://sswapmeet.sswap.info/map/units")), "band");
	}
	
	/**
	 * Tests the creation of an RRG from an RIG
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCreateRRG() throws URISyntaxException, ValidationException {
		RRG rrg = qtlByTraitAccessionRIG.getRRG();
		
		assertNotNull(rrg);
		
		assertEquals(qtlByTraitAccessionRIG.getURI(), rrg.getURI());
		
		SSWAPResource resource = rrg.getResource();		
		assertNotNull(resource);
		
		SSWAPSubject subject = resource.getGraph().getSubject();		
		assertNotNull(subject);
		
		SSWAPProperty property = subject.getProperty(rrg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		assertNotNull(property);
		
		SSWAPElement value = property.getValue();
		assertNotNull(value);
		
		assertEquals("abc", value.asString());
	}
	
	@Test
	public void testCreateRIGFromRRG() throws ValidationException {
		RRG rrg = qtlByTraitAccessionRIG.getRRG();
		
		RDG rdgTraitName = SSWAP.getRDG(URI.create(QTL_BY_TRAIT_NAME_URI));
		
		RIG newRIG = rrg.createRIG(rdgTraitName);
		assertNotNull(newRIG);
	}
	
	@Test
	public void testRRGValidate() throws Exception {
		RRG rrg = qtlByTraitAccessionRIG.getRRG();
		
		List<SSWAPObject> objects = new LinkedList<SSWAPObject>(rrg.getResource().getGraph().getSubject().getObjects());
		// additional object
		objects.add(rrg.createObject());
		
		rrg.validate();
	}
	
	/**
	 * Tests the whole invocation cycle; that is, creation of an RIG from an RDG by the client, serializing it (simulating
	 * sending of the RIG to the provider), deserializing it (simulating the read on the provider side),
	 * validating it on the provider side, creation of an RRG from the RIG on the provider side, setting
	 * the results on the RRG, serializing it back (simulating sending the response back to the provider),
	 * deserializing it (simulating the read on the client side), and reading the results from the RRG.
	 * 
	 * @throws ValidationException
	 * @throws URISyntaxException
	 */
	@Test
	public void testInvocationCycle() throws ValidationException, URISyntaxException {
		// serialization to the provider
		
		SSWAPSubject subject1 = qtlByTraitAccessionRIG.getResource().getGraph().getSubject();
		
		subject1.setProperty(qtlByTraitAccessionRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject1.setProperty(qtlByTraitAccessionRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
				
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		qtlByTraitAccessionRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);			
		RRG rrgProviderSide = rigProviderSide.getRRG();
		
		// RRG at this point should not be immutable -- test mutability
		rrgProviderSide.setNsPrefix("testPrefixProvider", URI.create(NS + "testPrefixProvider"));
		
		SSWAPResource resource = rrgProviderSide.getResource();
		SSWAPSubject subject = resource.getGraph().getSubject();
		SSWAPObject object = subject.getObject();
				
		// add a response to the sswap:Object (the object is implicitly of type qtl:QTL, as can be inferred
		// from the the domains of properties, and qtl:QTL requires that it has at least the qtl:symbol property)
		SSWAPPredicate symbol = rrgProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/symbol"));
		object.setProperty(symbol, "ABC", URI.create(XSD.xstring.toString()));
		
		// serialization back to the client
		intermediateOutputStream = new ByteArrayOutputStream();
		rrgProviderSide.serialize(intermediateOutputStream);
		intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing of the result on the client side
		RRG rrgClientSide = qtlByTraitAccessionRIG.getRRG(intermediateInputStream);
		
		// RRG at this point should be immutable -- testing the immutability it
		try {
			rrgClientSide.setNsPrefix("testPrefix", URI.create(NS + "testPrefix"));
			fail("Expected IllegalArgumentException not thrown");
		}
		catch (IllegalArgumentException e) {
			// correct behavior
		}
		
		resource = rrgClientSide.getResource();
		subject = resource.getGraph().getSubject();
		object = subject.getObject();
		
		SSWAPProperty symbolProperty = object.getProperty(rrgClientSide.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/symbol")));		
		assertNotNull(symbolProperty);
		
		SSWAPElement symbolValue = symbolProperty.getValue();
		assertNotNull(symbolValue);
		assertEquals("ABC", symbolValue.asString());
		
		RQG rqg = rrgClientSide.createRQG();
		
		SSWAPGraph rqgGraph = rqg.getResource().getGraph();
		SSWAPSubject rqgSubject = rqgGraph.getSubject();
		
		Collection<SSWAPType> rqgSubjectTypes = rqgSubject.getTypes();
	
		assertTrue(rqgSubjectTypes.contains(rqg.getType(URI.create("http://sswapmeet.sswap.info/qtl/QTLs"))));
		assertTrue(rqgSubjectTypes.contains(rqg.getType(URI.create("http://sswapmeet.sswap.info/qtl/QTL"))));
		assertTrue(rqgSubjectTypes.contains(rqg.getType(URI.create("http://sswapmeet.sswap.info/taxa/Taxa"))));
		assertTrue(rqgSubjectTypes.contains(rqg.getType(URI.create("http://sswapmeet.sswap.info/map/MapPosition"))));
		assertTrue(rqgSubjectTypes.contains(rqg.getType(URI.create("http://sswapmeet.sswap.info/map/Maps"))));
		assertTrue(rqgSubjectTypes.contains(rqg.getType(URI.create("http://sswapmeet.sswap.info/trait/Trait"))));
		assertTrue(rqgSubjectTypes.contains(rqg.getType(URI.create("http://sswapmeet.sswap.info/trait/Traits"))));
	}
	
	@Test(expected=ValidationException.class)
	@Ignore
	public void testValidationRRGBecomesOWLFull() throws ValidationException, URISyntaxException {
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		qtlByTraitAccessionRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);
		
		// add a non-OWL-DL construct -- a property that is a super property of owl:topDataProperty
		SSWAPPredicate testPredicate = rigProviderSide.getPredicate(URI.create("file:test/data/testProperty"));

		rigProviderSide.getResource().getGraph().getSubject().setProperty(testPredicate, "1");
		rigProviderSide.getRRG();
	}
	
	@Test
	public void testValidationIncomingOWLFullRIG() throws ValidationException, URISyntaxException {
		RIG rigClientSide = qtlByTraitAccessionRDG.getRIG();

		// add a non-OWL-DL construct -- a property that is a super property of owl:topDataProperty
		SSWAPPredicate testProperty = rigClientSide.getPredicate(URI.create(NS + "testProperty"));
		testProperty.addType(rigClientSide.getType(URI.create(OWL2.DatatypeProperty.toString())));
		SSWAPPredicate topDataProperty = rigClientSide.getPredicate(URI.create(OWL2.topDataProperty.toString()));
		topDataProperty.addSubPredicateOf(testProperty);
		
		SSWAPSubject subject1 = rigClientSide.getResource().getGraph().getSubject();
		
		subject1.setProperty(rigClientSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject1.setProperty(rigClientSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rigClientSide.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);
		
		SSWAPObject object = rigProviderSide.getResource().getGraph().getSubject().getObject();
		object.setProperty(object.getDocument().getPredicate(URI.create("http://sswapmeet.sswap.info/map/units")), "band");
			
		// the statement below must NOT throw ValidationException (since the RIG was already OWL Full)
		rigProviderSide.getRRG();
	}
	
	/**
	 * Test for a bug when objects in RIG where not persisted during the conversion to RRG
	 * 
	 * @throws Exception
	 */
	@Test 
	public void testPersistenceOfRIGObjectsToRRG() throws Exception {
		SSWAPSubject subject1 = qtlByTraitAccessionRIG.getResource().getGraph().getSubject();
		
		subject1.setProperty(qtlByTraitAccessionRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject1.setProperty(qtlByTraitAccessionRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");				
		
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		qtlByTraitAccessionRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);
		
		// Mimic the operation of AbstractSSWAPServlet (at that time) where the behavior was first observed
		
		SSWAPResource rigResource = rigProviderSide.getResource();
		
		// build a map from translated RIG SSWAPSubjects to their RIG SSWAPObjects
		Map<SSWAPSubject,Collection<SSWAPObject>> map = new HashMap<SSWAPSubject,Collection<SSWAPObject>>();

		for ( SSWAPGraph sswapGraph : rigResource.getGraphs() ) {
			for ( SSWAPSubject sswapSubject : sswapGraph.getSubjects() ) {
				map.put(rigProviderSide.translate(sswapSubject),sswapSubject.getObjects());
			}
		}

		for (SSWAPSubject s : map.keySet()) {
			for (SSWAPObject o :  map.get(s)) {
	    		SSWAPPredicate outPredicate = o.getDocument().getPredicate(URI.create("http://sswapmeet.sswap.info/sequence/sequenceStr"));
	    		o.addProperty(outPredicate, "abc");
	    	    
	    	    SSWAPPredicate hasTraitPredicate = o.getDocument().getPredicate(URI.create("http://sswapmeet.sswap.info/trait/hasTrait"));
	    	    
	    	    SSWAPIndividual trait = o.getDocument().createIndividual();
	    	    SSWAPType traitType = o.getDocument().getType(URI.create("http://sswapmeet.sswap.info/trait/Trait"));
	    	    trait.addType(traitType);
	    	    
	    	    SSWAPPredicate traitName = o.getDocument().getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name"));
	    	    trait.setProperty(traitName, "traitName");
	    	    
	    	    SSWAPPredicate traitSymbol = o.getDocument().getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol"));
	    	    trait.setProperty(traitSymbol, "traitSymbol");

	    	    o.setProperty(hasTraitPredicate, trait);
			}
		}
		
		RRG rrgProviderSide = rigProviderSide.getRRG();

		// serialization back to the client
		intermediateOutputStream = new ByteArrayOutputStream();
		rrgProviderSide.serialize(intermediateOutputStream);
		intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing of the result on the client side
		RRG rrgClientSide = qtlByTraitAccessionRIG.getRRG(intermediateInputStream);
		
		for ( SSWAPGraph sswapGraph : rrgClientSide.getResource().getGraphs() ) {
			for ( SSWAPSubject sswapSubject : sswapGraph.getSubjects() ) {
				for (SSWAPObject sswapObject : sswapSubject.getObjects()) {
					SSWAPProperty rrgProperty = sswapObject.getProperty(rrgClientSide.getPredicate(URI.create("http://sswapmeet.sswap.info/sequence/sequenceStr")));
					
					assertNotNull(rrgProperty);
					assertEquals("abc", rrgProperty.getValue().asString());
					
					SSWAPProperty hasTrait = sswapObject.getProperty(rrgClientSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/hasTrait")));
					
					assertNotNull(hasTrait);
					
					SSWAPElement traitValue = hasTrait.getValue();
					
					assertNotNull(traitValue);
					assertTrue(traitValue.isIndividual());
					
					SSWAPIndividual trait = traitValue.asIndividual();
					assertNotNull(trait);
					
					SSWAPProperty nameProperty = trait.getProperty(rrgClientSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")));
					assertNotNull(nameProperty);
					assertNotNull(nameProperty.getValue());
					assertEquals("traitName", nameProperty.getValue().asString());
					
					SSWAPProperty symbolProperty = trait.getProperty(rrgClientSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")));
					assertNotNull(symbolProperty);
					assertNotNull(symbolProperty.getValue());
					assertEquals("traitSymbol", symbolProperty.getValue().asString());
				}
			}
		}
	}
	
	@Test
	public void testPersistenceRRGAnnotationDetails() throws Exception {
	    FileInputStream fis = new FileInputStream("test/data/AnnotationDetails-rig.owl");
	    
	    RIG rig = SSWAP.getResourceGraph(fis, RIG.class, URI.create("http://localhost:8080/poAnnotations/resources/annotationDetails/AnnotationDetails"));
	    
	    System.out.println("RIG");
	    rig.serialize(System.out);
	    
	    RRG rrg = rig.getRRG();
	    
	    System.out.println("RRG");
	    rrg.serialize(System.out);
	}
	
	/**
	 * Test if RRGs can contain SSWAPObjects that are subclasses (and only subclasses) of the RDG 
	 */
	@Test
	public void testRdgRigRrgObject() {
		
		/*
		 * Problem setup:
		 * 
		 * Subsumption hierarchy:
		 * Axiom: C subClassOf B subClassOf A
		 * 
		 * Now consider:
		 * 
		 *    Graph   SSWAPObject    Expected	Observed
		 *    -----   -----------    --------	--------
		 *    RDG         B
		 * 
		 * 1. RIG1        C	 		  Reject    Reject		// OK, because while something of type C is of type B, the RDG
		 * 					 								//     does not guarantee it can honor the request to return
		 * 					 								//     something of class C, only B.
		 * 
		 * 2. RIG2        A	 		  Accept	Accept		// OK: because the RDG guarantees to return something of at most
		 * 					 								//     type B, which is a subtype of the request A. Use this RIG for RRGs below:
		 * 
		 * 3. RRG1        A	 		  Reject	Accept		// PROBLEM: because the RDG guarantees specialization down to the level of B
		 * 4. RRG2        B	 		  Accept	Accept		// OK:      because B is a subClass of the RIG2's request A and the RDG's contract B
		 * 5. RRG3        C	 		  Accept	Reject		// PROBLEM: because C is a subClass of the RIG2's request A and the RDG's contract B,
		 * 									  				//		    so if it returns something of C, it is satisfying both constraints
		 * 
		 * Summary:
		 *  * it's accepting RIGs w/ superclasses, but not subclasses, on the RDG's SSWAP Object (right)
		 *  * it's allowing RRGs w/ superclasses, but not subclasses, on the RDG's SSWAP Object (this turns out to be wrong)
		 * 
		 * The current RRG rule appears reversed; namely, it allows the return of classes more
		 * general than the RDG contract, while not allowing more specialized.
		 * 
		 * The RRG rule should be: RRG SSWAP Object is a subClassOf RDG SSWAP Object
		 * 
		 * There is one wrinkle with this change: if sent a RIG with 'A', and nothing is done, then the resultant
		 * default RRG--also of A--is invalid.  The current code returns the RIG on a server getRRG() failure,
		 * so a null-mapping would be the same.
		 *  
		 */
		
		final String NS = "tag:sswap.info:2011-03-18:sswap:java:api:RRGTests#RDG-RIG-RRG-Test/";
		final URI canonicalURI = URI.create("http://sswap.info/examples/resources/canonical/canonicalResource");
		
		int failures = 0;
		final URI A_URI = URI.create(NS + "A");
		final URI B_URI = URI.create(NS + "B");
		final URI C_URI = URI.create(NS + "C");

		RDG canonicalRDG = SSWAP.getRDG(canonicalURI);

		// make new RDG w/ sswapObject of type B
	    RDG rdg = SSWAP.getRDG(canonicalURI);
		{			
			SSWAPType A = rdg.getType(A_URI);
			SSWAPType B = rdg.getType(B_URI);
			SSWAPType C = rdg.getType(C_URI);
			
			C.addSubClassOf(B);
			B.addSubClassOf(A);

			rdg.getResource().getGraph().getSubject().getObject().addType(B);

			rdg.serialize(System.out);
		}
		
		// make new RIG (from "scratch") w/ sswapObject of type C; this should not succeed
		RIG rig = null;
		{
		    RIG clientRIG = SSWAP.getResourceGraph(canonicalRDG.getInputStream(), RIG.class, canonicalURI);
			
			SSWAPType A = clientRIG.getType(A_URI);
			SSWAPType B = clientRIG.getType(B_URI);
			SSWAPType C = clientRIG.getType(C_URI);
			
			C.addSubClassOf(B);
			B.addSubClassOf(A); 
			
			clientRIG.getResource().getGraph().getSubject().getObject().addType(C);
			clientRIG.serialize(System.out);

			try {
				rig = rdg.getRIG(clientRIG.getInputStream());
				failures++;
				System.err.println("1/5: getRIG() case sswapObject type C passed, but it should have failed");
			} catch ( Exception e ) {
				;	// a thrown exception is the success behavior
			}

		}
		
		// make new RIG w/ sswapObject of type A; this should make a valid RIG
		rig = null;
		{
		    RIG clientRIG = SSWAP.getResourceGraph(canonicalRDG.getInputStream(), RIG.class, canonicalURI);
			
			SSWAPType A = clientRIG.getType(A_URI);
			SSWAPType B = clientRIG.getType(B_URI);
			SSWAPType C = clientRIG.getType(C_URI);
			
			C.addSubClassOf(B);
			B.addSubClassOf(A);
			
			clientRIG.getResource().getGraph().getSubject().getObject().addType(A);
			clientRIG.serialize(System.out);

			try {
				rig = rdg.getRIG(clientRIG.getInputStream());
			} catch ( Exception e ) {
				failures++;
				System.err.println("2/5: getRIG() case sswapObject type A failed validation, but it should have passed");
				e.printStackTrace();
			}

		}
		
		// take RIG from above to make a new RRG w/ sswapObject of type A; this should be invalid
		RRG rrg = null;
		{
			try {
				rrg = rig.getRRG();	// should throw on "success"; i.e., should fail here
				failures++;
				System.err.println("3/5: getRRG() case sswapObject type A passed validation, but it should have failed");
			} catch ( Exception e ) {
				;	// a thrown exception is the success behavior
			}
		}
		
		// edit RIG to make a new RRG w/ sswapObject of type B; this should be valid
		rrg = null;
		{
			SSWAPGraph sswapGraph = rig.getResource().getGraph();
			SSWAPSubject sswapSubject = sswapGraph.getSubject();

			SSWAPObject sswapObject = rig.createObject();
			sswapSubject.setObject(sswapObject);	// should replace old
			SSWAPType B = rig.getType(B_URI);
			sswapObject.addType(B);

			rig.serialize(System.out);

			try {
				rrg = rig.getRRG();
				rrg.serialize(System.out);
			} catch ( Exception e ) {
				failures++;
				System.err.println("4/5: getRRG() case sswapObject type B failed validation, but it should have passed");
				e.printStackTrace();
			}
		}
		
		// edit RIG to make a new RRG w/ sswapObject of type C; this should be valid
		rrg = null;
		{
			SSWAPGraph sswapGraph = rig.getResource().getGraph();
			SSWAPSubject sswapSubject = sswapGraph.getSubject();

			SSWAPObject sswapObject = rig.createObject();
			sswapSubject.setObject(sswapObject);	// should replace old
			SSWAPType C = rig.getType(C_URI);

			sswapObject.addType(C);

			rig.serialize(System.out);

			try {
				rrg = rig.getRRG();
				rrg.serialize(System.out);
			} catch ( Exception e ) {
				failures++;
				System.err.println("5/5: getRRG() case sswapObject type C failed validation, but it should have passed");
				e.printStackTrace();
			}
		}
		
		if ( failures > 0 ) {
			fail("RDG-RIG-RRG test: failed " + failures + " of 5");
		}
	}
	
	@Test
	public void testRRGTypeTriples() throws DataAccessException, ValidationException {
		final String NS = "tag:sswap.info:2011-03-18:sswap:java:api:RRGTests#RDG-RIG-RRG-Test/";
		final URI canonicalURI = URI.create("http://sswap.info/examples/resources/canonical/canonicalResource");
		
		final URI A_URI = URI.create(NS + "A");
		final URI B_URI = URI.create(NS + "B");
		final URI C_URI = URI.create(NS + "C");

		RDG rdg = SSWAP.getRDG(canonicalURI);
		{
			SSWAPType A = rdg.getType(A_URI);
			SSWAPType B = rdg.getType(B_URI);
			SSWAPType C = rdg.getType(C_URI);

			C.addSubClassOf(B);
			B.addSubClassOf(A);

			rdg.getResource().getGraph().getSubject().getObject().addType(A);
		}

		RIG rig = rdg.getRIG();		
		{
			SSWAPGraph sswapGraph = rig.getResource().getGraph();
			SSWAPSubject sswapSubject = sswapGraph.getSubject();

			SSWAPObject sswapObject = rig.createObject();
			sswapSubject.setObject(sswapObject);	// should replace old
			SSWAPType C = rig.getType(C_URI);
			sswapObject.addType(C);
		}

		RRG rrg = rig.getRRG();
		{
			SSWAPGraph sswapGraph = rrg.getResource().getGraph();
			SSWAPSubject sswapSubject = sswapGraph.getSubject();

			SSWAPObject object = sswapSubject.getObject();
			
			assertTrue(object.getDeclaredTypes().contains(rrg.getType(A_URI)));
			assertFalse(object.getDeclaredTypes().contains(rrg.getType(B_URI)));
			assertTrue(object.getDeclaredTypes().contains(rrg.getType(C_URI)));
		}		
	}
	
	@Test
	public void testCrossDocumentReasoning() throws DataAccessException, ValidationException {
		final String NS = "tag:sswap.info:2011-03-18:sswap:java:api:RRGTests#RDG-RIG-RRG-Test/";
		final URI canonicalURI = URI.create("http://sswap.info/examples/resources/canonical/canonicalResource");
		
		final URI A_URI = URI.create(NS + "A");
		final URI B_URI = URI.create(NS + "B");
		final URI C_URI = URI.create(NS + "C");

		RDG rdg = SSWAP.getRDG(canonicalURI);
		{
			SSWAPType A = rdg.getType(A_URI);
			SSWAPType B = rdg.getType(B_URI);
			SSWAPType C = rdg.getType(C_URI);

			C.addSubClassOf(B);
			B.addSubClassOf(A);

			rdg.getResource().getGraph().getSubject().getObject().addType(A);
		}
		
		RIG rig = rdg.getRIG();		
		{
			SSWAPGraph sswapGraph = rig.getResource().getGraph();
			SSWAPSubject sswapSubject = sswapGraph.getSubject();

			SSWAPObject sswapObject = rig.createObject();
			sswapSubject.setObject(sswapObject);	// should replace old
			SSWAPType C = rig.getType(C_URI);
			sswapObject.addType(C);
		}	

		RRG rrg = rig.getRRG();
		{
			SSWAPGraph sswapGraph = rrg.getResource().getGraph();
			SSWAPSubject sswapSubject = sswapGraph.getSubject();

			SSWAPObject object = sswapSubject.getObject();
			
			assertTrue(object.getDeclaredTypes().contains(rrg.getType(A_URI)));
			assertFalse(object.getDeclaredTypes().contains(rrg.getType(B_URI)));
			assertTrue(object.getDeclaredTypes().contains(rrg.getType(C_URI)));
		}
		
		
		((RDGImpl) rdg).getModel().write(System.out);
		
		assertTrue(((ReasoningServiceImpl) rrg.getReasoningService()).isMappingValid(rrg.getResource().getGraph().getSubject().getObject(), MappingType.SUB, rdg.getResource().getGraph().getSubject().getObject()));
		assertTrue(rrg.getResource().getGraph().getSubject().getObject().getType().isSubTypeOf(rdg.getResource().getGraph().getSubject().getObject().getType()));
	}
	
	@Test
	public void testContigRRGObjectValidation() throws Exception {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap.dendrome.ucdavis.edu/resources/contigService/ContigService"));
		
		RIG rig = rdg.getRIG();
		
		SSWAPSubject subject = rig.getResource().getGraph().getSubject();
		
		List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
		subjects.add(subject);
		
		subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/id")), "0_10022", URI.create(XSD.xstring.toString()));
		
		subject = rig.createSubject();
		subject.addType(rig.getType(URI.create("http://sswapmeet.sswap.info/treeGenes/requests/ContigServiceRequest")));
		subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/id")), "0_10023", URI.create(XSD.xstring.toString()));
		
		subject.setObject(rig.createObject());
		
		subjects.add(subject);
		
		rig.getResource().getGraph().setSubjects(subjects);
		
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		rig.serialize(bos);
		
		rig = rdg.getRIG(new ByteArrayInputStream(bos.toByteArray()));
		
		subjects = new LinkedList<SSWAPSubject>(rig.getResource().getGraph().getSubjects());
		
		for (SSWAPSubject aSubject : subjects) {
			SSWAPObject object = rig.createObject();
			aSubject.addObject(object);

			object.addType(rig.getType(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/Contig")));
			object.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/id")), "0_10023", URI.create(XSD.xstring.toString()));
		}
		rig.serialize(System.out);
		
		rig.getRRG();		
	}
	
	@Test
	public void testContigRRGObjectValidation2() throws Exception {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap.dendrome.ucdavis.edu/resources/contigService/ContigService"));
		
		RIG rig = rdg.getRIG();
		
		SSWAPSubject subject = rig.getResource().getGraph().getSubject();
		
		List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
		subjects.add(subject);
		
		//subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/id")), "0_10022", URI.create(XSD.xstring.toString()));
		
		subject = rig.createSubject();
		subject.addType(rig.getType(URI.create("http://sswapmeet.sswap.info/treeGenes/requests/ContigServiceRequest")));
		subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/id")), "0_10023", URI.create(XSD.xstring.toString()));
		
		SSWAPObject object = rig.createObject();
		object.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/id")), "0_10023", URI.create(XSD.xstring.toString()));
		object.addType(rig.getType(URI.create("http://sswapmeet.sswap.info/treeGenes/contig/Contig")));
		subject.setObject(object);		
		
		subjects.add(subject);
		
		subject = rig.createSubject();
		subject.addType(rig.getType(URI.create("http://sswapmeet.sswap.info/treeGenes/requests/ContigServiceRequest")));
		subject.setProperty(rig.getPredicate(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/id")), "0_10023", URI.create(XSD.xstring.toString()));
		
		object = rig.createObject();
		object.addType(rig.getType(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/Contig")));
		object.setProperty(rig.getPredicate(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/id")), "0_10023", URI.create(XSD.xstring.toString()));
		subject.setObject(object);
		
		subjects.add(subject);
		
		rig.getResource().getGraph().setSubjects(subjects);
		rig.serialize(System.out);
		
		rig.getRRG();		
	}
	
	@Test
	@Ignore
	public void testRRGObjectValidationSubjectsViolateCardRestr() throws Exception {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap.dendrome.ucdavis.edu/resources/contigService/ContigService"));
		
		RIG rig = rdg.getRIG();

		SSWAPSubject subject = rig.getResource().getGraph().getSubject();
		
		List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
		subjects.add(subject);
		
		subject.setProperty(rig.getPredicate(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/id")), "0_10022", URI.create(XSD.xstring.toString()));
		subject.setObject(rig.createObject());
		
		subject = rig.createSubject();
		subject.addType(rig.getType(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/Contig")));
		// do not add id here
		
		subject.setObject(rig.createObject());
		
		subjects.add(subject);
		
		rig.getResource().getGraph().setSubjects(subjects);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		rig.serialize(bos);
		
		rig = rdg.getRIG(new ByteArrayInputStream(bos.toByteArray()));
		
		subjects = new LinkedList<SSWAPSubject>(rig.getResource().getGraph().getSubjects());

		for (SSWAPSubject aSubject : subjects) {
			if (aSubject.getProperty(rig.getPredicate(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/id"))) != null) {
				// add a contig object only to subjects with an id (subjects without id are not valid subjects and they should not have passed
				// cardinality validation) -- therefore, a lack of proper objects there should not cause an issue during RRG validation
				SSWAPObject object = rig.createObject();
				aSubject.addObject(object);

				object.addType(rig.getType(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/Contig")));				
			}
		}
		
		// actual RRG validation (should pass)
		rig.getRRG();
	}
	
	@Test
	@Ignore
	public void testRRGSubjectObjectSameURI() throws Exception {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap.dendrome.ucdavis.edu/resources/contigService/ContigService"));
		
		RIG rig = rdg.getRIG();

		List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
		
		SSWAPSubject subject = rig.createSubject(URI.create("urn:sswap:test:contig"));
		subject.addType(rig.getType(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/Contig")));
		subject.setProperty(rig.getPredicate(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/id")), "0_10022", URI.create(XSD.xstring.toString()));
		
		subject.setObject(rig.createObject());
		
		subjects.add(subject);
		
		rig.getResource().getGraph().setSubjects(subjects);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		rig.serialize(bos);
		
		rig = rdg.getRIG(new ByteArrayInputStream(bos.toByteArray()));
		
		subjects = new LinkedList<SSWAPSubject>(rig.getResource().getGraph().getSubjects());

		for (SSWAPSubject aSubject : subjects) {
			SSWAPObject object = rig.createObject(URI.create("urn:sswap:test:contig"));
			aSubject.addObject(object);

			object.addType(rig.getType(URI.create("http://sswap.dendrome.ucdavis.edu/ontologies/contig/Contig")));				
		}
		
		// actual RRG validation (should pass)
		rig.getRRG();
	}
	
	@Test
	public void testRRG2RIG() throws Exception {
		doTestRRG2RIG(1 /* rdgSubjectCount */, 1 /* rdgObjectCount */, 1 /* rrgObjectCount */);
		doTestRRG2RIG(1 /* rdgSubjectCount */, 1 /* rdgObjectCount */, 10 /* rrgObjectCount */);
		
		doTestRRG2RIG(1 /* rdgSubjectCount */, 2 /* rdgObjectCount */, 2 /* rrgObjectCount */);
		doTestRRG2RIG(1 /* rdgSubjectCount */, 2 /* rdgObjectCount */, 3 /* rrgObjectCount */);
				
		doTestRRG2RIG(2 /* rdgSubjectCount */, 1 /* rdgObjectCount */, 1 /* rrgObjectCount */);
	}
	
	private void doTestRRG2RIG(int rdgSubjectCount, int rdgObjectCount, int rrgObjectCount) throws Exception {
		for (boolean rdgAnonymousObjects : new boolean[] { true, false }) {
			for (boolean rrgAnonymousObjects : new boolean[] { true, false }) {
				RRG rrg = createTestRRG(rrgObjectCount, rrgAnonymousObjects);

				RDG rdg = createTestRDG(rdgSubjectCount, rdgObjectCount, rdgAnonymousObjects);

				RIG rig = rrg.createRIG(rdg);

				assertNotNull(rig);

				assertEquals(rrgObjectCount, rig.getResource().getGraph().getSubjects().size());

				for (SSWAPSubject subject : rig.getResource().getGraph().getSubjects()) {
					assertEquals(rdgObjectCount, subject.getObjects().size());				
					assertEquals(rrgAnonymousObjects, subject.isAnonymous());
					
					for (SSWAPObject object : subject.getObjects()) {
						assertEquals(rdgAnonymousObjects, object.isAnonymous());
					}
				}
			}
		}

	}
	
	private RDG createTestRDG(int subjectCount, int objectCount, boolean anonymousObjects) {
		return createTestRDG(1, subjectCount, objectCount, anonymousObjects);
	}
	
	private RDG createTestRDG(int graphCount, int subjectCount, int objectCount, boolean anonymousObjects) {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap.info/examples/resources/canonical/canonicalResource"));
		
		List<SSWAPGraph> graphs = new LinkedList<SSWAPGraph>();
		
		for (int j = 0; j < graphCount; j++) {
			SSWAPGraph graph = rdg.createGraph();
			
			List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();

			for (int i = 0; i < subjectCount; i++) {
				SSWAPSubject subject = rdg.createSubject();

				subject.setObjects(createObjects(rdg, objectCount, anonymousObjects, "urn:sswap:test:rdg:"));

				subjects.add(subject);
			}
			
			graph.setSubjects(subjects);
			
			graphs.add(graph);
		}
				
		rdg.getResource().setGraphs(graphs);		
		
		return rdg;
	}
	
	private RRG createTestRRG(int objectCount, boolean anonymousObjects) throws Exception {
		URL url = new URL("http://sswap.info/examples/resources/canonical/canonicalResource");
		InputStream is = url.openStream();
		
		RRG rrg = SSWAP.getResourceGraph(is, RRG.class);		
		is.close();
		
		rrg.getResource().getGraph().getSubject().setObjects(createObjects(rrg, objectCount, anonymousObjects, "urn:sswap:test:rrg:"));

		return rrg;
	}
	
	private Collection<SSWAPObject> createObjects(SSWAPProtocol protocol, int objectCount, boolean anonymousObjects, String uriTemplate) {
		List<SSWAPObject> objects = new LinkedList<SSWAPObject>();
		
		for (int i = 0; i < objectCount; i++) {
			SSWAPObject object = null;
			
			if (anonymousObjects) {
				object = protocol.createObject();
			}
			else {
				object = protocol.createObject(URI.create(uriTemplate + i));
			}
			
			objects.add(object);			
		}
		
		return objects;
	}
	
	private RDG createTestTypedRDG() {
		RDG rdg = createTestRDG(2 /* subject count */, 1 /* object count */, true /* anonymous objects */);
		
		SSWAPGraph graph = rdg.getResource().getGraph();
		
		int subjectIndex = 0;
		for (Iterator<SSWAPSubject> it = graph.getSubjects().iterator(); it.hasNext(); subjectIndex++) {
			SSWAPSubject subject = it.next();
			
			if (subjectIndex == 0) {
				subject.addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Plain")));
				subject.getObject().addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Jpeg")));
			}
			else {
				subject.addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Html")));
				subject.getObject().addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Png")));
			}
		}		
		
		return rdg;
	}
	
	private RRG createTestTypedRRG() throws Exception {
		RRG rrg = createTestRRG(3 /* object count */, false /* anonymous objects */);
		SSWAPGraph graph = rrg.getResource().getGraph();
		
		for (Iterator<SSWAPObject> it = graph.getSubject().getObjects().iterator(); it.hasNext(); ) {
			SSWAPObject object = it.next();
			int objectIndex = Integer.parseInt(object.getURI().toString().split(":")[4]);			
			
			if ((objectIndex % 3) == 0) {
				object.addType(rrg.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Plain")));
			}
			else if ((objectIndex % 3) == 1) {
				object.addType(rrg.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Html")));
			}
		}
		
		return rrg;
	}
	
	@Test
	@Ignore
	public void testRRG2RIGMultiGraphRDG() throws Exception {
		RDG rdg = createTestRDG(2, 1, 1, true);
		
		SSWAPResource resource = rdg.getResource();
		
		int graphIndex = 0;
		for (Iterator<SSWAPGraph> it = resource.getGraphs().iterator(); it.hasNext(); graphIndex++) {
			SSWAPGraph graph = it.next();
			
			SSWAPSubject subject = graph.getSubject();			
			
			if (graphIndex == 0) {
				subject.addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Plain")));
				subject.getObject().addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Jpeg")));
			}
			else {
				subject.addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Html")));
				subject.getObject().addType(rdg.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Png")));
			}
		}
		
		
		RRG rrg = createTestTypedRRG();
		
		
		RIG rig = rrg.createRIG(rdg);
	
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		rig.serialize(bos);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		
		RIG providerRIG = rdg.getRIG(bis);
		
		Collection<SSWAPSubject> translatedSubjects = providerRIG.getTranslatedSubjects();
		
		System.out.println(translatedSubjects.size());
	}
	
	@Test
	public void testRRG2RIGTyped() throws Exception {
		RDG rdg = createTestTypedRDG();		
		RRG rrg = createTestTypedRRG();		
		RIG rig = rrg.createRIG(rdg);
		
		Collection<SSWAPSubject> subjects = rig.getResource().getGraph().getSubjects();
		
		assertEquals(3, subjects.size());
		
		for (SSWAPSubject subject : subjects) {
			assertFalse(subject.isAnonymous());
			
			int subjectId = Integer.parseInt(subject.getURI().toString().split(":")[4]);
			
			if ((subjectId % 3) == 0) {
				assertTrue(subject.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Plain"))));
				assertFalse(subject.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Html"))));
				
				SSWAPObject object = subject.getObject();
				assertTrue(object.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Jpeg"))));
				assertFalse(object.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Png"))));
			}
			else if ((subjectId % 3) == 1) {
				assertFalse(subject.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Plain"))));
				assertTrue(subject.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Html"))));
				
				SSWAPObject object = subject.getObject();
				assertFalse(object.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Jpeg"))));
				assertTrue(object.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Png"))));
			}
			else {
				assertFalse(subject.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Plain"))));
				assertFalse(subject.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/text/Html"))));
				
				SSWAPObject object = subject.getObject();
				assertTrue(object.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Jpeg"))) 
 					    || object.isOfType(rig.getType(URI.create("http://sswapmeet.sswap.info/mime/image/Png"))));
			}
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		rig.serialize(bos);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		
		RIG validatedRIG = rdg.getRIG(bis);
		
		assertNotNull(validatedRIG);
		
		Collection<SSWAPSubject> translatedSubjects = validatedRIG.getTranslatedSubjects();
		
		// one subject is not translated (it does not have any matching type)
		assertEquals(2, translatedSubjects.size());
		
		for (SSWAPSubject subject : translatedSubjects) {
			assertFalse(subject.isAnonymous());
		}
	}
	
	@Test
	public void testMultiFastaRRGToRIG() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/multifasta3.rrg.owl");
		RRG rrg = SSWAP.getResourceGraph(fis, RRG.class);
		
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap.info/iplant/svc/fasttree-ranger-2.1.4"));
		
		RIG rig = rrg.createRIG(rdg);
		
		rig.serialize(System.out);
	}
	
	@Test
	public void testNullObjectRRGToRIG() throws Exception {
		final URI canonicalURI = URI.create("http://sswap.info/examples/resources/canonical/canonicalResource");
		
		RRG someRRG = createTestRRG(0 /*Number of objects*/, false /*Anonymous objects*/);
		assertNotNull(someRRG);
		assertTrue(someRRG.getResource().getGraph().getSubjects().size() > 0);
		assertEquals(someRRG.getResource().getGraph().getSubject().getObjects().size(), 0);
		
		RDG rdg = SSWAP.getRDG(canonicalURI);
		assertNotNull(rdg);
		assertTrue(rdg.getResource().getGraph().getSubjects().size() > 0);

		try {
			RIG testRig = someRRG.createRIG(rdg);
			fail("Expected DataAccessException not thrown");
		}
		catch (DataAccessException e) { 
			assertEquals(e.getMessage(), "Graph has no non-null Objects");
		}
		
		SSWAPObject nullObj = someRRG.createObject();
		SSWAPType utilNull = someRRG.getType(URI.create("http://sswapmeet.sswap.info/util/Null"));
		nullObj.addType(utilNull);
		assertNotNull(nullObj.getDeclaredTypes());
		assertTrue(nullObj.getDeclaredTypes().contains(utilNull));
		someRRG.getResource().getGraph().getSubject().addObject(nullObj);
		assertEquals(someRRG.getResource().getGraph().getSubject().getObjects().size(), 1);
		assertSame(someRRG.getResource().getGraph().getSubject().getObject(), nullObj);

		try {
			RIG testRig = someRRG.createRIG(rdg);
			fail("Expected DataAccessException not thrown");
		}
		catch (DataAccessException e) { 
			assertEquals(e.getMessage(), "Graph has no non-null Objects");
		}
		
		SSWAPObject nonNullObj = someRRG.createObject();
		SSWAPType validType = someRRG.getType(canonicalURI);
		nonNullObj.addType(validType);
		assertNotNull(nonNullObj.getDeclaredTypes());
		assertTrue(nonNullObj.getDeclaredTypes().contains(validType));
		someRRG.getResource().getGraph().getSubject().addObject(nonNullObj);
		assertEquals(someRRG.getResource().getGraph().getSubject().getObjects().size(), 2);
		assertTrue(someRRG.getResource().getGraph().getSubject().getObjects().contains(nullObj));
		assertTrue(someRRG.getResource().getGraph().getSubject().getObjects().contains(nonNullObj));
		
		try {
			RIG testRig = someRRG.createRIG(rdg);
		}
		catch (DataAccessException e) { 
			fail("DataAccessException thrown when Graph had non-null Object");
		}
	}
}
