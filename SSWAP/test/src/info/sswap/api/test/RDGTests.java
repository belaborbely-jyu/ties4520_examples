/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.PDG;
import info.sswap.api.model.RDG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.model.ImplFactory;
import info.sswap.impl.empire.model.ObjectImpl;
import info.sswap.impl.empire.model.ResourceImpl;
import info.sswap.impl.empire.model.SourceModel;
import info.sswap.impl.empire.model.SubjectImpl;

import org.junit.Test;

/**
 * Tests for handling of RDGs
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class RDGTests {
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:RDGTest#";
	
	/**
	 * Tests creation of an RDG
	 * @throws URISyntaxException
	 */
	@Test
	public void testCreateRDG() {
		RDG rdg = null;
		rdg = SSWAP.createRDG(URI.create(NS + "MyTestSSWAPService"), "Test SSWAP Service",
		                "This is a description", URI.create(NS + "TestSSWAPProvider"));
		rdg.setNsPrefix("example", URI.create(NS));

		assertEquals(URI.create(NS + "MyTestSSWAPService"), rdg.getURI());

		SSWAPResource resource = rdg.getResource();

		assertNotNull(resource);
		assertFalse(resource.isAnonymous());
		assertEquals(rdg, resource.getRDG());

		assertEquals(URI.create(NS + "MyTestSSWAPService"), resource.getURI());
		assertEquals("Test SSWAP Service", resource.getName());
		assertEquals("This is a description", resource.getOneLineDescription());
		assertFalse(resource.isAnonymous());

		resource.setAboutURI(URI.create(NS + "MyAboutURI"));
		assertEquals(URI.create(NS + "MyAboutURI"), resource.getAboutURI());

		resource.setInputURI(URI.create(NS + "MyInputURI"));
		assertEquals(URI.create(NS + "MyInputURI"), resource.getInputURI());

		resource.setOutputURI(URI.create(NS + "MyOutputURI"));
		assertEquals(URI.create(NS + "MyOutputURI"), resource.getOutputURI());

		resource.setMetadata(URI.create(NS + "MyMetadata"));
		assertEquals(URI.create(NS + "MyMetadata"), resource.getMetadata());

		SSWAPGraph graph = rdg.createGraph();
		assertTrue(graph.isAnonymous());
		resource.setGraph(graph);
		
		assertEquals(resource, graph.getResource());

		assertNotNull(resource.getGraph());

		SSWAPSubject subject = rdg.createSubject();
		graph.setSubject(subject);

		assertNotNull(graph.getSubject());
		
		assertEquals(graph, subject.getGraph());

		SSWAPObject object = rdg.createObject();
		assertTrue(object.isAnonymous());
		subject.setObject(object);

		assertNotNull(subject.getObject());
		assertEquals(subject, object.getSubject());

		rdg.addImport(URI.create(NS + "testImport"));
		
		try {
			rdg.addImport(URI.create("relativeURI"));
			fail("Allowed to add an import with a relative URI");
		}
		catch (IllegalArgumentException e) {
			// correct behavior
		}
		
		try {
			rdg.removeImport(URI.create("relativeURI"));
			fail("Allowed to remove an import with a relative URI");
		}
		catch (IllegalArgumentException e) {
			// correct behavior
		}

		Collection<String> imports = rdg.getImports();

		assertNotNull(imports);
		assertEquals(1, imports.size());
		assertTrue(imports.contains(NS + "testImport"));
		
		rdg.removeImport(URI.create(NS + "testImport"));
		imports = rdg.getImports();
		assertNotNull(imports);
		assertEquals(0, imports.size());

		rdg.serialize(System.out);
	}

	/**
	 * Tests creation of an RDG with multiple SSWAPGraphs
	 */
	@Test
	public void testCreateMultiGraphSubjectObjectRDG() {
		RDG rdg = null;
		rdg = SSWAP.createRDG(URI.create(NS + "MyTestSSWAPService"), "Test SSWAP Service",
		                "This is a description", URI.create(NS + "TestSSWAPProvider"));

		assertEquals(URI.create(NS + "MyTestSSWAPService"), rdg.getURI());

		SSWAPResource resource = rdg.getResource();

		assertNotNull(resource);

		assertEquals(URI.create(NS + "MyTestSSWAPService"), resource.getURI());
		assertEquals("Test SSWAP Service", resource.getName());
		assertEquals("This is a description", resource.getOneLineDescription());

		resource.setAboutURI(URI.create(NS + "MyAboutURI"));
		assertEquals(URI.create(NS + "MyAboutURI"), resource.getAboutURI());

		resource.setInputURI(URI.create(NS + "MyInputURI"));
		assertEquals(URI.create(NS + "MyInputURI"), resource.getInputURI());

		resource.setOutputURI(URI.create(NS + "MyOutputURI"));
		assertEquals(URI.create(NS + "MyOutputURI"), resource.getOutputURI());

		resource.setMetadata(URI.create(NS + "MyMetadata"));
		assertEquals(URI.create(NS + "MyMetadata"), resource.getMetadata());

		List<SSWAPGraph> graphs = new LinkedList<SSWAPGraph>();

		for (int i = 0; i < 2; i++) {
			SSWAPGraph graph = rdg.createGraph();
			List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();

			for (int j = 0; j < 3; j++) {
				SSWAPSubject subject = rdg.createSubject();
				List<SSWAPObject> objects = new LinkedList<SSWAPObject>();

				for (int k = 0; k < 4; k++) {
					SSWAPObject object = rdg.createObject();
					objects.add(object);
				}

				subject.setObjects(objects);
				subjects.add(subject);
			}

			graph.setSubjects(subjects);
			graphs.add(graph);
		}

		resource.setGraphs(graphs);

		assertTrue(rdg.isMultiGraphs());

		Map<SSWAPGraph, Collection<SSWAPSubject>> graphSubjectMap = rdg.getMappings();

		assertEquals(2, graphSubjectMap.keySet().size());

		for (SSWAPGraph graph : graphSubjectMap.keySet()) {
			assertEquals(resource, graph.getResource());
			assertEquals(3, graphSubjectMap.get(graph).size());

			for (SSWAPSubject subject : graphSubjectMap.get(graph)) {
				assertEquals(4, subject.getObjects().size());
			}
		}
	}

	/**
	 * Verifies that Java API can read an example RDG (http://sswap.gramene.org/vpin/qtl-by-trait-accession) properly.
	 * 
	 * @throws URISyntaxException
	 * @throws ValidationException 
	 */
	@Test
	public void readExampleRDG() throws URISyntaxException, ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));

		assertNotNull(rdg);
		assertEquals("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", rdg.getURI().toString());
		assertTrue(rdg.isDereferenced());

		rdg.validate();

		testExampleDereferencedRDG(rdg);
	}

	@Test(expected=ValidationException.class)
	public void testRDGNotVouchedByProvider() throws ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		
		SSWAPProvider provider = SSWAP.createProvider(URI.create("http://sswap.info/examples/resourceProvider"));
		rdg.getResource().setProvider(provider);
		
		// should NOT throw ValidationException
		try {
			rdg.validate();
		}
		catch (ValidationException e) {
			fail("Validation exception thrown from rdg.validate(); in this test case only resource.validateProvider() should throw ValidationException");
		}
		
		// should throw ValidationException
		rdg.getResource().validateProvider();
	}
		
	/**
	 * Test whether we can read an RDG from file (i.e., dereference from file) rather than from the network.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws ValidationException 
	 */
	@Test
	public void readExampleRDGFromFile() throws URISyntaxException, IOException, ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		
		FileInputStream fis = new FileInputStream("test/data/qtl-by-trait-accession.owl");
		
		rdg.dereference(fis);
		
		fis.close();
		
		rdg.validate();
		
		testExampleDereferencedRDG(rdg);
	}
	
	/**
	 * Tests whether the RDG read contains correct information (i.e., the information that is
	 * available in this document: http://sswap.gramene.org/vpin/qtl-by-trait-accession
	 * @param rdg the RDG to be tested
	 * @throws URISyntaxException
	 */
	private void testExampleDereferencedRDG(RDG rdg) throws URISyntaxException {
		assertTrue(rdg.isDereferenced());

		SSWAPResource resource = rdg.getResource();
		
		assertNotNull(resource);
		assertEquals(rdg, resource.getRDG());
		
		assertEquals(rdg.getURI(), resource.getURI());
		assertEquals("Gramene QTLs for Trait Ontology Accession ID Retrieval", resource.getName());

		assertEquals(URI.create("http://www.gramene.org/qtl"), resource.getAboutURI());
		assertEquals(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession-metadata.txt"), resource
		                .getMetadata());
		assertEquals(URI.create("http://sswap-c.iplantcollaborative.org/test/invoke-qtl-by-trait-accession.jsp"), resource
		                .getInputURI());

		Collection<String> imports = rdg.getImports();

		assertNotNull(imports);

		assertEquals(7, imports.size());
		assertTrue(imports.contains("http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/owlImports"));
		assertTrue(imports.contains("http://sswapmeet.sswap.info/map/owlImports"));
		assertTrue(imports.contains("http://sswapmeet.sswap.info/NCBITaxonomyRecord/owlImports"));
		assertTrue(imports.contains("http://sswapmeet.sswap.info/trait/owlImports"));
		assertTrue(imports.contains("http://sswapmeet.sswap.info/qtl/owlImports"));
		assertTrue(imports.contains("http://sswapmeet.sswap.info/oboInOwl/owlImports"));
		assertTrue(imports.contains("http://sswapmeet.sswap.info/sswap/owlOntology"));

		SSWAPGraph graph = rdg.getResource().getGraph();

		assertNotNull(graph);
		assertEquals(resource, graph.getResource());

		SSWAPSubject subject = graph.getSubject();

		assertNotNull(subject);
		assertEquals(graph, subject.getGraph());
		
		Collection<SSWAPType> types = subject.getDeclaredTypes();

		assertNotNull(types);

		assertTrue(types.contains(rdg.getType(URI.create("http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/QtlByTraitAccessionRequest"))));
		assertTrue(types.contains(rdg.getType(URI.create("http://sswapmeet.sswap.info/sswap/Subject"))));

		// check whether it contains accessionID

		assertEquals(1, subject.getProperties().size());

		assertTrue(getPropertyURIs(subject.getProperties()).contains(
		                URI.create("http://sswapmeet.sswap.info/trait/accessionID")));

		SSWAPObject object = subject.getObject();

		assertNotNull(object);
		assertEquals(subject, object.getSubject());

		types = object.getDeclaredTypes();

		assertTrue(types.contains(rdg.getType(URI.create("http://sswapmeet.sswap.info/sswap/Object"))));

		Collection<URI> objectProperties = getPropertyURIs(object.getProperties());

		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/qtl/accessionID")));
		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/qtl/symbol")));
		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/trait/symbol")));
		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/trait/name")));
		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/NCBITaxonomyRecord/commonName")));
		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/map/name")));
		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/map/startPosition")));
		assertTrue(objectProperties.contains(URI.create("http://sswapmeet.sswap.info/map/endPosition")));
		
		SSWAPProvider provider = rdg.getProvider();
		
		assertNotNull(provider);
		assertEquals("http://sswap-c.iplantcollaborative.org/test/resourceProvider", provider.getURI().toString());
		
		PDG pdg = provider.getPDG();
		
		assertNotNull(pdg);
		pdg.dereference();
		assertEquals("http://sswap-c.iplantcollaborative.org/test/resourceProvider", pdg.getURI().toString());
		assertEquals("Gramene", pdg.getProvider().getName());
	}
		
	/**
	 * Tests whether we can handle properly query strings and fragments encoded in sswap:outputURI
	 * @throws Exception
	 */
	@Test
	public void testURIFragmentHandling() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/Burrows-WheelerAligner-outputURI.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://localhost:8081/resources/UHTS/BWA/Burrows-WheelerAligner"));
				
		fis.close();
		
		SSWAPResource resource = rdg.getResource();
		URI outputURI = resource.getOutputURI();
		
		assertEquals("option=1", outputURI.getQuery());
		assertEquals("fragTest", outputURI.getFragment());
	}
	
	/**
	 * Converts a collection of SSWAPProperties into a collection of the URIs for these properties
	 * 
	 * @param properties a collection of properties
	 * @return
	 */
	private static Collection<URI> getPropertyURIs(Collection<SSWAPProperty> properties) {
		Set<URI> result = new HashSet<URI>();

		for (SSWAPProperty property : properties) {
			result.add(property.getURI());
		}

		return result;
	}
		
	/**
	 * Checks whether we throw DataAccessException when reading malformed RDF/XML data.
	 * 
	 * @throws DataAccessException -- expected behavior
	 */
	@Test(expected=DataAccessException.class)
	public void testReadMalformedData() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/qtl-by-trait-accession-malformed.owl");
		
		SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://sswap.gramene.org/vpin/qtl-by-trait-accession"));
				
		fis.close();
	}
	
	/**
	 * Checks the message in DataAccessException when RDF/XML is not valid (malformed XML/opening and closing tags do not match).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadMalformedData2() throws Exception {
		try {
			FileInputStream fis = new FileInputStream("test/data/qtl-by-trait-accession-malformed.owl");

			SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://sswap.gramene.org/vpin/qtl-by-trait-accession"));
			
			// the line below should not be reached -- DataAccessException should be generated
			fail();
		}
		catch (DataAccessException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("RDF/XML Syntax Error"));
		}
	}

	/**
	 * Checks the message in DataAccessException when RDF/XML is not valid (the top level tag is not rdf:RDF).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadMalformedData3() throws Exception {
		try {
			FileInputStream fis = new FileInputStream("test/data/qtl-by-trait-accession-malformed2.owl");

			SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://sswap.gramene.org/vpin/qtl-by-trait-accession"));

			// the line below should not be reached -- DataAccessException should be generated
			fail();
		}
		catch (DataAccessException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("RDF/XML Syntax Error"));
		}
	}


	/**
	 * Checks the message in DataAccessException when RDF/XML is not valid (there is some data in front of the XML document, which should not be there).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadMalformedData4() throws Exception {
		try {
			FileInputStream fis = new FileInputStream("test/data/qtl-by-trait-accession-malformed3.owl");

			SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://sswap.gramene.org/vpin/qtl-by-trait-accession"));

			// the line below should not be reached -- DataAccessException should be generated
			fail();
		}
		catch (DataAccessException e) {
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains("RDF/XML Syntax Error"));
		}
	}
	
	/**
	 * Checks the message in DataAccessException when an I/O error occurs.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadMalformedData5() throws Exception {
		try {
			FileInputStream fis = new FileInputStream("test/data/qtl-by-trait-accession-malformed3.owl");
			
			// close the stream to make it invalid (so that IOException is raised on read attempt)
			fis.close();

			SSWAP.getResourceGraph(fis, RDG.class, URI.create("http://sswap.gramene.org/vpin/qtl-by-trait-accession"));

			// the line below should not be reached -- DataAccessException should be generated
			fail();
		}
		catch (DataAccessException e) {
			System.out.println(e.getMessage());
			assertFalse(e.getMessage().contains("RDF/XML Syntax Error"));
		}
	}
	
	@Test
	public void testMultiLineHandling() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		
		assertTrue(rdg.getResource().getOneLineDescription().contains("\n"));
	}
	
	@Test
	public void testIconHandling() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		
		SSWAPResource resource = rdg.getResource();

		assertNull(resource.getIcon());
		resource.setIcon(URI.create("http://sswap.info/"));
		
		assertNotNull(resource.getIcon());
		assertEquals(URI.create("http://sswap.info/"), resource.getIcon());
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		rdg.serialize(bos);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		
		RDG rdg2 = SSWAP.getResourceGraph(bis, RDG.class, URI.create("http://sswap-c.iplantcollaborative.org.org/test/qtl-by-trait-accession"));
		
		SSWAPResource resource2 = rdg2.getResource();
		
		assertNotNull(resource2.getIcon());
		assertEquals(URI.create("http://sswap.info/"), resource2.getIcon());
	}
	
	@Test
	public void testSharedURIsRead() throws Exception {
		// read RDG where resource, subject, object share URI
		FileInputStream fis = new FileInputStream("test/data/shared-uris-1.owl");
		
		RDG rdg = SSWAP.getResourceGraph(fis, RDG.class);
		
		fis.close();
		
		// analyze/access resource/subject/object
		
		SSWAPResource resource = rdg.getResource();
		
		SSWAPSubject subject = ImplFactory.get().castDependentModel((SourceModel) rdg, resource.getURI(), SubjectImpl.class);
		
		assertNotNull(subject);
		
		SSWAPObject object = ImplFactory.get().castDependentModel((SourceModel) rdg, resource.getURI(), ObjectImpl.class);
		
		assertNotNull(object);
		
		SSWAPResource resourceCast = ImplFactory.get().castDependentModel((SourceModel) rdg, resource.getURI(), ResourceImpl.class);
		
		assertNotNull(resourceCast);
		
		SSWAPPredicate namePredicate = rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/name")); 
		
		// check that none of the resource/subject/object has namePredicate
		assertNull(resource.getProperty(namePredicate));
		assertNull(subject.getProperty(namePredicate));
		assertNull(object.getProperty(namePredicate));
		
		// set predicate on resource
		resource.setProperty(namePredicate, "somename");
		
		// check that the predicate is actually set
		assertNotNull(resource.getProperty(namePredicate));
		assertEquals("somename", resource.getProperty(namePredicate).getValue().asString());
		
		// check that predicate propagated automatically to subject and object
		assertNotNull(subject.getProperty(namePredicate));
		assertEquals("somename", subject.getProperty(namePredicate).getValue().asString());
		
		assertNotNull(object.getProperty(namePredicate));
		assertEquals("somename", object.getProperty(namePredicate).getValue().asString());
		
		resource.getGraph().getSubject().getObject();
		
		for (SSWAPGraph testGraph : resource.getGraphs()) {
			for (SSWAPSubject testSubject : testGraph.getSubjects()) {
				for (SSWAPObject testObject : testSubject.getObjects()) {
					for (SSWAPSubject testSubject2 : testObject.getSubjects()) {
						assertEquals(testSubject, testSubject2);
						
						for (SSWAPGraph testGraph2 : testSubject2.getGraphs()) {
							assertEquals(testGraph, testGraph2);
							
							assertEquals(resource, testGraph.getResource());
						}
					}
				}
			}
		}		
	}
	
	@Test
	public void testCopyIndividuals() throws URISyntaxException, ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));

		SSWAPResource resource = rdg.getResource();
		
		SSWAPResource resourceCopy = rdg.newIndividual(resource);
		
		assertNotNull(resourceCopy);
		assertTrue(resourceCopy.isAnonymous());
		
		SSWAPResource resourceCopy2 = rdg.newIndividual(resource, URI.create("http://sswap.info/"));
		
		assertNotNull(resourceCopy2);
		assertFalse(resourceCopy2.isAnonymous());
		assertEquals(URI.create("http://sswap.info/"), resourceCopy2.getURI());
		
		SSWAPGraph graph = resource.getGraph();
		
		SSWAPGraph graphCopy = rdg.newIndividual(graph);
		
		assertNotNull(graphCopy);
		assertTrue(graphCopy.isAnonymous());
		
		SSWAPSubject subject = graph.getSubject();
		
		SSWAPSubject subjectCopy = rdg.newIndividual(subject);
		
		assertNotNull(subjectCopy);
		assertTrue(subjectCopy.isAnonymous());
		
		SSWAPSubject subjectCopy2 = rdg.newIndividual(subject, URI.create("http://sswap.info/someSubject"));
		assertNotNull(subjectCopy2);
		assertFalse(subjectCopy2.isAnonymous());
		assertEquals(URI.create("http://sswap.info/someSubject"), subjectCopy2.getURI());
				
		SSWAPSubject subjectCopy3 = rdg.newIndividual(subject, URI.create("http://sswap.info/"));
		assertNotNull(subjectCopy3);
		assertFalse(subjectCopy3.isAnonymous());
		assertEquals(URI.create("http://sswap.info/"), subjectCopy3.getURI());
		
		assertTrue(subjectCopy3.isSSWAPResource());
		assertTrue(subjectCopy3.isSSWAPSubject());
		
		SSWAPSubject subjectCopy4 = rdg.newIndividual(subject, URI.create("http://sswap.info/"));
		assertNotNull(subjectCopy4);
		assertFalse(subjectCopy4.isAnonymous());
		assertEquals(URI.create("http://sswap.info/"), subjectCopy4.getURI());
		assertTrue(subjectCopy3 == subjectCopy4);
		
		SSWAPObject object = subject.getObject();
		
		SSWAPObject objectCopy = rdg.newIndividual(object);
		assertNotNull(objectCopy);
		assertTrue(objectCopy.isAnonymous());
		
		SSWAPObject objectCopy2 = rdg.newIndividual(object, URI.create("http://sswap.info/someObject"));
		assertNotNull(objectCopy2);
		assertFalse(objectCopy2.isAnonymous());

		subject.setObject(objectCopy2);
		rdg.serialize(System.out);

		SSWAPObject objectCopy3 = rdg.newIndividual(object, URI.create("http://sswap.info/"));
		assertNotNull(objectCopy3);
		assertFalse(objectCopy3.isAnonymous());
		assertEquals(URI.create("http://sswap.info/"), objectCopy3.getURI());
		
		SSWAPIndividual ind = rdg.createIndividual();
		
		ind.setProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), rdg.createLiteral("abc"));
		ind.setProperty(rdg.getPredicate(URI.create(NS + "myObjectProperty")), ind);
		
		SSWAPIndividual ind2 = rdg.newIndividual(ind);
			
		ind = rdg.createIndividual(URI.create("http://sswap.info/someIndividual"));
		
		ind.setProperty(rdg.getPredicate(URI.create("http://sswapmeet.sswap.info/trait/symbol")), rdg.createLiteral("abc"));
		ind.setProperty(rdg.getPredicate(URI.create(NS + "myObjectProperty")), ind);
		
		ind2 = rdg.newIndividual(ind, URI.create("http://sswap.info/someIndividual2"));
		
		subject.getObject().setProperty(rdg.getPredicate(URI.create(NS + "myObjectProperty")), ind2);
	}
}
