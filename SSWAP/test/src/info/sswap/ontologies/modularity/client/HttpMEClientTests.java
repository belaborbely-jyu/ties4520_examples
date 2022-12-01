/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.modularity.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.openrdf.model.vocabulary.OWL;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class HttpMEClientTests {

	/*
	 * Basically tests that one client can handle two different requests
	 */
	@Test
	public void testExtractResolve() throws Exception {
		HttpMEClient client = new HttpMEClient();
		URI term = URI.create("http://sswapmeet.sswap.info/oboInOwl/hasSubset");
		
		Model module = client.resolveTerm(term, null);
		
		module.write(System.out);
		
		assertEntityType(module, term.toString(), OWL.ANNOTATIONPROPERTY.stringValue());
		
		URI term2 = URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/TermSearchRequest");
		
		module = client.extract(Arrays.asList(term2), ModuleType.BOT, true);
		
		assertNotNull(module);
		assertFalse(module.isEmpty());		
	}	
	
	@Test
	public void testResolveTerm() throws Exception {
		HttpMEClient client = new HttpMEClient();
		URI term = URI.create("http://sswapmeet.sswap.info/oboInOwl/hasSubset");
		
		Model module = client.resolveTerm(term, null);
		
		assertNotNull(module);
		
		System.out.println("Module: " + module.size() + " RDF triples");
		
		module.write(System.out);
		
		assertEntityType(module, term.toString(), OWL.ANNOTATIONPROPERTY.stringValue());
	}
	
	@Test
	public void testResolveTerm2() throws Exception {
		HttpMEClient client = new HttpMEClient();
		URI term = URI.create("http://sswapmeet.sswap.info/trait/symbol");
		Model module = client.resolveTerm(term, null);
		
		assertNotNull(module);
		
		module.write(System.out);
		
		assertEntityType(module, term.toString(), OWL.DATATYPEPROPERTY.stringValue());
	}
	
	@Test
	public void testResolveDereferenceableTerm() throws Exception {
		HttpMEClient client = new HttpMEClient();
		URI term = URI.create("http://community.clarkparsia.com:16384/sswap_me/test/data/MyAccessionID");
		Model module = client.resolveTerm(term, null);
		
		assertNotNull(module);
		
		System.out.println("Module: " + module.size() + " RDF triples");
		
		module.write(System.out);
		
		assertEntityType(module, term.toString(), OWL.DATATYPEPROPERTY.stringValue());
	}	
	
	@Test
	public void testResolveTerm3() throws Exception {
		HttpMEClient client = new HttpMEClient();
		URI term = URI.create("http://sswapmeet.sswap.info/trait/hasTrait");
		
		Model module = client.resolveTerm(term, null);
		
		assertNotNull(module);
		
		System.out.println("Module: " + module.size() + " RDF triples");
		
		module.write(System.out);
		
		assertEntityType(module, term.toString(), OWL.OBJECTPROPERTY.stringValue());
	}	
	
	
	@Test
	public void testExtractBot() throws Exception {
		HttpMEClient client = new HttpMEClient();
		URI term = URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/TermSearchRequest");
		
		Model module = client.extract(Arrays.asList(term), ModuleType.BOT, true);
		
		assertNotNull(module);
		
		System.out.println("Module: " + module.size() + " RDF triples");
		
		OutputStreamWriter stdwriter = new OutputStreamWriter(System.out);
		module.write(stdwriter);
		stdwriter.flush();
	}
	
	@Test
	public void testExtractTop() throws Exception {
		HttpMEClient client = new HttpMEClient();
		URI term = URI.create("http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/TermSearchRequest");
		
		Model module = client.extract(Arrays.asList(term), ModuleType.TOP, true);
		
		assertNotNull(module);
		
		System.out.println("Module: " + module.size() + " RDF triples");
	}
	
	
	@Test
	public void testExtractBot2() throws Exception {
		HttpMEClient client = new HttpMEClient();
		Collection<URI> sig = getTestSignature();
		
		Model module = client.extract(sig, ModuleType.BOT, true);
		
		assertNotNull(module);
		
		module.write(System.out);
		
		/*Module empty = new JenaModule().union(module);
		
		OutputStream stream = new FileOutputStream(new File("/Users/pklinov/work/UA/j2ee/SSWAP-Java-API-pavel/test/test_ontology_client.owl"));
		empty.write(stream);
		stream.flush();
		stream.close();*/
				
		System.out.println("Module: " + module.size() + " RDF triples");
	}

	private Collection<URI> getTestSignature() {
		URI[] sig = new URI[] { URI
				.create("http://sswapmeet.sswap.info/map/units") };

		return Arrays.asList(sig);
	}
		
	private void assertEntityType(Model jenaModel, String term, String type) {
		// jenaModel.write(System.out);

		StmtIterator iter = jenaModel.listStatements(
				jenaModel.getResource(term), RDF.type,
				jenaModel.getResource(type));

		assertTrue(iter.hasNext());
	}		

}
