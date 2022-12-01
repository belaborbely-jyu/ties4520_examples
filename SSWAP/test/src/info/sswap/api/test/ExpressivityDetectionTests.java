/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import info.sswap.api.model.Expressivity;
import info.sswap.api.model.RDG;
import info.sswap.api.model.SSWAP;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Tests for detecting expressivity of models (their OWL profile)
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ExpressivityDetectionTests {
	/**
	 * Tests the expressivity determination for the RDG of QtlByTraitAccession service (OWL2 DL ontology)
	 * @throws URISyntaxException
	 */
	@Test
	public void testQtlByTraitAccessionRDG() throws URISyntaxException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-c.iplantcollaborative.org/test/qtl-by-trait-accession"));
		rdg.dereference();
		
		rdg.serialize(System.out);

		assertFalse(rdg.checkProfile(Expressivity.EL));
		assertTrue(rdg.checkProfile(Expressivity.DL));
		assertFalse(rdg.checkProfile(Expressivity.RL));
		assertFalse(rdg.checkProfile(Expressivity.QL));

		// the profiles that are not supported yet should always return false
		assertFalse(rdg.checkProfile(Expressivity.Lite));
		assertFalse(rdg.checkProfile(Expressivity.RDFS));
	}	
	

	@Test
	public void testCanonicalGraphRDG() throws URISyntaxException {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap.info/examples/resources/canonical/canonicalResource"));
		rdg.dereference();

		assertFalse(rdg.checkProfile(Expressivity.EL));
		assertTrue(rdg.checkProfile(Expressivity.DL));
		assertFalse(rdg.checkProfile(Expressivity.RL));
		assertFalse(rdg.checkProfile(Expressivity.QL));

		// the profiles that are not supported yet should always return false
		assertFalse(rdg.checkProfile(Expressivity.Lite));
		assertFalse(rdg.checkProfile(Expressivity.RDFS));
	}	
}
