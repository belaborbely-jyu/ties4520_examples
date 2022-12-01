/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import info.sswap.api.http.HTTPProvider;
import info.sswap.api.http.HTTPProvider.RDGResponse;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.api.spi.HTTPAPI;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import com.google.common.io.ByteStreams;

/**
 * Tests for writing TSV (Tab Separated Values)
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 **/

public class TSVTests {

	private static final String NS = "tag:sswap.info,2012-01-04:sswap:java:api:TSVTests#";
	
	/**
	 * Tests TSV (Tab Separated Values) on RDG, RIG, and RRG
	 * @throws FileNotFoundException on failure to read test file
	 * @throws IOException on failure to process test file into RDG
	 */
	@Test
	public void testTSV() throws FileNotFoundException, IOException {
		
		// read and serialize RDG
		FileInputStream fis = new FileInputStream("test/data/json/TSVTest.json");
		byte[] buf = ByteStreams.toByteArray(fis);
		
		HTTPProvider provider = HTTPAPI.getProvider();
		
		RDGResponse rdgResponse = provider.makeRDG(new ByteArrayInputStream(buf));
		RDG rdg = rdgResponse.getRDG();
		
		// currently requires human inspection for content check:
		rdg.serialize(System.out);
		rdg.serialize(System.out, RDFRepresentation.TSV, false);

		System.out.println("\n");
		
		// may be too slow
		//SSWAPDocument doc = ExtensionAPI.getInferredABox(rdg);
		//doc.serialize(System.out);
		//doc.serialize(System.out,RDFRepresentation.TSV, false);
	}
	
	
}
