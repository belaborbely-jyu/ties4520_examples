/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import info.sswap.api.model.DataAccessException;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.empire.io.ByteLimitExceededException;
import info.sswap.impl.empire.io.ByteLimitInputStream;
import info.sswap.impl.empire.io.Closure;
import info.sswap.impl.empire.io.ClosureBuilder;
import info.sswap.impl.empire.io.ClosureBuilderFactory;
import info.sswap.impl.empire.io.ModelCache;
import info.sswap.impl.empire.model.JenaModelFactory;
import info.sswap.impl.empire.model.ModelUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import com.complexible.common.base.Memory;
import com.google.common.io.ByteStreams;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Tests for computing the closure
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ClosureTests {
	public static File TEST_FILE = new File("test/data/qtl-by-trait-accession.owl");
		
	/**
	 * Builds a 1st degree closure (from a dereferenced model) and a second degree closure, and checks that the consecutive closures contain more
	 * facts.
	 * 
	 * @throws IOException if an I/O error should occur
	 */
	@Test
	public void testBuildClosures() throws IOException {
		Model baseModel = getModel("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession");
		
		Closure closure1 = ClosureBuilderFactory.newInstance().newBuilder().build(baseModel, "http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", 1);		
		assertNotNull(closure1);
		assertTrue(closure1.getClosureModel().size() > 0);
				
		Closure closure2 = ClosureBuilderFactory.newInstance().setMaxThreads(5).setMaxTime(10000).newBuilder().build(baseModel, "http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession", 3);
		
		assertTrue(closure1.getClosureModel().size() <= closure2.getClosureModel().size());
	}
	
	/**
	 * Utility method for getting a model from the specified URI
	 * 
	 * @param uri the URI
	 * @return the model
	 * @throws MalformedURLException if the URI is not syntactically valid
	 * @throws IOException if an I/O error should occur
	 */
	private Model getModel(String uri) throws MalformedURLException, IOException, DataAccessException {
		InputStream is = new URL(uri).openStream();		
		Model model = JenaModelFactory.get().getModel(is);
		is.close();
		
		ModelUtils.removeBNodes(model);
		
		return model;
	}
	
	/**
	 * Tests ByteLimitInputStream with a limit much larger than the length of the content
	 * @throws IOException if an I/O error should occur
	 */
	@Test
	public void testByteLimitInputStreamLargeLimit() throws IOException {
		readFileWithByteLimit(TEST_FILE, TEST_FILE.length() * 2);
	}
	
	/**
	 * Tests ByteLimitInputStream with a limit exactly equal to the length of the content (+1 byte).
	 * @throws IOException if an I/O error should occur
	 */
	@Test
	public void testByteLimitInputStreamExactLimit() throws IOException {
		readFileWithByteLimit(TEST_FILE, TEST_FILE.length() + 1);
	}
	
	/**
	 * Tests ByteLimitInputStream with a limit lower than the length of the content. It is expected that this 
	 * method will generate ByteLimitExceededException
	 * @throws IOException if an I/O error should occur
	 */
	@Test(expected=ByteLimitExceededException.class)
	public void testByteLimitInputStreamInsufficientLimit() throws IOException {
		readFileWithByteLimit(TEST_FILE, TEST_FILE.length() - 1);
	}
	
	/**
	 * Reads a file with a specified byte limit
	 * 
	 * @param file the file to be read
	 * @param byteLimit the byte limit
	 * @throws ByteLimitExceededException if a byte limit is exceeded during the read
	 * @throws IOException if another I/O error should occur
	 */
	private void readFileWithByteLimit(File file, long byteLimit) throws ByteLimitExceededException, IOException {
		FileInputStream fis = new FileInputStream(file);
		ByteLimitInputStream byteLimitInputStream = new ByteLimitInputStream(fis, byteLimit);
		
		ByteStreams.toByteArray(byteLimitInputStream);
		
		byteLimitInputStream.close();
	}
	
	private void assertModelContains(Model largerDegree, Model smallerDegree) {
		for (StmtIterator it = smallerDegree.listStatements(); it.hasNext(); ) {
			Statement s = it.next();
			
			if (!largerDegree.contains(s)) {
				if (s.getSubject().isAnon()) {
					continue;
				}
				
				if (s.getObject().isResource() && s.getObject().isAnon()) {
					continue;
				}
				
				throw new AssertionError("Larger degree model does not contain statement from a smaller degree model: " + s);
			}
		}
	}
	
	@Test
	public void testTermSearchClosureRetrieval() throws Exception {
		Model baseModel = getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch");
		
		ClosureBuilder builder = null;
		
		// build zeroth level, which should contain nothing but information obtained during the type retrieval step
		builder = ClosureBuilderFactory.newInstance().newBuilder();
		Closure zerothLevel = builder.build(baseModel, "http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", 0, 0);
		assertEquals(0, zerothLevel.getDegree());
		
		// type retrieval should have obtained information that sswap:Provider is an OWL.class
		assertTrue(zerothLevel.getClosureModel().contains(Vocabulary.SSWAP_PROVIDER, RDF.type, OWL.Class));
		
		// type retrieval should have obtained information that poAnnotation:exactMatch is an owl:DatatypeProperty
		assertTrue(zerothLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/TermSearchRequest"), 
						RDF.type, 
						OWL.Class));

		// however it should NOT have retrieved more information about poAnnotation:exactMatch (e.g., that it is a subproperty of poAnnotation:datatypeProperties)
		assertFalse(zerothLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/exactMatch"), 
						RDFS.subPropertyOf, 
						baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperties")));
		
		// Now, let's build first level
		builder = ClosureBuilderFactory.newInstance().newBuilder();
		Closure firstLevel = builder.build(baseModel, "http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", 1, 0);
		assertEquals(1, firstLevel.getDegree());
		
		// first level should include zeroth level
		assertModelContains(firstLevel.getClosureModel(), zerothLevel.getClosureModel());

		
		assertTrue(firstLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/TermSearchRequest"), 
						RDFS.subClassOf, 
						baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/POAnnotation")));

		
		// it should not contain this information yet
		assertFalse(firstLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/exactMatch"), 
						RDFS.subPropertyOf, 
						baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperties")));
		
		assertFalse(firstLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty"), 
						RDF.type, 
						OWL.DatatypeProperty));
		
		assertFalse(firstLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty"), 
						RDFS.comment, 
						baseModel.createTypedLiteral("Super property of all data type properties in this ontology.")));

		// Now, let's build second level 
		builder = ClosureBuilderFactory.newInstance().newBuilder();
		Closure secondLevel = builder.build(baseModel, "http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", 2, 0);
		assertEquals(2, secondLevel.getDegree());
		
		assertModelContains(secondLevel.getClosureModel(), zerothLevel.getClosureModel());
		assertModelContains(firstLevel.getClosureModel(), zerothLevel.getClosureModel());
		
		// it should NOW contain the previously missing information about poAnnotation:exactMatch (e.g., that it is a subproperty of poAnnotation:datatypeProperties)
		assertTrue(secondLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/exactMatch"), 
						RDFS.subPropertyOf, 
						baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty")));
		
		assertTrue(secondLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty"), 
						RDF.type, 
						OWL.DatatypeProperty));
		
		assertFalse(secondLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty"), 
						RDFS.comment, 
						secondLevel.getClosureModel().createTypedLiteral("Super property of all data type properties in this ontology.", XSDDatatype.XSDstring)));
		
		// time for third level
		builder = ClosureBuilderFactory.newInstance().newBuilder();
		Closure thirdLevel = builder.build(baseModel, "http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", 3, 0);
		assertEquals(3, thirdLevel.getDegree());
				
		assertModelContains(thirdLevel.getClosureModel(), zerothLevel.getClosureModel());
		assertModelContains(thirdLevel.getClosureModel(), firstLevel.getClosureModel());
		assertModelContains(thirdLevel.getClosureModel(), secondLevel.getClosureModel());
		
		assertTrue(thirdLevel.getClosureModel().contains(baseModel.getResource("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty"), 
						RDFS.comment, 
						thirdLevel.getClosureModel().createTypedLiteral("Super property of all data type properties in this ontology.", XSDDatatype.XSDstring)));
		
	}
	
	@Test
	public void testModelCache() {
		ModelCache modelCache = new ModelCache(-1, -1, false /* diskCacheEnabled */);
		
		// an empty (but not-null) test model that will be cached in this test
		Model testModel = JenaModelFactory.get().createEmptyModel();
		
		// first test some regular http URIs
		assertNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
		modelCache.setModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", testModel);
		assertNotNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
		
		//verify that clear works properly
		
		modelCache.clear();
		assertNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
		
		// now some tag:uris and urn:s (without #)
		assertNull(modelCache.getModel("tag:sswap.info,2011-01-31:sswap:java:api:ClosureTestTerm"));
		modelCache.setModel("tag:sswap.info,2011-01-31:sswap:java:api:ClosureTestTerm", testModel);
		assertNotNull(modelCache.getModel("tag:sswap.info,2011-01-31:sswap:java:api:ClosureTestTerm"));
		
		assertNull(modelCache.getModel("urn:sswap.info:test:ClosureTestTerm"));
		modelCache.setModel("urn:sswap.info:test:ClosureTestTerm", testModel);
		assertNotNull(modelCache.getModel("urn:sswap.info:test:ClosureTestTerm"));
		
		// now test URIs with #
		assertNull(modelCache.getModel("http://example.com/dummyOntology.owl#Term1"));
		modelCache.setModel("http://example.com/dummyOntology.owl#Term1", testModel);
		assertNotNull(modelCache.getModel("http://example.com/dummyOntology.owl#Term1"));
		
		// now we should also have cached model for all other URIs from that ontology (including the ontology itself) -- even if we have not
		// added them explicitly with the exact fragment part
		assertNotNull(modelCache.getModel("http://example.com/dummyOntology.owl#Term2"));
		assertNotNull(modelCache.getModel("http://example.com/dummyOntology.owl"));
		
		// URI fragment testing in tag: URIs and urn: URIs
		assertNull(modelCache.getModel("tag:sswap.info,2011-01-31:sswap:java:api:ClosureTests#Term1"));
		modelCache.setModel("tag:sswap.info,2011-01-31:sswap:java:api:ClosureTests#Term1", testModel);
		assertNotNull(modelCache.getModel("tag:sswap.info,2011-01-31:sswap:java:api:ClosureTests#Term1"));
		assertNotNull(modelCache.getModel("tag:sswap.info,2011-01-31:sswap:java:api:ClosureTests#Term2"));
		
		assertNull(modelCache.getModel("urn:sswap.info:test:ClosureTests#Term1"));
		modelCache.setModel("urn:sswap.info:test:ClosureTests#Term1", testModel);
		assertNotNull(modelCache.getModel("urn:sswap.info:test:ClosureTests#Term1"));
		assertNotNull(modelCache.getModel("urn:sswap.info:test:ClosureTests#Term2"));
	}
	
	@Test
	public void testModelCacheExpiration() {
		ModelCache modelCache = new ModelCache(500 /* entryTTL */, 200 /* negativeEntryTTL */, false /* diskCacheEnabled */);
		
		Model testModel = JenaModelFactory.get().createEmptyModel();
		
		// first try caching of existing, non-sswap terms
		modelCache.setModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", testModel);
		
		assertNotNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
		
		try {
	        Thread.sleep(501);
        }
        catch (InterruptedException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        
        // try caching of inaccessible, non-sswap terms
        assertNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
        
        modelCache.setAsInaccessible("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty");
        
        assertNotNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty"));
        
        try {
	        Thread.sleep(201);
        }
        catch (InterruptedException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
                
        assertNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/datatypeProperty"));
        
        // try caching of inaccessible, sswap terms (should not happen)
        assertNull(modelCache.getModel(Vocabulary.SSWAP_SUBJECT.toString()));
        modelCache.setAsInaccessible(Vocabulary.SSWAP_SUBJECT.toString());
        
        assertNull(modelCache.getModel(Vocabulary.SSWAP_SUBJECT.toString()));
        
        try {
	        Thread.sleep(201);
        }
        catch (InterruptedException e) {
	        e.printStackTrace();
        }
        
        assertNull(modelCache.getModel(Vocabulary.SSWAP_SUBJECT.toString()));
        
        // try caching of accessible sswap terms
        modelCache.setModel(Vocabulary.SSWAP_SUBJECT.toString(), testModel);
        
        assertNotNull(modelCache.getModel(Vocabulary.SSWAP_SUBJECT.toString()));
        
		try {
	        Thread.sleep(501);
        }
        catch (InterruptedException e) {
	        e.printStackTrace();
        }

        assertNull(modelCache.getModel(Vocabulary.SSWAP_SUBJECT.toString()));
        
        // checked mixed accessible/non-accessible caching
        // first say term is accessible
        modelCache.setModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", testModel);
        
        // then that it is not (the last set should prevail)
        modelCache.setAsInaccessible("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch");       
        
        assertNotNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
        
        try {
	        Thread.sleep(201);
        }
        catch (InterruptedException e) {
	        e.printStackTrace();
        }
        
        // after 200 ms, the term should be expired
        assertNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
        
        // now test the other way

        // first say term is inaccessible
        modelCache.setAsInaccessible("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch");
        
        // then that it is (the last set should prevail)
        modelCache.setModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch", testModel);
        
        assertNotNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));

        try {
	        Thread.sleep(201);
        }
        catch (InterruptedException e) {	      
	        e.printStackTrace();
        }

        assertNotNull(modelCache.getModel("http://plantontology.sswap.info/poAnnotations/resources/termSearch/TermSearch"));
	}
	
	@Test
	public void testMIMEOntology() throws Exception {
		Model baseModel = getModel("http://sswapmeet.sswap.info/mime/text/Plain");
		
		ClosureBuilder builder = null;
		
		// build zeroth level, which should contain nothing but information obtained during the type retrieval step and hierarchy retrieval
		builder = ClosureBuilderFactory.newInstance().newBuilder();
		Closure closure = builder.build(baseModel, "http://sswapmeet.sswap.info/mime/text/Plain", 0);
		
		// although the requested level was 0 -- the closure level should be 4 (because of the additional hierarchy retrieval)
		assertEquals(4, closure.getDegree());

	}
	
	@Test
	public void testContentNegotiation() throws IOException {
		testOntologyClosure("http://purl.oclc.org/NET/ssnx/ssn");
	}
	
	@Test
	public void testHTTPSRedirection() throws IOException {
		testOntologyClosure("http://www.auto.tuwien.ac.at/downloads/thinkhome/ontology/EnergyResourceOntology.owl");		
	}
	
	@Test
	public void testHashURIs() throws IOException {
		
		// 'prov' fails because http://www.w3.org/ns/prov (w/ required "Accept: application/rdf+xml") is not OWL-DL
		// (due to prov-o and other prefix issues). http://www.w3.org/ns/prov-o, which contains definitions for
		// the ns = http://www.w3.org/ns/prov, is OWL-DL, but this would require reading terms from one URL as
		// the location of definitions for a different namespace (?isDefinedBy= convention).
		//
		// Failure on OWL-DL causes ClosureBuilder to backtrack (remove terms) to the last DL model, which results
		// in only one term, thus failing the "> 1" conditional on the assertTrue() in testTermClosure().
		
		// String ns = "http://www.w3.org/ns/prov#";
		// testTermClosure(OWL.Class, ResourceFactory.createResource(ns + "Activity"), ResourceFactory.createResource(ns + "Agent"));
		
		String ns = "http://purl.oclc.org/NET/ssnx/ssn#";
		testTermClosure(OWL.Class, ResourceFactory.createResource(ns + "Input"), ResourceFactory.createResource(ns + "Output"));

	}
	
	private void testOntologyClosure(String ontologyURI) throws IOException {
		testTermClosure(OWL.Ontology, ResourceFactory.createResource(ontologyURI));
	}	

	private void testTermClosure(Resource typeURI, Resource... termURIs) throws IOException {
		Model baseModel = ModelFactory.createDefaultModel();
		for (Resource termURI : termURIs) {
			baseModel.add(termURI, RDF.type,typeURI);
		}
		
		Closure closure1 = ClosureBuilderFactory.newInstance()
				.setMaxBytes(Memory.MB)
				.addIgnoredNamespace("http://purl.org/dc/elements/1.1")
				.addIgnoredNamespace("http://creativecommons.org/ns")
				.addIgnoredNamespace("http://www.semanticweb.org/owlapi")
				.newBuilder()
				.build(baseModel, null, 1);
		
		//closure1.getClosureModel().write(System.out);
		
		assertNotNull(closure1);
		assertTrue(closure1.getClosureModel().size() > 1);	
	}
}
