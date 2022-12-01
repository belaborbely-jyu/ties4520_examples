/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import info.sswap.api.model.PDG;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.ValidationException;

import org.junit.Test;

/**
 * Tests for SSWAPProvider implementations
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public class ProviderTests {
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:ProviderTest#";
	
	private void testDereferencedProvider(SSWAPProvider provider, URI expectedURI) throws ValidationException {
		assertTrue( provider.isDereferenced() );
		assertFalse( provider.isAnonymous() );
		
		provider.validate();
		
		assertTrue( provider.isIndividual() );
		assertFalse( provider.isList() );
		assertNull( provider.asList() );
		assertFalse( provider.isLiteral() );
		assertFalse( provider.isSSWAPGraph() );
		assertNull( provider.asSSWAPGraph() );
		assertFalse( provider.isSSWAPObject() );
		assertNull( provider.asSSWAPObject() );
		assertTrue( provider.isSSWAPProvider() );
		assertNotNull( provider.asSSWAPProvider() );
		assertFalse( provider.isSSWAPResource() );
		assertNull( provider.asSSWAPResource() );
		assertFalse( provider.isSSWAPSubject() );
		assertNull( provider.asSSWAPSubject() );
		
		assertEquals( expectedURI, provider.getURI() );
		assertEquals( provider.getID(), provider.getURI() );
		
		assertEquals( provider.getName(), "An example SSWAP web resource provider" );
		assertEquals( provider.getOneLineDescription(), "This is an example resource provider" );
		assertEquals( provider.getAboutURI(), URI.create( "http://sswap.info/protocol.jsp" ) );
		assertEquals( provider.getMetadata(), URI.create( "http://sswap.info/examples/metadata.txt" ) );		
	}
	
	@Test
	public void readExamplePDG() throws URISyntaxException, ValidationException {
		PDG pdg = SSWAP.getPDG( URI.create("http://sswap.info/examples/resourceProvider") );
		
		assertNotNull( pdg );		
		assertEquals( pdg.getURI().toString(), "http://sswap.info/examples/resourceProvider" );		
		assertFalse( pdg.isDereferenced() );
		
		pdg.dereference();
		
		assertTrue( pdg.isDereferenced() );
		
		SSWAPProvider provider = pdg.getProvider();
		
		assertNotNull( provider );
		
		testDereferencedProvider(provider, pdg.getURI());
		
		Collection<RDG> rdgs = pdg.getRDGs();
		
		assertNotNull(rdgs);
		assertEquals(3, rdgs.size());
	}
	
	@Test
	public void readExampleProvider() throws URISyntaxException, ValidationException {
		SSWAPProvider provider = SSWAP.createProvider( URI.create("http://sswap.info/examples/resourceProvider") );
		
		assertNotNull( provider );
		
		assertFalse( provider.isDereferenced() );
		
		provider.dereference();
				
		testDereferencedProvider(provider, URI.create("http://sswap.info/examples/resourceProvider"));
	}
	
	@Test
	public void testCreatePDG() throws URISyntaxException {
		PDG pdg = SSWAP.createPDG(URI.create(NS + "testProvider"), "Test provider", "This is a test provider");
		
		assertNotNull(pdg);
		assertEquals(URI.create(NS + "testProvider"), pdg.getURI());
		
		SSWAPProvider provider = pdg.getProvider();
		assertNotNull(provider);
		
		assertEquals(pdg, provider.getPDG());
		
		assertEquals(URI.create(NS + "testProvider"), provider.getURI());
		assertEquals("Test provider", provider.getName());
		assertEquals("This is a test provider", provider.getOneLineDescription());
		
		provider.setAboutURI(URI.create(NS + "aboutURI"));
		provider.setMetadata(URI.create(NS + "metadataURI"));
				
		assertEquals(URI.create(NS + "aboutURI"), provider.getAboutURI());
		assertEquals(URI.create(NS + "metadataURI"), provider.getMetadata());
		
		SSWAPResource resource = SSWAP.createResource(URI.create("http://sswap.info/examples/resources/canonical/canonicalResource"));
		
		provider.addProvidesResource(resource);
		
		RDG rdg = resource.getRDG();
		
		assertNotNull(rdg);
		assertEquals("http://sswap.info/examples/resources/canonical/canonicalResource", rdg.getURI().toString());
		
		rdg.dereference();
		assertEquals("A canonical SSWAP web resource", rdg.getResource().getName());
		
		pdg.serialize(System.out, RDFRepresentation.RDF_XML, true);
		
		Collection<RDG> rdgs = pdg.getRDGs();
		
		assertNotNull(rdgs);
		assertEquals(1, rdgs.size());
		
		RDG rdg2 = rdgs.iterator().next();
	
		assertEquals("http://sswap.info/examples/resources/canonical/canonicalResource", rdg2.getURI().toString());
		assertEquals("A canonical SSWAP web resource", rdg2.getResource().getName());
	}
}
