/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPProtocol;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.model.JenaModelFactory;
import info.sswap.impl.empire.model.RDGImpl;
import info.sswap.impl.empire.model.RQGImpl;
import info.sswap.impl.empire.model.SourceModelImpl;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Tests for RQG matching.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class RQGTests {
	
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:RQGTest#";
	
	/**
	 * The URI for an RDG (QtlByTraitAccession) used for tests
	 */
	private static final String QTL_BY_TRAIT_ACCESSION_URI = "http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession";
	
	/**
	 * The file containing the current representation of QtlByTraitAccession
	 */
	private static final String QTL_BY_TRAIT_ACCESSION_FILE = "test/data/qtl-by-trait-accession.owl";	
	
	/**
	 * The base for URIs in the test ontology
	 */
	private static final String EXAMPLE_BASE_URI = NS + "rqg-match";

	// The types in the test ontology. They form the following type hierarchy:
	//      A
	//      |
	//      B
	//     / \
	//    C   D
	//    |
	//    E
	
	// Each of the types has a corresponding RDG with a subject of that type
	
	private static final SSWAPType A;
	private static final SSWAPType B;
	private static final SSWAPType C;
	private static final SSWAPType D;
	private static final SSWAPType E;
	
	/**
	 * The Jena model containing all the RDGs for the test types and their closures.
	 */
	private static final Model combinedModel;
	
	static {
		try {
			// create the ontology
			SSWAPDocument ontology = (SourceModelImpl) SSWAP.createSSWAPDocument(URI.create(EXAMPLE_BASE_URI + "/ontology"));

			// initialize the types
			A = ontology.getType(URI.create(EXAMPLE_BASE_URI + "/A"));
			B = ontology.getType(URI.create(EXAMPLE_BASE_URI + "/B"));
			C = ontology.getType(URI.create(EXAMPLE_BASE_URI + "/C"));
			D = ontology.getType(URI.create(EXAMPLE_BASE_URI + "/D"));
			E = ontology.getType(URI.create(EXAMPLE_BASE_URI + "/E"));

			// assert the type hierarchy
			B.addSubClassOf(A);
			C.addSubClassOf(B);
			D.addSubClassOf(B);
			E.addSubClassOf(C);

			// create RDGs
			RDGImpl rdgA = (RDGImpl) createRDG(EXAMPLE_BASE_URI + "/rdg-A", A.getURI().toString());
			RDGImpl rdgB = (RDGImpl) createRDG(EXAMPLE_BASE_URI + "/rdg-B", B.getURI().toString());
			RDGImpl rdgC = (RDGImpl) createRDG(EXAMPLE_BASE_URI + "/rdg-C", C.getURI().toString());
			RDGImpl rdgD = (RDGImpl) createRDG(EXAMPLE_BASE_URI + "/rdg-D", D.getURI().toString());
			RDGImpl rdgE = (RDGImpl) createRDG(EXAMPLE_BASE_URI + "/rdg-E", E.getURI().toString());

			// create an empty Jena model
			combinedModel = JenaModelFactory.get().createEmptyModel();

			// include all the RDGs and their closures in the model
			
			combinedModel.add(rdgA.getModel());
			combinedModel.add(rdgA.getClosureModel());
			combinedModel.add(rdgB.getModel());
			combinedModel.add(rdgB.getClosureModel());
			combinedModel.add(rdgC.getModel());
			combinedModel.add(rdgC.getClosureModel());
			combinedModel.add(rdgD.getModel());
			combinedModel.add(rdgD.getClosureModel());
			combinedModel.add(rdgE.getModel());
			combinedModel.add(rdgE.getClosureModel());
			combinedModel.add(((SourceModelImpl) ontology).getModel());
		} 
		catch (URISyntaxException e) {
			throw new RuntimeException("Unable to initialize the test ontology: ", e);
		}
	}
	
	/**
	 * Creates an RQG by reading its content from the specified file
	 * 
	 * @param path the path to the file
	 * @return the read RQG
	 * @throws IOException if an I/O error should occur
	 */
	private static RQG getRQG(String path) throws IOException {
		FileInputStream fis = new FileInputStream(path);
		RQG result = SSWAP.getRQG(fis);		
		fis.close();
		
		return result;
	}
	
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
	private static RDG getDereferencedRDG(String uri, String path) throws URISyntaxException, IOException, DataAccessException {
		RDG rdg = SSWAP.getRDG(new URI(uri));

		FileInputStream fis = new FileInputStream(path);
		rdg.dereference(fis);
		fis.close();

		return rdg;
	}

	/**
	 * Creates an RDG whose subject will be of the desired type. (Convenience method for testing.)
	 * 
	 * @param uriBase the base for all URIs within the RDG
	 * @param subjectType the URI of the subject
	 * @return the created RDG
	 * @throws URISyntaxException
	 */
	private static RDG createRDG(String uriBase, String subjectType) throws URISyntaxException {		
		RDGImpl rdg = (RDGImpl) SSWAP.createRDG(new URI(uriBase + "/resource"), "Test name", "Test description", new URI(uriBase + "/provider"));

		initializeProtocolGraph(rdg, subjectType);

		// make sure that the underlying Jena model contains all the triples
		rdg.persist();
		
		return rdg;
	}
	
	/**
	 * Creates an RQG whose subject will be of the desired type. (Convenience method for testing.)
	 * 
	 * @param uriBase the base for all URIs within the RQG
	 * @param subjectType the URI of the subject
	 * @return the created RQG
	 * @throws URISyntaxException
	 */
	private static RQG createRQG(String subjectType) throws URISyntaxException {
		RQGImpl rqg = (RQGImpl) SSWAP.createRQG(null);
		
		initializeProtocolGraph(rqg, subjectType);

		// make sure that the underlying Jena model contains all the triples
		rqg.persist();
		
		return rqg;
	}
	
	/**
	 * Initializes the canonical graph to contain one sswap:Graph, with one sswap:Subject that maps onto one sswap:Object.
	 * The sswap:Subject will be initialized to be of specified type. 
	 *  
	 * @param protocolGraph the canonical graph to be initialized
	 * @param subjectType the type of the subject
	 * @throws URISyntaxException
	 */
	private static void initializeProtocolGraph(SSWAPProtocol protocolGraph, String subjectType) throws URISyntaxException {
		SSWAPResource resource = protocolGraph.getResource();
		SSWAPGraph graph = protocolGraph.createGraph();
		SSWAPSubject subject = protocolGraph.createSubject();
		SSWAPObject object = protocolGraph.createObject();
		
		subject.addType(protocolGraph.getType(URI.create(subjectType)));
		
		subject.setObject(object);
		graph.setSubject(subject);
		resource.setGraph(graph);			
	}

	
	/**
	 * Tests creation of RQGs and whether such a generated RQG can be properly used with the reasoning service.
	 * This test case simulates a client creating RQG programmatically, then serializing it to the discovery
	 * server (DS), deserializing it at the DS, and performing reasoning there  
	 *  
	 * @throws URISyntaxException
	 */
	@Test
	public void testCreateRQG() throws URISyntaxException {
		// create an RQG on the client side
		RQG rqg = SSWAP.createRQG(null);		
		assertNotNull(rqg);
		
		SSWAPResource resource = rqg.getResource();		
		assertNotNull(resource);
		
		SSWAPGraph graph = rqg.createGraph();		
		assertNotNull(graph);		
		resource.setGraph(graph);
		
		SSWAPSubject subject = rqg.createSubject();
		assertNotNull(subject);
		graph.setSubject(subject);
		
		SSWAPType myType = rqg.getType(URI.create(NS + "MyType"));
		SSWAPType qtlByTraitAccessionRequest = rqg.getType(URI.create("http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/QtlByTraitAccessionRequest"));
		myType.addSubClassOf(qtlByTraitAccessionRequest);
		
		subject.addType(myType);
		
		SSWAPObject object = rqg.createObject();
		assertNotNull(object);
		subject.setObject(object);		
	
		// serialize the created RQG ("transmission of the RQG to the DS")
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		rqg.serialize(intermediateOutputStream);
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream.toByteArray());
		
		RQG rqg2 = SSWAP.getRQG(intermediateInputStream);
		
		resource = rqg.getResource();		
		assertNotNull(resource);
		
		graph = resource.getGraph();
		assertNotNull(graph);
		
		subject = graph.getSubject();
		assertNotNull(subject);
		
		qtlByTraitAccessionRequest = rqg2.getType(URI.create("http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/QtlByTraitAccessionRequest"));
		
		// test whether the subject of the RQG is of type that is a sublass of the reasoning service
		assertTrue(subject.getType().isSubTypeOf(qtlByTraitAccessionRequest));
		
		object = subject.getObject();		
		assertNotNull(object);	
	}
	
	/**
	 * Tests whether the RQG that is (content-wise) identical to the service (QtlByTraitQAccession) will match that 
	 * service.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Test
	public void testQuery() throws URISyntaxException, IOException {
		RQGImpl rqg = (RQGImpl) getRQG(QTL_BY_TRAIT_ACCESSION_FILE);
		
		rqg.getResource().setOneLineDescription(null);
		rqg.getResource().setAboutURI(null);
		rqg.getResource().setInputURI(null);
		rqg.getResource().setMetadata(null);
		rqg.getResource().setProvider(null);
		
		RDGImpl rdg = (RDGImpl) getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, QTL_BY_TRAIT_ACCESSION_FILE);
		
		System.out.println(rqg.getQuery());
		
		Collection<String> queryResults = rqg.executeQuery(rdg.getSourceModel().getModel(), rdg.getSourceModel().getClosureModel());
		
		assertNotNull(queryResults);
		assertEquals(1, queryResults.size());		
	}
	
	/**
	 * Tests whether an RQG that uses a custom class (a subclass of QtlByTraitAccession) as the type for the subject 
	 * will match QtlByTraitAccession service.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Test
	public void testQuery2() throws URISyntaxException, IOException {
		RQGImpl rqg = (RQGImpl) SSWAP.createRQG(null);		
		assertNotNull(rqg);
		
		SSWAPResource resource = rqg.getResource();		
		assertNotNull(resource);
		
		SSWAPGraph graph = rqg.createGraph();		
		assertNotNull(graph);		
		resource.setGraph(graph);
		
		SSWAPSubject subject = rqg.createSubject();
		assertNotNull(subject);
		graph.setSubject(subject);
		
		SSWAPType myType = rqg.getType(URI.create(NS + "MyType"));
		SSWAPType qtlByTraitAccessionRequest = rqg.getType(URI.create("http://sswap-c.iplantcollaborative.org/test/ontologies/qtl/QtlByTraitAccessionRequest"));
		myType.addSubClassOf(qtlByTraitAccessionRequest);
		
		subject.addType(myType);
		
		SSWAPObject object = rqg.createObject();
		assertNotNull(object);
		subject.setObject(object);
		
		rqg.serialize(System.out);
		
		RDGImpl rdg = (RDGImpl) getDereferencedRDG(QTL_BY_TRAIT_ACCESSION_URI, QTL_BY_TRAIT_ACCESSION_FILE);
		
		Collection<String> queryResults = rqg.executeQuery(rdg.getSourceModel().getModel(), rdg.getSourceModel().getClosureModel());
		
		assertNotNull(queryResults);
		assertEquals(1, queryResults.size());		
	}
	
	/**
	 * Tests subject matching in the test ontology. It creates an RQG whose subject is of type C, and checks whether
	 * it will match all the RDGs whose subjects are of type that is a superclass of C (i.e., C, B, and A). It also
	 * tests whether it won't match other subjects (of types D and E) 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQuery3() throws Exception {		
		RQGImpl rqg = (RQGImpl) createRQG(C.getURI().toString());
		
		System.out.println(rqg.getQuery());
		
		Collection<String> results = rqg.executeQuery(combinedModel);
		
		assertEquals(3, results.size());
		
		assertTrue(results.contains(EXAMPLE_BASE_URI + "/rdg-A/resource"));
		assertTrue(results.contains(EXAMPLE_BASE_URI + "/rdg-B/resource"));
		assertTrue(results.contains(EXAMPLE_BASE_URI + "/rdg-C/resource"));
		assertFalse(results.contains(EXAMPLE_BASE_URI + "/rdg-D/resource"));
		assertFalse(results.contains(EXAMPLE_BASE_URI + "/rdg-E/resource"));
	}	
	
	@Test
	public void testSatisfies1() throws URISyntaxException, IOException {
		RQG rqg = getRQG("test/data/rqg-qtl-by-trait-object-accessionID.owl");
		
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-accession"));
		
		assertTrue(rqg.satisfiesResource(rdg));		
	}
	
	@Test
	public void testSatisfies2() throws URISyntaxException, IOException {
		RQG rqg = getRQG("test/data/rqg-qtl-by-trait-object-accessionID.owl");
		
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-symbol"));
		
		assertTrue(rqg.satisfiesResource(rdg));		
	}
	
	@Test
	public void testSatisfies3() throws URISyntaxException, IOException {
		RQG rqg = getRQG("test/data/rqg-qtl-by-trait-object-accessionID.owl");
		
		RDG rdg = getDereferencedRDG("file:test/data/determine-qtl-taxa", "test/data/determine-qtl-taxa");
		
		assertFalse(rqg.satisfiesResource(rdg));		
	}
	
	@Test
	public void testRQGGenerationBothServices() {
		RDG upstreamRDG = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));		
		RDG downstreamRDG = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-accession"));
		
		RQG rqg = ExtensionAPI.generateRQG(upstreamRDG, downstreamRDG);
		
		rqg.serialize(System.out);
	}
	
	@Test
	public void testRQGGenerationDownstreamServiceOnly() {
		RDG upstreamRDG = null;		
		RDG downstreamRDG = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-accession"));
		
		RQG rqg = ExtensionAPI.generateRQG(upstreamRDG, downstreamRDG);
		
		rqg.serialize(System.out);
	}
	
	@Test
	public void testRQGGenerationUpstreamServiceOnly() {
		RDG upstreamRDG = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));		
		RDG downstreamRDG = null;
		
		RQG rqg = ExtensionAPI.generateRQG(upstreamRDG, downstreamRDG);
		
		rqg.serialize(System.out);
	}
	
	@Test
	public void testRQGGenerationNoServices() {
		RDG upstreamRDG = null;		
		RDG downstreamRDG = null;
		
		RQG rqg = ExtensionAPI.generateRQG(upstreamRDG, downstreamRDG);
		
		rqg.serialize(System.out);
	}
	
	@Test
	public void testRQGQuery() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/rqg-png-image.owl");
		
		RQG rqg = SSWAP.getResourceGraph(fis, RQG.class);
		
		fis.close();
		
		Collection<RDG> results = rqg.invoke(null);
		
		assertNotNull(results);
		
		assertTrue(results.size() >= 4);
		
		List<URI> uris  = new LinkedList<URI>();
		
		for (RDG rdg : results) {
			uris.add(rdg.getURI());
		}
		
		assertTrue(uris.contains(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/grayscale")));
		assertTrue(uris.contains(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim")));
		assertTrue(uris.contains(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/convertToPNG")));
		assertTrue(uris.contains(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/scale")));
	}
	
	@Test
	public void testRQGWithResourceAndRestrictions() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/rqg-scale.owl");
		
		RQG rqg = SSWAP.getResourceGraph(fis, RQG.class);
		
		fis.close();
		
		((RQGImpl) rqg).closeWorld();
		
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/scale"));
		
		assertTrue(rqg.satisfiesResource(rdg));
		
		((RQGImpl) rqg).uncloseWorld();		
	}
}
