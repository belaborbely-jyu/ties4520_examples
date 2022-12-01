/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPType;

import org.junit.Test;

/**
 * Test cases for cross-document reasoning
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class CrossDocumentReasoningTests {
	@Test
	public void crossDocumentReasoningTestRDG() throws Exception {
		RDG firstRDG = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim"));
		RDG secondRDG = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim"));
		
		SSWAPType firstType = firstRDG.getResource().getGraph().getSubject().getObject().getType();				
		SSWAPType secondType = secondRDG.getResource().getGraph().getSubject().getObject().getType();
		
		assertTrue(firstType.isSubTypeOf(secondType));
		assertTrue(secondType.isSubTypeOf(firstType));
	}
	
	@Test
	public void crossDocumentReasoningTestRDGRIG() throws Exception {
		RDG firstRDG = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim"));
		RIG firstRIG = firstRDG.getRIG();
		
		SSWAPType firstType = firstRDG.getResource().getGraph().getSubject().getObject().getType();				
		SSWAPType secondType = firstRIG.getResource().getGraph().getSubject().getObject().getType();
		
		assertTrue(firstType.isSubTypeOf(secondType));
		assertTrue(secondType.isSubTypeOf(firstType));
	}
	
	@Test
	public void crossDocumentReasoningTestRDGRIGRRG() throws Exception {
		RDG firstRDG = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim"));
		RIG firstRIG = firstRDG.getRIG();
		RRG firstRRG = firstRIG.getRRG();
		
		SSWAPType firstType = firstRDG.getResource().getGraph().getSubject().getObject().getType();				
		SSWAPType secondType = firstRIG.getResource().getGraph().getSubject().getObject().getType();
		SSWAPType thirdType = firstRRG.getResource().getGraph().getSubject().getObject().getType();
		
		assertTrue(firstType.isSubTypeOf(secondType));
		assertTrue(firstType.isSubTypeOf(thirdType));
		assertTrue(secondType.isSubTypeOf(firstType));
		assertTrue(secondType.isSubTypeOf(thirdType));
		assertTrue(thirdType.isSubTypeOf(firstType));
		assertTrue(thirdType.isSubTypeOf(secondType));
	}


	
	@Test
	public void crossDocumentReasoningTestRIG() throws Exception {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim"));
		
		RIG firstRIG = rdg.getRIG();		
		
		SSWAPType firstType = firstRIG.getResource().getGraph().getSubject().getObject().getType();
		
		RIG secondRIG = rdg.getRIG();		
		SSWAPType secondType = secondRIG.getResource().getGraph().getSubject().getObject().getType();
		
		assertTrue(firstType.isSubTypeOf(secondType));
		assertTrue(secondType.isSubTypeOf(firstType));
	}
	
	@Test
	public void crossDocumentReasoningTestRRG() throws Exception {
		RDG rdg = SSWAP.getRDG(URI.create("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/dim"));
		
		RRG firstRRG = rdg.getRIG().getRRG();		
		
		SSWAPType firstType = firstRRG.getResource().getGraph().getSubject().getObject().getType();
		
		RRG secondRRG = rdg.getRIG().getRRG();		
		SSWAPType secondType = secondRRG.getResource().getGraph().getSubject().getObject().getType();
		
		assertTrue(firstType.isSubTypeOf(secondType));
		assertTrue(secondType.isSubTypeOf(firstType));
	}	
}
