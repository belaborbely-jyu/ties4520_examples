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
import info.sswap.api.http.HTTPProvider;
import info.sswap.api.model.Config;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.ValidationException;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.model.ModelUtils;
import info.sswap.impl.empire.model.ResourceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.clarkparsia.utils.web.Response;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Tests for RIGs
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class RIGTests {
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:RIGTest#";
	
	private static final String NS_TEST_URL = "http://sswap-c.iplantcollaborative.org/";
	/**
	 * The URI for an RDG (QtlByTraitAccession) used for tests
	 */
	private static final String QTL_BY_TRAIT_ACCESSION_URI = "http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession";

	/**
	 * The path to the local file containing the data for QtlByTraitAccession RDG (to avoid a network connection during
	 * the test)
	 */
	private static final String QTL_BY_TRAIT_ACCESSION_FILE = "test/data/qtl-by-trait-accession.owl";
	
	private static final String QTL_BY_TRAIT_ACCESSION_RIG_FILE = "test/data/qtl-by-trait-accession-rig.owl";
	
	private static final String COMPLEX_CLASS_RDG_FILE = "test/data/complex-class-RDG.owl";

	/**
	 * The RDG object for QtlByTraitAccesion
	 */
	private static RDG qtlByTraitAccessionRDG;

	/**
	 * The URI for an RDG (QtlByTraitCategory) used for tests
	 */
	private static final String QTL_BY_TRAIT_CATEGORY_URI = "http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-category";

	/**
	 * The path to the local file containing the data for QtlByTraitCategory RDG (to avoid a network connection during
	 * the test)
	 */
	private static final String QTL_BY_TRAIT_CATEGORY_FILE = "test/data/qtl-by-trait-category.owl";
	
	private static final String BURROWS_WHEELER_ALIGNER_URI = "http://localhost:8081/resources/UHTS/BWA/Burrows-WheelerAligner";
	
	private static final String BURROWS_WHEELER_ALIGNER_FILE = "test/data/Burrows-WheelerAligner.owl";

	/**
	 * The RDG object for QtlByTraitCategory
	 */
	private static RDG qtlByTraitCategoryRDG;

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
	 * Initializes the test data (RDGs) before the actual tests are run.
	 * 
	 * @throws URISyntaxException
	 *             if the constants encode a URI that is not syntactically valid
	 * @throws IOException
	 *             if I/O error occurs while reading the test data
	 */
	@BeforeClass
	public static void initRDGs() throws URISyntaxException, IOException {
		qtlByTraitAccessionRDG = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, QTL_BY_TRAIT_ACCESSION_FILE);
		qtlByTraitCategoryRDG = getDereferencedRDG(QTL_BY_TRAIT_CATEGORY_URI, QTL_BY_TRAIT_CATEGORY_FILE);
	}

	/**
	 * Tests creation of an RIG from an existing RDG. (A use case of a client creating an RIG with an intention of
	 * sending it to a provider to invoke a service.)
	 */
	@Test
	public void testCreateRIG() {
		RIG rig = qtlByTraitAccessionRDG.getRIG();

		assertNotNull(rig);

		assertEquals(qtlByTraitAccessionRDG.getURI(), rig.getURI());
	}

	/**
	 * Tests validation of a proper RIG against an RDG. (A use case of a service provider that is receiving an incoming
	 * RIG (as stream), and needs to create an RIG object and validate it against its RDG.)
	 */
	@Test
	public void testRIGValidationCorrect() {
		// first get an RIG object
		RIG rig = qtlByTraitAccessionRDG.getRIG();
		
		rig.getResource().getGraph().getSubject().addProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		rig.getResource().getGraph().getSubject().addProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		try {
			// read the serialized RIG
			RIG rig2 = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);

			// check whether the result is something valid
			assertNotNull(rig2);
			assertEquals(rig.getURI(), rig2.getURI());
		}
		catch (ValidationException e) {
			System.out.println(e);
			fail("A proper RIG failed validation against the RDG it was created from");
		}
	}
	
	/**
	 * Tests whether it is possible to validate a RIG that is not exact as the RDG but the RIG subject's class is a subclass of the RDG subject's class.
	 * (This is legal in SSWAP protocol, and should succeed.)
	 * 
	 * @throws URISyntaxException if the URIs are not syntactically valid URIs
	 * @throws IOException if an I/O error should occur
	 */
	@Test
	public void testRIGValidationSubjectSubClass() throws URISyntaxException, IOException {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-subclass.owl");
		
		rdg.serialize(System.out);
		
		// first get an RIG object
		RIG rig = rdg.getRIG();
		
		rig.getResource().getGraph().getSubject().setProperty(rig.getPredicate(URI.create(NS_TEST_URL + "/test/data/MyAccessionID")), "abc");		
		rig.getResource().getGraph().getSubject().setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		rig.getResource().getGraph().getSubject().setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		try {
			// read the serialized RIG
			RIG rig2 = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);

			// check whether the result is something valid
			assertNotNull(rig2);
			assertEquals(rig.getURI(), rig2.getURI());
		}
		catch (ValidationException e) {
			fail("A proper RIG failed validation against the RDG it was created from");
		}		
	}

	@Test
	public void testRIGValidationSubjectSubClassAdditionalSubjects() throws URISyntaxException, IOException {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-subclass.owl");		
		
		// first get an RIG object
		RIG rig = rdg.getRIG();
		
		// add a new subject
		SSWAPGraph graph = rig.getResource().getGraph();
		SSWAPSubject subject1 = graph.getSubject();
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		SSWAPSubject subject2 = rig.createSubject();
		subject2.addType(rig.getType(URI.create(NS_TEST_URL + "/test/data/QtlByTraitAccessionRequestSubClass")));
		subject2.addProperty(rig.getPredicate(URI.create(NS_TEST_URL + "/test/data/MyAccessionID")), "");
		subject2.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject2.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");

		subject2.setObject(subject1.getObject());
		
		graph.setSubjects(Arrays.asList(subject1, subject2));
		
		rig.serialize(System.out);
		
		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		try {
			// read the serialized RIG
			RIG rig2 = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);

			// check whether the result is something valid
			assertNotNull(rig2);
			assertEquals(rig.getURI(), rig2.getURI());
			
			for (SSWAPSubject subject : rig2.getResource().getGraph().getSubjects()) {
				SSWAPSubject translatedSubject = rig2.translate(subject);
				SSWAPIndividual inferredSubject = subject.getInferredIndividual();
				assertNotNull(translatedSubject);
				assertNotNull(inferredSubject);
				
				translatedSubject.serialize(System.out);
				
				SSWAPProperty translatedProperty = translatedSubject.getProperty(rig2.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
				assertNotNull(translatedProperty);
				
				assertNotNull(inferredSubject.getProperty(inferredSubject.getDocument().getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))));
			}
		}
		catch (ValidationException e) {
			e.printStackTrace();
			fail("A proper RIG with additional subjects failed validation against the RDG it was created from");
		}		
	}
	
	@Test
	public void testRIGAdditional10SubjectsUnmatched() throws URISyntaxException, IOException {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-subclass.owl");		
		
		// first get an RIG object
		RIG rig = rdg.getRIG();
		
		// add a new subject
		SSWAPGraph graph = rig.getResource().getGraph();
		SSWAPSubject subject1 = graph.getSubject();
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
		subjects.add(subject1);
		
		for (int i = 0; i < 10; i++) {
			SSWAPSubject additionalSubject = rig.createSubject();
			additionalSubject.addType(rig.getType(URI.create("http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/QtlBySymbolRequest")));
			additionalSubject.addProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/symbol")), "33");
			additionalSubject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
			additionalSubject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");

			SSWAPObject additionalObject = rig.createObject();
			additionalSubject.setObject(additionalObject);

			subjects.add(additionalSubject);
		}

		graph.setSubjects(subjects);
		
		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		try {
			// read the serialized RIG (and perform validation)
			qtlByTraitAccessionRDG.getRIG(intermediateInputStream);
		}
		catch (ValidationException e) {
			fail("A proper RIG failed validation against the RDG it was created from");
		}		
	}

	@Test
	public void testRIGValidationSubjectSubClassAdditionalObjects() throws URISyntaxException, IOException {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-subclass.owl");		
		
		// first get an RIG object
		RIG rig = rdg.getRIG();
		
		// add a new subject
		SSWAPGraph graph = rig.getResource().getGraph();
		SSWAPSubject subject1 = graph.getSubject();
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		SSWAPObject object1 = subject1.getObject();
		SSWAPObject object2 = rig.createObject();		
		
		subject1.setObjects(Arrays.asList(object1, object2));
		
		rig.serialize(System.out);
		
		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		try {
			// read the serialized RIG
			RIG rig2 = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);

			// check whether the result is something valid
			assertNotNull(rig2);
			assertEquals(rig.getURI(), rig2.getURI());
			
			for (SSWAPSubject subject : rig2.getResource().getGraph().getSubjects()) {
				SSWAPSubject translatedSubject = rig2.translate(subject);
				assertNotNull(translatedSubject);
				
				translatedSubject.serialize(System.out);
				
				SSWAPProperty translatedProperty = translatedSubject.getProperty(rig2.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
				assertNotNull(translatedProperty);
			}
		}
		catch (ValidationException e) {
			fail("A proper RIG failed validation against the RDG it was created from");
		}		
	}

	@Test
	public void testRIGAdditional10ObjectsUnmatched() throws URISyntaxException, IOException {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-subclass.owl");		
		
		// first get an RIG object
		RIG rig = rdg.getRIG();
		
		// add a new subject
		SSWAPGraph graph = rig.getResource().getGraph();
		SSWAPSubject subject1 = graph.getSubject();
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject1.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");		
		
		List<SSWAPObject> objects = new LinkedList<SSWAPObject>();
		objects.add(subject1.getObject());
		
		for (int i = 0; i < 10; i++) {
			SSWAPObject additionalObject = rig.createObject();
			
			objects.add(additionalObject);
		}				
		
		subject1.setObjects(objects);
		
		rig.serialize(System.out);
		
		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		try {
			// read the serialized RIG
			RIG rig2 = qtlByTraitAccessionRDG.getRIG(intermediateInputStream);

			// check whether the result is something valid
			assertNotNull(rig2);
			assertEquals(rig.getURI(), rig2.getURI());
			
			for (SSWAPSubject subject : rig2.getResource().getGraph().getSubjects()) {
				SSWAPSubject translatedSubject = rig2.translate(subject);
				assertNotNull(translatedSubject);
				
				translatedSubject.serialize(System.out);
				
				SSWAPProperty translatedProperty = translatedSubject.getProperty(rig2.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
				assertNotNull(translatedProperty);
			}
		}
		catch (ValidationException e) {
			fail("A proper RIG failed validation against the RDG it was created from");
		}		
	}

	
	/**
	 * Tests whether it is possible to validate a RIG whose subject's class is not a subclass of the RDG's subject. (This
	 * should fail, since it is not legal in SSWAP.)
	 * 
	 * @throws URISyntaxException if the URIs are not syntactically valid URIs
	 * @throws IOException if an I/O error should occur
	 * @throws ValidationException this is the expected result of this test
	 */
	@Test(expected=ValidationException.class)
	public void testRIGValidationSubjectWrongSubClass() throws URISyntaxException, IOException, ValidationException {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-wrong-subclass.owl");		
		
		// first get an RIG object
		RIG rig = rdg.getRIG();
		
		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

	    qtlByTraitAccessionRDG.getRIG(intermediateInputStream);
	}
	
	/**
	 * Tests validation of an RIG created for one service against an RDG for a different service. (A use case of a
	 * service provider that is receiving an incoming RIG (as a stream), which is sent to a wrong service.)
	 * @throws ValidationException 
	 */
	@Test(expected=ValidationException.class)
	public void testRIGValidationDifferentService() throws ValidationException {
		// get an RIG for a different service (QtlByTraitCategory)
		RIG rigForDifferentService = qtlByTraitCategoryRDG.getRIG();

		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rigForDifferentService.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		// read the serialized RIG and try to validate against the RDG of this (QtlByTraitAccession) service
		// this should generate ValidateException
		qtlByTraitAccessionRDG.getRIG(intermediateInputStream);
	}
	
	@Test
	public void testRIGComplexSubjectClass() throws URISyntaxException, IOException, ValidationException {
		RDG rdg = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, COMPLEX_CLASS_RDG_FILE);
		
		RIG rig = rdg.getRIG();
		
		// adding required parameters so that the RIG would pass validation
		SSWAPIndividual mapSet = rig.createIndividual();
		mapSet.addType(rig.getType(URI.create("http://sswapmeet.sswap.info/map/MapSet")));

		rig.getResource().getGraph().getSubject().addProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/map/belongsToMapSet")), mapSet);

		// serialize it
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());

		// read the serialized RIG and try to validate against the RDG of this (QtlByTraitAccession) service
		RIG rigProviderSide = rdg.getRIG(intermediateInputStream);
		
		SSWAPSubject translatedSubject = rigProviderSide.translate(rigProviderSide.getResource().getGraph().getSubject());
		
		assertNotNull(translatedSubject);
		
		translatedSubject.serialize(System.out);
		
		SSWAPResource translatedResource = rigProviderSide.translate(rigProviderSide.getResource());
		
		assertNotNull(translatedResource);
		
		translatedResource.serialize(System.out);
		
		Collection<SSWAPProperty> belongsToMapSet2 = translatedSubject.getProperties(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/map/belongsToMapSet")));		
		assertTrue(!belongsToMapSet2.isEmpty());
	}
	
	/**
	 * Tests RIG translation (from the vocabulary submitted by the client to the vocabulary expected by the provider; i.e., defined in the
	 * published RDG).
	 * 
	 * @throws URISyntaxException if one of the URIs in the test should not be syntactically valid
	 * @throws IOException if an I/O error should occur
	 * @throws ValidationException if the submitted RIG should fail validation
	 */
	@Test
	public void testRIGTranslation() throws URISyntaxException, IOException, ValidationException {
		// steps "on the client side"
		
		// create a RIG whose subject is (QtlByTraitAccessionRequestSubClass) -- a subclass of QtlByTraitAccessionRequest 
		RIG clientRIG = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, "test/data/qtl-by-trait-accession-subclass.owl").getRIG();
		
		// set a value for MyAccessionID property (which is a subproperty of accessionID)
		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create(NS_TEST_URL + "/test/data/MyAccessionID")), "TestID");

		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");

		// serialize the created RDG ("transmission of the RIG to the provider")
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		clientRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// steps "on the provider side"
		
		// read the published RDG
		RDG providerRDG = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, QTL_BY_TRAIT_ACCESSION_FILE);


		
		// read the transmitted data by the client, and perform validation (this includes validation that the subject in RIG
		// is a subclass of the subject in the RDG)
		RIG rigProviderSide = providerRDG.getRIG(intermediateInputStream);		
		
		SSWAPResource resource = rigProviderSide.getResource();
		SSWAPResource translatedResource = rigProviderSide.translate(resource);
		
		assertFalse(resource == translatedResource);
		assertNotNull(((ResourceImpl) translatedResource).getOriginalResource());
		
		assertTrue(resource == ExtensionAPI.getUntranslatedNode(translatedResource));
		assertTrue(resource == ExtensionAPI.getUntranslatedNode(resource));
		
		// retrieve a SSWAP subject that contains only the properties mandated by the RDG and translated
		// to its vocabulary
		SSWAPSubject translatedSubject = rigProviderSide.translate(rigProviderSide.getResource().getGraph().getSubject());
		assertNotNull(translatedSubject);
		
		translatedSubject.serialize(System.out);
		
		// verify that the translated subject contains accessionID (which is not explicitely set by the client; instead
		// the client set MyAccessionID, which is a subproperty of accessionID).
		SSWAPProperty translatedProperty = translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		assertNotNull(translatedProperty);
		
		// verify that the value of the translated property is proper
		SSWAPElement translatedPropertyValue = translatedProperty.getValue();
		
		// verify that the value exists
		assertNotNull(translatedPropertyValue);
		
		// verify that the value is a literal
		assertTrue(translatedPropertyValue.isLiteral());
		
		// verify that the literal is proper
		assertEquals("TestID", translatedPropertyValue.asString());
		
		SSWAPSubject untranslatedSubject = ExtensionAPI.getUntranslatedNode(translatedSubject);
		assertNotNull(untranslatedSubject);
		
		assertTrue(untranslatedSubject == rigProviderSide.getResource().getGraph().getSubject());		
		assertTrue(ExtensionAPI.getUntranslatedNode(untranslatedSubject) == untranslatedSubject);
		
		assertTrue(ExtensionAPI.getUntranslatedNode(resource.getGraph()) == resource.getGraph());
		assertTrue(ExtensionAPI.getUntranslatedNode(untranslatedSubject.getObject()) == untranslatedSubject.getObject());
	}
	
	@Test
	public void testRIGTranslationIterableSubjects() throws URISyntaxException, IOException, ValidationException {
		// steps "on the client side"
		
		// create a RIG whose subject is (QtlByTraitAccessionRequestSubClass) -- a subclass of QtlByTraitAccessionRequest 
		RIG clientRIG = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, "test/data/qtl-by-trait-accession-subclass.owl").getRIG();
		
		// set a value for MyAccessionID property (which is a subproperty of accessionID)
		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create(NS_TEST_URL + "/test/data/MyAccessionID")), "TestID");

		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");

		// serialize the created RDG ("transmission of the RIG to the provider")
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		clientRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// steps "on the provider side"
		
		// read the published RDG
		RDG providerRDG = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, QTL_BY_TRAIT_ACCESSION_FILE);
		
		// read the transmitted data by the client, and perform validation (this includes validation that the subject in RIG
		// is a subclass of the subject in the RDG)
		RIG rigProviderSide = providerRDG.getRIG(intermediateInputStream);		
		
		Collection<SSWAPSubject> subjects = rigProviderSide.getTranslatedSubjects();
		
		assertNotNull(subjects);
		assertEquals(1, subjects.size());
		
		for (SSWAPSubject translatedSubject : subjects) {
			assertNotNull(translatedSubject);
			
			// check that we are indeed operating on a translated subject, and that the inferences are the same as if we used translate() method
			SSWAPProperty translatedProperty = translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
			assertNotNull(translatedProperty);
			
			// verify that the value of the translated property is proper
			SSWAPElement translatedPropertyValue = translatedProperty.getValue();
			
			// verify that the value exists
			assertNotNull(translatedPropertyValue);
			
			// verify that the value is a literal
			assertTrue(translatedPropertyValue.isLiteral());
			
			// verify that the literal is proper
			assertEquals("TestID", translatedPropertyValue.asString());
			
			// end of translation verification
						
			// check that this subject has only one object now
			assertEquals(1, rigProviderSide.getResource().getGraph().getSubject().getObjects().size());
			
			// add new object to the subject
			SSWAPObject newObject = rigProviderSide.createObject(URI.create("http://sswap.info"));			
			translatedSubject.addObject(newObject);
						
			// check that the object was added to the underlying subject (not to the translated copy)
			assertEquals(2, rigProviderSide.getResource().getGraph().getSubject().getObjects().size());
			
			// try to invoke a set on the translated subject -- it should be proxied to the underlying subject
			SSWAPObject newObject2 = rigProviderSide.createObject(URI.create("http://sswapmeet.sswap.info"));			
			translatedSubject.setObject(newObject2);
		
			// check objects of the underlying (real) subject -- it should be now only one -- the newly set one
			assertEquals(1, rigProviderSide.getResource().getGraph().getSubject().getObjects().size());			
			assertEquals("http://sswapmeet.sswap.info", rigProviderSide.getResource().getGraph().getSubject().getObjects().iterator().next().getURI().toString());
			
			SSWAPSubject untranslatedSubject = ExtensionAPI.getUntranslatedNode(translatedSubject);
			
			assertNotNull(untranslatedSubject);			
			assertTrue(rigProviderSide.getResource().getGraph().getSubjects().contains(untranslatedSubject));
		}	
	}
	
	@Test
	public void testRIGClientSideTranslation() throws URISyntaxException, IOException, ValidationException {		
		// create a RIG whose subject is (QtlByTraitAccessionRequestSubClass) -- a subclass of QtlByTraitAccessionRequest 
		RIG clientRIG = getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, "test/data/qtl-by-trait-accession-subclass.owl").getRIG();
		
		// set a value for MyAccessionID property (which is a subproperty of accessionID)
		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create(NS_TEST_URL + "/test/data/MyAccessionID")), "TestID");
		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		clientRIG.getResource().getGraph().getSubject().setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		// retrieve a SSWAP subject that contains only the properties mandated by the RDG and translated
		// to its vocabulary
		SSWAPSubject translatedSubject = clientRIG.translate(clientRIG.getResource().getGraph().getSubject());
		assertNotNull(translatedSubject);
		translatedSubject.serialize(System.out);
		
		// verify that the translated subject contains accessionID (which is not explicitely set by the client; instead
		// the client set MyAccessionID, which is a subproperty of accessionID).
		SSWAPProperty translatedProperty = translatedSubject.getProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		assertNotNull(translatedProperty);
		
		// verify that the value of the translated property is proper
		SSWAPElement translatedPropertyValue = translatedProperty.getValue();
		
		// verify that the value exists
		assertNotNull(translatedPropertyValue);
		
		// verify that the value is a literal
		assertTrue(translatedPropertyValue.isLiteral());
		
		// verify that the literal is proper
		assertEquals("TestID", translatedPropertyValue.asString());		
	}
	
	/**
	 * Tests whether we can read from RIG the property value that the "client" sent.
	 * 
	 * @throws IOException if an I/O error should occur
	 * @throws ValidationException if a validation of the RIG should fail (it should not)
	 * @throws URISyntaxException if a URI specified in this test is not a syntactically valid URI
	 */
	@Test
	public void testReadRIGProperties() throws IOException, ValidationException, URISyntaxException {
		FileInputStream fis = new FileInputStream(QTL_BY_TRAIT_ACCESSION_RIG_FILE);
		RIG rig = qtlByTraitAccessionRDG.getRIG(fis);		
		fis.close();
		
		SSWAPResource resource = rig.getResource();
		
		SSWAPSubject subject = resource.getGraph().getSubject();
		
		SSWAPProperty property = subject.getProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		
		assertNotNull(property);
		
		SSWAPElement value = property.getValue();
		assertNotNull(value);
		assertTrue(value.isLiteral());
		
		assertEquals("abc", value.asString());
	}
	
	/**
	 * Test for a bug where dependent objects (e.g., Subjects for a SSWAPGraph) were duplicated with every call to 
	 * graph.getSubjects().
	 */
	@Test
	public void testDependentObjectsNotDuplicated() throws Exception {
		FileInputStream fis = new FileInputStream(QTL_BY_TRAIT_ACCESSION_RIG_FILE);
		RIG rig = qtlByTraitAccessionRDG.getRIG(fis);		
		fis.close();
		
		SSWAPResource resource = rig.getResource();
		
		Collection<SSWAPSubject> subjects1 = resource.getGraph().getSubjects();
		Collection<SSWAPSubject> subjects2 = resource.getGraph().getSubjects();
		
		// at this point subjects1 and subjects2 should contain exactly the same objects (not just equal but identical)
		assertTrue(subjects1.iterator().next() == subjects2.iterator().next());
	}
	
	@Test
	public void testBurrowsWheelerAligner() throws IOException, ValidationException, URISyntaxException {
		RDG rdg = getDereferencedRDG(BURROWS_WHEELER_ALIGNER_URI, BURROWS_WHEELER_ALIGNER_FILE);
		
		FileInputStream fis = new FileInputStream(BURROWS_WHEELER_ALIGNER_FILE);
		
		rdg.getRIG(fis);
		fis.close();		
	}
	
	/**
	 * Test of validation and processing of a more complex, real-life ontology. The subject
	 * in this RIG is of type Annotation which requires three required parameters (hasTaxa, hasDBxRecord, and symbol).
	 * Moreover, the value of hasTaxa has to be an object of type Taxa and the value of hasDBxRecord has to
	 * be an object of type DBxRecord, which has further restrictions on it (it needs a dbxName property and dbxID).
	 * To further complicate things dbxName is a datatype property whose range is a custom datatype (derived
	 * from xsd:string and restricted via owl:oneOf to a certain set of vocabulary).
	 *  
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws ValidationException
	 */
	@Test
	@Ignore//because some poAnnotation URIs must be fixed
	public void testPOAnnotationValidation() throws URISyntaxException, IOException, ValidationException {
		RDG rdg = getDereferencedRDG("http://sswap.gramene.org/vpin/ontologies/poAnnotation/get-by-annotation", "test/data/po-annotation-rdg.owl"); 
		
		RIG clientRIG = rdg.getRIG();
		
		SSWAPSubject subject = clientRIG.getResource().getGraph().getSubject();
				
		SSWAPIndividual taxa = clientRIG.createIndividual();
		taxa.addType(clientRIG.getType(URI.create("http://sswapmeet.sswap.info/taxa/Taxa")));
	
		subject.addProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/taxa/hasTaxa")), taxa);		
		subject.addProperty(clientRIG.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/annotationSymbol")), "testSymbol");
				
		SSWAPIndividual dbxRecord = clientRIG.createIndividual();
		dbxRecord.addType(clientRIG.getType(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/DBxRecord")));
		dbxRecord.addProperty(clientRIG.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/dbxID")), "testDBxID");		
		dbxRecord.addProperty(clientRIG.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/dbxName")), "All", URI.create(XSD.xstring.getURI()));
			
		subject.addProperty(clientRIG.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/hasDBxRecord")), dbxRecord);
				
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		clientRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = rdg.getRIG(intermediateInputStream);	
		
		SSWAPSubject translatedSubject = rigProviderSide.translate(rigProviderSide.getResource().getGraph().getSubject());
		
		assertNotNull(translatedSubject);
		
		rigProviderSide.getResource().getGraph().getSubject().serialize(System.out);
		
		rigProviderSide.getResource().getGraph().getSubject().getInferredIndividual().serialize(System.out);
		
		translatedSubject.serialize(System.out);
		
		
		
		SSWAPProperty hasDBxRecord2 = translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/hasDBxRecord")));
		assertNotNull(hasDBxRecord2);
		
		SSWAPIndividual dbxRecord2 = hasDBxRecord2.getValue().asIndividual();
		assertNotNull(dbxRecord2);
		
		SSWAPProperty dbxName2 = dbxRecord2.getProperty(rigProviderSide.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/dbxName")));
		assertNotNull(dbxName2);
		assertEquals("All", dbxName2.getValue().asString());
		
		SSWAPProperty dbxID2 = dbxRecord2.getProperty(rigProviderSide.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/dbxID")));
		assertEquals("testDBxID", dbxID2.getValue().asString());
		
		SSWAPProperty symbol2 = translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://sswap.gramene.org/vpin/ontologies/poAnnotation/annotationSymbol")));
		assertEquals("testSymbol", symbol2.getValue().asString());
		
		SSWAPProperty hasTaxa2 = translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/taxa/hasTaxa")));
		SSWAPIndividual taxa2 = hasTaxa2.getValue().asIndividual();
		assertNotNull(taxa2);
	}
	
	@Test
	public void testDefaultParameters() throws Exception {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-with-default-parameters.owl"); 

		assertEquals("http://sswap.gramene.org/vpin/invoke-qtl-by-trait-accession.jsp", rdg.getResource().getInputURI().toString());
		assertEquals("ABC123", rdg.getResource().getGraph().getSubject().getProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());
		
		RIG clientRIG = rdg.getRIG();
		
		SSWAPResource clientResource = clientRIG.getResource();
		clientResource.setInputURI(null);
		
		SSWAPSubject subject = clientResource.getGraph().getSubject();
		subject.clearProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
				
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		clientRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = rdg.getRIG(intermediateInputStream);	
		
		assertEquals("http://sswap.gramene.org/vpin/invoke-qtl-by-trait-accession.jsp", rigProviderSide.getResource().getInputURI().toString());
		assertEquals("ABC123", rigProviderSide.getResource().getGraph().getSubject().getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());			
	}
	
	@Test
	public void testOverrideDefaultParameters() throws Exception {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-with-default-parameters.owl"); 

		assertEquals("http://sswap.gramene.org/vpin/invoke-qtl-by-trait-accession.jsp", rdg.getResource().getInputURI().toString());
		assertEquals("ABC123", rdg.getResource().getGraph().getSubject().getProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());
		
		RIG clientRIG = rdg.getRIG();
		
		SSWAPResource clientResource = clientRIG.getResource();
		clientResource.setInputURI(URI.create(NS + "some/input/URI"));
		clientResource.setOutputURI(null);
		clientResource.setAboutURI(null);
		clientResource.setMetadata(null);
		
		SSWAPSubject subject = clientResource.getGraph().getSubject();
		subject.clearProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		
		subject.addProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")), "XYZ456");
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		clientRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = rdg.getRIG(intermediateInputStream);	
		
		assertEquals(NS + "some/input/URI", rigProviderSide.getResource().getInputURI().toString());
		assertEquals("http://www.gramene.org/qtl", rigProviderSide.getResource().getAboutURI().toString());
		assertEquals("http://sswap.gramene.org/vpin/qtl-by-trait-accession-metadata.txt", rigProviderSide.getResource().getMetadata().toString());
		assertEquals("http://sswap.gramene.org/vpin/outputURI.txt", rigProviderSide.getResource().getOutputURI().toString());
		assertEquals("XYZ456", rigProviderSide.getResource().getGraph().getSubject().getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());			
	}
	
	@Test
	public void testSetDefaultParameters() throws Exception {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-with-default-parameters.owl"); 

		assertEquals("http://sswap.gramene.org/vpin/invoke-qtl-by-trait-accession.jsp", rdg.getResource().getInputURI().toString());
		assertEquals("ABC123", rdg.getResource().getGraph().getSubject().getProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());
		
		RIG clientRIG = rdg.getRIG();
		
		SSWAPResource clientResource = clientRIG.getResource();
		clientResource.setInputURI(null);
		clientResource.setOutputURI(null);
		clientResource.setAboutURI(null);
		clientResource.setMetadata(null);
		
		SSWAPSubject subject = clientResource.getGraph().getSubject();
		subject.clearProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		clientRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = rdg.getRIG(intermediateInputStream);	
		
		assertEquals("http://www.gramene.org/qtl", rigProviderSide.getResource().getAboutURI().toString());
		assertEquals("http://sswap.gramene.org/vpin/qtl-by-trait-accession-metadata.txt", rigProviderSide.getResource().getMetadata().toString());
		assertEquals("http://sswap.gramene.org/vpin/invoke-qtl-by-trait-accession.jsp", rigProviderSide.getResource().getInputURI().toString());		
		assertEquals("http://sswap.gramene.org/vpin/outputURI.txt", rigProviderSide.getResource().getOutputURI().toString());
		assertEquals("ABC123", rigProviderSide.getResource().getGraph().getSubject().getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());			
	}

	@Test
	public void testOverrideDefaultParametersSubProperties() throws Exception {
		RDG rdg = getDereferencedRDG("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", "test/data/qtl-by-trait-accession-with-default-parameters.owl"); 

		// check whether the crucial pieces of information are in the original RDG
		assertEquals("http://sswap.gramene.org/vpin/invoke-qtl-by-trait-accession.jsp", rdg.getResource().getInputURI().toString());
		assertEquals("ABC123", rdg.getResource().getGraph().getSubject().getProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());
		
		
		// create a RIG that overrides some of the default values from RDG
		RIG clientRIG = rdg.getRIG();
		
		// first, override the sswap:inputURI
		SSWAPResource clientResource = clientRIG.getResource();
		clientResource.setInputURI(URI.create(NS + "some/input/URI"));
		
		// now, replace the existing accessionID with a custom sub-property (myAccessionID) with a different value than the default		
		SSWAPSubject subject = clientResource.getGraph().getSubject();
		subject.clearProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID")));
		subject.addProperty(clientRIG.getPredicate(URI.create(NS_TEST_URL + "/test/data/MyAccessionID")), "XYZ456");
		
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), "abc");
		subject.setProperty(clientRIG.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")), "abc");
		
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		clientRIG.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		RIG rigProviderSide = rdg.getRIG(intermediateInputStream);	
		
		// check that the inputURI has been overridden (BTW, if the test got to this place, it means that there is only one value for inputURI,
		// since it is defined as an inverse functional property, and Pellet would have reported the ontology being inconsistent, if multiple values existed).
		assertEquals(NS + "some/input/URI", rigProviderSide.getResource().getInputURI().toString());
		
		// check that the overridden value for myAccessionID is there
		assertEquals("XYZ456", rigProviderSide.getResource().getGraph().getSubject().getProperty(rigProviderSide.getPredicate(URI.create(NS_TEST_URL + "/test/data/MyAccessionID"))).getValue().asString());
		
		// check that the original accessionID has *not* been copied (since MyAccessionID is its subproperty, it takes its place)
		assertNull(rigProviderSide.getResource().getGraph().getSubject().getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))));
		
		// now, check whether we can translate subject to the original terminology
		SSWAPSubject translatedSubject = rigProviderSide.translate(rigProviderSide.getResource().getGraph().getSubject());				
		assertNotNull(translatedSubject);
		
		// check whether we can get back the untranslated subject
		SSWAPSubject originalSubject = ExtensionAPI.getUntranslatedNode(translatedSubject);
		
		assertNotNull(originalSubject);
		assertTrue(originalSubject == rigProviderSide.getResource().getGraph().getSubject());
		
		// check that we get the overriden value for accessionID also via the translated subject
		assertEquals("XYZ456", translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/accessionID"))).getValue().asString());
	}
	
	// Tests for invocation of actual services using RIGs
	
	/**
	 * Invokes QTLByAccession using the current version of the invocation protocol; that is, the RIG is in the body of a POST
	 * request included verbatim and not encoded like a form submitted from a browser (MIME type application/x-www-form-urlencoded).
	 * 
	 * This method is currently not run in the test suite (@Ignore tag) because the currently deployed version of that service uses
	 * the legacy protocol that expects application/x-www-form-urlencoded. 
	 */
	@Test
	@Ignore
	public void testInvokeQtlByTraitAccession() throws DataAccessException, IOException, ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-accession"));
		
		RIG rig = rdg.getRIG();
		
		SSWAPSubject subject = rig.getResource().getGraph().getSubject();
		
		assertNotNull(subject);
	
		subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/accessionID")), "AQT007");
			
		HTTPProvider.RRGResponse rrgResponse = rig.invoke();
		RRG rrg = rrgResponse.getRRG();
		
		assertNotNull(rrg);
		
		SSWAPObject object = rrg.getResource().getGraph().getSubject().getObject();
		
		SSWAPProperty property = object.getProperty(rrg.getPredicate(URI.create("http://sswapmeet.sswap.info/NCBITaxonomyRecord/scientificName")));		
		assertNotNull(property);
		
		SSWAPElement value = property.getValue();
		
		assertNotNull(value);		
		assertTrue(value.isLiteral());
		
		assertEquals("Oryza sativa", value.asString());
	}
	
	/**
	 * Invokes QTLByAccession using the legacy version of the invocation protocol; that is, the body of the request is
	 * encoded like a form submitted from a browser (MIME type application/x-www-form-urlencoded), and RIG is available as the "graph"
	 * form entry. 
	 * 
	 * When/if QTLByAccession migrates to the new version of the protocol, this test should be removed.
	 *  
	 */
	@Test
	@Ignore /* The legacy service seems to be having technical difficulties */
	public void testInvokeLegacyService() throws IOException, DataAccessException, ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-accession"));
		
		RIG rig = rdg.getRIG();
		
		SSWAPSubject subject = rig.getResource().getGraph().getSubject();
		
		assertNotNull(subject);
		
		subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/accessionID")), "AQT007");
			
		// manual building of the invocation to mimic the legacy protocol
		
		// serialize RIG
		ByteArrayOutputStream bos = new ByteArrayOutputStream();		
		rig.serialize(bos, RDFRepresentation.RDF_XML, false /* commentedOutput */);
		
		// append the word "graph=" at the beginning and URL encode the RIG
		byte[] rigBytesLegacy = ("graph=" + URLEncoder.encode(new String(bos.toByteArray()), "UTF-8")).getBytes();
		
		// send the POST request and obtain RRG
		RRG rrg = null;
		Response response = null;
		try {
			response = ModelUtils.invoke(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-accession"),rigBytesLegacy);
			rrg = rig.getRRG(response.getContent());
		} finally {
				if ( response != null ) {
					response.close();
				}
		}
		
		assertNotNull(rrg);
		
		SSWAPObject object = rrg.getResource().getGraph().getSubject().getObject();
		
		SSWAPProperty property = object.getProperty(rrg.getPredicate(URI.create("http://sswapmeet.sswap.info/NCBITaxonomyRecord/scientificName")));		
		assertNotNull(property);
		
		SSWAPElement value = property.getValue();
		
		assertNotNull(value);		
		assertTrue(value.isLiteral());
		
		assertEquals("Oryza sativa", value.asString());
	}
	
	@Test
	public void testInvalidOwlOneOf() throws IOException, DataAccessException, ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-accession"));
		
		RIG rig = rdg.getRIG();
		
		SSWAPSubject subject = rig.getResource().getGraph().getSubject();
		
		assertNotNull(subject);
		
		subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/qtl/accessionID")), "AQT007");
		
		// set an invalid literal value (the only legal values for this property are plant_growth_and_development_stage and plant_structure)
		subject.setProperty(rig.getPredicate(URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/ontology")), "xyzzy");
				
		rig.serialize(System.out);
		
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side
		try {
			rdg.getRIG(intermediateInputStream);
			fail();
		}
		catch (Throwable e) {
			// correct behavior
			e.printStackTrace();
			
			System.out.println("msg " + e.getMessage());
			
			// verify that the consistency message is in RDF/XML
			assertTrue(e.getMessage().contains("</rdf:RDF>"));
		}
		
		try {
			intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
			
			// try to get explanation in TURTLE
			ExtensionAPI.setExplanationSyntax("TURTLE");
			rdg.getRIG(intermediateInputStream);
			fail();
		}
		catch (Throwable e) {
			// correct behavior
			e.printStackTrace();
			
			// verify that the consistency message is in Turtle
			assertTrue(e.getMessage().contains("@prefix"));
		}
		finally {
			// in any case, try to preserve the default
			ExtensionAPI.setExplanationSyntax("RDF/XML");
		}
		
		try {
			intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
			
			// try to get explanation in Pellet native syntax
			ExtensionAPI.setExplanationSyntax("PELLET");
			rdg.getRIG(intermediateInputStream);
			fail();
		}
		catch (Throwable e) {
			// correct behavior
			e.printStackTrace();
			
			// verify that the consistency message is in Pellet native format
			assertTrue(e.getMessage().contains("ExplanationSet: ["));
		}
		finally {
			// in any case, try to preserve the default
			ExtensionAPI.setExplanationSyntax("RDF/XML");
		}
		
		try {
			intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
			
			// try to revert back to default RDF/XML
			ExtensionAPI.setExplanationSyntax("RDF/XML");
			rdg.getRIG(intermediateInputStream);
			fail();
		}
		catch (Throwable e) {
			// correct behavior
			e.printStackTrace();
			
			// verify that the consistency message is in RDF/XML
			assertTrue(e.getMessage().contains("</rdf:RDF>"));
		}
	}
	
	@Test
	public void testPoAnnotationTermSearchRequest() throws IOException, DataAccessException, ValidationException {
		SSWAPDocument ontologyDefinition = SSWAP.createSSWAPDocument(URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/ontology"));
		ontologyDefinition.getReasoningService();
		
		RDG rdg = SSWAP.getRDG(URI.create("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
						
		RIG rig = rdg.getRIG();
		
		SSWAPSubject subject = rig.getResource().getGraph().getSubject();
		
		assertNotNull(subject);
		
		subject.setProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/OBO/id")), "ID");
		subject.setProperty(rig.getPredicate(URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/ontology")), "plant_anatomy");		
		subject.clearProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/OBO/name")));
		
		// serialization to the provider
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rig.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		// processing on the provider side	
		RIG rigProviderSide = rdg.getRIG(intermediateInputStream);
			
		SSWAPSubject providerSubject = rigProviderSide.getResource().getGraph().getSubject();
		
		SSWAPSubject translatedSubject = rigProviderSide.translate(providerSubject);
		
		SSWAPProperty providerId = translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://sswapmeet.sswap.info/OBO/id")));
		
		assertNotNull(providerId);
			
		SSWAPProperty providerOntology = translatedSubject.getProperty(rigProviderSide.getPredicate(URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/ontology")));
		assertNotNull(providerOntology);
		assertNotNull(providerOntology.getValue());
		assertEquals("plant_anatomy", providerOntology.getValue().asString());
	}
	
	/**
	 * Test to see if an RIG w/ either obo:name and/or obo:id exposes
	 * these properties in the translated subject with an RDG owl:unionOf
	 * restriction on said properties.
	 * <p>
	 * Code also implicitly tests client side translation by virtue of calling:
	 * <br>
	 *   RIG rig = rdg.getRIG();
	 * <br>
	 * followed by:
	 * <br>
	 *   SSWAPSubject translatedSubject = rig.translate(subject);
	 * <p>
	 * RDG should include a SSWAPSubject with a restriction on the union of
	 * either obo:id or obo:name.
	 * 
	 * @throws URISyntaxException
	 *             from translatedSubject.getProperty(propertyStr)
	 * 
	 */
	@Test
	public void testPoAnnotationTermSearchRequestOBOUnion() throws URISyntaxException {
		
		RDG rdg = SSWAP.getRDG(URI.create("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
		RIG rig = rdg.getRIG();
				
		Map<String,String> map = new HashMap<String,String>();
		map.put("http://sswapmeet.sswap.info/OBO/id", "ID");						// part of RDG
		map.put("http://sswapmeet.sswap.info/OBO/name", "name");					// part of RDG
		map.put("http://sswapmeet.sswap.info/genome/chromosomeNumber", "2");		// valid property, but not part of RDG
		map.put("http://sswapmeet.sswap.info/DoesNotExist", "DoesNotExistValue");	// not part of anything, but added to RIG
		map.put("http://sswapmeet.sswap.info/NotAdded", "NotAddedValue");			// not part of anything; not added to RIG


		for ( Entry<String, String> entry : map.entrySet() ) {
		
			String propertyStr = entry.getKey();
			String propertyValue = entry.getValue();
			
			SSWAPPredicate predicate = rig.getPredicate(URI.create(propertyStr));
			SSWAPSubject subject = rig.getResource().getGraph().getSubject();
			
			if ( ! propertyStr.equals("http://sswapmeet.sswap.info/NotAdded") ) {
				subject.setProperty(predicate, propertyValue);
			}

			SSWAPSubject translatedSubject = rig.translate(subject);
			SSWAPProperty translatedProperty = translatedSubject.getProperty(rig.getPredicate(URI.create(propertyStr)));
			
			if ( propertyStr.equals("http://sswapmeet.sswap.info/OBO/id") || propertyStr.equals("http://sswapmeet.sswap.info/OBO/name") ) {
				assertNotNull(translatedProperty);
				assertEquals(translatedProperty.getValue().asString(),propertyValue);
			} else {
				assertNull(translatedProperty);
			}
			
		}
		
	}
	
	/**
	 * Test RDG/RIG that have extensive use of fragment ('#') ontology terms
	 * 
	 * @throws DataAccessException from getRIG, getRRG
	 * @throws ValidationException from getRIG, getRRG
	 * @throws IOException from getDereferencedRDG, close
	 * @throws URISyntaxException from getDereferencedRDG
	 * @throws FileNotFoundException from FileInputStream
	 */
	@Test
	@Ignore
	public void testEINS() throws DataAccessException, ValidationException, IOException, URISyntaxException, FileNotFoundException {		
		// create an RDG
		RDG rdg = getDereferencedRDG("http://localhost:8080/AppSSWAP/sswap/EINS","test/data/EINS/EINS-rdg.owl");
		assertNotNull(rdg);
		
		// create the default RIG
		RIG rig = rdg.getRIG();
		assertNotNull(rig);

		// create a custom RIG
		FileInputStream fileInputStream = new FileInputStream("test/data/EINS/EINS-rig.owl");
		rig = rdg.getRIG(fileInputStream);
		assertNotNull(rig);
		fileInputStream.close();
		
		// create a RRG
		rig.getRRG();
	}
	
	@Test
	public void testMultiGraphRDGSingleGraphRIG() throws Exception {
		RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/multi-graph-rdg.owl");
		
		FileInputStream fis = new FileInputStream("test/data/single-graph-rig.owl");
		RIG rig = rdg.getRIG(fis);
		fis.close();
		
		assertNotNull(rig);
		
		Collection<SSWAPSubject> subjects = rig.getTranslatedSubjects();
		
		assertNotNull(subjects);
		assertEquals(1, subjects.size());
	}
	
	@Test
	public void testMultiGraphRDGMultiGraphRIG() throws Exception {
		RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/multi-graph-rdg.owl");
		
		FileInputStream fis = new FileInputStream("test/data/multi-graph-rig.owl");
		RIG rig = rdg.getRIG(fis);
		fis.close();
		
		assertNotNull(rig);
		
		Collection<SSWAPSubject> subjects = rig.getTranslatedSubjects();
		
		assertNotNull(subjects);
		assertEquals(5, subjects.size());
	}
	
	@Test
	public void testMultiGraphRDGMultiGraphRIGSomeNonMatch() throws Exception {
		RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/multi-graph-rdg.owl");
		
		FileInputStream fis = new FileInputStream("test/data/multi-graph-rig-some-non-match.owl");
		RIG rig = rdg.getRIG(fis);
		fis.close();
		
		assertNotNull(rig);
		
		Collection<SSWAPSubject> subjects = rig.getTranslatedSubjects();
		
		assertNotNull(subjects);
		assertEquals(3, subjects.size());
	}
	
	@Test(expected=ValidationException.class)
	public void testMultiGraphRDGMultiGraphRIGNoneMatch() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/multi-graph-rig-none-match.owl");
		
		try {
			RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/multi-graph-rdg.owl");

			rdg.getRIG(fis);
		}
		finally {
			fis.close();
		}
	}
	
	@Test
	public void testSingleGraphRDGMultiGraphRIG() throws Exception {
		RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/single-graph-rdg.owl");
		
		FileInputStream fis = new FileInputStream("test/data/multi-graph-rig.owl");
		RIG rig = rdg.getRIG(fis);
		fis.close();
		
		assertNotNull(rig);
		
		Collection<SSWAPSubject> subjects = rig.getTranslatedSubjects();
		
		assertNotNull(subjects);
		assertEquals(1, subjects.size());
	}
	
	@Test
	public void testSingleGraphRDGMultiGraphRIGMultiMatch() throws Exception {
		RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/single-graph-rdg.owl");
		
		FileInputStream fis = new FileInputStream("test/data/multi-graph-rig-multi-match.owl");
		RIG rig = rdg.getRIG(fis);
		fis.close();
		
		assertNotNull(rig);
		
		Collection<SSWAPSubject> subjects = rig.getTranslatedSubjects();
		
		assertNotNull(subjects);
		assertEquals(3, subjects.size());
	}
	
	@Test
	public void testSingleGraphRDGMultiGraphRIGSomeNonMatch() throws Exception {
		RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/single-graph-rdg.owl");
		
		FileInputStream fis = new FileInputStream("test/data/multi-graph-rig-some-non-match2.owl");
		RIG rig = rdg.getRIG(fis);
		fis.close();
		
		assertNotNull(rig);
		
		Collection<SSWAPSubject> subjects = rig.getTranslatedSubjects();
		
		assertNotNull(subjects);
		assertEquals(1, subjects.size());
		
		SSWAPSubject subject = subjects.iterator().next();
		
		assertNotNull(subject);
		
		assertEquals(URI.create("http://sswap.info/somejpeghere.jpg"), subject.getURI());
	}
	
	@Test(expected=ValidationException.class)
	public void testSingleGraphRDGMultiGraphRIGNoneMatch() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/multi-graph-rig-none-match.owl");
		
		try {
			RDG rdg = getDereferencedRDG("http://localhost:8080/Sswap/Fig2Doc","test/data/single-graph-rdg.owl");

			rdg.getRIG(fis);
		}
		finally {
			fis.close();
		}
	}
	
	/**
	 * Testing closed world when the RDG has owl:cardinality restriction
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClosedWorld() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig.owl");
		
		rdg.getRIG(fis);
		
		fis.close();
	}

	/**
	 * Testing closed world when the RDG has owl:maxCardinality restriction
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClosedWorld2() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg2.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig.owl");
		
		rdg.getRIG(fis);
		
		fis.close();
	}

	/**
	 * Testing closed world when the RDG has owl:minCardinality restriction
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClosedWorld3() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg3.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig.owl");
		
		rdg.getRIG(fis);
		
		fis.close();
	}

	/**
	 * Testing closed world when the RDG has owl:allValuesFrom restriction
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClosedWorld4() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg4.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig.owl");
		
		rdg.getRIG(fis);
		
		fis.close();
	}
	

	/**
	 * Testing closed world when the RDG has owl:someValuesFrom restriction
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClosedWorld5() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg5.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig.owl");
		
		rdg.getRIG(fis);
		
		fis.close();
	}
	
	/**
	 * Testing closed world when the RDG has owl:hasValue restriction
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClosedWorld6() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg6.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig.owl");
		
		rdg.getRIG(fis);
		
		fis.close();
	}
	
	@Test
	public void testClosedWorld7() throws Exception {
		// set the configuration properly since disabling UNA is not enabled by default
		String previousValue = Config.get().getProperty(Config.DISABLE_UNA_WHEN_CLOSING_WORLD_KEY);
		Config.get().setProperty(Config.DISABLE_UNA_WHEN_CLOSING_WORLD_KEY, "true");
		
		try {
			FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg7.owl");

			RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
			fis.close();

			fis = new FileInputStream("test/data/taxa-lookup-test-rig2.owl");

			rdg.getRIG(fis);

			fis.close();
		}
		finally {
			// make sure that we put the previous value back
			Config.get().setProperty(Config.DISABLE_UNA_WHEN_CLOSING_WORLD_KEY, previousValue);
		}
	}
	
	@Test
	public void testAppendixMatching() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig3.owl");
		
		RIG rig = rdg.getRIG(fis);
		
		fis.close();
		
		assertEquals(1, rig.getTranslatedSubjects().size());
	}
	
	@Test
	public void testAppendixMatching2() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig5.owl");
		
		RIG rig = rdg.getRIG(fis);
		
		fis.close();
		
		assertEquals(10, rig.getTranslatedSubjects().size());
	}
	
	@Test
	public void testAppendixMatching3() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig6.owl");
		
		RIG rig = rdg.getRIG(fis);
		
		fis.close();
		
		assertEquals(10, rig.getTranslatedSubjects().size());
		
		for (SSWAPSubject subject : rig.getTranslatedSubjects()) {
			SSWAPProperty property = subject.getProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/taxa/hasTaxa")));
			
			assertNotNull(property);
			
			assertNotNull(property.getValue());
			assertTrue(property.getValue().isIndividual());
			
			assertTrue(property.getValue().asIndividual().getURI().toString().startsWith("http://sswap.info/"));
		}
	}
	
	@Test
	public void testAppendixMatching4() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig6.owl");
		
		RIG rig = rdg.getRIG(fis);
		
		fis.close();
		
		assertEquals(10, rig.getTranslatedSubjects().size());
		
		for (SSWAPSubject subject : rig.getTranslatedSubjects()) {
			SSWAPProperty property = subject.getProperty(rig.getPredicate(URI.create("http://sswapmeet.sswap.info/taxa/hasTaxa")));
			
			assertNotNull(property);
			
			assertNotNull(property.getValue());
			assertTrue(property.getValue().isIndividual());
			
			assertTrue(property.getValue().asIndividual().getURI().toString().startsWith("http://sswap.info/"));
		}
	}

	
	@Test(expected=ValidationException.class)
	public void testAppendixMatchingSadPath() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/taxa-lookup-test-rdg.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://test.sswap.info/services/taxa-lookup-test"));
		fis.close();
				
		fis = new FileInputStream("test/data/taxa-lookup-test-rig4.owl");
		
		rdg.getRIG(fis);		
	}
}
