/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertEquals;

import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPLiteral;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.ValidationException;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.Vocabulary;

import java.net.URI;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDF;

public class ExtensionAPITests {

	@Test
	public void testInferenceExtraction() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));

		SSWAPDocument d1 = ExtensionAPI.getInferredTBox(rdg);
		
		assertNotNull(d1);
		
		SSWAPDocument d2 = ExtensionAPI.getInferredABox(rdg);
		
		assertNotNull(d2);
		
		SSWAPDocument d3 = ExtensionAPI.getClosureDocument(rdg);
		
		assertNotNull(d3);
	}
	
	@Test
	public void testClosureDocumentExtraction() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		
		assertNotNull(ExtensionAPI.getClosureDocument(rdg));
	}
	
	@Test
	public void testJenaModelAccess() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		
		// try converting whole document to Jena model
		Model m = ExtensionAPI.asJenaModel(rdg);		
		assertNotNull(m);		
		assertTrue(m.contains(m.getResource("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"), RDF.type, Vocabulary.SSWAP_RESOURCE));
		
		// try creating a SSWAPDocument from that model
		RIG rig = ExtensionAPI.createDocument(m, RIG.class);
		assertNotNull(rig);
		
		// try partial extraction of the model
		SSWAPSubject s = rdg.getResource().getGraph().getSubject();		
		assertNotNull(ExtensionAPI.asJenaModel(s));	
	}
	
	@Test
	public void generateRQGTests() throws ValidationException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));

		testRQGGeneration(null, null, null);
		testRQGGeneration(rdg, null, null);
		testRQGGeneration(null, rdg, null);
		testRQGGeneration(rdg, rdg, null);
		
		testRQGGeneration(null, null, URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		testRQGGeneration(rdg, null, URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		testRQGGeneration(null, rdg, URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		testRQGGeneration(rdg, rdg, URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));		
	}
	
	private void testRQGGeneration(RDG rdg1, RDG rdg2, URI resultURI) throws ValidationException {
		RQG rqg = ExtensionAPI.generateRQG(rdg1, rdg2, resultURI);
		
		assertNotNull(rqg);
		rqg.validate();
		
		SSWAPResource resource = rqg.getResource();
		
		if (resultURI == null) {
			assertTrue(resource.isAnonymous());
		}
		else {
			assertFalse(resource.isAnonymous());
			assertEquals(resultURI, resource.getURI());
		}
	}
	
	@Test
	public void testGetAsyncRIG() {
		URI service = URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession");
		URI input= URI.create("http://sswap-c.iplantcollaborative.org/test/some-input-uri");
		
		RIG rig = ExtensionAPI.getAsyncRIG(service, input);
		
		assertNotNull(rig);
	}
	
	@Test
	public void crossDocumentCopyTests() {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		SSWAPDocument dest = SSWAP.createSSWAPDocument();
		
		SSWAPElement copy = ExtensionAPI.copyElement(dest, rdg.getResource());
		
		assertNotNull(copy);
		assertTrue(copy instanceof SSWAPIndividual);
		
		SSWAPLiteral orig = rdg.createLiteral("abc");
		copy = ExtensionAPI.copyElement(dest, orig);
		
		assertNotNull(copy);
		assertTrue(copy instanceof SSWAPLiteral);
	}
}
