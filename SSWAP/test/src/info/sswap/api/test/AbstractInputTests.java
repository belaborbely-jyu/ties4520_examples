/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDatatype;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPLiteral;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPType;

import java.net.URI;

import org.junit.Before;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;

/**
 * Tests for inputs.
 * 
 * @author Evren Sirin
 */
public abstract class AbstractInputTests {
	// constants used in the tests
	
	protected static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:InputTest#";

	protected static SSWAPDocument DOC;
	protected static SSWAPType A;
	protected static SSWAPType B;
	protected static SSWAPType C;
	protected static SSWAPDatatype D;
	protected static SSWAPDatatype XSD_INT;
	protected static SSWAPIndividual a;
	protected static SSWAPIndividual b;
	protected static SSWAPLiteral l1;
	protected static SSWAPLiteral l2;
	protected static SSWAPPredicate P;
	protected static SSWAPPredicate R;
	
	/**
	 * Initializes the types and predicates.
	 * 
	 * @throws Exception
	 */
	@Before
	public void init() throws Exception {
		DOC = SSWAP.createSSWAPDocument(URI.create(NS + "Model"));
		A = DOC.getType(URI.create(NS + "A"));
		B = DOC.getType(URI.create(NS + "B"));
		C = DOC.getType(URI.create(NS + "C"));
		D = DOC.getDatatype(URI.create(NS + "D"));
		XSD_INT = DOC.getDatatype(URI.create(XSDDatatype.XSDint.getURI()));
		P = DOC.getPredicate(URI.create(NS + "P"));
		R = DOC.getPredicate(URI.create(NS + "R"));
		a = DOC.createIndividual(URI.create(NS + "a"));
		b = DOC.createIndividual(URI.create(NS + "b"));
		l1 = DOC.createLiteral("1");
		l2 = DOC.createTypedLiteral("2", XSD_INT.getURI());
	}
}
