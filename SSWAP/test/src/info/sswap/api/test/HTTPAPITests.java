/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import info.sswap.api.http.HTTPClient;
import info.sswap.api.http.HTTPProvider;
import info.sswap.api.http.HTTPClient.RIGResponse;
import info.sswap.api.http.HTTPClient.RQGResponse;
import info.sswap.api.http.HTTPProvider.PDGResponse;
import info.sswap.api.http.HTTPProvider.RDGResponse;
import info.sswap.api.http.HTTPProvider.RRGResponse;
import info.sswap.api.model.PDG;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPSubject;

import info.sswap.api.spi.HTTPAPI;

import org.junit.Test;

import com.google.common.io.ByteStreams;

/**
 * Tests for the Java handle on the HTTP API
 * 
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 */
public class HTTPAPITests {
		
	@Test
	public void createGoodPDG() throws IOException {
		
		FileInputStream fis = new FileInputStream("test/data/json/qtl-by-linkage-group-PDG.json");
		byte[] buf = ByteStreams.toByteArray(fis);
		
		HTTPProvider provider = HTTPAPI.getProvider();
		
		// test reading from an input stream
		PDGResponse pdgResponse = provider.makePDG(new ByteArrayInputStream(buf));
		PDG pdg = pdgResponse.getPDG();
		assertNotNull(pdg);
		
		// test reading from a string
		pdgResponse = provider.makePDG(new String(buf));
		pdg = pdgResponse.getPDG();
		assertNotNull(pdg);
		
	}
	
	@Test
	public void createGoodRDG() throws IOException {
		
		FileInputStream fis = new FileInputStream("test/data/json/qtl-by-linkage-group-RDG.json");
		byte[] buf = ByteStreams.toByteArray(fis);

		HTTPProvider provider = HTTPAPI.getProvider();
		
		// test reading from an input stream
		RDGResponse rdgResponse = provider.makeRDG(new ByteArrayInputStream(buf));
		RDG rdg = rdgResponse.getRDG();
		assertNotNull(rdg);
		
		// test reading from a string
		rdgResponse = provider.makeRDG(new String(buf));
		rdg = rdgResponse.getRDG();
		assertNotNull(rdg);
	}
	
	@Test
	public void createGoodRIG() throws IOException {
		
		FileInputStream fis = new FileInputStream("test/data/json/qtl-by-linkage-group-RIG.json");
		byte[] buf = ByteStreams.toByteArray(fis);

		HTTPClient client = HTTPAPI.getClient();
		
		// test reading from an input stream
		RIGResponse rigResponse = client.makeRIG(new ByteArrayInputStream(buf));
		RIG rig = rigResponse.getRIG();
		assertNotNull(rig);
		
		// test reading from a string
		rigResponse = client.makeRIG(new String(buf));
		rig = rigResponse.getRIG();
		assertNotNull(rig);
	}
	
	@Test
	public void createGoodRRG() throws IOException {
		
		FileInputStream fis = new FileInputStream("test/data/json/qtl-by-linkage-group-RRG.json");
		byte[] buf = ByteStreams.toByteArray(fis);

		HTTPProvider provider = HTTPAPI.getProvider();
		
		// test reading from an input stream
		RRGResponse rrgResponse = provider.makeRRG(new ByteArrayInputStream(buf));
		RRG rrg = rrgResponse.getRRG();
		assertNotNull(rrg);
		
		// test reading from a string
		rrgResponse = provider.makeRRG(new String(buf));
		rrg = rrgResponse.getRRG();
		assertNotNull(rrg);
		
	}
	@Test
	public void createGoodRQG() throws IOException {
		
		FileInputStream fis = new FileInputStream("test/data/json/qtl-by-linkage-group-RQG.json");
		byte[] buf = ByteStreams.toByteArray(fis);

		HTTPClient client = HTTPAPI.getClient();
		
		// test reading from an input stream
		RQGResponse rqgResponse = client.makeRQG(new ByteArrayInputStream(buf));
		RQG rqg = rqgResponse.getRQG();
		assertNotNull(rqg);
		
		// test reading from a string
		rqgResponse = client.makeRQG(new ByteArrayInputStream(buf));
		rqg = rqgResponse.getRQG();
		assertNotNull(rqg);
	}
	
	@Test
	public void testBadPDG() throws IOException {
		
		FileInputStream fis = new FileInputStream("test/data/json/qtl-by-linkage-group-PDG.json");
		byte[] buf = ByteStreams.toByteArray(fis);
		String badInput = new String(buf);
		badInput = badInput.substring(0,badInput.length()/2);	// cut in half
		
		HTTPProvider provider = HTTPAPI.getProvider();
		
		PDGResponse pdgResponse = provider.makePDG(badInput);
		PDG pdg = pdgResponse.getPDG();
		assertNull(pdg);
		
		System.out.println("response code: " + pdgResponse.getResponseCode());

		// any (body) content?
		InputStream content = pdgResponse.getContent();
		if ( content == null ) {
			System.out.println("no content");
		} else {
			System.out.write(ByteStreams.toByteArray(content));
		}
		
		// any error stream?
		InputStream errorStream = pdgResponse.getErrorStream();
		if ( errorStream == null ) {
			System.out.println("no error stream");
		} else {
			System.out.write(ByteStreams.toByteArray(errorStream));
		}
		
		// any SSWAP header?
		for ( String msg : pdgResponse.getSSWAPExceptionValues() ) {
			System.out.println("SSWAP Exception header: " + msg);
		}
		
		System.out.println("end");
	}
	
	@Test
	public void testRIGCreatedByHTTPAPIInvoke() throws Exception {
		FileInputStream fis = new FileInputStream("test/data/json/grayscale-RIG.json");
		byte[] buf = ByteStreams.toByteArray(fis);

		HTTPClient client = HTTPAPI.getClient();
		
		// test reading from an input stream
		RIGResponse rigResponse = client.makeRIG(new ByteArrayInputStream(buf));
		RIG rig = rigResponse.getRIG();
		
		assertEquals("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/test/data/pipeline/grayscale", rig.getURI().toString());
		
		RRGResponse rrgResponse = rig.invoke();
		
		assertNotNull(rrgResponse);
		
		assertEquals(200, rrgResponse.getResponseCode());
		
		RRG rrg = rrgResponse.getRRG();
		
		assertNotNull(rrg);
		
		SSWAPSubject subject = rrg.getResource().getGraph().getSubject();
		
		assertNotNull(subject);
		assertEquals("http://sswap.info/images/uoa-small.png", subject.getURI().toString());
		
		SSWAPObject object = subject.getObject();
		
		assertNotNull(object);
		assertFalse(object.isAnonymous());
		
		assertTrue(object.getURI().toString().startsWith("http://sswap-a.iplantcollaborative.org/sswap-pipeline-test/content"));
	}
}
