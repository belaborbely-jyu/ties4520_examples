/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.persistence.EntityExistsException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import info.sswap.api.model.*;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.empire.model.ObjectImpl;


/**
 * Test various pathological cases of creating a SSWAPObject
 * 
 * @author Damian Gessler
 **/

public class CreateObject {	

	private static final String NS = "tag:sswap.info,2012-03-02:sswap:java:api:CreateObject#";


	@Test
	public void testCreateObject() {

		// set to various test values below
		SSWAPObject sswapObject;
		URI sswapObjectURI;
		
		// initialize some URIs
		URI pdgURI = URI.create(NS + "providerURI");
		URI rdgURI = URI.create(NS + "resourceURI");
		URI taxonomyRecordURI = URI.create("http://sswapmeet.sswap.info/NCBITaxonomyRecord/TaxonomyRecord");
		
		// create a RDG (so we can get an RIG, so we can .createObject())
		SSWAPProvider sswapProvider = SSWAP.createProvider(pdgURI);
		RDG rdg = SSWAP.createRDG(rdgURI,"RDG name", "RDG oneLineDescription", sswapProvider);

		// get an RIG
		RIG rig = rdg.getRIG();

		// create (get) some types on it
		SSWAPType TaxonomyRecordType = rdg.getType(taxonomyRecordURI);

		// create an object at a null URI and set a type
		// we should silently handle this
		try {
			sswapObjectURI = null;
			sswapObject = rig.createObject(sswapObjectURI);
			sswapObject.addType(TaxonomyRecordType);
		} catch ( Exception e ) {
			Assert.fail(e.getMessage());
		}
		
		// create an object at URN and set a type
		// we should silently handle this
		try {
			sswapObjectURI = URI.create("urn:someNamespaceIdentifier:someNamespaceSpecificString");
			sswapObject = rig.createObject(sswapObjectURI);
			sswapObject.addType(TaxonomyRecordType);
		} catch ( Exception e ) {
			Assert.fail(e.getMessage());
		}
		
		// create an object at an ugly URN and set a type
		// Argh! throw an error
		try {
			sswapObjectURI = URI.create("urn:some NamespaceIdentifier:some \\ + % <<NamespaceSpecific:: String");
			sswapObject = rig.createObject(sswapObjectURI);
			sswapObject.addType(TaxonomyRecordType);
		} catch ( IllegalArgumentException iea ) {
				; // OK, should be thrown from URL.create()
		} catch ( Exception e ) {
			Assert.fail(e.getMessage());
		}
		
		// create an object at a relative URI
		// throw a (deep) IllegalArgumentException
		try {
			sswapObjectURI = URI.create("relativeURI");
			sswapObject = rig.createObject(sswapObjectURI);
			sswapObject.addType(TaxonomyRecordType);
		} catch ( IllegalArgumentException e ) {
			// throws on a relative URI, as it should; so this is OK
			Assert.assertTrue(e.getMessage().contains("Not a valid (absolute) URI:") );			
		} catch ( Exception e ) {
			Assert.fail(e.getMessage());
		}

		// create another object of the URI of a SSWAPType
		// (but SSWAPType is defined in another model RDG -- this should succeed; we should not introduce dependency on another model)
		try {
			sswapObjectURI = TaxonomyRecordType.getURI();
			sswapObject = rig.createObject(sswapObjectURI);
									
			sswapObject.addType(TaxonomyRecordType);			
		} catch ( Exception e ) {
			Assert.fail("Failed to create an object just because RDG happened to define a type with the same URI");
		}
		
		// create another object with the same URI as previously (should fail)
		try {
			sswapObjectURI = TaxonomyRecordType.getURI();
			sswapObject = rig.createObject(sswapObjectURI);
		} catch ( Exception e ) {
			Assert.fail("Unable to create another individual with the same URI as existing individual");
		}
		
		// create another object that uses the same URI as the type in this RIG
		try {
			sswapObjectURI = URI.create("urn:someNamespaceIdentifier:someNamespaceSpecificString2");
			
			rig.getType(sswapObjectURI);			
			sswapObject = rig.createObject(sswapObjectURI);
		} catch ( Exception e ) {
			// proper behavior
			Assert.fail("Unable to create another individual with the same URI as existing type URI");
		}					
	}
		
	@Test
	public void testCreateIndividuals() {
		URI pdgURI = URI.create(NS + "providerURI");
		URI rdgURI = URI.create(NS + "resourceURI");
		
		SSWAPProvider sswapProvider = SSWAP.createProvider(pdgURI);
		RDG rdg = SSWAP.createRDG(rdgURI,"RDG name", "RDG oneLineDescription", sswapProvider);
		
		SSWAPResource resource = rdg.getResource();
		
		assertNotNull(resource);
		assertEquals(rdgURI, resource.getURI());
		assertTrue(resource.getProperties().isEmpty());
				
		assertEquals("RDG name", resource.getName());
		
		SSWAPSubject subject = rdg.createSubject(rdgURI);
		
		assertNotNull(subject);
		assertEquals(rdgURI, subject.getURI());
		assertTrue(subject.getProperties().isEmpty());
		
		SSWAPObject object = rdg.createObject(rdgURI);
				
		assertNotNull(object);
		assertEquals(rdgURI, object.getURI());
		assertTrue(object.getProperties().isEmpty());
				
		assertFalse(resource == subject);
		assertFalse(subject == object);
		
		
		assertTrue(resource.isSSWAPResource());
		assertTrue(subject.isSSWAPResource());
		assertTrue(object.isSSWAPResource());
		
		assertFalse(resource.isSSWAPProvider());
		assertFalse(resource.isSSWAPGraph());		
		assertFalse(subject.isSSWAPProvider());
		assertFalse(subject.isSSWAPGraph());		
		assertFalse(object.isSSWAPProvider());
		assertFalse(object.isSSWAPGraph());
		
		assertNull(resource.asSSWAPGraph());
		assertNull(resource.asSSWAPProvider());		
		assertNull(subject.asSSWAPGraph());
		assertNull(subject.asSSWAPProvider());
		assertNull(object.asSSWAPGraph());
		assertNull(object.asSSWAPProvider());
		
		assertNotNull(resource.asSSWAPResource());
		assertNotNull(resource.asSSWAPSubject());
		assertNotNull(resource.asSSWAPObject());		
		assertNotNull(subject.asSSWAPResource());
		assertNotNull(subject.asSSWAPSubject());
		assertNotNull(subject.asSSWAPObject());		
		assertNotNull(object.asSSWAPResource());
		assertNotNull(object.asSSWAPSubject());
		assertNotNull(object.asSSWAPObject());
		
		assertTrue(resource == resource.asSSWAPResource());
		assertTrue(resource == subject.asSSWAPResource());
		assertTrue(resource == object.asSSWAPResource());
		
		assertTrue(subject == resource.asSSWAPSubject());
		assertTrue(subject == subject.asSSWAPSubject());
		assertTrue(subject == object.asSSWAPSubject());
		
		assertTrue(object == resource.asSSWAPObject());
		assertTrue(object == subject.asSSWAPObject());
		assertTrue(object == object.asSSWAPObject());
	}
	
	@Test
	public void testCreateRepeatedIndividuals() {
		URI pdgURI = URI.create(NS + "providerURI");
		URI rdgURI = URI.create(NS + "resourceURI");
		URI subjectURI = URI.create(NS + "subjectURI");
		URI objectURI = URI.create(NS + "objectURI");
		
		SSWAPProvider sswapProvider = SSWAP.createProvider(pdgURI);
		RDG rdg = SSWAP.createRDG(rdgURI,"RDG name", "RDG oneLineDescription", sswapProvider);
					
		SSWAPSubject subject = rdg.createSubject(subjectURI);
		
		assertNotNull(subject);
		assertEquals(subjectURI, subject.getURI());
				
		SSWAPSubject subject2 = rdg.createSubject(subjectURI);
		
		assertNotNull(subject2);
		assertEquals(subject, subject2);
		assertTrue(subject == subject2);
		
		SSWAPObject object = rdg.createObject(objectURI);
		assertNotNull(object);
		assertEquals(objectURI, object.getURI());
		
		SSWAPObject object2 = rdg.createObject(objectURI);
		assertNotNull(object2);
		assertEquals(objectURI, object2.getURI());
		
		assertEquals(object, object2);
		assertTrue(object == object2);		
	}
}
